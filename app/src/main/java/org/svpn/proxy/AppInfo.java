package org.svpn.proxy;

import android.graphics.drawable.Drawable;

public class AppInfo {
	
	public static final int TYPE_USER = 3;
    public static final int TYPE_UPDATED_SYSTEM = 2;
    public static final int TYPE_SYSTEM = 1;
	
    private Drawable appIcon;
    private String appLabel;
    private String pkgName;
	
    public Drawable getAppIcon() {
        return this.appIcon;
    }

    public String getAppLabel() {
        return this.appLabel;
    }

    public String getPkgName() {
        return this.pkgName;
    }
	
    public void setAppIcon(Drawable appIcon) {
        this.appIcon = appIcon;
    }

    public void setAppLabel(String appLabel) {
        this.appLabel = appLabel;
    }

    public void setPkgName(String pkgName) {
        this.pkgName = pkgName;
    }
		
}

