import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.Socket;

/***
 * Handles a client request, If there is something to read in the socket, this
 * client creates an HTTP request instance In the case where the protocol is
 * non-persistent, the thread dequeues itself from the Webserver thread queue
 * after handling the request. This connection also dequeues itself when the
 * client stops the connection
 * 
 * 
 * @author ore
 *
 */
public class TCPConnection extends Connection {
	boolean keepAlive = true;

	public TCPConnection(Socket i_socket) {
		super(i_socket);
	}

	@Override
	public void run() {
		try {
			PushbackInputStream clientIS = new PushbackInputStream(
					socket.getInputStream());
			DataOutputStream clientOS = new DataOutputStream(
					socket.getOutputStream());

			while (keepAlive && socket.isConnected()
					&& clientIS.available() > -1) {
				// Re-set keepAlive which is only decided later
				keepAlive = false;

				byte[] clientReq = readRequestFromClient(clientIS);
				InputStream inputStream = new ByteArrayInputStream(clientReq);
				BufferedReader readFromClient = new BufferedReader(
						new InputStreamReader(inputStream));
				//String clientIP = socket.getRemoteSocketAddress().toString();
				String[] strArr;
				String line;
				String hostname = null;
				String path = null;
				boolean innerRequest = false;
				// Read the status of the request
				line = readFromClient.readLine();
				strArr = line.split(" ");
				if (strArr.length > 1) {
					path = strArr[1];
					if (ConfigUtil.isPoliciesPath(path)) {
						innerRequest = true;
					}
				}
				while ((line = readFromClient.readLine()) != null) {
					strArr = line.split(" ");
					if (strArr[0].equals("Host:")) {
						hostname = strArr[1];
					} else if (strArr[0].equals(ConfigUtil.CONNECTION + ":")
							&& strArr[1].equals(ConfigUtil.KEEP_ALIVE))
						keepAlive = true;

				}
				// Create a new input stream with the client request
				inputStream = new ByteArrayInputStream(clientReq);

				if (innerRequest) {
					System.out.println(path + " is a inner request");
					// This is a inner Request to the server
					handleServerRequest(inputStream, clientOS);

				} else {
					if (!PoliciesUtil.legalPath(path)) {
						// This request is BLOCKED by the proxy
						System.out.println(path + " is not a legal request");
						new HttpResponse(clientOS, null,
								ConfigUtil.DEFAULT_PROTOCOL, false)
								.sendErrorResponse(ConfigUtil.ACCESS_DENIED);
					} else {
						handleProxyRequest(clientReq, clientOS, hostname);
					}
				}
				// Begin time stamp
				setTimeStamp(System.currentTimeMillis());

			}
			closeConnection();

		} catch (Exception ex) {
			String host = socket.getRemoteSocketAddress().toString();
			System.err.println("Problem connecting to - " + host + " " + ex);

		}
	}

	private void handleProxyRequest(byte[] clientReq, DataOutputStream sos,
			String i_host) throws IOException {
		// create server socket
		Socket serverSocket = new Socket(i_host, ConfigUtil.WEB_PORT);
		forwardRequestToHost(clientReq, serverSocket);

		forwardResponseToClient(sos, serverSocket);

	}

	private void handleServerRequest(InputStream inputStream,
			DataOutputStream sos) throws Exception, IOException {
		// Create request
		HttpRequest request = new HttpRequest(inputStream);
		// Create response
		HttpResponse response = new HttpResponse(sos, request.requestPath,
				request.requestProtocol, request.supportChunks());
		sendResponse(request, response);
	}

	/**
	 * Using the request parameters, sends the fitting response
	 * 
	 * @param req
	 * @param res
	 * @throws InternalErrorException
	 */
	private void sendResponse(HttpRequest req, HttpResponse res) {
		try {
			if (req.getErrorMessage() == null) {
				String requestMethod = req.getRequestMethod();
				if (requestMethod.equals(ConfigUtil.GET)) {
					// GET request
					res.sendGetResponse(req.getParamsMapGET());
				} else if (requestMethod.equals(ConfigUtil.POST)) {
					// POST request
					res.sendPostResponse(req.getParamsMapPOST());
				} else if (requestMethod.equals(ConfigUtil.OPTIONS)) {
					// OPTIONS request
					res.sendOptionsResponse();
				} else if (requestMethod.equals(ConfigUtil.HEAD)) {
					// HEAD request
					res.sendHeadResponse();
				} else if (requestMethod.equals(ConfigUtil.TRACE)) {
					// TRACE request
					res.sendTraceResponse(requestMethod, req.getHeadersMap());
				}

			} else {
				// Invalid HTTP request
				res.sendErrorResponse(req.getErrorMessage());
			}
		} catch (InternalErrorException e) {
			// Internal Server Error
			res.sendErrorResponse(e.getMessage());
		}
	}

	private byte[] readRequestFromClient(InputStream inputFromClient)
			throws IOException, Exception {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead; // start offset in the data
		byte[] data = new byte[16384];
		// read each byte from InputStream and write it to a
		// ByteArrayOutputStream
		while ((nRead = inputFromClient.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
			// if reached end of HTTP request, break out of this loop
			if (endOfRequest(data) == true)
				break;
		}
		buffer.flush(); // forces any buffered bytes to be written out
		data = buffer.toByteArray(); // retrieve the underlying byte
										// array
		Logger.newLog(new String(data));
		return data;
	}

	// method to detect end of HTTP request
	// return true if detected, otherwise false
	public static boolean endOfRequest(byte[] data) throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new ByteArrayInputStream(data)));
		String line;

		while ((line = br.readLine()) != null) {
			if (line.equals(""))
				return true;
		}
		return false;
	}

	private void forwardResponseToClient(OutputStream outputToClient,
			Socket serverSocket) throws IOException {
		// receive data from server and write response back
		DataInputStream inputFromServer = new DataInputStream(
				serverSocket.getInputStream());
		byte[] receivedData = new byte[1024];
		int size;
		while ((size = inputFromServer.read(receivedData, 0,
				receivedData.length)) != -1) {
			outputToClient.write(receivedData, 0, size);
			outputToClient.flush();
		}
	}

	private void forwardRequestToHost(byte[] data, Socket serverSocket)
			throws IOException {
		OutputStream outputToServer = serverSocket.getOutputStream();
		// forward request to server
		outputToServer.write(data);
	}
}
