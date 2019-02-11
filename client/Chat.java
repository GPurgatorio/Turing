package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;

import javax.swing.JTextArea;

import server.Configurations;

public class Chat extends Thread {
	
	// Chat related
	private boolean running;
	private String username;
	
	// Chat - Chat related
	private JTextArea chat;				// Dove fare l'append dei messaggi ricevuti
	private MulticastSocket socket;
	private InetAddress group;
	
	
	//Costruttore
	public Chat(String u, JTextArea c, MulticastSocket s, InetAddress g) {
		chat = c;
		socket = s;
		group = g;
		username = u;
		running = true;
	}
	
	
	// Viene chiamato per mandare un messaggio di notifica a tutti gli altri utenti che l'user si è connesso
	private void connectAlert(String msg) throws IOException {
		if(msg.length() > 0) {
			byte[] m = msg.getBytes();
			DatagramPacket packet = new DatagramPacket(m, m.length, group, Configurations.MULTICAST_PORT);
			socket.send(packet);
		}
	}

	
	// Fino alla disabilitazione si mette in attesa di messaggi da ricevere ed appendere
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
					chat.append(msg + '\n');
					
				} catch(SocketTimeoutException e) { ; }			// Ignoro timeout
			}
		} catch (IOException e1) { e1.printStackTrace(); }
	}

	
	// Quando il client esce dalla fase di Editing interrompe Chat chiamando questo metodo (Graceful Shutdown)
	public void disable() {
		running = false;
	}
}
