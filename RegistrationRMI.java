import java.rmi.RemoteException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RegistrationRMI implements RegistrationInterface {

	private ConcurrentHashMap<String,User> database;
	private Set<String> usersOffline;
	private Set<String> usersOnline;
	
	protected RegistrationRMI(ConcurrentHashMap<String,User> db, Set<String> uOff, Set<String> uOn) {
		this.database = db;
		this.usersOffline = uOff;
		this.usersOnline = uOn;
	}
	
	@Override
	public boolean registerRequest(String username, String password) throws RemoteException {
		if(!usersOnline.contains(username) && !usersOffline.contains(username)) {
			User u = new User(username, password);
			database.put(username, u);
			usersOffline.add(username);
			return true;
		}
		
		return false;
	}

}
