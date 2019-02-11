package server;

import java.rmi.RemoteException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RegistrationRMI implements RegistrationInterface {

	private ConcurrentHashMap<String,User> database;
	private Set<String> usersOffline;
	private Set<String> usersOnline;
	
	// Costruttore
	protected RegistrationRMI(ConcurrentHashMap<String,User> db, Set<String> uOff, Set<String> uOn) {
		this.database = db;
		this.usersOffline = uOff;
		this.usersOnline = uOn;
	}
	
	// Registrazione (RMI)
	@Override
	public synchronized boolean registerRequest(String username, String password) throws RemoteException {
		
		// Se l'utente non fa parte degli utenti online né di quelli offline..
		if(!usersOnline.contains(username) && !usersOffline.contains(username)) {
			User u = new User(username, password);		// Creo nuovo User
			database.put(username, u);					// Lo inserisco nel database
			usersOffline.add(username);					// E lo inserisco tra gli utenti offline
			return true;
		}
		
		// Se arrivo qua, l'utente è o offline o online, quindi già registrato
		return false;
	}

}
