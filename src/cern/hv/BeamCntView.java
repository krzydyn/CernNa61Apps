package cern.hv;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public class BeamCntView extends JPanel {
	private final HVChannelGroupUI bcnt=new HVChannelGroupUI(1,5,5);

	public BeamCntView(HVControl ctrl) {
		super(new GridBagLayout());
		GridBagConstraints constr = new GridBagConstraints();

		bcnt.create(ctrl,ctrl.getChannelGroup("bcnt"),6);
		constr.gridwidth=GridBagConstraints.REMAINDER;
		add(bcnt,constr);
	}
}
