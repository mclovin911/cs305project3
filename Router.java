import java.io.File;
import java.util.*;
import java.io.FileNotFoundException;
import java.lang.Integer;
import java.net.*;

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
    public String getDVString(){
        String s = "";
        Iterator it = DV.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            Address addr = (Address)pair.getKey();
            s = s+addr.getIp()+" "+addr.getPort()+" "+pair.getValue()+"\n";

        }
        return s;
    }

    public ArrayList<Address> getNeighbors(){
        ArrayList<Address> a = new ArrayList<Address>();
        Iterator it = distance.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            Address addr = (Address)pair.getKey();
           a.add(addr);

        }
        return a;
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
            System.out.println("test get String+"+ "\n"+getDVString() );

        } catch (FileNotFoundException e) {
            e.printStackTrace();

        }
        System.out.println("file read ");

    }
    public void putDV(Address senderInfo, HashMap<Address, Integer> tmp) {
        neighborDV.put(senderInfo, tmp);
    }

    public void putInTable(Address dest, Address next) {
        t.put(dest, next);
    }

    public void replaceInTable(Address dest, Address next){
        t.replace(dest, next);
    }

    public static void main(String args[]) {
        System.out.println("asdfasf");
        Router r = new Router("./test.txt", false);
        Router r2 = new Router("./test2.txt", false);
        String s = "127.0.0.1 9877 1" + "\n" + "127.0.0.1 9876 1" + "\n" + "127.0.0.1 9874 1";


        try {
            r.s.send_dv(s.getBytes(), "127.0.0.2", 9877);
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
            broadCastDV(r.getDVString());

            try {
                Thread.sleep(gap);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    private void broadCastDV(String s){
        ArrayList<Address> neighbor = r.getNeighbors();
        for(int i =0; i<neighbor.size();i++){
            try{
            r.s.send_dv(s.getBytes(),neighbor.get(i).getIp(),neighbor.get(i).getPort());
            System.out.println("Send DV to"+ neighbor.get(i).getPort());
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * run distance vector algorithm and update the distance vector
     */
    private void recalcDV() {
        System.out.println("recalcDV");
        //set the distance to itself to 0
        if(r.DV.containsKey(r.getAddr())){
            if(r.DV.get(r.getAddr())!=0){
                r.DV.replace(r.getAddr(), 0);
            }
        }else{
            r.DV.put(r.getAddr(),0);
        }

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
                int newer           = r.DV.get(from) + dist; //the distance go though this  map router
                if(r.DV.containsKey(address)){//if this router is the neighbor
                    int old = r.DV.get(address);   //the old distance
                    if( old > newer){
                        r.DV.replace(address, newer);
                        r.replaceInTable(address, from);
                    }
                }else{
                    r.DV.put(address, newer);
                    r.putInTable(address, from);
                }
                iterator.remove();
            }
            it.remove(); // avoids a ConcurrentModificationException
        }




        //dist(s) = min of all neighbor i{dist(i->s)+dist(i)}

        //this dist to itself is always 0
        //r.pubInTable(r.getAddr().getIp(),r.getAddr().getIp());
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
                    String g = s.nextLine();
                    String i[] = g.split(" ");
                    if(i.length==2){
                    String str = i[0]+" "+i[1];
                    DV.put(new Address(i[0],Integer.parseInt(i[1])),Integer.parseInt(i[2].trim()));}
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