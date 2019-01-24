import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Turing {

	private static Map<String, User> database;
	private static Map<String, Document> docs;
	private static Set<String> usersOnline;
	private static Set<String> usersOffline;
	private static int DEFAULT_PORT = 6789;
	private static ServerSocket welcomeSocket;
	
	public static void main(String[] args) throws IOException {
		
		database = new HashMap<String, User>();	// <username, User>
		docs = new HashMap<String, Document>();	// <docName, docCreator>
		usersOnline = new HashSet<String>();
		usersOffline = new HashSet<String>();
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
		database.put(username, u);
		usersOffline.add(username);
	}
	
	static boolean login(String username, String password) {
		
		if(usersOffline.contains(username)) {
			usersOffline.remove(username);
			usersOnline.add(username);
			return true;
		}
		
		return false;
	}

	static boolean disconnect (String username) {
		
		if(usersOnline.contains(username)) {
			usersOnline.remove(username);
			usersOffline.add(username);
			return true;
		}
		
		return false;
	}
	
	static boolean createDoc(String creator, String docName) {
		
		
		
		return true;
	}
	
	
	static boolean sendInvite(String sender, String receiver, String docName) {
		
		User rec = database.get(receiver);
		User snd = database.get(sender);
		Document d = docs.get(docName);
		
		//se il mittente non è registrato, se il destinatario non è registrato, se il documento già esiste, se il mittente non ha creato il documento o il destinatario è già editor
		if(!checkAll(sender) || !checkAll(receiver) || docs.containsKey(docName) || !docs.get(docName).isCreator(snd) || !database.get(receiver).isEditor(docName))
				return false;
		
		docs.get(docName).addEditor(database.get(receiver));
		rec.addToInvitedDocs(d);
		
		return true;
	}
}
