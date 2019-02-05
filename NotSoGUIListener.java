import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import javax.swing.JOptionPane;

public class NotSoGUIListener extends Thread {

	private String username;
	private Socket pendSocket;
	private BufferedReader pendIFS;
	private DataOutputStream pendOTS;
	private boolean running;
	
	public NotSoGUIListener(Socket pendSocket, DataOutputStream pendOTS, BufferedReader pendIFS, String username) {
		this.username = username;
		this.pendSocket = pendSocket;
		this.pendIFS = pendIFS;
		this.pendOTS = pendOTS;
		running = true;
	}
	
	public void run() {
		
		try {
			pendOTS.writeBytes(username);
		} catch (IOException e1) { return; }
		
		do {
			try {
				pendSocket.setSoTimeout(100);
				String instaInvite = pendIFS.readLine();
				JOptionPane.showMessageDialog(null, "Sei stato invitato al documento " + instaInvite, "Invite", JOptionPane.INFORMATION_MESSAGE);
			} catch (IOException e) { ; } 
		} while(running);
	}
	
	public void disable() {
		running = false;
	}

	
}
