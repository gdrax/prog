package Server;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

/**
 * Opzioni di configurazione di server e client
 * 
 * @author Gioele Bertoncini
 *
 */
public class Gossip_config {

	public static final int TCP_PORT = 5000; //porta per le connessioni TCP
	public static final String SERVER_NAME = "localhost"; //nome del server
	public static final int RMI_PORT = 7000; //porta per il registry
	public static final String RMI_NAME = "Gossip_notification"; //nome del servizio RMI
	public static final String MULTICAST_FIRST = "226.0.0.0"; //primo indirizzo multicast utilizzabile per le chatroom
	public static final String MUTLICAST_LAST = "226.0.0.255"; //ultimo indirizzo multicast utilizzabile per le chatroom
	public static final String DOWNLOAD_DIR = "/downloads/"; //directory nella quale scaricare i file ricevuti
	public static final String UPLOAD_DIR = "/uploads/"; //directory dalla quale caricare i file da inviare
	
	/**
	 * @return una porta libera
	 * @throws IOException 
	 * @throws Exception
	 */
	public static int findPort() throws PortNotFoundException, IOException {
		int min = 1025;
		int max = 65535;
		
		for (int i=min; i<max+1; i++) {
			DatagramSocket tmp = null;
			ServerSocket tmp2 = null;
			
			try {
				tmp2 = new ServerSocket(i);
				tmp2.setReuseAddress(true);
				tmp = new DatagramSocket(i);
				tmp.setReuseAddress(true);
				return i;
			} catch (IOException e) {}
			  finally {
				  if (tmp != null)
					  tmp.close();
				  if (tmp2 != null)
					  tmp2.close();
			  }
		}
		throw new PortNotFoundException();
	}	
}
