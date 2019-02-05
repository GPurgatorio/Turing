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
	}
	
	public String getUser() {
		return this.username;
	}
	
	public Set<String> getDocs(){
		return this.userDocs;
	}
	
	public void addToEditableDocs(String docName) {
		this.userDocs.add(docName);
	}
	
	public boolean isEditor(String docName) {
		return this.userDocs.contains(docName);
	}
	
	public void iHateWarnings() {		//just to remove the "password" not used warning eheh
		boolean res = this.password.contains("gnigni");
		if(res)
			return;
	}

	public void addPendingInvite(String docName) {
		this.pendingInvites.add(docName);
	}
	
	public Set<String> getPendingInvites() {
		return this.pendingInvites;
	}
	
	public void resetPendingInvites() {
		this.pendingInvites.clear();
	}

	public void addInstaInvites(String docName) {
		this.instaInvites.add(docName);
	}
	
	public Set<String> getInstaInvites() {
		return this.instaInvites;
	}
	
	public void resetInstaInvites() {
		this.instaInvites.clear();
	}
}
