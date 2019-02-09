package client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import javax.swing.JOptionPane;

import server.Configurations;

public class NotSoGUIListener extends Thread {

	private String username;			//username di chi ha fatto partire questo listener
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
			pendOTS.writeBytes(username + '\n');		//faccio sapere quale utente deve essere monitorato
		} catch (IOException e) { e.printStackTrace(); }
		
		if(Configurations.DEBUG)
			System.out.println("Live Invite Listener [" + username + "]: attivato!");
		
		do {
			try {
				String instaInvite = pendIFS.readLine();
				if(instaInvite != null && instaInvite.length() > 0)
					JOptionPane.showMessageDialog(null, "Sei appena stato invitato al documento:\n " + instaInvite, "Live Invite", JOptionPane.INFORMATION_MESSAGE);
			} catch (IOException e) { e.printStackTrace(); }
		} while(running);
		
		try {
			pendSocket.close();
		} catch (IOException e) { e.printStackTrace(); }
		
		if(Configurations.DEBUG)
			System.err.println("Live Invite Listener [" + username + "]: disattivato!");
	}
	
	public void disable() {
		running = false;
	}

	
}
