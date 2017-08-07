package tech.pronghorn;

import net.openhft.ticker.ITicker;
import net.openhft.ticker.Ticker;

public class MainTest {
    public static void main(String[] args) {
        int x = 0;
        while(x < 16) {
            int y = 0;
            long total = 0;
            long last = System.nanoTime();
            long pre = System.currentTimeMillis();
            while(y < 58000000){
                y += 1;
                long now = System.nanoTime();
                total += now - last;
                last = now;
            }
            long post = System.currentTimeMillis();
            System.out.println("Took : " + (post - pre) + "ms, " + total + "ns");
//            OtherTestPotato potato = new OtherTestPotato();
//            long totalWork = potato.totalWork;
//            long pre = System.currentTimeMillis();
//            potato.run();
//            long post = System.currentTimeMillis();
//            System.out.println("Took " + (post - pre) + "ms for " + totalWork + ", " + ((totalWork / (post - pre)) * 1000) + " million per second");
            x += 1;
        }
    }
}

class OtherTestPotato {
    int queueCapacity = 1024;
    long totalWork = 10000000L;

    int[] queue = new int[queueCapacity];

    long cons = 0;
    long prod = 0;

    boolean append(int work){
        queue[(int) (prod % queueCapacity)] = work;
        prod += 1;
        return true;
    }

    int getWorkItem() {
        int item = queue[(int) (cons % queueCapacity)];
        cons += 1;
        return item;
    }

    boolean isRunning = true;

    void run() {
        append(1);
        int work = getWorkItem();
        while (isRunning) {
            if (work != 0) {
                process(work);
            } else {
                System.out.println("FUCK");
                System.exit(1);
            }
            work = getWorkItem();
        }
    }

    void process(int work) {
        totalWork -= 1;
        append(1);
        if (totalWork < 1) {
            isRunning = false;
        }
    }
}
