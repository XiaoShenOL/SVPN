package org.svpn.proxy;

import android.os.Handler; 
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import org.svpn.proxy.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.Manifest;
import android.content.pm.PackageStats;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.svpn.proxy.utils.*;

/**
 * CheckBox 单选
 *
 * Created by Tnno Wu on 2018/01/04.
 */

public class SingleActivity extends AppCompatActivity {

    private static final String TAG = SingleActivity.class.getSimpleName();

    private List<AppCap> mAppList = new ArrayList<>();

    private FloatingActionButton mFab;

    private SingleAdapter mAdapter;
	
    private SpUtils sp;
	
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single);
		sp = new SpUtils(this);
		new MyThread().start(); 
    }
	
	class MyThread extends Thread 
	{ 
		@Override 
		public void run() { 
			runOnUiThread(new Runnable() { 
					@Override 
					public void run() { 
						// TODO Auto-generated method stub 
						try { 
							initDate();
							initView();
						} catch (Exception e) { 
							e.printStackTrace(); 
						} 
					} 
				}); 
		}
	}
	
    private void initView() {
        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.rcv_single);
        mFab = (FloatingActionButton)findViewById(R.id.fab);

        mAdapter = new SingleAdapter(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(mAdapter);

        mAdapter.setDataList(mAppList);

        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取当前选中的 CheckBox 的位置
                HashMap<Integer, Boolean> checkPosition = mAdapter.getMap();
                for (int i = 0; i < mAppList.size(); i++) {
                    if (checkPosition.get(i) != null && checkPosition.get(i)) {
                        Log.d(TAG, "onClick: " + i);
						AppCap cap = mAppList.get(i);
						sp.putString("cap_package",cap.getPackageName());
                        // 模拟传值到下一页（这里用提示的形式展示）
                        Snackbar.make(mFab, "已选择抓包：" + cap.getAppName(), Snackbar.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
	
    // 初始化数据
    private void initDate() {
		PackageManager pm = getPackageManager();
		ArrayList<ApplicationInfo> Applist = getAppInfo(pm);
		mAppList = new ArrayList<AppCap>();
		int i = 0;
		for (ApplicationInfo appinfo : Applist) {
			AppCap cap= new AppCap();
			cap.setAppName(appinfo.loadLabel(pm).toString());
			cap.setPackageName(appinfo.packageName);
			mAppList.add(cap);
			/*if(checkedApp.contains(appinfo.packageName)){
				mAppList.add(i++, cap);
			}else mAppList.add(cap);*/		
        }
    }

	public static ArrayList<ApplicationInfo> getAppInfo(PackageManager packageManager){
		ArrayList<ApplicationInfo> appInfos = new ArrayList<ApplicationInfo>();
		List<PackageInfo> packageInfos = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS
																			 |PackageManager.GET_UNINSTALLED_PACKAGES);
		for(PackageInfo packageInfo : packageInfos){
			String[] premissions = packageInfo.requestedPermissions;
			if(premissions != null && premissions.length > 0){
				for(String premission : premissions){
					if(Manifest.permission.INTERNET.equals(premission)){
						ApplicationInfo applicationInfo = packageInfo.applicationInfo;
						if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) <= 0){
							appInfos.add(applicationInfo);
						}
						break;
					}
				}
			}
		}
		return appInfos;
	}
	
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
