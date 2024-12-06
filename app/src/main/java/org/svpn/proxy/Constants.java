package org.svpn.proxy;

import android.util.Base64;

public interface Constants {
	
	//base64加密方式
	public static int enMethod = Base64.URL_SAFE | Base64.NO_WRAP;
	
	//初次使用
	public static String FIRST_USE = "first_use";
	
	//应用放行
	public static String APP_INFO = "app_info";
	public static String INFO_BOOLEAN = "info_boolean";
	
	//配置名称
	public static String CONF_NAME = "conf_name";
	
	//背景
	public static String BG_DATA = "bg_data";
	
	//自动更新配置
	public static String UPNETWORK = "upnetwork";
	public static String UPNETWORK_TIME = "upnetwork_time";
	
	//应用抓包
	public static String CAPTURE = "capture";
	//应用头域
	public static String CAPTURE_TY = "capture_ty";
	
	//Shadowsocks
	public static String SHADOW_SOCKS = "shadowsocks";
	public static String SHADOW_SOCKS_URL = "shadowsocks_url";
	
	//ShadowsocksR
	public static String SHADOW_SOCKS_R = "shadowsocksr";
	public static String SHADOW_SOCKS_R_URL = "shadowsocksr_url";
	
	//共享热点
	public static String HOTSPOT = "hotspot";
	
	//锁定目标ip
	public static String LOCK_IP = "lock_ip";
	public static String ON_LOCK_IP = "on_lock_ip";
	
	//签名
	public static String[] BASE_64 = new String[]{"YWRhZGFmZGJjY2VhZWJlYWVlYWFiZGFmYWNjZWFlYmVhZWVhYWJmZGFmZGRhZmRhZmZlZGNkZGNk\n",
		"Y2ZhZGVhZWRmZmZhYWZlZmNlY2JlZWVhZWFhZmJkZGJkY2VjY2NjY2FkYWZhZWNiYmFjY2ZlZWVl\n",
		"ZmZjYWZiZmFjY2JjYWFhZmRkYWZkYmVmYmFjZmNiYmFmZmNkZWJkZmZlY2NiZWZkZWFiYWNlZGRh\n",
		"ZGFiY2ZjZmZmYWJmYWVlZmFhZGJiZGFjZWZkZGVkY2FmYmZiY2NmYmFiZWZlYWRhYmFlZmY=\n"};
	
	public static String mUpdateUrl = "https://raw.githubusercontent.com/WVector/AppUpdateDemo/master/json/json.txt";
	public static String mUpdateUrl1 = "https://raw.githubusercontent.com/WVector/AppUpdateDemo/master/json/json1.txt";
	
}
