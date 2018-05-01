import java.io.File;
import java.util.HashMap;
import java.util.Scanner;
import java.io.FileNotFoundException;
import java.lang.Integer;
import java.net.*;

public class Router {

    Socket s;
    boolean reverse;
    String ip;
    int port;
    HashMap<String, Integer> rs = new HashMap<String, Integer>();

    public Router(String filename, boolean reverse) {


        this.reverse = reverse;
        readFile(filename);

        try {
            s = new Socket(ip, port);
        } catch (Exception e) {
            e.printStackTrace();
        }
        updateThread u = new updateThread(this, 10000);
        Thread uT = new Thread(u);
        uT.start();
        msgThread m = new msgThread(this);
        Thread mT = new Thread(m);
        mT.start();
        readThread r = new readThread();
        Thread rT = new Thread(r);
        rT.start();


    }

    public void readFile(String filename) {

        File f = new File(filename);
        String info = "";
        try {
            Scanner s = new Scanner(f);
            String[] data = s.nextLine().split(" ");
            ip = data[0];
            port = Integer.parseInt(data[1]);
            int index = 0;
            while (s.hasNextLine()) {

                String[] d = s.nextLine().split(" ");
                info = d[0] + " " + d[1];
                rs.put(info, Integer.parseInt(d[2]));
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();

        }
        System.out.println("file read ");


    }

    public static void main(String args[]) {
        System.out.println("asdfasf");
        Router r = new Router("./test.txt", false);
        Router r2 = new Router("./test2.txt", false);
        String s = "127.0.0.1 9877 1" + "\n" + "127.0.0.1 9876 1" + "\n" + "127.0.0.1 9874 1";

        try {
            //r.s.send(s.getBytes(), "127.0.0.2", 9877);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class updateThread implements Runnable{

    private long gap;

    public updateThread(Router r, int n){
        gap = n;
    }

   @Override
   public void run() {
       while (!Thread.currentThread().isInterrupted()) {

           System.out.println("update !!!");
           try {
               Thread.sleep(gap);
           } catch (InterruptedException e) {
               e.printStackTrace();
           }
       }
   }


}

class msgThread  implements Runnable {

    Router r;

    public msgThread(Router r) {
        this.r = r;

    }

    @Override
    public void run() {

        DatagramPacket data = null;
        while (!Thread.currentThread().isInterrupted()) {

            try {
                data = r.s.receive();

            } catch (Exception e) {
                e.printStackTrace();
            }
            if (data != null) {
                String info = data.getData().toString();
                Scanner s = new Scanner(info);
                HashMap<String, Integer> DV;
                String firstL = s.nextLine();
                System.out.println("get msg");
                while(s.hasNextLine()){
                    String i[] = s.nextLine().split(" ");
                    DV = new HashMap<String, Integer>();
                    String str = i[0]+" "+i[1];
                    DV.put(str,Integer.parseInt(i[2]));

                }
                //generateDV();


            }

        }

    }

}

class readThread implements Runnable {


        @Override
            public void run() {
                Scanner in = new Scanner(System.in);
                System.out.println("Pls give commands\n");
                while (in.hasNextLine()) {
                    try {
                        String command = in.nextLine();
                        Scanner s = new Scanner(command);
                        if (s.hasNext()) {
                            String firstWord = s.next();
                            switch (firstWord) {
                                case ("PRINT"): {

                                    break;
                                }
                                case ("MSG"): {
                                    String destIp = s.next();
                                    String destPort = s.next();
                                    String message = "";
                                    if(s.hasNext()){
                                        message += s.next();
                                    }
                                    while(s.hasNext()){
                                        message += " " + s.next();
                                    }

                                }
                                case ("CHANGE"): {
                                    String destIp = s.next();
                                    String destPort = s.next();
                                    String weight = s.next();
                                }
                            }
                        }

                    } catch (Exception e) {
                        System.out.println("Error, please try again");
                    }
                }
            }

}






