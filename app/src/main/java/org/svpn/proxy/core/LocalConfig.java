package org.svpn.proxy.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.os.Environment;
import java.io.File;
import java.util.Calendar;
import java.util.Locale;
import java.io.FileInputStream;
import android.util.*;

import org.svpn.proxy.MainActivity;
import org.svpn.proxy.utils.*;

public class LocalConfig implements LocalConstants
{
	public static String Local_Conf="";
	public static String Base_Conf="";
	
	public static String CapData="";
	
	//判断是否需要抓包
	public static boolean isCapture(String str,String[] header){
		if(header.length>0) for(String bytes : header)
				if(!str.contains(bytes+": ")) return false;
		return true;
	}
	
	public static String getPath(){
		return dataPath()+File.separator;
	}
	
	public static String dataPath(){
		return ToolUtils.getSDPath()+File.separator+("tiny");
	}

	//识别模式
	public static boolean isConfig(String name) {
		try {
			String data = LocalConfig.get_Data(getPath()+name);
			if(LocalConfig.isTiny(data)){
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static String getMiddle(String str, String start, String end){
		String middle = "";
		try{
			if(str.contains(start)){
				int i = str.indexOf(start)+start.length();
				str = str.substring(i);
				middle = str.substring(0,str.indexOf(end));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    	return middle;
    }

	public static String getAll(String str, String start, String end){
		String all = "";
		try{
			if(str.contains(start)){
				str = str.substring(str.indexOf(start));
				all = str.substring(0,str.indexOf(end)+end.length());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    	return all;
    }

	//获取配置文件
	public static String get_Data(String Dir) throws Exception {
		FileInputStream fileInputStream = new FileInputStream(Dir);
		int length = fileInputStream.available();       
		byte[] buffer = new byte[length];        
		int read = fileInputStream.read(buffer);
		//res = EncodingUtils.getString(buffer, "UNICODE"); 
		//res = EncodingUtils.getString(buffer, "BIG5"); 
		fileInputStream.close();
		return new String(buffer, 0, read, "utf-8");
	}

	//判断是否为tiny模式
	public static boolean isTiny(String str){
		//http配置
        if(str.contains(http_ip) && str.contains(http_port)
		   && str.contains(http_del) && str.contains(http_first)
		//https配置
		   && str.contains(https_ip) && str.contains(https_port)
		   && str.contains(https_first) && str.contains(dns_url)){
			return true;
        }
        return false;
    }

	//获取NetIp
	public static String getNetIp(String str,String bytes) throws Exception {
		str = str.substring(str.indexOf("{"), str.lastIndexOf("}") + 1);
		Pattern pattern = Pattern.compile(bytes+":\'([\\s\\S]*?)\'",32);
		Matcher matcher = pattern.matcher(str);
		String string = ("");
		while (matcher.find()) {
			string = matcher.group(1);
		}
		return string;
	}

	public static int getInt(String str) throws Exception {
		try {
			return Integer.parseInt(str.replace(" ",""));
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return -1;
	}

	//获取删除String[]字段
	//line 接收输入流
	public static String[] get_String(String line) throws Exception {
		if (!TextUtils.isEmpty(line)) return line.split(",");
		return new String[]{""};
	}
	
	//获取模式的带port的ip
	public static String get_ip(String ip) throws Exception {
		ip = ip.replace(" ","");
		int index = ip.indexOf(':');
		if(index>0){
			return ip.substring(0,index);
		}
		return "";
	}

	//获取模式的带ip的port
	public static int get_port(String port) throws Exception {
		int index = port.indexOf(':');
		try {
			if((index>0)){
				return getInt(port.substring(index+1));
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return -1;
	}

	//获取tiny模式中的首头
	public static String get_tiny1(String str,String bytes) throws Exception {
		str = str.substring((str.indexOf(bytes) + bytes.length()));
		return get_bytes(str.substring(0, str.indexOf(";")));
	}

	//获取tiny模式中的首头
	public static String get_tiny2(String str,String bytes) throws Exception {
		str = str.substring((str.indexOf(bytes+"\"") + bytes.length() + 1));
		return get_bytes(str.substring(0, str.indexOf("\";")));
	}

	//获取tiny模式中的ip
	public static String get_tiny_ip(String str,String bytes) throws Exception {
		return get_tiny1(str,bytes);
	}

	//获取tiny模式中的port
	public static int get_tiny_port(String str,String bytes) throws Exception {
		try {
			return getInt(get_tiny1(str,bytes));
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return -1;
	}

	//获取tiny模式中的删除字段
	public static String get_tiny_del(String str,String bytes) throws Exception {
		return get_tiny2(str,bytes);
	}

	//获取tiny模式中的dns解析
	public static String get_tiny_dns(String str,String bytes) throws Exception {
		return get_tiny2(str,bytes);
	}


	//转换首头信息
	public static String get_first(String str) throws Exception {
		String t = (" "),tab = t+t;
		String n = ("\n"),nn = n+n;
		while(true){
			if(str.contains(nn))
				str = str.replace(nn,n);
			else if(str.contains(tab))
				str = str.replace(tab,t);
			else if(str.contains(tab+n))
				str = str.replace(tab+n,n);
			else break;
		}

		return str.replace(" \r\n", "\r\n")
			.replace("\n\r\n", "\r\n")

			.replace("\r\n\n", "\r\n")

			.replace("\r\n\\r\\n", "\\r\\n")
			.replace("\\r\\n\r\n", "\\r\\n")
			.replace(" \\r\\n", "\\r\\n")
			.replace("\n\\r\\n", "\\r\\n")

			.replace("\\r\\n\n", "\\r\\n")

			.replace("\r\n", "[rn]")
			.replace("\\r\\n", "[rn]")

			.replace(" \\n", "[n]")
			.replace("\n\\n", "[n]")
			.replace("\\n\n", "[n]")
			.replace("\\n", "[n]")
			.replace("\n", "\r\n")
			.replace("\\b", "\b")
			.replace("\\f", "\f")
			.replace("\\r", "\r")
			.replace("[rn]", "\r\n")
			.replace("[n]", "\n")
			.replace("\\0", "\0")
			.replace("\\t", "\t");
	}

	public static String get_bytes(String str) throws Exception {
		return str.replaceAll("(\\[(?i)v\\])|(\\[(?i)version\\])", V)
			.replaceAll("(\\[(?i)h\\])|(\\[(?i)host\\])", H)
			.replaceAll("(\\[(?i)url\\])|(\\[(?i)u\\])|(\\[(?i)uri\\])", U)
			.replaceAll("(\\[(?i)p\\])|(\\[(?i)port\\])|(\\[(?i)Port\\])", P)
			.replaceAll("(\\[(?i)m\\])|(\\[(?i)method\\])", M)
			.replaceAll("(\\[(?i)host_no_port\\])|(\\[(?i)host_noport\\])", H_NP);
	}
}

