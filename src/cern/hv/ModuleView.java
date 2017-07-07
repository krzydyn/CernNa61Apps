package cern.hv;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.BevelBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.table.DefaultTableCellRenderer;

import caen.HVModule;
import caen.HVModule.ChannelSettings;
import caen.SY527;
import common.Logger;
import common.ui.EnchancedEditor;
import common.ui.UiUtils;

//1496398
@SuppressWarnings("serial")
public class ModuleView extends JPanel implements ActionListener{
	static Logger log=Logger.getLogger();
	final static public Font BOLD=new Font("times",Font.BOLD,12);
	final static Font valuefont=new Font("Courier",Font.PLAIN,12);

	final private HVControl ctrl;
	final HVModule mod;
	final JTable chnTable;

	private final ActionEvent popupEvent=new ActionEvent(this,0,"popup");
	private final ActionEvent parEvent=new ActionEvent(this,0,"par");
	private final JPopupMenu popup;
	private final JCheckBox cbxFast=new JCheckBox("Fast read",false);

	public ModuleView(HVControl ctrl,HVModule m) {
		super(new BorderLayout());
		this.ctrl=ctrl;
		mod=m;
		ctrl.addChannelListener(this);

		GridBagConstraints constr = new GridBagConstraints();
		//constr.fill = GridBagConstraints.NONE;
		//constr.fill = GridBagConstraints.BOTH;
		constr.weightx=0;constr.weighty=0;
		constr.insets=new Insets(0,5,0,5);

		JPanel p;{
		String[] cols=HVModule.fields();
		p=new JPanel(new GridBagLayout());
		constr.gridwidth=1;
		for (int i=0; i<cols.length; ++i)
			p.add(new JLabel(cols[i],JLabel.CENTER),constr);
		constr.weightx=1;
		constr.gridwidth=GridBagConstraints.REMAINDER;
		p.add(new JLabel(),constr);
		constr.weightx=0;
		cols=mod.values();
		constr.gridwidth=1;
		for (int i=0; i<cols.length; ++i){
			JLabel l;
			p.add(l=new JLabel(cols[i],JLabel.CENTER),constr);
			l.setFont(valuefont);
		}
		constr.weightx=1;
		constr.anchor=GridBagConstraints.EAST;
		constr.gridwidth=GridBagConstraints.REMAINDER;
		p.add(cbxFast,constr);
		cbxFast.setToolTipText(String.format("Enable fast read for %d seconds",HVControl.FASTREAD));
		cbxFast.setActionCommand("fast");
		cbxFast.addActionListener(this);
		p.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		add(p,BorderLayout.NORTH);
		}

		Vector<String> cols=new Vector<String>();
		//cols=ChannelSettings.fields();
		cols.add("#");
		ChannelSettings.fields(cols);

		Vector<Object> rows=new Vector<Object>();
		//String[][] rows=new String[mod.chns][colsv.size()];
		for (int chn=0; chn<mod.chns; ++chn){
			ChannelSettings s=mod.getChannelSettings(chn);
			Vector<String> v=new Vector<String>();
			v.add(String.format("%d",chn));
			s.values(mod,v);
			rows.add(v);
		}
		//JTable tab=new JTable(rows,cols);
		chnTable=new ModuleViewTable(rows,cols);
		chnTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked(final MouseEvent ev){
				if (ModuleView.this.ctrl.isLocked()) return ;
				if ((ev.getButton()==MouseEvent.BUTTON1 && ev.getClickCount()==2)){
					actionPerformed(parEvent);
				}
				else if ((ev.getButton()==MouseEvent.BUTTON3 && ev.getClickCount()==1)){
					popupEvent.setSource(ev);
					actionPerformed(popupEvent);
				}
			}
		});
		JScrollPane scroll=new JScrollPane(chnTable);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setWheelScrollingEnabled(true);
        scroll.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		add(scroll,BorderLayout.CENTER);

		chnTable.setRowMargin(2);
		chnTable.setColumnSelectionAllowed(false);
		chnTable.setSelectionMode(0);
		//chnTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        chnTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        chnTable.getTableHeader().setReorderingAllowed(false);
		chnTable.getTableHeader().setResizingAllowed(true);
		chnTable.getTableHeader().setFont(BOLD);
		chnTable.getColumnModel().setColumnMargin(2);
		EnchancedEditor.fitColumnWidth(chnTable,0);
        //chnTable.setEnabled(false);

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
		popup.add(mi=new JMenuItem("Params"));
		mi.setActionCommand("par");
		mi.addActionListener(this);

		//cbxFast.addChangeListener(this);
		this.addAncestorListener(new AncestorListener(){
			public void ancestorAdded(AncestorEvent event) {}
			public void ancestorMoved(AncestorEvent event) {}
			public void ancestorRemoved(AncestorEvent event) {
				setFast(false);
			}
		});
	}

	public void setFast(boolean b){
		if (cbxFast.isSelected()!=b){
			cbxFast.setSelected(b);
			ctrl.fastRead(b?mod:null);
		}
	}
	public boolean isFast(){return cbxFast.isSelected();}

	public void actionPerformed(ActionEvent ev) {
		String cmd=ev.getActionCommand();
		if (cmd.equals("fast")){
			if (cbxFast.isSelected()) ctrl.fastRead(mod);
			else ctrl.fastRead(null);
		}
		else if (cmd.equals("updChn")){
			//log.debug("updating module %s(%d)",mod.getName(),mod.getSlot());
			for (int chn=0; chn<mod.chns; ++chn){
				ChannelSettings s=mod.getChannelSettings(chn);
				String[] v=s.values(mod);
				for (int i=0; i<v.length; ++i)
					chnTable.setValueAt(v[i], chn, i+1);
			}
		}
		else if ("popup".equals(cmd)){
			int x=0,y=0,chn;
			if (ev.getSource() instanceof MouseEvent){
				MouseEvent me=(MouseEvent)ev.getSource();
				x=me.getX(); y=me.getY();
				chn=chnTable.rowAtPoint(me.getPoint());
				chnTable.setRowSelectionInterval(chn,chn);
			}
			chn=chnTable.getSelectedRow();
			if (chn>=0){
				JLabel t=(JLabel)popup.getComponent(0);
				t.setText(String.format("Set channel %d.%d",mod.getSlot(),chn));
				popup.show(chnTable,x,y);
			}
		}
		else if ("on".equals(cmd)){
			int chn=chnTable.getSelectedRow();
			log.debug("chn [%d.%d] set ON",mod.getSlot(),chn);
			ctrl.setChnParam(mod.getSlot(), chn, SY527.CMD_SetChnMF,(1<<3)|(1<<6)|(1<<11)|(1<<14));
		}
		else if ("off".equals(cmd)){
			int chn=chnTable.getSelectedRow();
			log.debug("chn [%d.%d] set OFF",mod.getSlot(),chn);
			ctrl.setChnParam(mod.getSlot(), chn, SY527.CMD_SetChnMF,(1<<11)|(1<<14));
		}
		else if ("par".equals(cmd)){
			int chn=chnTable.getSelectedRow();
			HVChannelParamsUI par=new HVChannelParamsUI(true);
			ChannelSettings csold=mod.getChannelSettings(chn);
			String n=String.format("Set channel %d.%d",mod.getSlot(),chn);
			par.setProperties(csold,mod.vdec,mod.idec);
			UiUtils.showDialog(this,par,n);
			if (par.getResult()==1){
				ChannelSettings cs=new ChannelSettings();
				csold.copySettings(cs);
				par.getProperties(cs,mod.vdec,mod.idec);
				ctrl.setChnSettings(mod.getSlot(), chn, csold, cs);
			}
		}
	}

	static class ModuleViewTable extends JTable{
		public ModuleViewTable(Vector<Object> rows,Vector<String> cols) {
			super(rows,cols);
			DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
			cellRenderer.setHorizontalAlignment(JLabel.RIGHT);
			for (int i = 0; i < getColumnCount(); i++){
				setDefaultRenderer(getColumnClass(i), cellRenderer);
			}
			Dimension d=getTableHeader().getPreferredSize();
			d.height+=4;
			getTableHeader().setPreferredSize(d);
		}
		public boolean isCellEditable(int row,int col) { return false; }
	}
}
