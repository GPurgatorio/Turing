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
		
		System.out.println("Server di supporto per inviti attivato");
		while(true) {
			Socket pendConnectionSocket;
			
			while (true) {		//listener
				try {
					pendConnectionSocket = null;
					pendConnectionSocket = pendWelcomeSocket.accept();
					
					//System.out.println("Un client è ora connesso per gli inviti in diretta.");
					pend_e.execute(new pendingInviteHandler(pendConnectionSocket));
					
				} catch (IOException e) { e.printStackTrace(); }
			}
		}
	}
}
