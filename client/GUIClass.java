package client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import server.Configurations;
import server.RegistrationInterface;

public class GUIClass extends JFrame {

	private static final long serialVersionUID = 1L;
	
	// Client-Server related
	private static Socket clientSocket;	
	private static DataOutputStream outToServer;
	private static BufferedReader inFromServer;
	private static SocketChannel clientChannel;	
	private static ServerSocketChannel serverSocket;
	private boolean offline;
	
	// User Interface related
	private JPasswordField passField;
	private JTextField userField;
	private JButton loginButton;
	private JButton registerButton;
	private JLabel userLabel;
	private JLabel passLabel;
	private JLabel logo;

	
	// Costruttore
	public GUIClass() {
		init();
		loginUI();
	}
	
	
	// Inizializzazioni varie
	private void init() {
		
		offline = false;			
		clientChannel = null;
		
		try {
			clientSocket = new Socket("localhost", Configurations.DEFAULT_PORT);
			outToServer = new DataOutputStream(clientSocket.getOutputStream());
			inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		}
		catch (Exception e) {		//se avviene un problema, mostro un avvertimento ed impedisco ulteriori azioni settando il booleano
			JOptionPane.showMessageDialog(null, "Il server è offline!", "Server offline", JOptionPane.ERROR_MESSAGE);
			offline = true;
		}
	}
	
	
	// Main del client, semplicemente chiama il primo costruttore e mostra la User Interface
	public static void main(String[] args) throws IOException {
		
		GUIClass window = new GUIClass();
		window.getContentPane().setBackground(Configurations.GUI_LOGIN_BACKGROUND);		
		window.setLocation(Configurations.GUI_X_POS, Configurations.GUI_Y_POS);
		window.setVisible(true);
	}


	// Interfaccia grafica
	private void loginUI() {
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 	// Per terminare l'applicazione quando viene chiusa
		setLayout(null);									// Per gestire manualmente tutta l'interfaccia
		setSize(550,450);
		
		// Carica logo
		try {
	        File file = new File("img/logo.png");
	        BufferedImage image = ImageIO.read(file);
	        logo = new JLabel(new ImageIcon(image));
	        logo.setBounds(120, 5, 300, 270);
	        add(logo);
		}
		catch (IOException e) {	if(Configurations.DEBUG) System.err.println("Logo non trovato."); }
		
		userField = new JTextField();
		passField = new JPasswordField();
		userLabel = new JLabel("Username:");
		passLabel = new JLabel("Password:");
		loginButton = new JButton("Login");
		registerButton = new JButton("Sign Up");
		
		userLabel.setBounds(120, 282, 75, 15);		
		passLabel.setBounds(120, 309, 75, 15);
		
		userField.setBounds(200, 278, 150, 25);
		userField.setColumns(10);
		passField.setBounds(200, 304, 150, 25);

		loginButton.setBounds(223, 340, 100, 25);
		registerButton.setBounds(223, 365, 100, 25);
		
		loginButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent ae) {
				if(checkTextFields()) {			
					try {
						loginRequest();
					} catch (IOException e) { e.printStackTrace(); }
				}
			}
		});
		
		registerButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent ae) {
				try {
					registerRequest();
				} catch (Exception e) {	e.printStackTrace(); }
			}
		});
		
		add(userLabel);
		add(passLabel);
		add(registerButton);        
		add(loginButton);
		add(userField);
		add(passField);
		
		// Premendo la Enter Key di default chiamerà il bottone di Login
		SwingUtilities.getRootPane(loginButton).setDefaultButton(loginButton);		
	}
	
	
	// Semplice controllo (in locale) sugli inputs
	private boolean checkTextFields() {			
		
		if(userField.getText().length() == 0) {
			JOptionPane.showMessageDialog(null, "Inserisci un Username.", "Error", JOptionPane.ERROR_MESSAGE);
        	return false;
		}
		if(passField.getPassword().length == 0) {
			JOptionPane.showMessageDialog(null, "Inserisci una Password.", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}
	
	// Richiesta di registrazione (RMI)
	public void registerRequest() throws NotBoundException, RemoteException{
		
		// Se vi è stato un problema (es. Server offline) impedisce di continuare
		if(offline) {
			JOptionPane.showMessageDialog(null, "Il server è offline. Chiudi il client e riprova.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		// Altrimenti prende gli inputs Username e Password e svolge ulteriori controlli (locali) sulla loro validità
        String inpUser;
        inpUser = userField.getText();
        if(inpUser.length() == 0) {
        	JOptionPane.showMessageDialog(null, "Inserisci un Username.", "Error", JOptionPane.ERROR_MESSAGE);
        	return;
        }
        
        if(!inpUser.matches(Configurations.VALID_CHARACTERS)) { 
        	JOptionPane.showMessageDialog(null, "Il campo Username presenta caratteri non validi.", "Error", JOptionPane.ERROR_MESSAGE);
        	return;
		}
        
        if(inpUser.length() > Configurations.MAX_LENGTH) {
        	JOptionPane.showMessageDialog(null, "Non puoi registrare un Username così lungo. Riprova", "Warning", JOptionPane.WARNING_MESSAGE);
        	return;
        }

        
        char[] inpPas;
        inpPas = passField.getPassword();
        String inpPass = new String(inpPas);
        
        if(inpPass.length() == 0) {
        	JOptionPane.showMessageDialog(null, "Inserisci una Password.", "Error", JOptionPane.ERROR_MESSAGE);
        	return;
        }
        
        if(!inpPass.matches(Configurations.VALID_CHARACTERS)) { 
        	JOptionPane.showMessageDialog(null, "Il campo Password presenta caratteri non validi.", "Error", JOptionPane.ERROR_MESSAGE);
        	return;
		}
        
        if(inpPass.length() > Configurations.MAX_LENGTH) {
        	JOptionPane.showMessageDialog(null, "Non puoi registrare una Password così lunga. Riprova", "Warning", JOptionPane.WARNING_MESSAGE);
        	return;
        }


        // Gli inputs sono coerenti con le specifiche, quindi si passa alla richiesta di Registrazione vera e propria
        Registry registry = LocateRegistry.getRegistry(Configurations.REGISTRATION_PORT);
        RegistrationInterface stub = (RegistrationInterface) registry.lookup(RegistrationInterface.SERVICE_NAME);
        boolean res = stub.registerRequest(inpUser, inpPass);
		
		if(res)
			JOptionPane.showMessageDialog(null, "Utente registrato con successo!", "Success", JOptionPane.INFORMATION_MESSAGE);
		else
			JOptionPane.showMessageDialog(null, "Esiste già un utente registrato con questo nome.", "Error", JOptionPane.ERROR_MESSAGE);
		
	}
	
	
	// Richiesta di Login (TCP)
	public void loginRequest() throws IOException {
		
		// Se vi è stato un problema (es. Server offline) impedisce di continuare
		if(offline) {		
			JOptionPane.showMessageDialog(null, "Il server è offline. Chiudi il client e riprova.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		outToServer.writeBytes("login" + '\n');			// Comando per iniziare la richiesta
		
        String inpUser, inpPass;
        char[] inpPas;
        inpUser = userField.getText();        			// N.B: qua non serve alcun controllo sugli inputs, in quanto per poter accedere
        outToServer.writeBytes(inpUser + '\n');			// 		serve una registrazione che abbia passato tali controlli

        inpPas = passField.getPassword();
        inpPass = new String(inpPas);
        outToServer.writeBytes(inpPass + '\n');

        String res = inFromServer.readLine();			// Risposta del server
		
        // Se l'esito della richiesta è positivo, cambio la schermata nella modalità di Gestione
		if(res.equals("SUCCESS")) {						
			
			GUILoggedClass w = new GUILoggedClass(clientSocket, clientChannel, serverSocket, inpUser, "login");
			w.getContentPane().setBackground(Configurations.GUI_BACKGROUND);	
			w.setLocation(Configurations.GUI_X_POS, Configurations.GUI_Y_POS);
			w.setVisible(true);
			this.dispose();								// Rilascia le risorse della finestra corrente (non di GUILoggedClass)
			
			if(Configurations.DEBUG)
				System.out.println("GUIClass: fine, passo in Logged");
		}
		
		// Altrimenti la richiesta ha avuto esito negativo, informo l'utente a seconda del tipo di errore
		else if(res.equals("UNKNWN_USR"))
			JOptionPane.showMessageDialog(null, "Username o Password errati.", "Error", JOptionPane.ERROR_MESSAGE);
		else if(res.equals("LOGGED_ALRD"))
			JOptionPane.showMessageDialog(null, "L'utente risulta essere già connesso.", "Error", JOptionPane.ERROR_MESSAGE);
		else 
			JOptionPane.showMessageDialog(null, "Errore generico.", "Error", JOptionPane.ERROR_MESSAGE);
	}
}