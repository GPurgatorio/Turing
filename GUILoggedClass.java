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
import javax.swing.JTextField;

public class GUILoggedClass extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	private String username;
	private static DataOutputStream outToServer;
	private static BufferedReader inFromServer;
	private static Socket clientSocket;
	private Image createDocImg, inviteImg, showImg, listImg, editImg, logoutImg;
	private JButton createDocButton, inviteButton, editButton, listButton, showButton, logoutButton;
	private JLabel userLabel;
	
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
			inviteButton.setBounds(210,50,115,115);
			showButton.setBounds(85,170,115,115);
			listButton.setBounds(210,170,115,115);
			editButton.setBounds(85,290,115,115);
			logoutButton.setBounds(210,290,115,115);
			
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
			
		userLabel = new JLabel("Current user: " + username);
		userLabel.setBounds(10,10,105,15);
		add(userLabel);
	}
	
	public void createDocRequest() throws IOException {
		
		JTextField docLabel = new JTextField();
		JTextField secLabel = new JTextField();
		
		Object[] struct = {
				"Nome Documento:", docLabel,
				"Numero Sezioni [1-9]:", secLabel
		};
		
		
		String res, docName = null;
		int sections; 
		
		do {
			try {
				
				int option = JOptionPane.showConfirmDialog(null, struct, "Creazione Documento", JOptionPane.OK_CANCEL_OPTION);
				
				if (option == JOptionPane.OK_OPTION) {
					docName = docLabel.getText();
					sections = Integer.parseInt(secLabel.getText());
				}
				
				else
					return;
			}
			catch (NumberFormatException e) {
				sections = -1;
			}
		} while (sections < 1 || sections > 9 || docName == null);
		
		outToServer.writeBytes(username + '\n');
		outToServer.writeBytes(docName + '\n');
		outToServer.writeByte(sections);
		
		res = inFromServer.readLine();
		
		switch(res) {
			case "SUCCESS":
				JOptionPane.showMessageDialog(null, "Documento creato con successo!");
				break;
			case "DOC_EXISTS":
				JOptionPane.showMessageDialog(null, "Nome del Documento già presente nel database.");
				break;
			case "HACKER":
				JOptionPane.showMessageDialog(null, "Non risulti registrato.");
				break;
			case "ERROR":
				JOptionPane.showMessageDialog(null, "Errore generico non specificato");
				break;
			default:
				JOptionPane.showMessageDialog(null, "This should never happen");
				break;
		}
		
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
