package cern.lv;

import java.util.ArrayList;

import sys.Logger;


public class SectorGroup {
	static Logger log=Logger.getLogger();
	private final ArrayList<Sector> sectors=new ArrayList<Sector>();
	private String name="";
	public SectorGroup() {name=null;}
	public SectorGroup(String n) {name=n;}
	public String getName(){return name==null?"":name;}
	public void setName(String n){name=n;}
	public void add(Sector c) { sectors.add(c);c.grp=this;}
	public int size() { return sectors.size(); }
	public Sector get(int i) { return sectors.get(i); }

	public static class Sector {
		public SectorGroup grp;
		public final String name;
		public final ArrayList<LVChannel> chnlist;
		public Sector(String n,ArrayList<LVChannel> chnlist){
			this.name=n;
			this.chnlist=chnlist;
			for (int i=0; i<chnlist.size(); ++i)
				chnlist.get(i).sector=this;
		}
	}
}
