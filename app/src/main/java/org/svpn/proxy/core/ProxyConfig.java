package org.svpn.proxy.core;

import android.annotation.SuppressLint;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import java.io.InputStream;

import android.net.Uri;
import android.util.Base64;
import android.annotation.SuppressLint;

import org.svpn.proxy.R;
import org.svpn.proxy.ss.*;
import org.svpn.proxy.MainActivity;
import org.svpn.proxy.utils.TextUtils;
import org.svpn.proxy.tcpip.CommonMethods;

public class ProxyConfig {
	public static final ProxyConfig Instance=new ProxyConfig();
	public final static boolean IS_DEBUG=false;
	public static String AppInstallID;
	
	public static String[] Capture;
	
	public static String CapApp;
	public static String CapPackage;
	
	public static String EncryptMethod;
    public static String Password;

	public static String APN;
	public static String LocalIp;

	public static String ConfDir;

    public static String HttpIp;
	public static int HttpPort;
	public static String[] HttpDel;
	public static String HttpDelBytes;
    public static String HttpHeader;

    public static String SslIp;
	public static int SslPort;
	public static String[] SslDel;
	public static String SslDelBytes;
	public static String SslHeader;

	public static String DnsUrl;
	
	//自动更新
	public static boolean IS_UPNETWORK=false;
	
	//锁定目标IP
	public static boolean IS_LOCK_IP;
	public static String GET_LOCK_IP;

	//使用ss
	public static boolean IS_SSRUN=false;

	//抓包
	public static boolean IS_CAPTURE=false;
	
	//热点共享
	public static boolean IS_HOTSPOT=false;

	public static String[] Q_GT = new String[]{"[Q-GUID]","[Q-Token]"};
	public static String[] Q_GT_1 = new String[]{"Q-GUID: ","Q-Token: "};
	
	public final static int FAKE_NETWORK_MASK=CommonMethods.ipStringToInt("255.255.0.0");
	public final static int FAKE_NETWORK_IP=CommonMethods.ipStringToInt("10.231.0.0");

	ArrayList<IPAddress> m_IpList;
    ArrayList<IPAddress> m_DnsList;
    ArrayList<IPAddress> m_RouteList;
    HashMap<String, Boolean> m_DomainMap;
    public ArrayList<InetSocketAddress> m_ProxyList;

    int m_dns_ttl;
    int m_mtu;
	Timer m_Timer;

    boolean m_outside_china_use_proxy = true;
    boolean m_isolate_http_host_header = true;

    public class IPAddress{
    	public final String Address;
    	public final int PrefixLength;
    	public IPAddress(String address,int prefixLength) {
    		this.Address=address;
    		this.PrefixLength=prefixLength;
		}
    	public IPAddress(String ipAddresString){
    		String[] arrStrings=ipAddresString.split("/");
    		String address=arrStrings[0];
    		int prefixLength=32;
    		if(arrStrings.length>1){
    			prefixLength=Integer.parseInt(arrStrings[1]);
    		}
    		this.Address=address;
    		this.PrefixLength=prefixLength;
    	}

    	@SuppressLint("DefaultLocale")
		@Override
    	public String toString() {
    		return String.format("%s/%d", Address,PrefixLength);
    	}

    	@Override
    	public boolean equals(Object o) {
			if(o==null){
				return false;
			}
			else {
				return this.toString().equals(o.toString());
			}
    	}
    }

    public ProxyConfig(){
		m_IpList = new ArrayList<IPAddress>();
    	m_DnsList=new ArrayList<IPAddress>();
    	m_RouteList=new ArrayList<IPAddress>();
		m_DomainMap=new HashMap<String, Boolean>();
		m_ProxyList = new ArrayList<InetSocketAddress>();

		m_Timer = new Timer();
        m_Timer.schedule(m_Task, 120000, 120000);//每两分钟刷新一次。

    }

    TimerTask m_Task = new TimerTask() {
        @Override
        public void run() {
            refreshProxyServer();//定时更新dns缓存
        }

        //定时更新dns缓存
        void refreshProxyServer() {
            try {
                for (int i = 0; i < m_ProxyList.size(); i++) {
                    try {
                        InetSocketAddress m_DefaultProxy = m_ProxyList.get(i);
                        InetAddress address = InetAddress.getByName(m_DefaultProxy.getHostName());
                        if (address != null && !address.equals(m_DefaultProxy.getAddress())) {
                            InetSocketAddress m_Proxy = new InetSocketAddress(address, m_DefaultProxy.getPort());
							if(m_Proxy instanceof InetSocketAddress){
								m_ProxyList.remove(i); //移除旧 InetSocketAddress
								m_ProxyList.add(i,m_Proxy); //添加新 InetSocketAddress
								MainActivity.writeLog("更新DNS(%d)缓冲成功！", i);
							}
                        }
                    } catch (Exception e) {
						MainActivity.writeLog("更新DNS(%d)缓存失败: %s", i, e.toString());
                    }
                }
            } catch (Exception e) {
				MainActivity.writeLog("更新DNS缓存失败: %s", e.toString());
            }
        }
    };

    public static boolean isFakeIP(int ip){
    	return (ip&ProxyConfig.FAKE_NETWORK_MASK)==ProxyConfig.FAKE_NETWORK_IP;
    }

	public InetSocketAddress getDefaultProxy(int i) {
        if (m_ProxyList.size() > i) {
            return m_ProxyList.get(i);
        } else {
            return null;
        }
    }

    public IPAddress getDefaultLocalIP(){
		if (m_IpList.size() > 0) {
            return m_IpList.get(0);
        } else if(TextUtils.isEmpty(LocalIp)){
			LocalIp = "10.8.0.2";
		}
		return new IPAddress(LocalIp,32);
    }

    public ArrayList<IPAddress> getDnsList(){
    	return m_DnsList;
    }

    public ArrayList<IPAddress> getRouteList(){
    	return m_RouteList;
    }

    public int getDnsTTL(){
    	if(m_dns_ttl<30){
    		m_dns_ttl=30;
    	}
    	return m_dns_ttl;
    }

    public String getUserAgent(){
    	return System.getProperty("http.agent");
    }

    public int getMTU(){
    	if(m_mtu>1400&&m_mtu<=20000){
    		return m_mtu;
    	}else {
			return 20000;
		}
    }

	private Boolean getDomainState(String domain){
		domain=domain.toLowerCase();
		while (domain.length()>0) {
			Boolean stateBoolean=m_DomainMap.get(domain);
			if(stateBoolean!=null){
				return stateBoolean;
			}else {
				int start=domain.indexOf('.')+1;
				if(start>0 && start<domain.length()){
					domain=domain.substring(start);
				}else {
					return null;
				}
			}
		}
		return null;
	}

	public boolean needProxy(String host,int ip){
    	if(host!=null){
    		Boolean stateBoolean=getDomainState(host);
    		if(stateBoolean!=null){
    			return stateBoolean.booleanValue();
    		}
    	}

    	if(isFakeIP(ip))
    		return true;

		if (m_outside_china_use_proxy && ip != 0) {
			return !ChinaIpMaskManager.isIPInChina(ip);
		}

    	return false;
    }

	public boolean isIsolateHttpHostHeader() {
        return m_isolate_http_host_header;
    }

    public void loadFromFile(InputStream inputStream) throws Exception {
        byte[] bytes = new byte[inputStream.available()];
        inputStream.read(bytes);
        loadFromLines(new String(bytes).split("\\r?\\n"));
    }

    protected void loadFromLines(String[] lines) throws Exception {
        m_IpList.clear();
        m_DnsList.clear();
        m_RouteList.clear();
        m_ProxyList.clear();
        m_DomainMap.clear();

        int lineNumber = 0;
        for (String line : lines) {
            lineNumber++;
            String[] items = line.split("\\s+");
            if (items.length < 2) {
                continue;
            }

            String tagString = items[0].toLowerCase(Locale.ENGLISH).trim();
            try {
                if (!tagString.startsWith("#")) {
                    if (ProxyConfig.IS_DEBUG)
                        System.out.println(line);

                    if (tagString.equals("ip")) {
                        addIPAddressToList(items, 1, m_IpList);
                    } else if (tagString.equals("dns")) {
                        addIPAddressToList(items, 1, m_DnsList);
                    } else if (tagString.equals("route")) {
                        addIPAddressToList(items, 1, m_RouteList);
                    } else if (tagString.equals("proxy")) {
                        addProxyToList(items, 1);
                    } else if (tagString.equals("direct_domain")) {
                        addDomainToHashMap(items, 1, false);
                    } else if (tagString.equals("proxy_domain")) {
                        addDomainToHashMap(items, 1, true);
                    } else if (tagString.equals("dns_ttl")) {
                        m_dns_ttl = Integer.parseInt(items[1]);
                    } else if (tagString.equals("outside_china_use_proxy")) {
                        m_outside_china_use_proxy = convertToBool(items[1]);
                    } else if (tagString.equals("isolate_http_host_header")) {
                        m_isolate_http_host_header = convertToBool(items[1]);
                    } else if (tagString.equals("mtu")) {
                        m_mtu = Integer.parseInt(items[1]);
                    }
                }
            } catch (Exception e) {
                throw new Exception(String.format("config file parse error: line:%d, tag:%s, error:%s", lineNumber, tagString, e));
            }

        }

    }

    public void addProxyToList(String url) throws Exception {
        InetSocketAddress config = null;
        if (ProxyConfig.isSsConfig(url)) {
            config = ProxyConfig.parse(url);
        }
		addDefaultProxy(config);
    }

	public static boolean isSsConfig(String url){
		if(TextUtils.isEmpty(url)){
			return false;
		}
		return url.startsWith("ss://");
    }
	
	public static boolean isSsrConfig(String url){
		if(TextUtils.isEmpty(url)){
			return false;
		}
		return url.startsWith("ssr://");
    }
	
    public void addDefaultProxy(InetSocketAddress config) throws Exception {
        //if (!m_ProxyList.contains(config)) {
            m_ProxyList.add(config);
            m_DomainMap.put(config.getHostName(), false);
        //}
    }

    public static InetSocketAddress parse(String proxyInfo) throws Exception {
		proxyInfo = proxyInfo.substring(5).replace("@",":");
		String[] userStrings = proxyInfo.split(":");
		if (!(userStrings.length >= 4)) {
			throw new Exception("加载Shadowsocks配置失败！");
		}
		ProxyConfig.EncryptMethod = userStrings[0];
		ProxyConfig.Password = userStrings[1];
        if (!CryptFactory.isCipherExisted(ProxyConfig.EncryptMethod)) {
            throw new Exception(String.format("Method: %s does not support!", ProxyConfig.EncryptMethod));
        }
		int port = -1;
		try {
			port = Integer.parseInt(userStrings[3]);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
        return new InetSocketAddress(userStrings[2], port);
    }

    private void addProxyToList(String[] items, int offset) throws Exception {
        for (int i = offset; i < items.length; i++) {
            addProxyToList(items[i].trim());
        }
    }

    private void addDomainToHashMap(String[] items, int offset, Boolean state) {
        for (int i = offset; i < items.length; i++) {
            String domainString = items[i].toLowerCase().trim();
            if (domainString.charAt(0) == '.') {
                domainString = domainString.substring(1);
            }
            m_DomainMap.put(domainString, state);
        }
    }

    private boolean convertToBool(String valueString) {
        if (valueString == null || valueString.isEmpty())
            return false;
        valueString = valueString.toLowerCase(Locale.ENGLISH).trim();
        if (valueString.equals("on") || valueString.equals("1") || valueString.equals("true") || valueString.equals("yes")) {
            return true;
        } else {
            return false;
        }
    }

    private void addIPAddressToList(String[] items, int offset, ArrayList<IPAddress> list) {
        for (int i = offset; i < items.length; i++) {
            String item = items[i].trim().toLowerCase();
            if (item.startsWith("#")) {
                break;
            } else {
                IPAddress ip = new IPAddress(item);
                if (!list.contains(ip)) {
                    list.add(ip);
                }
            }
        }
    }
}


