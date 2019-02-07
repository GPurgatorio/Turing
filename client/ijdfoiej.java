package client;

public class ijdfoiej {

	public static void main(String args[]) {
		String x = "";
		int port = x.hashCode() % 65535;
        if(port < 0) 
        	port = -port % 65535;
        if(port < 1024)		//evito le porte "Well-known" [0-1023]
        	port += 1024;
		System.out.println(port);
	}
}
