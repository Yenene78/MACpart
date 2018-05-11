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

public class MAC {
    public Thread threadMAC;
    public Thread TIMER;
    public MAC_Worker MACworker;
    public MACdata txFrame;
    public MACdata rxFrame;

    public static int STATE = 0;  // 0:idle, 1:busy;
    public static long TIMEOUT;
    public static long SLOTTIME;
    public static int RESEND;
    private static int CUR_ID;

    public void ini(){
        MACworker = new MAC_Worker();
        threadMAC = new Thread(MACworker, "MAC_Worker");
    }

    public static void startTimer(){
        long startTime = System.currentTimeMillis();
        while(System.currentTimeMillis()-startTime>=TIMEOUT){
            System.out.println("TIMEOUT: "+CUR_ID);
            //TODO: ask to resend;
            //suppose sender check the flag when sending => send/resend;
            Config.RESEND = 1;
        }
    }

    public int handleTxFrame(){
        int curType = txFrame.getType();
        int dst = txFrame.getDst();
        int src = txFrame.getSrc();
        if(curType == 0){   //DATA;
            //TODO: consider dst && src;
            //sender will not decide the line and directly pass the packs to MAC;
            STATE = 1;
            rxFrame = txFrame;
            TIMER = new Thread(MAC::startTimer, "TIMER");
            //TODO: pass rxFrame->recv;
            TIMER.start();
        }else if(curType == 1) { //ACK;
            TIMER.interrupt();
            STATE = 0;
        }
        return 0;
    }


    class MAC_Worker implements Runnable{
        public void run(){
            while(STATE == 0){
                //TODO: listen to both;
            }
            handleTxFrame();
        }
    }

    public void setTimeout(long timeout){
        TIMEOUT = timeout;
    }

    public void setSlotTime(int slotTime){
        SLOTTIME = slotTime;
    }

    public void setResend(int resend){
        RESEND = resend;
    }


    public void startMAC() {
        threadMAC.start();
    }

    public void stopMAC(){
        threadMAC.stop();
    }
}
