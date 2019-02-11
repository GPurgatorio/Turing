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

	private String nameServed;				// Nome utente che viene gestito
	private Socket pendClientSocket;		// Socket per la gestione TCP
	private BufferedReader pendIFC;			// Abbreviazione per InFromClient
	private DataOutputStream pendOTC;		// Abbreviazione per OutToClient
	
	
	// Costruttore
	public PendingInviteHandler(Socket s) throws IOException {
		pendClientSocket = s;
		pendIFC = new BufferedReader(new InputStreamReader(pendClientSocket.getInputStream()));
		pendOTC = new DataOutputStream(pendClientSocket.getOutputStream());
	}
	
	
	@Override
	public void run() {
		
		try {
			nameServed = pendIFC.readLine();				// Ricevo quale username monitorare
		} catch (IOException e) { e.printStackTrace(); }
		
		boolean running = true;
		
		while(running) {
			
			if(!Turing.isOnline(nameServed)) {						// Se l'utente è andato offline mi fermo (Graceful Shutdown)
				
				running = false;
				if(Configurations.DEBUG)
					System.err.println("PendingInviteHandler di " + nameServed + ", è andato offline!");
				break;
			}
			
			try {													// Altrimenti prendo il Set usato per gli inviti live
				Set<String> tmp = Turing.getInstaInvites(nameServed);
				if(!tmp.isEmpty()) {								// Se non è vuoto allora invio il contenuto
					Iterator<String> it = tmp.iterator();
					while(it.hasNext())
						pendOTC.writeBytes(it.next() + '\n');
				}
				Turing.resetInstaInvites(nameServed);				// Ed ovvio clear del Set
			} catch(SocketException e) { running = false; }				
			catch (IOException e1) { e1.printStackTrace(); }
		}
		
		try {
			pendClientSocket.close();
		} catch (IOException e) { e.printStackTrace(); }
	}
}
