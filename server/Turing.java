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
	private static Object updateDB;			
	private static Object updateSets;		
	private static ConcurrentHashMap<String, User> database;
	private static ConcurrentHashMap<String, Document> docs;
	private static Set<String> usersOnline;
	private static Set<String> usersOffline;
	private static Set<InetAddress> multicastAddresses;
	private static ServerSocket welcomeSocket;
	
	// Inizializzazioni varie
	private static void init() throws UnknownHostException, IOException, AlreadyBoundException {
		
		updateDB = new Object();							// Usato per la gestione sincronizzazione tra database e docs 
		updateSets = new Object();							// Usato per la gestione sincronizzazione tra usersOnline ed usersOffline
		database = new ConcurrentHashMap<String, User>();	// <username, User>, database per la gestione di User.java
		docs = new ConcurrentHashMap<String, Document>();	// <docName, Document>, database per la gestione di Document.java
		usersOnline = new HashSet<String>();
		usersOffline = new HashSet<String>();
		multicastAddresses = new HashSet<InetAddress>();	// Salvo gli indirizzi di Multicast già assegnati a documenti esistenti
		welcomeSocket  = new ServerSocket(Configurations.DEFAULT_PORT, 0, InetAddress.getByName(null));
		
		e = Executors.newFixedThreadPool(Configurations.THREADPOOL_EX_THREADS);		// Vedasi relazione per piccolo commento a riguardo
		
		// Classica RMI come abbiamo affrontato durante il corso
		RegistrationRMI obj = new RegistrationRMI(database, usersOffline, usersOnline);
        RegistrationInterface stub = (RegistrationInterface) UnicastRemoteObject.exportObject(obj, 0);

        Registry registry = LocateRegistry.createRegistry(Configurations.REGISTRATION_PORT);
        registry.bind(RegistrationInterface.SERVICE_NAME, stub);
        
        File dir = new File("Documents/");		// Dove vengono salvati i documenti (lato server)
        if(!dir.exists())
        	dir.mkdir();
		
	}
	
	
	// Semplice inizializzazione del database in modo da avere elementi su cui lavorare senza doverli ricreare ogni volta
	private static void boredInit() throws UnknownHostException {
		
		System.err.println("Nomi, docs ed invite di default attivi. Per disattivare, in Configurations.java, BORED_INIT = false.");
		
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
		
		if(Configurations.BORED_INIT)							// In caso si voglia testare eseguendo più volte, mette elementi di default
			boredInit();
		
		PendingInvites p = new PendingInvites();				// Listener di supporto per gli inviti live
		p.start();
		
		Socket connectionSocket;
		
		System.out.println("*** Turing è operativo! ***");		// Tutto è inizializzato correttamente, quindi..
		
		while (true) {											// .. Turing diventa il Listener
			connectionSocket = null;
			connectionSocket = welcomeSocket.accept();
			
			if(Configurations.DEBUG)
				System.out.println("Server: Un client si è connesso.");
			e.execute(new RequestHandler(connectionSocket));
		}
	}
	
	
	// Controlla se esiste un utente di nome username nei Sets
	private static boolean checkAll(String username) {  		
		return (usersOnline.contains(username) || usersOffline.contains(username));
	}
	
	
	//		*** Richiesta di Login ***
	static int login(String username, String password) {
		
		synchronized(updateSets) {
			// Se l'utente risulta tra gli utenti offline && la password è corretta
			if(usersOffline.contains(username) && database.get(username).checkPassword(password)) {
				usersOffline.remove(username);			// Rimuovi dal Set degli utenti offline
				usersOnline.add(username);				// E metti nel Set degli utenti online
				
				if(Configurations.DEBUG)
					System.out.println("Utente " + username + " si è connesso.");
				return 0;
			}
			
			// Se è già connesso..
			if(usersOnline.contains(username)) {
				if(Configurations.DEBUG)
					System.err.println("Utente " + username + " ha tentato un doppio login.");
				return -1;
			}
		}
		
		// Se arrivo qua, le credenziali non erano corrette
		if(Configurations.DEBUG)
			System.err.println("Tentativo di login con credenziali errate.");
		return -2;
	}
	
	
	// Richiesta degli inviti a cui si è stati invitati durante il periodo di permanenza offline
	static Set<String> getPendingInvites(String username) {
		
		synchronized(updateDB) {
			User u = database.get(username);
			Set<String> tmp = null;
			
			if(u == null) {
				if(Configurations.DEBUG)
					System.err.println("Turing, getPendInv: questa stampa dovrebbe essere impossibile.");
				return tmp;
			}
			
			if(Configurations.DEBUG)
				System.out.println("Turing: " + u.getUser() + " ha richiesto i nomi dei documenti a cui è stato invitato mentre era offline.");
			
			return u.getPendingInvites();
		}
	}
	
	
	// Richiesta di un Set usato per la gestione degli inviti in diretta (live)
	static Set<String> getInstaInvites(String nameServed) {
		Set<String> tmp = null;
		
		synchronized(updateDB) {
			User u = database.get(nameServed);
			
			if(u != null) 
				tmp = u.getInstaInvites();
		}
		
		return tmp;
	}
	
	
	// Una volta ottenuti gli inviti, non voglio che vengano mostrati nuovamente all'accesso successivo -> clear
	static void resetInvites(String username) {
		database.get(username).resetPendingInvites();
	}

	
	// Identico a resetInvites, semplicemente con un target diverso
	static void resetInstaInvites(String username) {
		database.get(username).resetInstaInvites();
	}

	
	//		*** Richiesta di logout ***
	static boolean disconnect (String username) {
		
		synchronized(updateSets) {
			// Se l'utente è online
			if(usersOnline.contains(username)) {		
				usersOnline.remove(username);		// Lo rimuovo dal Set degli utenti online
				usersOffline.add(username);			// Per metterlo nel Set degli utenti offline
				if(Configurations.DEBUG)
					System.out.println("Utente " + username + " si è disconnesso.");
				return true;
			}
		}
		
		if(Configurations.DEBUG)
			System.err.println("È arrivata una richiesta di disconnect di " + username + " che non ha avuto alcun effetto.");

		return false;
	}
	
	
	//		*** Richiesta di Creazione Documento ***
	static int createDoc(String creator, String docName, int sections) {
		
		synchronized(updateDB) {
			// Se esiste un documento con lo stesso nome -> errore
			if(docs.containsKey(docName)) {
				if(Configurations.DEBUG)
					System.err.println(creator + " ha tentato di creare un duplicato di " + docName);
				return -1;
			}
			
			// Controllo extra sulla validità dell'user richiedente, non dovrebbe essere necessario
			if (!database.containsKey(creator)) {
				if(Configurations.DEBUG)
					System.err.println(creator + " utente sconosciuto (?)");
				return -2;
			}
			
			InetAddress addr = null;
			String aux;
			
			// Cerca un indirizzo tra 239.0.0.0 e 239.255.255.255 non già utilizzato
			// La motivazione del range limitato a 239.* è nella relazione
			do {
				aux = "239." + (int)Math.floor(Math.random()*256) + "." + (int)Math.floor(Math.random()*256) + "." + (int)Math.floor(Math.random()*256);
				try {
					addr = InetAddress.getByName(aux);
					if(Configurations.DEBUG && multicastAddresses.contains(addr))
						System.err.println("Una cosa altamente improbabile non è impossibile.");		// è altamente improbabile che due documenti abbiano lo stesso address
				} catch (UnknownHostException e) { e.printStackTrace(); }
			} while(!addr.isMulticastAddress() && multicastAddresses.contains(addr));
			
			// Se arrivo qua, l'indirizzo di Multicast non è usato da altri documenti, quindi le chat sono coerenti
			multicastAddresses.add(addr);
			Document d = new Document(creator, docName, sections, addr);
			
			// Aggiorno effettivamente il database
			docs.put(docName, d);
			database.get(creator).addToEditableDocs(docName);
		}
		
		File dir = new File("Documents/" + docName);		// Crea dentro Documents (creato in init() ) la cartella docName
		dir.mkdir();
		
		for(int i = 0; i < sections; i++) {
			File x = new File("Documents/" + docName, docName + i + ".txt");	// Crea dentro la precedente cartella gli X files richiesti
			try {
				x.createNewFile();
			} catch (IOException e) { e.printStackTrace(); }
		}
		
		if(Configurations.DEBUG)
			System.out.println(creator + " ha creato il documento " + docName + " con " + sections + " sezioni.");
		return 0;
	}
	
	
	//		*** Richiesta di invito a documento ***
	static int sendInvite(String sender, String receiver, String docName) {
		
		synchronized(updateDB) {
			User rec = database.get(receiver);
			
			// Se il mittente non è registrato
			if(!checkAll(sender)) {
				if(Configurations.DEBUG)
					System.err.println(sender + " non registrato.");
				return -1;
			}
			// Se il destinatario non è registrato
			if(!checkAll(receiver)) {
				if(Configurations.DEBUG)
					System.err.println(receiver + " non registrato.");
				return -2;
			}
			// Se il documento non esiste
			if(!docs.containsKey(docName)) {
				if(Configurations.DEBUG)
					System.err.println(docName + " non è un documento esistente.");
				return -3;
			}
			// Se il mittente non ha creato il documento
			if(!docs.get(docName).isCreator(sender)) {
				if(Configurations.DEBUG)
					System.err.println(sender + " non può invitare al documento " + docName + " in quanto non è creatore.");
				return -4;
			}
			// Se il destinatario è già editor
			if(database.get(receiver).isEditor(docName)) {
				if(Configurations.DEBUG)
					System.err.println(receiver + " è già editor di " + docName);
				return -5;
			}
			
			if(usersOnline.contains(receiver)) {		// L'utente è online, scrivo immediatamente (live invite)
				docs.get(docName).addEditor(receiver);
				rec.addInstaInvites(docName);
				rec.addToEditableDocs(docName);
				if(Configurations.DEBUG)
					System.out.println(sender + " ha invitato (live)  " + receiver + " come editor del documento " + docName);
			}
			
			else {										// L'utente è offline, salvo per quando si connetterà (pending invite)
				docs.get(docName).addEditor(receiver);
				rec.addPendingInvite(docName);
				rec.addToEditableDocs(docName);
				if(Configurations.DEBUG)
					System.out.println(sender + " ha invitato (pending) " + receiver + " come editor del documento " + docName);
			}
		}
		return 0;
	}
	
	
	//		*** Richiesta dei documenti (List) ***
	static String getDocs(String username) {
		
		String res = null;
		int antiBug = 0;			
		
		synchronized(updateDB) {
			User u = database.get(username);
			Object[] uDocs = u.getDocs().toArray();
			antiBug = uDocs.length; 		// Se è != 0 -> c'è almeno un documento
			res = "Nessun documento.";		// Se non dovesse entrare nel ciclo, restituirà questa frase di default
			
			for(int i = 0; i < uDocs.length; i++) {
				
				Document d = docs.get(uDocs[i]);		// Prende il doc
				String edtr = d.getEditors();			// Prende gli editors
				if(edtr == null)
					edtr = "Nessuno.";					// Evito di mostrare "null" 
				
				if(res.equals("Nessun documento.")) 	// Se è la prima iterazione..
					res = "Nome documento: " + d.getName() +"\nCreatore: " + d.getCreator() + "\nCollaboratori: " + edtr;
				else									
					res = res + "\nNome documento: " + d.getName() +"\nCreatore: " + d.getCreator() + "\nCollaboratori: " + edtr;
				
				res = res + '\n';						// Per dare una newline tra info di un documento ed un altro
			}
		}
		
		if(antiBug != 0)				// Senza questo, la richiesta di List fa comparire un "null" nella finestra del Client
			res = res + '\n';
		
		return res;
	}
	
	
	//		*** Richiesta di modifica sezione di documento ***
	static String editDoc(String username, String docName, int section, SocketChannel clientChannel) throws IOException {
		
		Document d = null;
		String res = null;
		
		synchronized(updateDB) {
			d = docs.get(docName);
			
			// Se non esiste l'user o il doc richiedente/richiesto (anche qui, l'user dovrebbe essere sempre valido ma rimane il controllo extra)
			if(database.get(username) == null || d == null) {
				if(Configurations.DEBUG)
					System.err.println("Turing [EditRequest]: User non esistente || Documento inesistente");
				return "NULL";
			}
			
			// Se il richiedente non è né creatore né collaboratore del documento
			if(!d.isCreator(username) && !d.isEditor(username)) {
				if(Configurations.DEBUG)
					System.err.println("Turing [EditRequest]:" + username + " non può modificare questo documento.");
				return "UNABLE";
			}
			
			// Se la sezione richiesta è maggiore delle sezioni massime del documento
			if(d.getSize() <= section)
				return "OOB";
			
			// Se qualcun altro sta già lavorando sulla stessa sezione del documento
			if(d.isLocked(section)) {
				if(Configurations.DEBUG)
					System.err.println("Turing [EditRequest]: qualcuno sta già lavorando sulla sezione " + section + " di " + docName);
				return "LOCK";
			}
			
			// Provo effettivamente a prendere la lock su tale documento (non dovrebbe fallire per via della synchronized, ma check comunque)
			if(!d.editSection(section))
				return "TRYLOCK";
		
			// Apro in lettura il file richiesto
			FileChannel inChannel = FileChannel.open(Paths.get("Documents/" + docName + "/" + docName + section + ".txt"), StandardOpenOption.READ);
			// Ed alloco un buffer per inviarlo al client
			ByteBuffer buffer = ByteBuffer.allocateDirect(1024*1024);
			
			boolean stop = false;
			
			// Classica gestione NIO
			while (!stop) { 
				int bytesRead=inChannel.read(buffer);
				if (bytesRead==-1) 
					stop=true;
				
				buffer.flip();
				while (buffer.hasRemaining())
					clientChannel.write(buffer);
				buffer.clear();
			}
			clientChannel.close();		// Chiudo il channel verso il client per far sapere che è finita la fase di trasferimento
			inChannel.close(); 			// Chiudo il channel della lettura del file richiesto
			
			res = d.getAddr().toString();	// Ottengo l'indirizzo di multicast del documento
		}
		if(Configurations.DEBUG)
			System.out.println(username + " sta ora modificando la sezione " + section + " di " + docName);
		
		res = (String) res.subSequence(1, res.length());		// L'indirizzo di multicast ha uno / iniziale, lo tolgo
		return res;
	}

	
	//		*** Richiesta di upload della sezione di documento modificata ***
	static String endEdit(String username, String docName, int section, SocketChannel clientChannel) throws IOException {
		
		Document d = null;
		synchronized(updateDB) {
			d = docs.get(docName);
			
			File x = new File("Documents/" + docName, docName + section + ".txt");
			if(x.exists())			// Se esiste (dovrebbe in quanto è stato scaricato!)
				x.delete();			// Cancello il file precedente in quanto non aggiornato
			x.createNewFile();		// Crea il nuovo file su cui andrò a scrivere ciò che ricevo dal client
			
			// Apro in scrittura il file target
			FileChannel outChannel = FileChannel.open(Paths.get("Documents/" + docName + "/" + docName + section + ".txt"),	StandardOpenOption.WRITE);
			// Ed alloco un buffer per ricevere dal client
			ByteBuffer buffer = ByteBuffer.allocateDirect(1024*1024);
			
			boolean stop=false;
			
			// Tutto come prima
			while (!stop) { 
				int bytesRead=clientChannel.read(buffer);
				if (bytesRead==-1) 
					stop=true;

				buffer.flip();
				while (buffer.hasRemaining())
					outChannel.write(buffer);
				buffer.clear();
			}
			clientChannel.close(); 
			outChannel.close();
			
			d.unlockSection(section);			// Unlock della sezione così è di nuovo modificabile
			
		}
		
		return "SUCCESS";						
	}

	
	//		*** Richiesta di visualizzazione di sezione di un documento ***
	static int getFile(String username, String docName, int section, SocketChannel clientChannel) throws IOException {
		int res = 0;
		
		synchronized(updateDB) {
			
			// Se il documento non esiste mi fermo
			if(!docs.containsKey(docName))
				return -1;
			// Se il documento sta venendo modificato, salvo l'informazione 
			else if(docs.get(docName).isLocked(section))
				res = 1;
				
			// Stessa gestione NIO di poco fa
			FileChannel inChannel = FileChannel.open(Paths.get("Documents/" + docName + "/" + docName + section + ".txt"), StandardOpenOption.READ);
			ByteBuffer buffer = ByteBuffer.allocateDirect(1024*1024);
			boolean stop = false;
			
			while (!stop) { 
				int bytesRead=inChannel.read(buffer);
				if (bytesRead==-1) 
					stop=true;

				buffer.flip();
				while (buffer.hasRemaining())
					clientChannel.write(buffer);
				buffer.clear();
			}
			clientChannel.close();
			inChannel.close(); 
		}
		
		return res;			// Di default è 0, che significa "nessuno ci sta lavorando". Se -1, il documento non esiste. Se 1, "qualcuno ci sta lavorando"
	}

	
	// 		*** Richiesta di visualizzazione di intero documento ***
	static String getDocument(String username, String docName, SocketChannel clientChannel) throws IOException {
		String res = "SUCCESS";
		
		synchronized(updateDB) {
			
			// Se il documento non esiste
			if(!docs.containsKey(docName))
				return "NO_EXIST";
			
			Document d = docs.get(docName);
			for(int i = 0; i < d.getSize(); i++) {
				if(d.isLocked(i)) {				// Controllo se almeno una persona sta modificando
					if(res.equals("SUCCESS")) 
						res = "Sezioni sotto modifica: ";
					res = res + i + " ";		// Creo la stringa delle sezioni modificate
				}
			}
			
			ByteBuffer buffer = ByteBuffer.allocateDirect(1024*1024);
			// Gestione NIO come le precedenti, ma più FileChannels di lettura 
			// In pratica, avviene una concatenazione dei file txt in un unico txt
			for(int i = 0; i < d.getSize(); i++) {
				
				FileChannel inChannel = FileChannel.open(Paths.get("Documents/" + docName + "/" + docName + i + ".txt"), StandardOpenOption.READ);
				boolean stop = false;
				
				while (!stop) { 
					int bytesRead=inChannel.read(buffer);
					if (bytesRead==-1) 
						stop=true;

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

	
	// Semplice unlock di una sezione
	static void unlock(String nameServed, String docServed, int sectionDoc) {

		synchronized(updateDB) {
			Document d = docs.get(docServed);
			d.unlockSection(sectionDoc);
		}
		
	}

	
	// Controllo dei permessi e simili per un user, un doc e la sua size
	static int checkFile(String username, String docName, int section) {
		
		Document d = null;
		User u = null;
		
		synchronized(updateDB) {
			d = docs.get(docName);
			u = database.get(username);
		}
		
		if(d == null)
			return -1;
		
		if(u == null)
			return -2;
		
		if(!d.isEditor(username))
			return -3;
		
		if(!u.isEditor(docName))
			return -4;
		
		if(section >= d.getSize() && section <= Configurations.MAX_SECTIONS)
			return -5;
		
		return d.getSize();
	}

	
	// Controllo simile al precedente, ma senza considerare la size
	static boolean checkPermissions(String username, String docName) {
		synchronized(updateDB) {
			User u = database.get(username);
			Document d = docs.get(docName);
			
			if(u==null || d ==null)
				return false;
			
			if(!u.isEditor(docName))
				return false;
			
			if(!d.isEditor(username))
				return false;
		}
		return true;
	}

	
	// Controllo di supporto per sapere se un utente è online
	static boolean isOnline(String nameServed) {
		return usersOnline.contains(nameServed);
	}
}
