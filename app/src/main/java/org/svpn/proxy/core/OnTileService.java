package org.svpn.proxy.core;

/*
 * Copyright 2017 R3BL LLC.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor
 * license agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The ASF licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
 
import android.os.Build;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.annotation.TargetApi;
import android.support.annotation.IntDef;
import android.support.annotation.RequiresApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import java.net.*;
import android.os.Handler;

import org.svpn.proxy.*;
import org.svpn.proxy.utils.Base64Utils;
import org.svpn.proxy.utils.ToolUtils;
import org.svpn.proxy.utils.StatusBarUtils;

import com.github.shadowsocks.constant.State;
import com.github.shadowsocks.utils.SS_SDK;

@IntDef({Command.INVALID, Command.STOP, Command.START})
@Retention(RetentionPolicy.SOURCE)
@interface Command {
    int INVALID = -1;
    int STOP = 0;
    int START = 1;
}

@RequiresApi(api = Build.VERSION_CODES.N)
public class OnTileService extends TileService implements Constants{
	
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateTile();
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onClick() {
        super.onClick();
		if(SS_SDK.getInstance().getVPNstate()==State.CONNECTED){
			SS_SDK.getInstance().switchVpn(this);
			LocalVpnService.IsRunning = false;
			updateTile();
			return;
		}
		if(BaseContext.getSpUtils().getBoolean(SHADOW_SOCKS_R,false)){
			startSSR();
			LocalVpnService.IsRunning = true;
			updateTile();
			return;
		}
        if (LocalVpnService.IsRunning) {
			LocalVpnService.IsRunning = false;
			stopService(new Intent(this, LocalVpnService.class));
        } else {
			StatusBarUtils.collapsingNotification(this);
			Intent intent = LocalVpnService.prepare(this);
			if (intent == null) {
				MainActivity.writeLog(null);
				MainActivity.writeLog("starting...");
				startService(new Intent(this, LocalVpnService.class));
			}
        }
    }
	
	void startSSR(){
		try{
			String url = BaseContext.getSpUtils().getString(SHADOW_SOCKS_R_URL,"");
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
			final Handler m_Handler = new Handler();
			new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							final InetAddress address = InetAddress.getByName(flag[0]);
							MainActivity.writeLog("address: "+address.getHostAddress());
							m_Handler.post(new Runnable() {
									@Override
									public void run() {
										String xycs = flag1[1].substring(flag1[1].indexOf("=")+1);
										xycs = Base64Utils.decodeToString(xycs,enMethod);
										String hxcs = flag1[0].substring(flag1[0].indexOf("=")+1);
										hxcs = Base64Utils.decodeToString(hxcs,enMethod);
										MainActivity.writeLog("混淆参数: "+hxcs);
										SS_SDK.getInstance().setProfile(
											address.getHostAddress(),//服务地址
											port,//端口
											Base64Utils.decodeToString(flag[5],enMethod),//密码
											flag[3],//加密方式
											flag[2],//协议
											xycs,//协议参数
											flag[4],//混淆协议
											hxcs);//混淆参数
										SS_SDK.getInstance().switchVpn(OnTileService.this);
									}
								});
						} catch (Exception e) {
							e.printStackTrace();
							MainActivity.writeLog(e.toString());
						}
					}
				}).start();			
		} catch (Exception e) {
			e.printStackTrace();
			MainActivity.writeLog(e.toString());
		}
    }
	
    @TargetApi(Build.VERSION_CODES.O)
    public static void updateTileService(Context context, boolean stop) {
        Intent intent = new OnIntentBuilder(context).setCommand(Command.START).build();
		if(!stop) intent = new OnIntentBuilder(context).setCommand(Command.STOP).build();
		context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        routeIntentToCommand(intent);
        return START_NOT_STICKY;
    }

    private void routeIntentToCommand(Intent intent) {
        if (intent != null) {
            if (OnIntentBuilder.containsCommand(intent)) {
				try {
					switch (OnIntentBuilder.getCommand(intent)) {
						case Command.START:
							commandStart();
							break;
						case Command.STOP:
							commandStop();
							break;
					}
				} catch (Exception e) {
				}				
            }
        }
    }

    private void commandStop() {
		stopSelf();
        updateTile();
		if(!MainActivity.IS_DEBUG){
			System.runFinalization();
			System.exit(0);
		}
    }

    private void commandStart() {
		updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile != null) {
            if (LocalVpnService.IsRunning) {
                _isRunning(tile);
            } else {
                _isNotRunning(tile);
            }
        }
        tile.updateTile();
    }

    private void _isNotRunning(Tile tile) {
        tile.setState(Tile.STATE_INACTIVE);
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_service_idle));
        tile.setLabel(getString(R.string.app_name));
    }

    private void _isRunning(Tile tile) {
		tile.setState(Tile.STATE_ACTIVE);
		tile.setIcon(Icon.createWithResource(this, R.drawable.ic_service_busy));
		tile.setLabel(getString(R.string.vpn_connected_status));
    }

	public static class OnIntentBuilder {

		private static final String KEY_COMMAND = "cmd";
		private Context mContext;
		private @Command
		int mCommandId = Command.INVALID;

		public static OnIntentBuilder getInstance(Context context) {
			return new OnIntentBuilder(context);
		}

		public OnIntentBuilder(Context context) {
			this.mContext = context;
		}

		public OnIntentBuilder setCommand(@Command int command) {
			this.mCommandId = command;
			return this;
		}

		public Intent build() {
			Intent intent = new Intent(mContext, OnTileService.class);
			if (mCommandId != Command.INVALID) {
				intent.putExtra(KEY_COMMAND, mCommandId);
			}
			return intent;
		}

		public static boolean containsCommand(Intent intent) {
			return intent.getExtras().containsKey(KEY_COMMAND);
		}


		public static @Command
		int getCommand(Intent intent) {
			final @Command int commandId = intent.getExtras().getInt(KEY_COMMAND);
			return commandId;
		}

	}
}




