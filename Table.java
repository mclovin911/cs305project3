import java.util.HashMap;

/*
 * forwarding table
 * Contains a map between Address to Address,
 * The first address is the destination address and the second is the next hop address
 * A router can look up for the next hop in the table
 */

public class Table {

    private HashMap<Address, Address> table; // the first string is the desired dest, second is the router to send to

    /**
     * Constructor
     */
    public Table() {
        table = new HashMap<Address, Address>();
    }
    /**
     * Put a entry into the table
     * @param destination address
     * @param next hop address
     */
    public void put(Address dest, Address next) {
        table.put(dest, next);
    }
    /**
     * replace a entry in the table
     * @param key
     * @param value
     */
    public void replace(Address key, Address value){
        table.replace(key, value);
    }
    /**
     * look up for the next hop addr
     * @param destination addr
     * @return the next hop addr
     */

    public Address lookup(Address key){
        //System.out.println("key is: " + key.toString());
        //System.out.println("table is: " + table.toString());
        if(table.containsKey(key)){
            return table.get(key);
        }else{
            System.out.println("invalid address to lookup, not contained in the table");
            return null;
        }
    }
    /**
     * Check if the Table contains the destination Address
     * @param dest addr
     * @return if it contains
     */
    public boolean containsKey(Address key){
        return table.containsKey(key);
    }

}