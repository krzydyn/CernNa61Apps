package cern.pulser;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import cern.pulser.Pulser.Firmware;
import cern.pulser.Pulser.Voltage;
import cern.pulser.Pulser.Waveform;
import common.Logger;
import common.Version;
import common.connection.link.AbstractLink;
import common.connection.link.AbstractLink.LinkStateListener;
import common.crypt.Encryptor;
import common.ui.AudioPlayerWav;
import common.ui.UiUtils;
import common.util.Resource;
import conn.AbstrConn.ConnectorListener;
/*
 * Changelog
 * v1.04 improved smooth editor feature
 * v1.03 move operation
 * v1.02 non-persistent connections
 * v1.01 waveform editor
 * v1.00 initial
 */
@SuppressWarnings("serial")
public class PulserControl extends JPanel implements ActionListener, ConnectorListener, LinkStateListener {
	final static Logger log = Logger.getLogger();
	final static String appname = "Pulser Control";
	final static String appdescr = "Calibration Pulser";
	final static String author = "Krzysztof.Dynowski@cern.ch";
	
	final static String[] ADDRs={"na61dcs1.cern.ch:10001","na61dcs1.cern.ch:10002","localhost:10001","localhost:10002"};
	//final static String[] ADDRs={"localhost:10001","localhost:10002"};

	private final PulserConnector connector=new PulserConnector(this);
	private boolean locked=true;

	final static Border border=BorderFactory.createBevelBorder(BevelBorder.LOWERED);
	private static final Font fontTitle = new Font("Verdana", Font.BOLD, 25);
	private final JButton conn,dconn,unlock;
	private final JLabel rx, tx;
	private final JComboBox<String> address=new JComboBox<String>(ADDRs);
	private final JTextField firmware=new JTextField(18);
	private final JTextField voltage=new JTextField(18);
	private final JCheckBox[] clk=new JCheckBox[3];
	private final JPanel modules=new JPanel(new GridLayout(1,0,2,2));
	private final WavePanel wavepan=new WavePanel(connector);

	public PulserControl() {
		super(new BorderLayout(1,1));
		setPreferredSize(new Dimension(900,450));
		JPanel p;
		JButton b;
		JLabel l;
		p = new JPanel(null);
		p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
		ImageIcon icon=null;
		try{icon=new ImageIcon(ImageIO.read(Resource.getResourceURL("res/pulserna61.jpg")));}
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

		add(buildControlPanel(),BorderLayout.WEST);
		add(p=wavepan,BorderLayout.CENTER);
		p.setBorder(border);

		dconn.setEnabled(false);
		rx.setOpaque(true);
		tx.setOpaque(true);
		firmware.setEditable(false);
		firmware.setBorder(BorderFactory.createLineBorder(Color.GRAY,1));
		firmware.setHorizontalAlignment(JTextField.RIGHT);
		voltage.setEditable(false);
		voltage.setBorder(BorderFactory.createLineBorder(Color.GRAY,1));
		voltage.setHorizontalAlignment(JTextField.RIGHT);

		connector.setConnectorListener(this);
		modules.setPreferredSize(new Dimension(400,300));
	}
	public JMenuBar buildMenuBar() {
		return null;
	}
	private JPanel buildControlPanel(){
		JPanel p = new JPanel(new GridBagLayout());
		Dimension sz;
		p.setBorder(border);
		GridBagConstraints constr = new GridBagConstraints();
		constr.anchor=GridBagConstraints.LINE_START;
		constr.insets=new Insets(1,1,1,1);
		constr.weightx=0;constr.weighty=0;
		JButton b;
		constr.fill=GridBagConstraints.NONE;
		p.add(new JLabel("Address"),constr);
		constr.fill=GridBagConstraints.HORIZONTAL;
		constr.weightx=1;
		constr.gridwidth=3;
		p.add(address,constr);
		(sz=address.getPreferredSize()).height=18;
		address.setPreferredSize(sz);
		constr.gridwidth=GridBagConstraints.REMAINDER;
		constr.fill=GridBagConstraints.NONE;
		constr.weightx=0;
		p.add(new JLabel(),constr);

		constr.gridwidth=1;
		p.add(new JLabel("Firmware"),constr);
		constr.fill=GridBagConstraints.HORIZONTAL;
		constr.weightx=1;
		constr.gridwidth=3;
		p.add(firmware,constr);
		constr.fill=GridBagConstraints.NONE;
		constr.weightx=0;
		constr.gridwidth=GridBagConstraints.REMAINDER;
		p.add(b=new JButton("read"),constr);
		b.addActionListener(this);
		b.setActionCommand("firmware");
		(sz=b.getPreferredSize()).height=18;
		b.setPreferredSize(sz);

		constr.gridwidth=1;
		p.add(new JLabel("Voltage"),constr);
		constr.fill=GridBagConstraints.HORIZONTAL;
		constr.weightx=1;
		constr.gridwidth=3;
		p.add(voltage,constr);
		constr.fill=GridBagConstraints.NONE;
		constr.weightx=0;
		constr.gridwidth=1;
		p.add(b=new JButton("read"),constr);
		b.addActionListener(this);
		b.setActionCommand("vread");
		(sz=b.getPreferredSize()).height=18;
		b.setPreferredSize(sz);
		constr.gridwidth=GridBagConstraints.REMAINDER;
		p.add(b=new JButton("rst"),constr);
		b.addActionListener(this);
		b.setActionCommand("vrst");
		(sz=b.getPreferredSize()).height=18;
		b.setPreferredSize(sz);

		constr.gridwidth=1;
		p.add(new JLabel("Clock"),constr);
		constr.weightx=1;
		clk[0]=new JCheckBox("Ext");
		p.add(clk[0],constr);
		clk[1]=new JCheckBox("iON");
		p.add(clk[1],constr);
		clk[2]=new JCheckBox("Txt");
		p.add(clk[2],constr);
		constr.weightx=0;
		constr.gridwidth=GridBagConstraints.REMAINDER;
		p.add(b=new JButton("set"),constr);
		b.addActionListener(this);
		b.setActionCommand("setclk");
		(sz=b.getPreferredSize()).height=18;
		b.setPreferredSize(sz);
		
		constr.fill=GridBagConstraints.BOTH;
		constr.weighty=1;
		p.add(modules,constr);
		return p;
	}
	
	public void actionPerformed(ActionEvent ev) {
		final String cmd = ev.getActionCommand();
		log.info("Action: %s",cmd);
		if ("quit".equals(cmd)) {
			disconnect();
			((Window)getTopLevelAncestor()).dispose();
			System.exit(0);
			return;
		}
		if ("about".equals(cmd)) {
		} else if ("help".equals(cmd)){
			UiUtils.showHelp(this,"Pulser Help",Resource.getResourceURL("res/help/pulser/index.htm"));
		} else if ("conn".equals(cmd)) {
			connect();
		} else if ("disconn".equals(cmd)) {
			disconnect();
		} else if ("unlock".equals(cmd)) {
			setLocked(!locked);
		} else if ("firmware".equals(cmd)) {
			firmware.setText("");
			connector.readFirmware();
		} else if ("vread".equals(cmd)) {
			voltage.setText("");
			connector.readVoltage();
		} else if ("vrst".equals(cmd)) {
			voltage.setText("");
			connector.resetVoltage();
			connector.readVoltage();
		} else if ("setclk".equals(cmd)) {
			int m=0;
			for (int i=0; i<clk.length; ++i)
				if (clk[i].isSelected()) m|=1<<i;
			connector.writeClockMask(m);
			connector.readClockMask();
		} 
	}

	private void connect() {
		if (connector==null) return ;
		String a=(String)address.getSelectedItem();
		connector.setAddr(a);
		conn.setEnabled(false);
		wavepan.setWaveform(null);
		connector.start();
	}
	private void disconnect() {
		if (connector==null) return ;
		connector.stop();
	}
	public boolean isLocked() { return locked; }
	private void setLocked(boolean lock) {
		if (lock){
			if (locked) return ;
			locked=true;
		}
		else{
			if (!showConfim(this,"Are you sure to enter UNLOCK mode?")) return ;
			locked=false;
		}
		unlock.setText(locked ? "Unlock" : "Lock");
		log.info("Application %s",locked?"LOCKED":"UNLOCKED");
	}

	public void connected(){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				dconn.setEnabled(true);
				connector.readFirmware();
				connector.readVoltage();
				connector.readClockMask();
				connector.readModules();
				connector.readWaveformNr();
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
	private void responseGUI(int id){
		int cmd=id&0xff;
		id=(id>>16)&0xff;
		if (cmd==Pulser.CMD_FIRMVARE){
			Firmware f=connector.getFirmware();
			firmware.setText(String.format("%s %s %s",f.factory,f.name,f.ver));
		}
		else if (cmd==Pulser.CMD_READ_VMON){
			Voltage f=connector.getVoltage();
			voltage.setText(String.format("F:%s  5V:%s  %s  %s  %s  %s",Integer.toBinaryString(f.pwrFail),f.pwrAnalog==1?"ok":"!ok",f.v1,f.v2,f.v3,f.v4));
		}
		else if (cmd==Pulser.CMD_READ_CLOCK){
			int m=connector.getClockMask();
			log.debug("clk mask=%d",m);
			for (int i=0; i<clk.length; ++i)
				clk[i].setSelected((m&(1<<i))!=0);
		}
		else if (cmd==Pulser.CMD_MODULES){
			modules.removeAll();
			for (int i=0; i<7; ++i) {
				JComponent c;
				if (connector.hasModule(i)){
					c=new PulserModule(connector,i);
					connector.readPowerStatus(i);
				}
				else {
					c=new JLabel("N/A",JLabel.CENTER);
					c.setOpaque(true);
					c.setBackground(Color.LIGHT_GRAY);
				}
				c.setBorder(border);
				modules.add(c);
			}
			modules.validate();
			for (int i=0; i<7; ++i)
				if (connector.hasModule(i)) connector.readChannels(i);
		}
		else if (cmd==Pulser.CMD_GET_WAVE){
			int nr=connector.getWaveformNr();
			wavepan.setWaveformNr(nr);
			if (wavepan.getWaveform()<0) connector.readWaveform(nr);

		}
		else if (cmd==Pulser.CMD_READ_WAVE){
			Waveform w=connector.getWaveform();
			wavepan.setWaveform(w);
		}
		else if (cmd==Pulser.CMD_READ_POWER){
			JComponent c=(JComponent)modules.getComponent(id);
			if (c instanceof PulserModule) ((PulserModule)c).updatePwr();
			else log.debug("not a PulserModule");			
		}
		else if (cmd==Pulser.CMD_CHANNELS_GET){
			JComponent c=(JComponent)modules.getComponent(id);
			if (c instanceof PulserModule) ((PulserModule)c).updateChns();
			else log.debug("not a PulserModule");
		}		
	}
	
	public void execDone(final int id){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){responseGUI(id);}
		});
	}
	public void readDone(int r, String pv, float[] v) {}
	public void writeDone(int r, String pv) {}

	public void exception(final Exception e) {
		log.error(e);
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				StringBuilder b=new StringBuilder();
				b.append(e.getMessage()+"\n");
				if ((e.getCause())!=null){
					b.append("\nCaused by: "+e.getCause().getMessage());
				}
				AudioPlayerWav.play("ding");
				UiUtils.messageBox(PulserControl.this,"Error",b.toString(),JOptionPane.ERROR_MESSAGE);
			}
		});
	}
	public void stateChanged(final int state) {
		if (state==0) {
			log.debug(new Throwable("bt"),"RXTX zero");
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (state == AbstractLink.STATE_RECV) {
					rx.setBackground(Color.GREEN);
					tx.setBackground(getBackground());
				}
				else if (state == AbstractLink.STATE_SEND) {
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

	static public boolean showConfim(Component p,String msg) {
		String code=String.valueOf(Encryptor.genToken(4));
		String in=JOptionPane.showInputDialog(p,
				msg+"\nEnter confirmation token:  "+code,"Warning",JOptionPane.WARNING_MESSAGE);
		if (in==null) return false;
		return Version.getInstance().DEBUG ? true:code.equals(in);
	}
	
	static void startGUI(){
		ImageIcon icon=null;
		UiUtils.setupUIManagerFonts();
		UIManager.put("Button.margin", new Insets(1, 1, 1, 1));
		final PulserControl panel = new PulserControl();
		final JFrame f = new JFrame(appname+" "+ Version.getInstance());
		try{icon=new ImageIcon(Resource.getResourceURL("res/pulserico.jpg"));}
		catch (Exception e) {}
		if (icon!=null) f.setIconImage(icon.getImage());
		f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		panel.setOpaque(true);
		f.setJMenuBar(panel.buildMenuBar());
		f.setContentPane(panel);
		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				panel.actionPerformed(new ActionEvent(e.getSource(), e.getID(),
						"quit"));
			}
		});
		f.addMouseListener(new MouseAdapter(){
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
	}

	public static void main(String[] args) {
		Version.createInstance(PulserControl.class);
		//loadConfig(args);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() { startGUI(); }
		});
	}
}
