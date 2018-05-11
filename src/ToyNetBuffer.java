
public class ToyNetBuffer {
	/*
	 * |ID|LEN|data|CRC|
	 */
	
	private byte[] bf = new byte[Config.TNBF_MAX_BUFFER_SIZE];
	
	private byte ID;
	private int IDOffset = 0;
	
	private byte len;
	private int lenOffset = 1;
	
	private int dataSize;
	private int dataOffset = 2;
	
	
	private int maxSize = Config.TNBF_MAX_BUFFER_SIZE;
	
	private byte crc8;
	private int crc8Offset = -1;
	
	public void reset(){
		ID = 0;
		len = 0;
		crc8 = 0;
	}


	public void setCRC(byte crc) {
		//TODO
		bf[len-1] = crc;
		crc8 = crc;
	}
	
	public byte getCRC() {
		return crc8;
	}
	
	public void setLen(byte l) {
		bf[lenOffset] = l;
		len = l;
	}
	
	public short getLen() {
		return  (short) (0xFF & len);
	}
	
	
	public void setID(byte id) {
		bf[IDOffset] = id;
		ID = id;
	}
	
	public byte getID() {
		return ID;
	}
	

	public int getDataSize() {
		return dataSize;
	}
	
	public byte[] getRawbf() {
		return bf;
	}
	
	public ToyNetBuffer() {
	}
	
	public void setDatabf(byte value, int offset) {
		bf[dataOffset+offset] = value;
	}
	
	public ToyNetBuffer(byte[] inputdata, int validLength) {
		dataSize = validLength;
		crc8Offset = dataOffset+dataSize;
		
		for (int i=0; i<inputdata.length; i++) {
			bf[i+dataOffset] = inputdata[i];
		}
	}
}
