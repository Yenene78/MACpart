public class Config {
	/*
	* MAC
	*/
	public static int RESEND = 0;

	/*
	 * PHY 
	 */

	//frame buffer
	public final static int TNBF_MAX_BUFFER_SIZE = 15; //15 bytes
	public final static int TNBF_MAX_BUFFER_SIZE1 = 15+3;
	
	//frame buffer PHY size
	public final static int PHY_PAYLOAD_SIZE = 12; //12 bytes
	public final static int MAC_PAYLOAD_SIZE = 12 + 3;
	
	// Sampling Rate
	public final static int PHY_RX_SAMPLING_RATE = 48000;
	public final static int PHY_TX_SAMPLING_RATE = 48000;
	
	// Carrier Frequency
	public final static int PHY_CARRIER_FREQ = 10000;
	
	// Symbol duration
	public final static int PHY_SYMBOL_RATE = 1000;
	
	//Samples per Symbol
	public final static int PHY_RX_SAMPLES_PER_SYMBOL = PHY_RX_SAMPLING_RATE/PHY_SYMBOL_RATE;
	public final static int PHY_TX_SAMPLES_PER_SYMBOL = PHY_TX_SAMPLING_RATE/PHY_SYMBOL_RATE;
	
	//Samples per byte
	public final static int PHY_RX_SAMPLES_PER_BYTE = PHY_RX_SAMPLES_PER_SYMBOL*8;
	
	//Preamble Size (Unit: Symbols)
	public final static int PHY_PRE_SIZE = 8;
	public final static int PHY_PRE_FREQ_MIN = 2000;
	public final static int PHY_PRE_FREQ_MAX = 10000;
	
	//Rx dsp buffer size (unit: samples)
	public final static int PHY_RX_SYNC_BF_SIZE = PHY_RX_SAMPLES_PER_SYMBOL*PHY_PRE_SIZE;
	public final static int PHY_RX_DEC_BF_SIZE = PHY_RX_SAMPLES_PER_SYMBOL*PHY_PRE_SIZE;
	
	//Rx dsp state
	public final static int PHY_RX_DSP_STATE_SYNC = 0;
	public final static int PHY_RX_DSP_STATE_DECODE = 1;
	
	
	//read and write buffer size in sample
	public final static int PHY_RX_LINEBUFFER_SIZE = 8; //unit: sample
	public final static int PHY_TX_LINEBUFFER_SIZE = 8; //unit: sample
	
	
	/*
	 * 	  LINE	   Dump.bin
	 *		/\	    /\
	 *		|_______|
	 *			|
	 *			|Sink
	 *		____|____
	 *		/\	    /\
	 *		|		|
	 *	TXDSP		Inject.bin
	 * 
	*/
	
	// Tx DSP output
	public final static int PHY_TX_SINK_OUTPUT_LINE = 1<<0;
	public final static int PHY_TX_SINK_OUTPUT_FILE = 1<<1; //for debug
	public final static String TX_SINK_DUMP_FILE = "PHY_Tx_Dump.bin";
	public final static int PHY_TX_SINK_OUTPUT_DEFAULT = PHY_TX_SINK_OUTPUT_LINE;
	
	// Tx line input
	public final static int PHY_TX_SINK_INPUT_DSP = 0;
	public final static int PHY_TX_SINK_INPUT_FILE = 1; //for debug
	public final static String TX_SINK_INJECT_FILE = "PHY_Tx_Inject.bin";
	public final static int PHY_TX_SINK_INPUT_DEFAULT = PHY_TX_SINK_INPUT_DSP;
	
	/*
	 * 	  RXDSP		Dump.bin
	 *		/\	    /\
	 *		|_______|
	 *			|
	 *			|Source
	 *		____|____
	 *		/\	    /\
	 *		|		|
	 *	LINE		Inject.bin
	 * 
	*/
	
	// Rx Source input
	public final static int PHY_RX_SRC_INPUT_LINE = 0;
	public final static int PHY_RX_SRC_INPUT_FILE = 1; //for debug
	public final static String PHY_RX_SRC_INJECT_FILE = "PHY_Rx_Inject.bin";
	public final static int PHY_RX_SRC_INPUT_DEFAULT = PHY_RX_SRC_INPUT_LINE; 
	
	// Rx Source output
	public final static int PHY_RX_SRC_OUTPUT_DSP = 1<<0;
	public final static int PHY_RX_SRC_OUTPUT_FILE = 1<<1; //for debug
	public final static String PHY_RX_SRC_DUMP_FILE = "PHY_Rx_Dump.bin";	
	public final static int PHY_RX_SRC_OUTPUT_DEFAULT = PHY_RX_SRC_OUTPUT_DSP; 
	
}
