import java.io.File;
import java.util.*;
import java.io.FileNotFoundException;
import java.lang.Integer;
import java.net.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class Router {

    Socket s;
    ReentrantLock lock;
    private boolean reverse;
    private Address addr;
    private String ip;
    private int port;
    private Table t;
    ArrayList<Address> neighbors = new ArrayList<Address>();
    HashMap<Address, Integer> DV = new HashMap<Address, Integer>(); //this distance vector of this router
    //HashMap<Address, Integer> distance = new HashMap<Address, Integer>(); //this distance to neighbor
    HashMap<Address, HashMap<Address, Integer>> neighborDV = new HashMap<Address, HashMap<Address, Integer>>();//the distance vector that this router received

    public Router(String filename, boolean reverse) {

        this.reverse = reverse;
        t= new Table();
        lock = new ReentrantLock();
        readFile(filename);

        try {
            s = new Socket(ip, port);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(s==null){
            System.out.println("?????");
        }
        updateThread u = new updateThread(this, 15000);
        Thread uT = new Thread(u);
        uT.start();
        receiveThread m = new receiveThread(this);
        Thread mT = new Thread(m);
        mT.start();
        readThread r = new readThread(this);
        Thread rT = new Thread(r);
        rT.start();

    }



    public void dropNeighbor(Address a){
        DV.replace(a,99999);
        neighbors.remove(a);
        //distance.remove(a);
    }

    public String getIp(){
        return ip;
    }

    public int getPort(){
        return port;
    }

    public ReentrantLock getLock() {
        return lock;
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


    public void setPort(int port) {
        this.port = port;
    }

    public HashMap<Address, Integer> getDV() {
        return DV;
    }

    public void setDV(HashMap<Address, Integer> dV) {
        DV = dV;
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

    public String getString(HashMap<Address,Integer> HM){
        String s = "";
        Iterator it = HM.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            Address addr = (Address)pair.getKey();
            s = s+addr.getIp()+" "+addr.getPort()+" "+pair.getValue()+"\n";

        }
        return s;
    }

    public ArrayList<Address> getNeighbors(){

        return neighbors;
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
                Address addr = new Address(d[0],Integer.parseInt(d[1]));
                DV.put(addr, Integer.parseInt(d[2]));
                neighbors.add(addr);
                //DV = distance; //initialize the distance vector. (the weight to known neighbor)
                putInTable(addr, addr);
            }
            System.out.println("test get String+"+ "\n"+getDVString() );

        } catch (FileNotFoundException e) {
            e.printStackTrace();

        }
        System.out.println("file read ");

    }

    public void putNeighborDV(Address senderInfo, HashMap<Address, Integer> tmp) {
        neighborDV.put(senderInfo, tmp);
    }

    public void putInTable(Address dest, Address next) {
        if(t.containsKey(dest)){
            t.replace(dest, next);
        }else{
            t.put(dest, next);
        }
    }

    public Address lookup(String dest_ip, int dest_port){
        Address dest_addr = new Address(dest_ip, dest_port);
        Address next_hop = t.lookup(dest_addr);
        return next_hop;
    }

    public void replaceInTable(Address dest, Address next){
        t.replace(dest, next);
    }

    public void send(String data,String dest_ip, int dest_port){
        Address next_hop = lookup(dest_ip, dest_port);
        //System.out.println(next_hop.getPort()); //79
        s.send_msg(data, next_hop.getIp(), next_hop.getPort(), dest_ip, dest_port);
    }


    public static void main(String args[]) {

        Router r = new Router("./test4.txt", true);

        try{
            TimeUnit.SECONDS.sleep(10);
        }catch (Exception e){
        }
    }
}

class updateThread implements Runnable{

    private long gap;
    Router r; //this router
    HashMap<Address, Integer> neighborMissCount;

    public updateThread(Router r, int n){
        gap = n;
        this.r= r;
        neighborMissCount = new HashMap<Address, Integer>();
        for(Address a: r.getNeighbors()){
            neighborMissCount.put(a,0);
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {

            //System.out.println("update !!!");
            //System.out.println(r.getNeighbors().size());
            ArrayList<Address> toDrop = new ArrayList<Address>();
            for(int i =0; i<r.getNeighbors().size();i++) {
                if (!r.getNeighborDV().containsKey(r.getNeighbors().get(i))){
                    Integer oldValue = neighborMissCount.get(r.getNeighbors().get(i));
                    System.out.println(oldValue);
                    if(oldValue ==2){
                        //System.out.println("Receive no DV from "+r.getNeighbors().get(i).toString()+" after 3 update, this neighbor is droped");

                        toDrop.add(r.getNeighbors().get(i));

                    }else{
                    neighborMissCount.replace(r.getNeighbors().get(i),++oldValue);}
                }
            }
            for(Address ad:toDrop){
                r.dropNeighbor(ad);
            }
            toDrop.clear();

            r.neighborDV.clear();
            if(!r.isReverse()){
            broadCastDV(r.getDVString());
            }else{
                applyPoison();
            }


            try {
                Thread.sleep(gap);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void applyPoison(){
        HashMap<Address,Integer> poDV = new HashMap<Address, Integer>();
        Iterator it = r.getDV().entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry key_value = (Map.Entry)it.next();
            Address address     = (Address)key_value.getKey(); //to which router
            Integer dist        = (Integer)key_value.getValue(); //the distance
            if(address==r.getAddr()){
                dist=99999;
            }
            poDV.put(address,dist);
            broadCastDV(r.getString(poDV));

        }


    }

    private void broadCastDV(String s){
        ArrayList<Address> neighbor = r.getNeighbors();
        for(int i =0; i<neighbor.size();i++){
            try{
                r.s.send_dv(s.getBytes(),neighbor.get(i).getIp(),neighbor.get(i).getPort());
                //System.out.println("Send DV to"+ neighbor.get(i).getPort());
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }


}

class receiveThread  implements Runnable {

    Router r;

    public receiveThread(Router r) {
        this.r = r;

    }
    /**
     * run distance vector algorithm and update the distance vector
     */
    private void recalcDV() {
        //System.out.println("recalcDV");
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

            //System.out.println("new dv received from "+from.getIp()+"  "+from.getPort()+ " with the following distances:");

            while(iterator.hasNext()){
                Map.Entry key_value = (Map.Entry)iterator.next();
                Address address     = (Address)key_value.getKey(); //to which router
                Integer dist        = (Integer)key_value.getValue(); //the distance
                //System.out.println(address.getIp().toString() + ": " + address.getPort() + " dist: " + dist);
                int newer           = r.DV.get(from) + dist; //the distance go though this  map router
                if(r.DV.containsKey(address)){//if this router is the neighbor
                    int old = r.DV.get(address);   //the old distance
                    if( old > newer){
                        r.DV.replace(address, newer);
                        r.replaceInTable(address, from);
                        r.putInTable(address, from);
                    }
                }else{
                    r.DV.put(address, newer);
                    r.putInTable(address, from);
                }

            }

        }

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
                //System.out.println("get msg");
                while(s.hasNextLine()){
                    String g = s.nextLine();
                    String i[] = g.split(" ");
                    if(i[0].equals("msg")){
                        String dest_ip = i[1];
                        int dest_port = Integer.parseInt(i[2]);
                        StringBuilder sb = new StringBuilder();
                        for(int j = 3; j < i.length; j++){
                            sb.append(i[j]);
                        }
                        String msg = sb.toString();
                        System.out.println("check for port:" + dest_port + " : " + r.getPort());
                        if(dest_ip.equals(r.getIp())&&dest_port == r.getPort()){

                            System.out.println("Received message: " + msg);
                        }else{
                            System.out.println("redirecting message: " + msg);
                            Address next_hop = r.lookup(dest_ip, dest_port);
                            r.s.send_msg(msg, next_hop.getIp(), next_hop.getPort(), dest_ip, dest_port);
                        }
                    }
                    else if(i[0].equals("CHANGE")){
                        System.out.println("reveice change weight");
                        Address ad = new Address(i[1],Integer.parseInt(i[2]));
                        r.DV.replace(ad,Integer.parseInt(i[3].trim()));
                    }



                    else{
                        if(i.length==3){
                            String str = i[0]+" "+i[1];
                            DV.put(new Address(i[0],Integer.parseInt(i[1])),Integer.parseInt(i[2].trim()));
                        }
                    }
                }
                //System.out.println(data.getAddress().toString().split("/")[1]);
                r.putNeighborDV(new Address(data.getAddress().toString().split("/")[1],data.getPort()), DV); //put the DV sent by the neighbor to the neiborDV.

                recalcDV();
            }

        }

    }

}

class readThread implements Runnable {
    private Router r;

    /**
     * Constructor of readThread
     */
    public readThread(Router r) {
        this.r = r;
    }

    public synchronized void print(){
        System.out.println("Current DV for router " + r.getAddr().toString() + ": " + "\n" + r.getDVString());

        Iterator it = r.neighborDV.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            Address from = (Address) pair.getKey(); //from which router
            HashMap<Address, Integer> dv = (HashMap<Address, Integer>) pair.getValue();//the dv from that router

            Iterator iterator = dv.entrySet().iterator();

            System.out.println("DV received from " + from.toString());
            while (iterator.hasNext()) {
                Map.Entry key_value = (Map.Entry) iterator.next();
                Address address = (Address) key_value.getKey(); //to which router
                Integer dist = (Integer) key_value.getValue(); //the distance
                System.out.println(address.toString()+" "+dist);
            }
        }
    }


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
                            ReentrantLock lock = r.getLock();
                            lock.lock();
                            try {
                                print();
                            } finally {
                                lock.unlock();
                            }
                            s.close();
                            break;
                        }

                        case ("MSG"): {
                            String destIp = s.next();
                            int destPort = Integer.parseInt(s.next());
                            String message = "";
                            if(s.hasNext()){
                                message += s.next();
                            }
                            while(s.hasNext()){
                                message += " " + s.next();
                            }

                            r.send(message, destIp, destPort);
                            break;

                        }
                        case ("CHANGE"): {
                            String destIp = s.next();
                            String destPort = s.next();
                            String weight = s.next();
                            r.DV.replace(new Address(destIp,Integer.parseInt(destPort)),Integer.parseInt(weight));
                            String msg = "CHANGE "+r.getAddr().toString()+" "+weight;
                            r.s.send_change(msg.getBytes(),destIp,Integer.parseInt(destPort));
                            System.out.println("SUC");
                            break;
                        }
                    }
                }

            } catch (Exception e) {
                System.out.println("Error, please try again");
            }
        }
    }

}