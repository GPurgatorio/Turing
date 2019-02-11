package server;

public class Configurations {
	
	// Alcuni commenti per porzioni di codice uguali sono ripetuti nei vari files, altri no. 
	// Se una zona non è commentata, significa è stata commentata una parte identica in un qualche altro file!
	
	// Client-Server related
	public static final int DEFAULT_PORT = 6789;
	public static final int INVITE_PORT = 6788;
	public static final int REGISTRATION_PORT = 1099;
	public static final int MULTICAST_PORT = 4321;
	
	// Server settings
	public static final int MAX_SECTIONS = 9;				// Necessariamente inferiore a 256 (writeByte)
	public static final String VALID_CHARACTERS = "[a-zA-Z0-9]+";
	
	// Client settings
	public static final int TIMEOUT = 1000;
	
	// User Interface related
	public static final java.awt.Color GUI_LOGIN_BACKGROUND = new java.awt.Color(0, 172, 230);
	public static final java.awt.Color GUI_BACKGROUND = new java.awt.Color(194, 194, 163);
	public static final int GUI_X_POS = 400;				// Posizione X della finestra
	public static final int GUI_Y_POS = 100;				// Posizione Y della finestra
	
	// Miscellaneous
	public static final int THREADPOOL_EX_THREADS = 10;		// Numero threads
	public static final int MAX_LENGTH = 25;				// Massima lunghezza di un campo di input (Username, Document, Password)
	public static final boolean BORED_INIT = false;
	
	// Debug
	public static final boolean DEBUG = true;				// Mostra alcune stampe per il controllo del flusso
}
