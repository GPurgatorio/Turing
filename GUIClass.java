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
	
	public JPasswordField passField;
	public JTextField userField;
	public JButton loginButton;
	public JButton registerButton;
	public JLabel userLabel;
	public JLabel passLabel;
	public JLabel logo;

	public static void main(String[] args) throws IOException {
		
		GUIClass window = new GUIClass();
		window.setLocation(400, 100);
		window.setVisible(true);
	}

	public GUIClass() {
		loginUI();
	}

	public void loginUI() {
		
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
			clientUI();
		}
		else {
			JOptionPane.showMessageDialog(null, "Wrong Password / Username");
		}
	}
	
	public void clientUI() {

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
		setLayout(null);
		setSize(450,450);
		
		try {
			Image createDocImg = ImageIO.read(new File("img/add.png"));
			Image inviteImg = ImageIO.read(new File("img/invite.png"));
			Image showImg = ImageIO.read(new File("img/show.png"));
			Image listImg = ImageIO.read(new File("img/list.png"));
			Image editImg = ImageIO.read(new File("img/edit.png"));
			Image logoutImg = ImageIO.read(new File("img/logout.png"));
			
			createDocImg = createDocImg.getScaledInstance(100, 100, Image.SCALE_DEFAULT);			
			inviteImg = inviteImg.getScaledInstance(100, 100, Image.SCALE_DEFAULT);			
			showImg = showImg.getScaledInstance(100, 100, Image.SCALE_DEFAULT);			
			listImg = listImg.getScaledInstance(100, 100, Image.SCALE_DEFAULT);			
			editImg = editImg.getScaledInstance(100, 100, Image.SCALE_DEFAULT);			
			logoutImg = logoutImg.getScaledInstance(100, 100, Image.SCALE_DEFAULT);
			
			JButton createDocButton = new JButton();
			createDocButton.setIcon(new ImageIcon(createDocImg));
			JButton inviteButton = new JButton();
			inviteButton.setIcon(new ImageIcon(inviteImg));
			JButton showButton = new JButton();
			showButton.setIcon(new ImageIcon(showImg));
			JButton listButton = new JButton();
			listButton.setIcon(new ImageIcon(listImg));
			JButton editButton = new JButton();
			editButton.setIcon(new ImageIcon(editImg));
			JButton logoutButton = new JButton();
			logoutButton.setIcon(new ImageIcon(logoutImg));
			
			createDocButton.setBounds(85,50,115,115);
			inviteButton.setBounds(205,50,115,115);
			showButton.setBounds(85,170,115,115);
			listButton.setBounds(205,170,115,115);
			editButton.setBounds(85,290,115,115);
			logoutButton.setBounds(205,290,115,115);
			
			add(createDocButton);
			add(inviteButton);
			add(showButton);
			add(listButton);
			add(editButton);
			add(logoutButton);
		}
		catch (IOException e) {
			;
		}
			
		JLabel userLabel = new JLabel(username);
		JLabel stupidJLabel = new JLabel("Current user:");
		
		stupidJLabel.setBounds(10,10,75,15);
		userLabel.setBounds(90,10,75,15);

		add(stupidJLabel);
		add(userLabel);
	}
}