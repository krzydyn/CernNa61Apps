package cern.lv;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import javax.swing.event.ChangeEvent;

import channel.ChannelData;
import common.Logger;
import common.SysUtil;
import common.Version;
import common.connection.link.AbstractLink;
import common.connection.link.AbstractLink.LinkStateListener;
import common.crypt.Encryptor;
import common.ui.AudioPlayerWav;
import common.ui.UiUtils;
import common.util.Resource;
import conn.AbstrConn;
import conn.AbstrConn.ConnectorListener;
import conn.SyncConn;

@SuppressWarnings("serial")
public class LVControl extends JPanel implements ActionListener, LinkStateListener, ConnectorListener, LVControlInterface {
	//final static boolean DEBUG=false;
	final static Logger log = Logger.getLogger();
	final static String appname = "LV Control";
	final static String appdescr = "Low Voltage Control";
	final static String author = "Krzysztof.Dynowski@cern.ch";

	final static int readInterval=10;
	final private ActionEvent updCtl = new ActionEvent(this, 0, "updCtl");

	private String addr=null;
	private String connName=null;
	private SyncConn connector=null;
	private boolean locked=true;
	private long lockTm;

	private final Font fontTitle = new Font("Verdana", Font.BOLD, 25);
	private final JTabbedPane tabs;
	private final JButton connButton,dconn,unlock;
	private final JLabel rx, tx;

	private static final Properties config = new Properties();

	public LVControl() {
		super(new BorderLayout());
		tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);

		JPanel p;
		JButton b;
		JLabel l;
		p = new JPanel(null);
		p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
		ImageIcon icon=null;
		try{icon=new ImageIcon(ImageIO.read(Resource.getResourceURL("res/lvna61.jpg")));}
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
		connButton=b;
		b.setActionCommand("conn");
		b.addActionListener(this);
		p.add(Box.createHorizontalStrut(5));
		p.add(b = new JButton("Disconnect"));
		dconn=b;
		b.setActionCommand("disconn");
		b.addActionListener(this);
		p.add(Box.createHorizontalStrut(5));
		p.add(b = new JButton("All OFF"));
		b.setActionCommand("alloff");
		b.addActionListener(this);
		p.add(Box.createHorizontalStrut(5));
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
		String prefix=getClass().getPackage().getName();
		connName=config.getProperty("connector");
		if (connName!=null)
		try{
			if (connName.indexOf('.')<0) connName=prefix+"."+connName;
			connector=(SyncConn)SysUtil.loadClass(connName).newInstance();
		}catch (Exception e) { log.error(e); }

		addr=config.getProperty("vme.host");
		if (connector==null) connector=new LVConnectorVmeSync();
		connector.setConnectorListener(this);
		connector.getLink().setStateListener(this);

		add(tabs, BorderLayout.CENTER);
		tabs.addTab("TPC", new JScrollPane(new LVTPCView(this)));
	}

	public JMenuBar buildMenuBar() {
		return null;
	}
	@Override
	public void addChannelListener(final ActionListener al) {
		connector.addChannelListener(al);
		if (al instanceof SectorUI){
			SectorUI ui=(SectorUI)al;
			ArrayList<LVChannel> chns=ui.getChnList();
			for (int i=0; i<chns.size(); ++i) {
				LVChannel c=chns.get(i);
				if (c.addr<0) continue;
				//register channel to read
				LVChannel.setNamePv(c);
				connector.readChn(c);
			}
		}
	}
	@Override
	public void removeChannelListener(final ActionListener al) {
		connector.removeChannelListener(al);
	}

	@Override
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
			connector.fireAction(new ActionEvent(this,0,"off"));
		} else if ("disconn".equals(cmd)) {
			//if (!showConfim("Are you sure to disconnect?")) return ;
			disconnect();
		} else if ("unlock".equals(cmd)) {
			setLocked(!locked);
		}
	}
	@Override
	public void connected(){
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run(){
				dconn.setEnabled(true);
			}
		});
	}
	@Override
	public void disconnected(){
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run(){
				connButton.setEnabled(true);
				dconn.setEnabled(false);
			}
		});
	}
	@Override
	public void exception(final Exception e){
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run(){
				StringBuilder b=new StringBuilder();
				b.append(e.getMessage()+"\n");
				if ((e.getCause())!=null){
					b.append("\nCaused by: "+e.getCause().getMessage());
				}
				AudioPlayerWav.play("ding");
				UiUtils.messageBox(LVControl.this,"Error",b.toString(),JOptionPane.ERROR_MESSAGE);
			}
		});
	}

	@Override
	public boolean showConfim(String msg) {
		String code=String.valueOf(Encryptor.genToken(4));
		String in=JOptionPane.showInputDialog(this,
				msg+"\nEnter confirmation token:  "+code,"Warning",JOptionPane.WARNING_MESSAGE);
		if (in==null) return false;
		return Version.getInstance().DEBUG ? true:code.equals(in);
	}

	private void connect() {
		if (addr==null||connector==null) {
			log.error("%s is null",addr==null?"addr":"connector");
			return ;
		}
		connButton.setEnabled(false);
		connector.setAddr(addr);
		connector.start();
	}
	private void disconnect() {
		if (connector==null) return ;
		connector.stop();
	}
	@Override
	public boolean isLocked() { return locked; }
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
		connector.fireAction(updCtl);
		log.info("Application %s",locked?"LOCKED":"UNLOCKED");
		lockTm=System.currentTimeMillis()+60*1000;
	}

	@Override
	public void writeChannelStatus(LVChannel c,int st){
		if (!isLocked()) lockTm=System.currentTimeMillis()+60*1000;
		if (c==null || c.addr<0) return ;
		c.setStatus=st;
		connector.writeChn(c);
	}

	private int comp2addr(int a,int b,int c){
		if (a<0||b<0||c<0) return -1;
		return (a<<10)+(b<<7)+(c<<2);
	}
	@Override
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
		ImageIcon icon=null;
		Version.createInstance(LVControl.class);
		loadConfig(args);
		UiUtils.setupUIManagerFonts();
		UIManager.put("Button.margin", new Insets(1, 1, 1, 1));
		final LVControl panel = new LVControl();
		final JFrame f = new JFrame(appname+" "+ Version.getInstance()+(panel.connector==null?"":", "+panel.connector.getName()));
		try{icon=new ImageIcon(Resource.getResourceURL("res/lvico.jpg"));}
		catch (Exception e) {}
		if (icon!=null) f.setIconImage(icon.getImage());
		f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		panel.setOpaque(true);
		f.setJMenuBar(panel.buildMenuBar());
		f.setContentPane(panel);
		f.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				panel.actionPerformed(new ActionEvent(e.getSource(), e.getID(),
						"quit"));
			}
		});
		f.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseEntered(MouseEvent e) {
				f.setCursor(Cursor.getDefaultCursor());
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
			@Override
			public void run() {
				panel.actionPerformed(new ActionEvent(panel,0,"conn"));
			}
		});
	}

	@Override
	public void execDone(int id){}

	@Override
	public void readDone(int r, String pv, float[] v) {
		/*LVChannel c=(LVChannel)ch;
		if (ch.def.pv.endsWith(".ALAR")){
			c.alarm=getIntValue(o);
		}
		else if (ch.def.pv.endsWith(":Read")){
			c.status=getIntValue(o);
		}*/
	}

	@Override
	public void writeDone(int r, String pv) {
		if (pv==null) return ;
		log.debug("r=%d, writepv(%s)",r,pv);
	}

	@Override
	public void stateChanged(int st) {
		if (!isLocked() && lockTm<System.currentTimeMillis())
			setLocked(true);
		int state = st;
		if ((state & AbstractLink.STATE_RECV) != 0) rx.setBackground(Color.GREEN);
		else rx.setBackground(getBackground());
		if ((state & AbstractLink.STATE_SEND) != 0) tx.setBackground(Color.GREEN);
		else tx.setBackground(getBackground());
		if (state==0){
			//TODO read error message from connector or link layer
			StringBuilder err=new StringBuilder();
			//connector.getErrorMsg(err);
			if (err.length()>0){
				UiUtils.messageBox(LVControl.this,"Error","Hardware communication problem\n\n"+err.toString(),JOptionPane.ERROR_MESSAGE);
			}
		}
	}
}
