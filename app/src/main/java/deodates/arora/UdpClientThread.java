package deodates.arora;

/**
 * Created by 500050654 on 1/18/2018.
 */

import android.os.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import deodates.arora.MainActivity;

public class UdpClientThread extends Thread{

    String dstAddress;
    int dstPort;
    String messageStr_tx;
    private boolean running;
    MainActivity.UdpClientHandler handler;

    DatagramSocket socket;
    InetAddress address;

    public UdpClientThread(String addr, int port, String messageStr, MainActivity.UdpClientHandler handler) {
        super();
        dstAddress = addr;
        dstPort = port;
        messageStr_tx = messageStr;
        this.handler = handler;
    }

    public void setRunning(boolean running){
        this.running = running;
    }

    private void sendState(String state){
        handler.sendMessage(
                Message.obtain(handler,
                        MainActivity.UdpClientHandler.UPDATE_STATE, state));
    }

    @Override
    public void run() {
        sendState("connecting...");

        running = true;

        try {
            socket = new DatagramSocket();
            address = InetAddress.getByName(dstAddress);

            // send request
            byte[] buf = messageStr_tx.getBytes();

            DatagramPacket packet =
                    new DatagramPacket(buf, buf.length, address, dstPort);
            socket.send(packet);

            sendState("connected");

            /*
            // get response
            packet = new DatagramPacket(buf, buf.length);


            socket.receive(packet);
            //socket.setSoTimeout(10000);
            String line = new String(packet.getData(), 0, packet.getLength());

            handler.sendMessage(
                    Message.obtain(handler, MainActivity.UdpClientHandler.UPDATE_MSG, line));

            */

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(socket != null){
                socket.close();
                handler.sendEmptyMessage(MainActivity.UdpClientHandler.UPDATE_END);
            }
        }

    }
}
