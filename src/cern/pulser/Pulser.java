package cern.pulser;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import common.Errno;
import common.Logger;
import common.StrUtil;
import common.connection.link.DataLink;
import common.io.ByteInputStream;
import common.io.ByteOutputStream;
import common.io.IOUtils;

public class Pulser {
	static protected Logger log=Logger.getLogger();
	final protected static int
		CMD_FIRMVARE=0x02,CMD_INTERNAL_TOGGLE=0x04,CMD_SOURCE_TOGGLE=0x06,
		//CMD_SAVE_CFG=0x08,CMD_LOAD_CFG=0x0A,
		CMD_SET_WAVE=0x0C,CMD_GET_WAVE=0x0E,
		CMD_READ_VMON=0x10,CMD_RST_VMON=0x12,
		CMD_READ_WAVE=0x14,CMD_WRITE_WAVE=0x16,
		CMD_MODULES=0x18,
		CMD_CHANNELS_GET=0x1A,CMD_CHANNELS_SET=0x1C,
		CMD_READ_POWER=0x1E,CMD_RST_POWER=0x20,CMD_RST_FULL=0x22,
		CMD_READ_CLOCK=0x24,
		x=0;

	private DataLink link=null;
	public static class Module {
		private int reqmask=-1;
		private int chnmask=0;
		private int status=2;//error
		public int getChannelMask(){return reqmask>=0?reqmask:chnmask;}
		public int getStatus(){return status;}
	}

	public static class Firmware {
		public String ver;
		public String factory;
		public String name;
		void parse(DataInputStream is) throws IOException{
			ver=is.readUTF();
			factory=is.readUTF();
			name=is.readUTF();
		}
	}
	public static class Voltage {
		public int pwrFail;
		public int pwrAnalog;
		public String v1;
		public String v2;
		public String v3;
		public String v4;
		void read(DataInputStream is) throws IOException{
			pwrFail=readInt(is);
			v1=is.readUTF();
			v2=is.readUTF();
			v3=is.readUTF();
			v4=is.readUTF();
			pwrAnalog=readInt(is);
		}
	}
	public static class Waveform {
		int nr;
		public float[] data=new float[1375];
	}
	final private Module[] modules=new Module[7];

	final private Firmware firmware=new Firmware();
	final private Voltage voltage=new Voltage();
	final private Waveform wave=new Waveform();
	private int modmask=-1;
	private int wavenr=-1;
	private int clkmask=-1;

	static private int readInt(DataInputStream is) throws IOException{
		int l=is.readShort();
		if (is.available()<l) return -Errno.EIO;
		int r=0;
		for (int i=0; i<l; ++i){ r<<=8; r|=is.read()&0xff; }
		return r;	
	}
	
	public void setLink(DataLink l){link=l;}
	
	public Firmware getFirmware(){return firmware;}
	public Voltage getVoltage(){return voltage;}
	public int getModuleMask(){return modmask;}
	public boolean hasModule(int slot){
		if (slot<0||slot>6) return false;
		return (modmask&(1<<slot))!=0;
	}
	public Module getModule(int slot){return modules[slot];}
	public int getWaveformNr(){return wavenr;}
	public Waveform getWaveform(){return wave;}
	public int getClockMask(){return clkmask;}
	
	public int readFirmware(StringBuilder b) {
		b.setLength(0);
		int r=command(CMD_FIRMVARE,b);
		if (r<0) return r;
		try{
		DataInputStream dis=new DataInputStream(new ByteInputStream(b));
		firmware.parse(dis);
		}catch (Exception e) {}
		return b.length();
	}
	public int readVoltage(StringBuilder b) {
		b.setLength(0);
		int r=command(CMD_READ_VMON,b);
		if (r<0) return r;
		try{
		DataInputStream dis=new DataInputStream(new ByteInputStream(b));
		voltage.read(dis);
		}catch (Exception e) {}
		return b.length();
	}
	public int resetVoltage(StringBuilder b) {
		b.setLength(0);
		int r=command(CMD_RST_VMON,b);
		if (r<0) return r;
		return b.length();
	}
	/**
	 * read modules status in a crate (0=online,1=offline)
	 * @param b operation buffer
	 * @return bitmask: 0|m7|.....|m1
	 */
	public int readModules(StringBuilder b){
		b.setLength(0);
		int r=command(CMD_MODULES,b);
		if (r<0) return r;
		try{
		DataInputStream dis=new DataInputStream(new ByteInputStream(b));
		modmask=(readInt(dis)^0xff)&0x7f;
		for (int i=0; i<8; ++i)
			if (hasModule(i)) modules[i]=new Module();
		return modmask;
		}catch (Exception e) {}
		return -1;
	}
	/**
	 * read channels output status (0=closed, 1=open)
	 * @param b operation buffer
	 * @param slot module slot nr (0-6)
	 * @return bitmask: 0|ch9|.....|ch1
	 */
	public int readChannels(StringBuilder b,int slot){
		b.setLength(0);
		b.append((char)0);b.append((char)1); //length
		b.append((char)(slot&0xff));
		int r=command(CMD_CHANNELS_GET,b);
		if (r<0) return r;
		try{
			DataInputStream dis=new DataInputStream(new ByteInputStream(b));
			slot=readInt(dis);
			r=readInt(dis);
			modules[slot].chnmask=r;
			if (modules[slot].reqmask==r) modules[slot].reqmask=-1;
			return r;
		}catch (Exception e) {}
		return b.length();
	}
	/**
	 * write channels output status (0=closed, 1=open)
	 * @param b operation buffer
	 * @param slot module slot nr (0-6)
	 * @return bitmask: 0|ch9|.....|ch1
	 */
	public int writeChannels(StringBuilder b,int slot,int m){
		int r;
		b.setLength(0);
		b.append((char)0);b.append((char)1); //length
		b.append((char)(slot&0xff));
		b.append((char)0);b.append((char)2); //length
		b.append((char)((m>>8)&0xff));b.append((char)(m&0xff));
		modules[slot].reqmask=m;
		r=command(CMD_CHANNELS_SET,b);
		if (r<0) return r;
		return b.length();
	}
	/**
	 * read module power status: failure (1=occurd), current(1=ok)
	 * @param b operation buffer
	 * @param slot module slot nr (0-6)
	 * @return bitmask: 0|...|failure|current
	 */
	public int readPowerStatus(StringBuilder b,int slot){
		b.setLength(0);
		b.append((char)0);b.append((char)1); //length
		b.append((char)(slot&0xff));
		int r=command(CMD_READ_POWER,b);
		if (r<0) return r;
		try{
			DataInputStream dis=new DataInputStream(new ByteInputStream(b));
			slot=readInt(dis);
			r=readInt(dis);
			if (modules[slot]!=null) modules[slot].status=r;
			return r;
		}catch (Exception e) {}
		return -1;
	}
	public int readWaveformNr(StringBuilder b){
		b.setLength(0);
		int r=command(CMD_GET_WAVE,b);
		if (r<0) return r;
		try{
			DataInputStream dis=new DataInputStream(new ByteInputStream(b));
			wavenr=readInt(dis);
		}catch (Exception e) {log.error(e);}
		return b.length();
	}
	public int writeWaveformNr(StringBuilder b,int nr){
		if (nr<0||nr>5) return 0;
		b.setLength(0);
		b.append((char)0);b.append((char)1); //length
		b.append((char)(nr&0xff));
		int r=command(CMD_SET_WAVE,b);
		if (r<0) return r;
		return b.length();
	}
	/**
	 * read stored waveform data from location nr
	 * @param b operation buffer, returned data
	 * @param nr waveform number (0-3)
	 * @return 
	 */
	public int readWaveform(StringBuilder b,int nr){
		if (nr<0||nr>5) return 0;
		b.setLength(0);
		b.append((char)0);b.append((char)1); //length
		b.append((char)(nr&0xff));
		int r=command(CMD_READ_WAVE,b);
		if (r<0) return r;
		try{
			DataInputStream dis=new DataInputStream(new ByteInputStream(b));
			wave.nr=readInt(dis);
			int l=dis.readUnsignedShort()/2;
			if (l!=wave.data.length){
				log.error("wrong wave length %d!=%d",l,wave.data.length);
				wave.data=new float[l];
			}
			for (int i=0; i<l; ++i)
				wave.data[i]=dis.readUnsignedShort();
		}catch (Exception e) {log.error(e);}
		return b.length();
	}
	/**
	 * write waveform data into location nr
	 * @param b operation buffer, returned data
	 * @param nr waveform number (0-3)
	 * @return 
	 */
	public int writeWaveform(StringBuilder b,int nr){
		b.setLength(0);
		DataOutputStream bo=new DataOutputStream(new ByteOutputStream(b));
		try{
			bo.writeShort(1); //length
			bo.write(nr&0xff); 
			bo.writeShort(wave.data.length*4);
			for (int i=0; i<wave.data.length; ++i){
				int v=Math.round(wave.data[i]);
				//log.debug("wr data[%d]=%d",i,v);
				bo.write(StrUtil.bytes(String.format("%04X",v)));
			}
		}
		catch (Exception e) {log.debug(e);}
		finally {IOUtils.close(bo);}
		int r=command(CMD_WRITE_WAVE,b);
		if (r<0) return r;
		return b.length();
	}
	/**
	 * reset module power status
	 * @param b operation buffer
	 * @param slot module slot nr (0-6)
	 * @return 
	 */
	public int resetModulePower(StringBuilder b,int slot){
		b.setLength(0);
		b.append((char)0);b.append((char)1); //length
		b.append((char)(slot&0xff));
		int r=command(CMD_RST_POWER,b);
		if (r<0) return r;
		return b.length();
	}
	/**
	 * reset module full status
	 * @param b operation buffer
	 * @param slot module slot nr (0-6)
	 * @return 
	 */
	public int resetModuleFull(StringBuilder b,int slot){
		b.setLength(0);
		b.append((char)0);b.append((char)1); //length
		b.append((char)(slot&0xff));
		int r=command(CMD_RST_FULL,b);
		if (r<0) return r;
		return b.length();
	}
	public int readClockMask(StringBuilder b){
		b.setLength(0);
		int r=command(CMD_READ_CLOCK,b);
		if (r<0) return r;
		try{
			DataInputStream dis=new DataInputStream(new ByteInputStream(b));
			clkmask=readInt(dis);
		}catch (Exception e) {log.error(e);}
		return b.length();		
	}
	public int writeClockMask(StringBuilder b, int m) {
		int cm=(clkmask^m)&0x3;
		if (cm==0) return 0;
		int r=0;
		if ((cm&1)!=0){
			b.setLength(0);
			r=command(CMD_SOURCE_TOGGLE,b);
		}
		if ((cm&2)!=0){
			b.setLength(0);
			r=command(CMD_INTERNAL_TOGGLE,b);
		}
		return r;
	}

	public int command(int fn,StringBuilder b) {
		if (link==null) return -1;
		String msg=b.toString();
		log.debug("send[%d]: %02X %s",b.length(),fn,StrUtil.vis(msg));
		b.insert(0,(char)((fn<<8)&0xff));
		b.insert(1,(char)(fn&0xff));
		int r=link.send(b);
		if (r<0) return r;
		int trycnt=0;
		do{
			r=link.recv(b);
			++trycnt;
		}while (r==-Errno.EAGAIN);
		int fnr=-1;
		if (r>0){
			if (r<2) {
				log.error("invalid resp length=%d",r);
				fnr=r=-Errno.EIO;
			}
			else {
				fnr=b.charAt(0)&0xff;
				fnr<<=8; fnr|=b.charAt(1)&0xff;
				b.delete(0,2);
				if ((fnr&1)==0) {
					log.error("invalid resp cmd %x",fnr);
					return -Errno.EABORT;
				}
				if(fnr!=fn+1) log.error("asyn %02X!=%02X msg=%s",fnr,fn,StrUtil.hex(msg));
			}
		} else fnr=r;
		if (r<0) log.error("recv=%d: %02X msg=%s",r,fn,StrUtil.hex(msg));
		else if (trycnt>2) log.warn("(%d)recv[%d]: %02X %s",trycnt,b.length(),fnr,StrUtil.vis(b));
		else log.debug("recv[%d]: %02X %s",b.length(),fnr,StrUtil.vis(b,0,100));
		return fnr;
	}

}
