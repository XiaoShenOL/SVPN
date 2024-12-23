package org.svpn.proxy.utils;

import android.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Base64编解码工具类
 * Base64.DEFAULT: 使用默认方式加密
 * Created by Song on 2017/2/22.
 */
public class Base64Utils {

    private Base64Utils() {
        throw new UnsupportedOperationException("constrontor cannot be init");
    }

    /**
     * 字符串编码
     * @param data
     * @return
     */
    public static String encodeToString(String data) {
        return Base64Utils.encodeToString(data,Base64.DEFAULT);
    }

    /**
     * 字符串编码
     * @param data
     * @return
     */
    public static String encodeToString(String data, int flag) {
        return Base64.encodeToString(data.getBytes(),flag);
    }

    /**
     * 字符串解码
     * @param data
     * @return
     */
    public static String decodeToString(String data) {
        return Base64Utils.decodeToString(data,Base64.DEFAULT);
    }

    /**
     * 字符串解码
     * @param data
     * @return
     */
    public static String decodeToString(String data, int flag) {
        return new String(Base64.decode(data,flag));
    }

    /**
     * 文件编码
     * @param filePath
     * @return
     */
    public static String encodedFile(String filePath, int flag) {
        String result = "";
        File file = new File(filePath);
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[(int)file.length()];
            fis.read(buffer);
            fis.close();
            result = Base64.encodeToString(buffer,flag);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 文件解码
     * 解码后保存到指定目录，并返回解码后的内容
     * @param filePath
     * @param data
     * @return
     */
    public static String decodedFile(String filePath,String data,int flag) {
        String result = "";
        File file = new File(filePath);
        try {

            byte[] decodeBytes = Base64.decode(data.getBytes(),flag);
            result = new String(decodeBytes);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(decodeBytes);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }
}



