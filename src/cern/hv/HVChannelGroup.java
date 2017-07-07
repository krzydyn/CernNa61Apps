package cern.hv;

import java.util.ArrayList;

import common.Logger;

public class HVChannelGroup {
	static Logger log=Logger.getLogger();
	private final ArrayList<HVChannel> chns=new ArrayList<HVChannel>();
	private String name="";
	public HVChannelGroup() {name=null;}
	public HVChannelGroup(String n) {name=n;}
	public String getName(){return name==null?"":name;}
	public void setName(String n){name=n;}
	public void add(HVChannel c) { chns.add(c); }
	public int size() { return chns.size(); }
	public HVChannel get(int i) { return chns.get(i); }
}
