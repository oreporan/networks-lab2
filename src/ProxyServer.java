import java.lang.reflect.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/***
 * Web server accepts connections and creates TCP connection that handle each
 * request. The threadQueue max size is decided in the config.ini file, and each
 * new connection (not new request!) gets a new thread Requests that belong to
 * the same IP, go to the thread that is handling this TCP connection
 * 
 * @author ore
 *
 */
public class ProxyServer {
	public static int numOfThreads = 0;


	public static void main(String args[]) {
		if (args.length != 1) {
			printUsage();
			System.exit(0);
		}
		try {

			init(args);

			ServerSocket socket = new ServerSocket(ConfigUtil.getPort());
			String listeningLog = ("Listening on port: " + ConfigUtil.getPort());
			System.out.println(listeningLog);

			// Process HTTP server requests in an infinite loop.
			while (true) {
				while (ConnectionHandler.getNumberOfConnections() < ConfigUtil.getMaxThreads()) {

					ConnectionHandler.emptyTimedOutConnections();

					// Listen for a TCP connection request.
					Socket connection = socket.accept();
					ConnectionHandler.createConnection(connection);
				}
				ConnectionHandler.emptyTimedOutConnections();
			}
		} catch (Exception e) {
			Logger.error("Corrupt config/Policies File: " + e);
		}
	}


	private static void init(String[] args) throws Exception {
		Logger.init(ConfigUtil.logPath);
		PoliciesUtil.init(args[0]); // Init Policies file
		ConfigUtil.init(); // Init the config file
		ConnectionHandler.init();
	}


	private static void printUsage() {
		System.out.println("Usage: java ProxyServer [policy file]");
	}

}
