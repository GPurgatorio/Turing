package server;

import java.util.HashSet;
import java.util.Set;

public class User {
	
	private String username;
	private String password;
	private Set<String> userDocs;
	private Set<String> pendingInvites;
	private Set<String> instaInvites;
	
	public User(String username, String password) {
		this.username = username;
		this.password = password;
		this.userDocs = new HashSet<String>();
		this.pendingInvites = new HashSet<String>();
		this.instaInvites = new HashSet<String>();
	}
	
	public synchronized String getUser() {
		return this.username;
	}
	
	public synchronized Set<String> getDocs(){
		return this.userDocs;
	}
	
	public synchronized void addToEditableDocs(String docName) {		
		this.userDocs.add(docName);
	}
	
	public synchronized boolean isEditor(String docName) {
		return this.userDocs.contains(docName);
	}
	
	public synchronized boolean checkPassword(String pwd) {		//just to remove the "password" not used warning eheh
		return this.password.equals(pwd);
	}

	public synchronized void addPendingInvite(String docName) {					
		this.pendingInvites.add(docName);
	}
	
	public synchronized Set<String> getPendingInvites() {
		return this.pendingInvites;
	}
	
	public synchronized void resetPendingInvites() {
		this.pendingInvites.clear();
	}

	public synchronized void addInstaInvites(String docName) {
		this.instaInvites.add(docName);
	}
	
	public synchronized Set<String> getInstaInvites() {
		return this.instaInvites;
	}
	
	public synchronized void resetInstaInvites() {
		this.instaInvites.clear();
	}
}
