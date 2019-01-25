import java.io.IOException;
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
		//controlla se esiste un utente di nome username
		return (usersOnline.contains(username) || usersOffline.contains(username));
	}
	
	static boolean register(String username, String password) {
		
		if(!checkAll(username)) {
			User u = new User(username, password);
			database.put(username, u);
			usersOffline.add(username);
			return true;
		}
		
		return false;
	}
	
	static int login(String username, String password) {
		
		if(usersOffline.contains(username)) {
			usersOffline.remove(username);
			usersOnline.add(username);
			return 0;
		}
		
		if(usersOnline.contains(username)) {
			return -1;
		}
		
		return -2;
	}

	static boolean disconnect (String username) {
		
		if(usersOnline.contains(username)) {
			usersOnline.remove(username);
			usersOffline.add(username);
			return true;
		}
		
		return false;
	}
	
	static int createDoc(String creator, String docName, int sections) {
		
		User u = database.get(creator);
		
		if(docs.containsKey(docName))
			return -1;
		
		if (!database.containsKey(creator))
			return -2;
		
		Document d = new Document(u, docName, sections);
		docs.put(docName, d);
		database.get(creator).createDocument(docName, sections);
		
		return 0;
	}
	
	
	static int sendInvite(String sender, String receiver, String docName) {
		
		User rec = database.get(receiver);
		User snd = database.get(sender);
		Document d = docs.get(docName);
		
		// se il mittente non è registrato
		if(!checkAll(sender))
			return -1;
		// se il destinatario non è registrato
		if(!checkAll(receiver))
			return -2;
		// se il documento già esiste
		if(docs.containsKey(docName))
			return -3;
		// se il mittente non ha creato il documento
		if(!docs.get(docName).isCreator(snd))
			return -4;
		// se il destinatario è già editor
		if(!database.get(receiver).isEditor(docName))
			return -5;
		
		docs.get(docName).addEditor(database.get(receiver));
		rec.addToInvitedDocs(d);
		
		return 0;
	}
}
