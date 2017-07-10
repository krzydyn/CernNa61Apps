package cern.lv;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
public class LVTPCView extends JPanel {
	static Logger log=Logger.getLogger();
	final static Font infofont=new Font("Courier",Font.PLAIN,12);
	final static Border infoborder=BorderFactory.createEtchedBorder(Color.ORANGE,Color.YELLOW);
	final static Border boxborder=BorderFactory.createLineBorder(Color.LIGHT_GRAY.brighter());

	private final SectorGroupUI cwb=new SectorGroupUI(-1,1,1);
	private final SectorGroupUI tpcCB=new SectorGroupUI(-1,1,1);
	private final SectorGroupUI lmPD=new SectorGroupUI(-1,1,1);
	private final SectorGroupUI gTPC=new SectorGroupUI(-1,1,1);
	private final SectorGroupUI vTPC1=new SectorGroupUI(0,1,5);
	private final SectorGroupUI vTPC2=new SectorGroupUI(0,1,5);
	private final SectorGroupUI mTPCLs=new SectorGroupUI(4,1,1);
	private final SectorGroupUI mTPCLh=new SectorGroupUI(4,1,1);
	private final SectorGroupUI mTPCRh=new SectorGroupUI(4|(2<<8),1,1);
	private final SectorGroupUI mTPCRs=new SectorGroupUI(4|(2<<8),1,1);
	private final SectorGroupUI fTPC1=new SectorGroupUI(-1,1,1);
	private final SectorGroupUI fTPC2=new SectorGroupUI(-1,1,1);
	private final SectorGroupUI fTPC3=new SectorGroupUI(-1,1,1);

	public LVTPCView(LVControlInterface ctrl) {
		super(null);
		cwb.create(ctrl,ctrl.getBindings("cwb"),1);
		tpcCB.create(ctrl,ctrl.getBindings("tpccb"),1);
		lmPD.create(ctrl,ctrl.getBindings("lmpd"),1);
		gTPC.create(ctrl,ctrl.getBindings("gtpc"),1);
		vTPC1.create(ctrl,ctrl.getBindings("vtpc1"),3);
		vTPC2.create(ctrl,ctrl.getBindings("vtpc2"),3);
		mTPCLs.create(ctrl,ctrl.getBindings("mtpcLs"),5);
		mTPCLh.create(ctrl,ctrl.getBindings("mtpcLh"),5);
		mTPCRh.create(ctrl,ctrl.getBindings("mtpcRh"),5);
		mTPCRs.create(ctrl,ctrl.getBindings("mtpcRs"),5);
		fTPC1.create(ctrl,ctrl.getBindings("ftpc1"),1);
		fTPC2.create(ctrl,ctrl.getBindings("ftpc2"),1);
		fTPC3.create(ctrl,ctrl.getBindings("ftpc3"),1);
		buildLa2();
	}
	void buildLa2() {
		JComponent l;
		Insets insets5=new Insets(5,5,5,5);
		setLayout(new GridBagLayout());
		Border b=BorderFactory.createEtchedBorder();
		GridBagConstraints constr = new GridBagConstraints();
		constr.insets=insets5;
		constr.fill=GridBagConstraints.NONE;
		constr.anchor=GridBagConstraints.CENTER;
		constr.weightx=constr.weighty=1;

		constr.insets = new Insets(0, 0, 0, 0);
		constr.gridwidth=1;constr.gridheight=1;
		for (int i=0; i < 7; ++i) {
			constr.gridx=i;constr.gridy=4;
			add(new JLabel(),constr);
		}

		constr.insets=insets5;
		//Status Info
		constr.gridwidth=3;constr.gridheight=1;
		constr.gridx=1;constr.gridy=0;
		add(l=createStatusInfoBox(),constr);
		l.setBorder(infoborder);

		int gx=0;
		//LMPD,VTPC1,GAP,VTPC2,FTPC1
		constr.gridwidth=1;constr.gridheight=2;
		constr.gridx=gx++;constr.gridy=1;
		add(lmPD,constr);
		constr.gridx=gx++;constr.gridy=1;
		add(vTPC1,constr);vTPC1.setBorder(b);
		constr.gridx=gx++;constr.gridy=1;
		add(gTPC,constr);
		constr.gridx=gx++;constr.gridy=1;
		add(vTPC2,constr);vTPC2.setBorder(b);
		constr.gridx=gx++;constr.gridy=1;
		add(fTPC1,constr);

		JPanel mtpc;
		//CW,CB board and AlarmInfo
		mtpc=new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 2));
		mtpc.add(cwb);
		mtpc.add(l=createAlarmInfoBox());
		l.setBorder(infoborder);
		mtpc.add(tpcCB);
		constr.gridwidth=5;constr.gridheight=1;
		constr.gridx=0;constr.gridy=3;
		add(mtpc,constr);

		constr.gridwidth=1;constr.gridheight=2;
		constr.gridx=gx;constr.gridy=0;
		//MTPC-L
		mtpc=new JPanel(new BorderLayout(2,2));
		mtpc.add(mTPCLs,BorderLayout.CENTER);
		mtpc.add(mTPCLh,BorderLayout.SOUTH);
		add(mtpc,constr);
		//MTPC-R
		constr.gridx=gx;constr.gridy=2;
		mtpc=new JPanel(new BorderLayout(2,2));
		mtpc.add(mTPCRh,BorderLayout.NORTH);
		mtpc.add(mTPCRs,BorderLayout.CENTER);
		add(mtpc,constr);
		++gx;

		//FTPC2,FTPC3
		constr.gridwidth=1;constr.gridheight=1;
		constr.anchor=GridBagConstraints.SOUTH;
		constr.gridx=gx;constr.gridy=1;
		add(fTPC2,constr);
		constr.anchor=GridBagConstraints.NORTH;
		constr.gridx=gx;constr.gridy=2;
		add(fTPC3,constr);
	}
	JPanel createStatusInfoBox(){
		Dimension bs=new Dimension(14,14);
		JPanel p=new JPanel(new GridBagLayout());
		p.setBackground(Color.LIGHT_GRAY);
		GridBagConstraints constr = new GridBagConstraints();
		constr.anchor=GridBagConstraints.CENTER;
		constr.fill=GridBagConstraints.NONE;
		constr.insets=new Insets(5,5,0,0);
		constr.gridwidth=GridBagConstraints.REMAINDER;
		JLabel l;
		p.add(l=new JLabel("Board status colors"),constr);

		constr.anchor=GridBagConstraints.WEST;
		constr.gridwidth=1;
		p.add(l=new JLabel(),constr);l.setPreferredSize(bs);
		l.setBackground(SectorUI.COLOR_OFF);l.setOpaque(true);
		l.setBorder(boxborder);
		p.add(l=new JLabel("Board is OFF"),constr);l.setFont(infofont);
		p.add(l=new JLabel(),constr);l.setPreferredSize(bs);
		l.setBackground(SectorUI.COLOR_ON);l.setOpaque(true);
		l.setBorder(boxborder);
		p.add(l=new JLabel("Board is ON"),constr);l.setFont(infofont);
		constr.gridwidth=GridBagConstraints.REMAINDER;
		p.add(l=new JLabel(),constr);

		constr.gridwidth=1;
		p.add(l=new JLabel(),constr);l.setPreferredSize(bs);
		l.setBackground(SectorUI.COLOR_OFF1);l.setOpaque(true);
		l.setBorder(boxborder);
		p.add(l=new JLabel("At least one is OFF"),constr);l.setFont(infofont);
		p.add(l=new JLabel(),constr);l.setPreferredSize(bs);
		l.setBackground(SectorUI.COLOR_CTON);l.setOpaque(true);
		l.setBorder(boxborder);
		p.add(l=new JLabel("CT ON but all channels OFF"),constr);l.setFont(infofont);
		constr.gridwidth=GridBagConstraints.REMAINDER;
		p.add(l=new JLabel(),constr);

		constr.gridwidth=1;
		p.add(l=new JLabel(),constr);l.setPreferredSize(bs);
		l.setBackground(SectorUI.COLOR_NOTAVAIL);l.setOpaque(true);
		l.setBorder(boxborder);
		p.add(l=new JLabel("Not exists or broken"),constr);l.setFont(infofont);
		p.add(l=new JLabel(),constr);l.setPreferredSize(bs);
		l.setBackground(SectorUI.COLOR_DUMMY);l.setOpaque(true);
		l.setBorder(boxborder);
		p.add(l=new JLabel("Not used"),constr);l.setFont(infofont);
		constr.gridwidth=GridBagConstraints.REMAINDER;
		p.add(l=new JLabel(),constr);

		constr.gridwidth=GridBagConstraints.REMAINDER;
		p.add(l=new JLabel(),constr);
		return p;
	}
	JPanel createAlarmInfoBox(){
		Dimension bs=new Dimension(14,14);
		JPanel p=new JPanel(new GridBagLayout());
		p.setBackground(Color.LIGHT_GRAY);
		GridBagConstraints constr = new GridBagConstraints();
		constr.anchor=GridBagConstraints.CENTER;
		constr.fill=GridBagConstraints.NONE;
		constr.insets=new Insets(5,5,0,0);
		constr.gridwidth=GridBagConstraints.REMAINDER;
		JLabel l;
		p.add(l=new JLabel("Board alarm colors"),constr);

		constr.anchor=GridBagConstraints.WEST;
		constr.gridwidth=1;
		p.add(l=new JLabel(),constr);l.setPreferredSize(bs);
		l.setBackground(SectorUI.COLOR_NOALARM);l.setOpaque(true);
		l.setBorder(boxborder);
		p.add(l=new JLabel("No alarm"),constr);l.setFont(infofont);
		p.add(l=new JLabel(),constr);l.setPreferredSize(bs);
		l.setBackground(SectorUI.COLOR_ALARM);l.setOpaque(true);
		l.setBorder(boxborder);
		p.add(l=new JLabel("At least one alarm"),constr);l.setFont(infofont);
		constr.gridwidth=GridBagConstraints.REMAINDER;
		p.add(l=new JLabel(),constr);

		constr.gridwidth=1;
		p.add(l=new JLabel(),constr);l.setPreferredSize(bs);
		l.setBackground(SectorUI.COLOR_NOTAVAIL);l.setOpaque(true);
		l.setBorder(boxborder);
		p.add(l=new JLabel("Not exists or broken"),constr);l.setFont(infofont);
		p.add(l=new JLabel(),constr);l.setPreferredSize(bs);
		l.setBackground(SectorUI.COLOR_DUMMY);l.setOpaque(true);
		l.setBorder(boxborder);
		p.add(l=new JLabel("Not used"),constr);l.setFont(infofont);
		constr.gridwidth=GridBagConstraints.REMAINDER;
		p.add(l=new JLabel(),constr);

		constr.gridwidth=GridBagConstraints.REMAINDER;
		p.add(l=new JLabel(),constr);
		return p;
	}
}
