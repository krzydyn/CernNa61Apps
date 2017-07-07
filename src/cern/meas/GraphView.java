package cern.meas;
/**
*
* @author KySoft, Krzysztof Dynowski
*
*/

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileFilter;

import plot.PlotTime;
import plot.PlotXY;
import plot.PlotXY.ChannelParams;
import plot.PlotXY.PlotListener;
import channel.ChannelDef;
import channel.ui.ChannelSelector;
import common.ui.CheckBoxIcon;
import common.ui.UiUtils;

@SuppressWarnings("serial")
public class GraphView extends MeasView implements PlotListener,ActionListener {
	final static SimpleDateFormat tmf1=new SimpleDateFormat("yyyy-MM-dd HH:mm");
	final static SimpleDateFormat tmf2=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	final static double tmRangeDefault=1.0;
	final static Font titlefont=UIManager.getFont("Panel.font").deriveFont(Font.BOLD,10f);
	final static Font namefont1=titlefont.deriveFont(Font.BOLD,16f);
	final static Font namefont2=titlefont.deriveFont(Font.BOLD,14f);
	final static Font valuefont=titlefont.deriveFont(Font.BOLD,24f);
	final static Insets plotIns=new Insets(0,0,0,3);
	final static Border raisedBorder=BorderFactory.createBevelBorder(BevelBorder.RAISED,Color.GRAY,Color.DARK_GRAY);
	static final Border chkboxBorder=BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
	final static Border emptyBorder=BorderFactory.createEmptyBorder(0, 2, 0, 2);
	static private final Icon selicon=new CheckBoxIcon(7,7);
	/*static {
		URL u=ResourceExt.getResource("res/fonts/7seg.ttf");
		try{
			InputStream is=u.openStream();
			f=Font.createFont(Font.TRUETYPE_FONT,is);
			f=f.deriveFont(Font.BOLD,24f);
			is.close();
			valuefont=f;
		}catch (Exception e) {log.error(e.toString());}
	}*/
	final private static JFileChooser chooser = new JFileChooser();
	static {
		//chooser.setCurrentDirectory(new File("."));
		chooser.setFileFilter(new FileFilter(){
			public boolean accept(File f) {
				return f.isDirectory()||f.getName().endsWith(".csv");
			}
			public String getDescription() {
				return "CSV files";
			}
		});
	}

	private static List<ChannelDef> availDefs=null;
	private JPanel title=new JPanel(new GridBagLayout());
	private JLabel lname=new JLabel(),ltime=new JLabel("N/A"),lvalue=new JLabel("N/A");
	private PlotTime plot=new PlotTime();
	private PlotListener caretLitener;
	final boolean hist;
	boolean tmphist;

	Thread running=null;

	public GraphView(boolean hist) {
		setLayout(new GridBagLayout());
		setOpaque(true);

		this.hist=hist;
		title.setBorder(emptyBorder);
		
		lname.setHorizontalAlignment(SwingConstants.CENTER);
		lname.setFont(hist?namefont1:namefont2);
		lname.setBorder(raisedBorder);
		ltime.setHorizontalAlignment(SwingConstants.CENTER);
		ltime.setFont(hist?namefont1:namefont2);
		ltime.setBorder(raisedBorder);
		//ltime.setWrapStyleWord(true);
		lvalue.setHorizontalAlignment(SwingConstants.CENTER);
		lvalue.setFont(valuefont);
		lvalue.setForeground(Color.GRAY);
		lvalue.setBorder(raisedBorder);
		plot.setRange(tmRangeDefault,0);
		plot.setPlotListener(this);
		plot.setInsets(plotIns);

		tmf1.setTimeZone(TimeZone.getDefault());
		tmf2.setTimeZone(TimeZone.getDefault());

		JPanel p;
		GridBagConstraints constr = new GridBagConstraints();
		constr.fill = GridBagConstraints.BOTH;
		constr.anchor = GridBagConstraints.LINE_START;
		constr.gridwidth = GridBagConstraints.REMAINDER;
		constr.weighty=constr.weightx=1;

		//plot area (with title)
		p=new JPanel(new BorderLayout());
		p.setFocusable(false); //otherwise GV lose focus
		p.add(title,BorderLayout.NORTH);
		p.add(plot,BorderLayout.CENTER);
		p.setBorder(raisedBorder);
		add(p,constr);

		constr.weighty=0; constr.weightx=0.2;
		constr.gridwidth = 1;
		constr.ipadx=2;
		add(lname,constr);
		add(ltime,constr);
		constr.gridwidth = GridBagConstraints.REMAINDER;
		add(lvalue,constr);

		JMenuItem mi;
		if (availDefs!=null) {
			mi=new JMenuItem("Add channel...");
			mi.setActionCommand("addchn");
			mi.addActionListener(this);
			plot.addPopupItem(mi);
		}
		if (hist) {
			setEnabled(true);
			setFocusable(true); //allow keyboard events
			mi=new JMenuItem("Save as...");
			mi.setActionCommand("saveas");
			mi.addActionListener(this);
			plot.addPopupItem(mi);
			addKeyListener(new KeyAdapter(){
				public void keyPressed(KeyEvent ev){
					if (ev.isControlDown()&&ev.getKeyCode()==KeyEvent.VK_S)
						saveasAction();
					else plot.keyPressed(ev);
				}
			});
		}
		else {
			mi=new JMenuItem("Show history");
			mi.setActionCommand("hist");
			mi.addActionListener(this);
			plot.addPopupItem(mi);
			plot.setCheckVisible(true);
			plot.setSelMode(PlotXY.SELECTION_NONE);
		}
	}
	public void actionPerformed(ActionEvent ev){
		final String cmd=ev.getActionCommand();
		if ("saveas".equals(cmd)) saveasAction();
		else if ("hist".equals(cmd)) {
			MeasMonitor.showHistory(vci.defs.get(0));
		}
		else if ("addchn".equals(cmd)) {
			ChannelSelector p = new ChannelSelector(availDefs,vci.defs);
			if (p.getItems()<=0) {
				UiUtils.messageBox(null, "Adding Channel", "No more elements available", JOptionPane.WARNING_MESSAGE);
				return ;
			}
			UiUtils.showDialog(null, p, "Channel selector");
			if (p.getResult() > 0) {
				vci.defs.add(p.getSelectedItem());
				setChnInfo(vci);
			}
		}
		else if (cmd.startsWith("chn:")) {
			int chn=Integer.parseInt(cmd.substring(4));
			chn=plot.selectChannel(chn);
			lname.setText(vci.defs.get(chn).name);
		}
		else log.warn("%s: not handled '%s'",getClass().getName(),cmd);
	}
	//public void setLimis(float[] l, float[] u) {
	//	plot.setLimis(l, u);
	//}
	private void saveasAction(){
		if (running!=null) return ;
		File f=new File(vci.defs.get(0).name.replace('/', '_')+".csv");
		chooser.setSelectedFile(f);
		int r=chooser.showSaveDialog(getParent());
		if (r!=JFileChooser.APPROVE_OPTION) return ;
		f=chooser.getSelectedFile();
		Rectangle2D r2d=plot.getView();
		long fr=plot.double2time(r2d.getMinX())-300;
		long to=plot.double2time(r2d.getMaxX())+60;
		final DataSaver p=new DataSaver(vci,f,fr,to);
		new Thread(new Runnable(){
			public void run(){
				running=Thread.currentThread();
				try{p.save();}
				finally{running=null;}
			}
		}).start();
		UiUtils.showWindow(GraphView.this,p,"Save to file");
	}

	public void setCaretListener(PlotListener l){
		caretLitener=l;
		if (caretLitener!=null) caretLitener.caretChanged(plot);
	}
	public void setRange(double x,double y){plot.setRange(x, y);}
	public void clear(){plot.clear();}

	public void setChnInfo(ViewChannelsInfo pi) {
		super.setChnInfo(pi);
		//if (pi.defs.get(0).id==1)
		//	log.debug("color=%s",plot.getParams(0).color.toString());
		long tm=System.currentTimeMillis()/1000;
		int defs=vci.defs.size();		
		
		if (hist) plot.setAutoBounds(PlotXY.AUTOBOUNDS_NONE,PlotXY.AUTOBOUNDS_SCALE);
		else plot.setAutoBounds(PlotXY.AUTOBOUNDS_MOVE,PlotXY.AUTOBOUNDS_NONE);
		
		title.removeAll();
		plot.clear();
		plot.selectChannel(0);
		
		ChannelDef def;
		GridBagConstraints constr = new GridBagConstraints();
		Insets lins=new Insets(0, 0, 0, 2);
		Insets lori=constr.insets;
		constr.fill = GridBagConstraints.BOTH;
		constr.anchor = GridBagConstraints.LINE_START;
		constr.weighty=0;
		
		ButtonGroup bgrp=null;
		if (defs>1) bgrp=new ButtonGroup();
		for (int i=0; i<defs; ++i) {
			JLabel l=new JLabel();
			JCheckBox chkbx=null;
			ChannelParams chp=plot.getParams(i);
			if (bgrp!=null){
				chkbx=new JCheckBox(selicon);
				chkbx.setBorder(chkboxBorder);
				chkbx.setBorderPainted(true);//default is false
				chkbx.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				chkbx.setBackground(chp.color);

				bgrp.add(chkbx);
				chkbx.addActionListener(this);
				chkbx.setActionCommand("chn:"+i);
				if (bgrp.getButtonCount()==1)
					bgrp.setSelected(chkbx.getModel(), true);
			}
			l.setHorizontalAlignment(SwingConstants.RIGHT);
			l.setFont(titlefont);
			constr.weightx=1;
			constr.gridwidth = chkbx!=null ? 1:GridBagConstraints.REMAINDER;
			constr.insets=lins;
			title.add(l,constr);
			if (chkbx!=null) {
				constr.weightx=0;
				constr.gridwidth = GridBagConstraints.REMAINDER;
				constr.insets=lori;
				title.add(chkbx,constr);
			}
			
			def=vci.defs.get(i);
			l.setText(def.descr);
			this.setToolTipText(String.format("%s [%d]",def.descr,def.id));
		}
		
		if (defs>0) {
			def=vci.defs.get(plot.getSelectedChannel());
			plot.setUnit("time",def.unit);
			lname.setText(def.name);
			plot.setLimis(def.lLimits, def.uLimits);
		}
		
		Rectangle2D v=plot.getView();
		plot.setView(plot.time2double(tm)-v.getWidth(),v.getY(),v.getWidth(),v.getHeight());
	}
	public void currentTime(long tm){
		Rectangle2D v=plot.getView();
		v.setRect(plot.time2double(tm)-v.getWidth(),v.getY(),v.getWidth(),v.getHeight());
		plot.setCaret(v.getMaxX(),plot.getCaret().getY());
	}
	public void addValue(ChannelDef def,long tm,float v) {
		int i=vci.defs.indexOf(def);
		if (i<0) return ;
		plot.addPoint(i,tm,(double)v);
	}
	//public void addPoint(double x,double y) { plot.addPoint(x, y); }
	public void addValues(ChannelDef def,List<PntData> v) {
		int chn=vci.defs.indexOf(def);
		if (chn<0 || v.size()==0) return ;

		boolean empty=plot.getPoints(chn)==null || plot.getPoints(chn).size()==0;
		boolean noauto=false,fitY=false;
		if (hist){
			if (empty)
				plot.setAutoBounds(PlotXY.AUTOBOUNDS_NONE,PlotXY.AUTOBOUNDS_SCALE);
			if (v.get(v.size()-1).tm+300>plot.double2time(plot.getView().getMaxX()))
				noauto=true;
		}
		else if (plot.getView().getHeight()<0.1) {
			fitY=true;
		}
		List<Point2D> pnts=new ArrayList<Point2D>(v.size());
		for (int i=0; i<v.size(); ++i) {
			PntData pd=v.get(i);
			Point2D p=new Point2D.Float((float)plot.time2double(pd.tm), pd.value);
			pnts.add(p);
		}
		plot.addPoints(chn,pnts);
		if (fitY) plot.autoFitY2();
		if (noauto)
			plot.setAutoBounds(PlotXY.AUTOBOUNDS_NONE,PlotXY.AUTOBOUNDS_NONE);
	}
	public void caretChanged(PlotXY p) {
		if (p!=plot) return ;
		long tm=plot.getCaretTime();
		double v=plot.getCaret().getY();
		if (hist)
			ltime.setText(tmf2.format(new Date(tm*1000)));
		else
			ltime.setText(tmf1.format(new Date((tm+30)*1000)));
		if (Double.isNaN(v)) {
			lvalue.setForeground(Color.GRAY);
			lvalue.setText("N/A");
		}
		else {
			lvalue.setForeground(Color.BLACK);
			double g=plot.getView().getHeight()/50;
			if (hist) g/=10;
			lvalue.setText(PlotXY.format(v,plot.gridY(g)));
		}
		if (caretLitener!=null) caretLitener.caretChanged(plot);
		//requestFocusInWindow();
	}
	public void viewChanged(PlotXY pl,Rectangle2D oldview,int opt) {
		if (vci==null) return ;
		if ((opt&(PlotXY.CHANGED_X|PlotXY.CHANGED_W))==0) { return ; }
		Rectangle2D r=plot.getView();
		long fr=plot.double2time(r.getMinX())-6*60;
		long to=plot.double2time(r.getMaxX())+5*60;
		
		if (!hist){
			if (to+300<System.currentTimeMillis()/1000){
				if (!tmphist){tmphist=true;log.debug("tmphist ON");}
			}
			else {
				if (tmphist){
					tmphist=false;log.debug("tmphist OFF");
					r.setFrame(oldview);
				}
				currentTime(System.currentTimeMillis()/1000);
				fr=plot.double2time(r.getMinX())-6*60;
				to=plot.double2time(r.getMaxX())+5*60;
			}
		}

		Point2D p;

		List<Point2D> pnts;
		for (int i=0; (pnts=plot.getPoints(i))!=null; ++i) {
			if ((opt&PlotXY.CHANGED_W)==0 && pnts.size()>0){
				if (r.getMinX()<oldview.getMinX()) to=plot.double2time(oldview.getMinX());
				else fr=plot.double2time(oldview.getMaxX());
				while (pnts.size()>1){
					p=pnts.get(1);
					if (p.getX()>plot.getView().getMinX()) break;
					pnts.remove(0);
				}
				while (pnts.size()>1){
					p=pnts.get(pnts.size()-2);
					if (p.getX()<plot.getView().getMaxX()) break;
					pnts.remove(pnts.size()-1);
				}
			}
			else pnts.clear();			
		}
		//log.debug("getHist: "+df.format(new Date(fr*1000))+","+df.format(new Date(to*1000)));
		double step=1.0/60.0; //=1min
		if (1000*step<r.getWidth()) step=r.getWidth()/1000;
		for (ChannelDef def:vci.defs) {
			vci.rcv.getHistory(getName(),def.id,fr,to,(long)(step*3600));	
		}
		if (step<1.0/10.0) step=1.0/10.0;//=6min
		plot.setXDelta(step);
	}
	public void selChanged(PlotXY plot){
		Rectangle2D r=plot.getSelection();
		plot.setView(r.getX(),r.getY(),r.getWidth(),r.getHeight());
		plot.clearSelection();
	}
	public void paintDone(PlotXY plot) {
	}
	public PlotTime getPlot(){return plot;}
	public void dispose(){
		plot.dispose();
		title.removeAll();
		title=null;
		ltime=null; lvalue=null;
		plot=null;
		super.dispose();
	}
	
	public static void setAvailDefs(List<ChannelDef> defs) {
		availDefs=defs;
	}
}
