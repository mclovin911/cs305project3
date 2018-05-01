import java.io.File;
import java.util.HashMap;
import java.util.Scanner;
import java.io.FileNotFoundException;
import java.lang.Integer;

public class Router{

    boolean reverse;
    String ip;
    int port;
    HashMap<String, Integer> rs = new HashMap<String, Integer>();

    public Router(String filename, boolean reverse){

        this.reverse = reverse;
        readFile(filename);
        updateThread u= new updateThread(10000);
        Thread uT = new Thread(u);
        uT.start();
        msgThread m = new msgThread();
        Thread mT =new Thread(m);
        mT.start();
        readThread r = new readThread();
        Thread rT = new Thread(r);
        rT.start();



    }

    public void readFile(String filename){

        File f = new File(filename);
        String info = "";
        try{
        Scanner s = new Scanner(f);
        String[] data = s.nextLine().split(" ");
        ip = data[0];
        port = Integer.parseInt(data[1]);
        int index = 0;
        while(s.hasNextLine()){

            String[] d = s.nextLine().split(" ");
            info = d[0]+" "+d[1];
            rs.put(info,Integer.parseInt(d[2]));
        }

        }
        catch (FileNotFoundException e){
            e.printStackTrace();

        }
        System.out.println("file read ");


    }
    public static void main(String args[]){
        System.out.println("asdfasf");
        Router r = new Router("./test.txt",false);
    }
}

class updateThread implements Runnable{

    private long gap;

    public updateThread(int n){
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

class msgThread  implements Runnable{


    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            System.out.println("msg!!");
        }

    }

}

class readThread  implements Runnable{


    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {

            System.out.println("readddd");

        }
    }

}

