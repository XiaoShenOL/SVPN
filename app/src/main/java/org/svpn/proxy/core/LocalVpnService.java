package org.svpn.proxy.core;

import android.content.Intent;
import android.content.Context;

import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;

import android.app.PendingIntent;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import android.graphics.BitmapFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Method;

import android.widget.Toast;

import android.content.Intent;
import android.content.IntentFilter;

import org.svpn.proxy.*;
import org.svpn.proxy.MainActivity;
import org.svpn.proxy.dns.DnsPacket;
import org.svpn.proxy.tcpip.CommonMethods;
import org.svpn.proxy.tcpip.IPHeader;
import org.svpn.proxy.tcpip.TCPHeader;
import org.svpn.proxy.tcpip.UDPHeader;
import org.svpn.proxy.utils.SpUtils;
import org.svpn.proxy.utils.NetUtils;
import org.svpn.proxy.utils.AppUtils;
import org.svpn.proxy.utils.ToolUtils;
import org.svpn.proxy.utils.TextUtils;
import org.svpn.proxy.utils.ConfigUtils;
import org.svpn.proxy.utils.StatusBarUtils;
import org.svpn.proxy.core.wifi.ProxyServer;
import org.svpn.proxy.core.ProxyConfig.IPAddress;

public class LocalVpnService extends VpnService
implements Runnable, Constants, LocalConstants {

    public static LocalVpnService Instance;
    public static boolean IsRunning = false;
	public static boolean IS_DEBUG = false;

	private SpUtils sp;

    private static int ID;
    private static int LOCAL_IP;

    private Thread m_VPNThread;

    private ParcelFileDescriptor m_VPNInterface;
    private TcpProxyServer m_TcpProxyServer;
    private DnsProxy m_DnsProxy;
    private FileOutputStream m_VPNOutputStream;

    private byte[] m_Packet;
    private IPHeader m_IPHeader;
    private TCPHeader m_TCPHeader;
    private UDPHeader m_UDPHeader;
    private ByteBuffer m_DNSBuffer;

	//网速（接收、发送）
	private long m_SpeedSend;
	private long m_SpeedReceived;

	//统计流量(接收、发送)
	private long m_SendBytes;
	private long m_ReceivedBytes;

	private String m_SpeedR,m_SpeedS;

	private NotificationManager m_Manager;
	private Notification.Builder m_Builder;
	private Notification.BigTextStyle m_BigTextStyle;
	private int NOTIFICATION_ID = 3;

	private Timer m_Timer,up_Timer;
	private double TIME_SPAN = 1000d;
	private double TIME_SPAN_UP = 60*1000d;

	//时间计时
	private long m_Current = 3,s_Current = 2;

	private LocalReceivers mOnBroadcastReceiver;

    public LocalVpnService() {
        ID++;
		Instance = this;
		LocalVpnService.IS_DEBUG = true;
        m_Packet = new byte[20000];
        m_IPHeader = new IPHeader(m_Packet, 0);
        m_TCPHeader = new TCPHeader(m_Packet, 20);
        m_UDPHeader = new UDPHeader(m_Packet, 20);
        m_DNSBuffer = ((ByteBuffer) ByteBuffer.wrap(m_Packet).position(28)).slice();
        System.out.printf("New VPNService(%d)\n", ID);
    }

    @Override
    public void onCreate() {
        System.out.printf("VPNService(%s) created.\n", ID);
        // Start a new session by creating a new thread.
		sp = new SpUtils(this);
		mOnBroadcastReceiver = new LocalReceivers();
		IntentFilter filter = new IntentFilter();
        filter.addAction(LocalReceivers.PLAY_BROADCAST_NAME);
        filter.addAction(LocalReceivers.PAUSE_BROADCAST_NAME);
        registerReceiver(mOnBroadcastReceiver, filter); //注册广播

        if (m_VPNThread == null) {
			m_VPNThread = new Thread(this, "VPNServiceThread");
			m_VPNThread.start();
        }
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		this.IsRunning = true;
        return super.onStartCommand(intent, flags, startId);
    }

    public void sendUDPPacket(IPHeader ipHeader, UDPHeader udpHeader) {
        try {
            CommonMethods.ComputeUDPChecksum(ipHeader, udpHeader);
            this.m_VPNOutputStream.write(ipHeader.m_Data, ipHeader.m_Offset, ipHeader.getTotalLength());
        } catch (IOException e) {
            e.printStackTrace();
			MainActivity.writeLog("sendUDPPacket: "+e.toString());
        }
    }

    @Override
    public synchronized void run() {
        try {
            System.out.printf("VPNService(%s) work thread is runing...\n", ID);

            ProxyConfig.AppInstallID = sp.getString("AppInstallID", null);//获取安装ID
			if (ProxyConfig.AppInstallID == null || ProxyConfig.AppInstallID.isEmpty()) {
				ProxyConfig.AppInstallID = UUID.randomUUID().toString();
				sp.putString("AppInstallID", ProxyConfig.AppInstallID);
			}
			ProxyConfig.APN=NetUtils.getApnType(getApplicationContext(),0);
			ProxyConfig.LocalIp = NetUtils.getLocalIp();

			//Shadowsocks
			ProxyConfig.IS_SSRUN = sp.getBoolean(SHADOW_SOCKS,false);
			//判断是否使用自动更新配置
			ProxyConfig.IS_UPNETWORK = sp.getBoolean(UPNETWORK,false);

			if(!TextUtils.isEmpty(LocalConfig.Base_Conf)){
				if(ProxyConfig.IS_SSRUN){
					ProxyConfig.IS_SSRUN = false;
				}
			}

			if(ProxyConfig.IS_UPNETWORK&&ProxyConfig.IS_SSRUN){
				ProxyConfig.IS_SSRUN = false;
			}

			//共享热点
			ProxyConfig.IS_HOTSPOT = sp.getBoolean(HOTSPOT,false);
			//锁定目标IP
			ProxyConfig.IS_LOCK_IP = sp.getBoolean(LOCK_IP,false);
			ProxyConfig.GET_LOCK_IP = sp.getString(ON_LOCK_IP,"");

            System.out.printf("AppInstallID: %s\n", ProxyConfig.AppInstallID);
			MainActivity.writeLog("Local IP: %s\n", ProxyConfig.LocalIp);
			if(!TextUtils.isEmpty(ProxyConfig.APN))
				MainActivity.writeLog("Access Point Network: %s\n", ProxyConfig.APN);

			waitUntilPreapred();//检查是否准备完毕。

			if(ProxyConfig.IS_SSRUN){

				ChinaIpMaskManager.loadFromFile(getResources().openRawResource(R.raw.ipmask));//加载中国的IP段，用于IP分流。

				try {
					ProxyConfig.Instance.loadFromFile(getResources().openRawResource(R.raw.config));
				} catch (Exception e) {
					String errString = e.getMessage();
					if (errString == null || errString.isEmpty()) {
						errString = e.toString();
					}
					MainActivity.writeLog("Load failed with error: %s", errString);
				}
			}else{
				
				if(ProxyConfig.IS_UPNETWORK)
					LocalConfig.Base_Conf = ToolUtils.getHtml();
				
				//获取本地配置
				String path = LocalConfig.getPath()+sp.getString(CONF_NAME,"c.conf");
				LocalConfig.Local_Conf = LocalConfig.get_Data(path);

				//需要抓包的应用包名
				ProxyConfig.CapPackage = sp.getString("cap_package","com.tencent.mtt");
				//抓包头域
				String ty = sp.getString(CAPTURE_TY,"Q-GUID,Q-Token");

				//是否需要抓包
				ProxyConfig.IS_CAPTURE = sp.getBoolean(CAPTURE,false);

				if(ProxyConfig.IS_CAPTURE)
				//检查需要抓包的应用是否已安装
					if(AppUtils.checkPackInfo(this,ProxyConfig.CapPackage)){
						//设定抓包头域
						ProxyConfig.Capture =LocalConfig.get_String(ty);
						//设置抓包应用的名称
						ProxyConfig.CapApp = AppUtils.getAppName(this,ProxyConfig.CapPackage);
					}
				//判断是否需要使用服务器配置
				if(TextUtils.isEmpty(LocalConfig.Base_Conf)){
					MainActivity.writeLog("加载配置: \n%s\n", path);
				}else{
					BaseContext.handler.sendEmptyMessage(5);//提示首次获取成功
					MainActivity.writeLog("获取服务器配置成功！\n");
					Thread.sleep(800);
					if(ProxyConfig.IS_CAPTURE){
						ProxyConfig.IS_CAPTURE = false;
					}
				}

				ConfigUtils.intiConfig();

				//初始化抓包配置
				LocalConfig.CapData = LocalConfig.Local_Conf;
			}

			// 启动TcpServer
			if (m_TcpProxyServer == null) {
				m_TcpProxyServer = new TcpProxyServer(0);
				m_TcpProxyServer.start();
			}

			// 启动DNS解析器
			if (m_DnsProxy == null) {
				m_DnsProxy = new DnsProxy();
				m_DnsProxy.start();
			}

            while (true) {
                if (LocalVpnService.IsRunning) {
					if(ProxyConfig.IS_SSRUN){
						try {
							ProxyConfig.Instance.m_ProxyList.clear();
							ProxyConfig.Instance.addProxyToList(sp.getString(SHADOW_SOCKS_URL,""));
							MainActivity.writeLog("Shadowsocks: %s\n", ProxyConfig.Instance.getDefaultProxy(0));
						} catch (Exception e) {
							String errString = e.getMessage();
							if (errString == null || errString.isEmpty()) {
								errString = e.toString();
							}
							throw new Exception("Invalid config file: "+errString);
						}
					}else{
						//加载配置文件
						ConfigUtils.logConfig();
					}
                    runVPN();
                } else {
                    Thread.sleep(100);
                }
            }
        } catch (InterruptedException e) {
            System.out.println(e);
        } catch (Exception e) {
            e.printStackTrace();
			MainActivity.writeLog("Fatal error: %s", e.toString());
        } finally {
			if(MainActivity.IS_DEBUG)
				MainActivity.Instance.onStatusChanged(getString(R.string.app_name) + (" ") + getString(R.string.vpn_disconnected_status), false);
            dispose();
        }
    }

	//开启共享热点
	public static void startSocketProxy(){
		if(startProxyServer()){
			MainActivity.writeLog("已开启共享热点.");
		}else{
			MainActivity.writeLog("开启共享热点失败！");
		}
	}

	public static boolean startProxyServer(){
		if(!LocalVpnService.IsRunning)
			return false;
		ProxyServer proxyServer = ProxyServer.getInstance();
		if (proxyServer.isRunning()) {
			return true;
		}
		return proxyServer.start();
	}

	//关闭热点共享
	public static void stopSocketProxy(){
		if(!ProxyServer.getInstance().isRunning()){
			return;
		}
		if(stopProxyServer()){
			MainActivity.writeLog("已关闭共享热点.");
		}else{
			MainActivity.writeLog("关闭共享热点失败！");
		}
	}
	public static boolean stopProxyServer(){
		ProxyServer proxyServer = ProxyServer.getInstance();
		if (!proxyServer.isRunning()) {
			return true;
		}
		return proxyServer.stop();
	}

    private void runVPN() throws Exception {
        this.m_VPNInterface = establishVPN();
        this.m_VPNOutputStream = new FileOutputStream(m_VPNInterface.getFileDescriptor());
        FileInputStream in = new FileInputStream(m_VPNInterface.getFileDescriptor());
        int size = 0;
        while (size != -1 && LocalVpnService.IsRunning) {
            while ((size = in.read(m_Packet)) > 0 && LocalVpnService.IsRunning) {
                if (m_DnsProxy.Stopped || m_TcpProxyServer.Stopped) {
                    in.close();
                    throw new Exception("LocalServer stopped.");
                }
                onIPPacketReceived(m_IPHeader, size);
            }
            Thread.sleep(20);
        }
        in.close();
        disconnectVPN();
    }

    void onIPPacketReceived(IPHeader ipHeader, int size) throws IOException {
        switch (ipHeader.getProtocol()) {
            case IPHeader.TCP:
                TCPHeader tcpHeader = m_TCPHeader;
                tcpHeader.m_Offset = ipHeader.getHeaderLength();
                if (ipHeader.getSourceIP() == LOCAL_IP) {
                    if (tcpHeader.getSourcePort() == m_TcpProxyServer.Port) {// 收到本地TCP服务器数据
                        NatSession session = NatSessionManager.getSession(tcpHeader.getDestinationPort());
                        if (session != null) {
                            ipHeader.setSourceIP(ipHeader.getDestinationIP());
                            tcpHeader.setSourcePort(session.RemotePort);
                            ipHeader.setDestinationIP(LOCAL_IP);
							try {
								CommonMethods.ComputeTCPChecksum(ipHeader, tcpHeader);
								m_VPNOutputStream.write(ipHeader.m_Data, ipHeader.m_Offset, size);
								m_ReceivedBytes += size;
							} catch (IOException e) {
								e.printStackTrace();
								MainActivity.writeLog("ReceivedBytes: "+e.toString());
							}
                        } else {
                            System.out.printf("NoSession: %s %s\n", ipHeader.toString(), tcpHeader.toString());
                        }
                    } else {

                        // 添加端口映射
                        int portKey = tcpHeader.getSourcePort();
                        NatSession session = NatSessionManager.getSession(portKey);
                        if (session == null || session.RemoteIP != ipHeader.getDestinationIP() || session.RemotePort != tcpHeader.getDestinationPort()) {
                            session = NatSessionManager.createSession(portKey, ipHeader.getDestinationIP(), tcpHeader.getDestinationPort());
                        }

                        session.LastNanoTime = System.nanoTime();
                        session.PacketSent++;//注意顺序

                        int tcpDataSize = ipHeader.getDataLength() - tcpHeader.getHeaderLength();
                        if (session.PacketSent == 2 && tcpDataSize == 0) {
							return;//丢弃tcp握手的第二个ACK报文。因为客户端发数据的时候也会带上ACK，这样可以在服务器Accept之前分析出HOST信息。
						}

                        //分析数据，找到host
                        if (session.BytesSent == 0 && tcpDataSize > 10) {
                            int dataOffset = tcpHeader.m_Offset + tcpHeader.getHeaderLength();
                            String host = HttpHostHeaderParser.parseHost(session,tcpHeader.m_Data, dataOffset, tcpDataSize);
                            if (host != null) {
                                session.RemoteHost = host;
                            } else {
                                System.out.printf("No host name found: %s", session.RemoteHost);
                            }
                        }

                        // 转发给本地TCP服务器
                        ipHeader.setSourceIP(ipHeader.getDestinationIP());
                        ipHeader.setDestinationIP(LOCAL_IP);
                        tcpHeader.setDestinationPort(m_TcpProxyServer.Port);
						try {
							CommonMethods.ComputeTCPChecksum(ipHeader, tcpHeader);
							m_VPNOutputStream.write(ipHeader.m_Data, ipHeader.m_Offset, size);
							session.BytesSent += tcpDataSize;//注意顺序
							m_SendBytes += size;
						} catch (IOException e) {
							e.printStackTrace();
							MainActivity.writeLog("SendBytes: "+e.toString());
						}						
                    }

					if (m_Timer == null) {
						m_Timer = new Timer();
						m_Timer.scheduleAtFixedRate(new RefreshTask(), 0L, (long) TIME_SPAN);
					}

					if(ProxyConfig.IS_UPNETWORK)
						if (up_Timer == null) {
							up_Timer = new Timer();
							int time = sp.getInt(UPNETWORK_TIME,10);
							up_Timer.scheduleAtFixedRate(new UPRefreshTask(), (long) (time*TIME_SPAN_UP), (long) (time*TIME_SPAN_UP));
						}

                }
                break;
            case IPHeader.UDP:
                // 转发DNS数据包：
                UDPHeader udpHeader = m_UDPHeader;
                udpHeader.m_Offset = ipHeader.getHeaderLength();
                if (ipHeader.getSourceIP() == LOCAL_IP && udpHeader.getDestinationPort() == 53) {
                    m_DNSBuffer.clear();
                    m_DNSBuffer.limit(ipHeader.getDataLength() - 8);
                    DnsPacket dnsPacket = DnsPacket.FromBytes(m_DNSBuffer);
                    if (dnsPacket != null && dnsPacket.Header.QuestionCount > 0) {
                        m_DnsProxy.onDnsRequestReceived(ipHeader, udpHeader, dnsPacket);
                    }
                }
                break;
        }
    }

	/**
     * 更新通知栏UI *
     */
	class RefreshTask extends TimerTask {
        @Override
        public void run() {
			try {
				m_SpeedReceived+=m_ReceivedBytes; //累计接收字节
				m_SpeedSend+=m_SendBytes; //累计发送字节
				m_Build_Notification(); //建立通知栏
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
    }

	/**
     * 更新通知栏UI *
     */
	class UPRefreshTask extends TimerTask {
        @Override
        public void run() {
			try {
				ConfigUtils.getQQConfig();
			} catch (Exception e) {
				e.printStackTrace();
				MainActivity.writeLog("自动更新失败: "+e.toString());
			}
        }
    }

	private void m_Cancel_Notification() {
		try {
			if (m_Timer != null) {
				m_Timer.cancel();//关闭计时器
			}
			if (up_Timer != null) {
				up_Timer.cancel();//关闭计时器
			}
			
			m_Manager.cancelAll();//关闭通知栏
			//this.stopForeground(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    private void m_Build_Notification() {
		try {			
			if(m_Builder==null||m_Manager==null){
				m_Builder = new Notification.Builder(LocalVpnService.this);
				m_BigTextStyle = new Notification.BigTextStyle();
				m_Manager = (NotificationManager)
					getSystemService(Context.NOTIFICATION_SERVICE);

				if(AppUtils.checkPackInfo(this,ProxyConfig.CapPackage)&&(ProxyConfig.IS_CAPTURE)){
					Intent pauseIntent = new Intent(LocalReceivers.PAUSE_BROADCAST_NAME);
					pauseIntent.putExtra(LocalReceivers.FLAG_ID, LocalReceivers.PAUSE_FLAG);
					PendingIntent pausePIntent = PendingIntent.getBroadcast(LocalVpnService.this, 0, pauseIntent, 0);

					Notification.Action pauseAction =
						new Notification.Action.Builder(
						R.drawable.ic_service_idle,"重新抓包",pausePIntent).build();
					m_Builder.addAction(pauseAction);
				}

				if(ProxyConfig.IS_UPNETWORK){
					ProxyConfig.IS_UPNETWORK = false;
					Intent pauseIntent = new Intent(LocalReceivers.PLAY_BROADCAST_NAME);
					pauseIntent.putExtra(LocalReceivers.FLAG_ID, LocalReceivers.PLAY_FLAG);
					PendingIntent pausePIntent = PendingIntent.getBroadcast(LocalVpnService.this, 0, pauseIntent, 0);

					Notification.Action pauseAction =
						new Notification.Action.Builder(
						R.drawable.ic_service_idle,"手动获取",pausePIntent).build();
					m_Builder.addAction(pauseAction);
				}

				String str = getString(R.string.app_name)+(" - ")+Build.MODEL;
				if(ProxyConfig.IS_SSRUN)
					str = "Shadowsocks"+(" - ")+Build.MODEL;

				m_Builder.setContentTitle(str);
				m_Builder.setSmallIcon(R.drawable.rocket);
				m_Builder.setTicker(str);
				m_Builder.setWhen(System.currentTimeMillis());
				m_Builder.setOngoing(true);
				m_Builder.setVibrate(new long[]{0});
				m_Builder.setSound(null);
				m_Builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					//ID
					String id = getString(R.string.svpn_statusline_id);
					//Name
					String name = getString(R.string.svpn_statusline_name);
					NotificationChannel channel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_LOW);
					channel.setBypassDnd(true);    //设置绕过免打扰模式
					channel.canBypassDnd();       //检测是否绕过免打扰模式
					/*/ 设置通知出现时的闪灯（如果 android 设备支持的话）
					 notificationChannel .enableLights(true);
					 notificationChannel .setLightColor(Color.RED);*/
					channel.enableLights(false);
					// 配置通知渠道的属性
					channel .setDescription(getString(R.string.svpn_statusline_description));
					// 设置通知出现时的震动（如果 android 设备支持的话）
					channel .enableVibration(false);
					//如上设置使手机：静止1秒，震动2秒，静止1秒，震动3秒
					channel.setVibrationPattern(new long[]{0});
					channel.setSound(null, null);
					m_Manager.createNotificationChannel(channel);
					m_Builder.setChannelId(id);
				}
			}
			if (m_Current == 3) {//3秒执行一次
				m_SpeedR=ToolUtils.get_Szie(m_SpeedReceived);
				m_SpeedS=ToolUtils.get_Szie(m_SpeedSend);
				if(MainActivity.IS_DEBUG)
					MainActivity.Instance.onNetworkTraffic(m_SpeedS, m_SpeedR);
				m_Current=0; //从0开始重新累计
			}
			String R_Bytes=ToolUtils.get_Szie(m_ReceivedBytes);
			String S_Bytes=ToolUtils.get_Szie(m_SendBytes);

			if(s_Current == 2){
				m_Builder.setContentText(String.format(getString(R.string.svpn_statusline_bytecount),S_Bytes,R_Bytes));
				String bigText = String.format(getString(R.string.svpn_statusline_bigtext),m_SpeedS,S_Bytes,m_SpeedR,R_Bytes);
				m_BigTextStyle.bigText(bigText);
				//update
				m_Builder.setStyle(m_BigTextStyle);
				s_Current=0; //从0开始重新累计
			}
			m_Current++; //累计3秒时间
			s_Current++; //累计1秒时间
			//恢复(发送、接收)数据为0个字节
			m_SendBytes = 0; m_ReceivedBytes = 0;
			Notification notification = m_Builder.build();
			//build
			m_Manager.notify(NOTIFICATION_ID, notification);
			//this.startForeground(NOTIFICATION_ID,notification);
		} catch (Exception e) {
			e.printStackTrace();
			MainActivity.writeLog("显示通知失败: %s", e.toString().substring(1));
		}
    }

    private void waitUntilPreapred() {
        while (prepare(this) != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private ParcelFileDescriptor establishVPN() throws Exception {
        Builder builder = new Builder();
        builder.setMtu(ProxyConfig.Instance.getMTU());
        if (ProxyConfig.IS_DEBUG)
            System.out.printf("setMtu: %d\n", ProxyConfig.Instance.getMTU());

        IPAddress ipAddress = ProxyConfig.Instance.getDefaultLocalIP();
        LOCAL_IP = CommonMethods.ipStringToInt(ipAddress.Address);
        builder.addAddress(ipAddress.Address, ipAddress.PrefixLength);
        if (ProxyConfig.IS_DEBUG)
            System.out.printf("addAddress: %s/%d\n", ipAddress.Address, ipAddress.PrefixLength);

        for (ProxyConfig.IPAddress dns : ProxyConfig.Instance.getDnsList()) {
            builder.addDnsServer(dns.Address);
            if (ProxyConfig.IS_DEBUG)
                System.out.printf("addDnsServer: %s\n", dns.Address);
        }

        if (ProxyConfig.Instance.getRouteList().size() > 0) {
            for (ProxyConfig.IPAddress routeAddress : ProxyConfig.Instance.getRouteList()) {
                builder.addRoute(routeAddress.Address, routeAddress.PrefixLength);
                if (ProxyConfig.IS_DEBUG)
                    System.out.printf("addRoute: %s/%d\n", routeAddress.Address, routeAddress.PrefixLength);
            }
            builder.addRoute(CommonMethods.ipIntToString(ProxyConfig.FAKE_NETWORK_IP), 16);

            if (ProxyConfig.IS_DEBUG)
                System.out.printf("addRoute for FAKE_NETWORK: %s/%d\n", CommonMethods.ipIntToString(ProxyConfig.FAKE_NETWORK_IP), 16);
        } else {
            builder.addRoute("0.0.0.0", 0);
            if (ProxyConfig.IS_DEBUG)
                System.out.printf("addDefaultRoute: 0.0.0.0/0\n");
        }

		try {
			//配置dns
			if (!TextUtils.isEmpty(ProxyConfig.DnsUrl)) {
				StringBuilder sb = new StringBuilder();
				String[] mDnsUrl = LocalConfig.get_String(ProxyConfig.DnsUrl);
				for (String dns : mDnsUrl) {
					builder.addDnsServer(dns);
					// 3）DNS Server，就是该端口的DNS服务器地址；
					sb.append(",").append(dns);
				}
				MainActivity.writeLog("dns_url: %s\n", sb.toString().substring(1));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			//应用放行
			if (sp.getBoolean(INFO_BOOLEAN)) {
				String packages = sp.getString(APP_INFO,"");
				String[] appinfo = LocalConfig.get_String(packages);
				if (!TextUtils.isEmpty(packages)) {
					StringBuilder sb = new StringBuilder();
					for (String info : appinfo) {
						builder.addDisallowedApplication(info);
						sb.append(" | ").append(AppUtils.getAppName(this,info));
					}
					MainActivity.writeLog("应用放行: %s\n", sb.toString().substring(2));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		try{
			Class<?> SystemProperties = Class.forName("android.os.SystemProperties");
			Method method = SystemProperties.getMethod("get", new Class[]{String.class});
			ArrayList<String> servers = new ArrayList<String>();
			for (String name : new String[]{"net.dns1", "net.dns2", "net.dns3", "net.dns4",}) {
				String value = (String) method.invoke(null, name);
				if (value != null && !"".equals(value) && !servers.contains(value)) {
					servers.add(value);
					if (value.replaceAll("\\d", "").length() == 3){//防止IPv6地址导致问题
						builder.addRoute(value, 32);
					} else {
						builder.addRoute(value, 128);
					}
					if (ProxyConfig.IS_DEBUG)
						System.out.printf("%s=%s\n", name, value);
				}
			}
		} catch (Exception e){
			e.printStackTrace();
			MainActivity.writeLog(e.toString());
		}

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder.setConfigureIntent(pendingIntent);

        builder.setSession(getString(R.string.app_name));
        ParcelFileDescriptor pfdDescriptor = builder.establish();

		if (ToolUtils.isAndroidO()){
			OnTileService.updateTileService(this,true);
		}

		//执行热点共享
		if(ProxyConfig.IS_HOTSPOT){
			startSocketProxy();
		}

		if(TextUtils.isEmpty(LocalConfig.Base_Conf)){

			if(ProxyConfig.IS_CAPTURE){
				MainActivity.writeLog("已开启应用抓包.");
			}

			if(ProxyConfig.IS_CAPTURE&&!ProxyConfig.IS_UPNETWORK){
				AppUtils.OpenApp(this,ProxyConfig.CapPackage);
				StatusBarUtils.collapsingNotification(this);
			}
		}

		if(MainActivity.IS_DEBUG){
			MainActivity.Instance.onStatusChanged(getString(R.string.app_name) + (" ") + getString(R.string.vpn_connected_status), true);
		}

        return pfdDescriptor;
    }	

    public void disconnectVPN() {

		m_Cancel_Notification();//关闭通知栏

        try {
            if (m_VPNInterface != null) {
                m_VPNInterface.close();
                m_VPNInterface = null;
            }
        } catch (Exception e) {
			e.printStackTrace();
        }
		this.m_VPNOutputStream = null;
    }

    private synchronized void dispose() {
        // 断开VPN
        disconnectVPN();

        // 停止TcpServer
        if (m_TcpProxyServer != null) {
            m_TcpProxyServer.stop();
            m_TcpProxyServer = null;
        }

        // 停止DNS解析器
        if (m_DnsProxy != null) {
            m_DnsProxy.stop();
            m_DnsProxy = null;
        }

		//关闭热点共享
		stopSocketProxy();

		LocalVpnService.IS_DEBUG = false;
		LocalVpnService.IsRunning = false;

		LocalConfig.Base_Conf = null;
		LocalConfig.CapData = "";
		ProxyConfig.Q_GT = new String[]{"[Q-GUID]","[Q-Token]"};
		this.stopSelf();
    }

    @Override
    public void onDestroy() {
        System.out.printf("VPNService(%s) destoried.\n", ID);

		if (ToolUtils.isAndroidO())
			OnTileService.updateTileService(this,false);

		if (mOnBroadcastReceiver != null) {
            unregisterReceiver(mOnBroadcastReceiver); //注销广播
        }

        if (m_VPNThread != null) {
            m_VPNThread.interrupt();
        }
    }

}


