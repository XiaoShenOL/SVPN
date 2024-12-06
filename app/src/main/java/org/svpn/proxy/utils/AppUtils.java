package org.svpn.proxy.utils;

import android.content.Intent;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;

/**
 * @author feicien (ithcheng@gmail.com)
 * @since 2016-07-05 17:41
 */

public class AppUtils {

	public static boolean OpenApp(Context mContext,String packname) {
		try {
			PackageManager packageManager = mContext.getPackageManager();
			if (checkPackInfo(mContext,packname)) {
				Intent intent = packageManager.getLaunchIntentForPackage(packname);
				mContext.startActivity(intent);
			}
			return true;
		} catch (Exception e) {
            e.printStackTrace();
        }
		return false;
	}
    /**
     * 检查包是否存在
     *
     * @param packname
     * @return
     */
    public static boolean checkPackInfo(Context mContext,String packname) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = mContext.getPackageManager().getPackageInfo(packname, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return packageInfo != null;
    }

    public static int getVersionCode(Context mContext) {
        if (mContext != null) {
            try {
                return mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionCode;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return 0;
    }

    public static String getVersionName(Context mContext) {
        if (mContext != null) {
            try {
                return mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return "";
    }

	/** 获取单个App名称 **/
    public static String getAppName(Context context,String packageName) throws NameNotFoundException {
		PackageManager pm = context.getPackageManager();
		ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
        return pm.getApplicationLabel(appInfo).toString();
    }

}


