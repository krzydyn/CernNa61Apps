package cern.lv;

import java.awt.event.ActionListener;

public interface LVControlInterface {
	public SectorGroup getBindings(String id);
	public boolean showConfim(String msg);
	public void addChannelListener(ActionListener al);
	public void removeChannelListener(ActionListener al);
	public boolean isLocked();
	public void writeChannelStatus(LVChannel c,int st);
	//public void writeSectorStatus(final SectorGroup.Sector s,int st);
	//public void writeGroupStatus(final SectorGroup g,int st);
}
