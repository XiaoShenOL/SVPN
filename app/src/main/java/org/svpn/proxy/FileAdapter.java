package org.svpn.proxy;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import org.svpn.proxy.utils.SpUtils;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ModeViewHolder> 
implements Constants {
	
	SpUtils sp;
	Context mContext;
	LayoutInflater inflater;
	List<FileInfo> modelList;
	
	public interface OnItemClickListener {
        void onItemClick(ModeViewHolder holder, View view, int position);
    }

	public interface OnItemLongClickListener {
        void onItemLongClick(ModeViewHolder holder, View view, int position);
    }

    /**
     * 点击事件回调监听
     */
    private FileAdapter.OnItemClickListener onItemClickListener;

	/**
     * 长按事件回调监听
     */
    private FileAdapter.OnItemLongClickListener onItemLongClickListener;

	/**
     * 设置点击回调监听
     *
     * @param listener
     */
    public void setOnItemClickListener(FileAdapter.OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

	/**
     * 设置长按回调监听
     *
     * @param listener
     */
    public void setOnItemLongClickListener(FileAdapter.OnItemLongClickListener listener) {
        this.onItemLongClickListener = listener;
    }
	
	public FileAdapter(Context context, List<FileInfo> list) {
		this.mContext = context;
		sp = new SpUtils(mContext);
		inflater = LayoutInflater.from(context);
		modelList = new ArrayList<>(list);
	}

	@Override
	public ModeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = inflater.inflate(R.layout.recycler_file, parent, false);
		return new ModeViewHolder(view);
	}

	@Override
	public void onBindViewHolder(final ModeViewHolder holder, int position) {
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

	public class ModeViewHolder extends RecyclerView.ViewHolder {

		TextView logo,mainText, subText;

		public ModeViewHolder(View itemView) {
			super(itemView);
			logo = (TextView) itemView.findViewById(R.id.logo);
			mainText = (TextView) itemView.findViewById(R.id.mainText);
			subText = (TextView) itemView.findViewById(R.id.subText);
		}
		
		public void bindData(FileInfo info) {
			String name = info.getFileName();
			logo.setText(new StringBuffer().append(name.toCharArray()[0]).toString().toUpperCase());
			if(sp.getString(CONF_NAME,"").equals(name)){
				logo.setBackground(mContext.getResources().getDrawable(R.drawable.ic_text_end));
			}else{
				logo.setBackground(mContext.getResources().getDrawable(R.drawable.ic_text_start));
			}
			mainText.setText(name);
			subText.setText(info.getDate());
		}
	}
}

