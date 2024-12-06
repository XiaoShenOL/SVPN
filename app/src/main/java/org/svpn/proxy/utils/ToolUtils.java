package org.svpn.proxy.utils;

import java.util.ArrayList;
import java.util.List;
import android.net.Uri;
import java.io.File;
import java.text.DecimalFormat;
import android.util.Base64;
import android.content.Context;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.Manifest;
import android.os.Build;
import android.os.Environment;
import android.app.AlertDialog;
import android.provider.Settings;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageStats;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.lang.reflect.*;
import android.net.wifi.WifiManager;

import java.io.*;
import java.net.*;

public class ToolUtils
{
	
    public static String getHtml() throws Exception
    {
        URL url = new URL("http://helper.vtop.design/android_connect/aaa.php");
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setConnectTimeout(2000);
		conn.setReadTimeout(2000);
        InputStream inStream =  conn.getInputStream();
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		byte[]  buffer = new byte[1204];
		int len = 0;
		while ((len = inStream.read(buffer)) != -1)
		{
			outStream.write(buffer,0,len);
		}
		inStream.close();
        byte[] data = outStream.toByteArray();
        String html = new String(data);
        return html;
    }
	
	/**
     * 检查是否开启Wifi热点
     * @return
     */
    public static boolean isWifiApEnabled(Context context){
        try {
			// 取得WifiManager对象
			WifiManager mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            Method method=mWifiManager.getClass().getMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (boolean) method.invoke(mWifiManager);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }
	
	public static String getWifiApIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
					 .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                if (intf.getName().contains("wlan")) {
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress() && (inetAddress.getAddress().length == 4)) {
                            //Log.d("Main", inetAddress.getHostAddress());
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException ex) {
        }
        return null;
    }
	
	//字符格式化，为保留小数做准备
	private static final DecimalFormat df = new DecimalFormat("#.##");
	
	/**
     * 大小转换
     * @param size
     * @return
     */
    public static String get_Szie(long size){
		if (size < 1024)
			return df.format((double) size ) + " Bytes";
		else if (size < 1048576)
			return df.format((double) size / 1024 ) + " KB";
		else if (size < 1073741824)
			return df.format((double) size / 1048576 ) + " MB";
		else
			return df.format((double) size / 1073741824 ) + " GB";
    }
	
	public static boolean isAppUse(Context context){
		PackageInfo info = getPackageInfo(context);
		File f = new File(info.applicationInfo.sourceDir);
		String sign = SignUtils.getApkSignInfo(f.getAbsolutePath());
		sign = getAlphabet(sign);
		String base64 = ToString(org.svpn.proxy.Constants.BASE_64);
		if(sign.contains(new String(Base64.decode(base64,Base64.DEFAULT))))
			return true;
		return false;
	}

	public static PackageInfo getPackageInfo(Context context){
		PackageManager pm = null;
		PackageInfo info = null;
		try{
			pm = context.getPackageManager();
			info = pm.getPackageInfo(context.getPackageName(), 
									 PackageManager.GET_SIGNATURES);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return info;
	}
	/**
	 *将数学转换成文字
	 **/
	public static String charToString(String[] string) {
		StringBuffer sb = new StringBuffer().append("");
		try {
			if(string.length>0)
				for(String s : string){
					sb.append((char)Integer.parseInt(s));
				}
		} catch (Exception e) {
			e.printStackTrace();
		}
        return sb.toString();
    }

	/**
	 *将数学转换成文字
	 **/
	public static String charToString(int[] i) {
		StringBuffer sb = new StringBuffer().append("");
		try {
			if(i.length>0)
				for(int c : i){
					sb.append((char)c);
				}
		} catch (Exception e) {
			e.printStackTrace();
		}
        return sb.toString();
    }

	/**
	 *将集合合并
	 **/
	public static String ToString(String[] string) {
		StringBuffer sb = new StringBuffer().append("");
		try {
			if(string.length>0)
				for(String s : string){
					sb.append(s);
				}
		} catch (Exception e) {
			e.printStackTrace();
		}
        return sb.toString();
    }

	public static int getInt(String str){
		try {
			StringBuffer sb = new StringBuffer(); 
			String sp = "[0-9]";
			Pattern  pattern=Pattern.compile(sp);  
			Matcher  m = pattern.matcher(str);  
			while(m.find()){  
				sb.append(m.group());  
			}
			return Integer.parseInt(sb.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
    }

	public static String getAlphabet(String str){
		StringBuffer sb = new StringBuffer(); 
        String s = "[a-zA-Z]";
        Pattern  pattern=Pattern.compile(s);  
        Matcher  m = pattern.matcher(str);  
		while(m.find()){  
			sb.append(m.group());  
        }
		return sb.toString();
    }
	
	public static boolean isAndroidO() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);
    }
	
	/**
	 * 获取SD卡路径
	 * @return
	 */
	public static String getSDPath() {
		// 判断sd卡是否存在
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
		// 获取根目录
			return Environment.getExternalStorageDirectory().toString();
		else
			return Environment.getDataDirectory().toString();
	}

	
	/**
     * 检查是否拥有指定的所有权限
     */
    public static boolean checkPermissionAllGranted(Context context, String[] permissions) {
        for (String permission : permissions) {
			int i = ContextCompat.checkSelfPermission(context, permission);
            if (i == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else if (i == PackageManager.PERMISSION_DENIED) {
				return false;
			}
        }
        return true;
    }
	
	/**
     * 打开 APP 的详情设置
     */
    public static void openAppDetails(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("应用需要访问“外部存储器”权限，是否授予权限。");
        builder.setPositiveButton("授权", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent();
					intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
					intent.addCategory(Intent.CATEGORY_DEFAULT);
					intent.setData(Uri.parse("package:" + context.getPackageName()));
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
					intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
					context.startActivity(intent);
				}
			});
        builder.setNegativeButton("取消", null);
        builder.show();
    }
	
	public static int getVersionCode(Context mContext) {
        if (mContext != null) {
            try {
                return mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionCode;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return -1;
    }

    public static String getVersionName(Context mContext) {
        if (mContext != null) {
            try {
                return mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return null;
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
	
}
