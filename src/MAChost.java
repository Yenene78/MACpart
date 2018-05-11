import java.util.Queue;
import java.util.LinkedList;
import java.lang.Thread;


public class MAChost extends Thread{
    public Queue<MACdata> PACAGES = new LinkedList<MACdata>();
    private int STATE;  // 0: idle; 1: busy;
    private int DELAY;  // Sending delay;
    private int ACK_TIME;
    private long TIMEOUT;   // timeout for timer(ACK);
    private MACdata SEND_BUF;
    private MACdata RECV_BUF;
    public int TOTAL_NUM;  // total num to transmit;
    private int RESEND;

    public void ini(int delay, long timeout, int totalNum){
        this.DELAY = delay;
        this.RESEND = 0;
        this.TIMEOUT = timeout;
        this.TOTAL_NUM = totalNum;
    }

    public void checksum(){
        // TODO: checksum?
    }

    public void run(){
        while(TOTAL_NUM != 0){
            if (STATE == 0) {   // IDLE;
                synchronized (this) {
                    STATE = 1;
                    SEND_BUF = PACAGES.element();
                    int curId = SEND_BUF.getID();
                    // TODO: let Receiver get pac;
                    Timer timer = new Timer();
                    timer.ini(TIMEOUT);
                    while (timer.run() == 0) {
                        if (RECV_BUF.getType() == 1) {    // 1: ACK;
                            TOTAL_NUM --;
                            PACAGES.poll();
                            System.out.println("[Success] Packet ID: " + curId);
                            try {   // Send delay;
                                Thread.sleep(DELAY);
                            }catch (Exception e) {
                                e.printStackTrace();
                            }
                            RESEND = 0;
                        }
                    }
                    System.out.println("[ACK Timeout] Packet ID: " + curId);
                    RESEND ++;
                    if(RESEND > 10){    // drop pac;
                        System.out.println(("[Drop] Packet ID: " + curId));
                        PACAGES.poll();
                        RESEND = 0;
                    }
                    STATE = 0;
                }
            } else if (STATE == 1) {    // BUSY;
                long backOff = (int)(Math.random()*100);
                try{    // wait;
                    Thread.sleep(backOff);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}

