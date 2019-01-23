import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class requestHandler implements Runnable {

	private String nameServed = "";
	private static DataOutputStream outToServer;
	private static BufferedReader inFromServer;
	private static Socket clientSocket;
	private static BufferedReader inFromClient;
	private static DataOutputStream outToClient;
	
	
	public requestHandler(Socket s) throws IOException {
		clientSocket = s;
		inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		outToClient = new DataOutputStream(clientSocket.getOutputStream());
	}
	
	@Override
	public void run() {
		boolean flag = true;
		
		do {
			try {
				System.out.println("Thread serving.");
				String username, password, answer;
				String command = inFromClient.readLine();
				System.out.println("Server @Thread - Received: " + command);
				
				outToServer = new DataOutputStream(clientSocket.getOutputStream());
				inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				
				if(command.equals("register")) {
					System.out.println("Register");
					
					username = inFromClient.readLine();				
					password = inFromClient.readLine();
					answer = "ERROR" + '\n';
					
					if(checkBreak(username, password)) {
						outToClient.writeBytes(answer);
					}
					
					if(!Turing.checkAll(username)) {
						answer = "SUCCESS" + '\n';
						Turing.register(username,password);
					}
	
					outToClient.writeBytes(answer);
				}
				
				else if(command.equals("login")) {
					System.out.println("Login:");
					
					username = inFromClient.readLine();
					nameServed = username;
					password = inFromClient.readLine();
					answer = "ERROR" + '\n';
					
					System.out.printf("Received: %s %s\n", username, password);
		
					if(Turing.login(username, password)) 
						answer = "Logged in" + '\n';
					
					outToClient.writeBytes(answer);
				}
			}
			catch (Exception e) {
				try {
					clientSocket.close();
					Turing.disconnect(nameServed);
					break;
				} catch (IOException e1) {
					System.err.println("Ciaux");
					e1.printStackTrace();
				}
			}
			/*
			else if(command.equals("logout")) {
				System.out.println("Logout:");
				
				username = inFromClient.readLine();
				
				if(disconnect(username)) {
					
				}
			}
			
			else if(command.equals("invite")) {
				;
			}
			
			else if(command.equals("editDoc")) {
				;
			}*/
			
		} while(flag);
	}
	
	private static boolean checkBreak(String username, String password) {
		return (username.equals("BREAK") || password.equals("BREAK"));
	}

}
