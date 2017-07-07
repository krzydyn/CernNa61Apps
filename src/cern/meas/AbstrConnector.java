package cern.meas;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import channel.ChannelData;
import common.Logger;
import common.SysUtil;
import common.util.SyncQueue;

/**
 * @deprecated use {@link #AsyncConn} instead.
 */
@Deprecated
public abstract class AbstrConnector {
	static protected Logger log=Logger.getLogger();
	public static final int RXDATA=0x1;
	public static final int TXDATA=0x2;
	private static final int PERIOD=10;//in seconds

	protected final ChangeEvent rxtx=new ChangeEvent(this);
	protected final ActionEvent updChn = new ActionEvent(this, 0, "updChn");
	private final Runnable runloop=new Runnable(){
		public void run(){
			stopping=false;
			thread=Thread.currentThread();
			log.info("connector loop started");
			try{loop();}
			catch (Throwable e) {log.debug(e);}
			finally{
				//cleanup
				thread=null;
				rxtxState=0;
				chnWrite.clear();
				chnRead.clear();
				stopping=false;
				log.info("connector loop finished");
				disconnect();
				if (listener!=null) {
					listener.stateChanged(rxtx);
					listener.disconnected();
				}
			}
		}
	};
	//user request message queue
	protected final SyncQueue<Msg> msgq=new SyncQueue<Msg>();
	//TODO: make one pending for response list
	protected final ArrayList<ChannelData> chnRead=new ArrayList<ChannelData>();
	protected final ArrayList<ChannelData> chnWrite=new ArrayList<ChannelData>();
	protected String addr;
	protected ConnectorListener listener;
	private final ArrayList<ActionListener> chnlners = new ArrayList<ActionListener>();
	private Thread thread;
	protected boolean refresh=false;
	protected int rxtxState;
	private boolean stopping=false;

	public String getName() {return getClass().getSimpleName();}
	abstract public int getDefaultPort();
	public int getState(){return rxtxState;}
	public boolean isStopping(){return stopping;}
	
	static protected ChannelData getChannelData(ArrayList<ChannelData> l,String name){
		for (int i=0; i<l.size(); ++i)
			if (l.get(i).def.name.equals(name)) return l.get(i);
		return null;
	}
	public void start(String addr){
		if (addr==null) throw new NullPointerException("addr is null");
		if (addr.indexOf(':')<0) addr+=":"+getDefaultPort();
		log.info("starting connector with addr=%s",addr);
		this.addr=addr;
		new Thread(runloop).start();
	}
	public void stop(){
		if (thread!=null){
			stopping=true;
			thread.interrupt();
		}
		refresh=true;
	}
	public void getErrorMsg(StringBuilder b){}
	public void setConnectorListener(ConnectorListener l){
		listener=l;
	}
	public void addChannelListener(final ActionListener al) {
		if (!chnlners.contains(al)) chnlners.add(al);
	}
	public void removeChannelListener(final ActionListener al) {
		chnlners.remove(al);
	}
	public void fireAction(ActionEvent a){
		for (int i = 0; i < chnlners.size(); ++i) {
			ActionListener l = chnlners.get(i);
			l.actionPerformed(a);
		}
	}
	protected final void fire(final ActionEvent a){
		if (chnlners.size()==0) return ;
		SwingUtilities.invokeLater(new Runnable(){
			public void run() { fireAction(a); }
		});
	}

	//register channel for reading
	public void readChn(ChannelData c) {
		if (c.def.name==null) throw new NullPointerException("channel def.name");
		log.debug("put readChn(%s)",c.def.name);
		msgq.put(new Msg(Msg.CMD_READ,c));
		refresh=true;
	}
	//register channel for writing
	public void writeChn(ChannelData c) {
		if (c.def.name==null) throw new NullPointerException("channel def.name");
		if (c.elems==0) throw new NullPointerException("channel elems=0");
		log.debug("put writeChn(%s,%f)",c.def.name,c.value[0]);
		msgq.put(new Msg(Msg.CMD_WRITE,c));
		refresh=true;
	}

	abstract protected void connect()throws IOException;
	abstract protected void disconnect();
	abstract protected void checkHW(StringBuilder b) throws IOException;
	abstract protected void send(StringBuilder b) throws IOException;
	abstract protected void recv(StringBuilder b) throws IOException;

	protected void process() throws Exception{
		Msg msg;
		if (msgq.isEmpty()) return ;
		while ((msg=msgq.get(SysUtil.SECOND/10))!=null){
			if (msg.cmd==Msg.CMD_READ){
				if (!chnRead.contains(msg.chn)) chnRead.add(msg.chn);
				else log.debug("chn %s already reading",msg.chn.def.name);
			}
			else if (msg.cmd==Msg.CMD_WRITE) {
				if (!chnWrite.contains(msg.chn)) chnWrite.add(msg.chn);
			}
			else {
				log.error("wrong msg.cmd=%d",msg.cmd);
			}
		}
	}
	protected void loop(){
		StringBuilder b=new StringBuilder();
		long t,tproc;
		tproc=System.currentTimeMillis()+PERIOD/2;
		try{
			connect();
			checkHW(b);
			if (listener!=null) listener.connected();
			while (!thread.isInterrupted()){
				t=System.currentTimeMillis();
				if(t<tproc){
					while (!refresh){
						t=tproc-t;
						if (t<0) break;
						if (t>1000) t=1000;
						Thread.sleep(t);
						t=System.currentTimeMillis();
					}
					if (refresh) tproc=System.currentTimeMillis();
				}
				else tproc=t;
				tproc+=PERIOD*1000; refresh=false;
				try{
					process();
					rxtxState=TXDATA;
					if (listener!=null) listener.stateChanged(rxtx);
					checkHW(b);
					if (chnWrite.size()>0 || chnRead.size()>0){
						send(b);
						rxtxState=RXDATA;
						if (listener!=null) listener.stateChanged(rxtx);
						recv(b);
						log.info("readout done");
						rxtxState=0;
						if (listener!=null) listener.stateChanged(rxtx);
						fire(updChn);
					}
					else{
						rxtxState=0;
						if (listener!=null) listener.stateChanged(rxtx);
					}
				}
				catch (InterruptedException e) { throw e; }
				catch (IOException e) { stopping=true;throw e; }
				catch (Exception e) { log.error(e,"intr=%b",thread.isInterrupted()); }
			}
		}
		catch (InterruptedException e){
			log.error(e.toString());
		}
		catch (Exception e) {
			log.error(e);
			if (!stopping && listener!=null) listener.exception(e);
		}
	}
	public static class Msg{
		public static final int CMD_READ=0;
		public static final int CMD_WRITE=1;
		//static final int CMD_LOCK=2;
		public int cmd;
		public ChannelData chn;
		public boolean equals(Object o){
			if (o==null || !(o instanceof Msg)) return false;
			Msg m=(Msg)o;
			return cmd==m.cmd && chn.def.name.equals(m.chn.def.name);
		}
		private Msg(int cmd,ChannelData chn){this.cmd=cmd;this.chn=chn;}
	}
	public static interface ConnectorListener extends ChangeListener {
		public void connected();
		public void disconnected();
		public void exception(Exception e);
		public void writeDone(int r,ChannelData c);
		public void readDone(int r,ChannelData c,float[] v);
	}
}
