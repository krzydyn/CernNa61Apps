package cern.meas;
/**
*
* @author KySoft, Krzysztof Dynowski
*
*/

import java.io.DataInputStream;
import java.io.IOException;

public class PntData {
	public int id;
	public long tm;//in seconds from Unix epoch 
	public float value;
	public PntData(){}
	public PntData(PntData o) {
		id=o.id; tm=o.tm; value=o.value;
	}
	public void read(DataInputStream in) throws IOException {
		readId(in);
		readData(in);
	}
	public void readId(DataInputStream in) throws IOException {
		id=in.readInt();
	}
	public void readData(DataInputStream in) throws IOException {
		tm=in.readLong();
		value=in.readFloat();
	}
}
