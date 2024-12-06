package org.svpn.proxy.utils;

import android.content.Context;
import android.content.ClipData;
import android.content.ClipboardManager;

public class ClipboardUtils
{
	private Context mContext;
	private ClipboardManager mClipboard;
	
	public ClipboardUtils(Context context)
	{
		this.mContext = context;
		// 获取系统剪贴板
		mClipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
	}
	
	// 检查剪贴板是否有内容
	public boolean hasPrimaryClip()
	{
		return mClipboard.hasPrimaryClip();
	}
	
	public boolean copy(String text)
	{
		if(TextUtils.isEmpty(text))
			return false;
			
		try{
			// 创建一个剪贴数据集，包含一个普通文本数据条目（需要复制的数据）
			ClipData clipData = ClipData.newPlainText(null, text);
			// 把数据集设置（复制）到剪贴板
			mClipboard.setPrimaryClip(clipData);
			return true;
		}catch (Exception e){
			e.printStackTrace();
		}
		return false;
	}
	
	public String getText()
	{
		return getCharSequence().toString();
	}
	
	public CharSequence getCharSequence()
	{
		// 获取剪贴板的剪贴数据集
		ClipData clipData = mClipboard.getPrimaryClip();

		if (clipData != null && clipData.getItemCount() > 0) {
			// 从数据集中获取（粘贴）第一条文本数据
			CharSequence text = clipData.getItemAt(0).getText();
			return text;
		}
		return null;
	}
}
