import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class GUILoggedClass extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	private String username;
	private static DataOutputStream outToServer;
	private static BufferedReader inFromServer;
	private static Socket clientSocket;
	private Image createDocImg, inviteImg, showImg, listImg, editImg, logoutImg;
	private JButton createDocButton, inviteButton, editButton, listButton, showButton, logoutButton;
	private JLabel userLabel, stupidJLabel;
	
	public GUILoggedClass(DataOutputStream dos, BufferedReader ifs, Socket s, String username) {
		GUILoggedClass.outToServer = dos;
		GUILoggedClass.inFromServer = ifs;
		GUILoggedClass.clientSocket = s;
		this.username = username;
		clientUI();
	}

	public void clientUI() {

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
		setLayout(null);
		setSize(450,450);
		
		try {
			createDocImg = ImageIO.read(new File("img/add.png"));
			inviteImg = ImageIO.read(new File("img/invite.png"));
			showImg = ImageIO.read(new File("img/show.png"));
			listImg = ImageIO.read(new File("img/list.png"));
			editImg = ImageIO.read(new File("img/edit.png"));
			logoutImg = ImageIO.read(new File("img/logout.png"));
			
			createDocImg = createDocImg.getScaledInstance(100, 100, Image.SCALE_DEFAULT);			
			inviteImg = inviteImg.getScaledInstance(100, 100, Image.SCALE_DEFAULT);			
			showImg = showImg.getScaledInstance(100, 100, Image.SCALE_DEFAULT);			
			listImg = listImg.getScaledInstance(100, 100, Image.SCALE_DEFAULT);			
			editImg = editImg.getScaledInstance(100, 100, Image.SCALE_DEFAULT);			
			logoutImg = logoutImg.getScaledInstance(100, 100, Image.SCALE_DEFAULT);
			
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
			
			createDocButton.setBounds(85,50,115,115);
			inviteButton.setBounds(205,50,115,115);
			showButton.setBounds(85,170,115,115);
			listButton.setBounds(205,170,115,115);
			editButton.setBounds(85,290,115,115);
			logoutButton.setBounds(205,290,115,115);
			
			createDocButton.addActionListener(new ActionListener() { 
				public void actionPerformed(ActionEvent ae){
					try {
						outToServer.writeBytes("createDoc" + '\n');
						createDocRequest();
					} catch (IOException e) {
						e.printStackTrace();	//outToServer.writeBytes
					}
				}
			});
			
			inviteButton.addActionListener(new ActionListener() { 
				public void actionPerformed(ActionEvent ae){
					try {
						outToServer.writeBytes("invite" + '\n');
						inviteRequest();
					} catch (IOException e) {
						e.printStackTrace();	//outToServer.writeBytes
					}
				}
			});
			
			showButton.addActionListener(new ActionListener() { 
				public void actionPerformed(ActionEvent ae){
					try {
						outToServer.writeBytes("show" + '\n');
						showRequest();
					} catch (IOException e) {
						e.printStackTrace();	//outToServer.writeBytes
					}
				}
			});
			
			listButton.addActionListener(new ActionListener() { 
				public void actionPerformed(ActionEvent ae){
					try {
						outToServer.writeBytes("list" + '\n');
						listRequest();
					} catch (IOException e) {
						e.printStackTrace();	//outToServer.writeBytes
					}
				}
			});
			
			editButton.addActionListener(new ActionListener() { 
				public void actionPerformed(ActionEvent ae){
					try {
						outToServer.writeBytes("edit" + '\n');
						editRequest();
					} catch (IOException e) {
						e.printStackTrace();	//outToServer.writeBytes
					}
				}
			});
			
			logoutButton.addActionListener(new ActionListener() { 
				public void actionPerformed(ActionEvent ae){
					try {
						outToServer.writeBytes("logout" + '\n');
						logoutRequest();
					} catch (IOException e) {
						e.printStackTrace();	//outToServer.writeBytes
					}
				}
			});
			
			
			add(createDocButton);
			add(inviteButton);
			add(showButton);
			add(listButton);
			add(editButton);
			add(logoutButton);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
			
		userLabel = new JLabel(username);
		stupidJLabel = new JLabel("Current user:");
		
		stupidJLabel.setBounds(10,10,75,15);
		userLabel.setBounds(90,10,75,15);

		add(stupidJLabel);
		add(userLabel);
	}
	
	public void createDocRequest() throws IOException {
		outToServer.writeBytes(username + '\n');
		
	}
	
	public void inviteRequest() {
		
	}

	public void showRequest() {
		
	}
	
	public void listRequest() {
		
	}

	public void editRequest() {
		
	}

	public void logoutRequest() throws IOException {
		
		outToServer.writeBytes(username + '\n');

		String res = inFromServer.readLine();
		
		if(!res.equals("ERROR")) {
			username = "";
			remove(userLabel);
			remove(stupidJLabel);
			remove(createDocButton);
			remove(inviteButton);
			remove(showButton);
			remove(listButton);
			remove(editButton);
			remove(logoutButton);
			this.dispose();
			GUIClass w = new GUIClass(outToServer, inFromServer, clientSocket, username);
			w.setLocation(400, 100);
			w.setVisible(true);
		}
		else {
			JOptionPane.showMessageDialog(null, "You can't disconnect. Don't ask me why tho");
		}
	}

}
