import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;


public class PHY {
	//Rx
	Thread threadRxWorker;
	PHYRxWorker rxWorker;
	TargetDataLine rxLine;
	
	Thread threadRxSrcDumpFile;
	OutputFileHelper rxSrcDumpFile;
	PipedOutputStream rxSrcOutputPip;
	
	Thread threadRxSrcInjectFile;
	InputFileHelper rxSrcInjectFile;
	PipedInputStream rxSrcInputPip;
	
	//Tx
	Thread threadTxWorker;
	PHYTxWorker txWorker;
	SourceDataLine txLine;
	
	Thread threadTxSinkDumpFile;
	OutputFileHelper txSinkDumpFile;
	PipedOutputStream txSinkOutputPip;
	
	Thread threadTxSinkInjectFile;
	InputFileHelper txSinkInjectFile;
	PipedInputStream txSinkInputPip;

	Utility utility = new Utility();
	
	String errStr;
	
	public int sendFrame(ToyNetBuffer tnbf) {
		return txWorker.txFrame(tnbf); 
	}
	
	
	public void init() {
		//init Rx line
		initRxLine();
		//init rxWorker
		rxWorker = new PHYRxWorker();
    	rxWorker.setRxSrcInputFlag(Config.PHY_RX_SRC_INPUT_DEFAULT);
    	rxWorker.setRxSrcOutputFlag(Config.PHY_RX_SRC_OUTPUT_DEFAULT);
    	initRxFiles();
    	threadRxWorker = new Thread(rxWorker, "PHY_RxWorker");
    	
		
    	//init Tx line
    	initTxLine();
		
    	//init txWorker
    	txWorker = new PHYTxWorker();
    	txWorker.setTxSinkOutputFlag(Config.PHY_TX_SINK_OUTPUT_DEFAULT);
    	txWorker.setTxSinkInputFlag(Config.PHY_TX_SINK_INPUT_DEFAULT);
    	initTxFiles();
	}
	
	private void initRxFiles() {
		 rxSrcInputPip = new PipedInputStream();
		 rxSrcInjectFile = new InputFileHelper(rxSrcInputPip, Config.PHY_RX_SRC_INJECT_FILE);
		 threadRxSrcInjectFile = new Thread (rxSrcInjectFile, "PHY_RxSrcInjectFile");
	 
		 rxSrcOutputPip = new PipedOutputStream();
		 rxSrcDumpFile = new OutputFileHelper(rxSrcOutputPip, Config.PHY_RX_SRC_DUMP_FILE);
		 threadRxSrcDumpFile = new Thread (rxSrcDumpFile, "PHY_RxSrcDumpFile");
	}
	
	private void initTxFiles() {
		 txSinkInputPip = new PipedInputStream();
		 txSinkInjectFile = new InputFileHelper(txSinkInputPip, Config.TX_SINK_INJECT_FILE);
		 threadTxSinkInjectFile = new Thread (txSinkInjectFile, "PHY_TxSinkInjectFile");
	 
		 txSinkOutputPip = new PipedOutputStream();
		 txSinkDumpFile = new OutputFileHelper(txSinkOutputPip, Config.TX_SINK_DUMP_FILE);
		 threadTxSinkDumpFile = new Thread (txSinkDumpFile, "PHY_TxSinkDumpFile");
	}
	
	private void initRxLine () {
		AudioFormat format = new AudioFormat(
        		Config.PHY_RX_SAMPLING_RATE,// sample rate
        		16, // bits per sample 
        		1, // mono
        		true, // signed integer 
        		true); // bigEndian
        
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                    
        if (!AudioSystem.isLineSupported(info)) {
        	System.err.println("Line matching " + info + " not supported.");
            return;
        }

        // get and open the target data line for capture.

        try {
        	rxLine = (TargetDataLine) AudioSystem.getLine(info);
        	rxLine.open(format, rxLine.getBufferSize());
        	System.out.println("Rx buffer size (Samples): " + rxLine.getBufferSize());
        	System.out.println("Rx Sample Rate: " + rxLine.getFormat().getFrameRate());
        } catch (LineUnavailableException ex) { 
        	System.err.println("Unable to open the line: " + ex.toString());
            return;
        } catch (SecurityException ex) {
        	System.err.println(ex.toString());
            return;
        } catch (Exception ex) { 
        	System.err.println(ex.toString());
            return;
        }
	}
	
	private void initTxLine () {
		AudioFormat format = new AudioFormat(
        		Config.PHY_TX_SAMPLING_RATE,// sample rate
        		16, // bits per sample 
        		1, // mono
        		true, // signed integer 
        		true); // bigEndian
        
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                    
        if (!AudioSystem.isLineSupported(info)) {
        	System.err.println("Line matching " + info + " not supported.");
            return;
        }

        // get and open the source data line.
        try {
        	txLine = (SourceDataLine) AudioSystem.getLine(info);
        	txLine.open(format, txLine.getBufferSize());
        } catch (LineUnavailableException ex) { 
        	System.err.println("Unable to open the line: " + ex.toString());
            return;
        } catch (SecurityException ex) {
        	System.err.println(ex.toString());
            return;
        } catch (Exception ex) { 
        	System.err.println(ex.toString());
            return;
        }
	}	
	
    class PHYTxWorker {
    	int txSinkOutputFlag = 0;
    	int txSinkInputFlag = 0;
    	int[] samples;
    	byte[] buffhw = new byte[2*Config.PHY_TX_LINEBUFFER_SIZE];
    	int bfhwmask = buffhw.length;
    	
    	public int txFrame(ToyNetBuffer tnbf) {
    		
    		System.out.print("tx Buffer: ");
			for (int i=0; i<tnbf.getLen()-1; i++) {
    			System.out.printf("0x%02x ", tnbf.getRawbf()[i]);
    		}
    		System.out.println();
    		
			utility.txcrc8.reset();
			utility.txcrc8.update(tnbf.getRawbf(), 0, tnbf.getLen()-1);
			tnbf.setCRC(utility.txcrc8.getValue());
			System.out.printf("CRC: 0x%x\n", utility.txcrc8.getValue());
			return txDSP(tnbf.getRawbf());
    	}
    	
    	public void startTx() {
    		txLine.start();
            threadTxSinkInjectFile.start();
            threadTxSinkDumpFile.start();
    	}
    	
    	public void stopTx() {
    		txWorkerStop();
    	}
    	
    	/*
    	 * sampling rate 48000kbps
    	 * 1bit -> 48 samples 
    	 * preamble -> 192*2 samples(8bits)
    	 */
    	
    	private int txDSP(byte[] buffer) {
    		
    		int txByteNum = 0;
    		int txByteNum0 = 0;
    		int tempk = 0;
    		
    		if(txSinkInputFlag == Config.PHY_TX_SINK_INPUT_DSP) {
    			
    			//txLine.drain();
    			txByteNum0 = txPreamble();
    			txByteNum = txData(buffer);

    			if ((txByteNum0 == -1) || (txByteNum == -1)) {
    				System.err.println("txDSP error");
    				return -1;
    			}else {
    				System.out.printf("tx DSP: Generatted Byte %d, Should be %d\n", txByteNum0 + txByteNum, (buffer.length+1)*8*48*2);
        			return buffer.length;
    			}
    			
        	}else if (txSinkInputFlag == Config.PHY_TX_SINK_INPUT_FILE) {
        		int txByteCount = 0;
        		int fileReadNum = 0;
        		while (true) {
        			try {
        				fileReadNum = txSinkInputPip.read(buffhw, 0, buffhw.length);
					} catch (IOException e) {
						e.printStackTrace();
						break;
					}
        			if (fileReadNum == -1) {
        				System.out.printf("Inject Raw Wave %d bytes\n", txByteCount);
        				return txByteCount;
        			}else {
        				bfhwmask = fileReadNum;
        				txByteNum = txHW();
        				if(txByteNum != -1) {
        					txByteCount += txByteNum;
        				}else {
        					System.err.println("txDSP File inject error");
        					return -1;
        				}
        			}
        			
        		}
        		
        	}else {
        		System.err.println("Error");
        	}
    		
    		return -1;
    	}
    	
    	private int txData(byte[] data) {

    		int txByteNum = 0;
    		int txByteCount = 0;

    		byte tempData;
    		int tempBit;
    		for (int i=0; i<data.length; i++) {
    			tempData = data[i];
//    			System.out.printf("tx byte: 0x%02x\n", tempData);
    			for (int j=7; j>=0; j--) {
    				if(((tempData>>>j) & 0x01) == 1) {
    					tempBit = 1;
    				}else {
    					tempBit = 0;
    				}
    				txByteNum = txSymbol(tempBit);
    				if (txByteNum == -1) {
    					System.err.println("txData Error");
    					return -1;
    				}
    				txByteCount += txByteNum;
//    				System.out.print(tempBit+" ");
    			}
//    			System.out.println();
    		}
    		return txByteCount;
    	}
    	
    	private int txSymbol(int symbol) {
    		short [] symbolWave;
    		
    		if (symbol == 1) {
    			symbolWave = utility.getcarrierTx();
    		}else {
    			symbolWave = utility.getcarrierTxN();
    		}
    		return txWaveBF(symbolWave);
    	}
    	
    	private int txPreamble() {
    		short[] pre = utility.getpreambleTx();
    		return txWaveBF(pre);
    	}
    	
    	private int txWaveBF (short[] wavebf) {
    		int txByteNum = 0;
    		int txByteCount = 0;
    		for (int i=0; i<wavebf.length; i+=Config.PHY_TX_LINEBUFFER_SIZE) {
    			ByteBuffer.wrap(buffhw).order(ByteOrder.BIG_ENDIAN).asShortBuffer().put(wavebf, i, buffhw.length/2);
    			bfhwmask = buffhw.length;
    			txByteNum = txHW();
    			if (txByteNum == -1) {
    				System.err.println("txHW error");
    				return -1;
    			}
    			txByteCount += txByteNum;
    		}
    		return txByteCount;
    	}
    	
    	private int txHW() {
    		
            if((txSinkOutputFlag & Config.PHY_TX_SINK_OUTPUT_FILE) != 0) {

            	try {
            		txSinkOutputPip.write(buffhw, 0, bfhwmask);
				} catch (IOException e) {
					e.printStackTrace();
				}
            	
            	return bfhwmask;
            }
            if((txSinkOutputFlag & Config.PHY_TX_SINK_OUTPUT_LINE) != 0) {
            	return txLine.write(buffhw, 0, bfhwmask);
            }
            return -1;
        }
    
    	private void txWorkerStop() {
    		txSinkInjectFile.setStop();
            try {
				txSinkOutputPip.flush();
				txSinkOutputPip.close();
				txSinkInputPip.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
            
            txSinkDumpFile.setStop();
            
            try {
				threadTxSinkInjectFile.join();
				threadTxSinkDumpFile.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
            threadTxSinkInjectFile = null;
			threadTxSinkDumpFile = null;
            // we reached the end of the stream.  stop and close the line.
            txLine.flush();
            txLine.stop();
            txLine.close();
    	}
    	
    	public void setTxSinkInputFlag(int set) {
    		txSinkInputFlag = set;
		}
    	
    	public int getTxSinkInputFlag() {
    		return txSinkInputFlag;
    	}

		public void setTxSinkOutputFlag(int set) {
			txSinkOutputFlag = set;
		}
		
		public int getTxSinkOutput() {
    		return txSinkOutputFlag;
    	}
		
    }
	
    
    
    /** 
     * Reads data from the line and pass to the DSP
     */
    class PHYRxWorker implements Runnable {

    	int rxSrcInputFlag = 0;
    	int rxSrcOutputFlag = 0;
    	boolean stop = false;
    	int rxState = Config.PHY_RX_DSP_STATE_SYNC;
    	LinkedList<Integer> syncFIFO = new LinkedList<Integer>();
    	int[] decodebf = new int[Config.PHY_RX_SAMPLES_PER_BYTE];
    	byte[] rxhwbf = new byte[2*Config.PHY_RX_LINEBUFFER_SIZE];
    	int rxhwbfMask = rxhwbf.length;
        short[] rxsamplebf = new short[Config.PHY_RX_LINEBUFFER_SIZE];


    	public int getReceivedSignalStrength() {
    		return receivedPower;
    	}
    	
    	private void resetFIFO(LinkedList<Integer> fifo, int len) {
    		while (!fifo.isEmpty()) {
    			fifo.pop();
    		}
    		for (int i=0; i<len; i++) {
    			fifo.add(0);
    		}
    	}

        public void run() {
            int numBytesRead = 0;
            int numBytesReadSum = 0;
            long stamp0 = 0;
            long stamp1 = 0;
            long stamp2 = 0;
            long temp0 = 0;
            long temp1 = 0;
            long startLineLoc = 0;
            long endLineLoc = 0;
            
            
            System.out.println("Rx Thread Started");
            rxLine.start();
            threadRxSrcInjectFile.start();
            threadRxSrcDumpFile.start();
            
            startLineLoc = rxLine.getLongFramePosition();

            resetFIFO(syncFIFO, Config.PHY_RX_SYNC_BF_SIZE);
            

            while (!stop) {
            	stamp0 = System.currentTimeMillis();
            	if(rxSrcInputFlag == Config.PHY_RX_SRC_INPUT_LINE) {
	                if((numBytesRead = rxLine.read(rxhwbf, 0, rxhwbf.length)) == -1) {	
	                	System.err.println("Line Read Error");
	                    break;
	                }
	                rxhwbfMask = numBytesRead;
	                numBytesReadSum += numBytesRead;
	                endLineLoc = rxLine.getLongFramePosition();
	                
	                if (endLineLoc - startLineLoc - numBytesReadSum/2 > 10000) {
	                	//System.err.println("Overflow");
	                }
            	}else if (rxSrcInputFlag == Config.PHY_RX_SRC_INPUT_FILE) {
            		try {
						numBytesRead = rxSrcInputPip.read(rxhwbf, 0, rxhwbf.length);
						
					} catch (IOException e) {
						e.printStackTrace();
						break;
					}
            		if (numBytesRead == -1) {
        				System.out.printf("Rx Inject Raw Wave %d bytes\n", numBytesReadSum);
        				break;
            		}else {
            			rxhwbfMask = numBytesRead;
            			numBytesReadSum += numBytesRead;
            		}
            	}else {
            		System.err.println("Unknown Error");
            	}
                
                
                stamp1 = System.currentTimeMillis();
                
                //DSP
                if((rxSrcOutputFlag & Config.PHY_RX_SRC_OUTPUT_FILE) != 0) {
                	try {
						rxSrcOutputPip.write(rxhwbf, 0, rxhwbfMask);
					} catch (IOException e) {
						e.printStackTrace();
					}
                }
                if((rxSrcOutputFlag & Config.PHY_RX_SRC_OUTPUT_DSP) != 0) {
                	rxDSP();
                }
                stamp2 = System.currentTimeMillis();
                temp0 += (stamp1 - stamp0);
                temp1 += (stamp2 - stamp1);
            }
            System.out.printf("Received Correct Bytes: %d\n", rxWorker.correctDataCount);
            System.out.println("Rx Thread Stopped. Read Bytes: "+numBytesReadSum);
            System.out.println("Shift in Rx Line: "+(endLineLoc-startLineLoc)*2);
            System.out.println("Rx Read Sample Speed: "+(double)temp0/1000*48000*2/(double)numBytesReadSum);
            System.out.println("Rx DSP Speed: "+(double)temp1/1000*48000*2/(double)numBytesReadSum);
            
            rxWorkerStop();
        }
        
        long currentSampleID=0;
        
        void rxDSP() { 
        	ByteBuffer.wrap(rxhwbf).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get(rxsamplebf);
        	for(int i=0; i<rxhwbfMask/2; i++) {
        		rxProcessSample(rxsamplebf[i]);
        		currentSampleID++;
        	}
        	
        	//int MSB = (int) sampleByte[0];
            //int LSB = (int) sampleByte[1];
            //int sample = MSB << 8 | (255 & LSB);
    	}
        
        int syncPowerShort = 0;
        int syncPowerLong = 0;
    	int receivedPower = 0;
    	int syncPowerShortLocalMax = 0;
    	int startIndex = 0;
    	int currentIndex = 0;
    	int decodebfIndex = 0;
    	int decodeByteCount = 0;
    	int correctDataCount = 0;
    	ToyNetBuffer rxFrame = new ToyNetBuffer();
    	int [] decodedBits = new int [8];
    	byte decodedByte = 0;
    	int goodPhaseshift = -1;
    	
        void rxProcessSample(short sample) {
        	int sampleL = (int)sample;
        	currentIndex ++;
        	
        	receivedPower = receivedPower-receivedPower/64 + sampleL*sampleL/8192/64;
//        	System.out.println(receivedPower);
        	if (rxState == Config.PHY_RX_DSP_STATE_SYNC) {
    			syncFIFO.pop();
    			syncFIFO.add(sampleL);
    			calcShortCorr();
//    			System.out.printf("%d %d\n", syncPowerShort, receivedPower);
    			if ((syncPowerShort > receivedPower) && (syncPowerShort > syncPowerShortLocalMax) && (receivedPower > 1000)) {
//    				System.out.println(syncPowerShort);
    				syncPowerShortLocalMax = syncPowerShort;
    				startIndex = currentIndex;
    			}else if((currentIndex - startIndex == Config.PHY_RX_SYNC_BF_SIZE/2) && (startIndex != 0)){
    				calcLongCorr();
    				if(syncPowerLong > syncPowerShortLocalMax) {
//    					System.out.printf("%d %d\n", syncPowerLong, syncPowerShortLocalMax);
    					
    					//reset dsp sync status
    					resetDSPSync();
    					rxState = Config.PHY_RX_DSP_STATE_DECODE;
    					
    					System.out.println("------------------------Frame Detected! Sample: "+currentSampleID);
    					resetDSPDecode();
    				}else {
    					resetDSPSync();
    					System.out.println("Possible False Detection!");
    					rxState = Config.PHY_RX_DSP_STATE_SYNC;
    				}
    				
    			}else {
    				//System.err.println("rx Processing sample Unknown states");
    			}
    		}else if(rxState == Config.PHY_RX_DSP_STATE_DECODE) {
    			decodebf[decodebfIndex] = sampleL;
    			decodebfIndex++;
    			if (decodebfIndex == decodebf.length) {
    				decodebfIndex = 0;
    				if(goodPhaseshift == -1) {
    					findCarrierPhase();
    				}
    				removeCarrier();
    				getBit();
    				getByte();
    				if (decodeByteCount == 0) {
    					rxFrame.setID(decodedByte);
    					decodeByteCount++;
    				}else if( decodeByteCount == 1) {
    					rxFrame.setLen(decodedByte);
    					//System.out.println(decodedByte);
    					decodeByteCount++;
    					if(rxFrame.getLen() > 15 || rxFrame.getLen() <= 3) {
    						System.out.println("Decode frame len error!!Sample: "+currentSampleID);
    						rxState = Config.PHY_RX_DSP_STATE_SYNC;
    						resetDSPDecode();
    					}
    				}else if(decodeByteCount < rxFrame.getLen()-1){
    					rxFrame.setDatabf(decodedByte, decodeByteCount-2);
    					decodeByteCount++;
    				}else if((decodeByteCount==rxFrame.getLen()-1) && (decodeByteCount-1 <= Config.TNBF_MAX_BUFFER_SIZE)) {
    					//TODO better handler in tnbf, buffer offset etc.
    					rxFrame.setCRC(decodedByte);
    					System.out.println();
    					utility.rxcrc8.reset();
    					utility.rxcrc8.update(rxFrame.getRawbf(), 0, rxFrame.getLen()-1);
    					byte crcValue = utility.rxcrc8.getValue();
    					
    					if(crcValue == rxFrame.getCRC()) {
    						System.out.printf("ID %d \n", rxFrame.getID());
    						System.out.printf("LEN %d \n", rxFrame.getLen());
    						System.out.printf("-----------------------CRC Correct 0x%02x\n", crcValue);
    						decodeByteCount++;
    						correctDataCount += decodeByteCount-3;
    						resetDSPDecode();
    					}else {
    						resetDSPDecode();
    						System.out.printf("-----------------------CRC ERROR !! ID: %d\n", rxFrame.getID());
    					}
						rxState = Config.PHY_RX_DSP_STATE_SYNC;
    				}else {
    					System.out.println("Decode frame unknown error!!");
						rxState = Config.PHY_RX_DSP_STATE_SYNC;
    				}
    			}
    		}
        }
        
        void findCarrierPhase() {
//        	long max = -1;
//        	int maxIndex = -1;
//        	long temp =0 ;
//        	long temp2 = 0;
//        	for (int k=0; k<4; k++) {
//        		for (int i=0; i<Config.PHY_RX_SAMPLES_PER_SYMBOL; i++) {	
//    				temp += temp*(1-1/8)+decodebf[i]*utility.getcarrierRx(k)[i]/8000*(1/8);
//    				temp2 += Math.abs(decodebf[i]*utility.getcarrierRx(k)[i]-temp);
////    				System.out.println(temp + " " +decodebf[i]+" "+utility.getcarrierRx(0)[i]);
//    			}
//        		temp = -temp2;
//				
//        		if (temp > max) {
//        			max = temp;
//        			maxIndex = k;
//        		}
//        		temp = 0;
//        	}
//        	System.out.println("Index: "+maxIndex);
        	goodPhaseshift = 2 ;
        	//TODO 2 is the best. 
		}

		void resetDSPDecode() {
        	decodebfIndex = 0;
        	decodeByteCount = 0;
        	rxFrame.reset();
        	goodPhaseshift = -1;
        }
       
        
        void removeCarrier() {
//        	System.out.println("remove carrier");
        	for (int i=0; i<decodebf.length; i++) {
//        		System.out.printf("%d*%d =%d) ", decodebf[i], utility.getcarrierRx()[i], decodebf[i]*utility.getcarrierRx()[i]/8192);
        		decodebf[i] = decodebf[i]*utility.getcarrierRx(goodPhaseshift)[i]/8192;
        	}
//        	System.out.println();
        }
        void getBit() {
        	for (int i=0; i<decodebf.length; i+=Config.PHY_RX_SAMPLES_PER_SYMBOL) {
        		int tempSum = 0;
        		for (int j=20; j<40; j++) {
        			tempSum = tempSum+decodebf[i+j];
//        			System.out.printf("%d ", decodebf[i+j]);
        		}
//        		System.out.print(": ");
        		if(tempSum>0) {
        			decodedBits[i/Config.PHY_RX_SAMPLES_PER_SYMBOL] = 1;
        		}else {
        			decodedBits[i/Config.PHY_RX_SAMPLES_PER_SYMBOL] = 0;
        		}
        		System.out.printf("%d",decodedBits[i/Config.PHY_RX_SAMPLES_PER_SYMBOL]);
        	}
        	System.out.print(":");
        }
        void getByte() {
        	decodedByte = 0;
        	for (int i=0; i<8; i++) {
        		if(decodedBits[i] == 1) {
        			decodedByte = (byte) (decodedByte+((byte)1<<(7-i)));
        		}
        	}
        	System.out.printf("0x%02x \n",decodedByte);
        }
        
        
        
        void resetDSPSync() {
        	syncPowerShortLocalMax = 0;
        	startIndex = 0;
        	resetFIFO(syncFIFO, Config.PHY_RX_SYNC_BF_SIZE);
        }
        
        void calcLongCorr() {
        	int index = 0;
        	int sum = 0;
        	short [] preamble = utility.getpreambleRx();
        	for (ListIterator<Integer> it=syncFIFO.listIterator(); it.hasNext();) {
        		Integer temp = (Integer) it.next();
        		index++;
        		sum += temp*preamble[index-1]/8192;
        	}
        	syncPowerLong = sum/128+sum/256;
        }
        
        void calcShortCorr() {
        	int index = 0;
        	int sum = 0;
        	short [] preamble = utility.getpreambleRx();
        	for (ListIterator<Integer> it=syncFIFO.listIterator(); it.hasNext();) {
        		Integer temp = (Integer) it.next();
        		index++;
        		if(index > Config.PHY_RX_SYNC_BF_SIZE/2) {
        			sum += temp*preamble[index-1-Config.PHY_RX_SYNC_BF_SIZE/2]/8192;
//        			System.out.printf("%d %d\n",temp, preamble[index-1-Config.PHY_RX_SYNC_BF_SIZE/2]);
        		}
        	}
        	syncPowerShort = sum/64;
        }
        
        
        void rxWorkerStop() {
        	rxSrcInjectFile.setStop();
            try {
				rxSrcOutputPip.flush();
				rxSrcOutputPip.close();
				rxSrcInputPip.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
            
            rxSrcDumpFile.setStop();
            
            try {
            	threadRxSrcInjectFile.join();
            	threadRxSrcDumpFile.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
            threadTxSinkInjectFile = null;
			threadTxSinkDumpFile = null;
			
            // we reached the end of the stream.  stop and close the line.
            rxLine.stop();
            rxLine.close();
            rxLine = null;
        }
        
        
        public void setRxSrcInputFlag(int set) {
    		rxSrcInputFlag = set;
    	}
    	
    	public int getRxSrcInputFlag() {
    		return rxSrcInputFlag;
    	}
    	
    	public void setRxSrcOutputFlag(int set) {
    		rxSrcOutputFlag = set;
    	}

    	public int getRxSrcOutputFlag() {
    		return rxSrcOutputFlag;
    	}
    	
		public void setStop() {
			stop = true;
		}

		void RxDSP(byte [] sampleByte) {
			int MSB = (int) sampleByte[0];
		    int LSB = (int) sampleByte[1];
		    int sample = MSB << 8 | (255 & LSB);
		}
    }
    
    public void startRx() {
    	threadRxWorker.start();
    }
    
    public void stopRx() {
    	rxWorker.setStop();
    	try {
			threadRxWorker.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	threadRxWorker = null;
    }
    
}

