import java.io.File;
import java.util.*;
import java.io.FileNotFoundException;
import java.lang.Integer;
import java.net.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Jun Zhou, Xingwen Wei
 *
 *
 *         Router provides algorithm and functions to support the DV routing
 *         algorithm, msg sending and receiving, weight changing and command
 *         reading. A HashMap of Addresses to weight will represent the DV in
 *         this router . There is a HashMap of Addresses to DV(another HashMap)
 *         will store the DV received from neighbor. There is a list of
 *         neighbors
 *
 */
public class Router {

    Boolean Debug = false;
    private long startTime; // record the current time
    private Socket s; // socket for sending and receiving data
    private boolean reverse;// apply poison reverse or not
    private Address addr;// address of this router
    private Table t;// forwarding table of this router
    ArrayList<Address> neighbors = new ArrayList<Address>();// neighbors of this router
    HashMap<Address, Integer> DV = new HashMap<Address, Integer>(); // this distance vector of this router
    HashMap<Address, Integer> distance = new HashMap<Address, Integer>(); //this
    // distance to neighbor
    HashMap<Address, HashMap<Address, Integer>> neighborDV = new HashMap<Address, HashMap<Address, Integer>>();// the
    // distance
    // vector
    // that
    // this
    // router
    // received

    public Router(String filename, boolean reverse) {

        this.reverse = reverse;
        t = new Table();
        readFile(filename); // read the file and initialize this router

        try {
            s = new Socket(getIp(), getPort()); // initialize the socket
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (s == null) {
            System.out.println("Error: Socket is not open");
        }
        // open threads
        startTime = System.currentTimeMillis();
        updateThread u = new updateThread(this, 15000); // n = 15s
        Thread uT = new Thread(u);
        uT.start();
        receiveThread m = new receiveThread(this);
        Thread mT = new Thread(m);
        mT.start();
        readThread r = new readThread(this);
        Thread rT = new Thread(r);
        rT.start();

    }

    public long getTimeStart() {
        return startTime;
    }

    /**
     * drop the neighbor
     *
     * @param address
     */
    public void dropNeighbor(Address a) {

        if (Debug){
            System.out.println("neighbor " + a.toString() + " dropped");}
        DV.replace(a, 99999);
        neighbors.remove(a);
        // distance.remove(a);
    }

    /**
     *
     * @return ip address for this router
     */
    public String getIp() {
        return addr.getIp();
    }

    /**
     *
     * @return port num for this router
     */
    public int getPort() {
        return addr.getPort();
    }

    /**
     * If poison reverse is applied
     *
     * @return reverse
     */
    public boolean isReverse() {
        return reverse;
    }

    /**
     * set the poison reverse
     *
     * @param reverse
     */
    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    /**
     *
     * @return address of this router
     */
    public Address getAddr() {
        return addr;
    }

    /**
     * set address
     *
     * @param address
     *            of this router
     */
    public void setAddr(Address a) {
        this.addr = a;
    }

    /**
     *
     * @return DV
     */
    public HashMap<Address, Integer> getDV() {
        return DV;
    }

    /**
     * set the DV to be a given hashmap
     *
     * @param New
     *            DV
     */
    public void setDV(HashMap<Address, Integer> dV) {
        DV = dV;
    }

    /**
     * get the neighbors DV
     *
     * @return neighbors DV
     */
    public HashMap<Address, HashMap<Address, Integer>> getNeighborDV() {
        return neighborDV;
    }

    /**
     * set the NeiborDV
     *
     * @param neighborDV
     */
    public void setNeighborDV(HashMap<Address, HashMap<Address, Integer>> neighborDV) {
        this.neighborDV = neighborDV;
    }

    /**
     * get the String representation of the DV in this router
     *
     * @return the String
     */
    public String getDVString() {
        String s = ""; // the String
        Iterator it = DV.entrySet().iterator();// Iterate through the DV and store the info in the String

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            Address addr = (Address) pair.getKey();
            s = s + addr.getIp() + " " + addr.getPort() + " " + pair.getValue() + "\n";

        }
        return s;
    }

    /**
     * get the String representation for any given DV
     *
     * @param DV
     * @return the String
     */
    public String getString(HashMap<Address, Integer> HM) {
        String s = "";
        Iterator it = HM.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            Address addr = (Address) pair.getKey();
            s = s + addr.getIp() + " " + addr.getPort() + " " + pair.getValue() + "\n";

        }
        return s;
    }

    /**
     *
     * @return neighbors
     */
    public ArrayList<Address> getNeighbors() {

        return neighbors;
    }

    /**
     * read a given file and initialize the router info
     *
     * @param filename
     */
    public void readFile(String filename) {

        File f = new File(filename);
        String info = "";
        try {
            Scanner s = new Scanner(f);
            String[] data = s.nextLine().split(" ");
            setAddr(new Address(data[0], Integer.parseInt(data[1]))); // initialize the address with the firstline of
            // the file
            while (s.hasNextLine()) {
                String[] d = s.nextLine().split(" ");
                info = d[0] + " " + d[1];
                Address addr = new Address(d[0], Integer.parseInt(d[1]));
                DV.put(addr, Integer.parseInt(d[2]));
                distance.put(addr, Integer.parseInt(d[2]));
                neighbors.add(addr);// get the neighbors
                putInTable(addr, addr);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();

        }

        System.out.println("File read");

    }

    /**
     * put some DV into the neighborDV Called when receive a DV form neighbor
     *
     * @param senderInfo
     * @param DV
     */
    public void putNeighborDV(Address senderInfo, HashMap<Address, Integer> tmp) {
        neighborDV.put(senderInfo, tmp);
    }

    /**
     * put the entry into the forwarding table
     *
     * @param dest
     * @param next
     *            hop
     */

    public void putInTable(Address dest, Address next) {
        if (t.containsKey(dest)) {
            t.replace(dest, next);
        } else {
            t.put(dest, next);
        }
    }

    /**
     * look up in the forwarding table to find the next hop used for msg forwarding
     *
     * @param dest_ip
     * @param dest_port
     * @return next hop address
     */

    public Address lookup(String dest_ip, int dest_port) {
        Address dest_addr = new Address(dest_ip, dest_port);
        Address next_hop = t.lookup(dest_addr);
        return next_hop;
    }

    /**
     * replace some entry in the forwarding table
     *
     * @param dest
     * @param next
     */
    public void replaceInTable(Address dest, Address next) {
        t.replace(dest, next);
    }

    /**
     * send a msg to a given destination
     *
     * @param data
     * @param dest_ip
     * @param dest_port
     */
    public void send(String data, String dest_ip, int dest_port) {
        Address next_hop = lookup(dest_ip, dest_port);
        // System.out.println(next_hop.getPort()); //79
        s.send_msg(data, next_hop.getIp(), next_hop.getPort(), dest_ip, dest_port);
    }

    /**
     * send a DV update data
     *
     * @param DV
     *            data
     * @param dest_ip
     * @param dest_port
     * @throws Exception
     *             in the socket
     */
    public void sendDV(byte[] data, String dest_ip, int dest_port) throws Exception {
        s.send_dv(data, dest_ip, dest_port);
    }

    /**
     *
     * @return socket of this router
     */
    public Socket getSocket() {
        return s;
    }

    public static void main(String args[]) {

        Router r = new Router("./EX1.txt", true);

        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (Exception e) {
        }
    }
}

/*
 * Update thread for a given router
 */
class updateThread implements Runnable {

    private long gap;// gap between any update event
    Router r; // this router
    HashMap<Address, Integer> neighborMissCount;

    public updateThread(Router r, int n) {
        gap = n;
        this.r = r;
        neighborMissCount = new HashMap<Address, Integer>();
        for (Address a : r.getNeighbors()) {
            neighborMissCount.put(a, 0);
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {

            // System.out.println("update !!!");
            // System.out.println(r.getNeighbors().size());
            ArrayList<Address> toDrop = new ArrayList<Address>();
            for (int i = 0; i < r.getNeighbors().size(); i++) {
                if (!r.getNeighborDV().containsKey(r.getNeighbors().get(i))) {
                    Integer oldValue = neighborMissCount.get(r.getNeighbors().get(i));
                    //System.out.println(oldValue);
                    if (oldValue == 2) {
                        // after 3 update, this neighbor is dropped");

                        toDrop.add(r.getNeighbors().get(i));

                    } else {
                        neighborMissCount.replace(r.getNeighbors().get(i), ++oldValue);
                    }
                }
            }
            for (Address ad : toDrop) {
                r.dropNeighbor(ad);
            }
            toDrop.clear();

            r.neighborDV.clear();
            if (r.Debug) {
                System.out.println("Update sent to all neighbors at time"
                        + ((System.currentTimeMillis() - r.getTimeStart()) / 1000) + "(in seconds)");

            }

            if (!r.isReverse()) {
                broadCastDV(r.getDVString());
            } else {
                applyPoison();
            }

            try {
                Thread.sleep(gap);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * apply poison reverse for DV updates
     */
    private void applyPoison() {
        HashMap<Address, Integer> poDV = new HashMap<Address, Integer>();
        Iterator it = r.getDV().entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry key_value = (Map.Entry) it.next();
            Address address = (Address) key_value.getKey(); // to which router
            Integer dist = (Integer) key_value.getValue(); // the distance
            if (address == r.getAddr()) {
                dist = 99999;
            }
            poDV.put(address, dist);

            broadCastDV(r.getString(poDV));

        }

    }

    /**
     * boardCast this string
     *
     * @param s
     */
    private void broadCastDV(String s) {
        if (r.Debug){}
            //System.out.println(s);
            ArrayList<Address> neighbor = r.getNeighbors();
            for (int i = 0; i < neighbor.size(); i++) {
                try {
                    r.sendDV(s.getBytes(), neighbor.get(i).getIp(), neighbor.get(i).getPort());
                    // System.out.println("Send DV to"+ neighbor.get(i).getPort());
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }


    }
}

class receiveThread implements Runnable {

    Router r;

    public receiveThread(Router r) {
        this.r = r;

    }

    /**
     * run distance vector algorithm and update the distance vector
     */
    private void recalcDV() {
        if(r.Debug)
            System.out.println("new dv calculated: ");
        // set the distance to itself to 0
        if (r.DV.containsKey(r.getAddr())) {
            if (r.DV.get(r.getAddr()) != 0) {
                r.DV.replace(r.getAddr(), 0);
            }
        } else {
            r.DV.put(r.getAddr(), 0);
        }

        Iterator it = r.neighborDV.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            Address from = (Address) pair.getKey(); // from which router
            HashMap<Address, Integer> dv = (HashMap<Address, Integer>) pair.getValue();// the dv from that router

            Iterator iterator = dv.entrySet().iterator();

            // System.out.println("new dv received from "+from.getIp()+" "+from.getPort()+ "
            // with the following distances:");

            while (iterator.hasNext()) {
                Map.Entry key_value = (Map.Entry) iterator.next();
                Address address = (Address) key_value.getKey(); // to which router
                Integer dist = (Integer) key_value.getValue(); // the distance
                // System.out.println(address.getIp().toString() + ": " + address.getPort() + "
                // dist: " + dist);

                int newer = r.distance.get(from) + dist; // the distance go though this map router
                if (r.DV.containsKey(address)) {// if this router is the neighbor
                    int old = r.DV.get(address); // the old distance
                    if (old > newer) {
                        r.DV.replace(address, newer);
                        r.replaceInTable(address, from);
                        r.putInTable(address, from);

                    }
                } else {
                    r.DV.put(address, newer);
                    //r.putInTable(address, from);

                }


            }

        }

    }

    @Override
    public void run() {

        DatagramPacket data = null;
        while (!Thread.currentThread().isInterrupted()) {

            try {
                if (r.getSocket() != null)
                    data = r.getSocket().receive();

            } catch (Exception e) {
                e.printStackTrace();
            }
            if (data != null) {
                String info = new String(data.getData());
                Scanner s = new Scanner(info);
                HashMap<Address, Integer> DV;
                DV = new HashMap<Address, Integer>();
                if(r.Debug)
                    System.out.println("new dv received from "+ data.getAddress().toString().split("/")[1]+" "+data.getPort()+" with the following distances:");

                while (s.hasNextLine()) {
                    String g = s.nextLine();
                    String i[] = g.split(" ");
                    if (i[0].equals("msg")) {
                        String dest_ip = i[1];
                        int dest_port = Integer.parseInt(i[2]);
                        StringBuilder sb = new StringBuilder();
                        for (int j = 3; j < i.length; j++) {
                            sb.append(i[j]);
                        }
                        String msg = sb.toString();
                        //System.out.println("check for port:" + dest_port + " : " + r.getPort());
                        if (dest_ip.equals(r.getIp()) && dest_port == r.getPort()) {

                            System.out.println("Received message: " + msg);
                        } else {

                            Address next_hop = r.lookup(dest_ip, dest_port);
                            r.getSocket().send_msg(msg, next_hop.getIp(), next_hop.getPort(), dest_ip, dest_port);

                                System.out.println("Message msg from "+ data.getAddress().toString().split("/")[1]+" "+data.getPort() + " to " + dest_ip+" "+dest_port+" forwarded to "+next_hop+" "+msg+" ");
                        }
                    } else if (i[0].equals("CHANGE")) {

                        Address ad = new Address(i[1], Integer.parseInt(i[2]));
                        r.DV.replace(ad, Integer.parseInt(i[3].trim()));
                        r.distance.replace(ad, Integer.parseInt(i[3].trim()));
                        System.out.println(r.getDVString());


                            System.out.println("new weight to neighbor"+ ad.toString()+ " of " +Integer.parseInt(i[3].trim()));
                    }

                    else {
                        if (i.length == 3) {
                            String str = i[0] + " " + i[1];
                            DV.put(new Address(i[0], Integer.parseInt(i[1])), Integer.parseInt(i[2].trim()));
                            if(r.Debug)
                                System.out.println(str);

                        }
                    }
                }
                // System.out.println(data.getAddress().toString().split("/")[1]);
                r.putNeighborDV(new Address(data.getAddress().toString().split("/")[1], data.getPort()), DV);
                // put the DV sent by the neighbor to the neiborDV.


                recalcDV();
                if(r.Debug)System.out.println(r.getDVString());

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

    public synchronized void print() {
        System.out.println("Current DV for router " + r.getAddr().toString() + ": " + "\n" + r.getDVString());

        Iterator it = r.neighborDV.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            Address from = (Address) pair.getKey(); // from which router
            HashMap<Address, Integer> dv = (HashMap<Address, Integer>) pair.getValue();// the dv from that router

            Iterator iterator = dv.entrySet().iterator();

            System.out.println("DV received from " + from.toString());
            while (iterator.hasNext()) {
                Map.Entry key_value = (Map.Entry) iterator.next();
                Address address = (Address) key_value.getKey(); // to which router
                Integer dist = (Integer) key_value.getValue(); // the distance

                System.out.println(address.toString() + " " + dist);
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

                            print();

                            s.close();
                            break;
                        }

                        case ("MSG"): {
                            String destIp = s.next();
                            int destPort = Integer.parseInt(s.next());
                            String message = "";
                            if (s.hasNext()) {
                                message += s.next();
                            }
                            while (s.hasNext()) {
                                message += " " + s.next();
                            }

                            r.send(message, destIp, destPort);
                            break;

                        }
                        case ("CHANGE"): {
                            String destIp = s.next();
                            String destPort = s.next();
                            String weight = s.next();
                            r.distance.replace(new Address(destIp, Integer.parseInt(destPort)), Integer.parseInt(weight));
                            r.DV.replace(new Address(destIp, Integer.parseInt(destPort)), Integer.parseInt(weight));
                            String msg = "CHANGE " + r.getAddr().toString() + " " + weight;
                            r.getSocket().send_change(msg.getBytes(), destIp, Integer.parseInt(destPort));
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