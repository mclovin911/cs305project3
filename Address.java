/**
 * Simulation of Address.
 * Address contains an IP address and a Port number
 * Which represents the IP and Port for the router in out program
 *
 * @Author Jun Zhou, Wei Xingwen
 * @version May.4th.2018
 */
public class Address {
    /**
     * Default Constructor
     */
    public Address() {

    }
    /**
     * Constructor
     * @param ip
     * @param port
     */
    public Address(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }
    private String ip;
    private int port;
    /**
     *
     * @return IP
     */
    public String getIp() {
        return ip;
    }
    /**
     *
     * @param ip
     */
    public void setIp(String ip) {
        this.ip = ip;
    }
    /**
     *
     * @return port number
     */
    public int getPort() {
        return port;
    }
    /**
     *
     * @param port
     */
    public void setPort(int port) {
        this.port = port;
    }
    /**
     * @return the string representation of the Address
     */
    public String toString(){
        return ip+" "+port;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object o){

        return this.hashCode()==o.hashCode();
    }
}