package server;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Document {
	
	private String docName;
	private String creator;
	private List<String> editors;
	private List<ReentrantLock> locks;
	private InetAddress multicastAddr;
	
	public Document(String creator, String docName, int n, InetAddress randAddr) {
		this.creator = creator;
		this.docName = docName;
		this.locks = Arrays.asList(new ReentrantLock[n]);
		this.editors = new LinkedList<String>();
		this.multicastAddr = randAddr;
		
		for(int i = 0; i < n; i++) 
			locks.set(i, new ReentrantLock(true));					//lock con fairness
	}
	
	public synchronized String getName() {
		return this.docName;
	}
	
	public synchronized String getCreator() {
		return this.creator;
	}
	
	public synchronized int getSize() {
		return this.locks.size();
	}

	public synchronized void addEditor(String receiver) {
		this.editors.add(receiver);
	}

	public synchronized boolean isCreator(String username){
		return this.creator.equals(username);
	}
	
	public synchronized boolean isEditor(String receiver) {
		return (this.editors.contains(receiver) || this.creator.equals(receiver));
	}
	
	public synchronized boolean isLocked(int section) {
		return locks.get(section).isLocked();
	}
	
	public synchronized InetAddress getAddr() {
		return this.multicastAddr;
	}
	
	public synchronized boolean editSection(int section) {
		return locks.get(section).tryLock();
	}
	
	public synchronized void unlockSection(int section) {
		locks.get(section).unlock();
	}

	public synchronized String getEditors() {
		String res = null;
		
		for(int i = 0; i < editors.size(); i++) {
			if (res == null)
				res = editors.get(i);
			else
				res = res + ", " + editors.get(i);
		}
		
		return res;
	}
}
