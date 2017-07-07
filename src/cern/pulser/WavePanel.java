package cern.pulser;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;

import plot.PlotXY;
import plot.PlotXY.PlotListener;
import cern.pulser.Pulser.Waveform;
import common.Logger;
import common.algebra.Const;
import common.algebra.Geom2D;
import common.algebra.SqrFitt;
import common.io.IOUtils;
import common.ui.DocumentValidator;
import common.ui.UiUtils;

@SuppressWarnings("serial")
public class WavePanel extends JPanel implements ActionListener, PlotListener {
	final static Logger log = Logger.getLogger();
	final private JFileChooser chooser = new JFileChooser();

	private final JButton[] wavesel=new JButton[4];
	private final JButton[] waverd=new JButton[4]; 
	private JTextField curPos=new JTextField(4);
	private JTextField curVal=new JTextField(4);
	private final PlotXY plot=new PlotXY();
	private int curWaveRead=-1;

	final private PulserConnector connector;
	
	public WavePanel(PulserConnector c) {
		super(new GridBagLayout());
		connector=c;
		GridBagConstraints constr = new GridBagConstraints();
		Dimension sz=new Dimension(18,18);
		constr.anchor=GridBagConstraints.LINE_START;
		constr.insets=new Insets(1,1,1,1);
		constr.weightx=0;constr.weighty=0;
		JButton b;
		add(new JLabel("Select Wave"),constr);
		for (int i=0; i<wavesel.length; ++i){
			add(b=new JButton(String.format("%d",i+1)),constr);
			UiUtils.makeSimpleButton(b);
			b.setPreferredSize(sz);
			b.addActionListener(this);
			b.setActionCommand(String.format("s%d",i));
			wavesel[i]=b;
		}		
		constr.fill=GridBagConstraints.BOTH;
		constr.weightx=1;
		constr.gridwidth=4;
		add(new JLabel(),constr);
		constr.fill=GridBagConstraints.NONE;
		constr.gridwidth=1;
		constr.weightx=0;
		add(new JLabel("x:"),constr);
		constr.gridwidth=GridBagConstraints.REMAINDER;
		add(curPos,constr);
		
		constr.weightx=0;
		constr.fill=GridBagConstraints.NONE;
		constr.gridwidth=1;
		add(new JLabel("Read Wave"),constr);
		for (int i=0; i<waverd.length; ++i){
			add(b=new JButton(String.format("%d",i+1)),constr);
			UiUtils.makeSimpleButton(b);
			b.setPreferredSize(sz);
			b.addActionListener(this);
			b.setActionCommand(String.format("w%d",i));
			waverd[i]=b;
		}
		add(new JLabel("  "),constr);
		add(b=new JButton("Save..."),constr);
		b.addActionListener(this);
		b.setActionCommand("save");
		(sz=b.getPreferredSize()).height=18;
		b.setPreferredSize(sz);
		add(b=new JButton("Upload..."),constr);
		b.addActionListener(this);
		b.setActionCommand("upload");			
		(sz=b.getPreferredSize()).height=18;
		b.setPreferredSize(sz);
		constr.fill=GridBagConstraints.BOTH;
		constr.weightx=1;
		add(new JLabel(),constr);
		constr.fill=GridBagConstraints.NONE;
		constr.weightx=0;
		add(new JLabel("y:"),constr);
		constr.gridwidth=GridBagConstraints.REMAINDER;
		add(curVal,constr);
		
		constr.fill=GridBagConstraints.BOTH;
		constr.weightx=1;constr.weighty=1;
		add(plot,constr);
		setFocusable(true);
		addKeyListener(new KeyAdapter(){
			public void keyPressed(KeyEvent ev){
				plot.keyPressed(ev);
			}
		});
		
		plot.setAutoBounds(PlotXY.AUTOBOUNDS_NONE,PlotXY.AUTOBOUNDS_SCALE);
		plot.setUnit("time","Volts");
		plot.setPlotListener(this);
		plot.setXDelta(1.5);
		JMenuItem mi=new JMenuItem("Smooth");
		mi.setActionCommand("smooth");
		mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,0));
		mi.addActionListener(this);
		plot.addPopupItem(mi);
		mi=new JMenuItem("Move...");
		mi.setActionCommand("move");
		mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M,0));
		mi.addActionListener(this);
		plot.addPopupItem(mi);

		curPos.setHorizontalAlignment(JTextField.RIGHT);
		curVal.setHorizontalAlignment(JTextField.RIGHT);
		curVal.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ev){
            	Rectangle2D sel=plot.getSelection();
            	if (sel.getWidth()<Const.eps) return ;
            	List<Point2D> pnts=plot.getPoints(0);
            	int y=Integer.parseInt(curVal.getText());
            	for (int i=0; i<pnts.size(); ++i){
            		Point2D p=pnts.get(i);
            		if (p.getX()<sel.getMinX()) continue;
            		if (p.getX()>sel.getMaxX()) break;
            		p.setLocation(p.getX(),y);
            	}
            	if (y>plot.getView().getMaxY()) plot.autoFitY2();
            	plot.repaint(100);
            }});
		
		chooser.setCurrentDirectory(new File("."));
		chooser.setFileFilter(new FileFilter(){
			public boolean accept(File f) {
				return f.isDirectory()||f.getName().endsWith(".csv");
			}
			public String getDescription() {
				return "CSV files";
			}
		});
		File f=chooser.getCurrentDirectory();
		f=new File(f.getPath()+File.separator+"pulser");
		if (!f.exists()){
			if (!f.mkdir()) log.error("can't create dir %s",f.getPath());
			else log.debug("created %s",f.getPath());
		}
		chooser.setCurrentDirectory(f);
	}
	public void actionPerformed(ActionEvent ev) {
		String cmd=ev.getActionCommand();
		if (cmd.matches("s[0-9]+")) {
			int nr=Integer.parseInt(cmd.substring(1));
			connector.writeWaveformNr(nr);
			connector.readWaveformNr();			
		} else if (cmd.matches("w[0-9]+")) {
			int nr=Integer.parseInt(cmd.substring(1));
			connector.readWaveform(nr);
		} else if ("save".equals(cmd)) {
			if (curWaveRead<0) return ;
			File f=new File(String.format("wave-%d.csv",curWaveRead+1));
			chooser.setSelectedFile(f);
			int r=chooser.showSaveDialog(getParent());
			if (r!=JFileChooser.APPROVE_OPTION) return ;
			f=chooser.getSelectedFile();
			List<Point2D> pnts=plot.getPoints(0);
			try{
				PrintStream out=new PrintStream(f);
				for (int i=0; i<pnts.size(); ++i)
					out.printf("%d\n",Math.round(pnts.get(i).getY()));
				out.close();
				log.info("saved waveform %d to %s",curWaveRead+1,f.getAbsoluteFile());
			}catch (Exception e) {
				log.error(e.toString());
			}
				
		} else if ("upload".equals(cmd)) {
			if (curWaveRead<0) return ;
			File f=new File(String.format("wave-%d.csv",curWaveRead+1));
			chooser.setSelectedFile(f);
			int r=chooser.showOpenDialog(getParent());
			if (r!=JFileChooser.APPROVE_OPTION) return ;			
			f=chooser.getSelectedFile();
			BufferedReader in=null;
			Waveform w=connector.getWaveform();
			try{
				in=new BufferedReader(new FileReader(f));
				String ln;
				Pattern patt=Pattern.compile("([0-9]+)");
				int i=0;
				for (int line=1; (ln=in.readLine())!=null; ++line){
					if (ln.startsWith(";")||ln.startsWith(";")) continue;
					if (i>=w.data.length) break;
					Matcher m=patt.matcher(ln);
					if (m.find()){
						w.data[i]=Integer.parseInt(m.group(1));
						++i;
					}
					else log.error("%d(%d): waveform malformed",f,line);
				}
				for (; i<w.data.length; ++i) w.data[i]=0;
				log.info("loaded %d waveform",curWaveRead+1);
				connector.writeWaveform(curWaveRead);
			}catch (Exception e) {
				log.error(e.toString());
			} finally{
				if (in!=null) IOUtils.close(in);	
			}
		} else if ("smooth".equals(cmd)) {
        	Rectangle2D sel=plot.getSelection();
        	if (sel.getWidth()<Const.eps) return ;
        	List<Point2D> pnts=plot.getPoints(0);
        	int i,is=0,ie=pnts.size()-1;
        	for (i=0; i<pnts.size(); ++i){
        		Point2D p=pnts.get(i);
        		if (p.getX()>=sel.getMinX()) {is=i;break;}
        	}
        	for (; i<pnts.size(); i+=10){
        		Point2D p=pnts.get(i);
        		if (p.getX()>=sel.getMaxX()) {ie=i;break;}
        		
        	}
        	if (is==0) is=1;
        	if (ie>pnts.size()-2) ie=pnts.size()-2;
        	if (is>=ie) return ;
        	
        	int mode=2;
        	double ys=pnts.get(is).getY(), ye=pnts.get(ie).getY();
        	if (mode==0){//b-spline P(t)=(t^2-2t+1)*P0+(2t-2t^2)*P1+t^2*P2, t=(0..1)
        		double yc=pnts.get((is+ie)/2).getY();
	        	for (i=is+1; i<ie; ++i){
	        		Point2D p=pnts.get(i);
	        		double t=i-is; t/=(ie-is);
	        		double y=(t*t-2*t+1)*ys+(2*t-2*t*t)*yc+t*t*ye;
	        		double c=(t-0.5)*(t-0.5)*4;
	        		p.setLocation(p.getX(),p.getY()*c+y*(1.0-c));
	        	}
        	}
        	else if (mode==1){//sqrfitt to polynom 4
        		SqrFitt.PolyPar par=new SqrFitt.PolyPar();
        		SqrFitt.poly(4,pnts,is,ie-is,par);
        		//make convolution with current plot
        		for (i=is; i<=ie; ++i){
        			Point2D p=pnts.get(i);
        			double t=i-is; t/=(ie-is);
        			double y=par.calc(p.getX());
        			//t=0,c=1; t=0.5,c=0; t=1,c=1
        			double c=(t-0.5)*(t-0.5);
        			c=c*c*16;
        			p.setLocation(p.getX(),p.getY()*c+y*(1.0-c));
        		}
        	}
        	else if (mode==2){//b-spline, method2
        		Line2D l1=new Line2D.Double();
        		Line2D l2=new Line2D.Double();
        		Point2D pc1,pc2;
        		//1.find good point inside
        		double sl1=Math.abs(pnts.get(is).getY()-pnts.get(is-1).getY());
        		double sl2=Math.abs(pnts.get(ie).getY()-pnts.get(ie+1).getY());
        		double msl=0; //max slope
        		int e=(is+ie)/2;
        		msl=Math.abs(pnts.get(e).getY()-pnts.get(e).getY());
        		msl=Math.abs(msl-sl1)+Math.abs(msl-sl2);
        		for (i=is+(ie-is)/3; i<ie-(ie-is)/3; ++i){
        			double sl=Math.abs(pnts.get(i+1).getY()-pnts.get(i).getY());
        			if (Math.abs(sl-sl1)<Const.eps || Math.abs(sl-sl2)<Const.eps) continue;
        			sl=Math.abs(sl-sl1)+Math.abs(sl-sl2);
        			if (msl<sl){msl=sl;e=i;}
        		}
        		if (msl<Const.eps) return ;
        		ye=(pnts.get(e).getY()+pnts.get(e+1).getY())/2;
        		//2.find control points
    			l1.setLine(pnts.get(is-1),pnts.get(is));
        		l2.setLine(pnts.get(e),pnts.get(e+1));
        		pc1=Geom2D.intersection(l1, l2, true);
        		l1.setLine(pnts.get(ie),pnts.get(ie+1));
        		pc2=Geom2D.intersection(l1, l2, true);
        		if (pc1==null || pc2==null) return ;
        		
        		ys=pnts.get(is).getY();
        		//3.draw spline (part 1)
        		for (i=is; i<e; ++i){
        			Point2D p=pnts.get(i);
        			double t=i-is; t/=(e-is);
        			double y=(1-t)*(1-t)*ys+2*t*(1-t)*pc1.getY()+t*t*ye;
        			//double c=1-t*t;
        			//p.setLocation(p.getX(),p.getY()*c+y*(1.0-c));
        			p.setLocation(p.getX(),y);
        		}
        		is=e; e=ie;
        		ys=ye; ye=pnts.get(e).getY();
        		//4.draw spline (part 2)
        		for (i=is; i<e; ++i){
        			Point2D p=pnts.get(i);
        			double t=i-is; t/=(e-is);
        			double y=(1-t)*(1-t)*ys+2*t*(1-t)*pc2.getY()+t*t*ye;
        			//double c=t*t;
        			//p.setLocation(p.getX(),p.getY()*c+y*(1.0-c));
        			p.setLocation(p.getX(),y);
        		}
        	}
        	plot.repaint(100);			
		} else if ("move".equals(cmd)) {
			int mv=0;
			for(String in="";;){
				in=(String)JOptionPane.showInputDialog(this,
						"Move by: ","Enter value to move waveform by",JOptionPane.QUESTION_MESSAGE,null,null,in);
				if (in==null) return ;
				if (in.matches(DocumentValidator.DECIMAL_SIGNED)){
					mv=Integer.parseInt(in); break;
				}
			}
			if (mv==0) return ;
			List<Point2D> pnts=plot.getPoints(0);
			Point2D p1,p2;
			if (mv<0) {
				for (int i=-mv; i<pnts.size(); ++i){
					p1=pnts.get(i+mv); p2=pnts.get(i);
					p1.setLocation(p1.getX(),p2.getY());
				}
				for (int i=pnts.size()+mv; i<pnts.size(); ++i){
					p2=pnts.get(i);
					p2.setLocation(p2.getX(),0);
				}
			}
			else {
				for (int i=pnts.size()-mv; i>0; --i){
					p1=pnts.get(i-1+mv); p2=pnts.get(i-1);
					p1.setLocation(p1.getX(),p2.getY());
				}
				for (int i=0; i<mv; ++i){
					p2=pnts.get(i);
					p2.setLocation(p2.getX(),0);
				}				
			}
			plot.repaint(100);
		}
	}
	public void setWaveformNr(int nr) {
		for (int i=0; i<wavesel.length; ++i)
			if (i==nr) wavesel[i].setBackground(Color.GREEN);
			else wavesel[i].setBackground(null);
	}
	public int getWaveform() {return curWaveRead;}
	public void setWaveform(Waveform w) {
		int nr=w==null?-1:w.nr;
		for (int i=0; i<waverd.length; ++i) {
			if (i==nr) {
				waverd[i].setBackground(Color.GREEN);
			}
			else {
				waverd[i].setBackground(null);
			}
		}
		curWaveRead=nr;
		plot.clear();
		if (w!=null){
			plot.setView(0,0,w.data.length,100);
			plot.addPoints(0,0f,1f,w.data,w.data.length);						
		} else plot.setView(0,0,0,0);
	}
	
	public void caretChanged(PlotXY plot) {
		Point2D c=plot.getCaret();
		curPos.setText(String.format("%d",Math.round(c.getX())));
		curVal.setText(String.format("%d",Math.round(c.getY())));
	}
	public void paintDone(PlotXY plot) {
	}
	public void selChanged(PlotXY plot) {
	}
	public void viewChanged(PlotXY plot, Rectangle2D oldview, int opt) {
	}
}
