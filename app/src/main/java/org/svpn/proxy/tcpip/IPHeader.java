package org.svpn.proxy.tcpip;

public class IPHeader {
	/**
	 * IP报文格式
	 * 0                                   　　　　       15  16　　　　　　　　　　　　　　　　　　　　　　　　   31
	 * ｜　－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜  ４　位     ｜   ４位首     ｜      ８位服务类型      ｜      　　         １６位总长度            　   ｜
	 * ｜  版本号     ｜   部长度     ｜      （ＴＯＳ）　      ｜      　 　 （ｔｏｔａｌ　ｌｅｎｇｔｈ）    　    ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜  　　　　　　　　１６位标识符                         ｜　３位    ｜　　　　１３位片偏移                 ｜
	 * ｜            （ｉｎｄｅｎｔｉｆｉｅｒ）                 ｜　标志    ｜      （ｏｆｆｓｅｔ）　　           ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜      ８位生存时间ＴＴＬ      ｜       ８位协议        ｜　　　　　　　　１６位首部校验和                  ｜
	 * ｜（ｔｉｍｅ　ｔｏ　ｌｉｖｅ）　　｜   （ｐｒｏｔｏｃｏｌ） ｜              （ｃｈｅｃｋｓｕｍ）               ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜                              ３２位源ＩＰ地址（ｓｏｕｒｃｅ　ａｄｄｒｅｓｓ）                           ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜                         ３２位目的ＩＰ地址（ｄｅｓｔｉｎａｔｉｏｎ　ａｄｄｒｅｓｓ）                     ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜                                          ３２位选项（若有）                                        ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜                                                                                                  ｜
	 * ｜                                               数据                                               ｜
	 * ｜                                                                                                  ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 **/
	
	public static final short IP = 0x0800;
	public static final byte ICMP = 1;
	public static final byte TCP = 6;  //6: TCP协议号
	public static final byte UDP = 17; //17: UDP协议号
	
	static final byte offset_ver_ihl = 0; //0: 版本号（4bits） + 首部长度（4bits）
	static final byte offset_tos = 1; //1：服务类型偏移
	static final short offset_tlen = 2; //2：总长度偏移
	static final short offset_identification = 4; //4：16位标识符偏移
	static final short offset_flags_fo = 6; //6：标志（3bits）+ 片偏移（13bits）
	static final byte offset_ttl = 8; //8：生存时间偏移
	public static final byte offset_proto = 9; //9：8位协议偏移
	static final short offset_crc = 10; //10：首部校验和偏移
	public static final int offset_src_ip = 12; //12：源ip地址偏移
	public static final int offset_dest_ip = 16; //16：目标ip地址偏移
	static final int offset_op_pad = 20; //20：选项 + 填充
	
	public byte[] m_Data;
	public int m_Offset;

	public IPHeader(byte[] data, int offset) {
		this.m_Data = data;
		this.m_Offset = offset;
	}

	public void Default() {
		setHeaderLength(20);
		setTos((byte)0);
		setTotalLength(0);
		setIdentification(0);
		setFlagsAndOffset((short)0);
		setTTL((byte)64);
	}

	public int getDataLength() {
		return this.getTotalLength()-this.getHeaderLength();
	}

	public int getHeaderLength() {
		return (m_Data[m_Offset + offset_ver_ihl] & 0x0F) * 4;
	}

	public void setHeaderLength(int value) {
		m_Data[m_Offset+offset_ver_ihl]= (byte)((4<<4)|(value/4));
	}

	public byte getTos() {
		return m_Data[m_Offset + offset_tos];
	}

	public void setTos(byte value) {
		m_Data[m_Offset + offset_tos] = value;
	}

	public int getTotalLength() {
		return CommonMethods.readShort(m_Data, m_Offset + offset_tlen) & 0xFFFF;
	}

	public void setTotalLength(int value) {
		CommonMethods.writeShort(m_Data, m_Offset + offset_tlen, (short) value);
	}

	public int getIdentification() {
		return CommonMethods.readShort(m_Data, m_Offset + offset_identification) & 0xFFFF;
	}

	public void setIdentification(int value) {
		CommonMethods.writeShort(m_Data, m_Offset + offset_identification, (short) value);
	}

	public short getFlagsAndOffset() {
		return CommonMethods.readShort(m_Data, m_Offset + offset_flags_fo);
	}

	public void setFlagsAndOffset(short value) {
		CommonMethods.writeShort(m_Data, m_Offset + offset_flags_fo, value);
	}

	public byte getTTL() {
		return m_Data[m_Offset + offset_ttl];
	}

	public void setTTL(byte value) {
		m_Data[m_Offset + offset_ttl] = value;
	}

	public byte getProtocol() {
		return m_Data[m_Offset + offset_proto];
	}

	public void setProtocol(byte value) {
		m_Data[m_Offset + offset_proto] = value;
	}

	public short getCrc() {
		return CommonMethods.readShort(m_Data, m_Offset + offset_crc);
	}

	public void setCrc(short value) {
		CommonMethods.writeShort(m_Data, m_Offset + offset_crc, value);
	}

	public int getSourceIP() {
		return CommonMethods.readInt(m_Data, m_Offset + offset_src_ip);
	}

	public void setSourceIP(int value) {
		CommonMethods.writeInt(m_Data, m_Offset + offset_src_ip, value);
	}

	public int getDestinationIP() {
		return CommonMethods.readInt(m_Data, m_Offset + offset_dest_ip);
	}

	public void setDestinationIP(int value) {
		CommonMethods.writeInt(m_Data, m_Offset + offset_dest_ip, value);
	}

	@Override
	public String toString() {
		return String.format("%s->%s Pro=%s,HLen=%d", CommonMethods.ipIntToString(getSourceIP()),CommonMethods.ipIntToString(getDestinationIP()),getProtocol(),getHeaderLength());
	}

}

