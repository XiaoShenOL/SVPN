package org.svpn.proxy.utils;

import java.io.File;

import java.io.InputStream;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.support.annotation.NonNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SignUtils {

	/** 获取签名的MD5摘要 */
    public static String getSignatureDigest(@NonNull Context context, @NonNull String pkgName) {
		try {
			PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES);
            if (info.signatures != null && info.signatures.length > 0) {
                Signature signature = info.signatures[0];
                MessageDigest md5 = null;
                try {
                    md5 = MessageDigest.getInstance("MD5");
                    byte[] digest = md5.digest(signature.toByteArray());
                    return toHexString(digest);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    // Should not occur
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
			// Should not occur
		}
        return "";
    }

    /** 将字节数组转化为对应的十六进制字符串 */
    private static String toHexString(byte[] rawByteArray) {
		char[] HEX_CHAR = {
			'0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
		};
        char[] chars = new char[rawByteArray.length * 2];
        for (int i = 0; i < rawByteArray.length; ++i) {
            byte b = rawByteArray[i];
            chars[i*2] = HEX_CHAR[(b >>> 4 & 0x0F)];
            chars[i*2+1] = HEX_CHAR[(b & 0x0F)];
        }
        return new String(chars);
    }

	public static String getSHA1Signature(Context context) {
        try {
			PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), 
												 PackageManager.GET_SIGNATURES);
            byte[] cert = info.signatures[0].toByteArray();
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA1");
            byte[] publicKey = md.digest(cert);
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < publicKey.length; i++) {
                String appendString = Integer.toHexString(0xFF & publicKey[i])
					.toUpperCase(java.util.Locale.US);
                if (appendString.length() == 1)
                    hexString.append("0");
				if((i+1)==publicKey.length) {
					hexString.append(appendString);
				}else{
					hexString.append(appendString);
					hexString.append(":");
				}
            }
            return hexString.toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (java.security.NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

	/* 通过apk文件获取签名 */
	private static char[] toChars(byte[] mSignature) {
		byte[] sig = mSignature;
		final int N = sig.length;
		final int N2 = N * 2;
		char[] text = new char[N2];
		for (int j = 0; j < N; j++) {
			byte v = sig[j];
			int d = (v >> 4) & 0xf;
			text[j * 2] = (char) (d >= 10 ? ('a' + d - 10) : ('0' + d));
			d = v & 0xf;
			text[j * 2 + 1] = (char) (d >= 10 ? ('a' + d - 10) : ('0' + d));
		}
		return text;
	}
	/* 通过apk文件获取签名 */
	private static Certificate[] loadCertificates(JarFile jarFile, JarEntry je, byte[] readBuffer) {
		try {
			InputStream is = jarFile.getInputStream(je);
			while (is.read(readBuffer, 0, readBuffer.length) != -1) {
			}
			is.close();
			return (Certificate[]) (je != null ? je.getCertificates() : null);
		} catch (Exception e) {
		}
		return null;
	}
	/* 通过apk文件获取签名 */
	public static String getApkSignInfo(String apkFilePath) {
		byte[] readBuffer = new byte[8192];
		Certificate[] certs = null;
		try {
			JarFile jarFile = new JarFile(apkFilePath);
			Enumeration<?> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				JarEntry je = (JarEntry) entries.nextElement();
				if (je.isDirectory()) {
					continue;
				}
				if (je.getName().startsWith("META-INF"+File.separator)) {
					continue;
				}
				Certificate[] localCerts = loadCertificates(jarFile, je, readBuffer);
				if (certs == null) {
					certs = localCerts;
				} else {
					for (int i = 0; i < certs.length; i++) {
						boolean found = false;
						for (int j = 0; j < localCerts.length; j++) {
							if (certs[i] != null && certs[i].equals(localCerts[j])) {
								found = true;
								break;
							}
						}
						if (!found || certs.length != localCerts.length) {
							jarFile.close();
							return null;
						}
					}
				}
			}
			jarFile.close();
			return new String(toChars(certs[0].getEncoded()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}


