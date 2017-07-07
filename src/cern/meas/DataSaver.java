package cern.meas;
/**
*
* @author KySoft, Krzysztof Dynowski
*
*/

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.io.File;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import cern.meas.MeasView.ViewChannelsInfo;
import channel.ChannelDef;
import common.Logger;
import common.SysUtil;

@SuppressWarnings("serial")
public class DataSaver extends JPanel {
	Logger log=Logger.getLogger();

	final static SimpleDateFormat df=new SimpleDateFormat("yyyy-MM-dd HH:mm");

	DataChanel chn;
	File file;
	long tmFr,tmTo,tmCur;

	JProgressBar pbar;
	PrintStream out=null;
	long tmo;

	public DataSaver(ViewChannelsInfo pi,File f,long fr,long to) {
		super(new BorderLayout());
		df.setTimeZone(TimeZone.getDefault());
		chn=new DataChanel(this);
		chn.setChnInfo(pi);
		file=f;
		tmFr=fr;
		tmTo=to;
		add(new JLabel("<html>Retrieving data for "+pi.defs.get(0).toString()+"<br>"+
				"into file "+file.getName()),BorderLayout.NORTH);
		JPanel p=new JPanel();
		p.add(pbar=new JProgressBar(0,(int)(tmTo-tmFr)));
		pbar.setPreferredSize(new Dimension(150,20));
		pbar.setStringPainted(true);
		pbar.setIndeterminate(true);
		add(p,BorderLayout.CENTER);
	}
	public void save() {
		MeasMonitor.views.put(chn.getName(),chn);
		ChannelDef def=chn.vci.defs.get(0);
		try{
			out=new PrintStream(file);
			out.print(def.toString()+"\n");
			chn.vci.rcv.getHistory(chn.getName(),def.id, tmFr, tmTo, 60);
			pbar.setIndeterminate(false);
			for(tmo=SysUtil.timer_start(10*SysUtil.SECOND);!SysUtil.timer_expired(tmo);) {
				pbar.setValue((int)(tmCur-tmFr));
				SysUtil.delay(SysUtil.SECOND/2);
			}
		}
		catch (Exception e) { log.debug(e); }
		finally {
			pbar.setValue((int)(tmTo-tmFr));
			MeasMonitor.views.remove(chn.getName());
			chn.dispose();
			if (out!=null) {out.close();out=null;}
			((Window)getTopLevelAncestor()).dispose();
		}
	}
	public void save(List<PntData> v) {
		if (out==null) {
			log.debug("out is null");
			return ;
		}
		tmo=SysUtil.timer_start(2*SysUtil.SECOND);
		for (int i=0; i<v.size(); ++i) {
			PntData pd=v.get(i);
			out.printf("%s\t%d\t%.6g\n",df.format(new Date(pd.tm*1000)),pd.tm,pd.value);
			tmCur=pd.tm;
		}
		if (tmCur==tmTo) tmo=SysUtil.timer_get()-1;
	}

	static private class DataChanel extends MeasView
	{
		DataSaver dataSaver;
		public DataChanel(DataSaver dataSaver) {
			this.dataSaver=dataSaver;
		}
		public void addValue(ChannelDef def,long tm, float v) {}
		public void addValues(ChannelDef def,List<PntData> v) {
			dataSaver.save(v);
		}
	}

}
