import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.Iterator;
import java.util.Set;

public class pendingInviteHandler implements Runnable {

	private String nameServed = "";
	private Socket clientSocket;
	private BufferedReader inFromClient;
	private DataOutputStream outToClient;
	
	public pendingInviteHandler(Socket s) throws IOException {
		clientSocket = s;
		inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		outToClient = new DataOutputStream(clientSocket.getOutputStream());
	}
	
	@Override
	public void run() {
		
		try {
			do {
				try {
					nameServed = inFromClient.readLine();
				} catch (SocketException e) { ; }
			} while(nameServed.length() == 0);
			
			System.err.println("--->" + nameServed);
			
			Set<String> tmp = Turing.getInstaInvites(nameServed);
			if(!tmp.isEmpty()) {
				Iterator<String> it = tmp.iterator();
				while(it.hasNext()) {
					outToClient.writeBytes(it.next() + '\n');
				}
			}

			outToClient.writeByte('\n');		//notifica la fine dei documenti (N.B. non possono esistere documenti con questo nome!)
			clientSocket.close();
			
		} catch (IOException e) { e.printStackTrace(); }
		catch (NullPointerException e1) { ; }		//nameServed.length() lancia NPE altrimenti, devo ignorarlo
	}
}
