package server;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Turing {

	private static ExecutorService e;
	private static Object updateDB;			//usata per consistenza (synchronized) tra database e docs
	private static Object updateSets;
	private static ConcurrentHashMap<String, User> database;
	private static ConcurrentHashMap<String, Document> docs;
	private static Set<String> usersOnline;
	private static Set<String> usersOffline;
	private static Set<InetAddress> multicastAddresses;
	private static ServerSocket welcomeSocket;
	
	private static void init() throws UnknownHostException, IOException, AlreadyBoundException {
		
		updateDB = new Object();
		updateSets = new Object();
		database = new ConcurrentHashMap<String, User>();	// <username, User>
		docs = new ConcurrentHashMap<String, Document>();	// <docName, Document>
		usersOnline = new HashSet<String>();
		usersOffline = new HashSet<String>();
		multicastAddresses = new HashSet<InetAddress>();
		welcomeSocket  = new ServerSocket(Configurations.DEFAULT_PORT, 0, InetAddress.getByName(null));
		
		e = Executors.newFixedThreadPool(Configurations.THREADPOOL_EX_THREADS);
		
		RegistrationRMI obj = new RegistrationRMI(database, usersOffline, usersOnline);
        RegistrationInterface stub = (RegistrationInterface) UnicastRemoteObject.exportObject(obj, 0);

        Registry registry = LocateRegistry.createRegistry(Configurations.REGISTRATION_PORT);
        registry.bind(RegistrationInterface.SERVICE_NAME, stub);
        
        File dir = new File("Documents/");		//dove vengono salvati i documenti (lato server)
        if(!dir.exists())
        	dir.mkdir();
		
		dir = new File ("Downloads/");			//per il comando Show (lato client)
		if(!dir.exists())
			dir.mkdir();
		
		dir = new File("Editing/");				//per il comando Edit/EndEdit (lato client)
		if(!dir.exists())
			dir.mkdir();
	}
	
	private static void boredInit() throws UnknownHostException {
		User asd = new User("asd", "asd");
		User jkl = new User("jkl", "jkl");
		User iop = new User("iop", "iop");
		
		usersOffline.add("asd");
		usersOffline.add("jkl");
		usersOffline.add("iop");
		database.put("asd", asd);
		database.put("jkl", jkl);
		database.put("iop", iop);
		
		createDoc("asd", "qwe", 5);
		createDoc("asd", "solodiasd", 3);
		createDoc("asd", "qwertyuiop", 6);
		sendInvite("asd", "jkl", "qwe");
		sendInvite("asd", "jkl", "qwertyuiop");
		sendInvite("asd", "iop", "qwertyuiop");
	}
	
	public static void main(String[] args) throws IOException, AlreadyBoundException {
		
		init();
		
		boredInit();
		
		PendingInvites p = new PendingInvites();
		p.start();
		
		Socket connectionSocket;
		
		while (true) {							//turing diventa il listener
			connectionSocket = null;
			connectionSocket = welcomeSocket.accept();
			
			System.out.println("Server: Un client si è connesso.");
			e.execute(new RequestHandler(connectionSocket));
		}
	}
	

	private static boolean checkAll(String username) {
		//controlla se esiste un utente di nome username
		return (usersOnline.contains(username) || usersOffline.contains(username));
	}
	
	static int login(String username, String password) {
		
		synchronized(updateSets) {
			if(usersOffline.contains(username) && database.get(username).checkPassword(password)) {
				usersOffline.remove(username);
				usersOnline.add(username);
				System.out.println("Utente " + username + " si è connesso.");
				return 0;
			}
			
			if(usersOnline.contains(username)) {
				System.err.println("Utente " + username + " ha tentato un doppio login.");
				return -1;
			}
		}
		
		System.err.println("Tentativo di login con credenziali errate.");
		return -2;
	}
	
	static Set<String> getPendingInvites(String username) {
		
		synchronized(updateDB) {
			User u = database.get(username);
			Set<String> tmp = null;
			
			if(u == null) {
				if(Configurations.DEBUG)
					System.err.println("Ecchissei");
				return tmp;
			}
			
			return u.getPendingInvites();
		}
	}
	
	static Set<String> getInstaInvites(String nameServed) {
		Set<String> tmp = null;
		
		synchronized(updateDB) {
			User u = database.get(nameServed);
			
			if(u != null) 
				tmp = u.getInstaInvites();
		}
		
		return tmp;
	}
	
	static void resetInvites(String username) {
		database.get(username).resetPendingInvites();
	}

	static void resetInstaInvites(String username) {
		database.get(username).resetInstaInvites();
	}

	static boolean disconnect (String username) {
		
		synchronized(updateSets) {
			if(usersOnline.contains(username)) {
				usersOnline.remove(username);
				usersOffline.add(username);
				System.out.println("Utente " + username + " si è disconnesso.");
				return true;
			}
		}

		return false;
	}
	
	static int createDoc(String creator, String docName, int sections) {
		
		synchronized(updateDB) {
			if(docs.containsKey(docName)) {
				System.err.println(creator + " ha tentato di creare un duplicato di " + docName);
				return -1;
			}
			
			if (!database.containsKey(creator)) {
				System.err.println(creator + " utente sconosciuto (?)");
				return -2;
			}
			
			// https://www.iana.org/assignments/multicast-addresses/multicast-addresses.xhtml
			InetAddress addr = null;
			String aux;
			
			do {
				aux = "239." + (int)Math.floor(Math.random()*256) + "." + (int)Math.floor(Math.random()*256) + "." + (int)Math.floor(Math.random()*256);
				try {
					addr = InetAddress.getByName(aux);
					if(Configurations.DEBUG && multicastAddresses.contains(addr))
						System.err.println("Questa stampa è solo per vedere se, in sede d'esame, l'aver gestito questa cosa statisticamente impossibile mi farà fare una risata");
				} catch (UnknownHostException e) { e.printStackTrace(); }
			} while(!addr.isMulticastAddress() && multicastAddresses.contains(addr));
			
			multicastAddresses.add(addr);
			Document d = new Document(creator, docName, sections, addr);
			
			docs.put(docName, d);
			database.get(creator).addToEditableDocs(docName);
		}
		
		File dir = new File("Documents/" + docName);
		dir.mkdir();
		
		for(int i = 0; i < sections; i++) {
			File x = new File("Documents/" + docName, docName + i + ".txt");
			try {
				x.createNewFile();
			} catch (IOException e) { e.printStackTrace(); }
		}
		
		System.out.println(creator + " ha creato il documento " + docName + " con " + sections + " sezioni.");
		return 0;
	}
	
	
	static int sendInvite(String sender, String receiver, String docName) {
		
		synchronized(updateDB) {
			User rec = database.get(receiver);
			
			// se il mittente non è registrato
			if(!checkAll(sender)) {
				System.err.println(sender + " non registrato.");
				return -1;
			}
			// se il destinatario non è registrato
			if(!checkAll(receiver)) {
				System.err.println(receiver + " non registrato.");
				return -2;
			}
			// se il documento non esiste
			if(!docs.containsKey(docName)) {
				System.err.println(docName + " non è un documento esistente.");
				return -3;
			}
			// se il mittente non ha creato il documento
			if(!docs.get(docName).isCreator(sender)) {
				System.err.println(sender + " non può invitare al documento " + docName + " in quanto non è creatore.");
				return -4;
			}
			// se il destinatario è già editor
			if(database.get(receiver).isEditor(docName)) {
				System.err.println(receiver + " è già editor di " + docName);
				return -5;
			}
			
			if(usersOnline.contains(receiver)) {		//l'utente è online, scrivo immediatamente
				docs.get(docName).addEditor(receiver);
				rec.addInstaInvites(docName);
				rec.addToEditableDocs(docName);
				System.out.println(sender + " ha invitato (live)  " + receiver + " come editor del documento " + docName);
			}
			
			else {										//l'utente è offline, salvo
				docs.get(docName).addEditor(receiver);
				rec.addPendingInvite(docName);
				rec.addToEditableDocs(docName);
				System.out.println(sender + " ha invitato (pending) " + receiver + " come editor del documento " + docName);
			}
		}
		return 0;
	}
	
	static String getDocs(String username) {
		
		String res = null;
		synchronized(updateDB) {
			User u = database.get(username);
			Object[] uDocs = u.getDocs().toArray();
			
			for(int i = 0; i < uDocs.length; i++) {
				
				Document d = docs.get(uDocs[i]);
				String edtr = d.getEditors();
				if(edtr == null)
					edtr = "Nessuno.";			//evita di mostrare "null" nel risultato del Pannello del client
				
				if(res == null) 
					res = "Nome documento: " + d.getName() +"\nCreatore: " + d.getCreator() + "\nCollaboratori: " + edtr;
				else
					res = res + "\nNome documento: " + d.getName() +"\nCreatore: " + d.getCreator() + "\nCollaboratori: " + edtr;
				res = res + '\n';		//per dare spazio tra info di un doc ed un altro
			}
		}
		res = res + '\n';
		return res;
	}
	
	static String editDoc(String username, String docName, int section, SocketChannel clientChannel) throws IOException {
		
		Document d = null;
		String res = null;
		
		synchronized(updateDB) {
			d = docs.get(docName);
			
			if(database.get(username) == null || d == null) {
				System.err.println("User non esistente || Documento inesistente");
				return "NULL";
			}
			
			if(!d.isCreator(username) && !d.isEditor(username)) {
				System.err.println(username + " non può modificare questo documento.");
				return "UNABLE";
			}
			
			if(d.isLocked(section)) {
				System.err.println("Qualcuno sta già lavorando sulla sezione " + section + " di " + docName);
				return "LOCK";
			}
			
			if(!d.editSection(section))
				return "TRYLOCK";
			
			if(d.locks.size() <= section)
				return "OOB";
			
			FileChannel inChannel = FileChannel.open(Paths.get("Documents/" + docName + "/" + docName + section + ".txt"), StandardOpenOption.READ);
			ByteBuffer buffer = ByteBuffer.allocateDirect(1024*1024);
			
			boolean stop = false;
			
			while (!stop) { 
				int bytesRead=inChannel.read(buffer);
				if (bytesRead==-1) {
					stop=true;
				}
				buffer.flip();
				while (buffer.hasRemaining())
					clientChannel.write(buffer);
				buffer.clear();
			}
			clientChannel.close();
			inChannel.close(); 
			res = d.getAddr().toString();
		}
		
		System.out.println(username + " sta ora modificando la sezione " + section + " di " + docName);
		
		res = (String) res.subSequence(1, res.length());			//l'indirizzo ha uno / iniziale, lo tolgo
		return res;
	}

	static String endEdit(String username, String docName, int section, SocketChannel clientChannel) throws IOException {
		
		Document d = null;
		synchronized(updateDB) {
			d = docs.get(docName);
			
			File x = new File("Documents/" + docName, docName + section + ".txt");
			if(x.exists())
				x.delete();
			x.createNewFile();
			FileChannel outChannel = FileChannel.open(Paths.get("Documents/" + docName + "/" + docName + section + ".txt"),	StandardOpenOption.WRITE);
			ByteBuffer buffer = ByteBuffer.allocateDirect(1024*1024);
			
			boolean stop=false;
			
			while (!stop) { 
				int bytesRead=clientChannel.read(buffer);
				if (bytesRead==-1) {
					stop=true;
				}
				buffer.flip();
				while (buffer.hasRemaining())
					outChannel.write(buffer);
				buffer.clear();
			}
			clientChannel.close(); 
			outChannel.close();
			
			d.unlockSection(section);
			
		}
		return "SUCCESS";
	}

	static int getFile(String username, String docName, int section, SocketChannel clientChannel) throws IOException {
		int res = 0;
		
		synchronized(updateDB) {
			
			if(!docs.containsKey(docName))
				return -1;
			else if(docs.get(docName).isLocked(section))
				res = 1;
				
			FileChannel inChannel = FileChannel.open(Paths.get("Documents/" + docName + "/" + docName + section + ".txt"), StandardOpenOption.READ);
			ByteBuffer buffer = ByteBuffer.allocateDirect(1024*1024);
			boolean stop = false;
			
			while (!stop) { 
				int bytesRead=inChannel.read(buffer);
				if (bytesRead==-1) {
					stop=true;
				}
				buffer.flip();
				while (buffer.hasRemaining())
					clientChannel.write(buffer);
				buffer.clear();
			}
			clientChannel.close();
			inChannel.close(); 
		}
		return res;
	}

	static int getDocument(String username, String docName, SocketChannel clientChannel) throws IOException {
		int res = 0;
		
		synchronized(updateDB) {
			
			if(!docs.containsKey(docName))
				return -1;
			
			Document d = docs.get(docName);
			for(int i = 0; i < d.locks.size(); i++) {
				if(d.isLocked(i)) {
					res = 1;
					break;
				}
			}
			
			ByteBuffer buffer = ByteBuffer.allocateDirect(1024*1024);
			
			for(int i = 0; i < d.locks.size(); i++) {
				
				FileChannel inChannel = FileChannel.open(Paths.get("Documents/" + docName + "/" + docName + i + ".txt"), StandardOpenOption.READ);
				boolean stop = false;
				
				while (!stop) { 
					int bytesRead=inChannel.read(buffer);
					if (bytesRead==-1) {
						stop=true;
					}
					buffer.flip();
					while (buffer.hasRemaining())
						clientChannel.write(buffer);
					buffer.clear();
				}
				inChannel.close(); 
			}
			clientChannel.close();
		}
		return res;
	}

	public static void unlock(String nameServed, String docServed, int sectionDoc) {

		synchronized(updateDB) {
			Document d = docs.get(docServed);
			d.locks.get(sectionDoc).unlock();
		}
		
	}
}
