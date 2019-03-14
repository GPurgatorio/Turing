package client;

import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import server.Configurations;

public class GUILoggedClass extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	// Client related
	private String username;						// Nome dell'utente connesso
	
	// Client-Server related
	private static Socket clientSocket;				// Socket per la gestione TCP
	private static DataOutputStream outToServer;
	private static BufferedReader inFromServer;
	private static SocketChannel clientChannel;		// Channel per il download dei files
	private static NotSoGUIListener l;				// Listener degli Inviti Live
	
	// User Interface related
	private Image createDocImg, inviteImg, showImg, listImg, editImg, logoutImg;
	private JButton createDocButton, inviteButton, editButton, listButton, showButton, logoutButton;
	private JLabel userLabel;
	
	
	// Costruttore
	public GUILoggedClass(Socket s, SocketChannel c, String usr, String fromWhat) throws IOException {

		if(Configurations.DEBUG)
			System.out.println("Inizializzazione LoggedGUI");
		
		clientSocket = s;
		clientChannel = c;
		username = usr;
		outToServer = new DataOutputStream(clientSocket.getOutputStream());
		inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		
		clientUI();
		
		if(clientChannel == null) 
			clientChannel = createChannel();
		
		if(fromWhat.equals("login")) {		// Se arrivo da un login -> voglio controllare gli inviti
			checkPendingInvites();			// di quando ero offline...
			invitesListener();				// E voglio attivare il Listener per gli Inviti Live
		}
		
		if(Configurations.DEBUG)			//Per evitare confusione tra le varie consoles, un punto di riferimento
			System.out.println("Console di: " + username);
	}
	
	
	private SocketChannel createChannel() throws IOException {
		SocketChannel socketChannel = SocketChannel.open();
		int port = username.hashCode() % 65535;
        if(port < 0) 
        	port = -port % 65535;
        if(port < 1024)		//evito le porte "Well-known" [0-1023]
        	port += 1024;
        SocketAddress socketAddr = new InetSocketAddress("localhost", port);;
        socketChannel.connect(socketAddr);
        return socketChannel;
	}

	
	//Listener per gli inviti live (durante il periodo in cui l'utente è online)
	private void invitesListener() {
		
		Socket pendSocket = null;
		
		try {
			pendSocket = new Socket("localhost", Configurations.INVITE_PORT);
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Il server di supporto è offline!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		l = new NotSoGUIListener(pendSocket, username);		// Listener degli Inviti Live
		l.start();
		
		if(Configurations.DEBUG)
			System.out.println("Invite Live Listener attivo");
	}

	
	// Funzione che controlla se l'utente è stato invitato a qualche documento mentre era offline
	private void checkPendingInvites() throws IOException {
		
		String res = null;
		do {
			res = inFromServer.readLine();
			
			if(res.length() > 0)
				JOptionPane.showMessageDialog(null, "Mentre eri offline, sei stato invitato al documento:\n" + res, "Pending Invite", JOptionPane.INFORMATION_MESSAGE);
		
		} while(res.length() != 0);		// N.B: Crea un MessageDialog per ogni documento
	}

	
	//User Interface
	public void clientUI() throws IOException {

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 		// Per terminare l'applicazione quando viene chiusa
		setLayout(null);										// Per gestire manualmente tutta l'interfaccia
		setSize(450,450);

		createDocImg = ImageIO.read(new File("img/add.png"));
		inviteImg = ImageIO.read(new File("img/invite.png"));
		showImg = ImageIO.read(new File("img/show.png"));
		listImg = ImageIO.read(new File("img/list.png"));
		editImg = ImageIO.read(new File("img/edit.png"));
		logoutImg = ImageIO.read(new File("img/logout.png"));
		
		createDocImg = createDocImg.getScaledInstance(80, 80, Image.SCALE_DEFAULT);			
		inviteImg = inviteImg.getScaledInstance(80, 80, Image.SCALE_DEFAULT);			
		showImg = showImg.getScaledInstance(80, 80, Image.SCALE_DEFAULT);			
		listImg = listImg.getScaledInstance(80, 80, Image.SCALE_DEFAULT);			
		editImg = editImg.getScaledInstance(80, 80, Image.SCALE_DEFAULT);			
		logoutImg = logoutImg.getScaledInstance(80, 80, Image.SCALE_DEFAULT);
		
		createDocButton = new JButton();
		createDocButton.setIcon(new ImageIcon(createDocImg));
		inviteButton = new JButton();
		inviteButton.setIcon(new ImageIcon(inviteImg));
		showButton = new JButton();
		showButton.setIcon(new ImageIcon(showImg));
		listButton = new JButton();
		listButton.setIcon(new ImageIcon(listImg));
		editButton = new JButton();
		editButton.setIcon(new ImageIcon(editImg));
		logoutButton = new JButton();
		logoutButton.setIcon(new ImageIcon(logoutImg));
		
		createDocButton.setBounds(95,50,115,115);
		inviteButton.setBounds(225,50,115,115);
		showButton.setBounds(95,170,115,115);
		listButton.setBounds(225,170,115,115);
		editButton.setBounds(95,290,115,115);
		logoutButton.setBounds(225,290,115,115);
		
		createDocButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent ae){
				try {
					createDocRequest();
				} catch (IOException e) { e.printStackTrace(); }
			}
		});
		
		inviteButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent ae){
				try {
					inviteRequest();
				} catch (IOException e) { e.printStackTrace(); }
			}
		});
		
		showButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent ae){
				try {
					showRequest();
				} catch (IOException e) { e.printStackTrace(); }
			}
		});
		
		listButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent ae){
				try {
					listRequest();
				} catch (IOException e) { e.printStackTrace(); }
			}
		});
		
		editButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent ae){
				try {
					editRequest();
				} catch (IOException e) { e.printStackTrace(); }
			}
		});
		
		logoutButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent ae){
				try {
					logoutRequest();
				} catch (IOException e) { e.printStackTrace(); }
			}
		});
		
		add(createDocButton);
		add(inviteButton);
		add(showButton);
		add(listButton);
		add(editButton);
		add(logoutButton);

		userLabel = new JLabel("Current user: " + username);
		userLabel.setBounds(160, 20, 200, 15);
		userLabel.setFont(new Font("Franklin Gothic Medium", Font.PLAIN, 15));
		add(userLabel);
	}
	
	
	//			***CREATE DOCUMENT***
	
	public void createDocRequest() throws IOException {
		
		JTextField docLabel = new JTextField();
		JTextField secLabel = new JTextField();
		
		Object[] struct = {
				"Nome Documento:", docLabel,
				"Numero Sezioni [2-" + Configurations.MAX_SECTIONS + "]:", secLabel
		};
		
		String res, docName = null;
		int sections; 
		
		do {
			try {
				int option = JOptionPane.showConfirmDialog(null, struct, "Creazione Documento", JOptionPane.OK_CANCEL_OPTION);
				
				if (option == JOptionPane.OK_OPTION) {
					docName = docLabel.getText();						// Nome documento da creare
					sections = Integer.parseInt(secLabel.getText());	// Numero sezioni del documento da creare
				}
				else
					return;
			}
			catch (NumberFormatException e) {
				sections = -1;										// Viene settato a -1 così il while cicla nuovamente
			}	
		// Finché il numero di sezioni < 2 (non comporta editing collaborativo), o maggiore del Massimo permesso dal sistema, o manca il nome documento o il nome documento contiene caratteri non validi
		} while (sections < 2 || sections > Configurations.MAX_SECTIONS || docName == null || !docName.matches(Configurations.VALID_CHARACTERS));
		
		outToServer.writeBytes("createDoc" + '\n');			// Comando per iniziare la richiesta
		
		outToServer.writeBytes(username + '\n');			// Inputs per la richiesta
		outToServer.writeBytes(docName + '\n');
		outToServer.writeByte(sections);
		
		res = inFromServer.readLine();						// Risposta del server
		
		switch(res) {
			case "SUCCESS":
				JOptionPane.showMessageDialog(null, "Documento " + docName + " creato con successo!");
				break;
			case "DOC_EXISTS":
				JOptionPane.showMessageDialog(null, "Un documento con nome " + docName + " risulta già presente. Cambia nome!", "Error", JOptionPane.ERROR_MESSAGE);
				break;
			case "HACKER":									// Irraggiungibile (?)
				JOptionPane.showMessageDialog(null, "Non risulti essere registrato.");
				break;
			default:
				JOptionPane.showMessageDialog(null, "Errore generico.");
				if(Configurations.DEBUG)
					System.err.println(res);
				break;
		}
	}
	
	
	//			***INVITE***
	
	public void inviteRequest() throws IOException {
		
		JTextField docLabel = new JTextField();
		JTextField usLabel = new JTextField();
		
		Object[] struct = {
				"Documento:", docLabel,
				"Utente:", usLabel
		};
		
		String res, user = null, docName = null; 
		
		do {
			int option = JOptionPane.showConfirmDialog(null, struct, "Invito a Documento", JOptionPane.OK_CANCEL_OPTION);
			
			if (option == JOptionPane.OK_OPTION) {
				docName = docLabel.getText();					// Nome del documento a cui invitare
				user = usLabel.getText();						// Utente da invitare al documento
			}
			else
				return;
		} while (docName == null || user == null || !docName.matches(Configurations.VALID_CHARACTERS) || !user.matches(Configurations.VALID_CHARACTERS));				//prendi inputs corretti || stop
		
		outToServer.writeBytes("invite" + '\n');				// Comando per iniziare la richiesta
		
		outToServer.writeBytes(username + '\n');				// Inputs per la richiesta
		outToServer.writeBytes(user + '\n');
		outToServer.writeBytes(docName + '\n');
		
		res = inFromServer.readLine();							// Risposta del server
		
		switch(res) {
			case "SUCCESS":
				JOptionPane.showMessageDialog(null, "Utente " + user + " invitato con successo al documento " + docName + "!" );
				break;
			case "HACKER":						// Irraggiungibile (?)
				JOptionPane.showMessageDialog(null, "Non risulti registrato.");
				break;
			case "UNKNWN_USR":
				JOptionPane.showMessageDialog(null, "L'utente " + user + " non risulta essere registrato.");
				break;
			case "UNKNWN_DOC":
				JOptionPane.showMessageDialog(null, "Il documento " + docName + " non risulta essere presente.");
				break;
			case "NOT_CREATOR":
				JOptionPane.showMessageDialog(null, "Non puoi invitare persone ad un documento di cui non sei il creatore.");
				break;
			case "EDITOR_ALRD":
				JOptionPane.showMessageDialog(null, "L'utente " + user + " è già Editor del documento " + docName + "!");
				break;
			default:
				JOptionPane.showMessageDialog(null, "Errore generico.");
				break;
		}
	}
	
	
	//			***SHOW***

	public void showRequest() throws IOException {
		
		JTextField docLabel = new JTextField();
		JTextField secLabel = new JTextField();
		
		Object[] struct = {
				"Nome Documento:", docLabel,
				"Sezione: (" + Configurations.MAX_SECTIONS + " -> doc completo):", secLabel
		};
		
		String res, docName = null;
		int sections;
		
		do {
			try {
				sections = 256;
				int option = JOptionPane.showConfirmDialog(null, struct, "Mostra [Sezione di] Documento", JOptionPane.OK_CANCEL_OPTION);
				
				if (option == JOptionPane.OK_OPTION) {
					docName = docLabel.getText();							// Nome del documento che si desidera visualizzare
					if(secLabel.getText().length() > 0) 
						sections = Integer.parseInt(secLabel.getText());	// Sezione del documento che si desidera visualizzare
				}
				else
					return;
			}
			catch (NumberFormatException e) {
				sections = -1;
			}
		} while (docName == null || sections < 0 || sections > Configurations.MAX_SECTIONS || !docName.matches(Configurations.VALID_CHARACTERS));	//prendi inputs corretti || stop
		
		outToServer.writeBytes("show" + '\n');				// Comando per iniziare la richiesta
		
		outToServer.writeBytes(username + '\n');			// Inputs per la richiesta
		outToServer.writeBytes(docName + '\n');
		outToServer.writeByte(sections);	
		
		res = inFromServer.readLine();						// Risposta del server
		
		if(res.equals("SUCCESS")) {
			
			//Download del File
			
			File gen = new File ("Downloads/");				// Crea cartella Downloads se non esiste
			if(!gen.exists())
				gen.mkdir();
			
			File x, dir = new File("Downloads/" + username);
			
			if(!dir.exists())								// Crea cartella Downloads/username se non esiste
				dir.mkdir();
			
			if(sections >= 0 && sections < Configurations.MAX_SECTIONS)			// Se si sta tentando di prendere una sezione
				x = new File("Downloads/" + username, docName + sections + ".txt");
			else																// Se si sta tentando di scaricare il documento intero
				x = new File("Downloads/" + username, docName + "_COMPLETE.txt");
			
			if(x.exists())						// Se esiste un file con lo stesso nome è sicuramente più vecchio
				x.delete();						// Quindi lo elimino
			x.createNewFile();					// E ne creo uno nuovo 
			
			FileChannel outChannel;
			
			if(sections >= 0 && sections < Configurations.MAX_SECTIONS)		// Se è arrivato qua il file esiste, quindi è <= #sezioni e perciò ovviamente <= MaxSections
				outChannel = FileChannel.open(Paths.get("Downloads/" + username + "/" + docName + sections + ".txt"),	StandardOpenOption.WRITE);
			else
				outChannel = FileChannel.open(Paths.get("Downloads/" + username + "/" + docName + "_COMPLETE.txt"),	StandardOpenOption.WRITE);
			
			ByteBuffer buffer = ByteBuffer.allocateDirect(1024*1024);
			boolean stop = false;
			
			while(!stop) {				// NIO
	
				int bytesRead = clientChannel.read(buffer);
				if (bytesRead == -1) {
					stop=true;
				}
				buffer.flip();
				while (buffer.hasRemaining())
					outChannel.write(buffer);
				buffer.clear();
			}
			clientChannel.close();
			outChannel.close();
			
			clientChannel = createChannel();
			
			if(Configurations.DEBUG)
				System.out.println("File scaricato correttamente!");
			
			res = inFromServer.readLine();						// Per info ulteriori
		}
		
		switch(res) {
			case "SUCCESS":
				if(sections != Configurations.MAX_SECTIONS)
					JOptionPane.showMessageDialog(null, "Sezione " + sections + " del documento " + docName + " scaricato nella tua cartella personale!", "Success", JOptionPane.INFORMATION_MESSAGE );
				else	
					JOptionPane.showMessageDialog(null, "Documento " + docName + " scaricato nella tua cartella personale!", "Success", JOptionPane.INFORMATION_MESSAGE );
				break;
			case "EDITING":
				JOptionPane.showMessageDialog(null, "Documento " + docName + " scaricato. Qualcuno ci sta lavorando sopra!", "Section Not Up To Date", JOptionPane.INFORMATION_MESSAGE);
				break;
			case "NO_EXIST":
				JOptionPane.showMessageDialog(null, "Il documento " + docName + " non esiste", "Error", JOptionPane.ERROR_MESSAGE);
				break;
			case "HACKER":
				JOptionPane.showMessageDialog(null, "User non registrato (?)", "Error", JOptionPane.ERROR_MESSAGE);
				break;
			case "UNABLE":
				JOptionPane.showMessageDialog(null, "Non sei editor del documento (D)", "Error", JOptionPane.ERROR_MESSAGE);
				break;
			case "UNABLEU":
				JOptionPane.showMessageDialog(null, "Non sei editor del documento (U)", "Error", JOptionPane.ERROR_MESSAGE);
				break;
			case "OOB":
				JOptionPane.showMessageDialog(null, "Hai richiesto una sezione non globale ed Out Of Bound.", "Error", JOptionPane.ERROR_MESSAGE);
				break;
			default:
				if(res.startsWith("Sezioni"))
					JOptionPane.showMessageDialog(null, "Documento " + docName + " completo scaricato con successo. " + res);
				else {
					JOptionPane.showMessageDialog(null, "Errore generico.");
					if(Configurations.DEBUG)
						System.err.println(res);
				}
				break;
		}
	}
	
	
	//			***LIST***
	
	public void listRequest() throws IOException {
	
		outToServer.writeBytes("list" + '\n');		// Comando per iniziare la richiesta
		
		outToServer.writeBytes(username + '\n');	// Input per richiesta
		
		String res = null, tmp = null;				//N.B: non serve controllare esito, deve essere SUCCESS
		int check = 0;
		do {
			tmp = inFromServer.readLine();			// Costruisco la stringa finale
			
			if(tmp.length() < 1) {					// Il server manda uno \n per ogni riga (classico writeBytes) ed alla fine 
				check++;							// un ulteriore \n, contandoli so quando ho finito i documenti
				res = res + '\n';
			}
			else {
				check = 0;							// Non ho ricevuto \n , quindi sta arrivando un altro documento, reset
				if(res == null)
					res = tmp + '\n';
				else
					res = res + tmp + '\n';
			}
		} while(check < 2 && !tmp.equals("Nessun documento."));		

		if(res != null) 
			tmp = "SUCCESS";
		
		switch(tmp) {
			case "SUCCESS":
				JOptionPane.showMessageDialog(null, "Lista Documenti: \n\n" + res );
				break;
			default:
				JOptionPane.showMessageDialog(null, "Errore generico (?)");
				if(Configurations.DEBUG)
					System.err.println(res);
				break;
		}
	}
	
	
	//			***EDIT***

	public void editRequest() throws IOException {
		
		JTextField docLabel = new JTextField();
		JTextField secLabel = new JTextField();
		
		Object[] struct = {
				"Documento:", docLabel,
				"Sezione:", secLabel
		};
		
		String res, docName = null;
		int section; 
		do {
			try {
				int option = JOptionPane.showConfirmDialog(null, struct, "Modifica Sezione di Documento", JOptionPane.OK_CANCEL_OPTION);
				
				if (option == JOptionPane.OK_OPTION) {
					docName = docLabel.getText();						// Nome del documento che si desidera modificare
					section = Integer.parseInt(secLabel.getText());		// Sezione del documento che si desidera modificare
				}
				else
					return;
			}
			catch (NumberFormatException e) {
				section = -1;
			}
		} while (docName == null || section < 0 || section >= Configurations.MAX_SECTIONS || !docName.matches(Configurations.VALID_CHARACTERS));		//prendi inputs corretti || stop

		outToServer.writeBytes("editDoc" + '\n');				// Richiesta
		
		outToServer.writeBytes(username + '\n');				// Inputs per la richiesta
		outToServer.writeBytes(docName + '\n');
		outToServer.writeByte(section);
		
		String check = inFromServer.readLine();					// Risposta del server
		
		if(check.equals("OOB")) {								// "Out of Bounds"
			JOptionPane.showMessageDialog(null, "Sezione oltre numero sezioni del documento.", "Out Of Bounds", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		String tmp = inFromServer.readLine();					// Se arrivo qua -> Risultato richiesta
	
		if(tmp.startsWith("2")) {
			
			//Download del File
			
			File gen = new File("Editing/");					// Creo la cartella Editing se non esiste
			if(!gen.exists())
				gen.mkdir();
			
			File x, dir = new File("Editing/" + username);		// Creo la cartella Editing/username se non esiste
			if(!dir.exists())
				dir.mkdir();
			
			x = new File("Editing/" + username, docName + section + ".txt");
			
			if(x.exists())										// Se esiste un file con lo stesso Path, è sicuramente più vecchio
				x.delete();										// Quindi lo cancello
			x.createNewFile();									// E ne creo uno nuovo
			
			FileChannel outChannel = FileChannel.open(Paths.get("Editing/" + username + "/" + docName + section + ".txt"), StandardOpenOption.WRITE);
			ByteBuffer buffer = ByteBuffer.allocateDirect(1024*1024);
			boolean stop = false;
			
			// NIO
			while(!stop) {
	
				int bytesRead = clientChannel.read(buffer);
				if (bytesRead == -1) 
					stop=true;
				buffer.flip();
				while (buffer.hasRemaining())
					outChannel.write(buffer);
				buffer.clear();
			}
			
			clientChannel.close();
			outChannel.close(); 
			
			clientChannel = createChannel();
			
			res = "SUCCESS";				// Passo alla modalità di Editing
		}		
		
		else
			res = "ERROR";
		
		switch(res) {
			case "SUCCESS":
				GUIEditClass w = new GUIEditClass(clientSocket, clientChannel, username, tmp, docName, section);
				username = "";
				w.getContentPane().setBackground(Configurations.GUI_BACKGROUND);
				w.setLocation(Configurations.GUI_X_POS, Configurations.GUI_Y_POS);
				w.setVisible(true);
				this.dispose();		
				break;
			case "ERROR":
				if(tmp.equals("NULL"))
					JOptionPane.showMessageDialog(null, "Documento (o User..) non esistente.", "Error", JOptionPane.ERROR_MESSAGE);
				else if(tmp.equals("UNABLE"))
					JOptionPane.showMessageDialog(null, "Non puoi modificare questo documento.", "Unable", JOptionPane.ERROR_MESSAGE);					
				else if(tmp.equals("LOCK"))
					JOptionPane.showMessageDialog(null, "La sezione sta già venendo modificata da qualcun altro!", "Error", JOptionPane.ERROR_MESSAGE);
				else if(tmp.equals("TRYLOCK"))
					JOptionPane.showMessageDialog(null, "TryLock ha fallito.", "TryLock", JOptionPane.ERROR_MESSAGE);
				else if(tmp.equals("OOB"))
					JOptionPane.showMessageDialog(null, "Fuori dal numero di sezioni", "Out of Bounds", JOptionPane.ERROR_MESSAGE);
				break;
			default:
				JOptionPane.showMessageDialog(null, "Errore generico.");
				if(Configurations.DEBUG)
					System.err.println(tmp);
				break;
		}
	}
	
	
	//			***LOGOUT***

	public void logoutRequest() throws IOException {
		
		outToServer.writeBytes("logout" + '\n');				// Comando per iniziare la richiesta
		
		outToServer.writeBytes(username + '\n');				// Input per la richiesta

		String res = inFromServer.readLine();					// Risposta del server
		
		if(!res.equals("ERROR")) {
			username = "";
			l.disable();										// Interrompo Listener
			clientSocket.close();
			clientChannel.close();
			
			// Torno alla schermata di Login
			GUIClass w = new GUIClass();			
			w.getContentPane().setBackground(Configurations.GUI_LOGIN_BACKGROUND);	
			w.setLocation(w.getX(), w.getY());
			w.setVisible(true);
			this.dispose();
		}
		else if(Configurations.DEBUG)			// Irraggiungibile (?)
			JOptionPane.showMessageDialog(null, "Non risulti offline (?).");
	}

}
