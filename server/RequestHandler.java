package server;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class RequestHandler implements Runnable {

	// RequestHandler related
	private String nameServed = "";				// Si salva il nome al Login per poter gestire la sua disconnessione in caso di crash etc
	private String docServed = "";				// Come nameServed, ma per il documento in fase di editing. Serve per rilasciare la lock
	private int sectionDoc;						// Di supporto a docServed, per sapere quale lock lasciare
	
	// Client-Server related
	private Socket clientSocket;				// Socket per la gestione TCP
	private BufferedReader inFromClient;
	private DataOutputStream outToClient;
	private SocketChannel clientChannel = null;	// Channel per la gestione file
	
	public RequestHandler(Socket s) throws IOException {
		clientSocket = s;
		inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		outToClient = new DataOutputStream(clientSocket.getOutputStream());
		sectionDoc = -1;	
	}
	
	/* 	Questo runnable gestisce le richieste del client. Il formato è semplice:
	 * 
	 * (1) Leggi richiesta (login, logout, etc.)
	 * (2) se riconosciuta, leggi inputs (username, document, section, etc.)
	 * (3) chiama un metodo di Turing per elaborare la richiesta ed attendi risultato
	 * (4) una volta saputo il risultato della richiesta, inoltra al client
	 * *** a volte questioni extra che commenterò
	 */
	@Override
	public void run() {
		boolean flag = true;
		do {
			try {
				
				String username, password, answer, receiver, docName;
				String command = inFromClient.readLine();			// Legge richiesta del client
				
				if(Configurations.DEBUG)
					System.out.println("Request Handler: gestisce una richiesta di " + command);
				
				if(command.equals("login")) {
					
					username = inFromClient.readLine();
					nameServed = username;
					password = inFromClient.readLine();
					answer = "ERROR" + '\n';
		
					int res = Turing.login(username, password);
					
					if(res == -1)
						answer = "LOGGED_ALRD" + '\n';
					else if(res == -2)
						answer = "UNKNWN_USR" + '\n';
					else
						answer = "SUCCESS" + '\n';
					
					outToClient.writeBytes(answer);
					
					if(answer.equals("SUCCESS\n")) {		// Se il login ha avuto successo, il client deve essere informato
						sendPendingInvites();				// Degli inviti che l'utente ha ricevuto mentre era offline
						
						//creo channel
						if(clientChannel == null)
							clientChannel = createChannel();
						
					}
				}
			
				else if(command.equals("logout")) {
					
					username = inFromClient.readLine();
					answer = "ERROR" + '\n';

					if(Turing.disconnect(username)) {
						nameServed = "";					// Smette di ricordare il nome in quanto è già disconnesso
						answer = "SUCCESS" + '\n';
					}
					
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
						answer = "HACKER" + '\n';			// L'errore HACKER è volutamente ironico in quanto reputo non sia possibile raggiungerlo
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
					
					int c = Turing.checkFile(username, docName, section);
					
					if(c == -5) 		// Non serve gestire gli altri casi, verranno gestiti dopo
						outToClient.writeBytes("OOB" + '\n');
					
					else {
						if(c < 0 && Configurations.DEBUG)
							System.err.println("c è < 0, controlla se output torna");
						
						outToClient.writeBytes("ok" + '\n');
						String res = Turing.editDoc(username, docName, section, clientChannel);
						
						if(res.equals("NULL") || res.equals("UNABLE") || res.equals("LOCK") || res.equals("TRYLOCK") || res.equals("OOB")) 
							outToClient.writeBytes(res + '\n');
						
						else {
							outToClient.writeBytes(res + '\n');		// Se sono qua -> success
							docServed = docName;					// Salvo nome del documento
							sectionDoc = section;					// E sezione in caso di crash
							
							clientChannel = null;
							clientChannel = createChannel();
						}
					}
				}
				
				else if (command.equals("endEdit")) {
					
					username = inFromClient.readLine();
					docName = inFromClient.readLine();
					int section = inFromClient.read();
					
					String res = Turing.endEdit(username, docName, section, clientChannel);
					
					sectionDoc = -1;			// Smetto di ricordare cosa stava modificando
					docServed = "";
					
					outToClient.writeBytes(res + '\n');
					
					clientChannel = null;
					clientChannel = createChannel();
				}
				
				else if (command.equals("list")) {
					
					username = inFromClient.readLine();
					String res = Turing.getDocs(username);		// Restituisce la stringa già creata nel formato desiderato
					outToClient.writeBytes(res + '\n');
				}
				
				else if (command.equals("show")) {
					
					username = inFromClient.readLine();
					docName = inFromClient.readLine();
					int section = inFromClient.read();
					int res = 2;
					
					String check = null;
					int c = 0;
					
					if(section == Configurations.MAX_SECTIONS) {			// Documento intero (vedasi relazione)
						if(Turing.checkPermissions(username, docName)) 		// Controllo permessi
							check = "SUCCESS";
						else 
							check = "UNABLE";
						
						outToClient.writeBytes(check + '\n');
					}
					
					else {													// Section <> Config.MAX_SECTIONS
						c = Turing.checkFile(username, docName, section);	// Controllo permessi
						
						if(c == -1) 
							check = "NO_EXIST";
						else if(c == -2)
							check = "HACKER";
						else if(c == -3) 
							check = "UNABLE";
						else if(c == -4)
							check = "UNABLEU";
						else if(c == -5)
							check = "OOB";								// Section > #files
						else
							check = "SUCCESS";							// Section < #files
						
						outToClient.writeBytes(check + '\n');
					}


					if(check.equals("SUCCESS")) {
	
						if(section < c) {								// Scarica sezione singola del documento
							res = Turing.getFile(username, docName, section, clientChannel);
							if(Configurations.DEBUG)
								System.out.println(username + " scarica la sezione " + section + " del file " + docName);
							
							if(res == 0)
								outToClient.writeBytes("SUCCESS" + '\n');
							else if(res == 1)
								outToClient.writeBytes("EDITING" + '\n');
							else if(res == -1)
								outToClient.writeBytes("NO_EXIST" + '\n');
							else
								outToClient.writeBytes("ERROR" + '\n');
						}
						
						else {											// Scarica intero documento
							String tmp = Turing.getDocument(username, docName, clientChannel);
							if(Configurations.DEBUG)
								System.out.println(username + " scarica la versione intera del file " + docName);
							switch(tmp) {
								case "SUCCESS":
									outToClient.writeBytes("SUCCESS" + '\n');
									break;
								case "NO_EXIST":
									outToClient.writeBytes("NO_EXIST" + '\n');
									break;
								default:		// Sezioni occupate
									if(tmp.startsWith("Sezioni"))
										outToClient.writeBytes(tmp + '\n');
									else
										outToClient.writeBytes("ERROR" + '\n');
									break;
							}
						}
						
						clientChannel = null;
						clientChannel = createChannel();
						
					}
				}
				else {
					System.err.println("Comando non riconosciuto: ->" + command);
				}
					
			}
			catch (Exception e) {			// Qualsiasi eccezione accada, tenta di rilasciare tutto per preservare la consistenza dei dati
				try {
					clientSocket.close();			// Chiudo socket tanto il client non è più attivo
					if(!nameServed.equals(""))		// Disconnect in modo da farlo riconnettere
						Turing.disconnect(nameServed);
					if(sectionDoc != -1) 			// Unlock in caso di crash
						Turing.unlock(nameServed, docServed, sectionDoc);
					flag = false;					// Graceful Shutdown
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		} while(flag);
		
		if(Configurations.DEBUG)
			System.err.println("Un runnable RH ha terminato i suoi servizi.");
	}

	// Genera un SocketChannel per la gestione file
	private SocketChannel createChannel() throws IOException {
		SocketChannel socketChannel = SocketChannel.open();
		int port = nameServed.hashCode() % 65535;
        if(port < 0) 
        	port = -port % 65535;
        if(port < 1024)		//evito le porte "Well-known" [0-1023]
        	port += 1024;
        SocketAddress socketAddr = new InetSocketAddress("localhost", port);
        socketChannel.connect(socketAddr);
        return socketChannel;
	}

	// Invia gli inviti pendenti di nameServed
	private void sendPendingInvites() {
		
		try {	
			Set<String> tmp = Turing.getPendingInvites(nameServed);
			
			if(tmp != null) {
				Iterator<String> it = tmp.iterator();
				while(it.hasNext())
					outToClient.writeBytes(it.next() + '\n');
			}

			outToClient.writeByte('\n');		// Notifica la fine dei documenti (N.B. non possono esistere documenti con questo nome!)
			Turing.resetInvites(nameServed);	// Pulisce il set in modo da non reinviarli al prossimo login
		
		} catch (IOException e) { e.printStackTrace(); }
		
	}

}
