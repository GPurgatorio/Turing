package client;

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
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Calendar;
import java.util.TimeZone;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.JTextArea;
//import java.awt.Dimension;
//import javax.swing.ScrollPaneConstants;
//import javax.swing.JScrollPane;

import server.Configurations;

public class GUIEditClass extends JFrame {

	private static final long serialVersionUID = 1L;

	// Client related
	private String username;				// Chi sta lavorando
	private String docName;					// Nome del documento che sta venendo modificato (e che quindi verrà caricato)
	private int section;					// Sezione del documento
	
	// Client-Server related
	private Socket clientSocket;			// Socket per la gestione TCP
	private DataOutputStream outToServer; 
	private BufferedReader inFromServer;
	private SocketChannel clientChannel;	// Socket diretta verso il server per salvare le modifiche
	private MulticastSocket chatSocket;		// Socket per la chat Multicast UDP
	private InetAddress group;
	private Chat c;							// Listener per la chat
	private Calendar calendar;				// Per poter mettere l'orario nei messaggi
	
	// User Interface related
	private JButton sendMsgButton, endEditButton;
	private JLabel userLabel;
	private JTextArea chatArea, msgArea; 
	private Image endEditImg, sendMsgImg;
	//private JScrollPane scrollPane; 
	
	
	// Costruttore
	public GUIEditClass(Socket s, SocketChannel sc, String usr, String addr, String doc, int sec) throws IOException {
		
		clientSocket = s;
		clientChannel = sc;
		username = usr;
		docName = doc;
		section = sec;
		
		if(clientChannel == null)
			clientChannel = createChannel();
		
		try {
			outToServer = new DataOutputStream(clientSocket.getOutputStream());
			inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		} catch (IOException e) { e.printStackTrace(); }
		
		chatSocket = new MulticastSocket(Configurations.MULTICAST_PORT);
		group = InetAddress.getByName(addr);
		
		chatArea = new JTextArea(320,240);
		chatArea.setEditable(false);
		chatArea.setLineWrap(true);
		chatArea.setWrapStyleWord(true);
		
		msgArea = new JTextArea();
		msgArea.setEditable(true);
		msgArea.setLineWrap(true);
		msgArea.setWrapStyleWord(true);
		
		/* 
		 * con setLayout(null) non sembra essere possibile, essendo una cosa semplicemente grafica lascio perdere
		 * 
		scrollPane = new JScrollPane(chatArea);
		scrollPane.setPreferredSize(new Dimension(320,240));
		scrollPane.setAlignmentX(35);
		scrollPane.setAlignmentY(40);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		add(scrollPane);
		*/
		
		// Thread di supporto Sniffer Multicast, starà in attesa di messaggi per appenderli alla chatArea
		c = new Chat(username, chatArea, chatSocket, group);		
		c.start();
		
		editUI();
	}

	//User Interface
	private void editUI() throws IOException {
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 		// Per terminare l'applicazione quando viene chiusa
		setLayout(null);										// Per gestire manualmente tutta l'interfaccia
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
				c.disable();								// Interrompe il listener della chat
				try {
					disconnect();							// Manda il messaggio di disconnessione agli altri utenti
					loggedUI();								// Torna all'interfaccia precedente
				} catch (IOException e) { e.printStackTrace(); }
			}
		});
		
		addWindowListener(new java.awt.event.WindowAdapter() {		
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {		// In caso di chiusura della finestra
		    	try {
					disconnect();
				} catch (IOException e) { e.printStackTrace(); }
		    }
		});
		
		msgArea.addKeyListener(new KeyListener(){

            public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER) {			// Se viene premuto Invio mentre si è sulla msgArea
				      try {											// Invia il contenuto
						sendMsg();
				      } catch (IOException e1) { e1.printStackTrace(); }
				}
            }
            
			@Override
			public void keyReleased(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER) 			// Approssimazione del pulire una text area dopo aver inviato il messaggio
					msgArea.setText("");							// Viene inserito qua e non direttamente in sendMsg in quanto
			}														// altrimenti avremmo sempre una newline dopo l'invio di ciascun msg
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
																					// Se non si ha il controllo direttamente sulla
		SwingUtilities.getRootPane(sendMsgButton).setDefaultButton(sendMsgButton);	// msgArea, premere Invio chiama questo bottone	
	}
	
	
	private SocketChannel createChannel() throws IOException {
		SocketChannel socketChannel = SocketChannel.open();
		int port = username.hashCode() % 65535;
        if(port < 0) 
        	port = -port % 65535;
        if(port < 1024)		//evito le porte "Well-known" [0-1023]
        	port += 1024;
        SocketAddress socketAddr = new InetSocketAddress("localhost", port);
        socketChannel.connect(socketAddr);
        return socketChannel;
	}
	
	
	// Invia il testo presente nella msgArea
	private void sendMsg() throws IOException {
		
		calendar = Calendar.getInstance(TimeZone.getDefault());
		int hour = calendar.get(Calendar.HOUR);
		int minute = calendar.get(Calendar.MINUTE);
		
		String input = msgArea.getText();
		String msg = "[" + username + " " + hour + ":" + minute + "]: " + input;
		
		if(input.length() > 0) {			// Se effettivamente si sta inviando qualcosa..
			byte[] m = msg.getBytes();
			DatagramPacket packet = new DatagramPacket(m, m.length, group, Configurations.MULTICAST_PORT);
			chatSocket.send(packet);
		}
		
		msgArea.setText("");				// Azzera la msgArea
	}

	
	// Richiesta di End Edit (TCP)
	private void loggedUI() throws IOException {
		outToServer.writeBytes("endEdit" + '\n');
		
		outToServer.writeBytes(username + '\n');
		outToServer.writeBytes(docName + '\n');
		outToServer.writeByte(section);
		
		uploadFile();								// Salva il file sul server
		
		String res = inFromServer.readLine();
		
		clientChannel = null;
		
		if(res.equals("SUCCESS")) {
			
			if(Configurations.DEBUG)
				System.out.println("Fine Edit, torno in Logged");
			c.disable();
			GUILoggedClass w = new GUILoggedClass(clientSocket, clientChannel, username, "edit");
			w.getContentPane().setBackground(Configurations.GUI_BACKGROUND);	
			w.setLocation(Configurations.GUI_X_POS, Configurations.GUI_Y_POS);
			w.setVisible(true);
			this.dispose();
		}
	}
	
	
	// Carica il file (sia che sia stato modificato o meno) che è stato precedentemente scaricato
	protected void uploadFile() throws IOException {
		
		FileChannel inChannel = FileChannel.open(Paths.get("Editing/" + username + "/" + docName + section + ".txt"), StandardOpenOption.READ);
		ByteBuffer buffer = ByteBuffer.allocateDirect(1024*1024);
		
		boolean stop = false;
		
		while (!stop) { 
			int bytesRead=inChannel.read(buffer);
			if (bytesRead==-1) 
				stop=true;

			buffer.flip();
			while (buffer.hasRemaining())
				clientChannel.write(buffer);
			buffer.clear();
		}
		clientChannel.close();
		inChannel.close(); 
	}

	
	// Lascia un messaggio di disconnessione nella chat per gli altri utenti
	protected void disconnect() throws IOException {

		String finalMsg = username + " si è disconnesso.\n";
		byte[] m = finalMsg.getBytes();
		DatagramPacket packet = new DatagramPacket(m, m.length, group, Configurations.MULTICAST_PORT);
		chatSocket.send(packet);
		chatSocket.leaveGroup(group);
	}
}
