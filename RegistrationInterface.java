import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RegistrationInterface extends Remote {
	
	public String SERVICE_NAME = "RegisterRMI";
	
	public boolean registerRequest(String username, String password) throws RemoteException;

}
