package cern.lv;

import java.io.IOException;
import java.util.ArrayList;

import channel.ChannelData;
import epics.JcaConn;

public class LVConnectorJca extends JcaConn {
	final static private String pciErrPVfmt="LV:DaisyChain:%d:Error";

	private final ArrayList<Integer> pciErr=new ArrayList<Integer>();
	private final ArrayList<String> pciNames=new ArrayList<String>();

	public LVConnectorJca(){
		pciNames.add("MTPC create");
		pciNames.add("VTPC create");
	}
	
	public String getName(){return "LV-EPICS";}

	public void getErrorMsg(StringBuilder b){
		for (int i=0; i<pciErr.size(); ++i){
			int pci=pciErr.get(i);
			b.append("VME crate "+pci+" is not responding\n");
		}
		if (pciErr.size()>0){
			b.append("\n> Tip1: check if VME crates are switched ON <");
			b.append("\n> Tip2: check if cooling system is working <");
			b.append("\n------------------------------------------");
			b.append("\n Call DCS or DAQ expert ");
		}
	}
	public void resetErros() throws IOException {
		for (int pci=0; pci<2; ++pci){
			writePv(String.format(pciErrPVfmt,pci), 0);
		}
	}
	protected void connect() throws IOException {
		super.connect();
		try{
			pciErr.clear();
			checkHW(null);
		}catch (Throwable e) {
			log.debug(e);
			throw new IOException("Can't connect EPICS at "+addr,e);
		}
		//turn on monitoring on server
		writePv("LV:VTPC1:Monitor",1);
		writePv("LV:VTPC2:Monitor",1);
		writePv("LV:MTPCL:Monitor",1);
		writePv("LV:MTPCR:Monitor",1);
		writePv("LV:GTPC:Monitor",1);
		writePv("LV:LMPD:Monitor",1);
		writePv("LV:PS_C:Monitor",1);
		writePv("LV:PS_C:Monitor",1);
	}
	public void readChn(ChannelData c) {
		LVChannel.setNamePv((LVChannel)c);
		super.readChn(c);
	}
	public void writeChn(ChannelData c) {
		LVChannel.setNamePv((LVChannel)c);
		super.writeChn(c);
	}
	protected void checkHW(StringBuilder b) throws IOException {
		for (int pci=0; pci<2; ++pci){
			/*String pv=String.format(pciErrPVfmt,pci);
			Object o=readPv(pv);
			if (o==null || getIntValue(o)!=0){
				if (!pciErr.contains(pci)) {
					pciErr.add(pci);
					log.info("pci %d stopped to work",pci);
				}
			}
			else {
				if (pciErr.contains(pci)){
					pciErr.remove(pciErr.indexOf(pci));
					log.info("pci %d started to work");
				}
			}*/
		}
	}
}
