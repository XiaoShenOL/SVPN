package org.svpn.proxy.core;

import android.widget.Toast;
import android.content.Intent;
import android.content.Context;
import android.content.BroadcastReceiver;

import org.svpn.proxy.utils.*;

public class LocalReceivers extends BroadcastReceiver {
	
	public static final String FLAG_ID = "FLAG";
	
	public static final String PACKAGE_NAME = "org.vpn.proxy.core";
	public static final String PLAY_BROADCAST_NAME = PACKAGE_NAME+".play.broadcast";
    public static final String PAUSE_BROADCAST_NAME = PACKAGE_NAME+".pause.broadcast";
	
	public static final int PAUSE_FLAG = 0x1;
    public static final int PLAY_FLAG = 0x2;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		switch (intent.getIntExtra(FLAG_ID, -1)) {
			case PAUSE_FLAG:
				ProxyConfig.IS_CAPTURE = true;
				AppUtils.OpenApp(context, ProxyConfig.CapPackage);
				StatusBarUtils.collapsingNotification(context);
				//toast(context,"抓包已开启，请重新抓包!");
				break;
			case PLAY_FLAG:
				ProxyConfig.IS_UPNETWORK=true;
				//Toast.makeText(context, "NEXT_FLAG", Toast.LENGTH_SHORT).show();
				StatusBarUtils.collapsingNotification(context);
				try{
					ConfigUtils.getQQConfig();
				}catch (Exception e){
					e.printStackTrace();
				}
				break;
		}
	}
	
	public void toast(Context context,String str) {
		Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
	}
}
