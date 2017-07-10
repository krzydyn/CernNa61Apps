package cern.hv;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;

import sys.Logger;
import sys.ui.DocumentValidator;
import sys.ui.UiUtils;
import caen.HVModule;
import caen.HVModule.ChannelSettings;
import caen.SY527;

@SuppressWarnings("serial")
class HVChannelUI extends JPanel implements ActionListener {
	static Logger log=Logger.getLogger();
	final static Font valuefont=new Font("Courier",Font.PLAIN,12);
	final static Dimension valuesize=new Dimension(50,13);

	/*
	 * Colors in General (from MEDM Reference Manual)
	 *  DIN Standards 4844 and 5381 should be followed whenever possible.
	 *  Red is a danger color. It means Halt, Stop, Prohibited, etc.
	 *  Yellow is used as a warning color.
	 *  Green means Safety, OK, On etc.
	 *  Blue is used for giving directions, advice, signs, etc.
	 */
	final static Color COLOR_DEFAULT=UIManager.getColor("JLabel.background");
	final static Color COLOR_KILLED=Color.LIGHT_GRAY;
	final static Color COLOR_TRIPPED=Color.RED;
	final static Color COLOR_RAMPINGUP=Color.MAGENTA;
	final static Color COLOR_RAMPINGDN=Color.PINK.darker();
	final static Color COLOR_OVERCURR=Color.YELLOW;
	final static Color COLOR_OVERVOLT=Color.BLUE;
	final static Color COLOR_UNDERVOLT=Color.CYAN.darker();
	final static Color COLOR_OFF=Color.DARK_GRAY;
	final static Color COLOR_ON=Color.GREEN.darker();

	private final HVControl ctrl;
	private final JLabel nm,v1,v2;
	private final int slot,chn;
	private HVChannelGroupUI grp;

	private final ActionEvent popupEvent=new ActionEvent(this,0,"popup");
	private final ActionEvent parEvent=new ActionEvent(this,0,"par");
	private final JPopupMenu popup;
	public HVChannelUI(HVControl ctrl,String n,int slot,int chn) {
		super(new GridBagLayout());
		this.ctrl=ctrl;
		this.slot=slot;
		this.chn=chn;

		setName(n);
		setBorder(BorderFactory.createEtchedBorder());

		GridBagConstraints constr = new GridBagConstraints();
		constr.fill = GridBagConstraints.HORIZONTAL;
		constr.gridwidth=GridBagConstraints.REMAINDER;
		constr.weightx=1;

		JLabel l;
		add(l=new JLabel(" "+n+" "),constr);
		nm=l;
		constr.insets=new Insets(0,1,0,1);
		add(l=new JLabel("0.00",JLabel.RIGHT),constr);
		l.setFont(valuefont);
		l.setPreferredSize(valuesize);
		v1=l;
		add(l=new JLabel("0.00",JLabel.RIGHT),constr);
		l.setFont(valuefont);
		l.setPreferredSize(valuesize);
		v2=l;

		nm.setOpaque(true);
		ctrl.addChannelListener(this);

		popup=new JPopupMenu();
		JMenuItem mi;
		popup.add(new JLabel("Set channel",JLabel.CENTER));
		popup.addSeparator();
		popup.add(mi=new JMenuItem("On"));
		mi.setActionCommand("on");
		mi.addActionListener(this);
		popup.add(mi=new JMenuItem("Off"));
		mi.setActionCommand("off");
		mi.addActionListener(this);
		popup.add(mi=new JMenuItem("Ramp by..."));
		mi.setActionCommand("ramp");
		mi.addActionListener(this);
		popup.add(mi=new JMenuItem("Params"));
		mi.setActionCommand("par");
		mi.addActionListener(this);
		addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent ev) {
				if (HVChannelUI.this.ctrl.isLocked()) return ;
				if (ev.getButton()==MouseEvent.BUTTON1 && ev.getClickCount()==2) {
					actionPerformed(parEvent);
				}
				else if (ev.getButton()==MouseEvent.BUTTON3 && ev.getClickCount()==1) {
					popupEvent.setSource(ev);
					actionPerformed(popupEvent);
				}
			}
		});
	}
	public void setChnGroup(HVChannelGroupUI g){grp=g;}

	@Override
	public void actionPerformed(ActionEvent ev) {
		String cmd=ev.getActionCommand();
		HVModule m=ctrl.findModule(slot);
		if (m==null) nm.setBackground(getBackground());
		if ("updCfg".equals(cmd))
		{
			if (m==null) {log.error("no mod for slot=%d",slot);return ;}
			ChannelSettings s=m.getChannelSettings(chn);
			super.setToolTipText(String.format("Chn[%d.%d] %s",m.getSlot(),chn,s.name));
		}
		else if ("updChn".equals(cmd))
		{
			if (m==null) return ;
			ChannelSettings cs=m.getChannelSettings(chn);
			v1.setText(HVModule.scaledValue(cs.v0set,m.vdec));
			v2.setText(HVModule.scaledValue(cs.vread,m.vdec));
			if (cs.status==0) nm.setBackground(COLOR_DEFAULT);
			else if ((cs.flag&(1<<14))==0) nm.setBackground(COLOR_OFF);
			else if ((cs.status&(1<<6))!=0) nm.setBackground(COLOR_KILLED);
			else if ((cs.status&(1<<5))!=0) nm.setBackground(COLOR_TRIPPED);
			else if ((cs.status&(1<<9))!=0) nm.setBackground(COLOR_TRIPPED);
			else if ((cs.status&(1<<12))!=0) nm.setBackground(COLOR_OVERCURR);
			else if ((cs.status&(1<<13))!=0) nm.setBackground(COLOR_RAMPINGDN);
			else if ((cs.status&(1<<14))!=0) nm.setBackground(COLOR_RAMPINGUP);
			else if ((cs.status&(1<<10))!=0) nm.setBackground(COLOR_OVERVOLT);
			else if ((cs.status&(1<<11))!=0) nm.setBackground(COLOR_UNDERVOLT);
			else if ((cs.status&(1<<15))==0) nm.setBackground(COLOR_OFF);
			else nm.setBackground(COLOR_ON);
			if (nm.getBackground().equals(Color.BLACK)||
					nm.getBackground().equals(Color.DARK_GRAY))
				nm.setForeground(Color.WHITE);
			else
				nm.setForeground(Color.BLACK);
		}
		else if ("popup".equals(cmd)) {
			int x=0,y=0;
			if (ev.getSource() instanceof MouseEvent)
			{
				MouseEvent me=(MouseEvent)ev.getSource();
				x=me.getX(); y=me.getY();
			}
			popup.show(this,x,y);
		}
		else if ("on".equals(cmd))
		{
			log.debug("chn [%s.%s] set ON",grp.getName(),nm.getText());
			ctrl.setChnParam(slot, chn, SY527.CMD_SetChnMF,(1<<3)|(1<<6)|(1<<11)|(1<<14));
		}
		else if ("off".equals(cmd))
		{
			log.debug("chn [%s.%s] set OFF",grp.getName(),nm.getText());
			ctrl.setChnParam(slot, chn, SY527.CMD_SetChnMF,(1<<11)|(1<<14));
		}
		else if ("ramp".equals(cmd)){
			float scv=(float)Math.pow(10,m.vdec);
			int ramp=0;
			for(String in="";;){
				in=(String)JOptionPane.showInputDialog(ctrl,
						"Ramp channel by: ","Enter value to ramp by (V0set)",JOptionPane.QUESTION_MESSAGE,null,null,in);
				if (in==null) return ;
				if (in.matches(DocumentValidator.FLOATING_SIGNED)){
					ramp=(int)(Float.parseFloat(in)*scv); break;
				}
			}
			log.debug("channel [%s] ramp by %d",grp.getName(),ramp);
			ChannelSettings cs=m.getChannelSettings(chn);
			ctrl.setChnParam(slot,chn,SY527.CMD_SetChnV0SET,cs.v0set+ramp);
		}
		else if ("par".equals(cmd))
		{
			HVChannelParamsUI par=new HVChannelParamsUI();
			String n=String.format("Set %s[%s] ",grp.getName(),nm.getText());
			if (m!=null)
			{
				par.setProperties(m.getChannelSettings(chn),m.vdec,m.idec);
			}
			UiUtils.showDialog(this,par,n);
			if (par.getResult()==1 && m!=null)
			{
				ChannelSettings csold=m.getChannelSettings(chn);
				ChannelSettings cs=new ChannelSettings();
				csold.copySettings(cs);
				par.getProperties(cs,m.vdec,m.idec);
				ctrl.setChnSettings(slot, chn, csold, cs);
			}
		}
	}
}
