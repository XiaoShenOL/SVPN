package org.svpn.proxy;

import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.Handler;

import android.content.Intent;

import android.annotation.SuppressLint;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Calendar;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.text.TextUtils;

import org.svpn.proxy.utils.*;

public class MainFragment extends Fragment
implements View.OnClickListener, MainActivity.onStatusChangedListener {

    private View mRootView;

    private ScrollView scrollViewLog;
	private Calendar mCalendar;

	private TextView textViewLog,app_ver,android_model,android_sdk,android_ver,
	apn,network,send,received,http,https;

	final int tp1=0,ps2=1,connect=3;
	String http1=null,https2=null;

	final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case tp1:
					http.setText(http1);
					break;
				case ps2:
					https.setText(https2);
					break;
				case connect:
					http.setText("测试Http联网…");
					https.setText("测试Https联网…");
					break;
				default:
					http.setText("当前网络不可用！");
					https.setText("请手动连接网络！");
					break;
			}
			super.handleMessage(msg);
		}
	};
	
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mRootView == null){
            mRootView = inflater.inflate(R.layout.main_fragment,container,false);

			scrollViewLog = (ScrollView) mRootView.findViewById(R.id.scrollViewLog);
			textViewLog = (TextView) mRootView.findViewById(R.id.textViewLog);
			scrollViewLog.fullScroll(ScrollView.FOCUS_DOWN);
			mCalendar = Calendar.getInstance();
			MainActivity.addOnStatusChangedListener(this);

			app_ver = (TextView) mRootView.findViewById(R.id.app_ver);
			android_model = (TextView) mRootView.findViewById(R.id.android_model);
			android_sdk = (TextView) mRootView.findViewById(R.id.android_sdk);
			android_ver = (TextView) mRootView.findViewById(R.id.android_ver);

			send = (TextView) mRootView.findViewById(R.id.send);
			received = (TextView) mRootView.findViewById(R.id.received);

			app_ver.setText("应用版本: "+ToolUtils.getVersionName(getActivity()));
			android_model.setText("手机型号: "+Build.MODEL);
			android_sdk.setText("安卓SDK: "+Build.VERSION.SDK_INT);
			android_ver.setText("安卓版本: "+Build.VERSION.RELEASE);

			http = (TextView) mRootView.findViewById(R.id.http);
			https = (TextView) mRootView.findViewById(R.id.https);
			
			apn = (TextView) mRootView.findViewById(R.id.apn);
			network = (TextView) mRootView.findViewById(R.id.network);
			apn.setOnClickListener(this);
			network.setOnClickListener(this);
			//MainActivity.writeLog(BmobUtils.query_0());
        }

        ViewGroup parent = (ViewGroup) mRootView.getParent();
        if (parent != null){
            parent.removeView(mRootView);
        }
        return mRootView;
    }
	
	@Override
    public void onClick(View v) {
		switch (v.getId()) {
			case R.id.apn:
				try{
					Intent intent = new Intent(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS);
					getActivity().startActivity(intent);
				}catch (Exception e){
					e.printStackTrace();
					onLogReceived(e.toString());
				}
				break;
			case R.id.network:
				NET_CONNECT();
				break;
			default:
		}
    }
	
	@SuppressLint("DefaultLocale")
    @Override
    public void onLogReceived(String logStr) {
        mCalendar.setTimeInMillis(System.currentTimeMillis());
        String logString = String.format("[%1$02d:%2$02d:%3$02d] %4$s\n",
										 mCalendar.get(Calendar.HOUR_OF_DAY),
										 mCalendar.get(Calendar.MINUTE),
										 mCalendar.get(Calendar.SECOND),
										 logStr);

        System.out.println(logString);

        if (TextUtils.isEmpty(logStr)||textViewLog.getLineCount() > 200) {
            textViewLog.setText("");
        }else{
			textViewLog.append(logString);
			scrollViewLog.fullScroll(ScrollView.FOCUS_DOWN);
		}
    }

	@Override
    public void onStatusChanged(String status, Boolean isRunning) {
        onLogReceived(status);
		if(isRunning) NET_CONNECT();
    }

	@Override
	public void onNetworkTraffic(String SentBytes, String ReceivedBytes)
	{
		send.setText(SentBytes);
		received.setText(ReceivedBytes);
		// TODO: Implement this method
	}

	private void NET_CONNECT(){
		new Thread(new Runnable() {
				@Override
				public void run() {
					try{
						if(!NetUtils.isConnected(getActivity())){
							handler.sendEmptyMessage(-1);
							MainActivity.writeLog("未检测到可用网络！");
							return;
						}
						handler.sendEmptyMessage(connect);
						http1=ConnectUtils.getHttpConnection1();
						handler.sendEmptyMessage(tp1);
						https2=ConnectUtils.getHttpsConnection2();
						handler.sendEmptyMessage(ps2);
						String error = ConnectUtils.getError;
						if(http1.contains(error)||https2.contains(error)){
							MainActivity.writeLog("上网可能有限,请自行打开网页测试！！！");
						}else{
							MainActivity.writeLog("恭喜您！网络畅通！");
						}
					}catch (Exception e){
						e.printStackTrace();
						onLogReceived(e.toString());
					}					
				}
			}).start();
	}

	
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		onLogReceived("Welcome to SVPN！");
		// TODO: Implement this method
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onDestroyView()
	{
		MainActivity.removeOnStatusChangedListener(this);
		// TODO: Implement this method
		super.onDestroyView();
	}	
}
