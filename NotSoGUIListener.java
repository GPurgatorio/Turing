import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import javax.swing.JOptionPane;

public class NotSoGUIListener extends Thread {

	private String username;
	private Socket pendSocket;
	private BufferedReader pendIFS;
	private DataOutputStream pendOTS;
	private boolean running;
	
	public NotSoGUIListener(Socket pendSocket, String username) {
		this.username = username;
		this.pendSocket = pendSocket;
		running = true;
		
		try {
			pendOTS = new DataOutputStream(pendSocket.getOutputStream());
			pendIFS = new BufferedReader(new InputStreamReader(pendSocket.getInputStream()));
		} catch (IOException e) { e.printStackTrace(); }
	}
	
	public void run() {
		
		try {
			pendOTS.writeBytes(username + '\n');
		} catch (IOException e1) { e1.printStackTrace(); }
		
		do {
			try {
				String instaInvite = pendIFS.readLine();
				if(instaInvite != null && instaInvite.length() > 0)
					JOptionPane.showMessageDialog(null, "Sei appena stato invitato al documento:\n " + instaInvite, "Live Invite", JOptionPane.INFORMATION_MESSAGE);
			} catch (SocketException | SocketTimeoutException e) { ; } 
			catch (IOException e1) {e1.printStackTrace();}
		} while(running);
		
		try {
			pendSocket.close();
		} catch (IOException e) { e.printStackTrace(); }
	}
	
	public void disable() {
		running = false;
	}

	
}
