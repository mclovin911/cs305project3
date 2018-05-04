import java.io.*;
import java.net.*;

public class Socket{
    DatagramSocket serverSocket;

    InetAddress my_ip;
    String ip;
    int port;

    public Socket(String ip, int port_num)throws Exception{
        this.ip = ip;
        port = port_num;
        my_ip = InetAddress.getByName(ip);
        serverSocket = new DatagramSocket(port_num, my_ip);
    }



    public DatagramPacket receive()throws Exception{
        byte[] receive_data = new byte[1024];
        DatagramPacket receive_packet = new DatagramPacket(receive_data,
                receive_data.length);
        serverSocket.receive(receive_packet);
        //String sentence = new String(receive_packet.getData());
        //InetAddress ip_address = receive_packet.getAddress();
        //int port = receive_packet.getPort();
        return receive_packet;
    }

    public void send_dv(byte[] msg, String ip_address, int port)throws Exception{

        DatagramPacket send_packet = new DatagramPacket(msg, msg.length, InetAddress.getByName(ip_address), port);
        serverSocket.send(send_packet);
    }

    public void send_change(byte[] msg, String ip_address, int port)throws Exception{

        DatagramPacket send_packet = new DatagramPacket(msg, msg.length, InetAddress.getByName(ip_address), port);
        serverSocket.send(send_packet);
    }

    public void send_msg(String send_data, String ip_address, int port, String dest_ip, int dest_port){
        try{
            String suffixed_data = "msg "+dest_ip + " " + dest_port + " " + send_data + " " + this.ip + "-" + this.port;
            byte[] msg = suffixed_data.getBytes();
            DatagramPacket send_packet = new DatagramPacket(msg, msg.length, InetAddress.getByName(ip_address), port);
            serverSocket.send(send_packet);
        }catch (Exception e){
            System.out.println("fail to send msg in socket");
        }
    }
}