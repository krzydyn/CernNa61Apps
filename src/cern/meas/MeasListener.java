package cern.meas;
/**
*
* @author KySoft, Krzysztof Dynowski
*
*/

import java.util.List;

import channel.ChannelDef;

public interface MeasListener {
	public void connected();
	public void disconnected();
	public void exception(Exception e);
	public void rcvdConfig(List<ChannelDef> defs);
	public void rcvdAlarmConfig(ChannelDef def);
	public void rcvdData(List<PntData> v);
	public void rcvdData(String key,List<PntData> v);
	public void rcvdMsg(String msg);
}
