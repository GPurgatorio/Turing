import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Iterator;
import java.util.Set;

public class requestHandler implements Runnable {

	private String nameServed = "";
	private String docServed = "";
	private int sectionDoc;
	private Socket clientSocket;
	private BufferedReader inFromClient;
	private DataOutputStream outToClient;

	
	
	public requestHandler(Socket s) throws IOException {
		clientSocket = s;
		inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		outToClient = new DataOutputStream(clientSocket.getOutputStream());
		sectionDoc = -1;
	}
	
	@Override
	public void run() {
		boolean flag = true;
		
		do {
			try {
				
				String username, password, answer, receiver, docName;
				String command = inFromClient.readLine();
				//System.out.println("Thread - Serving a " + command + " request.");
				
				if(command.equals("login")) {
					
					username = inFromClient.readLine();
					nameServed = username;
					password = inFromClient.readLine();
					answer = "ERROR" + '\n';
					
					System.out.printf("Received: %s %s\n", username, password);
		
					int res = Turing.login(username, password);
					
					if(res == -1)
						answer = "LOGGED_ALRD" + '\n';
					else if(res == -2)
						answer = "UNKNWN_USR" + '\n';
					else
						answer = "SUCCESS" + '\n';
					
					outToClient.writeBytes(answer);
					
					sendPendingInvites();
					
				}
			
			
				else if(command.equals("logout")) {
					
					username = inFromClient.readLine();
					answer = "ERROR" + '\n';

					if(Turing.disconnect(username)) 
						answer = "SUCCESS" + '\n';
					
					outToClient.writeBytes(answer);
				}
				
				else if(command.equals("createDoc")) {
					
					username = inFromClient.readLine();
					docName = inFromClient.readLine();
					int sections = inFromClient.read();
					
					int res = Turing.createDoc(username, docName, sections);
					
					if(res == 0)
						answer = "SUCCESS" + '\n';
					else if (res == -1)
						answer = "DOC_EXISTS" + '\n';
					else if (res == -2)
						answer = "HACKER" + '\n';
					else
						answer = "ERROR" + '\n';
					
					outToClient.writeBytes(answer);
				}
				
				else if(command.equals("invite")) {
					
					username = inFromClient.readLine();
					receiver = inFromClient.readLine();
					docName = inFromClient.readLine();
					
					int res = Turing.sendInvite(username, receiver, docName);
					
					if(res == 0) 	
						answer = "SUCCESS" + '\n';
					else if (res == -1)
						answer = "HACKER" + '\n';
					else if (res == -2)
						answer = "UNKNWN_USR" + '\n';
					else if (res == -3)
						answer = "UNKNWN_DOC" + '\n';
					else if (res == -4)
						answer = "NOT_CREATOR" + '\n';
					else if (res == -5)
						answer = "EDITOR_ALRD" + '\n';
					else
						answer = "ERROR" + '\n';
					
					outToClient.writeBytes(answer);	
				}
				
				else if(command.equals("editDoc")) {
					
					username = inFromClient.readLine();
					docName = inFromClient.readLine();
					int section = inFromClient.read();
					docServed = docName;
					sectionDoc = section;
					
					String res = Turing.editDoc(username, docName, section);
					
					//TODO
					if(res.equals("NULL") || res.equals("UNABLE") || res.equals("LOCK")) 
						outToClient.writeBytes("ERROR" + '\n');
					else 
						outToClient.writeBytes(res + '\n');		//success
					
				}
				
				else if (command.equals("endEdit")) {
					
					username = inFromClient.readLine();
					docName = inFromClient.readLine();
					int section = inFromClient.read();
					
					String res = Turing.endEdit(username, docName, section);
					
					sectionDoc = -1;
					docServed = "";
					outToClient.writeBytes(res + '\n');
				}
				
				else if (command.equals("list")) {
					
					username = inFromClient.readLine();
					
					String res = Turing.getDocs(username);
					
					outToClient.writeBytes(res + '\n');
				}
			}
			catch (Exception e) {
				try {
					clientSocket.close();
					Turing.disconnect(nameServed);
					if(sectionDoc != -1) 			//unlock
						Turing.endEdit(nameServed, docServed, sectionDoc);
					flag = false;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			
		} while(flag);
	}

	private void sendPendingInvites() {
		try {
			
			Set<String> tmp = Turing.getPendingInvites(nameServed);
			
			if(tmp != null) {
				Iterator<String> it = tmp.iterator();
				while(it.hasNext()) {
					String x = it.next();
					outToClient.writeBytes(x + '\n');
				}
			}

			outToClient.writeByte('\n');		//notifica la fine dei documenti (N.B. non possono esistere documenti con questo nome!)
			
			Turing.resetInvites(nameServed);
		
		} catch (IOException e) { e.printStackTrace(); }
		
	}

}
