package org.svpn.proxy.core;

import java.util.Locale;
import org.svpn.proxy.MainActivity;
import org.svpn.proxy.tcpip.CommonMethods;

public class HttpHostHeaderParser {

	public static String parseHost(NatSession session,byte[] buffer,int offset,int count) {
		try {
			switch (buffer[offset]) {
				case 'G'://GET
				case 'P'://POST,PUT
				case 'C'://CONNECT
				case 'H'://HEAD
		        case 'D'://DELETE
		        case 'O'://OPTIONS
		        case 'T'://TRACE
		            return getHttpHost(session, buffer, offset, count);
		        case 0x16://SSL
		        	return getSNI(session, buffer, offset, count);
			}
		} catch (Exception e) {
			e.printStackTrace();
			MainActivity.writeLog("Error: parseHost:%s", e);
		}
		return null;
	}

	static String getHttpHost(NatSession session,byte[] bytes,int offset,int count){
		String headerString=new String(bytes,offset,count);
		String[] headerLines = headerString.split("\r\n");
		for (int i = 1; i < headerLines.length; i++) {
			String header = headerLines[i].toLowerCase(Locale.ENGLISH);
			if(header.startsWith("host:")){
				String[] nameValue = header.split(":");					
				String value = nameValue[1].trim();
				session.IsHttp = true;
				return value;
			}
		}
		/*if ((bytes[0]=='G')&&(bytes[1]=='E')&&(bytes[3]==' ')||
			(bytes[0]=='P')&&(bytes[1]=='O')&&(bytes[4]==' ')||
			(bytes[0]=='C')&&(bytes[1]=='O')&&(bytes[7]==' ')||
			(bytes[0]=='P')&&(bytes[1]=='U')&&(bytes[3]==' ')||
			(bytes[0]=='H')&&(bytes[1]=='E')&&(bytes[4]==' ')||
			(bytes[0]=='D')&&(bytes[1]=='E')&&(bytes[6]==' ')||
			(bytes[0]=='O')&&(bytes[1]=='P')&&(bytes[7]==' ')||
			(bytes[0]=='T')&&(bytes[1]=='R')&&(bytes[5]==' ')){			
		}*/
		return null;
	}

    static String getSNI(NatSession session,byte[] buffer,int offset,int count){
		int limit=offset+count;
        if (count > 43 && buffer[offset] == 0x16) {//TLS Client Hello
            offset +=43;//skip 43 bytes header

            //read sessionID:
            if(offset+1>limit)return null;
            int sessionIDLength = buffer[offset++]&0xFF;
            offset += sessionIDLength;

            //read cipher suites:
            if(offset+2>limit)return null;
            int cipherSuitesLength = CommonMethods.readShort(buffer, offset)&0xFFFF;
            offset+=2;
            offset += cipherSuitesLength;

            //read Compression method:
            if(offset+1>limit)return null;
            int compressionMethodLength = buffer[offset++]&0xFF;
            offset += compressionMethodLength;

            if (offset == limit){
                System.err.println("TLS Client Hello packet doesn't contains SNI info.(offset == limit)");
                return null;
            }

            //read Extensions:
            if(offset+2>limit)return null;
            int extensionsLength = CommonMethods.readShort(buffer, offset)&0xFFFF;
            offset+=2;

            if (offset + extensionsLength > limit){
				System.err.println("TLS Client Hello packet is incomplete.");
				return null;
            }

            while (offset+4 <= limit){
                int type0 = buffer[offset++]&0xFF;
                int type1 = buffer[offset++]&0xFF;
                int length = CommonMethods.readShort(buffer, offset)&0xFFFF;
                offset+=2;

                if (type0 == 0x00 && type1 == 0x00 && length > 5){ //have SNI
                	offset+=5;//skip SNI header.
                	length-=5;//SNI size;
                	if(offset+length>limit) return null;
                	String serverName =new String(buffer, offset, length);
					session.IsHttp = false;
                	if(ProxyConfig.IS_DEBUG)
                		System.out.printf("SNI: %s\n", serverName);
                    return serverName;
                }
                else {
                    offset += length;
                }
            }

            System.err.println("TLS Client Hello packet doesn't contains Host field info.");
            return null;
        }
        else {
        	System.err.println("Bad TLS Client Hello packet.");
            return null;
        }
    }
}

