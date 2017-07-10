package cern.hv;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import sys.Logger;

@SuppressWarnings("serial")
public class HVTPCView extends JPanel {
	static Logger log=Logger.getLogger();
	final static Font infofont=new Font("Courier",Font.PLAIN,12);
	final static Border infoborder=BorderFactory.createEtchedBorder(Color.ORANGE,Color.YELLOW);
	final static Border boxborder=BorderFactory.createLineBorder(Color.LIGHT_GRAY.brighter());

	private final HVChannelGroupUI lmPD=new HVChannelGroupUI(0,1,5);
	private final HVChannelGroupUI gTPC=new HVChannelGroupUI(-1,1,1);
	private final HVChannelGroupUI fTPC1=new HVChannelGroupUI(-1,1,1);
	private final HVChannelGroupUI fTPC2=new HVChannelGroupUI(-1,1,1);
	private final HVChannelGroupUI fTPC3=new HVChannelGroupUI(-1,1,1);

	private final HVChannelGroupUI vTPC1=new HVChannelGroupUI(0,1,5);
	private final HVChannelGroupUI vTPC2=new HVChannelGroupUI(0,1,5);
	private final HVChannelGroupUI mTPCLs=new HVChannelGroupUI(4,1,1);
	private final HVChannelGroupUI mTPCLh=new HVChannelGroupUI(4,1,1);
	private final HVChannelGroupUI mTPCRh=new HVChannelGroupUI(4|(2<<8),1,1);
	private final HVChannelGroupUI mTPCRs=new HVChannelGroupUI(4|(2<<8),1,1);

	private final HVChannelGroupUI vTPC1p=new HVChannelGroupUI(-1,1,1);
	private final HVChannelGroupUI vTPC2p=new HVChannelGroupUI(-1,1,1);
	private final HVChannelGroupUI mTPCLp=new HVChannelGroupUI(-1,1,10);
	private final HVChannelGroupUI mTPCRp=new HVChannelGroupUI(-1,1,10);

	public HVTPCView(HVControl ctrl)
	{
		super(null);
		lmPD.create(ctrl,ctrl.getChannelGroup("lmpd"),2);
		gTPC.create(ctrl,ctrl.getChannelGroup("gtpc"),1);
		fTPC1.create(ctrl,ctrl.getChannelGroup("ftpc1"),1);
		fTPC2.create(ctrl,ctrl.getChannelGroup("ftpc2"),1);
		fTPC3.create(ctrl,ctrl.getChannelGroup("ftpc3"),1);
		vTPC1.create(ctrl,ctrl.getChannelGroup("vtpc1"),3);
		vTPC2.create(ctrl,ctrl.getChannelGroup("vtpc2"),3);
		mTPCLs.create(ctrl,ctrl.getChannelGroup("mtpcLs"),5);
		mTPCLh.create(ctrl,ctrl.getChannelGroup("mtpcLh"),5);
		mTPCRh.create(ctrl,ctrl.getChannelGroup("mtpcRh"),5);
		mTPCRs.create(ctrl,ctrl.getChannelGroup("mtpcRs"),5);
		vTPC1p.create(ctrl,ctrl.getChannelGroup("vtpc1p"),1);
		vTPC2p.create(ctrl,ctrl.getChannelGroup("vtpc2p"),1);
		mTPCLp.create(ctrl,ctrl.getChannelGroup("mtpcLp"),1);
		mTPCRp.create(ctrl,ctrl.getChannelGroup("mtpcRp"),1);
		buildLa2();
	}
	void buildLa2()
	{
		JComponent l;
		Insets insets5=new Insets(5,5,5,5);
		setLayout(new GridBagLayout());
		Border b=BorderFactory.createEtchedBorder();
		GridBagConstraints constr = new GridBagConstraints();
		constr.insets=insets5;
		constr.anchor=GridBagConstraints.CENTER;
		constr.fill=GridBagConstraints.NONE;
		constr.weightx=constr.weighty=1;

		//VT1,VT2 pots
		constr.gridwidth=1;constr.gridheight=1;
		constr.gridx=1;constr.gridy=0;
		add(vTPC1p,constr);
		constr.gridx=3;constr.gridy=0;
		add(vTPC2p,constr);

		int gx=0;
		//LMPD,VTPC1,GAP,VTPC2,FTPC1
		constr.gridwidth=1;constr.gridheight=2;
		constr.gridx=gx++;constr.gridy=1;
		add(lmPD,constr);lmPD.setBorder(b);
		constr.gridx=gx++;constr.gridy=1;
		add(vTPC1,constr);vTPC1.setBorder(b);
		constr.gridx=gx++;constr.gridy=1;
		add(gTPC,constr);
		constr.gridx=gx++;constr.gridy=1;
		add(vTPC2,constr);vTPC2.setBorder(b);
		constr.gridx=gx++;constr.gridy=1;
		add(fTPC1,constr);

		constr.insets = insets5;
		constr.gridwidth=3;constr.gridheight=1;
		constr.gridx=1;constr.gridy=3;
		add(l=createInfoBox(),constr);
		l.setBorder(infoborder);

		//MTPC
		JPanel mtpc;
		//constr.anchor=GridBagConstraints.SOUTH;
		constr.gridwidth=1;constr.gridheight=2;
		constr.gridx=gx;constr.gridy=0;
		//MTPC-L
		mtpc=new JPanel(new BorderLayout(2,2));
		mtpc.add(mTPCLs,BorderLayout.CENTER);
		mtpc.add(mTPCLh,BorderLayout.SOUTH);
		add(mtpc,constr);
		//MTPC-R
		//constr.anchor=GridBagConstraints.NORTH;
		constr.gridx=gx;constr.gridy=2;
		mtpc=new JPanel(new BorderLayout(2,2));
		mtpc.add(mTPCRh,BorderLayout.NORTH);
		mtpc.add(mTPCRs,BorderLayout.CENTER);
		add(mtpc,constr);
		gx++;

		constr.gridwidth=1;constr.gridheight=1;
		constr.gridx=gx;constr.gridy=0;
		add(mTPCLp,constr);
		constr.gridx=gx;constr.gridy=3;
		add(mTPCRp,constr);

		constr.anchor=GridBagConstraints.SOUTH;
		constr.gridx=gx;constr.gridy=1;
		add(fTPC2,constr);
		constr.anchor=GridBagConstraints.NORTH;
		constr.gridx=gx;constr.gridy=2;
		add(fTPC3,constr);
	}
	JPanel createInfoBox(){
		Dimension bs=new Dimension(14,14);
		JPanel p=new JPanel(new GridBagLayout());
		p.setBackground(Color.LIGHT_GRAY);
		GridBagConstraints constr = new GridBagConstraints();
		constr.anchor=GridBagConstraints.CENTER;
		constr.fill=GridBagConstraints.NONE;
		constr.insets=new Insets(5,5,0,0);
		constr.gridwidth=GridBagConstraints.REMAINDER;
		JLabel l;
		p.add(l=new JLabel("Channel status colors"),constr);

		constr.anchor=GridBagConstraints.WEST;
		constr.gridwidth=1;
		p.add(l=new JLabel(),constr);l.setPreferredSize(bs);
		l.setBackground(HVChannelUI.COLOR_OFF);l.setOpaque(true);
		l.setBorder(boxborder);
		p.add(l=new JLabel("Channel is OFF"),constr);l.setFont(infofont);
		p.add(l=new JLabel(),constr);l.setPreferredSize(bs);
		l.setBackground(HVChannelUI.COLOR_ON);l.setOpaque(true);
		l.setBorder(boxborder);
		p.add(l=new JLabel("Channel is ON"),constr);l.setFont(infofont);
		constr.gridwidth=GridBagConstraints.REMAINDER;
		p.add(l=new JLabel(),constr);

		constr.gridwidth=1;
		p.add(l=new JLabel(),constr);l.setPreferredSize(bs);
		l.setBackground(HVChannelUI.COLOR_UNDERVOLT);l.setOpaque(true);
		l.setBorder(boxborder);
		p.add(l=new JLabel("Under voltage"),constr);l.setFont(infofont);
		p.add(l=new JLabel(),constr);l.setPreferredSize(bs);
		l.setBackground(HVChannelUI.COLOR_OVERVOLT);l.setOpaque(true);
		l.setBorder(boxborder);
		p.add(l=new JLabel("Over voltage"),constr);l.setFont(infofont);
		constr.gridwidth=GridBagConstraints.REMAINDER;
		p.add(l=new JLabel(),constr);

		constr.gridwidth=1;
		p.add(l=new JLabel(),constr);l.setPreferredSize(bs);
		l.setBackground(HVChannelUI.COLOR_RAMPINGDN);l.setOpaque(true);
		l.setBorder(boxborder);
		p.add(l=new JLabel("Ramping down"),constr);l.setFont(infofont);
		p.add(l=new JLabel(),constr);l.setPreferredSize(bs);
		l.setBackground(HVChannelUI.COLOR_RAMPINGUP);l.setOpaque(true);
		l.setBorder(boxborder);
		p.add(l=new JLabel("Ramping up"),constr);l.setFont(infofont);
		constr.gridwidth=GridBagConstraints.REMAINDER;
		p.add(l=new JLabel(),constr);

		constr.gridwidth=1;
		p.add(l=new JLabel(),constr);l.setPreferredSize(bs);
		l.setBackground(HVChannelUI.COLOR_TRIPPED);l.setOpaque(true);
		l.setBorder(boxborder);
		p.add(l=new JLabel("Tripped"),constr);l.setFont(infofont);
		p.add(l=new JLabel(),constr);l.setPreferredSize(bs);
		l.setBackground(HVChannelUI.COLOR_OVERCURR);l.setOpaque(true);
		l.setBorder(boxborder);
		p.add(l=new JLabel("Over current"),constr);l.setFont(infofont);
		constr.gridwidth=GridBagConstraints.REMAINDER;
		p.add(l=new JLabel(),constr);

		constr.gridwidth=GridBagConstraints.REMAINDER;
		p.add(l=new JLabel(),constr);
		return p;
	}
}
