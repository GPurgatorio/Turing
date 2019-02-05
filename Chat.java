import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import javax.swing.JTextArea;

public class Chat implements Runnable {
	
	private JTextArea chat;
	private MulticastSocket socket;
	private InetAddress group;
	private boolean running;
	
	public Chat(JTextArea c, MulticastSocket s, InetAddress g) {
		chat = c;
		socket = s;
		group = g;
		running = true;
	}

	public void listen() throws IOException {
		byte[] buffer = new byte[1024];
		//socket.setSoTimeout(100);
		socket.joinGroup(group);
		
		while(running) {
			
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			socket.receive(packet);
		
			String msg = new String(packet.getData(), packet.getOffset(), packet.getLength());
			//msg = decrypt(msg);			se avanza tempo
			chat.append(msg + '\n');
			/*
			catch (SocketTimeoutException e) {
				;
			} */

		}
		socket.leaveGroup(group);
	}

	public void disable() {
		running = false;
	}

	@Override
	public void run() {
		try {
			listen();
		} catch (IOException e) { e.printStackTrace(); }
		
	}
}
