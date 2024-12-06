package org.svpn.proxy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.Manifest;
import android.content.pm.PackageStats;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import android.os.Handler;
import android.os.Message;

import android.graphics.Bitmap;
import java.util.concurrent.Executors;

import org.svpn.proxy.utils.ToolUtils;
import org.svpn.proxy.utils.SpUtils;
import org.svpn.proxy.utils.DrawableUtils;
import org.github.statusbar.StatusBarUtils;

public class AppManager extends BaseActivity 
implements Constants {

	private static final int MSG_LOAD_START = 1;
	private static final int MSG_LOAD_FINISH = 2;

	private SpUtils sp;
	private RecyclerView mRecyclerView;
    private AppAdapter mAdapter;
	private ArrayList<AppInfo> mAppList;
	private SwipeRefreshLayout mSwipeRefreshLayout;

	public static List<String> checkedApp = new ArrayList<>();

	final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_LOAD_START:
					if(!mSwipeRefreshLayout.isRefreshing())
					//自动刷新，也可以延时
						mSwipeRefreshLayout.post(new Runnable() {
								@Override
								public void run() {
									mSwipeRefreshLayout.setRefreshing(true);
								}
							});
					break;
				case MSG_LOAD_FINISH:
					mAdapter = new AppAdapter(AppManager.this, mAppList);
					mAdapter.setOnItemClickListener(new AppAdapter.OnItemClickListener() {
							@Override
							public void onItemClick(AppAdapter.AppViewHolder holder, View view, int position) {
								// 改变CheckBox的状态
								holder.checkBox.toggle();
								// 将CheckBox的选中状况记录下来
								AppAdapter.getIsSelected().put(position, holder.checkBox.isChecked());
								String packageName = mAppList.get(position).getPkgName();
								// 调整选定条目
								if (holder.checkBox.isChecked() == true) {
									//checkNum++;
									putData(true,packageName);
								} else {
									//checkNum--;
									putData(false,packageName);
								}
								// 通知listView刷新
								mAdapter.notifyItemChanged(position);
								dataChanged();
							}

							/*@Override
							 public void onItemLongClick(AppAdapter.AppViewHolder holder, View view, int position) {
							 Toast.makeText(AppManager.this,"long click " + position + " item", Toast.LENGTH_SHORT).show();
							 }*/
						});

					mAdapter.setOnItemLongClickListener(new AppAdapter.OnItemLongClickListener() {
							@Override
							public void onItemLongClick(AppAdapter.AppViewHolder holder, View view, int position) {
								// 改变CheckBox的状态
								holder.checkBox.toggle();
								// 将CheckBox的选中状况记录下来
								AppAdapter.getIsSelected().put(position, holder.checkBox.isChecked());
								String packageName = mAppList.get(position).getPkgName();
								// 调整选定条目
								if (holder.checkBox.isChecked() == true) {
									//checkNum++;
									putData(true,packageName);
								} else {
									//checkNum--;
									putData(false,packageName);
								}
								// 通知listView刷新
								mAdapter.notifyItemChanged(position);
								dataChanged();
							}
						});

					if(mSwipeRefreshLayout.isRefreshing())
						mSwipeRefreshLayout.setRefreshing(false);

					mRecyclerView.setAdapter(mAdapter);
					mRecyclerView.setLayoutManager(new LinearLayoutManager(AppManager.this));

					break;
			}
			super.handleMessage(msg);
		}
	};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apps);
		
		sp = new SpUtils(AppManager.this);
		
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		Bitmap mBitmap = DrawableUtils.readBitmap(AppManager.this,"m_bg.png");
		LinearLayout mLinearLayout = (LinearLayout) findViewById(R.id.apps_layout);
		mLinearLayout.setBackground(DrawableUtils.getDrawable(mBitmap));
		
		mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
		//mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent,R.color.colorAccent);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
				@Override
				public void onRefresh() {
					intiChecked();
				}
			});
		intiChecked();
    }
		
	private void putData(boolean isadd, String packageName){
		if (isadd){
			if(!checkedApp.contains(packageName))
				if(!getPackageName().contains(packageName))
					checkedApp.add(packageName);
		} else {
			if (checkedApp.contains(packageName))
				checkedApp.remove(packageName);
		}
	}

	private void intiChecked(){
		handler.sendEmptyMessage(MSG_LOAD_START);
		Executors.newSingleThreadExecutor().execute(new Runnable() {
				@Override
				public void run() {					
					if(checkedApp.size()>0)
						checkedApp.clear(); //清楚数据
					String str = sp.getString(APP_INFO);
					if(str!=null&&str.trim().length()>=0){
						if (str.contains(",")) {
							String[] arr = str.split(",");
							checkedApp.addAll(Arrays.asList(arr));
						} else if(str.trim().length()>0){
							checkedApp.add(str);
						}
						//checkNum = checkedApp.size();
						//tv_show.setText("已选中" + checkNum + "项");
					}
					initDate(); //为Adapter准备数据				
					handler.sendEmptyMessage(MSG_LOAD_FINISH);
				}
			});		
	}

    // 初始化数据
    private void initDate() {
		PackageManager pm = getPackageManager();
		ArrayList<ApplicationInfo> Applist = getAppInfo(pm);
		mAppList = new ArrayList<AppInfo>();
		int i = 0;
		for (ApplicationInfo appinfo : Applist) {
			AppInfo info= new AppInfo();
			info.setAppIcon(appinfo.loadIcon(pm));
			info.setAppLabel(appinfo.loadLabel(pm).toString());
			info.setPkgName(appinfo.packageName);
			if(checkedApp.contains(appinfo.packageName)){
				mAppList.add(i++, info);
			}else mAppList.add(info);			
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

	// 刷新listview和TextView的显示
    private void dataChanged() {
        // 通知listView刷新
        //mAdapter.notifyDataSetChanged();

        // TextView显示最新的选中数目
        //tv_show.setText("已选中" + checkNum + "项");

		if(checkedApp==null) return;
		StringBuilder sb = new StringBuilder();
		if(checkedApp.size()>=0){
			int z = checkedApp.size();
			for (int i = 0; i < z; i++) {
				if (i + 1 == z) {
					sb.append(checkedApp.get(i));					
				} else {
					sb.append(checkedApp.get(i));
					sb.append(",");
				}
			}
			sp.putString(APP_INFO,sb.toString());
		}
    };

    @Override
    protected void onPause() {
        super.onPause();
    }
	
    @Override
    protected void setStatusBar() {
		StatusBarUtils.setTransparent(this);
    }
	
    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.info_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				break;
            default:
			break;
        }
		return super.onOptionsItemSelected(item);
    }*/
}

