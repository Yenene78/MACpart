public class Timer {
    private long START_TIME;
    private long TIMEOUT;

    // set para(TIMEOUT);
    public void ini(long timeOut){
        this.START_TIME = System.currentTimeMillis();
        this.TIMEOUT = timeOut;
    }

    // cal whether timeout: yes->1; no->0;
    public int run(){
        long curTime = System.currentTimeMillis();
        if(curTime - START_TIME > TIMEOUT){
            return 1;
        }else{
            return 0;
        }
    }
}
