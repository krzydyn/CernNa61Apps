package cern.meas;

/**
 *
 * @author KySoft, Krzysztof Dynowski
 *
 */
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import plot.PlotTime;
import plot.PlotXY;
import plot.PlotXY.ChannelParams;
import cern.meas.MeasView.ViewChannelsInfo;
import channel.ChannelDef;
import channel.ui.ChannelSelector;
import common.Logger;
import common.StrUtil;
import common.SysUtil;
import common.Version;
import common.connection.Connection;
import common.io.IOUtils;
import common.ui.AudioPlayerWav;
import common.ui.MainPanel;
import common.ui.SplashScreen;
import common.ui.UiUtils;
import common.util.Resource;

//TODO keep range params after change of layout

/*
 * Changelog
 * v1.56
 * fix: sorting when adding array of pnts
 * v1.55
 * fix: alarm lines
 * v1.54
 * new: alarm lines (hangs on linux)
 * chg: remove na61dcsdb from server list
 * v1.51
 * fix: add resource files (icon,splash,config) to .jar
 * v1.50
 * fix: exception loading GraphView with empty list of channels
 * new: Channel*.java moved to CernLib project
 * v1.49
 * new: Alt-N,Alt-P moves to prev/next page
 * v1.48
 * fix: remove empty views
 * new: save localconfig after page conf
 * v1.47
 * new: ui for channel color
 * v1.46
 * fix: adding channels on empty box
 * v1.45
 * new: adding channel from popup menu
 * v1.44
 * new: selector filter by unit
 * v1.43
 * new: multiple plots in one view
 * v1.41
 * fix: multiple db sources (fix order)
 * v1.40
 * new: multiple db sources
 * v1.38
 * new: locked option: config not saved, disabled monitor,tools menu
 * fix: reload monitors after disconnection
 * v1.37
 * localConfig: save/load chnID as a main chn identifier (was by name)
 * v1.36
 * GraphView: time formating
 * fix: don't clear chnMon on disconnect
 * v1.35
 * GraphView: name label width=80px (for !hist), ToolTip fmt="name[id]"
 * upd: make connection test once, don't ask user for host anymore
 * v1.34
 * fix: overlapping plotXY paint with ScrollBars
 * fix: detecting CERN networks by making test connection (good!)
 * v1.33
 * fix: detecting CERN networks by check net interfaces (not so good!)
 * v1.32
 * new: detecting CERN networks by check on local address (wrong!)
 * new: allow history in online monitors
 * v1.31
 * new: PlotXY uses GeneralPath to plot graph
 * new: plot line is semi transparent
 * fix: focus lost form GraphView
 * v1.30
 * new: show history (context menu)
 * new: menu Monitors
 * v1.29
 * new: autoFitY (context menu)
 * --------
 * v1.23
 * new: connect from outside via http-proxy
 * v1.22
 * new: selectable plots on page, kbd accelerators
 * v1.21
 * new: save y range to local config
 * fix: NaN in autoFitY
 * upd: disable current page MenuItem
 * v1.20
 * new: select x-range with mouse (in hist)
 * new: context menu on GraphViewer
 * upd: page configure
 * v1.19
 * upd application update
 * v1.18
 * upd application update
 * v1.17
 * new: channel selector dialog
 * new: application update
 * upd: splash screen status line
 * v1.16
 * new: save file browser for export to file
 * new: cvs value precision increased
 * v1.15
 * new: switch colors (for printer)
 * new: online monitors use getHistory (fast)
 * upd: simple export feature (timestamp added)
 * v1.14
 * new: NA61 splash screen
 * new: input dialog for host address
 * new: help on connecting via ssh tunnels
 * v1.13 broken version
 * v1.12
 * fix: y-grid labels sometimes were not accurate
 * upd: help system
 * v1.11
 * new: connection inputbox (for ssh tunnels)
 * new: simple help
 * v1.10
 * fix: y-grid labels sometimes were not accurate
 * upd: default time range in history changed to 168 hours (=7days=week)
 * new: simple export feature (to text file)
 * v1.9
 * fix: loading data into history graph
 * new: reload data into desktop viewers after disconnected > 2min
 * new: info message when connecting
 * new: send client startup info to the server (to diagnose client application)
 * v1.8
 * new: drop sqlite, use text file properties (measmonitor.ini in HOME directory)
 * v1.7
 * new: include JNI for sqlite (for Windows)
 * v1.6
 * new: use one per user config file meas.sqlite placed in user's HOME directory
 * new: algorithm to ignore jitters in graph visible area calculation
 * new(sys): send client application version number to a server
 * v1.5
 * new: configurable page name
 * new: after change of time scale on "history", the view is adjusted to place cursor in the center
 * fix: page size inside a JScrollPane
 * v1.4
 * new: configurable page: add/remove/layout (saved locally in meas.sqlite)
 */

@SuppressWarnings("serial")
public class MeasMonitor extends MainPanel implements ActionListener,MeasListener {
	final static Logger log = Logger.getLogger();
	final static String appname = "Measurement Monitor";
	final static String appdescr = "Measurement Monitor client application";
	final static String author = "Krzysztof Dynowski";
	final static SimpleDateFormat dfFull=new SimpleDateFormat("yyyy-MM-dd H:mm:ss");

	static ImageIcon icon = null;
	static SplashScreen splash = null;

	final static Resource text = Resource.getBundle("res/textNO");
	static String localcfg;
	final static double monitorTimeRange = 12.0;
	final static double historyTimeRange = 24.0*30;//30 days

	static final String CONFIG_NAME="measmonitor.ini";
	
	//TODO should go to ini
	static final String[] SRCDB={
		//"na61dcsdb.cern.ch:54331",
		"na61db.cern.ch:54331",
		"localhost:54331",
		"daq.if.pw.edu.pl:54331",
		//"na61db:54331/via:https://na61db.cern.ch"
	};

	static final String UPDATEHOST="http://kdynowsk.web.cern.ch/kdynowsk";

	public static String forceHost=null;
	public static boolean locked=false;
	private String dbHost=null; 

	private List<ChannelDef> chndefs = new ArrayList<ChannelDef>();
	private final JLabel bigInfo;
	private JMenu monitorMenu, pageMenu, histMenu, toolsMenu;
	private JMenuItem conn, disconn;
	private JPanel curPage = null;
	private JScrollPane scroll = null;
	static private MeasConnector connector;
	private final List<JPanel> pages = new ArrayList<JPanel>();
	private boolean isConnected = false;
	private long tmLastReceived;
	private boolean cfgChanged = false;
	final static List<ViewChannelsInfo> chnMon = new ArrayList<ViewChannelsInfo>();
	final static Hashtable<String, MeasView> views = new Hashtable<String, MeasView>();

	private final Runnable connRun=new Runnable(){
		private final ActionEvent ev=new ActionEvent(MeasMonitor.this,0,"conn");
		public void run() { MeasMonitor.this.actionPerformed(ev); }
	};
	private final Runnable updateOKRun=new Runnable() {
		private final ActionEvent ev=new ActionEvent(MeasMonitor.this,0,"updateOK");
		public void run() { MeasMonitor.this.actionPerformed(ev); }
	};

	public MeasMonitor() {
		super(new BorderLayout());
		setOpaque(true);
		setMinimumSize(new Dimension(800, 400));
		setPreferredSize(new Dimension(950, 630));
		
		connector = new MeasConnector(this);

		bigInfo = new JLabel("<html>Please Wait...", JLabel.CENTER);
		bigInfo.setFont(UIManager.getFont("Panel.font").deriveFont(Font.BOLD,20f));
		bigInfo.setForeground(Color.GREEN);
		//bigInfo.setOpaque(true);
		add(bigInfo, BorderLayout.CENTER);
		
		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				MeasMonitor.this.componentResized();
			}
		});
		invokeLater(connRun);
	}

	protected void componentResized() {
		if (curPage == null) return;
		JPanel page = curPage;
		Dimension sz = new Dimension(getWidth() - 10, getHeight() - 10);
		for (int i = 0; i < page.getComponentCount(); ++i) {
			JComponent c = (JComponent) page.getComponent(i);
			if (sz.height < c.getY() + c.getHeight())
				sz.height = c.getY() + c.getHeight();
			if (sz.width < c.getX() + c.getWidth())
				sz.width = c.getX() + c.getWidth();
		}
		page.setSize(sz);
		page.setMinimumSize(sz);
		validate();
		//repaint();
	}

	public JMenuBar buildMenuBar() {
		JMenuBar mb = new JMenuBar();
		JMenu m;
		JMenuItem mi;
		
		mb.add(m = new JMenu("File"));
		//m.setBorder(BorderFactory.createLineBorder(Color.GRAY,2));
		m.add(mi = new JMenuItem("Connect"));
		mi.setActionCommand("conn");
		conn = mi;
		mi.addActionListener(this);
		m.add(mi = new JMenuItem("Disconnect"));
		mi.setActionCommand("disconn");
		disconn = mi;
		mi.addActionListener(this);
		m.addSeparator();
		m.add(mi = new JMenuItem("Switch color"));
		mi.setActionCommand("swcolor");
		mi.addActionListener(this);
		m.addSeparator();
		m.add(mi = new JMenuItem("Quit"));
		mi.setActionCommand("quit");
		//mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,ActionEvent.CTRL_MASK));
		mi.addActionListener(this);
		mb.add(m = monitorMenu=new JMenu("Monitors"));
		mi=m.add("Select All");
		mi.setActionCommand("selall");
		mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,ActionEvent.CTRL_MASK));
		mi.addActionListener(this);
		mi=m.add("Deselect All");
		mi.setActionCommand("dselall");
		mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,ActionEvent.CTRL_MASK|ActionEvent.SHIFT_MASK));
		mi.addActionListener(this);
		m.add(mi = new JMenuItem("Autofit Y"));
		mi.setActionCommand("autofit");
		mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,ActionEvent.ALT_MASK));
		mi.addActionListener(this);
		m.add(mi = new JMenuItem("Properties..."));
		mi.setActionCommand("props");
		mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,ActionEvent.ALT_MASK));
		mi.addActionListener(this);
		mb.add(pageMenu = new JMenu("Pages"));
		mb.add(histMenu = new JMenu("History"));
		mi = histMenu.add("Channel selector...");
		mi.setActionCommand("histsel");
		mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H,ActionEvent.ALT_MASK));
		mi.addActionListener(this);
		mb.add(toolsMenu = m = new JMenu("Tools"));
		m.add(mi = new JMenuItem("New Page"));
		mi.setActionCommand("add");
		mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,ActionEvent.CTRL_MASK));
		mi.addActionListener(this);
		m.add(mi = new JMenuItem("Delete Page"));
		mi.setActionCommand("del");
		mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D,ActionEvent.CTRL_MASK));
		mi.addActionListener(this);
		m.add(mi = new JMenuItem("Configure Page..."));
		mi.setActionCommand("conf");
		mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,ActionEvent.ALT_MASK));
		mi.addActionListener(this);

		mb.add(Box.createHorizontalGlue()); // space

		mb.add(m = new JMenu(text.getString("menu.help", "Help")));
		m.add(mi = new JMenuItem(text.getString("menu.index", "Index...")));
		mi.setActionCommand("index");
		mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1,0));
		mi.addActionListener(this);
		m.add(mi = new JMenuItem(text.getString("menu.about", "About...")));
		mi.setActionCommand("about");
		mi.addActionListener(this);
		m.add(mi = new JMenuItem(text.getString("menu.update", "Check update...")));
		mi.setActionCommand("update");
		mi.addActionListener(this);
		m.add(mi = new JMenuItem(text.getString("menu.defaults", "Load defaults")));
		mi.setActionCommand("defaults");
		mi.addActionListener(this);

		setupMenus();
		return mb;
	}
	void setupMenus() {
		conn.setEnabled(!isConnected);
		disconn.setEnabled(isConnected);
		
		monitorMenu.setEnabled(pages.size()>0 && !locked);
		pageMenu.setEnabled(pages.size()>0);
		
		histMenu.setEnabled(chndefs.size()>0 && isConnected);
		toolsMenu.setEnabled(!locked);
	}


	private void buildPageMenu() {
		JMenuItem mi;
		pageMenu.removeAll();
		for (int p = 0; p < pages.size(); ++p) {
			JPanel page = pages.get(p);
			mi = pageMenu.add(page.getName());
			mi.setActionCommand("page" + p);
			if (p<10)
				mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0+(p+1)%10,ActionEvent.ALT_MASK));
			mi.addActionListener(MeasMonitor.this);
		}
		if (pages.size() > 0) {
			mi = pageMenu.add("Prev");;
			mi.setActionCommand("pagePrev");
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,ActionEvent.ALT_MASK));
			mi.addActionListener(MeasMonitor.this);
			mi = pageMenu.add("Next");;
			mi.setActionCommand("pageNext");
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,ActionEvent.ALT_MASK));
			mi.addActionListener(MeasMonitor.this);
		}
		pageMenu.validate();
	}

	public void setConfigChanged(){
		cfgChanged=true;
	}
	public void exiting() {
		isConnected = false;
		if (connector != null) {connector.stop();connector=null;}
		getTopLevelAncestor().setVisible(false);
		((Window) getTopLevelAncestor()).dispose();
		log.debug("gui closed, exiting");
		saveLocalConfig();
		SysUtil.delay(1000);		
	}
	public void actionPerformed(ActionEvent ev) {
		final String cmd = ev.getActionCommand();
		if ("quit".equals(cmd)) {
			exiting();
			System.exit(0);
		}
		if ("about".equals(cmd)) {
			Version v = Version.getInstance();
			UiUtils.messageBox(this, "About", appname + ", version " + v
					+ "\n" + (appdescr != null ? appdescr : "") + "\nby "
					+ author + "\n" + "Created on " + v.buildtime + 
					(dbHost != null ? "\nDB Host: "+dbHost:""),
					JOptionPane.INFORMATION_MESSAGE, icon);
		} else if ("index".equals(cmd)) {
			UiUtils.showHelp(this,"Help",Resource.getResourceURL("res/help/meas/index.htm"));
		} else if ("update".equals(cmd)) {
			checkUpdate(false);
		} else if ("defaults".equals(cmd)) {
			loadLocalConfig(true);
		} else if ("updateOK".equals(cmd)) {
			log.info(cmd);
			JOptionPane.showMessageDialog(MeasMonitor.this,
				"Downlod succesfull, restart is needed.\nYou must restart application",
				"Update",
				JOptionPane.INFORMATION_MESSAGE);
		} else if ("conn".equals(cmd)) {
			if (isConnected) connector.stop();
			if (dbHost==null){
				if (forceHost!=null) dbHost=forceHost;
				else if (SRCDB.length==1) dbHost=SRCDB[0];
				else {
					setInfo("Detecting network connection ...");
					//TODO run in background
					for (String h:SRCDB) {
						if (!Version.getInstance().DEBUG && h.startsWith("localhost"))
							continue;
						log.debug("Testing connection to %s ...",h);
						boolean r=Connection.testConnection(h);
						log.debug("connection to %s = %b",h,r);
						if (r) {
							log.info("conn ok %s",h);
							dbHost=h; break;
						}
					}
				}
			}
			if (chnMon.size() == 0) startSplash();
			if (dbHost!=null) {
				setInfo("Connecting " + dbHost + "<br>Please Wait ...");
				conn.setEnabled(false);
				connector.start(dbHost);
			}
			else {
				stopSplash();
				setInfo("Can't find working db host");
			}
		} else if ("disconn".equals(cmd)) {
			isConnected=false;
			connector.stop();
			SysUtil.delay(SysUtil.SECOND/2);
		} else if ("selall".equals(cmd)) {
			selectAll(curPage);
		} else if ("dselall".equals(cmd)) {
			deselectAll(curPage);
		} else if ("autofit".equals(cmd)) {
			JPanel page = curPage;
			boolean done=false;
			for (int i = 0; i < page.getComponentCount(); ++i) {
				JComponent c = (JComponent) page.getComponent(i);
				if (c instanceof GraphView) {
					GraphView gv = (GraphView) c;
					if (!gv.getPlot().isSelected()) continue;
					gv.getPlot().autoFitY2();
					done=true;
				}
			}
			if (!done){ //if no plot selected fit all
				for (int i = 0; i < page.getComponentCount(); ++i) {
					JComponent c = (JComponent) page.getComponent(i);
					if (c instanceof GraphView) {
						GraphView gv = (GraphView) c;
						gv.getPlot().autoFitY2();
					}
				}
			}
			cfgChanged=true;
		} else if ("props".equals(cmd)) {
			JPanel page = curPage;
			String yunit=null;
			GraphView gv1=null;
			for (int i = 0; i < page.getComponentCount(); ++i) {
				JComponent c = (JComponent) page.getComponent(i);
				if (c instanceof GraphView) {
					GraphView gv = (GraphView) c;
					if (gv1==null) gv1=gv;
					if (!gv.getPlot().isSelected()) continue;
					String yu=gv.getPlot().getUnitY();
					if (yunit==null) {yunit=yu;gv1=gv;}
					else if (!yunit.equals(yu)){yunit=null;break;}
				}
			}
			PlotTime.PropertiesPanel p = new PlotTime.PropertiesPanel("time",yunit);
			p.setProperties(gv1.getPlot());
			if (yunit==null) p.setXRange(monitorTimeRange);
			UiUtils.showDialog(this, p, "Monitors Properties");
			if (p.getResult() == 1) {
				for (int i = 0; i < page.getComponentCount(); ++i) {
					JComponent c = (JComponent) page.getComponent(i);
					if (c instanceof GraphView) {
						GraphView gv = (GraphView) c;
						if (yunit!=null && !gv.getPlot().isSelected()) continue;
						p.getProperties(gv.getPlot());
					}
				}
				cfgChanged=true;
			}
		} else if (cmd.startsWith("page")) {
			
			int i = pages.indexOf(curPage);
			if (cmd.equals("pagePrev")) {
				--i;
				if (i<0) return ;
			}
			else if (cmd.equals("pageNext")) {
				++i;
				if (i > pages.size()-1) return ;
			}
			else i=Integer.parseInt(cmd.substring(4));
			setCurrent(pages.get(i));
		} else if ("histsel".equals(cmd)) {
			ChannelSelector p = new ChannelSelector(chndefs,null);
			UiUtils.showDialog(this, p, "Channel selector");
			if (p.getResult() > 0) showHistory(p.getSelectedItem());
		} else if ("add".equals(cmd)) {
			JPanel p=createPage(3,null);
			if (confPage(p)) {addPage(p);setCurrent(p);}
		} else if ("del".equals(cmd)) {
			delPage();
		} else if ("conf".equals(cmd)) {
			confPage(curPage);
		} else if (cmd.startsWith("swcolor")) {
			int i = pages.indexOf(curPage);
			if (i < 0)
				return;
			JPanel page = curPage;
			for (i = 0; i < page.getComponentCount(); ++i) {
				JComponent c = (JComponent) page.getComponent(i);
				if (c instanceof GraphView) {
					GraphView view = (GraphView) c;
					if (view.getPlot().getPlotBackground() == Color.WHITE) {
						view.getPlot().setPlotBackground(Color.BLACK);
						view.getPlot().setBackground(getBackground());
						view.setBackground(getBackground());
					} else {
						view.getPlot().setPlotBackground(Color.WHITE);
						view.getPlot().setBackground(Color.WHITE);
						view.setBackground(Color.WHITE);
					}
				}
			}
		}
	}

	private void checkUpdate(boolean auto){
		Version ver=Version.getInstance();
		String v = null;
		try{
			v = Version.getVersion(UPDATEHOST);
		}catch (Exception e) {
			UiUtils.messageBox(this,"Update","Can't check updates\n"+e.getMessage(),
					JOptionPane.ERROR_MESSAGE);
			return ;
		}
		if (ver.version.compareToIgnoreCase(v)>=0) {
			log.info("Application is up-to-date L('%s')>=H('%s')",ver.version,v);
			if (auto) return ;
			UiUtils.messageBox(this,"Update",
				"Application is up-to-date.\n"+
				"Local version is "+ver.version+"\n"+
				"Host version is "+v,
				JOptionPane.INFORMATION_MESSAGE);
		} else {
			int i=JOptionPane.showConfirmDialog(this,
				"New update is available "+v+".\nDo you want to get it now?",
				"Update",
				JOptionPane.YES_NO_OPTION);
			if (i == JOptionPane.YES_OPTION){
				new Thread(){
					public void run(){
						try{
							if (Version.getDownload(UPDATEHOST))
								invokeLater(updateOKRun);
						}catch (Exception e) {log.error(e);}
					}
				}.start();
			}
		}
	}
	static public void showHistory(ChannelDef cdef) {
		final GraphView view = new GraphView(true);
		view.getPlot().setRange(historyTimeRange, 0);
		view.setChnInfo(new ViewChannelsInfo(cdef, connector, view));
		view.getPlot().setPreferredSize(new Dimension(800, 500));
		JFrame f = new JFrame(cdef.descr) {
			public void dispose() {
				super.dispose();
				views.remove(view.getName());
				log.debug("dispose view name: " + view.getName());
				view.dispose();
				System.gc();
			}
		};
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		UiUtils.setIcon(f, icon);
		views.put(view.getName(), view);
		log.debug("registered view name: " + view.getName());
		f.setContentPane(view);
		f.pack();
		f.setLocation(50, 0);
		f.setVisible(true);
	}

	public ChannelDef getChannelDef(String n) {
		if (n==null || n.length()==0) return null;
		for (int i = 0; i < chndefs.size(); ++i)
			if (chndefs.get(i).name.equals(n))
				return chndefs.get(i);
		return null;
	}
	public ChannelDef getChannelDef(int id) {
		if (id==0) return null;
		for (int i = 0; i < chndefs.size(); ++i)
			if (chndefs.get(i).id==id)
				return chndefs.get(i);
		return null;
	}

	private void setInfo(String info) {
		if (info == null)
			bigInfo.setVisible(false);
		else {
			log.info("%s",info);
			if (splash != null && splash.isVisible()) {
				int i;
				if ((i = info.indexOf('<')) >= 0)
					splash.setText(info.substring(0, i));
				else
					splash.setText(info);
				return;
			}
			//log.debug("biginfo: %s",info);
			if (info.indexOf('<') >= 0){
				bigInfo.setText("<html><center>" + info);
			}
			else
				bigInfo.setText(info);
			bigInfo.setVisible(true);
		}
	}

	private void selectAll(JPanel page) {
		if (page == null) return;
		for (int i = 0; i < page.getComponentCount(); ++i) {
			JComponent c = (JComponent) page.getComponent(i);
			if (c instanceof GraphView) {
				GraphView gv = (GraphView) c;
				gv.getPlot().setSelected(true);
			}
		}				
	}
	private void deselectAll(JPanel page) {
		if (page == null) return;
		for (int i = 0; i < page.getComponentCount(); ++i) {
			JComponent c = (JComponent) page.getComponent(i);
			if (c instanceof GraphView) {
				GraphView gv = (GraphView) c;
				gv.getPlot().setSelected(false);
			}
		}		
	}
	private void setCurrent(JPanel page) {
		if (page != null) setInfo(null);
		if (curPage == page) return;
		
		deselectAll(curPage);

		if (scroll != null) {
			remove(scroll);
			scroll.removeAll();
			scroll = null;
		}
		int i;
		for (i=0; i<pageMenu.getItemCount(); ++i){
			pageMenu.getItem(i).setEnabled(true);
			//pageMenu.getItem(i).setForeground(UIManager.getColor("MenuItem"));
		}
		curPage = page;
		if (curPage == null) {
			validate();
			return;
		}
		//clear selection
		/*for (i = 0; i < page.getComponentCount(); ++i) {
			JComponent c = (JComponent) page.getComponent(i);
			if (c instanceof GraphView) {
				GraphView g=(GraphView)c;
				g.getPlot().setChecked(false);
			}
		}*/

		i=pages.indexOf(curPage);
		pageMenu.getItem(i).setEnabled(false);

		scroll = new JScrollPane(curPage);
		scroll.setWheelScrollingEnabled(true);
		scroll.getVerticalScrollBar().setUnitIncrement(20);
		add(scroll, BorderLayout.CENTER);
		((JFrame) getTopLevelAncestor()).setTitle(appname + " ["
				+ curPage.getName() + "]");
		componentResized();
	}

	private JPanel createPage(int cols, String name) {
		JPanel page = new JPanel(new GridLayout(0, cols, 5, 5));
		if (name == null) name = "Page " + (pages.size() + 1);
		page.setName(name);		
		return page;
	}
	private void addPage(JPanel page) {
		pages.add(page);
		buildPageMenu();
	}

	private void delPage() {
		int i;
		i = pages.indexOf(curPage);
		if (i < 0) return;
		i = JOptionPane.showConfirmDialog(this, "Do you really want to delete this page?",
				"Delete this page",
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		if (i != JOptionPane.YES_OPTION) return;
		JPanel page = curPage;
		for (i = 0; i < page.getComponentCount(); ++i) {
			JComponent c = (JComponent) page.getComponent(i);
			if (c instanceof MeasView) {
				MeasView view = (MeasView) c;
				chnMon.remove(view.vci);
				views.remove(view.getName());
				view.dispose();
				view = null;
			}
		}
		i = pages.indexOf(curPage);
		pages.remove(i);
		curPage=null;
		if (pages.size() > 0) {
			if (i >= pages.size())
				i = pages.size() - 1;
			setCurrent(pages.get(i));
		} else
			setCurrent(null);
		buildPageMenu();
		cfgChanged=true;
		saveLocalConfig();
	}

	private boolean confPage(JPanel page){
		if (page == null) return false;
		//PageConfig p = new PageConfig(chndefs);
		PageConf p = new PageConf(chndefs);
		p.createSetup(page);
		UiUtils.showDialog(this,p,page.getName()+" layout configuration");
		if (p.getResult() == 1) {
			setPageSetup(page,p.getName(),p.getColumns(),p.getViewInfo());
			cfgChanged=true;
			saveLocalConfig();
			return true;
		}
		return false;
	}

	public void setPageSetup(JPanel page, String name, int cols, List<ViewChannelsInfo> vcl) {
		boolean namechng = false;
		if (name != null && name.length() > 0)
			namechng = !name.equals(page.getName());
		log.debug("pgname=%s cols=%d views=%d changed=%b",name,cols,vcl.size(),namechng);
		
		//remove current
		for (int i = 0; i < page.getComponentCount(); ++i) {
			JComponent c = (JComponent) page.getComponent(i);
			if (c instanceof MeasView) {
				MeasView view = (MeasView) c;
				chnMon.remove(view.vci);
				views.remove(view.getName());
				//view.dispose();
				view = null;
			}
		}
		page.removeAll();
		page.setLayout(new GridLayout(0, cols, 5, 5));
		
		for (int i = 0; i < vcl.size(); ++i) {
			ViewChannelsInfo vci=vcl.get(i);
			if (vci==null || vci.defs.size()==0)	
				{page.add(new JLabel());continue;}			
			//GraphView view = new GraphView(false);
			GraphView view=(GraphView)vci.view;
			page.add(view);
			vci.rcv=connector;
			view.getPlot().setRange(monitorTimeRange, 0);
			view.setChnInfo(vci);
			chnMon.add(view.vci);
			views.put(view.getName(), view);
		}
		if (namechng) {
			page.setName(name);
			buildPageMenu();
			((JFrame) getTopLevelAncestor()).setTitle(appname + " ["
					+ page.getName() + "]");
		}
		componentResized();
	}

	//called from connector thread
	public void connected() {
		connector.serverLog(log.getBuffer());
		log.setBuffered(false);
		isConnected=true;
		setupMenus();

		if (chndefs.size() == 0) {
			setInfo("Getting definitions...<br>Please Wait ...");

			java.util.Properties p=System.getProperties();
			log.setBuffered(true);
			for (java.util.Enumeration<Object>i=p.keys(); i.hasMoreElements();) {
				Object k=i.nextElement();
				if (k.toString().indexOf("name")>=0
						||k.toString().indexOf("user")>=0
						||k.toString().indexOf("os")>=0
						)
					log.info("%s = %s",k.toString(),p.get(k).toString());
			}
			connector.serverLog(log.getBuffer());
			log.setBuffered(false);

			// user.country,user.language
			String id = System.getProperty("user.name") + ","
					+ System.getProperty("user.language")+","
					+ System.getProperty("user.country");
			connector.getConfig(id);
		} else {
			stopSplash();
			//setInfo("Server Connected");
			setInfo(null);
			setupMenus();
			
			log.info("lastRecived %s",dfFull.format(new Date(tmLastReceived*1000)));
			log.info("currentTime %s",dfFull.format(new Date(System.currentTimeMillis())));
			/*if (tmLastReceived+60*60<System.currentTimeMillis()/1000) {
				for (int i=0; i<chnMon.size(); ++i){
					ChannelInfo ci=chnMon.get(i);
					if (ci.view instanceof GraphView){
						GraphView gv=(GraphView)ci.view;
						gv.currentTime(System.currentTimeMillis()/1000);
						gv.viewChanged(gv.getPlot(),gv.getPlot().getView(),PlotXY.CHANGED_W);
					}
				}
			}
			else */
			if (tmLastReceived+5*60<System.currentTimeMillis()/1000) {
				for (int i=0; i<chnMon.size(); ++i){
					ViewChannelsInfo ci=chnMon.get(i);
					if (ci.view instanceof GraphView){
						GraphView gv=(GraphView)ci.view;
						Rectangle2D rv=new Rectangle2D.Double();
						rv.setFrame(gv.getPlot().getView());
						gv.currentTime(System.currentTimeMillis()/1000);
						gv.viewChanged(gv.getPlot(),rv,PlotXY.CHANGED_X);
					}
				}
			}
		}
	}

	//called from connector thread
	public void disconnected() {
		log.setBuffered(true);
		log.debug("disconnected, was %b",isConnected);
		log.info("lastRecived %s",dfFull.format(new Date(tmLastReceived*1000)));
		boolean wasConn=isConnected;
		isConnected=false;
		if (wasConn){ // try reconnect
			log.info("reconecting");
			SysUtil.delay(2*SysUtil.SECOND); // don't retry too fast
			invokeLater(connRun);
			return;
		}
		dbHost=null;
		setupMenus();
		stopSplash();
		if (getTopLevelAncestor().isVisible())
			setInfo("Disconnected<br>Please try to connect");
	}

	//called from connector thread
	public void exception(final Exception e) {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					stopSplash();
					AudioPlayerWav.play("ding");
					UiUtils.messageBox(MeasMonitor.this,
							"Connection Exception", e.getClass()
									.getSimpleName()
									+ "\n" + e.getMessage(),
							JOptionPane.ERROR_MESSAGE);
				}
			});
		} catch (Exception e2) {
		}
	}

	//called from connector thread
	public void rcvdConfig(final List<ChannelDef> defs) {
		log.debug("rcvdConfig: %d defs", defs.size());
		chndefs.clear();
		chndefs.addAll(defs);
		defs.clear();
		if (chndefs.size()==0)
			setInfo("No channels availabe");
		else connector.getAlarms();
		
		GraphView.setAvailDefs(chndefs);

		setInfo("Building page views<br>Please Wait ...");
		loadLocalConfig(false);
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				setInfo("Building page views<br>Done.");
				buildPageMenu();
				setupMenus();
				if (pages.size() > 0) {
					setCurrent(pages.get(0));
				}
				else setCurrent(null);
				stopSplash();
			}			
		});

/*		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				checkUpdate(true);
			}
		});*/
	}
	
	//called from connector thread
	public void rcvdAlarmConfig(ChannelDef def) {
		ChannelDef pdef=getChannelDef(def.id);
		if (pdef==null) {
			log.error("no def for id=%d",def.id);
			return ;
		}
		if (def.lLimits!=null || def.uLimits!=null)
			log.debug("alarms id=%d: low=[%s]  up=[%s]",def.id,
				StrUtil.implode(",",def.lLimits), StrUtil.implode(",",def.uLimits));
		pdef.lLimits=def.lLimits;
		pdef.uLimits=def.uLimits;
		for (ViewChannelsInfo vci:chnMon) {
			if (vci.defs.get(0)==pdef) {
				if (vci.view instanceof GraphView){
					GraphView gv=(GraphView)vci.view;
					gv.getPlot().setLimis(pdef.lLimits, pdef.uLimits);
				}						
			}			
		}
	}

	//called from connector thread
	public void rcvdData(List<PntData> v) {
		if (v.size() == 0) return;
		//log.debug("received data size=%d",)
		for (int i=0; i<v.size(); ++i) {
			PntData pd=v.get(i);
			ChannelDef pdef=getChannelDef(pd.id);
			//distribute measured data
			for (ViewChannelsInfo vci:chnMon) {
				if (vci.view instanceof MeasView){
					if (vci.view instanceof GraphView){
						GraphView gv=(GraphView)vci.view;
						if(gv.tmphist) continue;
					}
					vci.view.addValue(pdef,pd.tm,pd.value);						
				}
			}
			if (tmLastReceived < pd.tm) tmLastReceived=pd.tm;	
		}
		
		for (int j=0; j<chnMon.size(); ++j) {
			ViewChannelsInfo pi=chnMon.get(j);
			if (pi.view instanceof GraphView){
				GraphView gv=(GraphView)pi.view;
				if(gv.tmphist) continue;
			}
			pi.view.currentTime(tmLastReceived);
		}
	}

	//called from connector thread
	public void rcvdData(String key, List<PntData> v) {
		if (v.size() == 0) return;
		MeasView view = views.get(key);
		if (view != null) {
			//log.debug("recvd data for %s",key);
			PntData pd=v.get(v.size()-1);
			ChannelDef pdef=getChannelDef(pd.id);
			if (tmLastReceived < pd.tm) tmLastReceived=pd.tm;
			view.addValues(pdef,v);
			
		}
		else log.error("no view for key='%s'", key);
	}

	//called from connector thread
	public void rcvdMsg(final String msg) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				UiUtils.messageBox(MeasMonitor.this, "Server Message", msg,
						JOptionPane.INFORMATION_MESSAGE);
			}
		});
	}
	
	public void loadLocalConfig(boolean defaults) {
		if (chndefs.size()==0) return;
		
		for (JPanel page:pages) {
			for (int i = 0; i < page.getComponentCount(); ++i) {
				JComponent c = (JComponent) page.getComponent(i);
				if (c instanceof MeasView) {
					MeasView view = (MeasView) c;
					chnMon.remove(view.vci);
					views.remove(view.getName());
					view.dispose();
					view = null;
				}
			}
			page.removeAll();
		}
		pages.clear();

		Properties p = new Properties();
		InputStream io=null;
		try {
			if (!defaults && new File(localcfg).exists())
				io = new FileInputStream(localcfg);
			else
				io = Resource.getResourceURL("res/"+CONFIG_NAME).openStream();
			p.load(io);
		} catch (Exception e) {
			log.debug(e);
			return;
		}
		finally {
			IOUtils.close(io);
		}
		String v;
		for (int i = 0; (v = p.getProperty("page[" + i + "]")) != null; ++i) {
			String[] row = v.split(",");
			final int cols = Integer.parseInt(row[0]);
			final String name = row.length > 1 ? row[1]:null;
			addPage(createPage(cols,name));
		}
		if (pages.size() > 0) {
			for (int i = 0; (v = p.getProperty("view[" + i + "]")) != null; ++i) {
				//pgno,id,name,className,....
				String[] row = v.split(",");
				int pg = Integer.parseInt(row[0]);
				if (pg >= pages.size()) {
					log.debug("page %d >= %d", pg, pages.size());
					continue;
				}
				int ri=1;
				int id=0;
				if (row[ri].matches("^[0-9]+$")){
					id=Integer.parseInt(row[ri++]);
				}
				String name = row[ri++];
				JPanel page = pages.get(pg);
				setInfo("Building " + page.getName() + " " + name+ "<br>Please Wait ...");
				if ("null".equals(name)) {
					page.add(new JLabel());
					continue;
				}
				ChannelDef pd = null;
				ViewChannelsInfo vci=new ViewChannelsInfo();
				vci.rcv=connector;
				if (id>0) {
					pd=getChannelDef(id);
					if (pd!=null) vci.defs.add(pd);
				}
					
				String cln = row[ri++];
				MeasView view = null;
				if (row.length < 3) {
					GraphView g = new GraphView(false);
					g.setRange(monitorTimeRange, 0);
					view = g;
				}
				else if ("GraphView".equals(cln)) {
					double xrng=monitorTimeRange,y0=0,yrng=0;
					GraphView g = new GraphView(false);
					int chn=0;
					for (; ri<row.length; ++ri) {
						if (row[ri].indexOf("=")>0){
							String[] kn=row[ri].split("=",2);
							if (kn[0].equals("w")) xrng=Double.parseDouble(kn[1]);
							else if (kn[0].equals("y")) y0=Double.parseDouble(kn[1]);
							else if (kn[0].equals("h")) yrng=Double.parseDouble(kn[1]);
							else if (kn[0].equals("ch")) {
								id=Integer.parseInt(kn[1]);
								pd=getChannelDef(id);
								if (pd==null) {
									pd=new ChannelDef(id, "id-"+id);
									pd.unit="N/A";
									pd.descr=String.format("no def on server [%s]",pd.name);
								}
								vci.defs.add(pd);
								++chn;
							}
							else if (kn[0].equals("c")) {
								int rgb=Integer.parseInt(kn[1],16);
								g.getPlot().getParams(chn-1).color=new Color(rgb);
							}
						}
						else
							xrng=Double.parseDouble(row[ri]);
					}
					if (xrng > 48) xrng = 48;
					g.getPlot().setView(0,y0,xrng,yrng);
					view = g;
				}
				else if ("BarView".equals(cln)) {
					BarView g = new BarView();
					/*double y0=0,yrng=0;
					for (int ri=3; ri<row.length; ++ri) {
						String[] kn=row[ri].split("=",2);
						if (kn[0].equals("y")) y0=Double.parseDouble(kn[1]);
						else if (kn[0].equals("h")) yrng=Double.parseDouble(kn[1]);						
					}*/
					view = g;
				}
				else {
					page.add(new JLabel(name + "/" + cln + " ?"));
					continue;
				}
				if (vci.defs.size() > 0) {
					view.setChnInfo(vci);
					chnMon.add(view.vci);
					page.add(view);
					views.put(view.getName(), view);
				}
				else {
					page.add(new JLabel());
				}
			}
		}
	}

	public void saveLocalConfig() {
		if (locked || !cfgChanged) return ;
		cfgChanged=false;
		if (pages.size() == 0) {
			if (chndefs.size()>0) {new File(localcfg).delete();}
			return;
		}
		Properties p = new Properties();
		for (int i = 0; i < pages.size(); ++i) {
			JPanel page = pages.get(i);
			LayoutManager lm = page.getLayout();
			int cols = 3;
			if (lm instanceof GridLayout)
				cols = ((GridLayout) lm).getColumns();
			p.put("page[" + i + "]", cols + "," + page.getName());
		}
		StringBuffer b = new StringBuffer();
		int vi = 0;
		for (int i = 0; i < pages.size(); ++i) {
			JPanel page = pages.get(i);
			for (int j = 0; j < page.getComponentCount(); ++j) {
				b.setLength(0);
				JComponent c = (JComponent) page.getComponent(j);
				b.append(String.format("%d", i));//page nr
				if (c instanceof MeasView) {
					MeasView mv=(MeasView)c;
					b.append(String.format(",%s",mv.vci.defs.get(0).name));
					b.append(String.format(",%s",c.getClass().getSimpleName()));					
					if (c instanceof GraphView) {
						Rectangle2D view=((GraphView)c).getPlot().getView();
						b.append(String.format(Locale.US, ",w=%.2f",view.getWidth()));
						b.append(String.format(Locale.US, ",y=%.2f,h=%.2f",view.getY(),view.getHeight()));
					}
					int chn=0;
					for (ChannelDef def: mv.vci.defs) {
						b.append(String.format(",ch=%d",def.id));
						if (c instanceof GraphView) {
							ChannelParams par=((GraphView)c).getPlot().getParams(chn);
							b.append(String.format(",c=%06X",par.color.getRGB()&0xffffff));
						}
						++chn;
					}
				} else b.append(",null");
				p.put("view[" + vi + "]", b.toString());
				++vi;
			}
		}
		try {
			FileOutputStream io = new FileOutputStream(localcfg);
			p.store(io, "MeasMonitor Config");
			io.close();
		} catch (Exception e) {
			log.debug(e);
		}
	}
	
	static void startSplash(){
		if (splash!=null){
			splash.setVisible(true);
			splash.toFront();
			return ;
		}
		try {
			splash = new SplashScreen(ImageIO.read(Resource.getResourceURL("res/na61mm.jpg")),"Loading...");
			//if (icon!=null) splash.setIconImage(icon.getImage());
			UiUtils.setIcon(splash, icon);
		} catch (Exception e) {
			log.error(e);
		}
	}
	static void stopSplash(){
		if (splash!=null) splash.setVisible(false);
	}

	public static void main(String[] args) {
		localcfg = System.getProperty("user.home") + File.separator;
		if (SysUtil.isUnix()) localcfg += ".meas" + File.separator;

		new File(localcfg).mkdirs();
		localcfg += CONFIG_NAME;
		log.setLevel(3);
		log.setBuffered(true);
		log.debug("localcfg=%s", localcfg);
		
		try {
			icon = new ImageIcon(ImageIO.read(Resource .getResourceURL("res/meas.jpg")));
		} catch (Exception e) {}
		UiUtils.setupUIManagerFonts();
		startSplash();
		
		for (int i=0; i<args.length; ++i){
			String a=args[i];
			int p=a.indexOf("=");
			if (p==0||a.length()==0) continue;
			if (a.equals("locked")) locked=true;
			else if (a.startsWith("host=")) forceHost=a.substring(5);
		}

		startGUI(appname,MeasMonitor.class);		
	}
}
