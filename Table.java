import java.util.HashMap;

/*
 * forwarding table
 */

public class Table {

    private HashMap<Address, Address> table; // the first string is the desired dest, second is the router to send to

    public Table() {
        table = new HashMap<Address, Address>();
    }
    public void put(Address dest, Address next) {
        table.put(dest, next);
    }
    public void replace(Address key, Address value){
        table.replace(key, value);
    }
    
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
    
    public boolean containsKey(Address key){
        return table.containsKey(key);
    }
    
}