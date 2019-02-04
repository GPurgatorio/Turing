import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class GUIEditClass extends JFrame {

	private static final long serialVersionUID = 1L;

	private String username;
	private String docName;
	private int section;
	private JButton sendMsgButton, endEditButton;
	private JLabel userLabel;
	private JTextArea chatArea, msgArea; 
	private JScrollPane scrollPane; 
	private Image endEditImg, sendMsgImg;
	private DataOutputStream outToServer; 
	private BufferedReader inFromServer;
	private Socket clientSocket;
	private MulticastSocket chatSocket;
	private InetAddress group;
	private int port;
	private Chat c;
	
	public GUIEditClass(DataOutputStream ots, BufferedReader ifs, Socket s, String usr, String addr, String doc, int sec) throws IOException {
		outToServer = ots;
		inFromServer = ifs;
		clientSocket = s;
		username = usr;
		docName = doc;
		section = sec;
		
		port = 4321;			//(int) (Math.random() * 16383) + 49152;
		chatSocket = new MulticastSocket(port);
		group = InetAddress.getByName(addr);
		
		chatArea = new JTextArea();
		chatArea.setEditable(false);
		chatArea.setLineWrap(true);
		chatArea.setWrapStyleWord(true);
		msgArea = new JTextArea();
		msgArea.setEditable(true);
		msgArea.setLineWrap(true);
		msgArea.setWrapStyleWord(true);
		scrollPane = new JScrollPane(chatArea);
		scrollPane.setVisible(true);
		
		ExecutorService e = Executors.newFixedThreadPool(1);
		c = new Chat(chatArea, chatSocket, group);
		e.execute(c);
		
		connectAlert(username + " si è connesso.");
		
		editUI();
	}

	private void editUI() throws IOException {
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
		setLayout(null);
		setSize(450,450);

		endEditImg = ImageIO.read(new File("img/upload.png"));
		sendMsgImg = ImageIO.read(new File("img/sendmsg.png"));

		endEditImg = endEditImg.getScaledInstance(40, 40, Image.SCALE_DEFAULT);
		sendMsgImg = sendMsgImg.getScaledInstance(40, 40, Image.SCALE_DEFAULT);
		
		sendMsgButton = new JButton();
		endEditButton = new JButton();
		
		sendMsgButton.setIcon(new ImageIcon(sendMsgImg));
		endEditButton.setIcon(new ImageIcon(endEditImg));
		
		chatArea.setBounds(35, 40, 365, 290);
		msgArea.setBounds(90, 340, 255, 50);
		sendMsgButton.setBounds(350, 340, 50, 50);
		endEditButton.setBounds(35, 340, 50, 50);
		
		sendMsgButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent ae) {
				try {
					sendMsg();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		
		endEditButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent ae) {
				//uploadFile
				c.disable();
				try {
					chatSocket.leaveGroup(group);
					loggedUI();
				} catch (IOException e) { e.printStackTrace(); }
			}
		});

		add(msgArea);
		add(chatArea);
		add(sendMsgButton);
		add(endEditButton);
		
		userLabel = new JLabel("Current user: " + username);
		userLabel.setBounds(13, 13, 200, 15);
		add(userLabel);
		
		SwingUtilities.getRootPane(sendMsgButton).setDefaultButton(sendMsgButton);
	}

	private void sendMsg() throws IOException {
		
		String msg = "[ " + username + " ]: " + msgArea.getText();
		if(msg.length() > 0) {
			byte[] m = msg.getBytes();
			DatagramPacket packet = new DatagramPacket(m, m.length, group, port);
			chatSocket.send(packet);
		}
		
		msgArea.setText("");
	}
	
	private void connectAlert(String msg) throws IOException {
		if(msg.length() > 0) {
			//crypt(msg)			se avanza tempo
			byte[] m = msg.getBytes();
			DatagramPacket packet = new DatagramPacket(m, m.length, group, port);
			chatSocket.send(packet);
		}
	}

	private void loggedUI() throws IOException {
		outToServer.writeBytes("endEdit" + '\n');
		
		outToServer.writeBytes(username + '\n');
		outToServer.writeBytes(docName + '\n');
		outToServer.writeByte(section);
		
		String res = inFromServer.readLine();
		
		if(res == "SUCCESS") {
			this.dispose();
			GUILoggedClass w = new GUILoggedClass(outToServer, inFromServer, clientSocket, username);
			w.getContentPane().setBackground(new java.awt.Color(194, 194, 163));	//new java.awt.Color(173, 178, 184));
			w.setLocation(400, 100);
			w.setVisible(true);
		}
	}
}
