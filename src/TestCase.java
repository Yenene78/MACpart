import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

public class TestCase {
	PipedInputStream fileInputPip;
	InputFileHelper testInputFile;
	Thread threadTestInputFile;
	
	
	public void txFile(PHY phy) {
		PipedInputStream fileInputPip = new PipedInputStream();
		InputFileHelper testInputFile = new InputFileHelper(fileInputPip, "INPUT.bin");
		Thread threadTestInputFile = new Thread (testInputFile, "Main_TestInputFile");
		threadTestInputFile.start();
		int numBytesRead = 0;
		int numBytesReadSum = 0;
		long stamp0;
		long stamp1;
		
		
		byte[] tempData = new byte[Config.PHY_PAYLOAD_SIZE];
		byte tempID = 0;
		stamp0 = System.currentTimeMillis();
		while (true) {
			try {
				numBytesRead = fileInputPip.read(tempData, 0, tempData.length);
				if(numBytesRead <= 0) {
					break;
				}
				numBytesReadSum += numBytesRead;
				System.out.println("***********************************START");
				System.out.println("txFile Receive Bytes: "+numBytesRead);
				
				if (numBytesRead == -1) {
					break;
				}else {
					ToyNetBuffer tnbf = new ToyNetBuffer(tempData, numBytesRead);
					tnbf.setID(tempID);
					tnbf.setLen((byte)(numBytesRead+3));

					MACdata macData = new MACdata(tempData, numBytesRead);
					macData.setID(tempID);
					macData.setLen((byte)(numBytesRead+3+3));
					macData.setDst((byte)1);
					macData.setSrc((byte)0);
					macData.setType((byte)0);	// DATA-type;
					
					if(phy.sendFrame(tnbf)!=-1) {
						System.out.printf("tx done ID: %d\n", tnbf.getID());
						System.out.println("-----------------------------------END");
					}else {
						System.err.println("tx error ");
						break;
					}
					if (numBytesRead != tempData.length) {
						break;
					}
					tempID++;
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		stamp1 = System.currentTimeMillis();
		
		System.out.printf("Tx File DONE! Total Bytes: %d Time: %f s\n", numBytesReadSum, (double)(stamp1-stamp0)/1000);
		
		
		testInputFile.setStop();
        try {
        	threadTestInputFile.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        threadTestInputFile = null;
		try {
			fileInputPip.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		boolean end = true;
		TestCase testCase = new TestCase();
		PHY phy = new PHY();
		phy.init();
		
		
		phy.txWorker.startTx();
		phy.startRx();
		testCase.txFile(phy);
		phy.txWorker.stopTx();
		
		end = true;

		while (!end) {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));  
			String cmd = null;  
	        try {
	            cmd = br.readLine();
	        } catch (IOException e) {
	            e.printStackTrace(); 
	        }
	        if (cmd.equals("stop rx")){
	        	System.out.println("stop rx ...");
	        	phy.stopRx();
	        	end = true;
	        }
	        System.out.println("Thread Num: "+Thread.activeCount());
	        
		}
		
		System.out.println("Thread Num: "+Thread.activeCount());
		System.out.println("BYE");
	}
}  

