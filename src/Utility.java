
public final class Utility {
	private short[] carrierTx = new short[Config.PHY_RX_SAMPLES_PER_SYMBOL]; 
	private short[] carrierTxN = new short[Config.PHY_RX_SAMPLES_PER_SYMBOL];
	private short[][] carrierRx = new short[4][Config.PHY_RX_SAMPLES_PER_BYTE]; 
	private short[] preambleTx = new short[Config.PHY_PRE_SIZE*Config.PHY_TX_SAMPLES_PER_SYMBOL];
	private short[] preambleRx = new short[Config.PHY_PRE_SIZE*Config.PHY_RX_SAMPLES_PER_SYMBOL];
	
	//crc8
	final static int   CRC_POLYNOM = 0xB2;
    final static byte  CRC_INITIAL = (byte) 0xFF;
    final static boolean CRC_LOOKUPT = true;
    
	public CRC8 txcrc8 = new CRC8();
	public CRC8 rxcrc8 = new CRC8();
	
	public Utility(){
		initUtility();
	}
	
	
	
	private void initUtility() {
		calcSinWave(Config.PHY_TX_SAMPLING_RATE, Config.PHY_CARRIER_FREQ, 0, carrierTx);
		calcSinWave(Config.PHY_RX_SAMPLING_RATE, Config.PHY_CARRIER_FREQ, 0, carrierTxN);
		neg(carrierTxN);
		calcSinWave(Config.PHY_RX_SAMPLING_RATE, Config.PHY_CARRIER_FREQ, 0, carrierRx[0]);
		calcSinWave(Config.PHY_RX_SAMPLING_RATE, Config.PHY_CARRIER_FREQ, Math.PI/4, carrierRx[1]);
		calcSinWave(Config.PHY_RX_SAMPLING_RATE, Config.PHY_CARRIER_FREQ, Math.PI/2, carrierRx[2]);
		calcSinWave(Config.PHY_RX_SAMPLING_RATE, Config.PHY_CARRIER_FREQ, -Math.PI/4, carrierRx[3]);
		
		
		calcPreamble(preambleTx, Config.PHY_TX_SAMPLING_RATE);
		calcPreamble(preambleRx, Config.PHY_RX_SAMPLING_RATE);
	}
	
	private void neg(short[] array) {
		for (int i=0; i<array.length; i++) {
			array[i] = (short) -array[i];
		}
	}
	
	private void calcPreamble(short[] array, int samplingRate){
		double angle = 0;
		double freq = 0;
		double freq_pre = freq;
		int freqMin = Config.PHY_PRE_FREQ_MIN;
		int freqMax = Config.PHY_PRE_FREQ_MAX;
		int halfLength = array.length/2;
		double freqStep = (double)(freqMax-freqMin)/(halfLength-1);
		double timeInt = (double)1/samplingRate;


		if (halfLength != Math.ceil(array.length/2)) {
			System.err.println("Possible Preamble Error");
		}
		
		freq = freqMin;
		freq_pre = freq;
		
//		System.out.println("Preamble: ");	
		for (int i=0;i<array.length;i++) {
			array[i] = (short) (Math.sin(angle)*32767);
			
			freq_pre = freq;
			if(i < array.length/2-1) {
				freq += freqStep;
			}else if(i == array.length/2-1){
				freq += 0;
			}else{
				freq -= freqStep;
			}
			
			angle += 2*Math.PI*(freq+freq_pre)/2*timeInt;
//			System.out.print(" "+array[i]);
		}

		
//		System.out.println();
	}
	
	private void calcSinWave(int samplingRate, int freq, double phase, short[] array) {
		double interval = (double)1/samplingRate;
		for (int i=0; i<array.length; i++) {
			array[i] = (short) (Math.round(Math.sin(10000*2*Math.PI*(interval*i)+phase)*32767));
		}
	}
	
	public static final byte [] int2Bytes(int sample) {
		byte[] sampleByte = new byte[2];
        sampleByte[1] = (byte) (255 & sample);
        sampleByte[0] = (byte) (255 & (sample >> 8));
        return sampleByte;
	}
	
	public static final byte [] short2Bytes(int sample) {
		byte[] sampleByte = new byte[2];
        sampleByte[1] = (byte) (255 & sample);
        sampleByte[0] = (byte) (255 & (sample >> 8));
        return sampleByte;
	}
	
	public short[] getpreambleRx() {
		return preambleRx;
	}
	
	public short[] getpreambleTx() {
		return preambleTx;
	}
	
	public short[] getcarrierTx() {
		return carrierTx;
	}
	
	public short[] getcarrierTxN() {
		return carrierTxN;
	}
	
	public short[] getcarrierRx(int goodOne) {

		return carrierRx[goodOne];
	}
	
	
	/*
	 * https://stackoverflow.com/questions/28877881/java-code-for-crc-calculation?answertab=active#tab-top
	 * http://www.sunshine2k.de/coding/javascript/crc/crc_js.html
	 * 
	 * Calculate CRC8 based on a lookup table.
	 * CRC-8     : CRC-8K/3 (HD=3, 247 bits max)
	 * polynomial: 0xa6 = x^8 + x^6 + x^3 + x^2 + 1  (0x14d) <=> (0xb2; 0x165)
	 * init = 0
	 *
	 * There are two ways to define a CRC, forward or reversed bits.
	 * The implementations of CRCs very frequently use the reversed bits convention,
	 * which this one does. 0xb2 is 0x4d reversed. The other common convention is
	 * to invert all of the bits of the CRC, which avoids a sequence of zeros on
	 * a zero CRC resulting in a zero CRC. The code below does that as well.
	 *
	 * usage:
	 * new Crc8().update("123456789").getHex() == "D8"
	 * new Crc8().update("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ").getHex() == "EF"
	 *
	 */
	class CRC8 {
	    private final byte[]   crcTable = new byte[256];
	    
	    private final boolean useLookupTable;
	    private byte crc8;

	    /**
	     * Construct a CRC8 specifying the polynomial and initial value.
	     * @param polynomial Polynomial, typically one of the POLYNOMIAL_* constants.
	     * @param init Initial value, typically either 0xff or zero.
	     */
	    public CRC8(){
	    	useLookupTable = Utility.CRC_LOOKUPT;
	        for(int i=0; i<256; i++){
	            int rem = i; // remainder from polynomial division
	            for(int j=0; j<8; j++){
	                if((rem & 1) == 1){
	                    rem >>= 1;
	                    rem ^= CRC_POLYNOM;
	                }else {
	                    rem >>= 1;
	                }
	            }
	            crcTable[i] = (byte)rem;
	        }
	        reset();
	    }

	    public void update(byte[] buffer, int offset, int len){
	    	for(int i=offset; i < len; i++){
	            update(buffer[i]);
	        }
	    }
	    
	    public void update(byte[] buffer){
	    	for(int i=0; i < buffer.length; i++){
	            update(buffer[i]);
	        }
	    }

	    public void update(byte b){
	    	if (useLookupTable){
//	    		System.out.printf("debug 0x%02x -> 0x%02x \n", (crc8 ^ b) & 0xFF, crcTable[(crc8 ^ b) & 0xFF]);
	            crc8 = crcTable[(crc8 ^ b) & 0xFF];
	        }else{
	            crc8 ^= b & 0xFF;
	            crc8 &= 0xFF;
	            for(int j=0; j<8; j++){
	                if((crc8 & 1) == 1){
	                    crc8 >>= 1;
	                    crc8 ^= CRC_POLYNOM;
	                }
	                else{
	                    crc8 >>= 1;
	                }
	            }
	        }
	    }

	    public byte getValue(){
	        return (byte) (crc8 ^ 0xff);
	    }

	    public void reset(){
	    	crc8 = CRC_INITIAL;
	    }
	}
}


