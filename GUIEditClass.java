import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.util.Calendar;
import java.util.TimeZone;

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
	private Chat c;
	private Calendar calendar;
	
	public GUIEditClass(Socket s, String usr, String addr, String doc, int sec) throws IOException {
		clientSocket = s;
		username = usr;
		docName = doc;
		section = sec;
		
		try {
			outToServer = new DataOutputStream(clientSocket.getOutputStream());
			inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		} catch (IOException e) { e.printStackTrace(); }
		
		downloadFile();
		
		chatSocket = new MulticastSocket(Configurations.MULTICAST_PORT);
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
		
		c = new Chat(username, chatArea, chatSocket, group);
		c.start();
		
		editUI();
	}

	private void downloadFile() {
		// TODO Auto-generated method stub
		
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
				} catch (IOException e) { e.printStackTrace(); }
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
		
		addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
		    	try {
					disconnect();
				} catch (IOException e) { e.printStackTrace(); }
		    }
		});
		
		msgArea.addKeyListener(new KeyListener(){

            public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER) {
				      try {
						sendMsg();
					} catch (IOException e1) { e1.printStackTrace(); }
				}
            }
            
			@Override
			public void keyReleased(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER) 
					msgArea.setText("");
			}
			@Override
			public void keyTyped(KeyEvent e) {;}
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

	protected void disconnect() throws IOException {
		System.err.println("Disconnect");
		String finalMsg = username + " si � disconnesso.\n";
		byte[] m = finalMsg.getBytes();
		DatagramPacket packet = new DatagramPacket(m, m.length, group, Configurations.MULTICAST_PORT);
		chatSocket.send(packet);
		chatSocket.leaveGroup(group);
	}

	private void sendMsg() throws IOException {
		calendar = Calendar.getInstance(TimeZone.getDefault());
		int hour = calendar.get(Calendar.HOUR);
		int minute = calendar.get(Calendar.MINUTE);
		
		String input = msgArea.getText();
		String msg = "[" + username + " " + hour + ":" + minute + "]: " + input;
		
		if(input.length() > 0) {
			byte[] m = msg.getBytes();
			DatagramPacket packet = new DatagramPacket(m, m.length, group, Configurations.MULTICAST_PORT);
			chatSocket.send(packet);
		}
		
		msgArea.setText("");
	}

	private void loggedUI() throws IOException {
		outToServer.writeBytes("endEdit" + '\n');
		
		outToServer.writeBytes(username + '\n');
		outToServer.writeBytes(docName + '\n');
		outToServer.writeByte(section);
		
		String res = inFromServer.readLine();
		
		if(res == "SUCCESS") {
			c.disable();
			this.dispose();
			GUILoggedClass w = new GUILoggedClass(clientSocket, username);
			w.getContentPane().setBackground(Configurations.GUI_BACKGROUND);	
			w.setLocation(Configurations.GUI_X_POS, Configurations.GUI_Y_POS);
			w.setVisible(true);
		}
	}
}
