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
import java.net.Socket;

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
	
	private String username;
	private static DataOutputStream outToServer;
	private static BufferedReader inFromServer;
	private static Socket clientSocket;
	private static NotSoGUIListener l;
	private Image createDocImg, inviteImg, showImg, listImg, editImg, logoutImg;
	private JButton createDocButton, inviteButton, editButton, listButton, showButton, logoutButton;
	private JLabel userLabel;
	
	public GUILoggedClass(Socket s, String usr) throws IOException {
		clientSocket = s;
		outToServer = new DataOutputStream(clientSocket.getOutputStream());
		inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		
		username = usr;
		clientUI();
		invitesListener();
		checkPendingInvites();
		
		if(Configurations.DEBUG)			//Per evitare confusione tra le varie consoles, un punto di riferimento
			System.out.println("Console di: " + username);
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
		
		l = new NotSoGUIListener(pendSocket, username);
		l.start();
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
	
	public void createDocRequest() throws IOException {
		
		JTextField docLabel = new JTextField();
		JTextField secLabel = new JTextField();
		
		Object[] struct = {
				"Nome Documento:", docLabel,
				"Numero Sezioni [1-" + Configurations.MAX_SECTIONS + "]:", secLabel
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
		} while (sections < 1 || sections > Configurations.MAX_SECTIONS || docName == null || !docName.matches("[a-zA-Z0-9]+"));	//prendi inputs corretti || stop
		
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
				JOptionPane.showMessageDialog(null, "Un documento con nome " + docName + " risulta già presente. Cambia nome!");
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
		} while (docName == null || user == null || !docName.matches("[a-zA-Z0-9]+") || !user.matches("[a-zA-Z0-9]+"));				//prendi inputs corretti || stop
		
		outToServer.writeBytes("invite" + '\n');				//richiesta
		
		outToServer.writeBytes(username + '\n');				//inputs per la richiesta
		outToServer.writeBytes(user + '\n');
		outToServer.writeBytes(docName + '\n');
		
		res = inFromServer.readLine();							//risultato richiesta
		
		/*
		if(res == null) {
			if(Configurations.DEBUG)
				System.err.println("res è null");
			return;
		}*/
		
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

	public void showRequest() throws IOException {
		
		JTextField docLabel = new JTextField();
		JTextField secLabel = new JTextField();
		
		Object[] struct = {
				"Nome Documento:", docLabel,
				"Sezione: (se > " + Configurations.MAX_SECTIONS + " -> doc completo):", secLabel
		};
		
		String res, docName = null;
		int sections;
		
		do {
			try {
				sections = 255;
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
		} while (docName == null || sections < 0 || sections > 255 || !docName.matches("[a-zA-Z0-9]+"));	//prendi inputs corretti || stop
		
		outToServer.writeBytes("show" + '\n');				//richiesta
		
		outToServer.writeBytes(username + '\n');			//inputs per la richiesta
		outToServer.writeBytes(docName + '\n');
		outToServer.writeByte(sections);		
		
		res = inFromServer.readLine();						//risultato richiesta
		
		switch(res) {
			case "SUCCESS":
				if(sections <= Configurations.MAX_SECTIONS)
					JOptionPane.showMessageDialog(null, "Sezione " + sections + " del documento " + docName + " scaricato nella tua cartella personale!", "Success", JOptionPane.OK_OPTION );
				else	
					JOptionPane.showMessageDialog(null, "Documento " + docName + " scaricato nella tua cartella personale!", "Success", JOptionPane.OK_OPTION );
				break;
			case "EDITING":
				JOptionPane.showMessageDialog(null, "Documento " + docName + " scaricato. Qualcuno ci sta lavorando sopra!", "Section Not Up To Date", JOptionPane.INFORMATION_MESSAGE);
				break;
			case "NOT_EXIST":
				JOptionPane.showMessageDialog(null, "Il documento " + docName + " non esiste", "Error", JOptionPane.ERROR_MESSAGE);
				break;
			default:
				JOptionPane.showMessageDialog(null, "Errore generico.");
				if(Configurations.DEBUG)
					System.err.println(res);
				break;
		}
	}
	
	public void listRequest() throws IOException {
	
		outToServer.writeBytes("list" + '\n');		//richiesta
		
		outToServer.writeBytes(username + '\n');	//input per richiesta
		
		String res = null, tmp = null;
		int check = 0;
		do {
			tmp = inFromServer.readLine();		//costruisco la stringa finale
			
				if(tmp.length() < 1) {			//Il server manda uno \n per ogni riga (classico writeBytes) ed alla fine 
					check++;					//un ulteriore \n, contandoli so quando ho finito i documenti
				res = res + '\n';
			}
			else {
				check = 0;						//C'è un ulteriore documento, reset
				if(res == null)
					res = tmp + '\n';
				else
					res = res + tmp + '\n';
			}
		} while(check < 2);

		if(res != null) 
			tmp = "SUCCESS";
		
		switch(tmp) {
			case "SUCCESS":
				JOptionPane.showMessageDialog(null, "Lista Documenti: \n\n" + res );
				break;
				//TODO
			default:
				JOptionPane.showMessageDialog(null, "Errore generico.");
				if(Configurations.DEBUG)
					System.err.println(res);
				break;
		}
	}

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
		} while (docName == null || section < 0 || section > Configurations.MAX_LENGTH || !docName.matches("[a-zA-Z0-9]+"));		//prendi inputs corretti || stop

		outToServer.writeBytes("editDoc" + '\n');				//richiesta
		
		outToServer.writeBytes(username + '\n');				//inputs per la richiesta
		outToServer.writeBytes(docName + '\n');
		outToServer.writeByte(section);
		
		String tmp = inFromServer.readLine();					//risultato richiesta
		
		if(tmp.startsWith("2"))									//è un indirizzo
			res = "SUCCESS";									//TODO: change con regex dell'es di laboratorio Weblog se hai tempo
		
		else
			res = "ERROR";
		
		switch(res) {
			case "SUCCESS":
				this.dispose();		//passo alla modalità Editing
				GUIEditClass w = new GUIEditClass(clientSocket, username, tmp, docName, section);
				username = "";
				w.getContentPane().setBackground(Configurations.GUI_BACKGROUND);
				w.setLocation(Configurations.GUI_X_POS, Configurations.GUI_Y_POS);
				w.setVisible(true);
				break;
			//TODO
			default:
				JOptionPane.showMessageDialog(null, "Errore generico.");
				if(Configurations.DEBUG)
					System.err.println(tmp);
				break;
		}
	}

	public void logoutRequest() throws IOException {
		
		outToServer.writeBytes("logout" + '\n');				//richiesta
		
		outToServer.writeBytes(username + '\n');				//input per la richiesta

		String res = inFromServer.readLine();					//risultato richiesta
		
		if(!res.equals("ERROR")) {
			username = "";
			l.disable();
			this.dispose();
			GUIClass w = new GUIClass(clientSocket);			//torno alla schermata di login/register (connessione persistente)
			w.getContentPane().setBackground(Configurations.GUI_LOGIN_BACKGROUND);	
			w.setLocation(Configurations.GUI_X_POS, Configurations.GUI_Y_POS);
			w.setVisible(true);
		}
		else if(Configurations.DEBUG)
			JOptionPane.showMessageDialog(null, "Non risulti offline (?).");
	}

}
