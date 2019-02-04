import java.util.HashSet;
import java.util.Set;

public class User {
	
	private String username;
	private String password;
	private Set<String> userDocs;
	
	public User(String username, String password) {
		this.username = username;
		this.password = password;
		this.userDocs = new HashSet<String>();
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
}
