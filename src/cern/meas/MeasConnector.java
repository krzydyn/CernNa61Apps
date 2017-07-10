package cern.meas;
/**
*
* @author KySoft, Krzysztof Dynowski
*
*/

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import com.io.IOStream;
import com.link.TCP;

import sys.Errno;
import sys.Logger;
import sys.StrUtil;
import sys.SysUtil;
import sys.Version;
import channel.ChannelDef;

public class MeasConnector implements Runnable {
	final static Logger log=Logger.getLogger();
	public final static int DATATMO=300+120; //5+2min
	public final static int CMD_CONFIG=0;
	public final static int CMD_LAST=1;
	public final static int CMD_AUTH=2;
	public final static int CMD_HISTORY=3;
	public final static int CMD_MSG=4;
	public final static int CMD_ALARMS=8;

	private IOStream conn=null;
	private final MeasListener listener;
	private final List<byte[]> msgq=Collections.synchronizedList(new LinkedList<byte[]>());

	public MeasConnector(MeasListener lst){ listener=lst; }
	public void start(String uri){
		if (conn!=null) stop();
		conn=IOStream.createIOStream(uri);
		Thread t=new Thread(this,"MeasConnector");
		//t.setDaemon(true);
		t.start();
	}
	public void stop() {
		if (conn==null) return ;
		synchronized (conn) { conn.close(); }
		conn=null;
	}
	@Override
	public void run(){
		try{
			StringBuilder buf=new StringBuilder();
			conn.setChrTmo(500);
			conn.open();
			SysUtil.delay(SysUtil.SECOND/2);
			TCP link=new TCP();
			link.setIO(conn);
			long rcvtmo=System.currentTimeMillis()+1000*DATATMO;
			if (link.open()>=0) {
				listener.connected();
				int r;
				for (;;) {
					if ((r=link.recv(buf))<0) {
						if (r==-Errno.EAGAIN) {
							for (r=0; !msgq.isEmpty() && r<20; ++r) {
								byte[] s=msgq.remove(0);
								//log.debug("send[%d]: %s",s.length,StrUtil.hex(s));
								if (s.length>0) link.send(s);
							}
							SysUtil.delay(100);
							if (rcvtmo>System.currentTimeMillis())
								continue;
							r=-Errno.ETIMEOUT;
						}
						log.error("conn.recv=%d",r);
						break;
					}

					rcvtmo=System.currentTimeMillis()+1000*DATATMO;
					//log.debug("recv[%d]",buf.length());
					//log.debug("recv[%d]: %s",buf.length(),StrUtil.vis(StrUtil.bytes(buf),0,20));
					DataInputStream bi=new DataInputStream(new ByteArrayInputStream(StrUtil.bytes(buf)));
					try{
					//dispatch received message
					int cmd=bi.readUnsignedShort();
					//log.debug("recv[%d]: cmd=%X",buf.length(),cmd);
					if (cmd==CMD_CONFIG) {
						List<ChannelDef> defs=new ArrayList<ChannelDef>();
						try{
							while(bi.available()>0){
								ChannelDef p=new ChannelDef(0);
								p.read(bi); //overwrite id=0
								defs.add(p);
							}
						}catch (EOFException e) {}
						listener.rcvdConfig(defs);
					}
					else if (cmd==CMD_ALARMS) {
						ChannelDef p=new ChannelDef(0);
						try{
							while(bi.available()>0){
								p.id=bi.readInt();
								p.readAlarms(bi);
								listener.rcvdAlarmConfig(p);
							}
						}catch (EOFException e) {}
					}
					else if (cmd==CMD_LAST) {
						Vector<PntData> v=new Vector<PntData>();
						try{
							PntData pd=new PntData();
							do{
							pd.read(bi);
							v.add(new PntData(pd));
							}while(bi.available()>0);
						}catch (EOFException e) {}
						listener.rcvdData(v);
					}
					else if (cmd==CMD_AUTH) {
						log.warn("this command is not supported yet");
					}
					else if (cmd==CMD_HISTORY) {
						Vector<PntData> v=new Vector<PntData>();
						String key=null;
						try{
							PntData pd=new PntData();
							pd.readId(bi);
							key=bi.readUTF();
							do{
							pd.readData(bi);
							v.add(new PntData(pd));
							}while(bi.available()>0);
						}catch (EOFException e) {}
						listener.rcvdData(key,v); v.clear(); v=null;
					}
					else if (cmd==CMD_MSG) {
						String msg=bi.readUTF();
						listener.rcvdMsg(msg);
					}
					}catch (Exception e) { log.error(e,StrUtil.vis(buf.toString())); }
				}
				link.close();
			}
		}catch (Exception e) {
			log.error(e);
			listener.exception(e);
		}
		finally {
			if (conn!=null) { conn.close();conn=null; }
			listener.disconnected();
			log.debug("Receiver stopped");
		}
	}
	public void getConfig(String userid) {
		ByteArrayOutputStream ba=new ByteArrayOutputStream();
		DataOutputStream out=new DataOutputStream(ba);
		try{
		out.writeShort(CMD_CONFIG);
		out.writeUTF(userid);
		out.writeUTF(Version.getInstance().toString());
		out.close();
		msgq.add(ba.toByteArray());
		ba.close(); ba=null;
		}catch (Exception e) {log.debug(e);}
	}
	public void getAlarms() {
		log.debug("getAlarms()");
		ByteArrayOutputStream ba=new ByteArrayOutputStream();
		DataOutputStream out=new DataOutputStream(ba);
		try{
		out.writeShort(CMD_ALARMS);
		out.close();
		msgq.add(ba.toByteArray());
		ba.close(); ba=null;
		}catch (Exception e) {}
	}
	public void serverLog(String msg) {
		ByteArrayOutputStream ba=new ByteArrayOutputStream();
		DataOutputStream out=new DataOutputStream(ba);
		try{
		out.writeShort(CMD_MSG);
		out.writeUTF(msg);
		out.close();
		msgq.add(ba.toByteArray());
		ba.close(); ba=null;
		}catch (Exception e) {}
	}
	public void getLast() {
		ByteArrayOutputStream ba=new ByteArrayOutputStream();
		DataOutputStream out=new DataOutputStream(ba);
		try{
		out.writeShort(CMD_LAST);
		out.close();
		msgq.add(ba.toByteArray());
		ba.close(); ba=null;
		}catch (Exception e) {}
	}
	public void getData(int id,long fr,long to) {
		ByteArrayOutputStream ba=new ByteArrayOutputStream();
		DataOutputStream out=new DataOutputStream(ba);
		try{
		out.writeShort(CMD_AUTH);
		out.writeInt(id);
		out.writeLong(fr);
		out.writeLong(to);
		msgq.add(ba.toByteArray());
		ba.close(); ba=null;
		}catch (Exception e) {}
	}
	public void getHistory(String token,int id,long fr,long to,long step) {
		//log.debug("getHistory(tok=%s,id=%d,fr=%d,to=%d,step=%d)",token,id,fr,to,step);
		ByteArrayOutputStream ba=new ByteArrayOutputStream();
		DataOutputStream out=new DataOutputStream(ba);
		try{
		out.writeShort(CMD_HISTORY);
		out.writeInt(id);
		out.writeUTF(token);
		out.writeLong(fr);
		out.writeLong(to);
		out.writeLong(step);
		msgq.add(ba.toByteArray());
		ba.close(); ba=null;
		}catch (Exception e) {}
	}
}
