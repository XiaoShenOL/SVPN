package org.svpn.proxy.utils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class ConnectUtils
{

	public static String getSuccess = ("联网成功√");
	public static String getError = ("联网失败ㄨ");

	public static String http_1 = ("Http");
	public static String https_2 = ("Https");

	public static String getHttpConnection1() {
		try{
			URL url = new URL("http://www.baidu.com/duty/index.html");
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			// 在网络异常的情况下，可能会导致程序僵死而不继续往下执行。可以通过以下两个语句来设置相应的超时：
			// 超时设置，防止 网络异常的情况下，可能会导致程序僵死而不继续往下执行
			conn.setConnectTimeout(2000);
			conn.setReadTimeout(2000);
			if(conn.getResponseCode() == HttpURLConnection.HTTP_OK)
				return http_1+getSuccess;
		}catch(Exception e){  
			e.printStackTrace();  
		}
		return http_1+getError;
	}

	private static class TrustAnyTrustManager implements X509TrustManager {

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[] {};
        }
    }

    private static class TrustAnyHostnameVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

	public static String getHttpsConnection2(){  
		try {
			URL url = new URL("https://www.tencent.com/zh-cn/zc/privacypolicyTraditional.shtml");
			HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
			if (conn instanceof HttpsURLConnection) {
				SSLContext sc = SSLContext.getInstance("SSL");
				sc.init(null, new TrustManager[] { new TrustAnyTrustManager() }, new java.security.SecureRandom());
				((HttpsURLConnection) conn).setSSLSocketFactory(sc.getSocketFactory());
				((HttpsURLConnection) conn).setHostnameVerifier(new TrustAnyHostnameVerifier());
			}
			// 在网络异常的情况下，可能会导致程序僵死而不继续往下执行。可以通过以下两个语句来设置相应的超时：
			// 超时设置，防止 网络异常的情况下，可能会导致程序僵死而不继续往下执行
			conn.setConnectTimeout(2000);
			conn.setReadTimeout(2000);
			if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK)
				return https_2+getSuccess;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return https_2+getError;
	}

}

