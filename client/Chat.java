package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;

import javax.swing.JTextArea;

import server.Configurations;

public class Chat extends Thread {
	
	private JTextArea chat;
	private MulticastSocket socket;
	private InetAddress group;
	private boolean running;
	private String username;
	
	public Chat(String u, JTextArea c, MulticastSocket s, InetAddress g) {
		chat = c;
		socket = s;
		group = g;
		username = u;
		running = true;
	}
	
	private void connectAlert(String msg) throws IOException {
		if(msg.length() > 0) {
			//crypt(msg)			se avanza tempo
			byte[] m = msg.getBytes();
			DatagramPacket packet = new DatagramPacket(m, m.length, group, Configurations.MULTICAST_PORT);
			socket.send(packet);
		}
	}

	public void run() {
		byte[] buffer = new byte[1024];
		DatagramPacket packet;
		
		try {
			connectAlert(username + " si è connesso.");
		
			socket.setSoTimeout(Configurations.TIMEOUT);
			socket.joinGroup(group);
			
			while(running) {
				packet = null;
				try {
					packet = new DatagramPacket(buffer, buffer.length);
					socket.receive(packet);
				
					String msg = new String(packet.getData(), packet.getOffset(), packet.getLength());
					//msg = decrypt(msg);						se avanza tempo
					chat.append(msg + '\n');
				} catch(SocketTimeoutException e) { ; }			//ignoro timeout
			}
		
		} catch (IOException e1) { e1.printStackTrace(); }
	}

	public void disable() {
		running = false;
	}

}
