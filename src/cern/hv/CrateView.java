package cern.hv;

import java.awt.BorderLayout;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import sys.Logger;
import caen.CaenCrate;
import caen.HVModule;

@SuppressWarnings("serial")
public class CrateView extends JPanel {
	static Logger log=Logger.getLogger();
	private final JTabbedPane tabs;
	private final Vector<ModuleView> mods=new Vector<ModuleView>();
	public CrateView(HVControl ctrl,CaenCrate cr) {
		super(new BorderLayout());
		tabs=new JTabbedPane(JTabbedPane.LEFT,JTabbedPane.SCROLL_TAB_LAYOUT);
		add(tabs,BorderLayout.CENTER);

		log.debug("creating CrateView");
		for (int i=0; i<cr.getModulesCount(); ++i) {
			HVModule m=(HVModule)cr.getModule(i);
			log.debug("creating mod %s",m.toString());
			ModuleView mu=new ModuleView(ctrl,m);
			mods.add(mu);
			tabs.addTab("Slot "+m.getSlot(),mu);
		}
	}
	public void dispose() {
		tabs.removeAll();
		mods.removeAllElements();
	}
	ModuleView getModuleView(HVModule m){
		if (m==null) throw new NullPointerException("HVModule is null");
		for (int i=0; i<tabs.getTabCount(); ++i) {
			ModuleView v=(ModuleView)tabs.getComponentAt(i);
			if (v.mod==m) return v;
			//if (v.mod.getSlot()==m.getSlot()) return v;
		}
		return null;
	}
}
