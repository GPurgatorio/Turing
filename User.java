import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class User {
	
	public enum Status {
		REGISTERED,
		LOGGED,
		EDITING
	}
	
	private String username;
	private String password;
	private Status status;
	private List<Document> invitedDocs;
	private List<Document> createdDocs;
	private Set<String> userDocs;
	
	public User(String username, String password) {
		this.username = username;
		this.password = password;
		this.status = Status.REGISTERED;
		this.invitedDocs = new LinkedList<Document>();
		this.createdDocs = new LinkedList<Document>();
		this.userDocs = new HashSet<String>();
	}
	
	public String getUser() {
		return this.username;
	}
	
	
	
	public int logout(){
		if(this.status != Status.LOGGED) {
			System.err.println("Utente non loggato");
			return -1;
		}
		this.status = Status.REGISTERED;
		return 0;
	}
	
	public int createDocument(String docName, int numSections) {
		
		if(!this.userDocs.contains(docName)) {
			Document d = new Document(this, docName, numSections);
			this.createdDocs.add(d);
			this.userDocs.add(docName);
			//dì a Turing di aggiungere d ai documenti
		}
		
		return 0;
	}
	
	public void inviteUser (User username, Document doc) {
		//Turing sendInvite(this,username,doc);
		return;
	}
	
	public void edit(Document doc, int section) {
		if(!doc.locks.get(section).tryLock())
			System.err.println("Sezione già in editing");
		
		else {
			//downloadFile
			;
		}
	}
	
	public void endEdit(Document doc, int section) {
		if(!doc.locks.get(section).isHeldByCurrentThread()) 
			System.err.println("Lock non di questo thread");
		
		
	}
	
	public void addToInvitedDocs(Document doc) {
		this.invitedDocs.add(doc);
	}
	
	
	public boolean hasCreated(Document doc){
		return this.createdDocs.contains(doc);
	}
	
	public boolean isEditor(String docName) {
		return this.userDocs.contains(docName);
	}
}
