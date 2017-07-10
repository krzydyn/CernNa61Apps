package cern.casaver;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.io.IOStream;
import com.link.TCP;

import sys.Logger;
import sys.StrUtil;
import sys.SysUtil;
import channel.ChannelDef;
import conn.AbstrConn.ConnectorListener;
import epics.JcaConn;

public class CASaver {
	static Logger log=Logger.getLogger();
	static List<SaveChannelDef> chndefs=new ArrayList<SaveChannelDef>();
	static Map<String,SaveChannelDef> pvmap=new HashMap<String,SaveChannelDef>();
	static String dbserver=null;
	static String addr_list=null;
	static int readPeriod=60;
	static int sendPeriod=300;
	static int done=0;

	static void readConfig()throws IOException{
		IOStream io=new IOStream(new FileInputStream("casaver.conf"),null);
		StringBuilder b=new StringBuilder(100);
		String key;
		while (io.readln(b)>=0){
			if (b.charAt(0)=='#')continue;
			StrUtil.trim(b);
			if (b.toString().startsWith(key="dbserver=")){
				dbserver=b.substring(key.length());
			}
			else if (b.toString().startsWith(key="dbperiod=")){
				sendPeriod=Integer.parseInt(b.substring(key.length()));
			}
			else if (b.toString().startsWith(key="period=")){
				readPeriod=Integer.parseInt(b.substring(key.length()));
			}
			else if (b.toString().startsWith(key="addr_list=")){
				addr_list=b.substring(key.length());
			}
			else if (b.toString().startsWith(key="chn=")){
				String[] s=b.substring(key.length()).split(",");
				SaveChannelDef chn=new SaveChannelDef(s[0]);
				if (s.length>1) chn.pv=s[1];
				chndefs.add(chn);
			}
		}
		io.close();
		log.info("loaded config");
		for (int i=0; i<chndefs.size(); ++i){
			ChannelDef chn=chndefs.get(i);
			log.info("chn[%d]=%s,%s",i,chn.name,chn.pv);
		}
	}
	static void sendToDB(){
		StringBuilder buf=new StringBuilder();
		buf.append("D");
		for (int i=0; i<chndefs.size(); ++i){
			SaveChannelDef def=chndefs.get(i);
			if (def.cnt<=0) {
				log.info("chn=%s cnt=%d",def.pv,def.cnt);
				continue;
			}
			float v=def.getData();
			def.resetData();
			log.info("pv('%s')=%g tm=%d",def.pv,v,def.tm);
			buf.append(String.format((Locale)null,"%s,%d,%.5f;",def.name,def.tm,v));
		}
		if (buf.length()<=1) {
			log.info("buf empty, nothing to send");
			return ;
		}
		if (dbserver!=null && dbserver.indexOf(":")>0){
			IOStream c=IOStream.createIOStream(dbserver);
			try{
				c.open();
				TCP link=new TCP();
				link.setIO(c);
				do{
					if (link.open()<0) break;
					log.debug("send: %s",buf.toString());
					if (link.send(buf)<0) break;
					if (link.recv(buf)<0) break;
					log.debug("recv: %s",buf.toString());
				}while(false);
			}catch (Exception e) {log.debug(e);}
			finally{c.close();}
		} else log.info("* buf[%d]:%s",buf.length(),buf.toString());
		buf=null;
	}
	static ConnectorListener connli=new ConnectorListener(){
		@Override
		public void connected() {
		}
		@Override
		public void disconnected() {
		}
		@Override
		public void exception(Exception e) {
		}
		@Override
		public void execDone(int id){}
		@Override
		public void readDone(int r, String pv, float[] v) {
			SaveChannelDef chn=pvmap.get(pv);
			if (chn==null) return ;
			++done;
			if (r>=0) {
				log.info("r=%d, readpv(%s)=%g",r,chn.name,v[0]);
				if (chn.name.startsWith("vd")) {
					 if (v[0]>=0) chn.addData(v);
				}
				else chn.addData(v);
			}
			else {
				chn.cnt=-1;
				log.error("readpv(%s) err %d",chn.name,r);
			}
		}
		@Override
		public void writeDone(int r, String pv) {
			SaveChannelDef chn=pvmap.get(pv);
			if (chn==null) return ;
			log.debug("r=%d, writepv(%s)",r,chn.name);
		}
	};
	public static void main(String[] args) {
		try{
			readConfig();
		}catch (Exception e) { log.error(e); }
		if (addr_list==null) {
			log.error("addr_list not given");
			return ;
		}
		Logger.getLogger().addFileHandler("casaver.log");
		for (int i=0; i<chndefs.size(); ++i) {
			SaveChannelDef c=chndefs.get(i);
			pvmap.put(c.pv,c);
		}
		JcaConn jca=new JcaConn();
		jca.setConnectorListener(connli);
		//TODO support list of addresses (create jca for each)
		jca.setAddr(addr_list);
		jca.start();
		jca.sendInit(null);//or "client@host"

		long readTm,sendTm;
		long tm=System.currentTimeMillis()/1000;
		sendTm=readTm=tm;
		readTm+=readPeriod-1; readTm/=readPeriod; readTm*=readPeriod;
		sendTm+=sendPeriod-1; sendTm/=sendPeriod; sendTm*=sendPeriod;

		log.info("Next send in %d secs",sendTm-tm);
		log.info("Next read in %d secs",readTm-tm);
		for(;!Thread.currentThread().isInterrupted();) {
			SysUtil.delay(1000);
			if (jca.isStopped()) {
				log.debug("jca is stopped");
				break;
			}
			tm=System.currentTimeMillis()/1000;
			if (readTm<=tm){
				done=0;
				log.info("start measure tm=%d",readTm);
				for (int i=0; i<chndefs.size(); ++i){
					SaveChannelDef c=chndefs.get(i);
					c.tm=readTm;
					jca.readPv(c.pv);
				}
				readTm+=readPeriod;
				if (readTm<tm){
					readTm=tm;
					readTm+=readPeriod-1; readTm/=readPeriod; readTm*=readPeriod;
				}
				log.info("Next read in %d secs",readTm-tm);
			}
			if (sendTm<tm && done==chndefs.size()){
				sendToDB();
				sendTm+=sendPeriod;
				if (sendTm<readTm+5) sendTm=readTm+5;
				log.info("Next send in %d secs",sendTm-tm);
			}
		}
	}

	static class SaveChannelDef extends ChannelDef {
		public SaveChannelDef(String nm) { super(0,nm); }
		public void resetData() {sum=0;cnt=0;}
		public void addData(float[] v) {
			sum+=v[0];
			++cnt;
		}
		public float getData() {return (float)(sum/cnt);}
		public long getTime() {return tm;}
		long tm;
		private double sum=0.0;
		private int cnt=0;
	}
}
