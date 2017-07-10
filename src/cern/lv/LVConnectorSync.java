package cern.lv;

import java.io.IOException;
import java.util.ArrayList;

import channel.ChannelData;
import sys.StrUtil;
import sys.SysUtil;
import caen.CaenVME;
import conn.SyncConn;

/*
 * pairs (idx, crate) for CAENVME_Init:
 *  MTPC-R (0, 0)         A=0
 *  MTPC-L i GTPC (0, 1)  A=1
 *  VTPC-1 (1, 0)         A=0
 *  VTPC-2 (1, 1)         A=0
 * ------------------------------------------------
 * A: crate controller id (0..1)
 * B: crate number (0..6)
 * C: board number (1..24)
 *
 * status_address(R/W) = (A << 10) + (B << 7) + (C << 2)
 * alarm_address = alarm_address(R) = status_address + 2
 *
 * status bits
 *   bit0-11 chn on flag
 *   bit12   CT on
 *   bit13   -2V on
 *   bit14   1=all channels off
 *   bit15   1=one channel off
 *
 * alarm bits
 *   bit0-11 chn alarm flag
 *   bit12   1=CT on alarm
 *   bit13   always =1
 *   bit14   always =1
 *   bit15   1=board not in create
 *   NOTE: bits 6,7 in alarm must be swapped
 *
 * CW (Cooling Water) channel:
 * status bits description:
 *   bit0: 0=VTPC1 cooling on
 *   bit1: 0=VTPC2 cooling on
 *   bit2: 0=MTPC(HR) cooling on
 *   bit3: 0=MTPC(SR) cooling on
 *
 */

public class LVConnectorSync extends SyncConn {
	final static int CW_VTPC1=1;
	final static int CW_VTPC2=2;
	final static int CW_MTPC_HR=4;
	final static int CW_MTPC_SR=8;

	private final ArrayList<Integer> pciErr=new ArrayList<Integer>();
	private final ArrayList<LVChannel> chnRead=new ArrayList<LVChannel>();
	private final ArrayList<LVChannel> chnWrite=new ArrayList<LVChannel>();
	private LVChannel cwChannel=null;

	public LVConnectorSync() {
		super(new CaenVME());
	}

	@Override
	public String getName(){return "LV-VME";}
	@Override
	public int defaultPort(){ return 31515; }

	public void getErrorMsg(StringBuilder b){
		for (int i=0; i<pciErr.size(); ++i){
			int pci=pciErr.get(i);
			b.append("DaisyChain "+pci+" is not responding\n");
		}
		if (pciErr.size()>0){
			b.append("\n> Tip1: check if VME crates are switched ON <");
			b.append("\n> Tip2: check if cooling system is working <");
		}
	}

	@Override
	public void readChn(ChannelData chn){
		LVChannel c = (LVChannel)chn;
		chnRead.add(c);
		if (cwChannel==null && c.sector.name.equals("CW"))
			cwChannel=c;
	}
	@Override
	public void writeChn(ChannelData chn){
		chnWrite.add((LVChannel)chn);
	}

	public void updateChannels() {
		msgq.put(new SyncConn.Command() {
			@Override
			protected int execute() {
				StringBuilder b = new StringBuilder();
				CaenVME link=(CaenVME)LVConnectorSync.this.link;
				for (int i=0; i<chnRead.size(); ++i) {
					LVChannel c=chnRead.get(i);
					int r=link.read(c.pci,c.crate,c.addr,b);
					if (r < 0) return r;
					c.status=SysUtil.getInt(StrUtil.bytes(b),2);
					r=link.read(c.pci,c.crate,c.addr+2,b);
					if (r < 0) return r;
					r=SysUtil.getInt(StrUtil.bytes(b),2);
					// alarm bits 6,7 are swapped in Hardware by mistake,
					// swap them now to fix it
					c.alarm=(r&0xFF3F)|((r&0x0080)>>1)|((r&0x0040)<<1);
				}
				return 0;
			}
		});
		msgq.put(new SyncConn.Command() {
			@Override
			protected int execute() {
				StringBuilder b = new StringBuilder();
				CaenVME link=(CaenVME)LVConnectorSync.this.link;
				for (int i=0; i<chnWrite.size(); ++i) {
					LVChannel c=chnWrite.get(i);
					link.write(c.pci,c.crate,c.addr+2, c.setStatus, b);
				}
				return 0;
			}
		});
	}

	@Override
	protected void connect() throws IOException{
		super.connect();
		CaenVME link=(CaenVME)this.link;
		int r;
		StringBuilder b=new StringBuilder();
		if ((r=link.readSWVersion(b)) <= 0){
			log.error("SWVersion: %d",r);
			throw new IOException("CaenVME library is not installed\n"+
				"Tip: contact "+addr+" administrator");
		}
		log.info("SWVersion: %s",b.toString());
	}

	private void updateCW(){
		if (cwChannel==null) return ;
		int cwStatus=0;
		for (int i=0; i<chnRead.size(); ++i){
			if (Thread.currentThread().isInterrupted()) break;
			LVChannel c=chnRead.get(i);
			if (c.isON()){
				String gn=c.sector.grp.getName();
				if (gn.endsWith("-1")) cwStatus|=CW_VTPC1;
				else if (gn.endsWith("-2")) cwStatus|=CW_VTPC2;
				else if (gn.endsWith("-Ls")) cwStatus|=CW_MTPC_SR;
				else if (gn.endsWith("-Rs")) cwStatus|=CW_MTPC_SR;
				else if (gn.endsWith("-Lh")) cwStatus|=CW_MTPC_HR;
				else if (gn.endsWith("-Rh")) cwStatus|=CW_MTPC_HR;
			}
		}
		cwChannel.setStatus=cwStatus^0xf;
		if (!chnWrite.contains(cwChannel)) chnWrite.add(cwChannel);
	}

	protected void checkHW(StringBuilder b) throws IOException{
		CaenVME link=(CaenVME)this.link;
		int r;
		for (int pci=0; pci<2; ++pci){
			r=link.readBoardVersion(pci,0,b);
			if (r>0){
				if (pciErr.contains(pci)){
					log.info("pci %d started to work, rel=%s",pci,b.toString());
					pciErr.remove(pciErr.indexOf(pci));
				}
			} else {
				log.warn("readBoardVersion(%d,0)=%d",pci,r);
				if (!pciErr.contains(pci)) pciErr.add(pci);
			}
		}
	}
	protected void send(StringBuilder b) throws IOException{
		CaenVME link=(CaenVME)this.link;
		log.info("Write channels %d",chnWrite.size());
		for (int i=0; i<chnWrite.size(); ++i){
			if (Thread.currentThread().isInterrupted()) break;
			LVChannel c=chnWrite.get(i);
			if (pciErr.contains(c.pci)){
				chnWrite.remove(i); --i;
				continue;
			}
			log.info("write chn(%d,%d,0x%04X) 0x%04X",c.pci,c.crate,c.addr,c.setStatus);
			int r=link.write(c.pci,c.crate,c.addr,c.setStatus,b);
			if (r<0) {
				log.error("write(pci=%d,cr=%d)=%d",c.pci,c.crate,r);
				throw new IOException("caen.write");
			}
			if (r==0){
				log.warn("write(pci=%d,cr=%d)=%d",c.pci,c.crate,r);
				c.setStatus=c.status=-1; pciErr.add(c.pci);
				continue;
			}
		}
		chnWrite.remove(cwChannel);
	}
	protected void recv(StringBuilder b) throws IOException{
		CaenVME link=(CaenVME)this.link;
		int r;
		log.info("Read channels %d",chnRead.size());
		for (int i=0; i<chnRead.size(); ++i){
			if (Thread.currentThread().isInterrupted()) break;
			LVChannel c=chnRead.get(i);
			if (pciErr.contains(c.pci)) {
				c.status=c.alarm=0;
				continue;
			}
			//c.statusLast=c.status; c.alarmLast=c.alarm;
			r=link.read(c.pci,c.crate,c.addr,b);
			if (r<0) {
				log.error("read(pci=%d,cr=%d)=%d",c.pci,c.crate,r);
				throw new IOException("caen.read");
			}
			if (r==0){
				log.warn("read(pci=%d,cr=%d)=%d",c.pci,c.crate,r);
				c.setStatus=c.status=-1; pciErr.add(c.pci);
				continue;
			}
			c.status=SysUtil.getInt(StrUtil.bytes(b),2);
			r=link.read(c.pci,c.crate,c.addr+2,b);
			if (r<0) {
				log.error("read(pci=%d,cr=%d)=%d",c.pci,c.crate,r);
				throw new IOException("caen.read");
			}
			if (r==0){
				c.setStatus=c.status=-1; pciErr.add(c.pci);
				continue;
			}
			r=SysUtil.getInt(StrUtil.bytes(b),2);
			// alarm bits 6,7 are swapped in Hardware by mistake,
			// swap them now to fix it
			c.alarm=(r&0xFF3F)|((r&0x0080)>>1)|((r&0x0040)<<1);

			if (c==cwChannel){
				c.status=c.setStatus;
				//c.alarm&=0x7fff;
			}

			if (chnWrite.size()>0 && chnWrite.indexOf(c)>=0){
				if ((c.alarm&(1<<15))!=0) chnWrite.remove(c);
				else if (((c.setStatus^c.status)&0xfff)==0)
					chnWrite.remove(c);
				else {
					log.debug("Chn(%d,%d,0x%04X) 0x%04X!=0x%04X",c.pci,c.crate,c.addr,c.status,c.setStatus);
					//if (((c.setStatus^c.status)&0xf000)==0) chnWrite.remove(c);
				}
			}
			else if (c.setStatus>=0 && c.setStatus!=c.status)
				chnWrite.add(c);
		}
		updateCW();
	}
}
