import java.awt.Color;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class GUIClass extends JFrame {
	

	public enum Status {
		REGISTERED,
		LOGGED,
		EDITING
	}
	
	//private static BufferedReader inFromUser;
	private static DataOutputStream outToServer;
	private static BufferedReader inFromServer;
	private static Socket clientSocket;
	private static String username;
	private static final long serialVersionUID = 1L;
	
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

	
	public GUIClass(DataOutputStream dos, BufferedReader br, Socket s, String u) {
		outToServer = dos;
		inFromServer = br;
		clientSocket = s;
		username = u;
		loginUI();
	}
	
	public static void main(String[] args) throws IOException {
		
		GUIClass window = new GUIClass();
		window.setLocation(400, 100);
		window.setVisible(true);
	}

	private void init() {
		try {
			//inFromUser = new BufferedReader(new InputStreamReader(System.in));
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
	        File file = new File("img/logo.bmp");
	        BufferedImage image = ImageIO.read(file);
	        logo = new JLabel(new ImageIcon(image));
	        logo.setBounds(120, 5, 300, 250);
	        add(logo);
		}
		catch (IOException e) {
			System.err.println("Logo not found.");
		}
		
		userField = new JTextField();
		passField = new JPasswordField();
		userLabel = new JLabel("Username:");
		passLabel = new JLabel("Password:");
		loginButton = new JButton("Login");
		registerButton = new JButton("Sign Up");
		
		userLabel.setBounds(120, 257, 75, 15);		
		passLabel.setBounds(120, 285, 75, 15);
		
		userField.setBounds(200, 254, 150, 25);
		userField.setColumns(10);
		passField.setBounds(200, 280, 150, 25);

		loginButton.setBounds(223, 320, 100, 25);
		registerButton.setBounds(223, 345, 100, 25);
		
		loginButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent ae){
				try {
					outToServer.writeBytes("login" + '\n');
					loginRequest();
				} catch (IOException e) {
					e.printStackTrace();	//outToServer.writeBytes
				}
			}
		});
		
		registerButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent ae){
				try {
					outToServer.writeBytes("register" + '\n');
					registerRequest();
				} catch (IOException e) {
					e.printStackTrace();	//outToServer.writeBytes
				}
			}
		});
		
		add(userLabel);
		add(passLabel);
		add(registerButton);        
		add(loginButton);
		add(userField);
		add(passField);
	}
	
	public void registerRequest() throws IOException{
        
        String inpUser;
        inpUser = userField.getText();
        if(inpUser.length()==0)
        	JOptionPane.showMessageDialog(null, "Insert Username.");
        outToServer.writeBytes(inpUser + '\n');

        char[] inpPas;
        inpPas = passField.getPassword();
        String inpPass = new String(inpPas);
        if(inpPass.length()==0)
        	JOptionPane.showMessageDialog(null, "Insert Password.");
        outToServer.writeBytes(inpPass + '\n');

        String res = inFromServer.readLine();
		
		if(res.equals("ERROR"))
			JOptionPane.showMessageDialog(null, "Username already exists.");
		else
			JOptionPane.showMessageDialog(null, "User successfully registered!");
	}


	public void loginRequest() throws IOException{
        
        String inpUser;
        inpUser = userField.getText();
        if(inpUser.length()==0) {
        	JOptionPane.showMessageDialog(null, "Insert Username");
        	outToServer.writeBytes("BREAK" + '\n');
        	return;
        }
        outToServer.writeBytes(inpUser + '\n');

        char[] inpPas;
        inpPas = passField.getPassword();
        String inpPass = new String(inpPas);
        if(inpPass.length()==0) {
        	JOptionPane.showMessageDialog(null, "Insert Password");
        	outToServer.writeBytes("BREAK" + '\n');
        	return;
        }
        outToServer.writeBytes(inpPass + '\n');

        String res = inFromServer.readLine();
		
		if(!res.equals("ERROR")) {
			username = inpUser;
			remove(userLabel);
			remove(passLabel);
			remove(registerButton);
			remove(loginButton);
			remove(userField);
			remove(passField);
			remove(logo);
			this.dispose();
			GUILoggedClass w = new GUILoggedClass(outToServer, inFromServer, clientSocket, inpUser);
			w.setLocation(400, 100);
			w.setVisible(true);
		}
		else {
			JOptionPane.showMessageDialog(null, "Wrong Password / Username");
		}
	}
	
	
}