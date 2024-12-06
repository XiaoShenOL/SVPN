package org.svpn.proxy.utils;

import android.os.Handler;
import java.net.InetSocketAddress;

import org.github.itoast.IToast;

import org.svpn.proxy.MainActivity;
import org.svpn.proxy.BaseContext;
import org.svpn.proxy.core.ProxyConfig;
import org.svpn.proxy.core.LocalConfig;
import org.svpn.proxy.core.LocalConstants;
import org.svpn.proxy.utils.TextUtils;

public class ConfigUtils implements LocalConstants
{	
	public static void getQQConfig() throws Exception {
		new Thread(new Runnable() {
				@Override
				public void run() {
					try{
						LocalConfig.Base_Conf = ToolUtils.getHtml();
						//MainActivity.writeLog("已获取: " + LocalConfig.Base_Conf);
						BaseContext.handler.sendEmptyMessage(2);//提示自动更新成功
					}catch (Exception e){
						e.printStackTrace();
						ProxyConfig.IS_UPNETWORK=false;
						BaseContext.handler.sendEmptyMessage(4);//提示自动更新失败
					}
				}
			}).start();		
	}
	
	public static void intiConfig() throws Exception {
		try{
			ProxyConfig.Instance.m_ProxyList.clear();
		}catch (Exception e){
			e.printStackTrace();
		}
		//tiny配置
		if(LocalConfig.isTiny(LocalConfig.Local_Conf)){
			ProxyConfig.HttpIp=LocalConfig.get_tiny_ip(LocalConfig.Local_Conf,http_ip);
			ProxyConfig.HttpPort=LocalConfig.get_tiny_port(LocalConfig.Local_Conf,http_port);
			ProxyConfig.SslIp=LocalConfig.get_tiny_ip(LocalConfig.Local_Conf,https_ip);
			ProxyConfig.SslPort=LocalConfig.get_tiny_port(LocalConfig.Local_Conf,https_port);
			
			if(ProxyConfig.IS_LOCK_IP){
				if(!TextUtils.isEmpty(ProxyConfig.GET_LOCK_IP)){
					ProxyConfig.HttpIp = ProxyConfig.GET_LOCK_IP;
					ProxyConfig.SslIp = ProxyConfig.GET_LOCK_IP;
				}
			}
			
			new Thread(new Runnable() {
					@Override
					public void run() {
						try{
							InetSocketAddress http = new InetSocketAddress(ProxyConfig.HttpIp,ProxyConfig.HttpPort);
							InetSocketAddress ssl = new InetSocketAddress(ProxyConfig.SslIp,ProxyConfig.SslPort);
							ProxyConfig.Instance.addDefaultProxy(http);
							ProxyConfig.Instance.addDefaultProxy(ssl);
						}catch (Exception e){
							e.printStackTrace();
						}
					}
				}).start();		
			
			ProxyConfig.HttpDelBytes=LocalConfig.get_tiny_del(LocalConfig.Local_Conf,http_del);
			ProxyConfig.HttpHeader=LocalConfig.get_tiny2(LocalConfig.Local_Conf,http_first);
			
			ProxyConfig.SslDelBytes=LocalConfig.get_tiny_del(LocalConfig.Local_Conf,https_del);
			ProxyConfig.SslHeader=LocalConfig.get_tiny2(LocalConfig.Local_Conf,https_first);
			
			ProxyConfig.DnsUrl=LocalConfig.get_tiny_dns(LocalConfig.Local_Conf,dns_url);
			if(ProxyConfig.IS_UPNETWORK) setQQConfig(false);
		}
	}
	
	public static void setQQConfig(boolean b) throws Exception {
		if(!TextUtils.isEmpty(LocalConfig.Base_Conf)){
			String[] s = LocalConfig.Base_Conf.split(",");
			for(int i=0;i<s.length;i++){
				ProxyConfig.HttpHeader = ProxyConfig.HttpHeader.replace(ProxyConfig.Q_GT[i],s[i]);
				ProxyConfig.SslHeader = ProxyConfig.SslHeader.replace(ProxyConfig.Q_GT[i],s[i]);
				ProxyConfig.Q_GT[i] = s[i];
				if(b){
					MainActivity.writeLog("更新"+ProxyConfig.Q_GT_1[i]+ProxyConfig.Q_GT[i]+"完毕!");
				}
			}
			if(b){
				ProxyConfig.IS_UPNETWORK=false;
				IToast.success("自动更新完毕");
				MainActivity.writeLog("自动更新完毕!\n");
			}
		}
	}
	
	public static void logConfig() throws Exception {
		MainActivity.writeLog("http_ip: %s", ProxyConfig.HttpIp);
		MainActivity.writeLog("http_port: %d", ProxyConfig.HttpPort);
		MainActivity.writeLog("http_del: %s", ProxyConfig.HttpDelBytes);
		MainActivity.writeLog("http_first: %s\n", ProxyConfig.HttpHeader);

		MainActivity.writeLog("https_ip: %s", ProxyConfig.SslIp);
		MainActivity.writeLog("https_port: %d", ProxyConfig.SslPort);
		MainActivity.writeLog("https_del: %s", ProxyConfig.SslDelBytes);
		MainActivity.writeLog("https_first: %s\n", ProxyConfig.SslHeader);

		//初始化删除头域
		ProxyConfig.HttpDel = LocalConfig.get_String(ProxyConfig.HttpDelBytes);
		ProxyConfig.SslDel = LocalConfig.get_String(ProxyConfig.SslDelBytes);
		
		ConfigUtils.setHeader();//转化首头
		//MainActivity.writeLog("Local_Conf: %s\n", LocalConfig.Local_Conf);
		ProxyConfig.HttpIp = null; ProxyConfig.SslIp = null;
		
	}

	public static void setHeader() throws Exception {
		//转化为请求首头
		String rn = ("\r\n"),n = ("\n");
		ProxyConfig.HttpHeader=LocalConfig.get_first(ProxyConfig.HttpHeader);
		if (!ProxyConfig.HttpHeader.endsWith(rn)) {
			if (!ProxyConfig.HttpHeader.endsWith(n))
				ProxyConfig.HttpHeader = (ProxyConfig.HttpHeader+(rn)).toString();
		}
		ProxyConfig.SslHeader = LocalConfig.get_first(ProxyConfig.SslHeader);
		if (!ProxyConfig.SslHeader.endsWith(rn)) {
			if (!ProxyConfig.SslHeader.endsWith(n))
				ProxyConfig.SslHeader = (ProxyConfig.SslHeader+(rn)).toString();
		}
	}
}
