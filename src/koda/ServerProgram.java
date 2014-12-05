package koda;

import java.util.Date;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

public class ServerProgram extends Listener {

	static Server server;
	static int udpPort = 27960;
	static int tcpPort = 27960;
	
	public static void main(String[] args) throws Exception {
		System.out.println("Creating server...");
		server = new Server();
		server.getKryo().register(PacketMessage.class);
		server.bind(tcpPort, udpPort);
		server.start();
		server.addListener(new ServerProgram());
		System.out.println("Server operational.");
	}
	
	@Override
	public void connected(Connection c) {
		System.out.println("Received connection from " + c.getRemoteAddressTCP().getHostString());
		PacketMessage packetMessage = new PacketMessage();
		packetMessage.message = "Hello, friend! Today is: " + new Date().toString();
		c.sendTCP(packetMessage);
	}
	
	@Override
	public void received(Connection c, Object p) {
		
	}
	
	@Override
	public void disconnected(Connection c) {
		System.out.println("A client disconnected");
	}
}
