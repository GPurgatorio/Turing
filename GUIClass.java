import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
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

public class GUIClass extends JFrame {

	private static final long serialVersionUID = 1L;
	private static int REGISTRATION_PORT = 1099;
	
	private static DataOutputStream outToServer;
	private static BufferedReader inFromServer;
	private static Socket clientSocket;
	
	private JPasswordField passField;
	private JTextField userField;
	private JButton loginButton;
	private JButton registerButton;
	private JLabel userLabel;
	private JLabel passLabel;
	private JLabel logo;

	public GUIClass() {
		init();
		loginUI();
	}
	
	public GUIClass(DataOutputStream dos, BufferedReader br, Socket s) {
		outToServer = dos;
		inFromServer = br;
		clientSocket = s;
		loginUI();
	}
	
	public static void main(String[] args) throws IOException {
		
		GUIClass window = new GUIClass();
		window.getContentPane().setBackground(new java.awt.Color(4, 167, 210));		
		window.setLocation(400, 100);
		window.setVisible(true);
	}

	private void init() {
		try {
			clientSocket = new Socket("127.0.0.1", 6789);
			outToServer = new DataOutputStream(clientSocket.getOutputStream());
			inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		}
		catch (Exception e) {
			System.err.println("Server is not up");
			this.dispose();
		}
	}

	private void loginUI() {
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
		setLayout(null);
		setSize(550,450);
		
		try {
	        File file = new File("img/logo.png");
	        BufferedImage image = ImageIO.read(file);
	        logo = new JLabel(new ImageIcon(image));
	        logo.setBounds(120, 5, 300, 270);
	        add(logo);
		}
		catch (IOException e) {
			System.err.println("Logo non trovato.");
		}
		
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
					} catch (IOException e) {
						e.printStackTrace();	//outToServer.writeBytes
					}
				}
			}
		});
		
		registerButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent ae) {
				try {
					registerRequest();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		add(userLabel);
		add(passLabel);
		add(registerButton);        
		add(loginButton);
		add(userField);
		add(passField);
		
		SwingUtilities.getRootPane(loginButton).setDefaultButton(loginButton);
	}
	
	public void registerRequest() throws NotBoundException, RemoteException{
		
        String inpUser;
        inpUser = userField.getText();
        if(inpUser.length()==0) {
        	JOptionPane.showMessageDialog(null, "Inserisci un Username.", "Error", JOptionPane.ERROR_MESSAGE);
        	return;
        }

        char[] inpPas;
        inpPas = passField.getPassword();
        String inpPass = new String(inpPas);
        if(inpPass.length()==0) {
        	JOptionPane.showMessageDialog(null, "Inserisci una Password.", "Error", JOptionPane.ERROR_MESSAGE);
        	return;
        }
        
        Registry registry = LocateRegistry.getRegistry(REGISTRATION_PORT);
        RegistrationInterface stub = (RegistrationInterface) registry.lookup(RegistrationInterface.SERVICE_NAME);
        boolean res = stub.registerRequest(inpUser, inpPass);
		
		if(res)
			JOptionPane.showMessageDialog(null, "Utente registrato con successo!");
		else
			JOptionPane.showMessageDialog(null, "Esiste già un utente registrato con questo nome.");
		
	}

	private boolean checkTextFields() {
		
		if(userField.getText().length()==0) {
        	JOptionPane.showMessageDialog(null, "Inserisci Username");
        	return false;
		}
        	
		if(passField.getPassword().length==0) {
			JOptionPane.showMessageDialog(null, "Inserisci Password");
			return false;
		}
		
		return true;
	}
	
	public void loginRequest() throws IOException{
		
		outToServer.writeBytes("login" + '\n');
		
        String inpUser, inpPass;
        char[] inpPas;
        inpUser = userField.getText();        
        outToServer.writeBytes(inpUser + '\n');

        inpPas = passField.getPassword();
        inpPass = new String(inpPas);
        outToServer.writeBytes(inpPass + '\n');

        String res = inFromServer.readLine();
		
		if(res.equals("SUCCESS")) {
			this.dispose();
			GUILoggedClass w = new GUILoggedClass(outToServer, inFromServer, clientSocket, inpUser);
			w.getContentPane().setBackground(new java.awt.Color(173, 178, 184));
			w.setLocation(400, 100);
			w.setVisible(true);
		}
		
		else if(res.equals("UNKNWN_USR"))
			JOptionPane.showMessageDialog(null, "Username o Password errati.");
		
		else if(res.equals("LOGGED_ALRD"))
			JOptionPane.showMessageDialog(null, "You are already logged in.");
		
		else
			JOptionPane.showMessageDialog(null, "Something weird happened.");
		
	}
	
	
}