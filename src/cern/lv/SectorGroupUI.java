package cern.lv;

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
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import common.Logger;

@SuppressWarnings("serial")
class SectorGroupUI extends JPanel implements ActionListener
{
	static Logger log=Logger.getLogger();
	final private int layb,hgap,vgap;
	private LVControlInterface ctrl;
	SectorGroup grp;
	private JButton set;
	private JPopupMenu popup;

	public SectorGroupUI(int layb,int hgap,int vgap)
	{
		super(new BorderLayout(2,2));
		this.layb=layb;
		this.hgap=hgap;
		this.vgap=vgap;
	}
	public void actionPerformed(ActionEvent ev) {
		if (ctrl==null) return ;
		String cmd=ev.getActionCommand();
		if ("updCtl".equals(cmd)) {
			if (set!=null) set.setEnabled(!ctrl.isLocked());
		}
		else if ("popup".equals(cmd)) {
			if (ctrl.isLocked()) return ;
			JComponent c=this;
			int x=0,y=0;
			if (ev.getSource() instanceof MouseEvent) {
				MouseEvent me=(MouseEvent)ev.getSource();
				x=me.getX(); y=me.getY();
			}
			else if (ev.getSource() instanceof JComponent) {
				c=(JComponent)ev.getSource();
				//x=c.getX(); y=c.getY();
			}
			popup.show(c,x,y);
		}
	}
	public void create(LVControlInterface ctrl,SectorGroup g,int cols)
	{
		this.ctrl=ctrl;
		grp=g;
		setName(g.getName());
		ctrl.addChannelListener(this);
		if (cols==0) cols=g.size();
		JPanel p=new JPanel(new GridLayout(0,cols,hgap,vgap));
		for (int i=0; i<g.size(); ++i){
			SectorGroup.Sector c=g.get(i);
			if (c.chnlist.size()==0) p.add(new JLabel(c.name));
			else
			{
				SectorUI cui=new SectorUI(ctrl,c);
				cui.setGroup(this);
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

		if (l==0){
			add(p,BorderLayout.NORTH);
			add(Box.createRigidArea(p.getPreferredSize()),BorderLayout.SOUTH);
		}
		if (l==1) add(p,BorderLayout.NORTH);
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
	}

	public static class GroupAction extends AbstractAction {
		private final LVControlInterface ctrl;
		private final SectorGroup grp;
		public GroupAction(LVControlInterface ctrl,SectorGroup grp,String cmd,String nm) {
			this.ctrl=ctrl;this.grp=grp;
			super.putValue(ACTION_COMMAND_KEY, cmd);
			super.putValue(NAME, nm);
		}
		public void actionPerformed(ActionEvent e) {
			if (grp.size()==0 || ctrl.isLocked()) return ;
			String cmd=e.getActionCommand();
			if ("on".equals(cmd)) {
				writeGroupStatus(grp,0xffff);
			}
			else if ("off".equals(cmd)) {
				writeGroupStatus(grp,0);
			}
		}
		private void writeGroupStatus(final SectorGroup g,int st){
			log.debug("writeGroupStatus(%s,%x)",g.getName(),st);
			for (int i=0; i<g.size(); ++i){
				SectorGroup.Sector s=g.get(i);
				for (int j=0; j<s.chnlist.size(); ++j){
					ctrl.writeChannelStatus(s.chnlist.get(j),st);
				}
			}
		}
	}
}
