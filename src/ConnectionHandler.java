import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class ConnectionHandler {
	static HashMap<String, TCPConnection> connections;

	public static void init() {
		connections = new HashMap<String, TCPConnection>();
	}

	public static void createConnection(Socket i_socket) throws Exception {
		if (!connectionExists(i_socket)) { // New connection
			TCPConnection newConnection = new TCPConnection(i_socket);
			String host = i_socket.getRemoteSocketAddress().toString();
			System.out.println(host + " has connected");
			Thread t = new Thread(newConnection);
			connections.put(host, newConnection);
			t.start();

		}// Connection exists - do nothing, TCP connection will catch it
	}

	private static boolean connectionExists(Socket i_socket) {
		String host = i_socket.getRemoteSocketAddress().toString();
		return (connections.containsKey(host));
	}

	public static void emptyTimedOutConnections() {
		Iterator<Entry<String, TCPConnection>> iter = connections.entrySet()
				.iterator();
		while (iter.hasNext()) {
			Entry<String, TCPConnection> entry = iter.next();
			if (entry.getValue().isDead
					|| entry.getValue().isThisConnectionTimedOut()) {
				iter.remove();
			}
		}
	}

	public static int getNumberOfConnections() {
		return connections.size();
	}
}
