package cern.pulser;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cern.pulser.Pulser.Module;
import common.Logger;
import common.img.ImageOps;
import common.ui.UiUtils;
import common.util.Resource;

@SuppressWarnings("serial")
public class PulserModule extends JPanel implements ActionListener {
	static protected Logger log=Logger.getLogger();
	static final int LEDSIZE=20;
	static ImageIcon ledgray,ledgreen,ledred;
	private final JLabel pwrOK=new JLabel("ok");
	private final JLabel pwrERR=new JLabel("err");
	private final JButton[] chns=new JButton[9];
	private PulserConnector connector;
	private int slot;
	public PulserModule(PulserConnector c,int s) {
		super(new GridBagLayout());
		connector=c; slot=s;
		//setPreferredSize(new Dimension(47,500));
		//setMinimumSize(getPreferredSize());
		if (ledgray==null){
			BufferedImage gray,i=null;
			try {
				i=ImageIO.read(Resource.getResourceURL("res/led2.png"));
			} catch (Exception e) {}
			
			ImageOps.makeGray(i);
			gray=ImageOps.getScaledImageQ(i,LEDSIZE,LEDSIZE);
			ledgray=new ImageIcon(gray);
			
			float[] mrgb={1f,2.8f,1f};
			i=ImageOps.getScaledImage(gray,LEDSIZE,LEDSIZE,BufferedImage.TYPE_INT_ARGB);
			ledgreen=new ImageIcon(ImageOps.imgMult(i,mrgb));
			
			mrgb[0]=2.8f;mrgb[1]=1f;
			i=ImageOps.getScaledImage(gray,LEDSIZE,LEDSIZE,BufferedImage.TYPE_INT_ARGB);
			ledred=new ImageIcon(ImageOps.imgMult(i,mrgb));
		}
		
		Cursor hand=Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
		
		GridBagConstraints constr = new GridBagConstraints();
		//constr.insets=new Insets(1,1,1,1);
		constr.fill=GridBagConstraints.HORIZONTAL;
		constr.weightx=1;constr.weighty=1;
		constr.gridwidth=1;
		JButton b;
		Dimension sz=new Dimension(30,18);
		add(b=new JButton("Off"),constr);
		b.setCursor(hand);
		UiUtils.makeRawButton(b);
		b.setFont(getFont().deriveFont(10f));
		b.setActionCommand("off");
		b.setPreferredSize(sz);
		b.addActionListener(this);
		constr.insets=new Insets(0,1,0,0);
		constr.gridwidth=GridBagConstraints.REMAINDER;
		add(b=new JButton("On"),constr);
		b.setCursor(hand);
		UiUtils.makeRawButton(b);
		b.setActionCommand("on");
		b.setFont(getFont().deriveFont(10f));
		b.setPreferredSize(sz);
		b.addActionListener(this);
		
		constr.insets=new Insets(1,1,1,1);
		constr.fill=GridBagConstraints.NONE;
		constr.weightx=0;
		add(new JLabel("PWR"),constr);
		pwrOK.setIcon(ledgray);pwrERR.setIcon(ledgray);
		add(pwrOK,constr);
		add(pwrERR,constr);
		add(new JLabel(),constr);
		
		add(new JLabel("CHAN"),constr);
		//sz=new Dimension(20,20);
		for (int i=0; i<9; ++i){
			add(b=new JButton(String.format("%d",i+1),ledgray),constr);
			b.setCursor(hand);
			UiUtils.makeRawButton(b);
			b.addActionListener(this);
			//b.setPreferredSize(sz);
			chns[i]=b;
		}
	}
	public void actionPerformed(ActionEvent ev) {
		String cmd=ev.getActionCommand();
		Module m=connector.getModule(slot);
		int mask=m.getChannelMask();
		if ("on".equals(cmd)){
			mask=0xffff;
		}
		else if ("off".equals(cmd)){
			mask=0;
		}
		else if (cmd.matches("[0-9]")){
			int chn=Integer.parseInt(cmd)-1;
			mask^=1<<chn;
		}
		else return;
		connector.clearq();
		connector.writeChannels(slot,mask);
		connector.readChannels(slot);
		connector.readPowerStatus(slot);
	}
	public void updatePwr() {
		int m=connector.getModule(slot).getStatus();
		if (m<0) return ;
		pwrOK.setIcon((m&1)!=0?ledgreen:ledred);
		pwrERR.setIcon((m&2)!=0?ledred:ledgray);
	}
	public void updateChns() {
		int m=connector.getModule(slot).getChannelMask();
		if (m<0) return ;
		//log.debug("update module %d to %s",slot,Integer.toBinaryString(m));
		for (int i=0; i<9; ++i){
			if ((m&(1<<i))!=0) chns[i].setIcon(ledgreen);
			else chns[i].setIcon(ledgray);
			UiUtils.makeRawButton(chns[i]);
		}
	}
}
