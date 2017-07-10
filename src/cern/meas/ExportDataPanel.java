package cern.meas;
/**
*
* @author KySoft, Krzysztof Dynowski
*
*/

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import sys.Logger;
import sys.StrUtil;
import channel.ChannelDef;

@SuppressWarnings("serial")
public class ExportDataPanel extends JPanel implements ActionListener {
	Logger log=Logger.getLogger();

	private final Vector<ChannelDef> avail;
	private final Vector<ChannelDef> choice=new Vector<ChannelDef>();

	private final JList<ChannelDef> avList,chList;
	public ExportDataPanel(Vector<ChannelDef> pntdef)
	{
		super(new GridBagLayout());
		GridBagConstraints constr = new GridBagConstraints();
        constr.fill = GridBagConstraints.BOTH;
        constr.anchor = GridBagConstraints.LINE_START;
        //constr.weightx=constr.weighty=0;
        constr.insets=new Insets(5,5,0,0);

		avail=new Vector<ChannelDef>(pntdef);

        JList<ChannelDef> li = new JList<ChannelDef>();
		li.setListData(avail);
		add(new JScrollPane(li),constr);
		avList=li;

		JButton b;
		JPanel p=new JPanel(new GridLayout(0,1));
		p.add(b=new JButton(">"));
		b.setActionCommand("av2ch");
		b.addActionListener(this);
		p.add(b=new JButton("<"));
		b.setActionCommand("ch2av");
		b.addActionListener(this);
		add(p,constr);

		li=new JList(choice);
		add(new JScrollPane(li),constr);
		chList=li;
	}
	@Override
	public void actionPerformed(ActionEvent ev) {
		final String cmd=ev.getActionCommand();
		if ("av2ch".equals(cmd) || "ch2av".equals(cmd))
		{
			Vector<ChannelDef> src,dst;
			int ind[];
			if ("av2ch".equals(cmd))
			{
				src=avail; dst=choice;
				ind=avList.getSelectedIndices();
			}
			else
			{
				src=choice; dst=avail;
				ind=chList.getSelectedIndices();
			}
			log.debug("ind[n=%d]='%s'",ind.length,StrUtil.implode(",",ind));
			if (ind.length==0) return ;
			Vector<ChannelDef> sel=new Vector<ChannelDef>();
			for (int i=0; i<ind.length; ++i)
			{
				ChannelDef pd=src.elementAt(ind[i]);
				sel.addElement(pd);
				dst.addElement(pd);
			}
			while (sel.size()>0) src.remove(sel.remove(0));
			avList.setListData(avail);
			chList.setListData(choice);
		}
	}
}
