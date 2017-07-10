package cern.hv;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;

import sys.StrUtil;
import sys.ui.DialogPanel;
import sys.ui.DocumentValidator;
import caen.HVModule;
import caen.HVModule.ChannelSettings;

@SuppressWarnings("serial")
class HVChannelParamsUI extends DialogPanel {
	private final static String[] fieldsNames={
		"V0set [V]", "I0set ["+StrUtil.MICRO+"A]",
		"Vmax [V]","Trip [s]", "RampUp [V/s]","RampDn [V/s]"};
	private final static int V0set=0;
	private final static int I0set=1;
	private final static int Vmax=2;
	private final static int Trip=3;
	private final static int Rup=4;
	private final static int Rdn=5;

	private final boolean withName;
	private final JTextField nameField;
	private final JTextField[] fields;

	public HVChannelParamsUI() {this(false);}
	public HVChannelParamsUI(boolean name) {
		super(new GridBagLayout());
		withName=name;
		GridBagConstraints constr = new GridBagConstraints();
		constr.fill = GridBagConstraints.BOTH;
		constr.insets=new Insets(5,5,0,0);//top,left,bot,right

		if (withName){
			constr.gridwidth = 1;
			add(new JLabel("Name"),constr);
			add(nameField=new JTextField(8),constr);
			constr.gridwidth = GridBagConstraints.REMAINDER;
			add(new JLabel(),constr);
			((AbstractDocument)nameField.getDocument()).setDocumentFilter(
					new DocumentValidator().setLimit(11).setRegex(DocumentValidator.USASCII));
		}
		else nameField=null;
		//JButton b;
		fields=new JTextField[fieldsNames.length];

		for (int i=0; i<fieldsNames.length; ++i)
		{
			constr.gridwidth = 1;
			add(new JLabel(fieldsNames[i]),constr);
			add(fields[i]=new JTextField(6),constr);
			constr.gridwidth = GridBagConstraints.REMAINDER;
			add(new JLabel(),constr);
		}
		((AbstractDocument)fields[V0set].getDocument()).setDocumentFilter(
				new DocumentValidator().setLimit(7).setRegex(DocumentValidator.FLOATING_UNSIGNED));
		((AbstractDocument)fields[I0set].getDocument()).setDocumentFilter(
				new DocumentValidator().setLimit(6).setRegex(DocumentValidator.FLOATING_UNSIGNED));
		((AbstractDocument)fields[Vmax].getDocument()).setDocumentFilter(
				new DocumentValidator().setLimit(5).setRegex(DocumentValidator.DECIMAL_USIGNED));
		((AbstractDocument)fields[Trip].getDocument()).setDocumentFilter(
				new DocumentValidator().setLimit(4).setRegex(DocumentValidator.FLOATING_UNSIGNED));
		((AbstractDocument)fields[Rup].getDocument()).setDocumentFilter(
				new DocumentValidator().setLimit(3).setRegex(DocumentValidator.DECIMAL_USIGNED));
		((AbstractDocument)fields[Rdn].getDocument()).setDocumentFilter(
				new DocumentValidator().setLimit(3).setRegex(DocumentValidator.DECIMAL_USIGNED));
		addButtons();
	}
	public void setProperties(ChannelSettings cs,int vdec,int idec) {
		if (withName){
			nameField.setText(cs.name);
		}
		fields[V0set].setText(HVModule.scaledValue(cs.v0set,vdec));
		fields[I0set].setText(HVModule.scaledValue(cs.i0set,idec));
		fields[Vmax].setText(String.format("%d",cs.vmax));
		fields[Trip].setText(HVModule.scaledValue(cs.trip,1));
		fields[Rup].setText(String.format("%d",cs.rup));
		fields[Rdn].setText(String.format("%d",cs.rup));
	}
	public void getProperties(ChannelSettings cs,int vdec,int idec){
		if (withName){
			cs.name=nameField.getText();
		}
		float scv=(float)Math.pow(10,vdec);
		float sci=(float)Math.pow(10,idec);
		cs.v0set=(int)(Float.parseFloat(fields[V0set].getText())*scv);
		cs.i0set=(int)(Float.parseFloat(fields[I0set].getText())*sci);
		cs.vmax=Integer.parseInt(fields[Vmax].getText());
		cs.trip=(int)(Float.parseFloat(fields[Trip].getText())*10f);
		cs.rup=Integer.parseInt(fields[Rup].getText());
		cs.rdn=Integer.parseInt(fields[Rdn].getText());
	}
}
