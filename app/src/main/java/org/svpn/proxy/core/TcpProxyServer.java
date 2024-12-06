package org.svpn.proxy.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import org.svpn.proxy.MainActivity;
import org.svpn.proxy.core.ProxyConfig;
import org.svpn.proxy.utils.TextUtils;
import org.svpn.proxy.tcpip.CommonMethods;

public class TcpProxyServer implements Runnable {

	/**
	 *SelectionKey.OP_CONNECT
	 *SelectionKey.OP_ACCEPT
	 *SelectionKey.OP_READ
	 *SelectionKey.OP_WRITE

	 *static int	OP_ACCEPT         用于套接字接受操作的操作集位。
	 *static int	OP_CONNECT        用于套接字连接操作的操作集位。
	 *static int	OP_READ           用于读取操作的操作集位。
	 *static int	OP_WRITE          用于写入操作的操作集位。
	 */

	/**
	 *protected	SelectionKey()        构造此类的一个实例。
	 *Object	attach(Object ob)     将给定的对象附加到此键。
	 *Object	attachment()          获取当前的附加对象。
	 *abstract  void	cancel()      请求取消此键的通道到其选择器的注册。
	 *abstract  SelectableChannel	channel()    返回为之创建此键的通道。
	 *abstract  int	interestOps()     获取此键的 interest 集合。
	 *abstract  SelectionKey	interestOps(int ops)    将此键的 interest 集合设置为给定值。
	 *boolean	isAcceptable()        测试此键的通道是否已准备好接受新的套接字连接。
	 *boolean	isConnectable()       测试此键的通道是否已完成其套接字连接操作。
	 *boolean	isReadable()          测试此键的通道是否已准备好进行读取。
	 *boolean	isWritable()          测试此键的通道是否已准备好进行写入。
	 *abstract  boolean	isValid()     告知此键是否有效。
	 *abstract  int	readyOps()        获取此键的 ready 操作集合。
	 *abstract  Selector	selector()      返回为此选择器创建的键。
	 */

	/**
	 *capacity	缓冲区数组的总长度
	 *position	下一个要操作的数据元素的位置
	 *limit	缓冲区数组中不可操作的下一个元素的位置：limit<=capacity
	 *mark	用于记录当前position的前一个位置或者默认是0
	 */

	public boolean Stopped;
	public short Port;

	//信道选择器
	Selector m_Selector;

	//注册通道
	ServerSocketChannel m_ServerSocketChannel;

	//线程
	Thread m_ServerThread;

	public TcpProxyServer(int port) throws IOException {
		// 创建选择器
		m_Selector = Selector.open();
		// 打开监听信道
		m_ServerSocketChannel = ServerSocketChannel.open();
		// 设置为非阻塞模式
		m_ServerSocketChannel.configureBlocking(false);
		// 与本地端口绑定
		m_ServerSocketChannel.socket().bind(new InetSocketAddress(port));
		// 将选择器绑定到监听信道,只有非阻塞信道才可以注册选择器.并在注册过程中指出该信道可以进行Accept操作
		m_ServerSocketChannel.register(m_Selector, SelectionKey.OP_ACCEPT);
		this.Port=(short) m_ServerSocketChannel.socket().getLocalPort();
		System.out.printf("AsyncTcpServer listen on %d success.\n", this.Port&0xFFFF);
	}

	public void start(){
		m_ServerThread=new Thread(this);
		m_ServerThread.setName("TcpProxyServerThread");
		m_ServerThread.start();
	}

	public void stop(){
		this.Stopped=true;
		if(m_Selector!=null){
			try {
				m_Selector.close();
				m_Selector=null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if(m_ServerSocketChannel!=null){
			try {
				m_ServerSocketChannel.close();
				m_ServerSocketChannel=null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run() {
		try {
			// 反复循环,等待IO
			while (true) {
				m_Selector.select();
				// 取得迭代器.selectedKeys()中包含了每个准备好某一I/O操作的信道的SelectionKey
				/**
				 *Selected-key Iterator 代表了所有通过
				 *select() 方法监测到可以进行
				 *IO 操作的 channel
				 *这个集合可以通过 selectedKeys() 拿到
				 */
				Iterator<SelectionKey> keyIterator = m_Selector.selectedKeys().iterator();
				while (keyIterator.hasNext()) {
					SelectionKey selectionKey = keyIterator.next();
					if (selectionKey.isValid()) {
						try {
							if(ProxyConfig.IS_SSRUN){
								if (selectionKey.isReadable()) {
									// 判断是否有数据发送过来
									// 从客户端读取数据
									((ShadowChannelWrapper)selectionKey.attachment()).onReadable(selectionKey);
									// a channel is ready for reading
								}
								else if (selectionKey.isWritable()) {
									// 发送给客户端
									// 客户端可写时
									((ShadowChannelWrapper)selectionKey.attachment()).onWritable(selectionKey);
									// a channel is ready for writing
								}
								else if (selectionKey.isConnectable()) {

									((ShadowChannelWrapper)selectionKey.attachment()).onConnectable();
									// a connection was established with a remote server.
								}
								else  if (selectionKey.isAcceptable()) {
									// 有客户端连接请求时
									onShadowAccepted(selectionKey);
									// a connection was accepted by a ServerSocketChannel.
								}
							}else{
								if (selectionKey.isReadable()) {
									// 判断是否有数据发送过来
									// 从客户端读取数据
									((SocketChannelWrapper)selectionKey.attachment()).onReadable(selectionKey);
									// a channel is ready for reading
								}
								else if (selectionKey.isWritable()) {
									// 发送给客户端
									// 客户端可写时
									((SocketChannelWrapper)selectionKey.attachment()).onWritable(selectionKey);
									// a channel is ready for writing
								}
								else if (selectionKey.isConnectable()) {

									((SocketChannelWrapper)selectionKey.attachment()).onConnectable();
									// a connection was established with a remote server.
								}
								else  if (selectionKey.isAcceptable()) {
									// 有客户端连接请求时
									onAccepted(selectionKey);
									// a connection was accepted by a ServerSocketChannel.
								}
							}
						} catch (Exception e) {
							System.out.println(e.toString());
							// 出现异常（如客户端断开连接）时移除处理过的键
							keyIterator.remove();
							continue;
						}
					}
					keyIterator.remove();
					//注意这里必须手动remove；表明该selectkey我已经处理过了；
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			this.stop();
			System.out.println("TcpServer thread exited.");
		}
	}

    void onShadowAccepted(SelectionKey key) {
        ShadowChannelWrapper localTunnel = null;
        try {
            SocketChannel localChannel = m_ServerSocketChannel.accept();
            localTunnel = new ShadowChannelWrapper(localChannel, m_Selector);

            InetSocketAddress destAddress = getDestAddress(localChannel);
            if (destAddress  instanceof InetSocketAddress) {
                ShadowChannelWrapper remoteTunnel = localTunnel.createTunnel(m_Selector);
                remoteTunnel.setBrotherTunnel(localTunnel);//关联兄弟
                localTunnel.setBrotherTunnel(remoteTunnel);//关联兄弟
                remoteTunnel.connect(destAddress);//开始连接
            } else {
                MainActivity.writeLog("Error: socket(%s:%d) target host is null.", localChannel.socket().getInetAddress().toString(), localChannel.socket().getPort());
                localTunnel.dispose();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.printf("Error: remote socket create failed: %s", e.toString());
            if (localTunnel != null) {
                localTunnel.dispose();
            }
        }
    }

	InetSocketAddress getDestAddress(SocketChannel localChannel) {
        short portKey = (short) localChannel.socket().getPort();
        NatSession session = NatSessionManager.getSession(portKey);
        if (session != null) {
            if (ProxyConfig.Instance.needProxy(session.RemoteHost, session.RemoteIP)) {
                return InetSocketAddress.createUnresolved(session.RemoteHost, session.RemotePort & 0xFFFF);
            } else {
                return new InetSocketAddress(localChannel.socket().getInetAddress(), session.RemotePort & 0xFFFF);
            }
        }
        return null;
    }

	void onAccepted(SelectionKey selectionKey){
		SocketChannelWrapper socketChannelWrapper =null;
		try {
			// 返回创建此键的通道，接受客户端建立连接的请求，并返回 SocketChannel 对象
			SocketChannel accept = ((ServerSocketChannel) selectionKey.channel()).accept();			
			socketChannelWrapper = new SocketChannelWrapper(accept, m_Selector);
			
			InetSocketAddress targetSocketAddress = null;
			short portKey = (short) accept.socket().getPort();
			NatSession session = NatSessionManager.getSession(portKey);
			if (session != null) {
				if(!ProxyConfig.IS_CAPTURE&&!ProxyConfig.IS_UPNETWORK){
					if (session.IsHttp){
						targetSocketAddress = ProxyConfig.Instance.getDefaultProxy(0);
					} else {
						targetSocketAddress = InetSocketAddress.createUnresolved(session.RemoteHost, session.RemotePort&0xFFFF);
					}
				} else {
					targetSocketAddress = new InetSocketAddress(accept.socket().getInetAddress(), session.RemotePort&0xFFFF);
				}
			}
						
			if(targetSocketAddress != null){
				SocketChannelWrapper remoteTunnel = socketChannelWrapper.createNew(m_Selector);
				remoteTunnel.setBrotherChannelWrapper(socketChannelWrapper);//关联兄弟
				socketChannelWrapper.setBrotherChannelWrapper(remoteTunnel);//关联兄弟
				remoteTunnel.connect(targetSocketAddress, session.RemotePort);//开始连接
				return;
			}
			MainActivity.writeLog("Error: socket(%s:%d) target host is null.",accept.socket().getInetAddress().toString(),accept.socket().getPort());
			socketChannelWrapper.dispose();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.printf("Error: remote socket create failed: %s",e.toString());
			if(socketChannelWrapper!=null){
				socketChannelWrapper.dispose();
			}
		}
	}
}
