package server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.Iterator;
import java.util.Set;

public class PendingInviteHandler implements Runnable {

	private String nameServed;				//nome utente che viene gestito
	private Socket pendClientSocket;
	private BufferedReader pendIFC;			//abbreviazione per InFromClient
	private DataOutputStream pendOTC;		//abbreviazione per OutToClient
	
	public PendingInviteHandler(Socket s) throws IOException {
		pendClientSocket = s;
		pendIFC = new BufferedReader(new InputStreamReader(pendClientSocket.getInputStream()));
		pendOTC = new DataOutputStream(pendClientSocket.getOutputStream());
	}
	
	@Override
	public void run() {
		
		try {
			nameServed = pendIFC.readLine();				//ricevo quale username monitorare
		} catch (IOException e) { e.printStackTrace(); }
		
		boolean running = true;
		
		while(running) {
			
			if(!Turing.isOnline(nameServed)) {				//se è offline mi fermo
				running = false;
				if(Configurations.DEBUG)
					System.err.println("PendingInviteHandler di " + nameServed + ", è andato offline!");
				break;
			}
			try {											//altrimenti prendo il Set usato per gli inviti live
				Set<String> tmp = Turing.getInstaInvites(nameServed);
				if(!tmp.isEmpty()) {						//se non è vuoto allora invio il contenuto
					Iterator<String> it = tmp.iterator();
					while(it.hasNext())
						pendOTC.writeBytes(it.next() + '\n');
				}
				Turing.resetInstaInvites(nameServed);		//e clear del Set
			} catch(SocketException e) { running = false; }				
			catch (IOException e1) { e1.printStackTrace(); }
		}
		
		try {
			pendClientSocket.close();
		} catch (IOException e) { e.printStackTrace(); }
	}
}
