package org.svpn.proxy.core;

import java.util.Locale;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import android.annotation.SuppressLint;

import org.svpn.proxy.BaseContext;
import org.svpn.proxy.MainActivity;

public class SocketChannelWrapper
implements LocalConstants {

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
	final static ByteBuffer GL_BUFFER = ByteBuffer.allocate(40960);

	SocketChannel m_InnerChannel;

	ByteBuffer m_SendRemainBuffer;
	Selector m_Selector;
	SocketChannelWrapper m_BrotherChannelWrapper;

	boolean m_UseProxy;	
	boolean m_TunnelEstablished;

	int m_RemotePort = 80;

	InetSocketAddress m_TargetSocketAddress;

	public SocketChannelWrapper(SocketChannel socketChannel,Selector selector){
		this.m_InnerChannel=socketChannel;
		this.m_Selector=selector;
	}

	public SocketChannelWrapper createNew(Selector selector) throws Exception{
		SocketChannel socketChannel = null;
		try {
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            return new SocketChannelWrapper(socketChannel, selector);
        } catch (Exception exception) {
            if (socketChannel != null) {
                socketChannel.close();
            }
            throw exception;
        }
	}

	public void setBrotherChannelWrapper(SocketChannelWrapper socketChannelWrapper){
		this.m_BrotherChannelWrapper=socketChannelWrapper;
	}

    @SuppressLint("DefaultLocale")
	public void connect(InetSocketAddress targetSocketAddress, int RemotePort) throws Exception{
		if(LocalVpnService.Instance.protect(m_InnerChannel.socket())){//保护socket不走vpn
			this.m_TargetSocketAddress = targetSocketAddress;
			if (targetSocketAddress.isUnresolved()) {//未解析
				targetSocketAddress = ProxyConfig.Instance.getDefaultProxy(1);
				if (targetSocketAddress != null){
					this.m_UseProxy = true;
				}
			} else {
				this.m_UseProxy = false;
				this.m_RemotePort = RemotePort;
				this.m_BrotherChannelWrapper.m_RemotePort = RemotePort;
			}
			m_InnerChannel.connect(targetSocketAddress);//连接目标
			m_InnerChannel.register(m_Selector, SelectionKey.OP_CONNECT,this);//注册连接事件
			return;
		}
		throw new Exception("VPN protect socket failed.");
	}

    void onTunnelEstablished() throws Exception {
		this.m_TunnelEstablished = true;
		this.registerReadOperation();//开始接收数据
		this.m_BrotherChannelWrapper.m_TunnelEstablished = true;
		this.m_BrotherChannelWrapper.registerReadOperation();//兄弟也开始收数据吧
    }

	void registerReadOperation() throws Exception{
		if(m_InnerChannel.isBlocking()){
			m_InnerChannel.configureBlocking(false);
		}
		m_InnerChannel.register(m_Selector, SelectionKey.OP_READ,this);//注册读事件
	}

	boolean write(ByteBuffer buffer,boolean copyRemainData) throws Exception {
    	while (buffer.hasRemaining()) {
			if(m_InnerChannel.write(buffer)==0){
				break;//不能再发送了，终止循环
			}
		}
    	if(buffer.hasRemaining()){//数据没有发送完毕
    		if(copyRemainData){//拷贝剩余数据，然后侦听写入事件，待可写入时写入。
    			//拷贝剩余数据
    			if(m_SendRemainBuffer==null){
    				m_SendRemainBuffer=ByteBuffer.allocate(buffer.capacity());
    			}
    			m_SendRemainBuffer.clear();
        		m_SendRemainBuffer.put(buffer);
    			m_SendRemainBuffer.flip();
    			m_InnerChannel.register(m_Selector,SelectionKey.OP_WRITE, this);//注册写事件
    		}
			return false;
    	}
    	else {//发送完毕了
    		return true;
		}
	}

    @SuppressLint("DefaultLocale")
	public void onConnectable(){
    	try {
            ByteBuffer byteBuffer = GL_BUFFER;
        	if(!this.m_InnerChannel.finishConnect()){//连接失败
        		MainActivity.writeLog("Error: connect to %s failed.",this.m_UseProxy ? "proxy" : "server");
				this.dispose();
        	} else if(this.m_UseProxy){//连接成功&使用代理
				Charset forName = Charset.forName("ISO-8859-1");

				//TCP已连接，可以根据协议实现握手等。
				String SslHeader = (ProxyConfig.SslHeader+"\r\n")
					.replace(H,m_TargetSocketAddress.getHostName()+":"+m_TargetSocketAddress.getPort())
					.replace(H_NP,m_TargetSocketAddress.getHostName())
					.replace(M,"CONNECT").replace(":"+P,P)
					.replace(P,(":")+m_TargetSocketAddress.getPort())
					.replace(V,"HTTP/1.1");

                byteBuffer.clear();
                byteBuffer.put(SslHeader.getBytes(forName),0,SslHeader.length());
                byteBuffer.flip();

                if (this.write(byteBuffer, true)) {
                    registerReadOperation();
                }
            } else {
				this.onTunnelEstablished();//开始接收数据
            }
		} catch (Exception e) {
			this.dispose();
		}
    }

	public void onReadable(SelectionKey selectionKey){
		try {
            ByteBuffer buffer = GL_BUFFER;
			buffer.clear();
			//绑定原始的通道
			int len = m_InnerChannel.read(buffer);
			if(len>0){
				buffer.flip();
				if(!this.m_TunnelEstablished){
					//收到代理服务器响应数据
					//分析响应并判断是否连接成功
					String response = new String(buffer.array(),buffer.position(),12);
					if(response.matches("^HTTP/1.[01] 200$")){
						buffer.limit(buffer.position());
					} else {
						throw new Exception(String.format("Proxy server responsed an error: %s",response));
					}
					this.onTunnelEstablished();//开始接收数据
				} else {//将读到的数据，转发给兄弟。
					try{
						Charset forName = Charset.forName("ISO-8859-1");
						byte[] bytes = new byte[buffer.limit()];
						buffer.get(bytes,0,bytes.length);
						String data = new String(bytes,0,bytes.length,forName);
						if(ProxyConfig.IS_CAPTURE){
							if(LocalConfig.isCapture(data,ProxyConfig.Capture)){
								String[] split_rn = data.split("\r\n");
								String cap_byte = "";
								boolean isSuccess=false;
								MainActivity.writeLog(null);
								StringBuffer sb = new StringBuffer();
								for(String s:ProxyConfig.Capture){
									sb.append(",").append(s);
								}
								String ty = sb.toString().substring(1);
								MainActivity.writeLog("抓包( %s ){ %s }\n",ProxyConfig.CapApp,ty);
								for(String header : ProxyConfig.Capture){
									for (String h : split_rn) {
										if(h.startsWith(header + ":")){
											cap_byte = h.split(":")[1];

											String tp = LocalConfig.getMiddle(ProxyConfig.HttpHeader,header + ":","\r\n");
											String ps = LocalConfig.getMiddle(ProxyConfig.SslHeader,header + ":","\r\n");

											MainActivity.writeLog("抓获%s"+" "+cap_byte+" "+"成功！",header+":");
											if(ProxyConfig.HttpHeader.contains(header)||ProxyConfig.SslHeader.contains(header)){

												ProxyConfig.HttpHeader = ProxyConfig.HttpHeader.replace(tp,cap_byte);
												ProxyConfig.SslHeader = ProxyConfig.SslHeader.replace(ps,cap_byte);

												LocalConfig.CapData = LocalConfig.CapData.replace(tp,cap_byte);
											}
										}
									}
									if(ProxyConfig.HttpHeader.contains(header + ":" + cap_byte)
									   ||ProxyConfig.SslHeader.contains(header + ":" + cap_byte)){
										isSuccess=true;
										MainActivity.writeLog("更新%s完毕！\n",header);
									}else {
										isSuccess=false;
										MainActivity.writeLog("更新%s失败！\n",header);
									}
								}
								if(isSuccess){
									BaseContext.handler.sendEmptyMessage(0);//提示抓包成功
								}else{
									BaseContext.handler.sendEmptyMessage(3);//抓包失败
								}
								ProxyConfig.IS_CAPTURE = false;
								MainActivity.writeLog("本次抓包完毕，已停止抓包！");
							}
							ByteBuffer wrap = ByteBuffer.wrap(data.getBytes(forName),0,data.length());
							if (!this.m_BrotherChannelWrapper.write(wrap, true)) {
								selectionKey.cancel();//兄弟吃不消，就取消读取事件。
							}
						}else{

							//GET $ POST
							if ((bytes[0]=='G')&&(bytes[1]=='E')&&(bytes[3]==' ')||
								(bytes[0]=='P')&&(bytes[1]=='O')&&(bytes[4]==' ')) {
								try{
									int rn = data.indexOf("\r\n");
									String f = data.substring(0,rn);
									//MainActivity.writeLog("打印首头:"+f);
									String m = f.substring(0,f.indexOf(' '));
									//MainActivity.writeLog("打印M:"+m);
									String url = f.substring(f.indexOf(' ')+1);
									int u_end = url.lastIndexOf(' ');
									url = url.substring(0,u_end);
									//MainActivity.writeLog("打印URL:"+u);
									int v_end = f.lastIndexOf(' ');
									String ver = f.substring(v_end+1);
									//MainActivity.writeLog("打印版本:"+v);
									String other = data.substring(rn+2);
									//MainActivity.writeLog("打印其他:"+other);
									//MainActivity.writeLog("打印data:"+data.replace(f+"\r\n",""));

									String uri = url;
									boolean useRemotePort = true;
									if(uri.startsWith("http://"))
									{
										uri = uri.substring(7);
										uri = uri.substring(uri.indexOf("/"));
										useRemotePort = false;
									}

									//获取http模式首行
									String http = ProxyConfig.HttpHeader;
									//设置请求首头
									http = http.replace(M,m)
										//设置http_uri
										.replace(U,uri)
										//设置http协议版本
										.replace(V,ver);

									String[] split_rn = other.split("\r\n");
									//设定Host
									for (int i =0;i<split_rn.length;i++) {
										String name = split_rn[i].toLowerCase(Locale.ENGLISH);
										if(name.startsWith("host:")){
											String host = split_rn[i];
											host = host.replace(host.substring(0,5),"").replace(" ","");
											if((host.indexOf(':')==-1)){
												if(useRemotePort){
													host = host+":"+this.m_RemotePort;
												}
											}
											http = http.replace(H,host);
										}
										//删除头域
										for (String del_byte : ProxyConfig.HttpDel) {
											String del = del_byte.toLowerCase(Locale.ENGLISH);
											if(name.startsWith(del+":")){
												data = data.replace(split_rn[i]+"\r\n","");
											}
										}
									}
									//修改首行请求头
									data = data.replace(f+"\r\n",http);
									ByteBuffer wrap = ByteBuffer.wrap(data.getBytes(forName),0,data.length());
									if (!this.m_BrotherChannelWrapper.write(wrap, true)) {
										selectionKey.cancel();//兄弟吃不消，就取消读取事件。
									}
								} catch (Exception e) {
									e.printStackTrace();
									MainActivity.writeLog("GP代理错误: "+e.toString());
								}

								//CONNECT
							} else if((bytes[0]=='C')&&(bytes[1]=='O')&&(bytes[7]==' ')){
								try{
									//String[] split_rn = data.split("\r\n");
									//String[] split_tab = split_rn[0].split(" ");
									int rn = data.indexOf("\r\n");
									String f = data.substring(0,rn);
									//MainActivity.writeLog("打印首头:"+f);
									String m = f.substring(0,f.indexOf(' '));
									//MainActivity.writeLog("打印M:"+m);
									String url = f.substring(f.indexOf(' ')+1);
									int u_end = url.lastIndexOf(' ');
									url = url.substring(0,u_end);
									//MainActivity.writeLog("打印URL:"+u);
									int v_end = f.lastIndexOf(' ');
									String ver = f.substring(v_end+1);
									//MainActivity.writeLog("打印版本:"+v);
									String other = data.substring(rn+2);
									//MainActivity.writeLog("打印其他:"+other);
									//MainActivity.writeLog("打印data:"+data.replace(f+"\r\n",""));

									//获取https模式首行
									String ssl = ProxyConfig.SslHeader;
									//设置请求首头
									ssl = ssl.replace(M,m)
										//设定Host
										.replace(H,url)
										//设置https协议版本
										.replace(V,ver);

									String[] split_rn = other.split("\r\n");
									//设定Host
									for (int i =0;i<split_rn.length;i++) {
										String name = split_rn[i].toLowerCase(Locale.ENGLISH);
										//删除头域
										for (String del_byte : ProxyConfig.SslDel) {
											String del = del_byte.toLowerCase(Locale.ENGLISH);
											if(name.startsWith(del+":")){
												data = data.replace(split_rn[i]+"\r\n","");
											}
										}
									}
									//修改数据
									data = data.replace(f+"\r\n",ssl);
									ByteBuffer wrap = ByteBuffer.wrap(data.getBytes(forName),0,data.length());
									if (!this.m_BrotherChannelWrapper.write(wrap, true)) {
										selectionKey.cancel();//兄弟吃不消，就取消读取事件。
									}
								} catch (Exception e) {
									e.printStackTrace();
									MainActivity.writeLog("C代理错误: "+e.toString());
								}

							} else {
								ByteBuffer wrap = ByteBuffer.wrap(data.getBytes(forName),0,data.length());
								if (!this.m_BrotherChannelWrapper.write(wrap, true)) {
									selectionKey.cancel();//兄弟吃不消，就取消读取事件。
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}else if(len<0) {
				this.dispose();//连接已关闭，释放资源。
			}
		} catch (Exception e) {
			e.printStackTrace();
			this.dispose();
		}
	}

	public void onWritable(SelectionKey key){
		try {
			if(this.write(m_SendRemainBuffer, false)) {//如果剩余数据已经发送完毕
				key.cancel();//取消写事件。
				if(this.m_TunnelEstablished){
					this.m_BrotherChannelWrapper.registerReadOperation();//这边数据发送完毕，通知兄弟可以收数据了。
				}else {
					this.registerReadOperation();//开始接收代理服务器响应数据
				}
			}
		} catch (Exception e) {
			this.dispose();
		}
	}

	public void dispose(){
		this.disposeInternal(true);
	}

	void disposeInternal(boolean disposeBrother) {
		try {
			this.m_InnerChannel.close();
		} catch (Exception e) {
		}

		if(this.m_BrotherChannelWrapper!=null&&disposeBrother){
			this.m_BrotherChannelWrapper.disposeInternal(false);//把兄弟的资源也释放了。
		}

		this.m_InnerChannel=null;
		this.m_SendRemainBuffer=null;
		this.m_Selector=null;
		this.m_BrotherChannelWrapper=null;
		this.m_TargetSocketAddress = null;
	}
}


