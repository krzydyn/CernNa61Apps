package cern.pulser;

import com.link.AbstractLink.LinkStateListener;

import cern.pulser.Pulser.Firmware;
import cern.pulser.Pulser.Module;
import cern.pulser.Pulser.Voltage;
import cern.pulser.Pulser.Waveform;
import conn.SyncConn;

public class PulserConnector extends SyncConn {
	private final static int DEFAULT_PORT=-1;
	private final Pulser dev=new Pulser();
	private final StringBuilder buf=new StringBuilder();

	public PulserConnector(LinkStateListener l) {
		link.setStateListener(l);
		persistent=false;
		dev.setLink(link);
	}
	@Override
	protected int defaultPort() {return DEFAULT_PORT;}

	public Firmware getFirmware(){return dev.getFirmware();}
	public Voltage getVoltage(){return dev.getVoltage();}
	public int getModuleMask(){return dev.getModuleMask();}
	public boolean hasModule(int slot){return dev.hasModule(slot);}
	public Module getModule(int slot){return dev.getModule(slot);}
	public int getWaveformNr(){return dev.getWaveformNr();}
	public Waveform getWaveform(){return dev.getWaveform();}
	public int getClockMask(){return dev.getClockMask();}

	public void readFirmware() {
		request(new Command(Pulser.CMD_FIRMVARE){
			@Override
			public int execute(){return dev.readFirmware(buf);}
		});
	}
	public void readVoltage() {
		request(new Command(Pulser.CMD_READ_VMON){
			@Override
			public int execute(){return dev.readVoltage(buf);}
		});
	}
	public void resetVoltage() {
		request(new Command(Pulser.CMD_RST_VMON){
			@Override
			public int execute(){return dev.resetVoltage(buf);}
		});
	}
	/**
	 * read modules status in a crate (0=online,1=offline)
	 * @return bitmask: 0|m7|.....|m1
	 */
	public void readModules(){
		request(new Command(Pulser.CMD_MODULES){
			@Override
			public int execute(){return dev.readModules(buf);}
		});
	}
	/**
	 * read channels output status (0=closed, 1=open)
	 * @param slot module slot nr (0-6)
	 * @return bitmask: 0|ch9|.....|ch1
	 */
	public void readChannels(final int slot){
		request(new Command(Pulser.CMD_CHANNELS_GET|(slot<<16)){
			@Override
			public int execute(){return dev.readChannels(buf,slot);}
		});
	}
	/**
	 * write channels output status (0=closed, 1=open)
	 * @param slot module slot nr (0-6)
	 * @return bitmask: 0|ch9|.....|ch1
	 */
	public void writeChannels(final int slot,final int m){
		request(new Command(Pulser.CMD_CHANNELS_SET|(slot<<16)){
			@Override
			public int execute(){return dev.writeChannels(buf,slot,m);}
		});
	}
	/**
	 * read module power status: failure (1=occurd), current(1=ok)
	 * @param slot module slot nr (0-6)
	 * @return bitmask: 0|...|failure|current
	 */
	public void readPowerStatus(final int slot){
		request(new Command(Pulser.CMD_READ_POWER|(slot<<16)){
			@Override
			public int execute(){return dev.readPowerStatus(buf,slot);}
		});
	}
	public void readWaveformNr(){
		request(new Command(Pulser.CMD_GET_WAVE){
			@Override
			public int execute(){return dev.readWaveformNr(buf);}
		});
	}
	public void writeWaveformNr(final int nr){
		request(new Command(Pulser.CMD_SET_WAVE){
			@Override
			public int execute(){return dev.writeWaveformNr(buf,nr);}
		});
	}
	/**
	 * read stored waveform data from location nr
	 * @param nr waveform number (0-3)
	 * @return
	 */
	public void readWaveform(final int nr){
		request(new Command(Pulser.CMD_READ_WAVE|(nr<<16)){
			@Override
			public int execute(){return dev.readWaveform(buf,nr);}
		});
	}
	public void writeWaveform(final int nr){
		request(new Command(Pulser.CMD_WRITE_WAVE|(nr<<16)){
			@Override
			public int execute(){
				return dev.writeWaveform(buf,nr);}
		});
	}
	public void readClockMask(){
		request(new Command(Pulser.CMD_READ_CLOCK){
			@Override
			public int execute(){return dev.readClockMask(buf);}
		});
	}
	public void writeClockMask(final int m) {
		request(new Command(Pulser.CMD_INTERNAL_TOGGLE){
			@Override
			public int execute(){return dev.writeClockMask(buf,m);}
		});
	}

	int idle_start=0;
	@Override
	protected void idle(){
		/*if (idle_start>=7) idle_start=0;
		int i;
		for (i=idle_start; i<7; ++i)
			if (hasModule(i)) {
				readPowerStatus(i);
				readChannels(i);
				break;
			}
		idle_start=i+1;*/
	}
}