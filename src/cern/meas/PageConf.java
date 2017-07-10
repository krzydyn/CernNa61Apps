package cern.meas;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import plot.PlotXY;
import plot.PlotXY.ChannelParams;
import sys.ui.DialogPanel;
import sys.ui.UiUtils;
import cern.meas.MeasView.ViewChannelsInfo;
import channel.ChannelDef;
import channel.ui.ChannelSelector;


@SuppressWarnings("serial")
public class PageConf extends DialogPanel {
	private final static int MAX_CHANNELS=4;
	private final static Border focusedBorder=BorderFactory.createLineBorder(Color.DARK_GRAY, 3);
	private final static Border unfocusBorder=BorderFactory.createEmptyBorder(3, 3, 3, 3);
	private final List<ChannelDef> vpd;
	private final List<ViewChannelsInfo> vlist=new ArrayList<ViewChannelsInfo>();
	private final JTextField pgname=new JTextField(15);
	private final JComboBox<String> cols=new JComboBox<String>(new String[]{"1","2","3","4","5"});
	private final JComboBox<String> rows=new JComboBox<String>(new String[]{"1","2","3","4","5"});

	private final JPanel userPanel=new JPanel();
	private JScrollPane sel=null;
	private final JButton addChn,delChn,colorChn;

	public PageConf(List<ChannelDef> vpd) {
		super(new GridBagLayout());
		this.vpd = vpd;
		GridBagConstraints constr = new GridBagConstraints();
		//constr.anchor = GridBagConstraints.LINE_START;
		constr.fill = GridBagConstraints.BOTH;
		constr.weighty=0; constr.weightx=0;
		constr.insets=new Insets(5,5,0,0);//top,left,bot,right
		constr.gridwidth = 2;
		add(new JLabel("Page name:"),constr);
		constr.gridwidth = GridBagConstraints.REMAINDER;
		add(pgname,constr);

		JButton b;
		constr.gridwidth = 1;
		constr.weightx=0;
		add(new JLabel("Cols:"),constr);
		add(cols,constr);
		add(new JLabel("Rows:"),constr);
		add(rows,constr);
		add(b=new JButton("Set"),constr);
		b.setActionCommand("set");
		b.addActionListener(this);
		constr.gridwidth = GridBagConstraints.REMAINDER;
		constr.weightx=1;
		add(new JLabel(),constr);

		constr.insets.right=5;
		constr.gridwidth = 6;
		constr.weighty=constr.weightx=1;
		add(userPanel,constr);

		constr.gridwidth = GridBagConstraints.REMAINDER;
		constr.weighty=constr.weightx=0;
		JPanel p=new JPanel(new GridBagLayout());
		p.add(b=new JButton("+"),constr);
		addChn=b;
		b.setActionCommand("addchn");
		b.addActionListener(this);
		p.add(b=new JButton("-"),constr);
		delChn=b;
		b.setActionCommand("delchn");
		b.addActionListener(this);
		p.add(b=new JButton("Color"),constr);
		colorChn=b;
		UiUtils.makeSimpleButton(b);
		b.setActionCommand("colorchn");
		b.addActionListener(this);
		constr.weighty=1;
		p.add(new JLabel(),constr);
		add(p,constr);
		setupButtons();

		addButtons();
	}
	public int getColumns() { return cols.getSelectedIndex()+1; }
	public List<ViewChannelsInfo> getViewInfo() { return vlist; }
	public void createSetup(JPanel page) {
		if (page==null) return ;
		int cols=3,rows;
		pgname.setText(page.getName());
		if (page.getLayout() instanceof GridLayout) {
			int c=((GridLayout)page.getLayout()).getColumns();
			if (c>=0) cols=c;
		}
		rows=(page.getComponentCount()+cols-1)/cols;
		if (rows==0) rows=2;
		this.cols.setSelectedIndex(cols-1);
		this.rows.setSelectedIndex(rows-1);
		vlist.clear();
		if (page.getComponentCount()>0){
			for (int i=0; i<page.getComponentCount(); ++i){
				JComponent c=(JComponent)page.getComponent(i);
				if (c!=null && c instanceof MeasView) {
					vlist.add(((MeasView)c).vci);
				}
				else {
					ViewChannelsInfo vci= new ViewChannelsInfo();
					vci.view=new GraphView(false);
					vlist.add(vci);
				}
			}
		}
		buildUserPanel(rows, cols);
	}
	@SuppressWarnings("unchecked")
	private void buildUserPanel(int rows,int cols){
		userPanel.setLayout(new GridLayout(0,cols,2,2));
		int n=rows*cols;
		if (userPanel.getComponentCount()<n){
			for (int i=userPanel.getComponentCount(); i<n; ++i) {
				final JList<ChannelDef> l=new JList<ChannelDef>(
						new DefaultListModel<ChannelDef>());
				final JScrollPane sp=new JScrollPane(l,
						ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
						ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
				sp.setBorder(unfocusBorder);
				sp.setPreferredSize(new Dimension(120,35));//height ~1.5 line in JList

				l.addFocusListener(new FocusListener() {
					@Override
					public void focusLost(FocusEvent e) {
					}
					@Override
					public void focusGained(FocusEvent e) {
						if (sel!=null) {
							if (sel==sp) return ;
							sel.setBorder(unfocusBorder);
							JList<ChannelDef> l=(JList<ChannelDef>)sel.getViewport().getView();
							l.clearSelection();
						}
						sel=sp;
						JList<ChannelDef> l=(JList<ChannelDef>)sel.getViewport().getView();
						if (l.getSelectedIndex()<0 && l.getMaxSelectionIndex()>=0)
							l.setSelectedIndex(0);
						sp.setBorder(focusedBorder);
						setupButtons();
					}
				});
				l.addListSelectionListener(new ListSelectionListener() {
					@Override
					public void valueChanged(ListSelectionEvent e) {
						setColorButton(l);
					}
				});
				userPanel.add(sp);
			}
		}
		else {
			while (userPanel.getComponentCount()>n)
				userPanel.remove(n);
		}
		while (vlist.size()>n) {
			vlist.remove(vlist.size()-1);
		}
		while (vlist.size()<n) {
			ViewChannelsInfo vci= new ViewChannelsInfo();
			vci.view=new GraphView(false);
			vlist.add(vci);
		}
		for (int i=0; i<n; ++i) {
			JScrollPane sp=(JScrollPane)userPanel.getComponent(i);
			JList<ChannelDef> l=(JList<ChannelDef>)sp.getViewport().getView();
			//JList<ChannelDef> l=(JList<ChannelDef>)userPanel.getComponent(i);
			DefaultListModel<ChannelDef> lm=(DefaultListModel<ChannelDef>)l.getModel();
			lm.clear();
			if (i>=vlist.size()) continue;

			ViewChannelsInfo vci=vlist.get(i);
			if (vci==null) continue;
			for (int j=0; j<vci.defs.size(); ++j) {
				ChannelDef pd=vci.defs.get(j);
				lm.add(j, pd);
			}
		}
		validate();
	}
	@SuppressWarnings("unchecked")
	private void setupButtons() {
		if (sel==null) {
			addChn.setEnabled(false);
			delChn.setEnabled(false);
			colorChn.setBackground(this.getBackground());
			colorChn.setEnabled(false);
		}
		else {
			JList<ChannelDef> l=(JList<ChannelDef>)sel.getViewport().getView();
			DefaultListModel<ChannelDef> lm=(DefaultListModel<ChannelDef>)l.getModel();
			addChn.setEnabled(lm.size()<MAX_CHANNELS);
			delChn.setEnabled(lm.size()>0 && l.getSelectedIndex()>=0);
			colorChn.setEnabled(delChn.isEnabled());
			setColorButton(l);
		}
	}
	private void setColorButton(JList<ChannelDef> l){
		if (colorChn.isEnabled()) {
			ViewChannelsInfo vci=null;
			for (int i=0; i<userPanel.getComponentCount(); ++i) {
				if (sel==userPanel.getComponent(i))
					{vci=vlist.get(i); break;}
			}
			if (l.getSelectedIndex()>=0) {
				ChannelParams par=((GraphView)vci.view).getPlot().getParams(l.getSelectedIndex());
				colorChn.setBackground(par.color);
				return ;
			}
		}
		colorChn.setBackground(this.getBackground());
	}
	@Override
	@SuppressWarnings("unchecked")
	public void actionPerformed(ActionEvent ev) {
		final String cmd=ev.getActionCommand();
		if ("set".equals(cmd)) {
			int cols=this.cols.getSelectedIndex()+1;
			int rows=this.rows.getSelectedIndex()+1;
			buildUserPanel(rows,cols);
			((JDialog)getTopLevelAncestor()).pack();
			return ;
		}
		else if ("addchn".equals(cmd)) {
			if (sel==null) return ;
			List<ChannelDef> cur=null;
			for (int i=0; i<userPanel.getComponentCount(); ++i) {
				if (sel==userPanel.getComponent(i))
					{cur=vlist.get(i).defs; break;}
			}

			ChannelSelector p=new ChannelSelector(vpd,cur);
			if (p.getItems()<=0) {
				UiUtils.messageBox(null, "Adding Channel", "No more elements available", JOptionPane.WARNING_MESSAGE);
				return ;
			}
			UiUtils.showDialog(this,p,"Channel selector");
			if (p.getResult()>0){
				ChannelDef pd=p.getSelectedItem();
				JList<ChannelDef> l=(JList<ChannelDef>)sel.getViewport().getView();
				DefaultListModel<ChannelDef> lm=(DefaultListModel<ChannelDef>)l.getModel();
				lm.add(lm.size(), pd);
				cur.add(pd);
				setupButtons();
			}
		}
		else if ("delchn".equals(cmd)) {
			if (sel==null) return ;
			List<ChannelDef> cur=null;
			for (int i=0; i<userPanel.getComponentCount(); ++i) {
				if (sel==userPanel.getComponent(i))
					{cur=vlist.get(i).defs; break;}
			}
			JList<ChannelDef> l=(JList<ChannelDef>)sel.getViewport().getView();
			if (l.getSelectedIndex()>=0) {
				DefaultListModel<ChannelDef> lm=(DefaultListModel<ChannelDef>)l.getModel();
				cur.remove(l.getSelectedIndex());
				lm.remove(l.getSelectedIndex());
				l.clearSelection();
				setupButtons();
			}
		}
		else if ("colorchn".equals(cmd)) {
			if (sel==null) return ;
			ViewChannelsInfo vci=null;
			for (int i=0; i<userPanel.getComponentCount(); ++i) {
				if (sel==userPanel.getComponent(i))
					{vci=vlist.get(i); break;}
			}
			if (vci!=null) {
				DialogPanel p=new DialogPanel();
				JList<String> cl=new JList<String>(new String[]{"Yellow","Cyan","Blue","Green","Magenta","Pink"});
				p.add(cl);
				p.addButtons();
				UiUtils.showDialog(this,p,"Color selector");
				if (p.getResult()>0 && cl.getSelectedIndex()>=0){
					JList<ChannelDef> l=(JList<ChannelDef>)sel.getViewport().getView();
					ChannelParams par=((GraphView)vci.view).getPlot().getParams(l.getSelectedIndex());
					par.color=PlotXY.chnColorSimple[cl.getSelectedIndex()];
					setColorButton(l);
				}
			}
		}
		else {
			if ("ok".equals(cmd)){
				this.setName(pgname.getText());
				while (vlist.size()>1 && vlist.get(vlist.size()-1).defs.size()==0)
					vlist.remove(vlist.size()-1);
			}
			super.actionPerformed(ev);
		}
	}
}
