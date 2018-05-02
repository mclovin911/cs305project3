import java.io.File;
import java.util.HashMap;
import java.util.Scanner;
import java.io.FileNotFoundException;
import java.lang.Integer;
import java.net.*;
import java.util.Iterator;
import java.util.Map;

public class Router {

    Socket s;
    private boolean reverse;
    private Address addr;
    private String ip;
    private int port;
    private Table t;
    HashMap<Address, Integer> DV = new HashMap<Address, Integer>(); //this distance vector of this router
    HashMap<Address, Integer> distance = new HashMap<Address, Integer>(); //this distance to neighbor
    HashMap<Address, HashMap<Address, Integer>> neighborDV = new HashMap<Address, HashMap<Address, Integer>>();//the distance vector that this router received

    public Router(String filename, boolean reverse) {

        this.reverse = reverse;
        t= new Table();
        readFile(filename);

        try {
            s = new Socket(ip, port);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(s==null){
            System.out.println("?????");
        }
        updateThread u = new updateThread(this, 10000);
        Thread uT = new Thread(u);
        uT.start();
        receiveThread m = new receiveThread(this);
        Thread mT = new Thread(m);
        mT.start();
        readThread r = new readThread();
        Thread rT = new Thread(r);
        rT.start();

    }
    public boolean isReverse() {
        return reverse;
    }

    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    public Address getAddr() {
        return addr;
    }

    public void setAddr(Address a) {
        this.addr = a;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public HashMap<Address, Integer> getDV() {
        return DV;
    }

    public void setDV(HashMap<Address, Integer> dV) {
        DV = dV;
    }

    public HashMap<Address, Integer> getDistance() {
        return distance;
    }

    public void setDistance(HashMap<Address, Integer> distance) {
        this.distance = distance;
    }

    public HashMap<Address, HashMap<Address, Integer>> getNeighborDV() {
        return neighborDV;
    }

    public void setNeighborDV(HashMap<Address, HashMap<Address, Integer>> neighborDV) {
        this.neighborDV = neighborDV;
    }

    public void readFile(String filename) {

        File f = new File(filename);
        String info = "";
        try {
            Scanner s = new Scanner(f);
            String[] data = s.nextLine().split(" ");
            ip = data[0];
            port = Integer.parseInt(data[1]);
            setAddr(new Address(ip,port));
            int index = 0;
            while (s.hasNextLine()) {

                String[] d = s.nextLine().split(" ");
                info = d[0] + " " + d[1];
                distance.put(new Address(d[0],Integer.parseInt(d[1])), Integer.parseInt(d[2]));
                DV = distance; //initialize the distance vector. (the weight to known neighbor)
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();

        }
        System.out.println("file read ");

    }
    public void putDV(Address senderInfo, HashMap<Address, Integer> tmp) {
        neighborDV.put(senderInfo, tmp);
    }

    public void pubInTable(String dest, String next) {
        t.put(dest, next);
    }

    public static void main(String args[]) {
        System.out.println("asdfasf");
        Router r = new Router("./test.txt", false);
        Router r2 = new Router("./test2.txt", false);
        String s = "127.0.0.1 9877 1" + "\n" + "127.0.0.1 9876 1" + "\n" + "127.0.0.1 9874 1";

        try {
            r.s.send(s.getBytes(), "127.0.0.2", 9877);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class updateThread implements Runnable{

    private long gap;
    Router r; //this router

    public updateThread(Router r, int n){
        gap = n;
        this.r= r;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {

            //System.out.println("update !!!");
            recalcDV();
            try {
                Thread.sleep(gap);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * run distance vector algorithm and update the distance vector
     */
    private void recalcDV() {
        System.out.println("recalcDV");
        Iterator it = r.neighborDV.entrySet().iterator();
        
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            Address from = (Address)pair.getKey(); //from which router
            HashMap<Address, Integer> dv = (HashMap<Address, Integer>)pair.getValue();//the dv from that router
            
            Iterator iterator = dv.entrySet().iterator();
            
            while(it.hasNext()){
               Map.Entry key_value = (Map.Entry)it.next();
               Address address     = (Address)pair.getKey(); //to which router
               Integer dist        = (Integer)pair.getValue(); //the distance
               
               if(r.DV.containsKey(address)){//if this router is the neighbor 
                   int old = r.DV.get(address);   //the old distance
                   int newer = r.DV.get(from) + dist; //the distance go though this  map router
                   if( old > newer){
                       r.DV.replace(address, newer);
                    }                 
                }  
                iterator.remove();
            }
            it.remove(); // avoids a ConcurrentModificationException
        }

        
        
        
        //dist(s) = min of all neighbor i{dist(i->s)+dist(i)}

        //this dist to itself is always 0
        r.pubInTable(r.getAddr().getIp(),r.getAddr().getIp());
        //r.getDV().put();


    }

}

class receiveThread  implements Runnable {

    Router r;

    public receiveThread(Router r) {
        this.r = r;

    }

    @Override
    public void run() {

        DatagramPacket data = null;
        while (!Thread.currentThread().isInterrupted()) {

            try {
                if(r.s!=null)
                    data = r.s.receive();

            } catch (Exception e) {
                e.printStackTrace();
            }
            if (data != null) {
                String info = new String(data.getData());
                Scanner s = new Scanner(info);
                HashMap<Address, Integer> DV;
                DV = new HashMap<Address, Integer>();
                System.out.println("get msg");
                while(s.hasNextLine()){
                    String i[] = s.nextLine().split(" ");
                    String str = i[0]+" "+i[1];
                    DV.put(new Address(i[0],Integer.parseInt(i[1])),Integer.parseInt(i[2].trim()));
                }
                System.out.println(data.getAddress().toString().split("/")[1]);
                r.putDV(new Address(data.getAddress().toString().split("/")[1],data.getPort()), DV); //put the DV sent by the neighbor to the neiborDV.

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