import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class InputFileHelper implements Runnable{

	PipedOutputStream outputStream = null;
	String inputFileName = null; 
	DataInputStream in = null;
	boolean stop = false;
	byte[] tempData = new byte[128];
	
	public PipedOutputStream getPipStream() {
		return outputStream;
	}
	
	public InputFileHelper(PipedInputStream input, String fileName) {
		outputStream = new PipedOutputStream();
		try {
			outputStream.connect(input);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		inputFileName = fileName;
		
		try {
			in = new DataInputStream(new FileInputStream("./"+inputFileName));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void setStop() {
		stop = true;
	}
	
	@Override
	public void run() {
		while (!stop) {
			try {
				int numBytesRead = in.read(tempData, 0, tempData.length);
				if (numBytesRead == -1) {
					setStop();
				}else {
					outputStream.write(tempData, 0, numBytesRead);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			in.close();
			outputStream.flush();
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
