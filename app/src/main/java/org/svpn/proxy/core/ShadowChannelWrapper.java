package org.svpn.proxy.core;

import java.net.InetSocketAddress;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import android.annotation.SuppressLint;

import org.svpn.proxy.ss.*;
import org.svpn.proxy.MainActivity;

public class ShadowChannelWrapper {

    final static ByteBuffer GL_BUFFER = ByteBuffer.allocate(20000);
    public static long SessionCount;

    private SocketChannel m_InnerChannel;
    private ByteBuffer m_SendRemainBuffer;
    private Selector m_Selector;
	ShadowChannelWrapper m_BrotherTunnel;
    private boolean m_Disposed;
	boolean m_UseProxy;

    protected InetSocketAddress m_DestAddress;

    ICrypt m_Encryptor;
    boolean m_TunnelEstablished;

    public ShadowChannelWrapper(SocketChannel innerChannel, Selector selector)
	throws Exception {
		m_Encryptor = CryptFactory.get(ProxyConfig.EncryptMethod, ProxyConfig.Password);
        this.m_InnerChannel = innerChannel;
        this.m_Selector = selector;
		SessionCount++;
    }

	public ShadowChannelWrapper createTunnel(Selector selector) throws Exception {
		SocketChannel socketChannel = null;
		try {
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
			return new ShadowChannelWrapper(socketChannel,selector);		
        } catch (Exception e) {
            if (socketChannel != null)
                socketChannel.close();
            throw new Exception(e.toString());
        }		
    }

	public void setBrotherTunnel(ShadowChannelWrapper brotherTunnel) {
        m_BrotherTunnel = brotherTunnel;
    }

    public void connect(InetSocketAddress destAddress) throws Exception {
        if (LocalVpnService.Instance.protect(m_InnerChannel.socket())) {//保护socket不走vpn
            m_DestAddress = destAddress;
			if (destAddress.isUnresolved()) {
				destAddress = ProxyConfig.Instance.getDefaultProxy(0);
				this.m_UseProxy = true;
			}else this.m_UseProxy = false;
            m_InnerChannel.register(m_Selector, SelectionKey.OP_CONNECT, this);//注册连接事件
            m_InnerChannel.connect(destAddress);//连接目标
        } else {
            throw new Exception("VPN protect socket failed.");
        }
    }

    protected void beginReceive() throws Exception {
        if (m_InnerChannel.isBlocking()) {
            m_InnerChannel.configureBlocking(false);
        }
        m_InnerChannel.register(m_Selector, SelectionKey.OP_READ, this);//注册读事件
    }


    protected boolean write(ByteBuffer buffer, boolean copyRemainData) throws Exception {
        while (buffer.hasRemaining()) {
            if (m_InnerChannel.write(buffer) == 0) {
                break;//不能再发送了，终止循环
            }
        }
        if (buffer.hasRemaining()) {//数据没有发送完毕
            if (copyRemainData) {//拷贝剩余数据，然后侦听写入事件，待可写入时写入。
                //拷贝剩余数据
                if (m_SendRemainBuffer == null) {
                    m_SendRemainBuffer = ByteBuffer.allocate(buffer.capacity());
                }
                m_SendRemainBuffer.clear();
                m_SendRemainBuffer.put(buffer);
                m_SendRemainBuffer.flip();
                m_InnerChannel.register(m_Selector, SelectionKey.OP_WRITE, this);//注册写事件
            }
            return false;
        } else {//发送完毕了
            return true;
        }
    }

    protected void onTunnelEstablished() throws Exception {
        this.beginReceive();//开始接收数据
		m_TunnelEstablished = true;
        m_BrotherTunnel.beginReceive();//兄弟也开始收数据吧
		m_BrotherTunnel.m_TunnelEstablished = true;
    }

    @SuppressLint("DefaultLocale")
    public void onConnectable() {
        try {
			ByteBuffer buffer = GL_BUFFER;
            if (!m_InnerChannel.finishConnect()) {//连接失败
				MainActivity.writeLog("Error: connect to %s failed.", m_DestAddress);
                this.dispose();
            } else if (this.m_UseProxy) {//连接成功
				//根据协议实现握手
				buffer.clear();
				// https://shadowsocks.org/en/spec/Protocol.html

				buffer.put((byte) 0x03);//domain
				byte[] domainBytes = m_DestAddress.getHostName().getBytes();
				buffer.put((byte) domainBytes.length);//domain length;
				buffer.put(domainBytes);
				buffer.putShort((short) m_DestAddress.getPort());
				buffer.flip();

				byte[] _header = new byte[buffer.limit()];
				buffer.get(_header);

				buffer.clear();
				buffer.put(m_Encryptor.encrypt(_header));
				buffer.flip();

				if (write(buffer, true)) {
					onTunnelEstablished();
				}
            }else{
				onTunnelEstablished();
			}
        } catch (Exception e) {
            MainActivity.writeLog("Error: connect to %s failed: %s", m_DestAddress, e);
            this.dispose();
        }
    }

    public void onReadable(SelectionKey key) {
        try {
            ByteBuffer buffer = GL_BUFFER;
            buffer.clear();
            if (m_InnerChannel.read(buffer) > 0) {
                buffer.flip();
				if(this.m_UseProxy){
					m_BrotherTunnel.m_UseProxy = !this.m_UseProxy;
					afterReceived(buffer);//先让子类处理，例如解密数据。
				}
                if (m_BrotherTunnel.m_TunnelEstablished) {//将读到的数据，转发给兄弟。
					if(m_BrotherTunnel.m_UseProxy)
						m_BrotherTunnel.beforeSend(buffer);//发送之前，先让子类处理，例如做加密等。
                    if (!m_BrotherTunnel.write(buffer, true)) {
                        key.cancel();//兄弟吃不消，就取消读取事件。
                        if (ProxyConfig.IS_DEBUG)
                            System.out.printf("%s can not read more.\n", m_DestAddress);
                    }
                }
            } else {
                this.dispose();//连接已关闭，释放资源。
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.dispose();
        }
    }

    protected void beforeSend(ByteBuffer buffer) throws Exception {
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        byte[] newbytes = m_Encryptor.encrypt(bytes);
        buffer.clear();
        buffer.put(newbytes);
        buffer.flip();
    }

    protected void afterReceived(ByteBuffer buffer) throws Exception {
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        byte[] newbytes = m_Encryptor.decrypt(bytes);
        buffer.clear();
        buffer.put(newbytes);
        buffer.flip();
    }

    public void onWritable(SelectionKey key) {
        try {
			if(this.m_UseProxy)
				this.beforeSend(m_SendRemainBuffer);//发送之前，先让子类处理，例如做加密等。
            if (this.write(m_SendRemainBuffer, false)) {//如果剩余数据已经发送完毕
                key.cancel();//取消写事件。
                if (m_TunnelEstablished) {
                    m_BrotherTunnel.beginReceive();//这边数据发送完毕，通知兄弟可以收数据了。
                } else {
                    this.beginReceive();//开始接收代理服务器响应数据
                }
            }
        } catch (Exception e) {
            this.dispose();
        }
    }

    public void dispose() {
        disposeInternal(true);
    }

    void disposeInternal(boolean disposeBrother) {
        if (m_Disposed) {
            return;
        } else {
            try {
                m_InnerChannel.close();
            } catch (Exception e) {
            }

            if (m_BrotherTunnel != null && disposeBrother) {
                m_BrotherTunnel.disposeInternal(false);//把兄弟的资源也释放了。
            }
			m_Encryptor = null;
            m_InnerChannel = null;
            m_SendRemainBuffer = null;
            m_Selector = null;
            m_BrotherTunnel = null;
            m_Disposed = true;
            SessionCount--;
        }
    }
}

