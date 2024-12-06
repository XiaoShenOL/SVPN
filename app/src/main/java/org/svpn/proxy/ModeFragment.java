package org.svpn.proxy;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.LinearLayoutManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;

import android.widget.TextView;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import android.Manifest;
import java.io.File;
import android.content.pm.PackageStats;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.svpn.proxy.utils.ToolUtils;
import org.svpn.proxy.utils.SpUtils;
import org.svpn.proxy.core.LocalConfig;

public class ModeFragment extends Fragment 
implements Constants {

    private View mRootView;
	private SpUtils sp;
	RecyclerView mRecyclerView;
    FileAdapter mAdapter;
	private List<FileInfo> mAppList;
	public static List<String> checkedApp = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mRootView == null){
            mRootView = inflater.inflate(R.layout.mode_fragment,container,false);

			sp = new SpUtils(getActivity());

			mRecyclerView = (RecyclerView) mRootView.findViewById(R.id.recycler_view);
			mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

			mAppList = getFileList(LocalConfig.dataPath());
			mAdapter = new FileAdapter(getActivity(), mAppList);

			mAdapter.setOnItemClickListener(new FileAdapter.OnItemClickListener() {
					@Override
					public void onItemClick(FileAdapter.ModeViewHolder holder, View view, int position) {
						FileInfo fi = mAppList.get(position);
						String name = fi.getFileName();
						sp.putString(CONF_NAME,name);
						for(int i=0;i<mAppList.size();i++){
							// 通知listView刷新
							mAdapter.notifyItemChanged(i);
						}
						MainActivity.writeLog("已选取: "+name);
					}
				});

			/*mAdapter.setOnItemLongClickListener(new FileAdapter.OnItemLongClickListener() {
					@Override
					public void onItemLongClick(FileAdapter.ModeViewHolder holder, View view, int position) {
						MainActivity.writeLog("onItemLongClick");
					}
				});*/

			mRecyclerView.setAdapter(mAdapter);

        }
        ViewGroup parent = (ViewGroup) mRootView.getParent();
        if (parent != null){
            parent.removeView(mRootView);
        }
        return mRootView;
    }

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		// TODO: Implement this method
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onDestroyView()
	{
		// TODO: Implement this method
		super.onDestroyView();
	}

	/**
     * 返回指定目录下的文件
     * @param strPath 目录
     * @return 当前文件夹下的文件
     */
	public List<FileInfo> getFileList(String filePath) {
		List<FileInfo> list = new ArrayList<>();
		try{
			File f = new File(filePath);
			if(!f.exists()) f.mkdir();
			File[] files = f.listFiles();// 列出所有文件  
			// 将所有文件存入list中  
			if(files != null){  
				for (File file : files) {
					if(file.isFile()&&!file.isDirectory())
						if(file.getName().endsWith(".conf")){
							if(LocalConfig.isConfig(file.getName())){
								SimpleDateFormat sdate = new SimpleDateFormat("yy/MM/dd", Locale.getDefault());
								SimpleDateFormat stime = new SimpleDateFormat("aa hh:mm", Locale.getDefault());
								Date date = new Date(file.lastModified());

								FileInfo info = new FileInfo();
								info.setFileName(file.getName());
								info.setDate(("Last edited: ")+sdate.format(date)+(", ")+stime.format(date));
								list.add(info);
							}
						}
				}  
			}  
		}catch(Exception ex){  
			ex.printStackTrace();  
		}
		return list;
	}	
}
