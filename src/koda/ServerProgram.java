package koda;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

public class ServerProgram extends Listener {

	static Server server;
	static int tcpPort = 27960;
	static int udpPort = 27961;
	
	static UserDatabase udb;
	static JFrame frame = new JFrame("Server");
	static JPanel panel1 = new JPanel();
	static JPanel panel2 = new JPanel();
	static JPanel panel3 = new JPanel();
	static JPanel master_panel = new JPanel();
	static JButton button_disconnect = new JButton("Disconnect");
	static DefaultListModel<String> list_model = new DefaultListModel<String>();
	static JList<String> client_list = new JList<String>(list_model);
	
	static int num_users_online = 0;

	public static void main(String[] args) throws Exception {
		
		System.out.println("Creating server...");
		server = new Server();
		registerPackets();
		server.bind(tcpPort, udpPort);
		server.start();
		server.addListener(new ServerProgram());
		System.out.println("Server operational.");
		
		udb = UserDatabase.read();
		
		button_disconnect.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int index = client_list.getSelectedIndex();
				
				if (index != -1) {
					int id = getClientIdFromModel(list_model.get(index));
					StatusMessage pkt = new StatusMessage();
					pkt.status = StatusMessage.KICKED;
					server.sendToTCP(id, pkt);
					
					AnnouncementMessage msg = new AnnouncementMessage();
					msg.announcement_type = AnnouncementMessage.ANNOUNCEMENT_NOTIFICATION;
					msg.message = getClientNameFromModel(list_model.get(index)) + " has been kicked!\n";
					server.sendToAllExceptTCP(id, msg);
				}
			}
		});
		
		panel1.setLayout(new GridLayout(1, 1, 1, 1));
		panel1.add(button_disconnect);
		
		panel2.setLayout(new BorderLayout(1, 1));
		panel2.add(panel1, BorderLayout.NORTH);
		panel2.add(new JScrollPane(client_list), BorderLayout.CENTER);
		
		master_panel.setLayout(new GridLayout(1, 1, 1, 1));
		master_panel.add(panel2);
		master_panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		frame.setContentPane(master_panel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing(WindowEvent e) {
				udb.write();
				
				StatusMessage pkt = new StatusMessage();
				pkt.status = StatusMessage.SERVER_SHUTTING_DOWN;
				server.sendToAllTCP(pkt);
				server.stop();
			}
		});
		
		frame.pack();
		frame.setSize(350, 400);
		frame.setLocationRelativeTo(null);
		frame.setResizable(false);
		frame.setVisible(true);
	}
	
	public static void registerPackets() {
		server.getKryo().register(LoginResponse.class);
		server.getKryo().register(ChatMessage.class);
		server.getKryo().register(StatusMessage.class);
		server.getKryo().register(LoginMessage.class);
		server.getKryo().register(AnnouncementMessage.class);
	}
	
	private static int getClientIdFromModel(String model) {
		String s = model;
		s = s.substring(s.indexOf(" ") + 1, s.indexOf("-") - 1);
		return Integer.parseInt(s);
	}
	
	private static String getClientNameFromModel(String model) {
		String s = model;
		s = s.substring(s.indexOf("-") + 2, s.indexOf(" ", s.indexOf("-") + 2));
		return s;
	}
	
	private static void updateModel(int id, String username) {
		for (int i = 0; i < list_model.size(); i++) {
			String s = list_model.get(i);
			if (id == getClientIdFromModel(s)) {
				String prefix = s.substring(0, s.indexOf("-") + 2);
				prefix += username + " - ";
				prefix += s.substring(s.indexOf("-") + 2);
				list_model.set(i, prefix);
			}
		}
	}
	
	@Override
	public void connected(Connection c) {
		list_model.addElement("ID: " + c.getID() + " - " + c.getRemoteAddressTCP().getHostString());
		num_users_online++;
	}
	
	@Override
	public void received(Connection c, Object p) {
		if (p instanceof ChatMessage) {
			ChatMessage pkt = (ChatMessage) p;
			server.sendToAllExceptTCP(c.getID(), pkt);
		} else if (p instanceof LoginMessage) {
			LoginMessage pkt = (LoginMessage) p;
			LoginResponse response = new LoginResponse();
			response.login_status = udb.verifyUser(pkt.username, pkt.password);
			
			boolean valid = false;
			switch (response.login_status) {
			case LoginResponse.LOGIN_SUCCESSFUL:
				response.message = "Welcome back, " + pkt.username + "!";
				valid = true;
				break;
			case LoginResponse.LOGIN_NEW_USER:
				response.message = "Welcome, new user " + pkt.username + "!";
				valid = true;
				break;
			case LoginResponse.LOGIN_BAD_PASSWORD:
				response.message = "Incorrect password! Please try again.";
				break;
			}
			
			if (valid) {
				updateModel(c.getID(), pkt.username);
				AnnouncementMessage msg = new AnnouncementMessage();
				msg.announcement_type = AnnouncementMessage.ANNOUNCEMENT_REGULAR;
				msg.message = "Welcome to the chat server. There are currently " + num_users_online + " user(s) in the room.\n";
				c.sendTCP(msg);
				
				msg = new AnnouncementMessage();
				msg.announcement_type = AnnouncementMessage.ANNOUNCEMENT_REGULAR;
				msg.message = pkt.username + " has just joined the server!\n";
				server.sendToAllExceptTCP(c.getID(), msg);
			}
			
			c.sendTCP(response);
		}
	}
	
	@Override
	public void disconnected(Connection c) {
		//System.out.println("A client disconnected");
		for (int i = 0; i < list_model.size(); i++) {
			int id = getClientIdFromModel(list_model.get(i));
			if (id == c.getID()) {
				list_model.remove(i);
				num_users_online--;
				break;
			}
		}
	}
}
