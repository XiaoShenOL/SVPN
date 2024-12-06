package org.svpn.proxy;

public class AppCap
{
	private String app_name;
	private String package_name;
	
	public String getAppName(){
		return this.app_name;
	}
	
	public void setAppName(String app_name){
		this.app_name = app_name;
	}
	
	public String getPackageName(){
		return this.package_name;
	}
	
	public void setPackageName(String package_name){
		this.package_name = package_name;
	}
	
}
