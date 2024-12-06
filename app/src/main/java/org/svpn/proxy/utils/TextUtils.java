package org.svpn.proxy.utils;

public class TextUtils
{
	//判断内容是否为空
	public static boolean isEmpty(String s){
		if(s==null||s.isEmpty()||s.equals("null"))
			return true;
		return false;
	}

}

