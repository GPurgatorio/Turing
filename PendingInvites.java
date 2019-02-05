import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PendingInvites extends Thread {
	
	private ServerSocket welcomeSocket;
	private final int DEFAULT_PORT;
	private ExecutorService e;
	
	public PendingInvites() throws UnknownHostException, IOException {
		DEFAULT_PORT = 6788;
		welcomeSocket  = new ServerSocket(DEFAULT_PORT, 0, InetAddress.getByName(null));
		e = Executors.newFixedThreadPool(10);
	}
	
	public void run() {
		
		System.out.println("Server di supporto per inviti attivato");
		while(true) {
			Socket connectionSocket;
			
			while (true) {		//listener
				try {
					connectionSocket = null;
					connectionSocket = welcomeSocket.accept();
					
					System.out.println("Un client è ora connesso per gli inviti in diretta.");
					e.execute(new pendingInviteHandler(connectionSocket));
					
				} catch (IOException e) { e.printStackTrace(); }
			}
		}
	}
}
