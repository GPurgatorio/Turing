package server;

import java.util.HashSet;
import java.util.Set;

public class User {
	
	private String username;			// Rappresentatore univoco all'interno del database
	private String password;
	private Set<String> userDocs;		// Insieme dei documenti che un utente è abilitato a modificare
	private Set<String> pendingInvites;	// Insieme dei documenti a cui l'utente è stato invitato mentre era offline
	private Set<String> instaInvites;	// Insieme dei documenti usato per la gestione degli inviti mentre è online
	
	
	// Costruttore
	public User(String username, String password) {
		this.username = username;
		this.password = password;
		this.userDocs = new HashSet<String>();
		this.pendingInvites = new HashSet<String>();
		this.instaInvites = new HashSet<String>();
	}

	// Funzioni per operazioni standard sugli attributi di questa classe
	
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
	
	public synchronized boolean checkPassword(String pwd) {		
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
