package cern.lv;

import channel.ChannelData;
import channel.ChannelDef;
import common.Logger;
import common.StrUtil;

public class LVChannel extends ChannelData {
	final static private String readChnNPVfmt="LV:%s:%s:%d:";
	final static private String readChn1PVfmt="LV:%s:1:%d:";

	static Logger log=Logger.getLogger();
	public SectorGroup.Sector sector;
	final int pci;
	final int crate;
	final int addr;
	final int lvc; //physical number sticked on rack

	int setStatus=-1;
	int status=-1;
	int alarm=0;
	//int statusLast=-1;
	//int alarmLast=0;

	//String pv; //EPIS support

	public LVChannel(int pci,int crate,int addr,int lvc){
		//super(new ChannelDef(String.format("lv.%d",lvc),null));
		super(new ChannelDef(0));
		this.pci=pci;
		this.crate=crate;
		this.addr=addr;
		this.lvc=lvc;
	}
	public boolean equals(Object o){
		if (o instanceof LVChannel){
			LVChannel l=(LVChannel)o;
			return l.pci==pci && l.crate==crate && l.addr==addr;
		}
		log.error("Object %s not supported by LVChannel",o.getClass().getName());
		return false;
	}
	public boolean isON(){
		if (status<0) return false; //unknown
		else if ((alarm&(1<<15))!=0) return false;//not avail.
		else if ((status&(1<<14))==0) return false;//off
		return true;
	}
	
	static public void setNamePv(LVChannel c){
		if (c.addr>=0 && c.def.pv==null){
			if (c.sector==null || c.sector.grp==null) {
				return ;
			}
			int idx=c.sector.chnlist.indexOf(c);
			String grpname=StrUtil.remove(c.sector.grp.getName(),new String[]{"-","h","s"});
			String secname=c.sector.name;
			c.def.name=grpname+"."+secname+"["+(idx+1)+"]";

			//special sector name mapping for pv creation
			if (secname.equals("CW")) secname="Cooling";
			else if (secname.equals("CB")) secname="PS_C";
			if (c.sector.chnlist.size()>1) {
				c.def.pv=String.format(readChnNPVfmt,grpname,secname,idx+1);
			}
			else {
				c.def.pv=String.format(readChn1PVfmt,secname,idx+1);
			}
		}
	}


	/*static public class State{
		int setStatus=0;
		int status=-1;
		int alarm=0;
	}*/
	
}
