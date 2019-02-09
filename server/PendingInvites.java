package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PendingInvites extends Thread {
	
	private ServerSocket pendWelcomeSocket;
	private ExecutorService pend_e;
	
	public PendingInvites() throws UnknownHostException, IOException {
		pendWelcomeSocket  = new ServerSocket(Configurations.INVITE_PORT, 0, InetAddress.getByName(null));
		pend_e = Executors.newFixedThreadPool(Configurations.THREADPOOL_EX_THREADS);
	}
	
	public void run() {
		
		while(true) {
			Socket pendConnectionSocket;
			
			while (true) {		//listener
				try {
					pendConnectionSocket = null;
					pendConnectionSocket = pendWelcomeSocket.accept();
					
					if(Configurations.DEBUG)
						System.out.println("PendingInvites: un client ha richiesto il servizio di Live Invite.");
					pend_e.execute(new PendingInviteHandler(pendConnectionSocket));
					
				} catch (IOException e) { e.printStackTrace(); }
			}
		}
	}
}
