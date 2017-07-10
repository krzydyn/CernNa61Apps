package cern.lv;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import sys.Logger;
import sys.Resource;
import sys.ui.UiUtils;

@SuppressWarnings("serial")
class SectorUI extends JPanel implements ActionListener {
	static Logger log=Logger.getLogger();
	final static Font valuefont=new Font("Courier",Font.PLAIN,12);
	final static Font onofffont=new Font("Courier",Font.PLAIN,8);
	final static Dimension valuesize=new Dimension(50,27);
	final static Dimension minBoxSize=new Dimension(6,13);
	final static Border border=BorderFactory.createEmptyBorder(0,1,2,2);
	final static Border border5=BorderFactory.createEmptyBorder(0,2,2,1);

	final static Color COLOR_NOTAVAIL=Color.BLACK;
	final static Color COLOR_OFF=Color.DARK_GRAY;
	final static Color COLOR_OFF1=Color.YELLOW;
	final static Color COLOR_ON=Color.GREEN.darker();
	final static Color COLOR_CTON=Color.RED;
	final static Color COLOR_ALARM=Color.RED;
	final static Color COLOR_NOALARM=Color.LIGHT_GRAY;
	final static Color COLOR_DUMMY=Color.GRAY;

	final static ImageIcon iconON;
	final static ImageIcon iconOFF;
	static {
		ImageIcon i1,i2;
		try{
		i1=new ImageIcon(Resource.getResourceURL("res/icon/arrow-right.png"));
		i2=new ImageIcon(Resource.getResourceURL("res/icon/rect11.png"));
		}catch (Exception e) { i1=i2=null; }
		iconON=i1;iconOFF=i2;
	}

	final MouseAdapter mouseActions;

	private final LVControlInterface ctrl;
	private final JLabel nm;
	private final JButton on,off;
	private final JLabel[] status,alarm;
	SectorGroup.Sector sector;
	private SectorGroupUI grp;

	private final ActionEvent infoEvent=new ActionEvent(this,0,"info");
	public SectorUI(LVControlInterface ctrl,SectorGroup.Sector sector) {
		super(new BorderLayout());
		this.ctrl=ctrl;
		this.sector=sector;

		mouseActions=new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent ev){
				if (ev.getButton()==MouseEvent.BUTTON1 && ev.getClickCount()==2) {
					infoEvent.setSource(ev.getComponent());
					actionPerformed(infoEvent);
				}
			}
		};
		setName(sector.name);
		setBorder(BorderFactory.createEtchedBorder());

		status=new JLabel[sector.chnlist.size()];
		alarm=new JLabel[sector.chnlist.size()];
		JPanel p;
		JLabel l;
		p=new JPanel(null);
		p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));
		p.add(nm=new JLabel(sector.name));
		p.add(Box.createHorizontalGlue());
		JButton b;
		ImageIcon icon;
		icon=iconON;
		if (icon!=null){
			b=new JButton(icon);
			b.setPreferredSize(new Dimension(icon.getIconWidth()+2,icon.getIconHeight()+2));
			b.setMaximumSize(b.getPreferredSize());
		}
		else {
			b=new JButton("On");
			b.setFont(onofffont);
			UiUtils.makeSimpleButton(b);
		}
		b.setToolTipText("Sector ON");
		b.setMargin(null);
		b.addActionListener(this);
		b.setActionCommand("on");
		p.add(on=b);
		icon=iconOFF;
		if (icon!=null){
			b=new JButton(icon);
			b.setPreferredSize(new Dimension(icon.getIconWidth()+2,icon.getIconHeight()+2));
			b.setMaximumSize(b.getPreferredSize());
		}
		else {
			b=new JButton("Off");
			b.setFont(onofffont);
			UiUtils.makeSimpleButton(b);
		}
		b.setToolTipText("Sector OFF");
		b.setMargin(null);
		b.addActionListener(this);
		b.setActionCommand("off");
		p.add(off=b);
		p.add(Box.createHorizontalStrut(2));
		add(p,BorderLayout.NORTH);

		p=new JPanel(new GridLayout(2,0,1,1));
		p.setPreferredSize(valuesize);
		if (sector.chnlist.size()==5) p.setBorder(border5);
		else p.setBorder(border);
		if (sector.chnlist.size()==1)p.add(new JLabel());
		for (int i=0; i<sector.chnlist.size(); ++i) {
			l=new JLabel();l.setOpaque(true);
			l.setPreferredSize(minBoxSize);
			p.add(l); status[i]=l;
			if (sector.chnlist.get(i).addr<0) l.setBackground(COLOR_DUMMY);
			else l.setBackground(Color.LIGHT_GRAY);
			LVChannel c=sector.chnlist.get(i);
			if (c.addr>=0) l.addMouseListener(mouseActions);
		}
		if (sector.chnlist.size()==1){p.add(new JLabel());p.add(new JLabel());}
		for (int i=0; i<sector.chnlist.size(); ++i) {
			l=new JLabel();l.setOpaque(true);
			l.setPreferredSize(minBoxSize);
			p.add(l); alarm[i]=l;
			if (sector.chnlist.get(i).addr<0) l.setBackground(COLOR_DUMMY);
			else l.setBackground(Color.LIGHT_GRAY);
			LVChannel c=sector.chnlist.get(i);
			if (c.addr>=0) l.addMouseListener(mouseActions);
		}
		if (sector.chnlist.size()==1)p.add(new JLabel());
		add(p,BorderLayout.CENTER);
		ctrl.addChannelListener(this);

		on.setEnabled(!ctrl.isLocked());
		off.setEnabled(!ctrl.isLocked());
	}
	public void setGroup(SectorGroupUI g){grp=g;}
	public ArrayList<LVChannel> getChnList(){return sector.chnlist;}

	@Override
	public void actionPerformed(ActionEvent ev) {
		String cmd=ev.getActionCommand();
		if ("updCtl".equals(cmd)) {
			on.setEnabled(!ctrl.isLocked());
			off.setEnabled(!ctrl.isLocked());
		}
		else if (cmd.equals("updChn")) {
			for (int i=0; i<sector.chnlist.size(); ++i) {
				LVChannel c=sector.chnlist.get(i);
				if (c.addr<0) continue;

				if (c.status<0){
					status[i].setBackground(Color.LIGHT_GRAY);
					alarm[i].setBackground(Color.LIGHT_GRAY);
				}
				else if ((c.alarm&(1<<15))!=0){
					status[i].setBackground(COLOR_NOTAVAIL);
					alarm[i].setBackground(COLOR_NOTAVAIL);
				}
				else {
					if ((c.status&(1<<14))==0) //board is off
						status[i].setBackground(COLOR_OFF);
					else if ((c.status&(1<<12))==0) //CTon
						status[i].setBackground(COLOR_CTON);
					else if ((c.status&(1<<15))==0) //at least one off
						status[i].setBackground(COLOR_OFF1);
					else if ((c.status&0xfff)==0)
						status[i].setBackground(COLOR_OFF);
					else if ((c.status&0xfff)!=0xfff)
						status[i].setBackground(COLOR_OFF1);
					else
						status[i].setBackground(COLOR_ON);

					if ((c.alarm&(1<<14))==0)
						alarm[i].setBackground(COLOR_OFF);
					else if ((c.alarm&(1<<12))==0)  //at least one alarm
						alarm[i].setBackground(COLOR_ALARM);
					else if ((c.alarm&0xfff)!=0xfff)//alarm detection
						alarm[i].setBackground(COLOR_ALARM);
					else alarm[i].setBackground(COLOR_NOALARM);
				}
			}
		}
		else if ("info".equals(cmd)){
			JComponent src=(JComponent)ev.getSource();
			for (int i=0; i<status.length; ++i)
				if (src==status[i] || src==alarm[i]){
					LVChannel c=sector.chnlist.get(i);
					String n=String.format("Board %s.%s.%d",grp.getName(),nm.getText(),i+1);
					LVChannelUI cu=new LVChannelUI(ctrl,c);
					UiUtils.showDialog(src,cu,n);
					if (cu.getResult()==1 && !ctrl.isLocked()){
						ctrl.writeChannelStatus(c,cu.getStatus());
					}
					ctrl.removeChannelListener(cu);
					break;
				}
		}
		else if (ctrl.isLocked()) ;
		else if ("on".equals(cmd))
		{
			log.debug("sector [%s.%s] set ON",grp.getName(),nm.getText());
			writeSectorStatus(sector, 0xffff);
		}
		else if ("off".equals(cmd))
		{
			log.debug("sector [%s.%s] set OFF",grp.getName(),nm.getText());
			writeSectorStatus(sector, 0);
		}
	}
	private void writeSectorStatus(SectorGroup.Sector s, int st) {
		log.debug("writeSectorStatus(%s,%x)",s.name,st);
		for (int j=0; j<s.chnlist.size(); ++j){
			ctrl.writeChannelStatus(s.chnlist.get(j),st);
		}
	}

}
