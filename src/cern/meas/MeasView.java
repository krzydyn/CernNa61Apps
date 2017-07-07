package cern.meas;
/**
*
* @author KySoft, Krzysztof Dynowski
*
*/

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import channel.ChannelDef;
import common.Logger;

@SuppressWarnings("serial")
abstract public class MeasView extends JPanel {
	static final Logger log=Logger.getLogger();
	protected ViewChannelsInfo vci;
	public MeasView() { setName(Integer.toHexString(hashCode())); }
	public void setChnInfo(ViewChannelsInfo pi){this.vci=pi;vci.view=this;}
	abstract public void addValue(ChannelDef def,long tm,float v);
	abstract public void addValues(ChannelDef def,List<PntData> v);
	public void currentTime(long tm){}
	public void dispose() { vci=null; setName(null); }

	static class ViewChannelsInfo {
		public ViewChannelsInfo(ChannelDef def,MeasConnector rcv,MeasView view) {
			this.defs.add(def);this.rcv=rcv;this.view=view;
		}
		public ViewChannelsInfo() {}
		final List<ChannelDef> defs=new ArrayList<ChannelDef>();
		MeasConnector rcv;
		MeasView view;
	}
	public void setLimis(float[] l, float[] u) {}
}
