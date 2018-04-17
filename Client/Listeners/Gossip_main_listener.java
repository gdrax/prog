package Client.Listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

import javax.swing.DefaultListModel;
import javax.swing.JTextArea;

import Client.Gossip_RMI_client_implementation;
import Client.Forms.Gossip_main_form;
import Client.Threads.Gossip_add_chat_thread;
import Client.Threads.Gossip_enter_chat_thread;
import Client.Threads.Gossip_exit_chat_thread;
import Client.Threads.Gossip_logout_thread;
import Client.Threads.Gossip_message_receiver_thread;
import Client.Threads.Gossip_remove_chat_thread;
import Client.Threads.Gossip_search_thread;
import Messages.RMI.Gossip_RMI_client_interface;
import Messages.RMI.Gossip_RMI_server_interface;
import Server.Gossip_config;
import Server.Graph.NodeNotFoundException;
import Server.Structures.Gossip_chat;
import Server.Structures.Gossip_user;

/**
 * Controller della finestra principale
 * 
 * @author Gioele Bertoncini
 *
 */
public class Gossip_main_listener extends Gossip_listener {

	private Gossip_main_form window;
	private Gossip_main_listener main;
	private ArrayList<Gossip_chatroom_listener> chatroom_listeners;
	private ArrayList<Gossip_friend_chat_listener> friend_chat_listeners;
	private ArrayList<Gossip_user_listener> user_listeners;
	private Gossip_RMI_server_interface server_interface;
	private Gossip_RMI_client_implementation callback;
	private Gossip_message_receiver_thread th_receiver;
	private ArrayList<Gossip_chat> chatrooms;
	private String password;	
	
	public Gossip_main_listener(DataInputStream i, DataOutputStream o, Socket s, Gossip_user u, ArrayList<Gossip_user> f, ArrayList<Gossip_chat> c, String p) {
		super(i, o, s, u);
		
		if (u == null)
			throw new NullPointerException();
		user = u;
		password = p;
		window = new Gossip_main_form(f, c, user);
		this.setFrame(window.getFrame());
		this_listener = this;
		chatroom_listeners = new ArrayList<Gossip_chatroom_listener>();
		friend_chat_listeners = new ArrayList<Gossip_friend_chat_listener>();
		chatrooms = c;
		user_listeners = new ArrayList<Gossip_user_listener>();
		main = this;
	}
	
	private void initRMI() {
		try {
			//inizializzazione RMI
			Registry reg = 	LocateRegistry.getRegistry(Gossip_config.RMI_PORT);
			server_interface = (Gossip_RMI_server_interface) reg.lookup(Gossip_config.RMI_NAME);
			callback = new Gossip_RMI_client_implementation(this);
			Gossip_RMI_client_interface stub = (Gossip_RMI_client_interface) UnicastRemoteObject.exportObject(callback,  0);
			server_interface.registerForCallback(user.getName(), stub);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		} catch (NodeNotFoundException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Avvia il thread che riceve i messaggi di notifica
	 */
	private void initThreadReceiver() {
		th_receiver = new Gossip_message_receiver_thread(socket.getInetAddress(), socket.getPort(), main, user, password);
		th_receiver.start();
	}
	
	public void listen() {
		super.listen();
		
		initRMI();
		initThreadReceiver();
		
		//listener della chiusura della finestra (che equivale a eseguire il logout)
		window.getFrame().addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing(WindowEvent e) {
				//avvio thread per inviare la richiesta di logout
				new Gossip_logout_thread(input, output, socket, main, user).start();
			}
		});
		
		//listener del bottone per eseguire il logout
		window.getLogoutButton().addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				//avvio thread per inviare la richiesta di logout
				new Gossip_logout_thread(input, output, socket, main, user).start();
			}
		});
		
		//listener del bottone per ricercare un utente
		window.getSearchButton().addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				//avvio thread per inviare una richiesta di ricerca
				new Gossip_search_thread(input, output, socket, main, user, window.getSearchText().getText()).start();;
			}
		});
		
		//listener del bottone per avviare una chat con amico
		window.getFriendButton().addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				String friend = window.getFriendList().getSelectedValue();
				if (friend == null)
					//l'utente non ha selezionato nulla
					infoMessage("Seleziona un amico con cui chattare");
				else {
					Gossip_friend_chat_listener listener;
					synchronized (friend_chat_listeners) {
						try {
							//se la finestra della chat esisteva già la mostro
							listener = friend_chat_listeners.get(friend_chat_listeners.indexOf(new Gossip_friend_chat_listener(friend)));
							if (!listener.getFrame().isVisible())
								listener.setVisible(true);
						} catch (IndexOutOfBoundsException ex) {
							//la finestra della chat non c'è, ne apro una nuova
							listener = new Gossip_friend_chat_listener(input, output, socket, main, user, friend);
							friend_chat_listeners.add(listener);
							listener.init();
						}
					}
				}	
			}
		});
		
		//listener del bottone per creare una nuova chatroom
		window.getAddChatButton().addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				//avvio thread per inviare una richiesta di creazione nuova chat
				new Gossip_add_chat_thread(input, output, socket, main, user, window.getNewChatText().getText()).start();
			}
		});
		
		//listener del bottone per rimuovere una chatroom
		window.getRemoveChatButton().addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				//avvio thread per richiesta di rimozione chat
				new Gossip_remove_chat_thread(input, output, socket, main, user, window.getChatList().getSelectedValue()).start();
			}
		});
		
		//listener del bottone per entrare in una chatroom
		window.getEnterChatButton().addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				//avvio thread per richiesta di aggiunta a chat
				new Gossip_enter_chat_thread(input, output, socket, main, user, window.getChatList().getSelectedValue()).start();
			}
		});
		
		//listener del bottone per uscire da un chatroom
		window.getExitChatButton().addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				//avvio thread per richiesta di uscida da chat
				new Gossip_exit_chat_thread(input, output, socket, main, user, window.getChatList().getSelectedValue()).start();
			}
		});
	}
	
	/**
	 * Aggiunge il listener della finestra info dell'utente cercato se non è già presente
	 * @param searchedUser: searchedUser utente cercato
	 */
	public void addSearchedUserListener(Gossip_user searchedUser) {
		synchronized (user_listeners) {
			if(!user_listeners.contains(new Gossip_user_listener(searchedUser))) {
				Gossip_user_listener newListener = new Gossip_user_listener(input, output, socket, main, user, searchedUser);
				user_listeners.add(newListener);
				newListener.init();
			}
		}
	}

	/**
	 * Elimina il listener della finestra info di un utente
	 * @param listener: listener da chiudere
	 */
	public void removeUserListener(Gossip_listener listener) {
		synchronized (user_listeners) {
			if (listener != null) {
				user_listeners.remove(listener);
			}
		}
	}
	
	/**
	 * Restituisce l'area di testo della finestra relativa alla chat su cui stampare i messaggi
	 * @param chat
	 * @throws UnknownHostException 
	 * @throws SocketException 
	 */
	public JTextArea getChatMessageArea(Gossip_chat chat) {
		try {
			System.out.println("GOTCHA");
			return chatroom_listeners.get(chatroom_listeners.indexOf(new Gossip_chatroom_listener(chat))).getTextArea();
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}
	
	/**
	 * Apre la finestra di una chatroom, se non è presente in lista ne crea una nuova
	 * @param chat
	 * @throws SocketException
	 * @throws UnknownHostException
	 */
	public void addChatroomListener(Gossip_chat chat) throws SocketException, UnknownHostException {
		Gossip_chatroom_listener newListener;
		try {
			synchronized (chatroom_listeners) {
				newListener = chatroom_listeners.get(chatroom_listeners.indexOf(new Gossip_chatroom_listener(chat)));
				
				System.out.println("Aprilo");
				if (!newListener.getFrame().isVisible())
					newListener.setVisible(true);
				System.out.println("Open "+chatroom_listeners.size());
			}
		} catch (IndexOutOfBoundsException e) {
			//la finestra non c'è ne apro una nuova
			newListener = new Gossip_chatroom_listener(input, output, socket, main, user, chat);
			System.out.println("IOOB");
			synchronized (chatroom_listeners ) {
				chatroom_listeners.add(newListener);
			}
			synchronized (chatrooms) {
				chatrooms.add(chat);
			}
			newListener.init();
			System.out.println("Add "+chatroom_listeners.size());
		}
		
	}
	
	/**
	 * Elimina il listener della finestra di interazione con la chat
	 * @param chatname: chat contenuta dal listener
	 */
	public void removeChatroomListener(Gossip_chat chat) {
		synchronized (chatroom_listeners) {
			try {
				Gossip_chatroom_listener toRemove = chatroom_listeners.get(chatroom_listeners.indexOf(new Gossip_chatroom_listener(chat)));
				//rimuovo listener
				toRemove.stopReceiverThread();
				chatroom_listeners.remove(toRemove);
			} catch (IndexOutOfBoundsException e) {
				//chat non presente
				System.out.println("NOPE");
			}
		}
		System.out.println("Remove: "+chatroom_listeners.size());
	}
	
	/**
	 * Elimina la chat dalla lista e l'eventuale listener associato
	 * @param chat: chat da eliminare
	 */
	public void removeChatroom(Gossip_chat chat) {
		//rimuovo listener
		removeChatroomListener(chat);
		//rimuovo chat
		synchronized(chatrooms) {
			chatrooms.remove(chat);
		}
	}
	
	/**
	 * Aggiorna i dati di una chatroom
	 * @param chat
	 */
	public void updateChatroom(Gossip_chat chat) {
		synchronized (chatrooms) {
			//aggiorno chat
			chatrooms.remove(chat);
			chatrooms.add(chat);
		}
	}
	
	
	/**
	 * Restituisce l'area di testo della finestra della chat, se non esiste ne apre una nuova
	 * @param friend
	 * @return area di testo dove scrivere i messaggi che vengono ricevuti
	 */
	public JTextArea getMessageArea(String friend) {
		synchronized (friend_chat_listeners) {
			try {
				return friend_chat_listeners.get(friend_chat_listeners.indexOf(new Gossip_friend_chat_listener(friend))).getTextArea();
			} catch (IndexOutOfBoundsException e) {
				//non c'è la finestra della chat, quindi la apro
				Gossip_friend_chat_listener newChat = new Gossip_friend_chat_listener(input, output, socket, main, user, friend);
				friend_chat_listeners.add(newChat);
				newChat.init();
				return newChat.getTextArea();
			}
		}
	}
	
	/**
	 * Rimuove un listener di chat amico dallal lista
	 * @param listener
	 */
	public void removeFriendChatListener(Gossip_friend_chat_listener listener) {
		synchronized (friend_chat_listeners) {
			friend_chat_listeners.remove(listener);	
		}
	}
	
	/**
	 * Chiude le connessioni aperte per le chatroom
	 */
	public void closeChatroomConnections() {
		for (Gossip_chatroom_listener l: chatroom_listeners) {
			l.closeConnection();
			chatroom_listeners.remove(l);
		}
	}
	
	/**
	 * Aggiunge una chatroom alla lista
	 * @param chat
	 */
	public void addChatroom(Gossip_chat chat) {
		if (!chatrooms.contains(chat))
			chatrooms.add(chat);
	}
	
	/**
	 * Termina il thread receiver
	 */
	public void closeReceiverThread() {
		th_receiver.close();
	}
	
	public synchronized Gossip_chat getChatroom(String chatname) {
		try {
			return chatrooms.get(chatrooms.indexOf(new Gossip_chat(chatname)));
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}
	
	public synchronized DefaultListModel<String> getFriendListModel() {
		return window.getFriendModel();
	}

	public synchronized DefaultListModel<String> getChatListModel() {
		return window.getChatModel();
	}
	
	public Gossip_RMI_server_interface getServerInterface() {
		return server_interface;
	}
	
	public Gossip_RMI_client_implementation getCallback() {
		return callback;
	}
}
