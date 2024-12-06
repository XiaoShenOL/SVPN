package org.svpn.proxy.utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;

import android.content.Context;
import android.app.WallpaperManager;

import java.io.File;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class DrawableUtils
{
	public static Bitmap getBitmap(Drawable db) {
		BitmapDrawable bd = (BitmapDrawable) db;
		return getBitmap(bd);
	}

	public static Bitmap getBitmap(BitmapDrawable bd) {
		Bitmap bm = bd.getBitmap();
		return bm;
	}

	public static Drawable getDrawable(Bitmap bm) {
		BitmapDrawable bd = new BitmapDrawable(bm);
		return bd;
	}

	public static Drawable getDrawable(BitmapDrawable bm) {
		return bm;
	}

	/**
     * 保存Bitmap图片到指定文件
     *
     * @param bm
     */
    public static Bitmap readBitmap(Context context, String fileName) {
        try {
            FileInputStream in = context.openFileInput(fileName);
			return BitmapFactory.decodeStream(in);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
		return DrawableUtils.getWallpaper(context);
    }

    public static Bitmap readBitmap1(Context context, String path) {
        try {
            FileInputStream in = new FileInputStream(new File(path));
			return BitmapFactory.decodeStream((InputStream)in);
        } catch (IOException e) {
            e.printStackTrace();
        }
		return DrawableUtils.getWallpaper(context);
    }

	public static Bitmap getWallpaper(Context context) {
		//获取壁纸管理器
		WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
		//获取壁纸图片
		Drawable wallpaperDrawable = wallpaperManager.getDrawable();
		return DrawableUtils.getBitmap(wallpaperDrawable);
	}

	/**
     * 保存Bitmap图片到指定文件
     *
     * @param bm
     */
    public static boolean saveBitmap(Context context, Bitmap bm, String fileName) {
        try {
            FileOutputStream out = context.openFileOutput(fileName, 0);
            bm.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

	/**从sd卡中获取图片的bitmap对象*/
	public static Bitmap getBitmapFromSDCard(String path) {
		Bitmap bitmap = null;
		try {
			FileInputStream fileInputStream = new FileInputStream(path);
			if(fileInputStream != null) {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = 2; //当图片资源太大的适合，会出现内存溢出。图片宽高都为原来的二分之一，即图片为原来的四分一
				bitmap = BitmapFactory.decodeStream((InputStream)fileInputStream, null, options);
			}
		} catch(Exception e) {
			return null;
		}

		return bitmap;
	}

	public static Bitmap getCanvasBitmap(Bitmap bitmap, String color) {
		Bitmap alertBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
		Canvas canvas=new Canvas(alertBitmap);
		Paint paint=new Paint();
		paint.setColor(Color.BLACK);
		canvas.drawBitmap(bitmap, new Matrix(), paint);

		Bitmap alertBitmap1 = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),bitmap.getConfig());
		int width = alertBitmap1.getWidth();
		int height = alertBitmap1.getHeight();

		int i = Color.parseColor(color);
		for (int w = 0; w < width; w++) {
			for (int h = 0; h < height; h++) {
				alertBitmap1.setPixel(w, h, i);
			}
		}
		canvas.drawBitmap(alertBitmap1, new Matrix(), paint);
		return alertBitmap;
	}
}
