import java.io.IOException;
import java.net.Socket;

public class Connection implements Runnable {
	private long timeStamp;
	Socket socket;
	boolean isDead = false;

	public Connection(Socket i_socket) {
		this.socket = i_socket;
	}

	public boolean isThisConnectionTimedOut() {
		long currentTime = System.currentTimeMillis();
		if (this.getTimeStamp() != 0
				&& (currentTime - this.getTimeStamp() > ConfigUtil.CONNECTION_TIMEOUT)) {
			String host = socket.getRemoteSocketAddress().toString();
			System.out.println(host + " has timed out..");
			return true;
		}
		return false;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	/*
	 * Closes this connection, can also be called by the server on Timeout
	 */
	public void closeConnection() {
		this.isDead = true;
		if (socket.isConnected()) {
			try {
				socket.close();
			} catch (IOException e) {
				String host = socket.getRemoteSocketAddress().toString();
				System.err.println("Error closing socket - " + host
						+ ", reason: " + e.getLocalizedMessage());
			}
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}
}
