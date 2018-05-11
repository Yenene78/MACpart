import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

public class OutputFileHelper implements Runnable{
	
	PipedInputStream inputStream = null;
	String outputFileName = null; 
	DataOutputStream out = null;
	boolean stop = false;
	byte[] tempData = new byte[128];
	
	public PipedInputStream getPipStream() {
		return inputStream;
	}
	
	public OutputFileHelper(PipedOutputStream output, String fileName) {
		inputStream = new PipedInputStream();
		try {
			inputStream.connect(output);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		outputFileName = fileName;
		
		try {
			out = new DataOutputStream(new FileOutputStream("./"+outputFileName));
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
				inputStream.read(tempData, 0, tempData.length);
				out.write(tempData, 0, tempData.length);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			inputStream.close();
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}