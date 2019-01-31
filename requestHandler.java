import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class requestHandler implements Runnable {

	private String nameServed = "";
	private static Socket clientSocket;
	private static BufferedReader inFromClient;
	private static DataOutputStream outToClient;
	
	
	public requestHandler(Socket s) throws IOException {
		clientSocket = s;
		inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		outToClient = new DataOutputStream(clientSocket.getOutputStream());
	}
	
	@Override
	public void run() {
		boolean flag = true;
		
		do {
			try {
				
				String username, password, answer, receiver, docName;
				String command = inFromClient.readLine();
				System.out.println("Server @Thread - Received: " + command);
				
				if(command.equals("login")) {
					System.out.println("Thread handling: loginRequest:");
					
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
				}
			
			
				else if(command.equals("logout")) {
					System.out.println("Thread handling: logoutRequest:");
					
					username = inFromClient.readLine();
					answer = "ERROR" + '\n';

					if(Turing.disconnect(username)) 
						answer = "SUCCESS" + '\n';
					
					outToClient.writeBytes(answer);
					
				}
				
				else if(command.equals("createDoc")) {
					System.out.println("Thread handling: createDocRequest");
					
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
					System.out.println("Thread handling: inviteRequest");
					
					username = inFromClient.readLine();
					receiver = inFromClient.readLine();
					docName = inFromClient.readLine();
					
					
					int res = Turing.sendInvite(username, receiver, docName);
					
					if(res != 0) 	//controllo errore
						answer = "ERROR: " + res + '\n';
					
					else
						answer = "SUCCESS" + '\n';
					
					outToClient.writeBytes(answer);
					
				}
				
				else if(command.equals("editDoc")) {
					System.out.println("Thread handling: editRequest");
				}
			}
			catch (Exception e) {
				try {
					clientSocket.close();
					Turing.disconnect(nameServed);
					break;
				} catch (IOException e1) {
					System.err.println("Ciaux");
					e1.printStackTrace();
				}
			}
			
		} while(flag);
	}

}
