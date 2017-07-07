package cern.hv;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import caen.CaenNet;
import caen.HVModule;
import caen.HVModule.ChannelSettings;
import caen.SY527;
import common.Logger;
import common.SysUtil;
import common.Version;
import common.connection.Connection;
import common.connection.link.AbstractLink.LinkStateListener;
import common.crypt.Encryptor;
import common.ui.AudioPlayerWav;
import common.ui.UiUtils;
import common.util.Resource;

@SuppressWarnings("serial")
public class HVControl extends JPanel implements ActionListener, LinkStateListener {
	//final static boolean DEBUG=false;
	final static Logger log = Logger.getLogger();
	final static String appname = "HV Control";
	final static String appdescr = "High Voltage Control";
	final static String author = "Krzysztof.Dynowski@cern.ch";
	//final static String defaultAddr="na61dcs1.cern.ch:31514";
	final static String defaultAddr="localhost:31514";

	final public static int FASTREAD=5;

	private String addr;
	private final CaenNet caen = new CaenNet();
	private final SY527 sy527 = new SY527();
	private HVModule fastReadMod=null;

	private final ActionEvent onChn=new ActionEvent(this,0,"on");
	private final ActionEvent offChn=new ActionEvent(this,0,"off");

	private final ActionEvent updCfg = new ActionEvent(this, 0, "updCfg");
	private final ActionEvent updChn = new ActionEvent(this, 0, "updChn");
	private final ActionEvent updCtl = new ActionEvent(this, 0, "updCtl");

	//Arial,Verdana,Times
	private final Font fontTitle = new Font("Verdana", Font.BOLD, 25);
	private final JTabbedPane tabs;
	private final JButton conn,dconn,unlock;
	private final JLabel rx, tx;

	private final Properties bindings = new Properties();
	private final ArrayList<ActionListener> chnlners = new ArrayList<ActionListener>();
	private final ArrayList<ChannelSettings> tripped=new ArrayList<ChannelSettings>();

	private boolean locked = true;
	private long unlockTm=0; //seconds when auto-lock
	private Thread workloop=null;

	public HVControl() {
		super(new BorderLayout());
		tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);

		JPanel p;
		JButton b;
		JLabel l;
		p = new JPanel(null);
		p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
		ImageIcon icon=null;
		try{icon=new ImageIcon(Resource.getResourceURL("res/hvna61.jpg"));}
		catch (Exception e) {}
		p.add(Box.createHorizontalStrut(4));
		p.add(l=new JLabel("<html>"+appdescr+"<br><font size=2 color=#CC6600>by "+author,icon,JLabel.LEFT));
		l.setFont(fontTitle);
		p.add(Box.createHorizontalStrut(10));
		p.add(Box.createHorizontalGlue());{
			JPanel rtp = new JPanel(null);
			rtp.setLayout(new BoxLayout(rtp, BoxLayout.Y_AXIS));
			rtp.add(rx = new JLabel("RX"));
			rtp.add(tx = new JLabel("TX"));
			p.add(rtp);
			p.add(Box.createHorizontalStrut(5));
		}
		p.add(b = new JButton("Connect"));
		conn=b;
		b.setActionCommand("conn");
		b.addActionListener(this);
		p.add(Box.createHorizontalStrut(5));
		p.add(b = new JButton("Disconnect"));
		dconn=b;
		b.setActionCommand("disconn");
		b.addActionListener(this);
		p.add(Box.createHorizontalStrut(5));
		/*p.add(b = new JButton("All ON"));
		b.setActionCommand("allon");
		b.addActionListener(this);
		p.add(Box.createHorizontalStrut(5));*/
		p.add(b = new JButton("All OFF"));
		b.setActionCommand("alloff");
		b.addActionListener(this);
		p.add(Box.createHorizontalStrut(5));
		p.add(unlock = b = new JButton("Unlock"));
		b.setActionCommand("unlock");
		b.addActionListener(this);
		p.add(Box.createHorizontalStrut(5));
		p.add(b = new JButton("<html><center>EMERGENCY<br>KILL ALL"));
		b.setMaximumSize(b.getPreferredSize());
		UiUtils.makeSimpleButton(b);
		b.setBackground(Color.RED);
		b.setActionCommand("kill");
		b.addActionListener(this);
		p.add(Box.createHorizontalStrut(5));
		p.add(b = new JButton("Help"));
		b.setActionCommand("help");
		b.addActionListener(this);
		add(p, BorderLayout.NORTH);

		if (!loadBindings()) {
			add(new JLabel("Can't load 'hvchannels.conf'"), BorderLayout.CENTER);
			return;
		}

		add(tabs, BorderLayout.CENTER);
		tabs.addTab("TPC", new JScrollPane(new HVTPCView(this)));
		tabs.addTab("BPD", new JScrollPane(new BPDView(this)));
		tabs.addTab("Beam-CNT", new JScrollPane(new BeamCntView(this)));

		dconn.setEnabled(false);
		rx.setOpaque(true);
		tx.setOpaque(true);

		caen.setStateListener(this);

		addr=bindings.getProperty("caenet.host");
		if (addr==null) addr=defaultAddr;
	}

	public JMenuBar buildMenuBar() {
		return null;
	}

	public void addChannelListener(final ActionListener c) {
		chnlners.add(c);
	}

	private void createCrateTab() {
		if (sy527.getModulesCount() <= 0) return;
		try {
			tabs.addTab(sy527.getName(), new CrateView(this,sy527));
		} catch (Exception e) {
			log.debug(e);
		}
	}

	public boolean isLocked() { return locked; }

	public void showLocked() {
		UiUtils.messageBox(this,"Security Alert",
			"Application is in VIEW mode\nand this operation is not allowed",
			JOptionPane.ERROR_MESSAGE);
	}
	public boolean showConfim(String msg) {
		String code=String.valueOf(Encryptor.genToken(4));
		String in=JOptionPane.showInputDialog(this,
				msg+"\nEnter confirmation token:  "+code,"Warning",JOptionPane.WARNING_MESSAGE);
		return code.equals(in);
	}

	public void actionPerformed(ActionEvent ev) {
		final String cmd = ev.getActionCommand();
		log.info("Action: %s",cmd);
		if ("quit".equals(cmd)) {
			disconnectSY527();
			((Window) getTopLevelAncestor()).dispose();
			System.exit(0);
			return;
		}
		if ("about".equals(cmd)) {
			Version v = Version.getInstance();
			UiUtils.messageBox(this, "About", appname + ", version "
					+ v.version + "\n" + (appdescr != null ? appdescr : "")
					+ "\n" + "by " + author + "\n" + "Build " + v.buildno
					+ " on " + v.buildtime, JOptionPane.INFORMATION_MESSAGE);
		} else if ("help".equals(cmd)){
			UiUtils.showHelp(this,"HV Help",Resource.getResourceURL("res/help/hv/index.htm"));
		} else if ("conn".equals(cmd)) {
			try{connectSY527();}
			catch (Exception e) {
				AudioPlayerWav.play("ding");
				UiUtils.messageBox(this,"Error","Can't connect SY527\n\n"+e.getMessage(),JOptionPane.ERROR_MESSAGE);
				conn.setEnabled(true);
			}
		} else if ("disconn".equals(cmd)) {
			//if (!showConfim("Are you sure to disconnect?")) return ;
			try { disconnectSY527(); } catch (Exception e) {log.error(e);}
		} else if ("unlock".equals(cmd)) {
			setLocked(!locked);
		} else if ("allon".equals(cmd)) {
			if (locked) { showLocked(); return; }
			if (!showConfim("Are you sure to switch all channels ON?")) return ;
			for (int i = 0; i < chnlners.size(); ++i) {
				ActionListener l = chnlners.get(i);
				if (l instanceof HVChannelUI) {
					HVChannelUI c = (HVChannelUI) l;
					c.actionPerformed(onChn);
				}
			}
		} else if ("alloff".equals(cmd)) {
			if (locked) { showLocked(); return; }
			if (!showConfim("Are you sure to switch all channels OFF?")) return ;
			for (int i = 0; i < chnlners.size(); ++i) {
				ActionListener l = chnlners.get(i);
				if (l instanceof HVChannelUI) {
					HVChannelUI c = (HVChannelUI) l;
					c.actionPerformed(offChn);
				}
			}
		} else if ("kill".equals(cmd)) {
			if (!showConfim("Are you sure to kill all channels now?")) return ;
			try {
				killChannels();
			} catch (Exception e) {
				log.debug(e);
			}
		}
	}

	private void setLocked(boolean lock){
		if (lock){
			if (locked) return ;
			locked=true;
		}
		else{
			if (!showConfim("Are you sure to enter UNLOCK mode?")) return ;
			locked=false;
		}
		unlock.setText(locked ? "Unlock" : "Lock");
		// notify Listeners something changed
		log.info("Application %s",locked?"LOCKED":"UNLOCKED");
		for (int i = 0; i < chnlners.size(); ++i) {
			ActionListener l = chnlners.get(i);
			// log.debug("chnl[%d] %s",i,l.getClass().getSimpleName());
			if (l instanceof HVChannelGroupUI) {
				HVChannelGroupUI g = (HVChannelGroupUI) l;
				g.actionPerformed(updCtl);
			}
		}
		if (locked) unlockTm=0;
		else unlockTm=System.currentTimeMillis()/1000+5*60;
	}

	private boolean connectSY527() throws Exception {
		conn.setEnabled(false);
		if (sy527.getAddress() > 0) return true;
		while (tabs.getTabCount()>0) {
			int n=tabs.getTabCount();
			//yes getComponentAt, not getTabComponentAt !!!
			Component c=tabs.getComponentAt(n - 1);
			if (!(c instanceof CrateView)) break;
			tabs.removeTabAt(n - 1);
			((CrateView)c).dispose();
			c=null;
		}
		for (int i = 0; i < sy527.getModulesCount(); ++i) {
			HVModule m = (HVModule)sy527.getModule(i);
			for (int chn = 0; chn < m.chns; ++chn){
				ChannelSettings cs=m.getChannelSettings(chn);
				cs.status=0;
				cs.v0set=cs.v1set=cs.vread=0;
				if (Thread.currentThread().isInterrupted()) break;
			}
			if (Thread.currentThread().isInterrupted()) break;
		}
		//inform listeners about update
		for (int i = 0; i < chnlners.size(); ++i)
			chnlners.get(i).actionPerformed(updChn);

		Connection c = Connection.getConnection(addr);
		try{
			c.connect();
			StringBuilder b = new StringBuilder();
			synchronized(sy527) {
				caen.setIO(c);
				caen.open();
				for (int cr = 1; cr < 5; ++cr) {
					int r;
					if ((r=caen.readIdent(cr, b)) <= 0){
						log.info("Crate[%d]: %d",cr,r);
						if (cr==1) throw new Exception("No response from CaenNet\n"+
							"Tip: check if SY527 crate is switched ON");
						else break;
					}
					log.info("Crate[%d]: %s",cr,b.toString());
					if (b.indexOf("SY527") >= 0) {
						sy527.setCaenNet(caen);
						sy527.setAddress(cr);
						sy527.setName(b.toString());
						sy527.readCrateConf();
						break;
					}
				}
				caen.close();
			}
		}finally {
			c.disconnect();
		}
		if (sy527.getAddress() <= 0){
			disconnected();
			throw new Exception("SY527 not found on CaenNet");
		}
		log.info("Connected %s, address: %d",sy527.getName(),sy527.getAddress());
		connected();
		new Thread(new Runnable() {
			public void run(){
				workloop=Thread.currentThread();
				try{ readLoop(); } finally {
					workloop=null;
					disconnected();
				}
			}
		}).start();

		return true;
	}

	private void disconnectSY527() {
		log.info("Disconnecting %s",sy527.getName());
		if (sy527.getAddress() <= 0) {
			log.debug("sy527 address is 0");
			return;
		}
		dconn.setEnabled(false);
		if (workloop!=null) {workloop.interrupt();SysUtil.delay(100);}
		synchronized(sy527) {
			sy527.setAddress(0);
			for (int i = 0; i < chnlners.size();) {
				if (chnlners.get(i) instanceof ModuleView) {
					chnlners.remove(i);
					continue;
				}
				++i;
			}
		}
		log.info("Disconnected %s",sy527.getName());
	}

	public void connected(){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				dconn.setEnabled(true);
			}
		});
	}
	public void disconnected(){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				conn.setEnabled(true);
				dconn.setEnabled(false);
			}
		});
	}

	private void readConfig() {
		if (sy527.getAddress() <= 0) return;
		Connection c = Connection.getConnection(addr);
		try{
			c.connect();
			StringBuilder buf=new StringBuilder();
			synchronized(sy527) {
				caen.setIO(c);
				caen.open();
				for (int i = 0; i < sy527.getModulesCount(); ++i) {
					HVModule m = (HVModule)sy527.getModule(i);
					for (int chn = 0; chn < m.chns; ++chn){
						sy527.readChnSettings(m,chn,buf);
						if (Thread.currentThread().isInterrupted()) break;
					}
					if (Thread.currentThread().isInterrupted()) break;
				}
				caen.close();
			}
		}catch (Exception e) {
			log.error(e);
		}finally {
			c.disconnect();
		}
		//inform listeners about update (run in EDT)
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				for (int i = 0; i < chnlners.size(); ++i)
					chnlners.get(i).actionPerformed(updCfg);
			}
		});
	}

	private void killChannels() {
		if (workloop!=null) {workloop.interrupt();SysUtil.delay(100);}
		if (sy527.getAddress() <= 0) return;
		Connection c = Connection.getConnection(addr);
		try{
			c.connect();
			synchronized(sy527) {
				caen.setIO(c);
				caen.open();
				sy527.killChannels();
				caen.close();
			}
		}catch (Exception e) {
			log.error(e);
		}finally {
			c.disconnect();
		}
		log.info("Killed channels");
	}

	public void checkTripped() {
		if (sy527.getModulesCount() <= 0) return;
		StringBuilder buf=new StringBuilder();
		synchronized(sy527) {
		for (int i = 0; i < sy527.getModulesCount(); ++i) {
			HVModule m = (HVModule)sy527.getModule(i);
			for (int chn = 0; chn < m.chns; ++chn){
				ChannelSettings c=m.getChannelSettings(chn);
				if (c.isTripped()){
					if (tripped.contains(c)) c=null;
					else tripped.add(c);
				}
				else {
					if (!tripped.contains(c)) c=null; 
					else tripped.remove(c);
				}
				if (c!=null){
					buf.append("\n");
					buf.append(String.format("Chn[%d.%d] %s ",m.getSlot(),chn,c.name));
					buf.append(c.isTripped()?"tripped":"not tripped");
				}
			}
			if (Thread.currentThread().isInterrupted()) break;
		}
		}
		if (buf.length()>0){
			log.info(buf.toString());
		}
		if (tripped.size()>0) {
			UiUtils.messageBox(HVControl.this,"Error","Hardware communication problem\n\n"+buf.toString(),JOptionPane.ERROR_MESSAGE);
			AudioPlayerWav.play("alarm");
		}
	}

	public void readChannels(HVModule m) {
		if (sy527.getAddress() <= 0) return;
		if (sy527.getModulesCount() <= 0) return;
		Connection c = Connection.getConnection(addr);
		try{
			c.connect();
			StringBuilder buf=new StringBuilder();
			synchronized(sy527) {
				caen.setIO(c);
				caen.open();
				for (int chn = 0; chn < m.chns; ++chn){
					sy527.readChnSettings(m,chn,buf);
					if ((m.getChannelSettings(chn).flag&(1<<14))==0) continue;
					sy527.readChnStatus(m,chn,buf);
					if (Thread.currentThread().isInterrupted()) break;
				}
				caen.close();
			}
		} catch (Exception e) {
			log.error(e);
		} finally {
			c.disconnect();
		}
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				for (int i = 0; i < chnlners.size(); ++i)
					chnlners.get(i).actionPerformed(updChn);
			}
		});
	}
	public void readChannels() {
		if (sy527.getAddress() <= 0) return;
		if (sy527.getModulesCount() <= 0) return;
		Connection c = Connection.getConnection(addr);
		try{
			c.connect();
			StringBuilder buf=new StringBuilder();
			synchronized(sy527) {
			caen.setIO(c);
			caen.open();
			for (int i = 0; i < sy527.getModulesCount(); ++i) {
				HVModule m = (HVModule)sy527.getModule(i);
				for (int chn = 0; chn < m.chns; ++chn){
					//sy527.readChnSettings(m,chn,buf);
					sy527.readChnStatus(m,chn,buf);
					if (Thread.currentThread().isInterrupted()) break;
				}
				if (Thread.currentThread().isInterrupted()) break;
			}
			caen.close();
			}
		} catch (Exception e) {
			log.error(e);
		} finally {
			c.disconnect();
		}
		//inform listeners about update (run in EDT)
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				for (int i = 0; i < chnlners.size(); ++i)
					chnlners.get(i).actionPerformed(updChn);
				checkTripped();
			}
		});
	}

	public void setChnParam(int slot, int chn, int par, int v) {
		if (locked) { showLocked(); return; }
		if (sy527.getAddress() <= 0) return;
		if (sy527.getModulesCount() <= 0) return;
		HVModule m = sy527.findHVModule(slot);
		if (m == null) return;
		Connection c = Connection.getConnection(addr);
		try {
			c.connect();
			synchronized(sy527) {
				caen.setIO(c);
				caen.open();
				sy527.setChnParam(slot, chn, par, v);
				SysUtil.delay(100);
				sy527.readChnSettings(m,chn,null);
				sy527.readChnStatus(m,chn,null);
				caen.close();
			}
		} catch (Exception e) {
			log.error(e);
		} finally {
			c.disconnect();
		}
		log.info("Action: set channel %d.%d param %d=%d",slot,chn,par,v);
		for (int i = 0; i < chnlners.size(); ++i)
			chnlners.get(i).actionPerformed(updChn);
	}

	public void setChnSettings(int slot, int chn, ChannelSettings csold,ChannelSettings cs) {
		if (csold.equalSettings(cs)) {log.debug("settings are equal %d.%d",slot,chn); return ;}
		if (locked) { showLocked(); return; }
		if (sy527.getAddress() <= 0) return;
		if (sy527.getModulesCount() <= 0) return;
		HVModule m = sy527.findHVModule(slot);
		if (m == null) return;
		Connection c = Connection.getConnection(addr);
		try {
			c.connect();
			synchronized(sy527) {
				caen.setIO(c);
				caen.open();

				if (cs.name!=null && !cs.name.equals(csold.name))
					sy527.setChnName(slot, chn, cs.name);
				if (cs.v0set!=csold.v0set)
					sy527.setChnParam(slot, chn, SY527.CMD_SetChnV0SET,cs.v0set);
				if (cs.i0set!=csold.i0set)
					sy527.setChnParam(slot, chn, SY527.CMD_SetChnI0SET,cs.i0set);
				if (cs.vmax!=csold.vmax)
					sy527.setChnParam(slot, chn, SY527.CMD_SetChnVMAX,cs.vmax);
				if (cs.trip!=csold.trip)
					sy527.setChnParam(slot, chn, SY527.CMD_SetChnTRIP,cs.trip);
				if (cs.rup!=csold.rup)
					sy527.setChnParam(slot, chn, SY527.CMD_SetChnRUP,cs.rup);
				if (cs.rdn!=csold.rdn)
					sy527.setChnParam(slot, chn, SY527.CMD_SetChnRDN,cs.rdn);

				SysUtil.delay(100);
				sy527.readChnSettings(m,chn,null);
				sy527.readChnStatus(m,chn,null);
				caen.close();
			}
		} catch (Exception e) {
			log.error(e);
		} finally {
			c.disconnect();
		}
		log.info("Action: set channel %d.%d settings %s",slot,chn,cs.toString());
		for (int i = 0; i < chnlners.size(); ++i)
			chnlners.get(i).actionPerformed(updChn);
	}
	
	public void rampGrpBy(HVChannelGroup grp, int v) {
		if (locked) { showLocked(); return; }
		if (v==0 || grp.size() == 0) return;
		if (sy527.getAddress() <= 0) return;
		if (sy527.getModulesCount() <= 0) return;
		Connection c = Connection.getConnection(addr);
		try {
			c.connect();
			synchronized(sy527) {
			caen.setIO(c);
			caen.open();
			for (int i = 0; i < grp.size(); ++i) {
				HVChannel chn = grp.get(i);
				ChannelSettings cs=sy527.findHVModule(chn.slot).getChannelSettings(chn.chn);
				sy527.setChnParam(chn.slot,chn.chn,SY527.CMD_SetChnV0SET,cs.v0set+v);
			}
			SysUtil.delay(100);
			StringBuilder buf=new StringBuilder();
			for (int i = 0; i < grp.size(); ++i) {
				HVChannel chn = grp.get(i);
				HVModule m=sy527.findHVModule(chn.slot);
				sy527.readChnSettings(m,chn.chn,buf);
				sy527.readChnStatus(m,chn.chn,buf);
				if (Thread.currentThread().isInterrupted()) break;
			}
			caen.close();
			}
		} catch (Exception e) {
			log.error(e);
		} finally {
			c.disconnect();
		}
		log.info("Action: ramp group %S by %d",grp.getName(),v);
		for (int i = 0; i < chnlners.size(); ++i)
			chnlners.get(i).actionPerformed(updChn);
	}

	public void setGrpParam(HVChannelGroup grp, int par, int v) {
		if (locked) { showLocked(); return; }
		if (grp.size() == 0) return;
		if (sy527.getAddress() <= 0) return;
		if (sy527.getModulesCount() <= 0) return;
		Connection c = Connection.getConnection(addr);
		try {
			c.connect();
			synchronized(sy527) {
				caen.setIO(c);
				caen.open();
				for (int i = 0; i < grp.size(); ++i) {
					HVChannel chn = grp.get(i);
					if (sy527.setChnParam(chn.slot, chn.chn, par, v)<0) break;
				}
				SysUtil.delay(100);
				StringBuilder buf=new StringBuilder();
				for (int i = 0; i < grp.size(); ++i) {
					if (Thread.currentThread().isInterrupted()) break;
					HVChannel chn = grp.get(i);
					HVModule m=sy527.findHVModule(chn.slot);
					sy527.readChnSettings(m,chn.chn,buf);
					sy527.readChnStatus(m,chn.chn,buf);
				}
				caen.close();
			}
		} catch (Exception e) {
			log.error(e);
		} finally {
			c.disconnect();
		}
		log.info("Action: set group %s param %d=%d",grp.getName(),par,v);
		for (int i = 0; i < chnlners.size(); ++i)
			chnlners.get(i).actionPerformed(updChn);
	}

	public void setGrpSettings(HVChannelGroup grp, ChannelSettings cs) {
		if (locked) { showLocked(); return; }
		if (grp.size() == 0) return;
		if (sy527.getAddress() <= 0) return;
		if (sy527.getModulesCount() <= 0) return;
		Connection c = Connection.getConnection(addr);
		StringBuilder info=new StringBuilder();
		try {
			c.connect();
			synchronized(sy527) {
				caen.setIO(c);
				caen.open();
				ChannelSettings csold;
				for (int i = 0; i < grp.size(); ++i) {
					HVChannel chn = grp.get(i);
					csold=sy527.findHVModule(chn.slot).getChannelSettings(chn.chn);
					info.append(String.format("chn%d.%d:",chn.slot,chn.chn));
					if (cs.v0set!=csold.v0set) {
						sy527.setChnParam(chn.slot, chn.chn, SY527.CMD_SetChnV0SET,cs.v0set);
						info.append(String.format("v0set=%d,",cs.v0set));
					}
					if (cs.i0set!=csold.i0set) {
						sy527.setChnParam(chn.slot, chn.chn, SY527.CMD_SetChnI0SET,cs.i0set);
						info.append(String.format("i0set=%d,",cs.i0set));
					}
					if (cs.vmax!=csold.vmax) {
						sy527.setChnParam(chn.slot, chn.chn, SY527.CMD_SetChnVMAX,cs.vmax);
						info.append(String.format("vmax=%d,",cs.vmax));
					}
					if (cs.trip!=csold.trip) {
						sy527.setChnParam(chn.slot, chn.chn, SY527.CMD_SetChnTRIP,cs.trip);
						info.append(String.format("trip=%d,",cs.trip));
					}
					if (cs.rup!=csold.rup) {
						sy527.setChnParam(chn.slot, chn.chn, SY527.CMD_SetChnRUP,cs.rup);
						info.append(String.format("rup=%d,",cs.rup));
					}
					if (cs.rdn!=csold.rdn) {
						sy527.setChnParam(chn.slot, chn.chn, SY527.CMD_SetChnRDN,cs.rdn);
						info.append(String.format("rdn=%d,",cs.rdn));
					}
					info.append("\n");
				}
				SysUtil.delay(100);
				StringBuilder buf=new StringBuilder();
				for (int i = 0; i < grp.size(); ++i) {
					HVChannel chn = grp.get(i);
					HVModule m=sy527.findHVModule(chn.slot);
					sy527.readChnSettings(m,chn.chn,buf);
					sy527.readChnStatus(m,chn.chn,buf);
					if (Thread.currentThread().isInterrupted()) break;
				}
				caen.close();
			}
		} catch (Exception e) {
			log.error(e);
		} finally {
			c.disconnect();
		}
		log.info("Action: set group %s settings\n%s",grp.getName(),info.toString());
		for (int i = 0; i < chnlners.size(); ++i)
			chnlners.get(i).actionPerformed(updChn);
	}

	@Override
	public void stateChanged(int state) {
		if ((state & CaenNet.STATE_RECV) != 0) rx.setBackground(Color.GREEN);
		else rx.setBackground(getBackground());
		if ((state & CaenNet.STATE_SEND) != 0) tx.setBackground(Color.GREEN);
		else tx.setBackground(getBackground());		
	}

	public void fastRead(HVModule m){
		fastReadMod=m;
	}

	public void readLoop() {
		long tm=System.currentTimeMillis()/1000;
		long readChn=tm;
		long readCfg=tm+60;
		long fastChn=0;
		try {
			readConfig();
			createCrateTab();
		} catch (Exception e) {
			log.error(e);
		}
		for (;;) {
			if (sy527.getAddress() <= 0) break;
			tm=System.currentTimeMillis()/1000;
			if (unlockTm<tm) setLocked(true);
			if (fastChn>0 && fastChn<tm){
				if (fastReadMod!=null){
					Component c=tabs.getComponentAt(tabs.getTabCount() - 1);
					if ((c instanceof CrateView)){
						final ModuleView v=((CrateView)c).getModuleView(fastReadMod);
						if (v!=null)
						SwingUtilities.invokeLater(new Runnable(){
							public void run() { v.setFast(false); }
						});
						else log.error("not found slot[%d] %s",fastReadMod.getSlot(),fastReadMod.getName());
					}
					else log.error("last tab is not CrateView");
				}
				fastChn=0; fastReadMod=null;
			}
			if (fastReadMod!=null) {
				if (fastChn==0) fastChn=tm+FASTREAD;
				readChannels(fastReadMod);
			}
			else {
				if (readChn<tm) {
					readChannels();
					readChn=tm+20;
				}
				if (readCfg<tm) {
					readConfig();
					readCfg=tm+60;
				}				
			}
			if (Thread.currentThread().isInterrupted()) break;
			SysUtil.delay(1000);
		}
	}

	public HVModule findModule(int slot) {
		return sy527.findHVModule(slot);
	}
	public ArrayList<HVModule> getModules() {
		ArrayList<HVModule> mods = new ArrayList<HVModule>();
		for (int i = 0; i < sy527.getModulesCount(); ++i)
			mods.add((HVModule)sy527.getModule(i));
		return mods;
	}

	public HVChannelGroup getChannelGroup(String id){
		HVChannelGroup g=new HVChannelGroup();
		Properties p = bindings;
		String v;
		if ((v=p.getProperty(id+".name"))!=null) g.setName(v);
		int chns=0;
		for (int i = 0; (v = p.getProperty(id+"["+i+"]")) != null; ++i) {
			String[] row = v.split(",");
			if (row.length == 1) {
				g.add(new HVChannel(row[0], -1, -1));
				continue;
			}
			if (row.length < 3) {
				log.error("wrong value for '%s'", id + "[" + i + "]");
				continue;
			}
			String n = row[0].trim();
			int slot = Integer.parseInt(row[1].trim());
			int chn = Integer.parseInt(row[2].trim());
			g.add(new HVChannel(n, slot, chn));
			++chns;
		}
		log.debug("group %s: %d chns",id,chns);
		return g;
	}

	private boolean loadBindings() {
		boolean ret=false;
		InputStream f = null;
		try {
			f=Resource.getResourceURL("res/hvchannels.conf").openStream();
			bindings.load(f);
			ret=true;
		} catch (Exception e) {
		} finally {
			if (f != null) try { f.close(); } catch (Exception e) { }
		}
		if (!ret) log.error("Can't load 'hvchannels.conf'");
		return ret;
	}
	
	public static void main(String[] args) {
		try {
			//UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (Exception e) {}
		Version.createInstance(HVControl.class);
		UiUtils.setupUIManagerFonts();
		UIManager.put("Button.margin", new Insets(1, 1, 1, 1));
		ImageIcon icon=null;
		final HVControl panel = new HVControl();
		JFrame f = new JFrame(appname + " " + Version.getInstance());
		f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		try{icon=new ImageIcon(ImageIO.read(Resource.getResourceURL("res/hvico.jpg")));}
		catch (Exception e) {}
		UiUtils.setIcon(f, icon);
		panel.setOpaque(true);
		f.setJMenuBar(panel.buildMenuBar());
		f.setContentPane(panel);
		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				panel.actionPerformed(new ActionEvent(e.getSource(),e.getID(),"quit"));
			}
		});
		f.pack();
		Dimension s=f.getSize();
		//s.width+=10;
		s.height+=10;
		f.setMinimumSize(s);
		//f.setLocation(50, 0);
		f.setLocationRelativeTo(null);
		f.setVisible(true);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				panel.actionPerformed(new ActionEvent(panel,0,"conn"));
			}
		});
	}
}
