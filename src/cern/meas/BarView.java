package cern.meas;
/**
*
* @author KySoft, Krzysztof Dynowski
*
*/

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import plot.PlotBar;
import plot.PlotXY;
import channel.ChannelDef;

@SuppressWarnings("serial")
public class BarView extends MeasView {
	final static Font valuefont=UIManager.getFont("Panel.font").deriveFont(Font.BOLD,25f);
	private final PlotBar plot;
	private final JLabel lvalue;

	public BarView(){
		setLayout(new GridBagLayout());
		setBackground(Color.WHITE);
		plot=new PlotBar();
		lvalue=new JLabel("N/A");

		Border b=BorderFactory.createBevelBorder(BevelBorder.RAISED,Color.GRAY,Color.DARK_GRAY);

		lvalue.setHorizontalAlignment(SwingConstants.CENTER);
		lvalue.setFont(valuefont);

		GridBagConstraints constr = new GridBagConstraints();
		constr.fill = GridBagConstraints.BOTH;
		constr.anchor = GridBagConstraints.LINE_START;
		constr.gridwidth = GridBagConstraints.REMAINDER;
		constr.weighty=constr.weightx=1;
		
		plot.setBorder(b);
		add(plot,constr);
		lvalue.setBorder(b);
		constr.ipadx=4; constr.weighty=0;
		add(lvalue,constr);
	}
	public void setChnInfo(ViewChannelsInfo pi){
		super.setChnInfo(pi);
		ChannelDef def=pi.defs.get(0);
		plot.setUnit(null,def.unit);
		long now=System.currentTimeMillis()/1000;
		pi.rcv.getData(def.id,now-300,now);
		setToolTipText("<html>"+def.name+"<br>"+def.descr);
	}
	public void addValue(ChannelDef def,long tm,float v){
		plot.addPoint(tm,v);
		if (Double.isNaN(v)) {
			lvalue.setForeground(Color.GRAY);
			lvalue.setText("N/A");
		}
		else {
			lvalue.setForeground(Color.BLACK);
			lvalue.setText(PlotXY.format(v,plot.gridY(plot.getView().getHeight()/50)));
		}
	}
	public void addValues(ChannelDef def,List<PntData> v){
		log.error("BarView addValues should not be used");
	}
}
