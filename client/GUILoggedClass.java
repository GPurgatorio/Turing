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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
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
	
	private String username;					//username dell'utente che ha fatto login
	private static DataOutputStream outToServer;
	private static BufferedReader inFromServer;
	private static Socket clientSocket;
	private static SocketChannel clientChannel;
	private static ServerSocketChannel serverSocket;
	private static NotSoGUIListener l;			//listener degli inviti live
	private Image createDocImg, inviteImg, showImg, listImg, editImg, logoutImg;
	private JButton createDocButton, inviteButton, editButton, listButton, showButton, logoutButton;
	private JLabel userLabel;
	
	public GUILoggedClass(Socket s, SocketChannel c, ServerSocketChannel ssc, String usr, String fromWhat) throws IOException {

		if(Configurations.DEBUG)
			System.out.println("Inizializzazione LoggedGUI");
		
		clientSocket = s;
		clientChannel = c;
		serverSocket = ssc;
		username = usr;
		outToServer = new DataOutputStream(clientSocket.getOutputStream());
		inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		
		if(serverSocket == null)
			createServerSocketChannel();
		
		if(clientChannel == null)
			clientChannel = acceptServerSocket();
		
		clientUI();
		
		if(fromWhat.equals("login")) {		//se arrivo da un login -> voglio controllare gli inviti
			checkPendingInvites();
			invitesListener();				//e voglio attivare il Listener per gli inviti live
		}
		
		if(Configurations.DEBUG)			//Per evitare confusione tra le varie consoles, un punto di riferimento
			System.out.println("Console di: " + username);
	}
	
	private void createServerSocketChannel() throws IOException {
        serverSocket = ServerSocketChannel.open();
        int port = username.hashCode() % 65535;
        if(port < 0) 
        	port = -port % 65535;
        if(port < 1024)		//evito le porte "Well-known" [0-1023]
        	port += 1024;
        serverSocket.socket().bind(new InetSocketAddress(port));
	}
	
	private SocketChannel acceptServerSocket() throws IOException {
        SocketChannel client = null;
		client = serverSocket.accept();
        return client;
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
		
		l = new NotSoGUIListener(pendSocket, username);		//listener inviti live
		l.start();
		
		if(Configurations.DEBUG)
			System.out.println("Invite Live Listener attivo");
	}

	//Funzione che controlla se l'utente è stato invitato a qualche documento mentre era offline
	private void checkPendingInvites() throws IOException {
		
		String res = null;
		do {
			res = inFromServer.readLine();
			
			if(res.length() > 0)
				JOptionPane.showMessageDialog(null, "Mentre eri offline, sei stato invitato al documento:\n" + res, "Pending Invite", JOptionPane.INFORMATION_MESSAGE);
		
		} while(res.length() != 0);		//crea un MessageDialog per ogni documento
	}

	//User Interface
	public void clientUI() throws IOException {

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
		setLayout(null);
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
					docName = docLabel.getText();						//nome documento da creare
					sections = Integer.parseInt(secLabel.getText());	//numero sezioni del documento da creare
				}
				else
					return;
			}
			catch (NumberFormatException e) {
				sections = -1;
			}
		} while (sections < 2 || sections > Configurations.MAX_SECTIONS || docName == null || !docName.matches(Configurations.VALID_CHARACTERS));	//prendi inputs corretti || stop
		
		outToServer.writeBytes("createDoc" + '\n');			//richiesta
		
		outToServer.writeBytes(username + '\n');			//inputs per la richiesta
		outToServer.writeBytes(docName + '\n');
		outToServer.writeByte(sections);
		
		res = inFromServer.readLine();						//risultato richiesta
		
		switch(res) {
			case "SUCCESS":
				JOptionPane.showMessageDialog(null, "Documento " + docName + " creato con successo!");
				break;
			case "DOC_EXISTS":
				JOptionPane.showMessageDialog(null, "Un documento con nome " + docName + " risulta già presente. Cambia nome!", "Error", JOptionPane.ERROR_MESSAGE);
				break;
			case "HACKER":
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
				docName = docLabel.getText();					//nome del documento a cui invitare
				user = usLabel.getText();						//utente da invitare al documento
			}
			else
				return;
		} while (docName == null || user == null || !docName.matches(Configurations.VALID_CHARACTERS) || !user.matches(Configurations.VALID_CHARACTERS));				//prendi inputs corretti || stop
		
		outToServer.writeBytes("invite" + '\n');				//richiesta
		
		outToServer.writeBytes(username + '\n');				//inputs per la richiesta
		outToServer.writeBytes(user + '\n');
		outToServer.writeBytes(docName + '\n');
		
		res = inFromServer.readLine();							//risultato richiesta
		
		switch(res) {
			case "SUCCESS":
				JOptionPane.showMessageDialog(null, "Utente " + user + " invitato con successo al documento " + docName + "!" );
				break;
			case "HACKER":
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
					docName = docLabel.getText();							//nome del documento che si desidera visualizzare
					if(secLabel.getText().length() > 0) 
						sections = Integer.parseInt(secLabel.getText());	//sezione del documento che si desidera visualizzare [opt]
				}
				else
					return;
			}
			catch (NumberFormatException e) {
				sections = -1;
			}
		} while (docName == null || sections < 0 || sections > Configurations.MAX_SECTIONS || !docName.matches(Configurations.VALID_CHARACTERS));	//prendi inputs corretti || stop
		
		outToServer.writeBytes("show" + '\n');				//richiesta
		
		outToServer.writeBytes(username + '\n');			//inputs per la richiesta
		outToServer.writeBytes(docName + '\n');
		outToServer.writeByte(sections);	
		
		res = inFromServer.readLine();
		
		if(res.equals("SUCCESS")) {
			
			//DOWNLOAD DEL FILE
			
			File gen = new File ("Downloads/");			
			if(!gen.exists())
				gen.mkdir();
			
			File x, dir = new File("Downloads/" + username);
			
			if(!dir.exists())
				dir.mkdir();
			
			if(sections >= 0 && sections < Configurations.MAX_SECTIONS)		
				x = new File("Downloads/" + username, docName + sections + ".txt");
			else
				x = new File("Downloads/" + username, docName + "_COMPLETE.txt");
			
			if(x.exists())
				x.delete();
			x.createNewFile();
			
			FileChannel outChannel;
			
			if(sections >= 0 && sections < Configurations.MAX_SECTIONS)		//se è arrivato qua il file esiste, quindi è <= #sezioni e perciò ovviamente <= MaxSections
				outChannel = FileChannel.open(Paths.get("Downloads/" + username + "/" + docName + sections + ".txt"),	StandardOpenOption.WRITE);
			else
				outChannel = FileChannel.open(Paths.get("Downloads/" + username + "/" + docName + "_COMPLETE.txt"),	StandardOpenOption.WRITE);
			
			ByteBuffer buffer = ByteBuffer.allocateDirect(1024*1024);
			boolean stop = false;
			
			while(!stop) {
	
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
	        
			clientChannel = null;
			clientChannel = acceptServerSocket();
			
			if(Configurations.DEBUG)
				System.out.println("File scaricato correttamente!");
			
			res = inFromServer.readLine();						//risultato richiesta
		}
		
		switch(res) {
			case "SUCCESS":
				if(sections != Configurations.MAX_SECTIONS)
					JOptionPane.showMessageDialog(null, "Sezione " + sections + " del documento " + docName + " scaricato nella tua cartella personale!", "Success", JOptionPane.OK_OPTION );
				else	
					JOptionPane.showMessageDialog(null, "Documento " + docName + " scaricato nella tua cartella personale!", "Success", JOptionPane.OK_OPTION );
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
				JOptionPane.showMessageDialog(null, "Errore generico.");
				if(Configurations.DEBUG)
					System.err.println(res);
				break;
		}
	}
	
	
	//			***LIST***
	
	public void listRequest() throws IOException {
	
		outToServer.writeBytes("list" + '\n');		//richiesta
		
		outToServer.writeBytes(username + '\n');	//input per richiesta
		
		String res = null, tmp = null;
		int check = 0;
		do {
			tmp = inFromServer.readLine();		//costruisco la stringa finale
			
			if(tmp.length() < 1) {				//Il server manda uno \n per ogni riga (classico writeBytes) ed alla fine 
				check++;						//un ulteriore \n, contandoli so quando ho finito i documenti
				res = res + '\n';
			}
			else {
				check = 0;						//C'è un ulteriore documento, reset
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
					docName = docLabel.getText();						//nome del documento che si desidera modificare
					section = Integer.parseInt(secLabel.getText());		//sezione del documento che si desidera modificare
				}
				else
					return;
			}
			catch (NumberFormatException e) {
				section = -1;
			}
		} while (docName == null || section < 0 || section >= Configurations.MAX_SECTIONS || !docName.matches(Configurations.VALID_CHARACTERS));		//prendi inputs corretti || stop

		outToServer.writeBytes("editDoc" + '\n');				//richiesta
		
		outToServer.writeBytes(username + '\n');				//inputs per la richiesta
		outToServer.writeBytes(docName + '\n');
		outToServer.writeByte(section);
		
		String check = inFromServer.readLine();
		
		if(check.equals("OOB")) {
			JOptionPane.showMessageDialog(null, "Sezione oltre numero sezioni del documento.", "Out Of Bounds", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		String tmp = inFromServer.readLine();					//risultato richiesta
	
		if(tmp.startsWith("2")) {
			
			//DOWNLOAD DEL FILE
			
			File gen = new File("Editing/");				
			if(!gen.exists())
				gen.mkdir();
			
			File x, dir = new File("Editing/" + username);
			if(!dir.exists())
				dir.mkdir();
			
			x = new File("Editing/" + username, docName + section + ".txt");
			
			if(x.exists())
				x.delete();
			x.createNewFile();
			
			FileChannel outChannel = FileChannel.open(Paths.get("Editing/" + username + "/" + docName + section + ".txt"), StandardOpenOption.WRITE);
			ByteBuffer buffer = ByteBuffer.allocateDirect(1024*1024);
			boolean stop = false;
			
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
	        
			clientChannel = null;
			clientChannel = acceptServerSocket();
			res = "SUCCESS";
		}		
		
		else
			res = "ERROR";
		
		switch(res) {
			case "SUCCESS":
				GUIEditClass w = new GUIEditClass(clientSocket, clientChannel, serverSocket, username, tmp, docName, section);
				username = "";
				w.getContentPane().setBackground(Configurations.GUI_BACKGROUND);
				w.setLocation(Configurations.GUI_X_POS, Configurations.GUI_Y_POS);
				w.setVisible(true);
				this.dispose();		//passo alla modalità Editing
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
		
		outToServer.writeBytes("logout" + '\n');				//richiesta
		
		outToServer.writeBytes(username + '\n');				//input per la richiesta

		String res = inFromServer.readLine();					//risultato richiesta
		
		if(!res.equals("ERROR")) {
			username = "";
			l.disable();
			GUIClass w = new GUIClass(clientSocket, clientChannel, serverSocket);			//torno alla schermata di login/register (connessione persistente)
			w.getContentPane().setBackground(Configurations.GUI_LOGIN_BACKGROUND);	
			w.setLocation(Configurations.GUI_X_POS, Configurations.GUI_Y_POS);
			w.setVisible(true);
			this.dispose();
		}
		else if(Configurations.DEBUG)
			JOptionPane.showMessageDialog(null, "Non risulti offline (?).");
	}

}
