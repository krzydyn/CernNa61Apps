package cern.hv;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public class BPDView extends JPanel {
	private final HVChannelGroupUI bpd=new HVChannelGroupUI(1,5,5);

	public BPDView(HVControl ctrl) {
		super(new GridBagLayout());
		GridBagConstraints constr = new GridBagConstraints();

		bpd.create(ctrl,ctrl.getChannelGroup("bpd"),0);
		constr.gridwidth=GridBagConstraints.REMAINDER;
		add(bpd,constr);
	}
}
