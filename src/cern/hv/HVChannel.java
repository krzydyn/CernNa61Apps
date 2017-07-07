package cern.hv;

public class HVChannel {
	public final String name;
	public final int slot,chn;
	public HVChannel(String n,int slot,int chn)
	{
		this.name=n;
		this.slot=slot; this.chn=chn;
	}
}
