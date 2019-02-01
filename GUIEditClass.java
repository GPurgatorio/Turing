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
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class GUIEditClass extends JFrame {

	private String username;
	private JButton sendMsg, endEdit;
	private JLabel userLabel;
	private JTextArea chatArea, msgArea; 
	private JScrollPane scrollPane; 
	private Image endEditImg, sendMsgImg;
	
	public GUIEditClass(DataOutputStream outToServer, BufferedReader inFromServer, Socket clientSocket, String usr) throws IOException {
		username = usr;
		chatArea = new JTextArea();
		chatArea.setEditable(false);
		msgArea = new JTextArea();
		msgArea.setEditable(true);
		scrollPane = new JScrollPane(chatArea);
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
		
		sendMsg = new JButton();
		endEdit = new JButton();
		
		sendMsg.setIcon(new ImageIcon(sendMsgImg));
		endEdit.setIcon(new ImageIcon(endEditImg));
		
		chatArea.setBounds(35, 40, 365, 290);
		msgArea.setBounds(90, 340, 255, 50);
		sendMsg.setBounds(350, 340, 50, 50);
		endEdit.setBounds(35, 340, 50, 50);
		
		sendMsg.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent ae){
				;
			}
		});
		
		endEdit.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent ae){
				// w.getContentPane().setBackground(new java.awt.Color(173, 178, 184));
				;
			}
		});

		add(msgArea);
		add(chatArea);
		add(sendMsg);
		add(endEdit);
		
		userLabel = new JLabel("Current user: " + username);
		userLabel.setBounds(13, 13, 200, 15);
		add(userLabel);
	}

}
