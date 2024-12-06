package com.github.shadowsocks;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import android.content.Context;

/**
 * Created by victor on 2017/4/7.
 */

public class TrafficMonitorThread extends Thread {
    String PATH ;
    public  boolean isRunning = true;
    public LocalServerSocket serverSocket;

    public TrafficMonitorThread(Context mContext) {
        PATH = mContext.getApplicationInfo().dataDir + "/stat_path";
    }

    private void closeServerSocket() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            serverSocket = null;
        }
    }

    public void stopThread() {
        isRunning = false;
        closeServerSocket();
    }

    @Override
    public void run() {
        new File(PATH).delete();
        try {
            LocalSocket localSocket = new LocalSocket();
            localSocket.bind(new LocalSocketAddress(PATH,LocalSocketAddress.Namespace.FILESYSTEM));
            serverSocket = new LocalServerSocket(localSocket.getFileDescriptor());

        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        ExecutorService pool = Executors.newFixedThreadPool(1);
        while (isRunning) {
            try {
                final LocalSocket socket = serverSocket.accept();
                pool.execute(new Runnable() {
						@Override
						public void run() {
							try {
								InputStream input = socket.getInputStream();
								OutputStream output = socket.getOutputStream();
								
								byte[] buffer = new byte[16];
								if (input.read(buffer) != 16)
									throw new IOException("Unexpected traffic stat length.");
								ByteBuffer stat = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
								long out = stat.getLong(0);
								long in = stat.getLong(8);
								
								output.write(0);
								
								input.close();
								output.close();

							} catch (Exception e) {
								e.printStackTrace();
							}
							try {
								socket.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					});
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

        }

    }
}
