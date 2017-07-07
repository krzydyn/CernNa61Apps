package cern.hv;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import caen.CaenCrate;
import caen.CaenNet;
import caen.N470;
import caen.SY527;
import common.Version;
import common.connection.Connection;
import common.ui.UiUtils;

@SuppressWarnings("serial")
public class CeanNetBrowser extends JPanel implements ActionListener {
	final static String appname="HV Control";
	final static String appdescr=null;
	final static String author="Krzysztof Dynowski";

	String addr="na61dcs1.cern.ch:31514";
	private final CaenNet caen=new CaenNet();
	private final JComboBox<String> cratescb=new JComboBox<String>();
	private final ArrayList<CaenCrate> crates=new ArrayList<CaenCrate>();
	public CeanNetBrowser()
	{
		super(new GridBagLayout());
		GridBagConstraints constr = new GridBagConstraints();
		constr.fill = GridBagConstraints.NONE;
		constr.anchor = GridBagConstraints.LINE_START;
		JButton b;
		add(new JLabel("CAENet:"));
		add(b=new JButton("scan"),constr);
		b.setActionCommand("scan_net");
		b.addActionListener(this);
		constr.gridwidth=GridBagConstraints.REMAINDER;
		add(new JLabel(),constr);

		constr.gridwidth=1;
		add(cratescb,constr);
		add(b=new JButton("scan"),constr);
		b.setActionCommand("scan_crate");
		b.addActionListener(this);
		constr.gridwidth=GridBagConstraints.REMAINDER;
		add(new JLabel(),constr);
	}
	public JMenuBar buildMenuBar()
	{
		return null;
	}

	public void actionPerformed(ActionEvent ev) {
		final String cmd=ev.getActionCommand();
		if ("quit".equals(cmd))
		{
			((Window)getTopLevelAncestor()).dispose();
			System.exit(0); return ;
		}
		if ("about".equals(cmd))
		{
			Version v=Version.getInstance();
			UiUtils.messageBox(this,"About",appname+", version "+v.version+"\n"+
					(appdescr!=null?appdescr:"")+"\n"+
					"by "+author+"\n"+
					"Build "+v.buildno+" on "+v.buildtime,
					JOptionPane.INFORMATION_MESSAGE);
		}
		else if ("scan_net".equals(cmd))
			try{scan_net();}catch (Exception e) {}
		else if ("scan_crate".equals(cmd))
			try{scan_crate();}catch (Exception e) {}
	}

	public void scan_net() throws ConnectException, IOException
	{
		StringBuilder b=new StringBuilder();
		Connection c = Connection.getConnection(addr);
		c.connect();
		caen.setIO(c);
		caen.open();
		for (int cr=1; cr<5; ++cr)
		{
			if (caen.readIdent(cr,b)<=0) break;
			CaenCrate m=null;
			if (b.indexOf("SY527")>=0) m=new SY527();
			else if (b.indexOf("N470")>=0) m=new N470();
			else m=new CaenCrate();
			m.setCaenNet(caen);
			m.setAddress(cr);
			m.setName(b.toString());
			crates.add(m);
		}
		caen.close();
		c.disconnect();
		cratescb.removeAllItems();
		for (int cr=0; cr<crates.size(); ++cr)
			cratescb.addItem(crates.get(cr).getName());
	}
	public void scan_crate() throws ConnectException, IOException
	{

	}


	public static void main(String[] args) {
		Version.createInstance(CeanNetBrowser.class);
		final CeanNetBrowser panel=new CeanNetBrowser();
		JFrame f=new JFrame(appname+" "+Version.getInstance());
		f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		panel.setOpaque(true);
		f.setJMenuBar(panel.buildMenuBar());
		f.setContentPane(panel);
		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e)
			{
				panel.actionPerformed(new ActionEvent(e.getSource(),e.getID(),"quit"));
			}
		});
		f.setMinimumSize(new Dimension(400,300));
		f.pack();
		f.setLocation(50,0);
		f.setVisible(true);
	}
}
