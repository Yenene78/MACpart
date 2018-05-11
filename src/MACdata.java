public class MACdata {
    /*
     * |ID|LEN|Dst|Src|Type|beg?|fin?|data|CRC|
     */

    private byte[] bf = new byte[Config.TNBF_MAX_BUFFER_SIZE1];

    private byte ID;
    private int IDOffset = 0;

    private byte len;
    private int lenOffset = 1;

    private byte Dst;
    private int DstOffset = 2;

    private byte Src;
    private int SrcOffset = 3;

    private byte Type;
    private int TypeOffset = 4;

    private int dataSize;
    private int dataOffset = 5;


    private int maxSize = Config.TNBF_MAX_BUFFER_SIZE;

    private byte crc8;
    private int crc8Offset = -1;

    public void reset(){
        ID = 0;
        len = 0;
        Dst = -1;
        Src = -1;
        Type = -1;
        crc8 = 0;
    }

    public void setDst(byte dst){
        bf[DstOffset] = dst;
        Dst = dst;
    }
    public int getDst(){
        return Dst;
    }

    public void setSrc(byte src){
        bf[SrcOffset] = src;
        Src = src;
    }
    public int getSrc(){
        return Src;
    }

    public void setType(byte type){
        bf[TypeOffset] = type;
        Type = type;
    }
    public int getType(){
        return Type;
    }

    public void setCRC(byte crc) {
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

    public void setDatabf(byte value, int offset) {
        bf[dataOffset+offset] = value;
    }

    public MACdata(byte[] inputdata, int validLength) {
        dataSize = validLength;
        crc8Offset = dataOffset + dataSize;

        for (int i = 0; i < inputdata.length; i++) {
            bf[i + dataOffset] = inputdata[i];
        }
    }
}
