import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;


public class Client {
	
	public enum Status {
		REGISTERED,
		LOGGED,
		EDITING
	}
	
	private static BufferedReader inFromUser;
	private static DataOutputStream outToServer;
	private static BufferedReader inFromServer;
	private Socket clientSocket;
	private static String command;
	private static String username;
	private static Status status;

	
	public Client() throws UnknownHostException, IOException {
		inFromUser = new BufferedReader(new InputStreamReader(System.in));
		clientSocket = new Socket("localhost", 6789);
		outToServer = new DataOutputStream(clientSocket.getOutputStream());
		inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	}
	
	private static void login() throws IOException {
		
		System.out.println("Username");
		String username = inFromUser.readLine();
		outToServer.writeBytes(username + '\n');
		
		System.out.println("Password");
		String password = inFromUser.readLine();
		outToServer.writeBytes(password + '\n');
		
		String res = inFromServer.readLine();
		
		if(!res.equals("ERROR")) {
			Client.username = username;
			status = Status.LOGGED;
		}
	}
	
	private static void register() throws IOException {
		
		System.out.println("Username");
		String username = inFromUser.readLine();
		outToServer.writeBytes(username + '\n');
		
		System.out.println("Password");
		String password = inFromUser.readLine();
		outToServer.writeBytes(password + '\n');
		
		String res = inFromServer.readLine();
		if(!res.equals("ERROR"))
			status = Status.REGISTERED;
	}
	
	private static void invite() throws IOException {
		
		System.out.println("Invite: DocName");
		String documentName = inFromUser.readLine();
		outToServer.writeBytes(documentName);
		
		System.out.println("Receiver");
		String receiver = inFromUser.readLine();
		outToServer.writeBytes(receiver);
		
		String res = inFromServer.readLine();
		if(res.equals("ERROR"))
			System.err.println("Errore");
		else
			System.out.printf("%s invitato al Documento %s!\n", receiver, documentName);
	}
	
	private static void show() throws IOException {
		
		System.out.println("Show: DocName [Sezione]");
		String docName = inFromUser.readLine();
		outToServer.writeBytes(docName);
		
		String res = inFromServer.readLine();
		if(res.equals("ERROR"))
			System.err.println("Errore");
		else
			System.out.println("Doc scaricato!");
			//scarica doc xd
	}
	
	private static void createDoc() throws IOException {
		System.out.println("CreateDoc: DocName");
		String documentName = inFromUser.readLine();
		outToServer.writeBytes(documentName);
		
		System.out.println("Section Number");
		String sectionNumber = inFromUser.readLine();
		outToServer.writeBytes(sectionNumber);
		
		String res = inFromServer.readLine();
		if(res.equals("ERROR")) 
			System.err.println("Errore");
		else 
			System.out.println("Documento creato!");
	}
	
	public static void main() throws IOException {
		
		while(true) {
			
			if(status != Status.LOGGED || status != Status.EDITING) {
				System.out.println("Register or Login");
			
				command = inFromUser.readLine();
				
				if(command.equals("Register")) {
					register();
					if(Client.status != Status.REGISTERED)
						System.err.println("Errore nella fase di registrazione");
					else
						System.out.println("Utente registrato. Fai il login!");
				}
				
				else if (command.equals("Login")) {
					login();
					if(Client.status != Status.LOGGED)
						System.err.println("Errore nella fase di login");
					else
						System.out.println("Login avvenuto con successo");
				}
			}
			
			else {
				
				outToServer.writeBytes(Client.username);		//richiesta
				String check = inFromServer.readLine();			//check validità mio ID
				
				if(!check.equals("ERROR")) {
					if(status == Status.LOGGED) {
						System.out.println("CreateDoc, Invite, Show, List or Edit");
						
						command = inFromUser.readLine();
						
						if(command.equals("CreateDoc")) {
							createDoc();
						}
						
						else if(command.equals("Invite")) {
							invite();
						}
						
						else if (command.equals("Show")) {
							show();
						}
						
						else if (command.equals("List")) {
							;
						}
						
						else if (command.equals("Edit")) {
							;
						}
						
						else 
							System.err.println("Comando non riconosciuto");
					}
					
					else if (status == Status.EDITING) {
						System.out.println("EndEdit");
						
						command = inFromUser.readLine();
						
						if(command.equals("EndEdit")) {
							;
						}
						
						else
							System.err.println("Comando non riconosciuto");
					}
					
					else 
						System.err.println("Status non valido");
				}
				
				else 
					System.err.println("Generic error");
			}
		}
	}
}
