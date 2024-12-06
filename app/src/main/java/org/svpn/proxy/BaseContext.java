package org.svpn.proxy;

import android.os.Message;
import android.os.Handler;
import android.content.Intent;
import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;
import android.app.Application;
import android.app.PendingIntent;

//import cn.bmob.v3.Bmob;

import org.github.itoast.IToast;

import org.svpn.proxy.core.*;
import org.svpn.proxy.utils.*;

import com.github.shadowsocks.utils.SS_SDK;

public class BaseContext extends Application {

	private static Context mContext;
	public static BaseContext Instance;
	
	private static SpUtils mSpUtils;

	public final static SpUtils getSpUtils() {
		if(mSpUtils == null){
			mSpUtils = new SpUtils(getContext());
		}
		return mSpUtils;
	}
	
	public static Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case 0://提示抓包成功
					IToast.success("应用抓包完毕");
					break;
				case 1://发送广播_执行VPN
					Intent intent1 = LocalVpnService.prepare(getContext());
					if (intent1 == null) {
						getContext().startService(new Intent(getContext(), LocalVpnService.class));
					}
					break;
				case 2://自动更新
					try{
						ConfigUtils.setQQConfig(true);
					}catch (Exception e){
						e.printStackTrace();
					}
					break;
				case 3://提示抓包失败
					IToast.error("应用抓包失败");
					break;
				case 4://提示抓包失败
					IToast.error("自动更新失败");
					break;
				case 5://提示获取成功
					IToast.success("首次获取成功");
					break;
				default:
					break;
			}
			super.handleMessage(msg);
		}
	};

	public void onCreate() {
		this.mContext = getApplicationContext();
		Instance = new BaseContext();
        SS_SDK.init(this);
		//初始化对话框
		IToast.newBuilder(this).build();
		//初始化Bmob，
		//Bmob.initialize(this, "217192dd7b4b873c71290dd2c9927927");
	}

	public static Context getContext() {
		return BaseContext.Instance.mContext;
	}

}



