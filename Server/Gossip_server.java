package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import Messages.RMI.Gossip_RMI_server_interface;
import Server.Gossip_RMI_handler;
import Server.Gossip_config;
import Server.Structures.Gossip_data;
import Server.Threads.Gossip_worker;

/**
 * Thread principale del server
 * 
 * @author Gioele Bertoncini
 *
 */
public class Gossip_server implements Runnable {
	
	private ServerSocket socket = null; //socket su cui accettare le connessioni
	private ThreadPoolExecutor pool = null; //pool di thread per servire le richieste dei client
	private Gossip_data data = null; //struttura dati del server
	
	public Gossip_server() throws IOException {
		//creo socket
		socket = new ServerSocket(Gossip_config.TCP_PORT);
		
		//creo thread pool
		pool = (ThreadPoolExecutor)Executors.newCachedThreadPool();
		
		//ininzializzo struttura dati
		data = new Gossip_data();
		
	}
	
	/**
	 * Esporta l'interfaccia RMI del server
	 */
	public void initRMI() {
		try {
			//creo RMI handler
			Gossip_RMI_handler handler = new Gossip_RMI_handler(data);
			//esporto l'interfaccia
			Gossip_RMI_server_interface stub = (Gossip_RMI_server_interface)UnicastRemoteObject.exportObject(handler,  0);
			LocateRegistry.createRegistry(Gossip_config.RMI_PORT);
			Registry r = LocateRegistry.getRegistry(Gossip_config.RMI_PORT);
			r.rebind(Gossip_config.RMI_NAME,  stub);
			
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		
		initRMI();
		System.out.println("Server in esecuzione...");

		try {
			while(true) {
				//accetto una connessione
				Socket client = socket.accept();
				System.out.println("Nuova connessione, la passo a un thread");
				//passo il socket a un thread del pool
				pool.execute(new Gossip_worker(client, data));	
			}
		} catch (IOException e) {e.printStackTrace();}
		
		finally {
			try {
				//chiudo socket
				socket.close();
				//termino i thread worker
				pool.shutdown();
				System.out.println("Server chiuso");
			} catch (IOException e) {e.printStackTrace();}
		}
	}
	
	public static void main(String[] args) {
		try {
			Gossip_server server = new Gossip_server();
			System.out.println("Server inizializzato");
			server.run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
