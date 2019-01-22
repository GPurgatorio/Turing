import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Turing {

	private Map<User,Document> database;
	private static Set<String> usersOnline;
	private static Set<String> usersOffline;
	private Set<Document> docs;
	private static int DEFAULT_PORT = 6789;
	private static ServerSocket welcomeSocket;
	
	public Turing() throws IOException {
		this.database = new HashMap<User,Document>();
		this.usersOnline = new HashSet<String>();
		this.usersOffline = new HashSet<String>();
		this.docs = new HashSet<Document>();
		this.welcomeSocket  = new ServerSocket(DEFAULT_PORT);
	}
	
	public static void main() throws IOException {
		
		while (true) {
			
			String username, password, answer;
			Socket connectionSocket = welcomeSocket.accept();
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
			
			System.out.println("Server - Waiting for command..");
			String command = inFromClient.readLine();
			System.out.println("Server - Received: " + command);
			
			if(command.equals("login")) {
				System.out.println("Login:");
				
				username = inFromClient.readLine();				
				password = inFromClient.readLine();
	
				if(login(username, password)) 
					answer = "Logged in" + '\n';
				else 
					answer = "ERROR" + '\n';
	
				outToClient.writeBytes(answer);
			}
			
			else if(command.equals("logout")) {
				System.out.println("Logout:");
				
				username = inFromClient.readLine();
				
				if(disconnect(username))
			}
			
			else if(command.equals("invite")) {
				;
			}
			
			else if(command.equals("editDoc")) {
				;
			}
			
		}
		
		//welcomeSocket.close();*/
	}
	
	private boolean checkAll(String username) {
		return (usersOnline.contains(username) || usersOffline.contains(username));
	}
	
	private void register(String username, String password) {
		
		if(checkAll(username))
			System.err.println("Turing - register: Username già registrato");
		
		User u = new User(username, password);
		database.put(u, null);
		usersOffline.add(username);
	}
	
	private static boolean login(String username, String password) {
		
		if(usersOffline.contains(username)) {
			usersOffline.remove(username);
			usersOnline.add(username);
			//database
			return true;
		}
		
		return false;
	}

	private static boolean disconnect (String username) {
		
		if(usersOnline.contains(username)) {
			usersOnline.remove(username);
			usersOffline.add(username);
			//database
			return true;
		}
		
		return false;
	}
	
	
	private boolean sendInvite(User sender, User receiver, Document doc) {
		
		//se il mittente non è registrato, se il destinatario non è registrato, se il documento non esiste,
		if(!checkAll(sender.getUser()) || !checkAll(receiver.getUser()) || !docs.contains(doc) || 
				//se il mittente non ha creato il documento o il destinatario è già editor
				!doc.isCreator(sender) || !doc.isEditor(receiver))
			return false;
		
		doc.addEditor(receiver);
		//database
		return true;
	}
}
