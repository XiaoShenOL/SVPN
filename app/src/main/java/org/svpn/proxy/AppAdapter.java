package org.svpn.proxy;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;
import android.widget.ImageView;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;
import android.widget.CheckBox;
import java.util.HashMap;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> {
	Context mContext;
	LayoutInflater inflater;
	List<AppInfo> modelList;
    // 用来控制CheckBox的选中状况
    private static HashMap<Integer,Boolean> isSelected;
	
	public interface OnItemClickListener {
        void onItemClick(AppViewHolder holder, View view, int position);
    }
	
	public interface OnItemLongClickListener {
        void onItemLongClick(AppViewHolder holder, View view, int position);
    }
	
    /**
     * 点击事件回调监听
     */
    private AppAdapter.OnItemClickListener onItemClickListener;
	
	/**
     * 长按事件回调监听
     */
    private AppAdapter.OnItemLongClickListener onItemLongClickListener;
	
	/**
     * 设置点击回调监听
     *
     * @param listener
     */
    public void setOnItemClickListener(AppAdapter.OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
	
	/**
     * 设置长按回调监听
     *
     * @param listener
     */
    public void setOnItemLongClickListener(AppAdapter.OnItemLongClickListener listener) {
        this.onItemLongClickListener = listener;
    }
	
	public AppAdapter(Context context, List<AppInfo> list) {
		this.mContext = context;
		inflater = LayoutInflater.from(context);
		modelList = new ArrayList<>(list);
		isSelected = new HashMap<Integer, Boolean>();
        // 初始化数据
        initDate();
	}
	
    // 初始化isSelected的数据
    private void initDate(){
        for(int i=0; i<modelList.size();i++) {
			if(AppManager.checkedApp.contains(modelList.get(i).getPkgName()))
				getIsSelected().put(i,true);
			else getIsSelected().put(i,false);
        }
    }
	
    public static HashMap<Integer,Boolean> getIsSelected() {
        return isSelected;
    }

    public static void setIsSelected(HashMap<Integer,Boolean> isSelected) {
        AppAdapter.isSelected = isSelected;
    }
	
	@Override
	public AppViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = inflater.inflate(R.layout.recycler_app, parent, false);
		return new AppViewHolder(view);
	}

	@Override
	public void onBindViewHolder(final AppViewHolder holder, int position) {
		holder.bindData(modelList.get(position));
		holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(final View v) {
					if(onItemClickListener != null) {
						int pos = holder.getLayoutPosition();
						onItemClickListener.onItemClick(holder,holder.itemView, pos);
					}
				}
			});

		holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					if(onItemLongClickListener != null) {
						int pos = holder.getLayoutPosition();
						onItemLongClickListener.onItemLongClick(holder,holder.itemView, pos);
					}
					//表示此事件已经消费，不会触发单击事件
					return true;
				}
			});
	}

	@Override
	public int getItemCount() {
		return modelList.size();
	}

	public class AppViewHolder extends RecyclerView.ViewHolder {

		ImageView icon;
		CheckBox checkBox;
		TextView mainText, subText;

		public AppViewHolder(final View itemView) {
			super(itemView);
			icon = (ImageView) itemView.findViewById(R.id.app_icon);
			checkBox = (CheckBox) itemView.findViewById(R.id.checkBox);
			mainText = (TextView) itemView.findViewById(R.id.mainText);
			subText = (TextView) itemView.findViewById(R.id.subText);
		}

		public void bindData(AppInfo info) {
			icon.setImageDrawable(info.getAppIcon());
			mainText.setText(info.getAppLabel());
			subText.setText(info.getPkgName());

			if(info.getPkgName().equals(mContext.getPackageName())){
				checkBox.setVisibility(View.GONE);
			}else if(AppManager.checkedApp.contains(info.getPkgName())){
				checkBox.setVisibility(View.VISIBLE);
				checkBox.setChecked(true);
			}else {
				checkBox.setVisibility(View.VISIBLE);
				checkBox.setChecked(false);
			}
		}
	}	
}

