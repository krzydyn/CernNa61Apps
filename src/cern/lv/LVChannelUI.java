package cern.lv;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import common.ui.DialogPanel;

@SuppressWarnings("serial")
public class LVChannelUI extends DialogPanel {
	final LVChannel chn;
	final JLabel[] status=new JLabel[16];
	final JLabel[] alarm=new JLabel[16];
	final JCheckBox[] stbit=new JCheckBox[16];
	final boolean locked;
	final static String[] tip={"At least 1 channel OFF","All channels OFF",
		"-2V is ON","MotherBoard not attached"};
	public LVChannelUI(LVControlInterface ctrl,LVChannel chn){
		super(new BorderLayout(5,0));
		this.chn=chn;
		this.locked=ctrl.isLocked();
		ctrl.addChannelListener(this);

		JPanel p;
		JLabel l;
		l=new JLabel(String.format("PCI(%d)  CR(%d)  A(%04X)  LVC(%d)",chn.pci,chn.crate,chn.addr,chn.lvc),JLabel.LEFT);
		add(l,BorderLayout.NORTH);

		p=new JPanel(new GridLayout(0,1,2,2));
		p.add(new JLabel());
		p.add(new JLabel("Status:"));
		p.add(new JLabel("Alarms:"));
		if (!locked) p.add(new JLabel("Set status:"));
		add(p,BorderLayout.WEST);

		p=new JPanel(new GridLayout(0,16,5,2));
		for (int i=0; i<16; ++i){
			if (15-i==15) p.add(l=new JLabel("1OF",JLabel.CENTER));
			else if (15-i==14) p.add(l=new JLabel("OFF",JLabel.CENTER));
			else if (15-i==13) p.add(l=new JLabel("-2V",JLabel.CENTER));
			else if (15-i==12) p.add(l=new JLabel("CT",JLabel.CENTER));
			else p.add(l=new JLabel(String.valueOf(15-i),JLabel.CENTER));
			if (i<tip.length) l.setToolTipText(tip[i]);
		}
		for (int i=0; i<16; ++i){
			p.add(l=new JLabel("",JLabel.CENTER));
			if (i>3)l.setOpaque(true);
			status[i]=l;
		}
		for (int i=0; i<16; ++i){
			p.add(l=new JLabel("",JLabel.CENTER));
			if (i>3)l.setOpaque(true);
			alarm[i]=l;
		}
		if (!locked){
			int s=chn.setStatus;
			if (s<=0) s=chn.status;
			for (int i=0; i<16; ++i){
				boolean on=(s&(1<<(15-i)))!=0;
				p.add(stbit[i]=new JCheckBox("",on));
			}
		}
		add(p,BorderLayout.CENTER);
		addButtons();
		updateChnUI();
	}
	public void destroy(){
		for (int i=0; i<16; ++i){
			alarm[i]=status[i]=null;
			stbit[i]=null;
		}
	}
	public int getStatus(){
		if (locked) return -1;
		int s=0;
		for (int i=0; i<16; ++i){
			if (stbit[i].isSelected()) s|=1<<(15-i);
		}
		return s;
	}
	private void updateChnUI(){
		JLabel l;
		boolean on;
		int s=chn.status;
		int a=chn.alarm;
		//log.debug("chn.s=%x, chn.a=%x",s,a);
		if (s<0) {s=0; a=0;}
		for (int i=0; i<16; ++i){
			l=status[i];
			on=(s&(1<<(15-i)))!=0;
			l.setText(on?"1":"0");
			if (i>3) l.setBackground(on?SectorUI.COLOR_ON:SectorUI.COLOR_OFF);
			l=alarm[i];
			on=(a&(1<<(15-i)))!=0;
			l.setText(on?"1":"0");
			if (i>3) l.setBackground(on?SectorUI.COLOR_NOALARM:SectorUI.COLOR_ALARM);
		}
	}
	public void actionPerformed(ActionEvent ev) {
		String cmd=ev.getActionCommand();
		if (cmd.equals("updChn")) updateChnUI();
		else super.actionPerformed(ev);
	}
}
