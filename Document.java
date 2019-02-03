import java.io.File;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Document {
	
	private String creator;
	private String docName;
	private List<File> section;
	protected List<ReentrantLock> locks;
	private List<String> editors;
	private InetAddress multicastAddr;
	
	public Document(String creator, String docName, int n, InetAddress randAddr) {
		this.creator = creator;
		this.docName = docName;
		this.section = Arrays.asList(new File[n]);
		this.locks = Arrays.asList(new ReentrantLock[n]);
		this.editors = new LinkedList<String>();
		this.multicastAddr = randAddr;
		
		for(int i = 0; i < n; i++) 
			locks.set(i, new ReentrantLock(true));					//lock con fairness
	}
	
	public String getName() {
		return this.docName;
	}
	
	public String getCreator() {
		return this.creator;
	}

	public void addEditor(String receiver) {
		this.editors.add(receiver);
	}

	public boolean isCreator(String username){
		return this.creator.equals(username);
	}
	
	public boolean isEditor(String receiver) {
		return this.editors.contains(receiver);
	}
	
	public boolean isLocked(int section) {
		return locks.get(section).isLocked();
	}
	
	public InetAddress getAddr() {
		return this.multicastAddr;
	}
	
	public boolean editSection(int section) {
		return locks.get(section).tryLock();
	}
	
	public List<File> showDoc() {
		//CHANGE EHEH
		System.err.println("DEVI ANCORA IMPLEMENTARMI SCEEEEEMO [showDoc - Document.java]");
		return this.section;
	}

	public void unlockSection(int section) {
		locks.get(section).unlock();
	}
}
