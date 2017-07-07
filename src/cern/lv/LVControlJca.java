package cern.lv;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import common.SysUtil;
import common.Version;
import common.connection.link.AbstractLink;
import common.connection.link.AbstractLink.LinkStateListener;
import common.crypt.Encryptor;
import common.ui.MainPanel;
import common.ui.UiUtils;
import common.util.Resource;
import conn.AbstrConn.ConnectorListener;
import epics.JcaConn;

@SuppressWarnings("serial")
public class LVControlJca extends MainPanel implements ActionListener,LVControlInterface,ConnectorListener {
	final static String appname = "LV Control";
	final static String appdescr = "Low Voltage Control";
	final static String author = "Krzysztof.Dynowski@cern.ch";

	final static int UNLOCK_INTERVAL=60;
	final static int UPDATE_INTERVAL=10;
	final static int CHECKHW_INTERVAL=31;
	
	//control state changed (locked/unlocled)
	final private ActionEvent updCtl = new ActionEvent(this, 0, "updCtl");
	//channel value changed
	final private ActionEvent updChn = new ActionEvent(this, 0, "updChn");

	private String addr=null;
	private JcaConn connector=null;
	private boolean locked=true;
	private long tmLock;

	private final Font fontTitle = new Font("Verdana", Font.BOLD, 25);
	private final JTabbedPane tabs;
	private final JButton conn,dconn,unlock;
	private final JLabel rx, tx;

	private final ArrayList<LVChannel> chns = new ArrayList<LVChannel>();
	static Map<String,LVChannel> pvmap=new HashMap<String,LVChannel>();
	private final ArrayList<ActionListener> chnlners = new ArrayList<ActionListener>();
	
	private static final Properties config = new Properties();
	private boolean dlgOn=false;

	public LVControlJca() {
		tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		JPanel p;
		JButton b;
		JLabel l;
		p = new JPanel(null);
		p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
		ImageIcon icon=null;
		try{icon=new ImageIcon(ImageIO.read(Resource.getResourceURL("res/lvna61.jpg")));}
		catch (Exception e) {}
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
		/*p.add(b = new JButton("All OFF"));
		b.setActionCommand("alloff");
		b.addActionListener(this);
		p.add(Box.createHorizontalStrut(5));*/
		p.add(unlock = b = new JButton("Unlock"));
		b.setActionCommand("unlock");
		b.addActionListener(this);
		p.add(Box.createHorizontalStrut(5));
		p.add(b = new JButton("Help"));
		b.setActionCommand("help");
		b.addActionListener(this);
		add(p, BorderLayout.NORTH);
		
		dconn.setEnabled(false);
		rx.setOpaque(true);
		tx.setOpaque(true);

		if (config.isEmpty()) {
			add(new JLabel("Can't load configuration"), BorderLayout.CENTER);
			return;
		}
		addr=config.getProperty("vme.host");
		
		add(tabs, BorderLayout.CENTER);
		tabs.addTab("TPC", new JScrollPane(new LVTPCView(this)));
		if (connector==null) connector=new JcaConn();
		connector.setConnectorListener(this);
		connector.getLink().setStateListener(new LinkStateListener() {
			public void stateChanged(final int st) {
				invokeLater(new Runnable() {
					public void run() {
						if (st == AbstractLink.STATE_RECV) {
							rx.setBackground(Color.GREEN);
							tx.setBackground(getBackground());
						}
						else if (st == AbstractLink.STATE_SEND) {
							rx.setBackground(getBackground());
							tx.setBackground(Color.GREEN);
						}
						else {
							rx.setBackground(getBackground());
							tx.setBackground(getBackground());
						}
					}
				});
			}
		});
	}
	
	public void actionPerformed(ActionEvent ev) {
		final String cmd = ev.getActionCommand();
		log.info("Action: %s",cmd);
		if ("quit".equals(cmd)) {
			disconnect();
			((Window) getTopLevelAncestor()).dispose();
			System.exit(0);
			return;
		}
		if ("about".equals(cmd)) {
		} else if ("help".equals(cmd)){
			UiUtils.showHelp(this,"LV Help",Resource.getResourceURL("res/help/lv/index.htm"));
		} else if ("conn".equals(cmd)) {
			connect();
		} else if ("alloff".equals(cmd)) {
			if (locked) { return; }
			if (!showConfim("Are you sure to switch all channels OFF?")) return ;
			fire(new ActionEvent(this,0,"off"));
		} else if ("disconn".equals(cmd)) {
			disconnect();
		} else if ("unlock".equals(cmd)) {
			setLocked(!locked);
		}
	}
	private void setLocked(boolean lock) {
		if (lock){
			if (locked) return ;
			locked=true;
		}
		else{
			if (!showConfim("Are you sure to enter UNLOCK mode?")) return ;
			locked=false;
		}
		unlock.setText(locked ? "Unlock" : "Lock");
		// notify Listeners control changed
		fire(updCtl);
		log.info("Application %s",locked?"LOCKED":"UNLOCKED");
		tmLock=System.currentTimeMillis()/1000+UNLOCK_INTERVAL;
		tmQuickread=0;
	}

	private void connect() {
		if (addr==null||connector==null) {
			log.error("%s is null",addr==null?"addr":"connector");
			return ;
		}
		conn.setEnabled(false);
		connector.setAddr(addr);
		connector.start();
	}
	private void disconnect() {
		if (connector==null) return ;
		connector.stop();
	}


	//ControlInterface
	private int comp2addr(int a,int b,int c){
		if (a<0||b<0||c<0) return -1;
		return (a<<10)+(b<<7)+(c<<2);
	}
	public SectorGroup getBindings(String id) {
		SectorGroup g=new SectorGroup();
		Properties p = config;
		String v;
		if ((v=p.getProperty(id+".name"))!=null) g.setName(v);
		int chns=0;
		for (int i = 0; (v = p.getProperty(id+"["+i+"]")) != null; ++i) {
			String name = v.split(",")[0].trim();
			ArrayList<LVChannel> chnlist=new ArrayList<LVChannel>();
			for (int j=0; (v = p.getProperty(id+"["+i+"]."+j)) != null; ++j){
				String[] row=v.split(",");
				if (row.length < 5) {
					chnlist.add(new LVChannel(0,0,-1,0));
					continue;
				}
				int pci = Integer.parseInt(row[0].trim());
				int cr = Integer.parseInt(row[1].trim());
				int scr = Integer.parseInt(row[2].trim());
				int slot = Integer.parseInt(row[3].trim());
				int chn = Integer.parseInt(row[4].trim());
				int lvc = 0;
				try{
					if (row.length>5) lvc=Integer.parseInt(row[5].trim());
				}catch (Exception e) {}
				int a=comp2addr(scr,slot,chn);
				chnlist.add(new LVChannel(pci,cr,a,lvc));
				++chns;
			}
			g.add(new SectorGroup.Sector(name,chnlist));
		}
		log.debug("group %s: %d chns",id,chns);
		return g;
	}
	public boolean showConfim(String msg) {
		String code=String.valueOf(Encryptor.genToken(4));
		String in=JOptionPane.showInputDialog(this,
				msg+"\nEnter confirmation token:  "+code,"Warning",JOptionPane.WARNING_MESSAGE);
		if (in==null) return false;
		return Version.getInstance().DEBUG ? true:code.equals(in);
	}
	private void fire(ActionEvent a){
		for (int i = 0; i < chnlners.size(); ++i) {
			ActionListener l = chnlners.get(i);
			l.actionPerformed(a);
		}
	}
	public final void fireAction(final ActionEvent a){
		if (chnlners.size()==0) {
			log.error("no listeners to fire");
			return ;
		}
		invokeLater(new Runnable(){
			public void run() { fire(a); }
		});
	}
	public void addChannelListener(ActionListener al) {
		if (!chnlners.contains(al)) chnlners.add(al);
		if (al instanceof SectorUI){
			SectorUI ui=(SectorUI)al;
			for (LVChannel c:ui.getChnList()) {
				if (c.addr<0) continue;
				LVChannel.setNamePv(c);
				if (c.def.pv!=null) {
					chns.add(c);
					pvmap.put(c.def.pv, c);
				}
				else log.error("lv(%s) pv=null",c.def.name);
			}
			//chns.addAll(ui.getChnList());
		}
	}
	public void removeChannelListener(ActionListener al) {
		chnlners.remove(al);
	}
	public boolean isLocked() {
		return locked;
	}
	private long tmQuickread=0;
	//request to write channel status value
	public void writeChannelStatus(LVChannel c, int st) {
		if (c==null || c.addr<0) return ;
		if (isLocked()) return ;
		String pv=c.def.pv+"Write";
		log.debug("writePv(%s,%x)",pv,st);
		connector.writePv(pv, st);
		tmLock=System.currentTimeMillis()/1000+UNLOCK_INTERVAL;
		tmQuickread=System.currentTimeMillis()/1000+UPDATE_INTERVAL*2;
	}
	private Thread readThread;
	//connector interface
	public void connected() {
		invokeLater(new Runnable(){
			public void run(){
				dconn.setEnabled(true);
				dconn.requestFocus();
			}
		});
		readThread=new Thread(new Runnable() {
			public void run() {
				try{
					readLoop();
				}finally {
					readThread=null;
				}
			}
		});
		readThread.start();
	}
	public void disconnected() {
		if(readThread!=null)
			readThread.interrupt();
		invokeLater(new Runnable(){
			public void run(){
				conn.setEnabled(true);
				dconn.setEnabled(false);
				conn.requestFocus();
			}
		});
	}
	public void exception(Exception e) {
		log.error(e,"connector exception");
	}
	public void execDone(int id) {
	}
	private boolean sendUpdChn=false;
	private final int[] pciErr=new int[]{0,0};
	public void readDone(int r, String pv, float[] v) {
		if (r<0) {
			log.error("read(%s) r=%d",pv,r);
			return ;
		}
		if (pv.endsWith(":Error")){
			int err=(int)v[0],pci=-1;
			if (err!=0) log.error("read(%s) = %d",pv,err);
			if (pv.contains("Chain:0")) pci=0;
			else if (pv.contains("Chain:1")) pci=1;
			else return ;
			pciErr[pci]=err;
			return ;
		}
		
		//log.debug("read(%s) done, r=%d, v=%d",pv,r,(int)v[0]);
		//find LVChacnnel for pv
		String cpv=pv.substring(0,pv.lastIndexOf(':')+1);
		LVChannel c=pvmap.get(cpv);
		if (c==null) {
			log.error("no LV for pv=%s",cpv);
			return ;
		}
		sendUpdChn=true;
		if (pv.endsWith(":Read")){
			c.status=(int)v[0];
		}
		else if (pv.endsWith(":Read.ALAR")){
			c.alarm=(int)v[0];
			//log.debug("Read.ALAR=%d",c.alarm);
		}
		else log.error("unknown pv=%s",pv);
	}
	public void writeDone(int r, String pv) {
		log.debug("write(%s) done, r=%d",pv,r);
	}
	
	final static private String pciErrPVfmt="LV:DaisyChain:%d:Error";
	private void initHW(){
		connector.writePv("LV:VTPC1:Monitor",1);
		connector.writePv("LV:VTPC2:Monitor",1);
		connector.writePv("LV:MTPCL:Monitor",1);
		connector.writePv("LV:MTPCR:Monitor",1);
		connector.writePv("LV:GTPC:Monitor",1);
		connector.writePv("LV:LMPD:Monitor",1);
		connector.writePv("LV:PS_C:Monitor",1);
		connector.writePv("LV:PS_C:Monitor",1);
		for (int pci=0; pci<2; ++pci)
			connector.writePv(String.format(pciErrPVfmt,pci),0);
	}
	private void checkHW(){
		for (int pci=0; pci<2; ++pci){
			connector.readPv(String.format(pciErrPVfmt,pci));
		}
		if (pciErr[0]==0 && pciErr[1]==0)return ;
		if (dlgOn) return ;
		dlgOn=true;
		final int[] pe={pciErr[0],pciErr[1]};
		invokeLater(new Runnable(){
			public void run(){
				String[] crate={"MTPC create","VTPC create"};
				StringBuilder b=new StringBuilder();
				for (int i=0; i<2; ++i){
					if (pe[i]!=0)
						b.append(String.format("%s(%d) is not responding (error=%d)\n",crate[i],i,pe[i]));
				}
				b.append("\n> Tip1: check if VME crates are switched ON <");
				b.append("\n> Tip2: check if cooling system is working <");
				b.append("\n------------------------------------------");
				b.append("\n Call DCS or DAQ expert ");
				UiUtils.messageBox(null,"Error","Hardware communication problem\n\n"+b.toString(),JOptionPane.ERROR_MESSAGE);
				dlgOn=false;
			}
		});
	}

	private void readLoop() {
		long tm,tmread=0,tmcheck=0;
		initHW();
		//turn on monitoring on server
		for (;;) {
			tm=System.currentTimeMillis()/1000;
			if (Thread.currentThread().isInterrupted()) break;
			if (tmcheck<tm) {
				checkHW();
				tmcheck=tm+CHECKHW_INTERVAL;
			}
			if (tmread<tm || tm<tmQuickread) {
				log.debug("read channels %d (CA channels=%d)",chns.size(),connector.getChannels().size());
				//TODO move to idle call from connector thread
				for (LVChannel c: chns) {
					connector.readPv(c.def.pv+"Read");
					if (!c.def.pv.contains("Cooling"))
						connector.readPv(c.def.pv+"Read.ALAR");
				}
				tmread=tm+UPDATE_INTERVAL;
			}
			SysUtil.delay(1000);
			if (sendUpdChn) {
				sendUpdChn=false;
				fireAction(updChn);
			}
			if (!isLocked() && tmLock<tm)
				setLocked(true);			
		}
	}
	protected void exiting(){
		disconnect();
	}

	private static void loadConfig(String[] args) {
		boolean ret=false;
		InputStream f = null;
		try {
			f=Resource.getResourceURL("res/lvchannels.conf").openStream();
			config.load(f);
			ret=true;
		} catch (Exception e) {
		} finally {
			if (!ret) log.error("Can't load 'lvchannels.conf'");
			if (f != null) try { f.close(); } catch (Exception e) { }
		}
		for (int i=0; i<args.length; ++i){
			String a=args[i];
			int p=a.indexOf("=");
			if (p==0||a.length()==0) continue;
			if (p<0) config.remove(a);
			else if (p+1==a.length()) config.remove(a.substring(0,p));
			else config.put(a.substring(0,p),a.substring(p+1));
		}
	}
	public static void main(String[] args) {
		loadConfig(args);
		UiUtils.macify(appname);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				LVControlJca p=(LVControlJca)startGUI(appname,LVControlJca.class);
				p.actionPerformed(new ActionEvent(p,0,"conn"));
			}
		});
	}

}
