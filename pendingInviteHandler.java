import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.Iterator;
import java.util.Set;

public class pendingInviteHandler implements Runnable {

	private String nameServed;
	private Socket pendClientSocket;
	private BufferedReader pendIFC;
	private DataOutputStream pendOTC;
	
	public pendingInviteHandler(Socket s) throws IOException {
		pendClientSocket = s;
		pendIFC = new BufferedReader(new InputStreamReader(pendClientSocket.getInputStream()));
		pendOTC = new DataOutputStream(pendClientSocket.getOutputStream());
	}
	
	@Override
	public void run() {
		
		try {
			nameServed = pendIFC.readLine();
		} catch (IOException e) { e.printStackTrace(); }
		
		boolean running = true;
		
		while(running) {
			try {
				Set<String> tmp = Turing.getInstaInvites(nameServed);
				if(!tmp.isEmpty()) {
					Iterator<String> it = tmp.iterator();
					while(it.hasNext())
						pendOTC.writeBytes(it.next() + '\n');
				}
				Turing.resetInstaInvites(nameServed);
				pendOTC.writeByte('\n');		//quando il client si sconnetterà, 
				//questa writeByte fallirà lanciando una SocketException che setterà running a false per far terminare questo runnable
				//la write non è problematica per gli inviti in quanto controllo che non siano null per stamparli!
			} catch(SocketException e) { running = false;  }
			catch (IOException e1) { e1.printStackTrace(); }
		}
	}
}
