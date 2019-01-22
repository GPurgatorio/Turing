import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Document {
	
	private User creator;
	private String docName;
	private List<File> section;
	List<ReentrantLock> locks;
	private List<User> editors;
	
	public Document(User creator, String docName, int n) {
		this.creator = creator;
		this.docName = docName;
		this.section = Arrays.asList(new File[n]);
		this.locks = Arrays.asList(new ReentrantLock[n]);;
		
		for(int i = 0; i < n; i++) 
			locks.set(i, new ReentrantLock(true));					//lock con fairness
	}

	public void addEditor(User receiver) {
		this.editors.add(receiver);
	}

	public boolean isCreator(User u){
		return this.creator.equals(u);
	}
	public boolean isEditor(User receiver) {
		return this.editors.contains(receiver);
	}
	
}
