import java.util.HashMap;

/*
 * forwarding table
 */

public class Table {

	private HashMap<String, String> table; // the first string is the desired dest, second is the router to send to
	
	public Table() {
		table = new HashMap<String, String>();
	}
	public void put(String dest, String next) {
		table.put(dest, next);
	}
}
