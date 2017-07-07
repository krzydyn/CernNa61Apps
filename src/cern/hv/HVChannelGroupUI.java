package cern.hv;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import caen.HVModule;
import caen.HVModule.ChannelSettings;
import caen.SY527;
import common.Logger;
import common.ui.DocumentValidator;
import common.ui.UiUtils;

@SuppressWarnings("serial")
class HVChannelGroupUI extends JPanel implements ActionListener{
	static Logger log=Logger.getLogger();
	final private int layb,hgap,vgap;
	HVControl ctrl;
	private JButton set;
	private JPopupMenu popup;

	public HVChannelGroupUI(int layb,int hgap,int vgap){
		super(new BorderLayout(2,2));
		this.layb=layb;
		this.hgap=hgap;
		this.vgap=vgap;
	}
	public boolean isLocked(){return !set.isEnabled();}
	public void actionPerformed(ActionEvent ev) {
		if (ctrl==null) return ;
		String cmd=ev.getActionCommand();
		if ("updCtl".equals(cmd)){
			if (set!=null) set.setEnabled(!ctrl.isLocked());
		}
		else if ("popup".equals(cmd)){
			if (ctrl.isLocked()) return ;
			JComponent c=this;
			int x=0,y=0;
			if (ev.getSource() instanceof MouseEvent){
				MouseEvent me=(MouseEvent)ev.getSource();
				x=me.getX(); y=me.getY();
			}
			else if (ev.getSource() instanceof JComponent){
				c=(JComponent)ev.getSource();
				//x=c.getX(); y=c.getY();
			}
			popup.show(c,x,y);
		}
	}
	public void create(HVControl ctrl,HVChannelGroup g,int cols){
		this.ctrl=ctrl;
		setName(g.getName());
		ctrl.addChannelListener(this);
		if (cols==0) cols=g.size();
		JPanel p=new JPanel(new GridLayout(0,cols,hgap,vgap));
		for (int i=0; i<g.size(); ++i){
			HVChannel c=g.get(i);
			if (c.slot<0||c.chn<0) p.add(new JLabel(c.name));
			else{
				HVChannelUI cui=new HVChannelUI(ctrl,c.name,c.slot,c.chn);
				cui.setChnGroup(this);
				p.add(cui);
			}
		}
		add(p,BorderLayout.CENTER);

		if (layb<0) return ;//no buttons
		int l=layb&0xf;
		p=new JPanel(null);
		if (l<=2) p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));
		else p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
		set=new JButton(g.getName());
		set.setActionCommand("popup");
		set.addActionListener(this);
		p.add(set);
		set.setBackground(Color.LIGHT_GRAY);
		set.setEnabled(false);

		if (l==0) {
			add(p,BorderLayout.NORTH);
			add(Box.createRigidArea(p.getPreferredSize()),BorderLayout.SOUTH);
		}
		else if (l==1) add(p,BorderLayout.NORTH);
		else if (l==2) add(p,BorderLayout.SOUTH);
		else if (l==3) add(p,BorderLayout.WEST);
		else if (l==4) add(p,BorderLayout.EAST);
		l=(layb>>4)&0xf;
		if (l==0) ;
		else if (l==1) p.setAlignmentX(0.5f);
		else if (l==2) p.add(Box.createHorizontalGlue(), 0);
		l=(layb>>8)&0xf;
		if (l==0) ;
		else if (l==1) p.setAlignmentY(0.5f);
		else if (l==2) p.add(Box.createVerticalGlue(), 0);

		popup=new JPopupMenu();
		popup.add(new JLabel("Set "+g.getName(),JLabel.CENTER));
		popup.addSeparator();
		popup.add(new JMenuItem(new GroupAction(ctrl,g,"on","On")));
		popup.add(new JMenuItem(new GroupAction(ctrl,g,"off","Off")));
		popup.add(new JMenuItem(new GroupAction(ctrl,g,"ramp","Ramp by...")));
		popup.add(new JMenuItem(new GroupAction(ctrl,g,"par","Params...")));
	}

	public static class GroupAction extends AbstractAction {
		private final HVControl ctrl;
		private final HVChannelGroup grp;
		public GroupAction(HVControl ctrl,HVChannelGroup grp,String cmd,String nm){
			this.ctrl=ctrl;this.grp=grp;
			super.putValue(ACTION_COMMAND_KEY, cmd);
			super.putValue(NAME, nm);
		}
		public void actionPerformed(ActionEvent e) {
			if (grp.size()==0 || ctrl.isLocked()) return ;
			String cmd=e.getActionCommand();
			if ("on".equals(cmd)){
				log.debug("group [%s] set ON",grp.getName());
				ctrl.setGrpParam(grp, SY527.CMD_SetChnMF,(1<<3)|(1<<6)|(1<<11)|(1<<14));
			}
			else if ("off".equals(cmd)){
				log.debug("group [%s] set OFF",grp.getName());
				ctrl.setGrpParam(grp, SY527.CMD_SetChnMF,(1<<11)|(1<<14));
			}
			else if ("ramp".equals(cmd)){
				HVChannel c=grp.get(0);
				HVModule m=ctrl.findModule(c.slot);
				float scv=(float)Math.pow(10,m.vdec);
				int ramp=0;
				for(String in="";;){
					in=(String)JOptionPane.showInputDialog(ctrl,
							"Ramp group by: ","Enter value to ramp by (V0set)",JOptionPane.QUESTION_MESSAGE,null,null,in);
					if (in==null) return ;
					if (in.matches(DocumentValidator.FLOATING_SIGNED)){
						ramp=(int)(Float.parseFloat(in)*scv); break;
					}
				}
				log.debug("group [%s] ramp by %d",grp.getName(),ramp);
				ctrl.rampGrpBy(grp,ramp);
			}
			else if ("par".equals(cmd)){
				if (grp.size()==0) return ;
				HVChannel c=grp.get(0);
				HVChannelParamsUI par=new HVChannelParamsUI();
				HVModule m=ctrl.findModule(c.slot);
				if (m!=null){
					par.setProperties(m.getChannelSettings(c.chn),m.vdec,m.idec);
				}

				String n=String.format("Set group %s ",grp.getName());
				UiUtils.showDialog(ctrl,par,n);
				if (par.getResult()==1 && m!=null){
					if (!ctrl.showConfim("Are you sure to change group settings?")) return ;
					log.debug("group [%s] set PARAMS",grp.getName());
					ChannelSettings cs=new ChannelSettings();
					m.getChannelSettings(c.chn).copySettings(cs);
					par.getProperties(cs,m.vdec,m.idec);
					ctrl.setGrpSettings(grp, cs);
				}
			}
		}
	}
}
