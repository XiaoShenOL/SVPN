package org.svpn.proxy;

import android.view.*;
import android.widget.*;
import android.net.Uri;
import java.util.List;
import java.util.ArrayList;
import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.graphics.Color;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.support.annotation.NonNull;
import android.content.pm.PackageManager;
import android.support.v4.view.ViewPager; 
import android.support.v4.view.GravityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment; 
import android.support.v4.app.FragmentManager; 
import android.support.v4.app.FragmentPagerAdapter; 
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;

import java.net.*;

import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Method;

import android.text.InputType;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import org.github.itoast.IToast;

import org.svpn.proxy.core.*;
import org.svpn.proxy.utils.*;
import org.github.settingitem.SettingItem;
import org.github.statusbar.StatusBarUtils;

import com.github.shadowsocks.constant.State;
import com.github.shadowsocks.utils.SS_SDK;
import com.github.shadowsocks.utils.VpnCallback;

/*
import com.vector.update.http.OkGoUpdateHttpUtil;
import com.vector.update.http.UpdateAppHttpUtil;
import com.vector.update.SilenceUpdateCallback;
import com.vector.update.UpdateAppBean;
import com.vector.update.UpdateAppManager;
import com.vector.update.UpdateCallback;
import com.vector.update.listener.ExceptionHandler;
import com.vector.update.listener.IUpdateDialogFragmentListener;
import com.vector.update.service.DownloadService;
import com.vector.update.utils.AppUpdateUtils;
import com.vector.update.utils.DrawableUtil;
*/
public class MainActivity extends BaseActivity 
implements Constants, View.OnClickListener, OnCheckedChangeListener,
SettingItem.OnItemClickListener, SettingItem.OnItemLongClickListener, VpnCallback {

	public static MainActivity Instance;
	public static boolean IS_DEBUG = false;

	private Handler m_Handler;
	
    private DrawerLayout mDrawerLayout;
    private Toolbar mToolbar;
	private Button main,mode;
	private ViewPager mViewPager;

    private LinearLayout contentLayout;
	private SpUtils sp;
	private ClipboardUtils cu;

	private SwitchCompat switchProxy;

	private ProgressDialog pd;
	private static final int MSG_LOAD_BG = 1;
	private static final int MSG_LOAD_START = 2;
	private static final int MSG_LOAD_FINISH = 3;

	private final static int REQUEST_CHOOSEFILE = 0;

	private int mColor[] = {Color.parseColor("#e0303030"),
		Color.parseColor("#70303030")};
    private int mTextColor[] = {Color.parseColor("#18B4ED"),
		Color.parseColor("#FFFFFF")};
	private List<Fragment> mFragmentList = new ArrayList<>(); 

	private SettingItem cellphone_android,tshirt_crew,speedometer,rocket,busy,
	apple_safari,github,android_debug_bridge,dns,radio_tower,package_variant,
	cloud_download,minus_network,content_copy,cog,export;

	private static final int MY_PERMISSION_REQUEST_CODE = 10000;
	private static final int START_VPN_SERVICE_REQUEST_CODE = 1985;

	private static String[] permission = new String[] {
		Manifest.permission.READ_CONTACTS,
		Manifest.permission.READ_EXTERNAL_STORAGE,
		Manifest.permission.WRITE_EXTERNAL_STORAGE};
		
	/**
     * 最简方式
     *
     * @param view
     *
    private void updateApp() {
        new UpdateAppManager
			.Builder()
			//当前Activity
			.setActivity(this)
			//更新地址
			.setUpdateUrl(mUpdateUrl)
			.handleException(new ExceptionHandler() {
				@Override
				public void onException(Exception e) {
					e.printStackTrace();
				}
			})
			//实现httpManager接口的对象
			.setHttpManager(new UpdateAppHttpUtil())
			.build()
			.update();
    }

	/**
     * 强制更新
     *
     * @param view
     *
    private void constraintUpdate() {
        new UpdateAppManager
			.Builder()
			//当前Activity
			.setActivity(this)
			//更新地址
			.setUpdateUrl(mUpdateUrl1)
			//实现httpManager接口的对象
			.setHttpManager(new UpdateAppHttpUtil())
			.build()
			.update();
    }*/
	
	final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_LOAD_BG:
					setBackground(null,false);//设置背景
					break;
				case MSG_LOAD_START:
					pd = new ProgressDialog(MainActivity.this);
					pd.setMessage("正在加载背景…");
					// 设置是否可以通过点击Back键取消
					pd.setCancelable(false);
					pd.show();					
					break;
				case MSG_LOAD_FINISH:
					if(pd!=null){
						pd.cancel();
					}
					break;
			}
			super.handleMessage(msg);
		}
	};

	private static ConcurrentHashMap<onStatusChangedListener, Object>
	m_OnStatusChangedListeners = new ConcurrentHashMap<onStatusChangedListener, Object>();
	public interface onStatusChangedListener {
        public void onStatusChanged(String status, Boolean isRunning);
        public void onLogReceived(String logString);
		public void onNetworkTraffic(String SentBytes,String ReceivedBytes);
    }

    public static void addOnStatusChangedListener(onStatusChangedListener listener) {
        if (!m_OnStatusChangedListeners.containsKey(listener)) {
            m_OnStatusChangedListeners.put(listener, 1);
        }
    }

    public static void removeOnStatusChangedListener(onStatusChangedListener listener) {
        if (m_OnStatusChangedListeners.containsKey(listener)) {
            m_OnStatusChangedListeners.remove(listener);
        }
    }

    public void onNetworkTraffic(final String SentBytes, final String ReceivedBytes) {
        m_Handler.post(new Runnable() {
				@Override
				public void run() {
					for (Map.Entry<onStatusChangedListener, Object> entry : m_OnStatusChangedListeners.entrySet()) {
						entry.getKey().onNetworkTraffic(SentBytes, ReceivedBytes);
					}
				}
			});
    }

    public void onStatusChanged(final String status, final boolean isRunning) {
        m_Handler.post(new Runnable() {
				@Override
				public void run() {
					for (Map.Entry<onStatusChangedListener, Object> entry : m_OnStatusChangedListeners.entrySet()) {
						entry.getKey().onStatusChanged(status, isRunning);
					}
					switchProxy.post(new Runnable() {
							@Override
							public void run() {
								switchProxy.setEnabled(true);
								switchProxy.setChecked(isRunning);
							}
						});
					Toast.makeText(MainActivity.this, status, Toast.LENGTH_SHORT).show();
				}
			});
    }

	public static void writeLog(String format, Object... args) {
		if(MainActivity.IS_DEBUG)
			MainActivity.Instance.onLogReceived(format,args);
	}

    public void onLogReceived(String format, Object... args) {
		if(format==null||format.equals("")) format="";
        final String logString = String.format(format, args);
        m_Handler.post(new Runnable() {
				@Override
				public void run() {
					for (Map.Entry<onStatusChangedListener, Object> entry : m_OnStatusChangedListeners.entrySet()) {
						entry.getKey().onLogReceived(logString);
					}
				}
			});
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		Instance = this;
		sp = BaseContext.getSpUtils();
		cu = new ClipboardUtils(this);
		m_Handler = new Handler();
		MainActivity.IS_DEBUG = true;
        SS_SDK.getInstance().setStateCallback(this);
		
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        contentLayout = (LinearLayout)findViewById(R.id.main);
        mToolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.navigation_drawer_open,
																 R.string.navigation_drawer_close);
        mDrawerLayout.setDrawerListener(toggle);
        toggle.syncState();

		mViewPager = (ViewPager) findViewById(R.id.viewpager); 

		main = (Button) findViewById(R.id.main_fragment);
		main.setOnClickListener(this);

		mode = (Button) findViewById(R.id.mode_fragment);
		mode.setOnClickListener(this);

		if(!ToolUtils.checkPermissionAllGranted(this,permission))
			ActivityCompat.requestPermissions(this,permission,MY_PERMISSION_REQUEST_CODE);

		initView();
		initViewPager();
		showWelcomeView();
		handler.sendEmptyMessage(MSG_LOAD_BG);//设置背景		
    }
	
	void showWelcomeView(){
		new Handler().postDelayed(new Runnable(){
				public void run(){
					try{
						if(!ToolUtils.isAppUse(MainActivity.this)){ APP_USE(); return;}
						if(!sp.getBoolean(FIRST_USE,false)) FIRST_USE();
						//软件自动更新
						//updateApp();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			},500);
    }
	
	private void initViewPager(){ 
		//adapter for the viewpager 
		FragmentPagerAdapter adapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
			@Override 
			public Fragment getItem(int position) { 
				return mFragmentList.get(position); 
			} 
			@Override 
			public int getCount() { 
				return mFragmentList.size(); 
			}
		};
		mFragmentList.add(new MainFragment()); 
		mFragmentList.add(new ModeFragment()); 

		//set adapter 
		mViewPager.setAdapter(adapter); 

		mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
				@Override
				public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				}
				@Override
				public void onPageSelected(int position) {
					setupView(position);
				}
				@Override
				public void onPageScrollStateChanged(int state) {
				}
			});		
	}

	private void setupView(int position){
		if(position==0){
			main.setBackgroundColor(mColor[position]);
			main.setTextColor(mTextColor[position]);
			mode.setBackgroundColor(mColor[1]);
			mode.setTextColor(mTextColor[1]);
		}else{
			main.setBackgroundColor(mColor[position]);
			main.setTextColor(mTextColor[position]);
			mode.setBackgroundColor(mColor[0]);
			mode.setTextColor(mTextColor[0]);
		}
	}

    private void initView() {
		//关于设备
		cellphone_android = (SettingItem) findViewById(R.id.cellphone_android);
		cellphone_android.setOnItemClickListener(this);
		//应用背景
		tshirt_crew = (SettingItem) findViewById(R.id.tshirt_crew);
		if(!sp.getBoolean(BG_DATA,false)) tshirt_crew.setRightText("默认");
		else tshirt_crew.setRightText("自定义");
		tshirt_crew.setOnItemClickListener(this);
		tshirt_crew.setOnItemLongClickListener(this);
		//speedtest
		speedometer = (SettingItem) findViewById(R.id.speedometer);
		speedometer.setOnItemClickListener(this);
		//Shadowsocks
		rocket = (SettingItem) findViewById(R.id.rocket);
		rocket.setChecked(sp.getBoolean(SHADOW_SOCKS,false));
		rocket.setOnItemClickListener(this);
		rocket.setOnItemLongClickListener(this);
		//ShadowsocksR
		busy = (SettingItem) findViewById(R.id.busy);
		busy.setChecked(sp.getBoolean(SHADOW_SOCKS_R,false));
		busy.setOnItemClickListener(this);
		busy.setOnItemLongClickListener(this);
		//应用指南
		apple_safari = (SettingItem) findViewById(R.id.apple_safari);
		apple_safari.setOnItemClickListener(this);
		//github
		github = (SettingItem) findViewById(R.id.github);
		github.setOnItemClickListener(this);
		//应用反馈
		android_debug_bridge = (SettingItem) findViewById(R.id.android_debug_bridge);
		android_debug_bridge.setOnItemClickListener(this);
		//应用放行
		dns = (SettingItem) findViewById(R.id.dns);
		dns.setChecked(sp.getBoolean(INFO_BOOLEAN,false));
		dns.setOnItemClickListener(this);
		dns.setOnItemLongClickListener(this);
		//热点共享
		radio_tower = (SettingItem) findViewById(R.id.radio_tower);
		radio_tower.setChecked(sp.getBoolean(HOTSPOT,false));
		radio_tower.setOnItemClickListener(this);
		radio_tower.setOnItemLongClickListener(this);
		//应用抓包
		package_variant = (SettingItem) findViewById(R.id.package_variant);
		package_variant.setChecked(sp.getBoolean(CAPTURE,false));
		package_variant.setOnItemClickListener(this);
		package_variant.setOnItemLongClickListener(this);
		//动态配置
		cloud_download = (SettingItem) findViewById(R.id.cloud_download);
		cloud_download.setChecked(sp.getBoolean(UPNETWORK,false));
		cloud_download.setOnItemClickListener(this);
		cloud_download.setOnItemLongClickListener(this);
		//锁定目标IP
		minus_network = (SettingItem) findViewById(R.id.minus_network);
		minus_network.setChecked(sp.getBoolean(LOCK_IP,false));
		minus_network.setOnItemClickListener(this);
		minus_network.setOnItemLongClickListener(this);
		//复制抓包
		content_copy = (SettingItem) findViewById(R.id.content_copy);
		content_copy.setOnItemClickListener(this);
		if(sp.getBoolean(CAPTURE,false)) content_copy.setRightText("抓包已开启");
		//设置
		cog = (SettingItem) findViewById(R.id.cog);
		cog.setOnItemClickListener(this);
		//退出
		export = (SettingItem) findViewById(R.id.export);
		export.setOnItemClickListener(this);

		mDrawerLayout.setScrimColor(Color.TRANSPARENT);
        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
				/**
				 * @param drawerView
				 * @param slideOffset   偏移(0-1)
				 */
				@Override
				public void onDrawerSlide(View drawerView, float slideOffset) {
					View main = mDrawerLayout.getChildAt(0);

					int offset = (int) (drawerView.getWidth() * slideOffset);
					main.setTranslationX(offset);
				}

				@Override
				public void onDrawerOpened(View drawerView) {
				}

				@Override
				public void onDrawerClosed(View drawerView) {
				}

				/**
				 * 当抽屉滑动状态改变的时候被调用
				 * 状态值是STATE_IDLE（闲置-0），STATE_DRAGGING（拖拽-1），STATE_SETTLING（固定-2）中之一。
				 * 抽屉打开的时候，点击抽屉，drawer的状态就会变成STATE_DRAGGING，然后变成STATE_IDLE.
				 *
				 * @param newState
				 */
				@Override
				public void onDrawerStateChanged(int newState) {

				}
			});
	}

    @Override
    protected void setStatusBar() {
        int mStatusBarColor = getResources().getColor(R.color.colorPrimary);
        StatusBarUtils.setColorForDrawerLayout(this, (DrawerLayout) findViewById(R.id.drawer_layout), mStatusBarColor, 0);
    }

	private void startVPNService() {
		onLogReceived(null);
        if (!LocalConfig.isConfig(sp.getString(CONF_NAME,"c.conf"))) {
			Toast.makeText(this, R.string.err_invalid_url, Toast.LENGTH_SHORT).show();
			onLogReceived(getString(R.string.err_invalid_url));
			switchProxy.post(new Runnable() {
					@Override
					public void run() {
						switchProxy.setChecked(false);
						switchProxy.setEnabled(true);
					}
				});
			return;
		}
        onLogReceived("starting...");
		if(sp.getBoolean(SHADOW_SOCKS_R,false)){
			startSSR();
			return;
		}
        startService(new Intent(this, LocalVpnService.class));
    }

	void startSSR(){
		try{
			String url = sp.getString(SHADOW_SOCKS_R_URL,"");
			url = url.substring(6);
			String base = Base64Utils.decodeToString(url,enMethod);
			final String[] flag = base.split("/")[0].split(":");
			final String[] flag1 = base.split("/")[1].split("&");
			int p = 80;
			try {
				p = Integer.parseInt(flag[1]);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
			final int port = p;
			new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							final InetAddress address = InetAddress.getByName(flag[0]);
							writeLog("address: "+address.getHostAddress());
							m_Handler.post(new Runnable() {
									@Override
									public void run() {
										String xycs = flag1[1].substring(flag1[1].indexOf("=")+1);
										xycs = Base64Utils.decodeToString(xycs,enMethod);
										String hxcs = flag1[0].substring(flag1[0].indexOf("=")+1);
										hxcs = Base64Utils.decodeToString(hxcs,enMethod);
										writeLog("混淆参数: "+hxcs);
										SS_SDK.getInstance().setProfile(
											address.getHostAddress(),//服务地址
											port,//端口
											Base64Utils.decodeToString(flag[5],enMethod),//密码
											flag[3],//加密方式
											flag[2],//协议
											xycs,//协议参数
											flag[4],//混淆协议
											hxcs);//混淆参数
										SS_SDK.getInstance().switchVpn(MainActivity.this);
									}
								});
						} catch (Exception e) {
							e.printStackTrace();
							writeLog(e.toString());
						}
					}
				}).start();			
		} catch (Exception e) {
			e.printStackTrace();
			writeLog(e.toString());
		}
    }
	
	@Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if(SS_SDK.getInstance().getVPNstate()==State.CONNECTED){
			SS_SDK.getInstance().switchVpn(this);
			return;
		}
		
        if (LocalVpnService.IsRunning != isChecked) {
            switchProxy.setEnabled(false);
            if (isChecked) {
                Intent intent = LocalVpnService.prepare(this);
                if (intent == null) {
                    startVPNService();
                } else {
                    startActivityForResult(intent, START_VPN_SERVICE_REQUEST_CODE);
                }
            } else {
                LocalVpnService.IsRunning = false;
				stopService(new Intent(this, LocalVpnService.class));
            }
        }
    }
	
    @Override
    public void callback(int state) {
        switch (state) {
            case State.CONNECTING:
                //button.setText("连接中");
                break;
            case State.CONNECTED:
				switchProxy.setChecked(true);
				switchProxy.setEnabled(true);
				writeLog("ShadowsocksR已连接.");
				LocalVpnService.IsRunning = true;
				if (ToolUtils.isAndroidO())
					OnTileService.updateTileService(this,true);
                break;
            case State.STOPPED:
				switchProxy.setChecked(false);
				switchProxy.setEnabled(true);
				writeLog("ShadowsocksR已断开.");
				LocalVpnService.IsRunning = false;
				if (ToolUtils.isAndroidO())
					OnTileService.updateTileService(this,false);
                break;
            case State.STOPPING:
                //button.setText("正在停止");
                break;
        }
    }
		
	@Override
    public void onClick(View v) {
		switch (v.getId()) {
			case R.id.main_fragment:
				setupView(0);
				mViewPager.setCurrentItem(0);
				break;
			case R.id.mode_fragment:
				setupView(1);
				mViewPager.setCurrentItem(1);
				break;
			default:
		}
        if (switchProxy.isChecked()) {
            return;
        }
    }

	@Override
	public void onItemLongClick(View v) {
        switch (v.getId()) {
			case R.id.tshirt_crew:
				sp.putBoolean("bg",false);
				setBackground(null,true);//恢复默认
				break;
			case R.id.rocket:
				showConfigUrlInputDialog();
				break;
			case R.id.busy:
				showSSRUrlInputDialog();
				break;
			case R.id.dns:
				startActivity(new Intent(MainActivity.this, AppManager.class));
                break;
			case R.id.radio_tower://共享热点
				String msg = "共享热点需手动配置代理，请使用已连接热点的手机打开已连接到热点；\n\n"
					+"设置代理为（手动）：\n主机名："+ToolUtils.getWifiApIpAddress()
					+"\n端口：8787";
				if(!ToolUtils.isWifiApEnabled(this)){
					msg = "请手动打开手机热点！";
				}
				new AlertDialog.Builder(this)
					.setTitle("共享热点")
					.setCancelable(false)
					.setMessage(msg)
					.setPositiveButton("确定", null).show();
				break;
			case R.id.cloud_download:
				showUPNETWORKInputDialog();
				break;
			case R.id.package_variant:
				showCapInputDialog();
				break;
			case R.id.minus_network:
				showIPInputDialog();
				break;
		}
	}


	@Override
	public void onItemClick(View v, boolean isChecked) {
        switch (v.getId()) {
			case R.id.cellphone_android:
				break;
			case R.id.tshirt_crew:
				openAlbum();
				break;
			case R.id.speedometer:
				break;
			case R.id.rocket:
				sp.putBoolean(SHADOW_SOCKS,isChecked);
				break;
			case R.id.busy:
				sp.putBoolean(SHADOW_SOCKS_R,isChecked);
				break;
			case R.id.apple_safari:
			case R.id.github:
			case R.id.android_debug_bridge:
				//Toast.makeText(getApplicationContext(), "功能未开启！", Toast.LENGTH_SHORT).show();
				break;
			case R.id.dns:
				sp.putBoolean(INFO_BOOLEAN,isChecked);
                break;

			case R.id.radio_tower://共享热点
				sp.putBoolean(HOTSPOT,isChecked);
				if(LocalVpnService.IsRunning){
					if(isChecked){
						//关闭共享热点
						LocalVpnService.startSocketProxy();
					}else{
						//开启共享热点
						LocalVpnService.stopSocketProxy();
					}
				}else if(!LocalVpnService.IsRunning&&isChecked){
					onLogReceived("启动VPN,共享热点自动开启.");
				}
				break;
			case R.id.package_variant:
				sp.putBoolean(CAPTURE,isChecked);
				if(sp.getBoolean(CAPTURE,false)) content_copy.setRightText("抓包已开启");
				else content_copy.setRightText("抓包未开启");
				break;
			case R.id.cloud_download:
				sp.putBoolean(UPNETWORK,isChecked);
				if(!isChecked){
					return;
				}
				closeDrawer();
				if(!LocalVpnService.IsRunning){
					startVPNService();
				}
				break;
			case R.id.minus_network:
				sp.putBoolean(LOCK_IP,isChecked);
				break;
			case R.id.content_copy:
				if(TextUtils.isEmpty(LocalConfig.CapData)||
				   LocalConfig.CapData.equals(LocalConfig.Local_Conf)){
					IToast.info("应用未抓包");
				}else{
					if(cu.copy(LocalConfig.CapData)){
						IToast.success("复制抓包完毕");
					}else{
						IToast.error("复制抓包失败");
					}
				}
				break;
			case R.id.cog:
			case R.id.export:
				//Toast.makeText(getApplicationContext(), "功能未开启！", Toast.LENGTH_SHORT).show();
				//queryPersonByObjectId();
				break;
        }
	}

	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == START_VPN_SERVICE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startVPNService();
            } else {
                switchProxy.setChecked(false);
                switchProxy.setEnabled(true);
                onLogReceived("canceled.");
            }
            return;
        }
		if (requestCode == REQUEST_CHOOSEFILE) {
            if (resultCode == RESULT_OK) {
				Uri uri = intent.getData();
				String path = FileChooseUtils.getPath(this,uri);
				//Toast.makeText(MainActivity.this, path, Toast.LENGTH_SHORT).show();
				if(path.endsWith(".jpg")||path.endsWith(".png")){
					setBackground(path,true);
					sp.putBoolean(BG_DATA,true);//自定义背景
					tshirt_crew.setRightText("自定义");
				}else{
					Toast.makeText(MainActivity.this, "格式无效！", Toast.LENGTH_SHORT).show();
				}
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, intent);
    }

	@Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		switch (requestCode){
			case MY_PERMISSION_REQUEST_CODE:
				// 判断是否所有的权限都已经授予了
				for (int grant : grantResults) {
					if (grant != PackageManager.PERMISSION_GRANTED) {
						ToolUtils.openAppDetails(this);
						break;
					}
				}
				break;
        }
    }

	private void closeDrawer() {
		//关闭侧滑栏
		mDrawerLayout.closeDrawer(GravityCompat.START);
	}
	
	private void showUPNETWORKInputDialog() {
        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        editText.setHint("请输入自动更新时长…");
        editText.setText(sp.getInt(UPNETWORK_TIME,10)+"");

        new AlertDialog.Builder(this)
			.setTitle("自动更新时长(分钟)")
			.setView(editText)
			.setPositiveButton(R.string.btn_ok, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String configUrl = editText.getText().toString().trim();
					int time = -1;
					try {
						time = Integer.parseInt(configUrl);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
					if (time>0&&time<=120) {
						sp.putInt(UPNETWORK_TIME,time);
						Toast.makeText(MainActivity.this, "已保存为: "+time+"分钟", Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(MainActivity.this, "保存失败！", Toast.LENGTH_SHORT).show();
					}
				}
			})
			.setNegativeButton(R.string.btn_cancel, null)
			.show();
    }
	
	private void showCapInputDialog() {
        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        editText.setHint("请输入抓包头域…");
        editText.setText(sp.getString(CAPTURE_TY,"Q-GUID,Q-Token"));

        new AlertDialog.Builder(this)
			.setTitle("抓包头域")
			.setView(editText)
			.setPositiveButton(R.string.btn_ok, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String configUrl = editText.getText().toString().trim();					
					if (!TextUtils.isEmpty(configUrl)) {
						sp.putString(CAPTURE_TY,configUrl);
						Toast.makeText(MainActivity.this, "已保存: "+configUrl, Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(MainActivity.this, "保存失败！", Toast.LENGTH_SHORT).show();
					}
				}
			})
			.setNeutralButton("抓包应用", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					startActivity(new Intent(MainActivity.this, SingleActivity.class));
				}
			})
			.setNegativeButton(R.string.btn_cancel, null)
			.show();
    }
	
	private void showIPInputDialog() {
        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        editText.setHint("请输入锁定目标IP…");
        editText.setText(sp.getString(ON_LOCK_IP,""));

        new AlertDialog.Builder(this)
			.setTitle("锁定目标IP")
			.setView(editText)
			.setPositiveButton(R.string.btn_ok, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String configUrl = editText.getText().toString().trim();					
					if (!TextUtils.isEmpty(configUrl)) {
						sp.putString(ON_LOCK_IP,configUrl);
						Toast.makeText(MainActivity.this, "已保存: "+configUrl, Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(MainActivity.this, "保存失败！", Toast.LENGTH_SHORT).show();
					}
				}
			})
			.setNegativeButton(R.string.btn_cancel, null)
			.show();
    }
	
    private void showConfigUrlInputDialog() {
        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        editText.setHint(getString(R.string.config_url_hint));
        editText.setText(sp.getString(SHADOW_SOCKS_URL,""));

        new AlertDialog.Builder(this)
			.setTitle(R.string.config_url)
			.setView(editText)
			.setPositiveButton(R.string.btn_ok, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String configUrl = editText.getText().toString().trim();					
					if (ProxyConfig.isSsConfig(configUrl)) {
						sp.putString(SHADOW_SOCKS_URL,configUrl);
						Toast.makeText(MainActivity.this, "已保存: "+configUrl, Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(MainActivity.this, R.string.err_invalid_url, Toast.LENGTH_SHORT).show();
					}
				}
			})
			.setNegativeButton(R.string.btn_cancel, null)
			.show();
    }
	
	private void showSSRUrlInputDialog() {
        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        editText.setHint("ssr://base64…");
        editText.setText(sp.getString(SHADOW_SOCKS_R_URL,""));

        new AlertDialog.Builder(this)
			.setTitle(R.string.config_url)
			.setView(editText)
			.setPositiveButton(R.string.btn_ok, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String configUrl = editText.getText().toString().trim();					
					if (ProxyConfig.isSsrConfig(configUrl)) {
						sp.putString(SHADOW_SOCKS_R_URL,configUrl);
						Toast.makeText(MainActivity.this, "已保存: "+configUrl, Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(MainActivity.this, R.string.err_invalid_url, Toast.LENGTH_SHORT).show();
					}
				}
			})
			.setNegativeButton(R.string.btn_cancel, null)
			.show();
    }
	
	private void openAlbum() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");//选择图片
        //intent.setType("audio/*"); //选择音频
        //intent.setType("video/*"); //选择视频 （mp4 3gp 是android支持的视频格式）
        //intent.setType("video/*;image/*");//同时选择视频和图片
		//intent.setType("*/*");//无类型限制
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CHOOSEFILE);
	}

	private void setBackground(final String path, final boolean isFirst) {
		final boolean bg = sp.getBoolean("bg",false);
		new Thread(new Runnable() {
				@Override
				public void run() {
					try{

						if(path!=null||isFirst){
							handler.sendEmptyMessage(MSG_LOAD_START);
							Thread.sleep(500);
						}

						runOnUiThread(new Runnable() {
								@Override
								public void run() {
									try{
										Bitmap alertBitmap,blurBitmap;
										if(path!=null){
											Bitmap bitmap = DrawableUtils.getBitmapFromSDCard(path);
											alertBitmap = DrawableUtils.getCanvasBitmap(bitmap,"#50000000");
											//scaledBitmap为目标图像，10是缩放的倍数（越大模糊效果越高）
											blurBitmap = FastBlurUtils.toBlur(alertBitmap, 10);
											
											if(DrawableUtils.saveBitmap(MainActivity.this,alertBitmap,"m_bg.png")&&
											   DrawableUtils.saveBitmap(MainActivity.this,blurBitmap,"v_bg.png"))
												sp.putBoolean("bg",true);
										}
										if(!bg){
											sp.putBoolean(BG_DATA,false);//默认背景
											tshirt_crew.setRightText("默认");
											//获取壁纸管理器
											WallpaperManager wallpaperManager = WallpaperManager.getInstance(MainActivity.this);
											//获取壁纸图片
											Drawable wallpaperDrawable = wallpaperManager.getDrawable();

											Bitmap bitmap = DrawableUtils.getBitmap(wallpaperDrawable);
											alertBitmap = DrawableUtils.getCanvasBitmap(bitmap,"#50000000");
											//scaledBitmap为目标图像，10是缩放的倍数（越大模糊效果越高）
											blurBitmap = FastBlurUtils.toBlur(alertBitmap, 10);
											
											if(DrawableUtils.saveBitmap(MainActivity.this,alertBitmap,"m_bg.png")&&
											   DrawableUtils.saveBitmap(MainActivity.this,blurBitmap,"v_bg.png"))
												sp.putBoolean("bg",true);
										}else{
											alertBitmap = DrawableUtils.readBitmap(MainActivity.this,"m_bg.png");
											blurBitmap = DrawableUtils.readBitmap(MainActivity.this,"v_bg.png");
										}

										contentLayout.setBackground(DrawableUtils.getDrawable(alertBitmap));
										mDrawerLayout.setBackground(DrawableUtils.getDrawable(blurBitmap));

										handler.sendEmptyMessage(MSG_LOAD_FINISH);

									}catch (Exception e){
										e.printStackTrace();
									}
								}
							});
					}catch (Exception e){
						e.printStackTrace();
					}
				}
			}).start();		
	}
	
	public void FIRST_USE() {
		new AlertDialog.Builder(this)
			.setTitle("温馨提示")
			.setCancelable(false)
			.setMessage("使用本软件带来的一切责任与开发者无关！")
			.setPositiveButton(R.string.btn_ok, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					sp.putBoolean(FIRST_USE,true);
					toast("欢迎使用！");
				}
			})
			.setNegativeButton(R.string.btn_cancel,new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					System.runFinalization();
					System.exit(0);
				}
			})
			.show();
    }
	
	private void APP_USE() {
		int[] title = new int[]{35686,21578};

		int[] msg = new int[]{24403,21069,20351,29992,30340,20026,38750,27491,29256,23433,35013,21253,65292,21487,33021,23384,22312,27424,36153,21361,38505,65292,35831,19979,36733,27491,29256,20351,29992,12290};
		//int[] positive = new int[]{19979,36733,27491,29256};
		//final int[] info = new int[]{27491,22312,36339,36716,27983,35272,22120,19979,36733,27491,29256,8230};
		//final int[] uri = new int[]{104,116,116,112,115,58,47,47,103,105,116,104,117,98,46,99,111,109,47,88,105,97,111,83,104,101,110,79,76,47,86,112,110,80,114,111,120,121,47,114,97,119,47,109,97,115,116,101,114,47,86,112,110,80,114,111,120,121,49,46,48,57,46,97,112,107};

		new AlertDialog.Builder(this)
			.setTitle(ToolUtils.charToString(title))
			.setCancelable(false)
			.setMessage(ToolUtils.charToString(msg))
			/*.setPositiveButton(ToolUtils.charToString(positive), new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					toast(ToolUtils.charToString(info));
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ToolUtils.charToString(uri))));
				}
			})*/.show();
	}
	
	private void toast(String str){
		Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_actions, menu);

        MenuItem menuItem = menu.findItem(R.id.menu_item_switch);
        if (menuItem == null) {
            return false;
        }

        switchProxy = (SwitchCompat) menuItem.getActionView();
        if (switchProxy == null) {
            return false;
        }

        switchProxy.setChecked(LocalVpnService.IsRunning);
        switchProxy.setOnCheckedChangeListener(this);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_exit:
                if (!LocalVpnService.IsRunning) {
                    finish();
                    return true;
                }

                new AlertDialog.Builder(this)
					.setTitle(R.string.menu_item_exit)
					.setMessage(R.string.exit_confirm_info)
					.setPositiveButton(R.string.btn_ok, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							LocalVpnService.IsRunning = false;
							LocalVpnService.Instance.disconnectVPN();
							stopService(new Intent(MainActivity.this, LocalVpnService.class));
							System.runFinalization();
							System.exit(0);
						}
					})
					.setNegativeButton(R.string.btn_cancel, null)
					.show();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

}
