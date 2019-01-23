import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Turing {

	private static Map<User,Document> database;
	private static Set<String> usersOnline;
	private static Set<String> usersOffline;
	private static Set<Document> docs;
	private static int DEFAULT_PORT = 6789;
	private static ServerSocket welcomeSocket;
	
	public Turing() throws IOException {
		;
	}
	
	public static void main(String[] args) throws IOException {
		
		database = new HashMap<User,Document>();
		usersOnline = new HashSet<String>();
		usersOffline = new HashSet<String>();
		docs = new HashSet<Document>();
		welcomeSocket  = new ServerSocket(DEFAULT_PORT, 0, InetAddress.getByName(null));
		
		ExecutorService e = Executors.newFixedThreadPool(10);
		
		while (true) {
			Socket connectionSocket = null;
			connectionSocket = welcomeSocket.accept();
			
			System.out.println("Server - A client just connected.");
			e.execute(new requestHandler(connectionSocket));
		}
	}
	

	static boolean checkAll(String username) {
		return (usersOnline.contains(username) || usersOffline.contains(username));
	}
	
	static void register(String username, String password) {
		
		User u = new User(username, password);
		database.put(u, null);
		usersOffline.add(username);
	}
	
	static boolean login(String username, String password) {
		
		if(usersOffline.contains(username)) {
			usersOffline.remove(username);
			usersOnline.add(username);
			//database
			return true;
		}
		
		return false;
	}

	static boolean disconnect (String username) {
		
		if(usersOnline.contains(username)) {
			usersOnline.remove(username);
			usersOffline.add(username);
			//database
			return true;
		}
		
		return false;
	}
	
	
	static boolean sendInvite(User sender, User receiver, Document doc) {
		
		//se il mittente non è registrato, se il destinatario non è registrato, se il documento non esiste, se il mittente non ha creato il documento o il destinatario è già editor
		if(!checkAll(sender.getUser()) || !checkAll(receiver.getUser()) || !docs.contains(doc) || !doc.isCreator(sender) || !doc.isEditor(receiver))
				return false;
		
		doc.addEditor(receiver);
		//database
		return true;
	}
}
