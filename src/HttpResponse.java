import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;

/***
 * This class writes to the output stream of the socket given by the request. It
 * handles all the supported responses as configured by the config Util class.
 * 
 * @author ore
 * 
 */
public class HttpResponse {
	String path;
	String protocol;
	String statusLine;
	DataOutputStream os;
	private boolean supportsChunks;

	/*
	 * Constructor - gets the OutputStream to write to, the path to get the
	 * file/img/application, and which protocol this client wants
	 */
	public HttpResponse(DataOutputStream i_sos, String i_path,
			String i_protocol, boolean supportsChunks) throws IOException {
		this.path = i_path;
		this.os = i_sos;
		this.protocol = i_protocol;
		this.statusLine = protocol + " " + ConfigUtil.OK + ConfigUtil.CRLF;
		this.supportsChunks = supportsChunks;

	}

	private void writeBody(byte[] body) throws IOException {
		if (this.supportsChunks) {
			int chunkSize = body.length / ConfigUtil.CHUNK_SIZE;
			String chunkHexSize = Integer.toHexString(chunkSize);
			byte[] chunk = null;
			int index = 0;
			int bodyLength = body.length;
			int bytesToWrite = bodyLength;
			String lastChunckedMessage = "0" + ConfigUtil.CRLF
					+ ConfigUtil.CRLF;
			while (bytesToWrite > 0) {
				if (bytesToWrite >= chunkSize) {
					chunk = new byte[chunkSize];
				} else {
					chunk = new byte[bytesToWrite];
					chunkSize = chunk.length;
					chunkHexSize = Integer.toHexString(chunkSize);
				}
				// Copy bytes from body to chunk array
				System.arraycopy(body, index, chunk, 0, chunkSize);
				os.write(chunkHexSize.getBytes());
				os.write(ConfigUtil.CRLF.getBytes());
				os.write(chunk);
				os.write(ConfigUtil.CRLF.getBytes());
				bytesToWrite -= chunkSize;
				index += chunk.length;
				// GC
				chunk = null;
			}
			os.write(lastChunckedMessage.getBytes());

		} else {
			os.write(body);
		}
	}

	public void writeHead(String i_status, String i_contentType,
			int i_contentLength) throws IOException {

		String statusLine = protocol + " " + i_status + ConfigUtil.CRLF;
		String contentTypeLine = "Content-Type: " + i_contentType
				+ ConfigUtil.CRLF;

		String contentLength = "Content-Length: " + i_contentLength
				+ ConfigUtil.CRLF;

		os.writeBytes(statusLine + contentTypeLine + contentLength);

		if (supportsChunks) {
			os.writeBytes(ConfigUtil.CHUNKED_RESPONSE + ConfigUtil.CRLF);
		}
		os.writeBytes(ConfigUtil.CRLF);

		// Print line
		Logger.newLog(statusLine + contentTypeLine + contentLength);
	}

	/**
	 * Send a Get response
	 * 
	 * @param paramsMapGET
	 * @throws InternalErrorException
	 */
	public void sendGetResponse(Map<String, String> paramsMapGET)
			throws InternalErrorException {
		try {
			String contentType = getContentType(path);
			byte[] entityBody = getContent(path, contentType);

			// Send the Headers
			writeHead(ConfigUtil.OK, contentType, entityBody.length);

			// Send the content
			writeBody(entityBody);

		} catch (Exception e) {
			throw new InternalErrorException();
		}
	}

	/**
	 * Sends a Error Response
	 * 
	 * @param i_status
	 */
	public void sendErrorResponse(String i_status) {
		try {
			// Send the body of the error
			String entityBody = "<!DOCTYPE html><HTML><BODY><H1>" + i_status
					+ "</H1></BODY></HTML>";

			writeHead(i_status, ConfigUtil.CONTENT_TYPE_HTML,
					entityBody.length());

			// Write the error body
			writeBody(entityBody.getBytes());

		} catch (Exception e) {
			System.err.println("Could not send error: " + e);

		}

	}

	/**
	 * Sends a Post response
	 * 
	 * @param i_paramsMapPOST
	 * @throws InternalErrorException
	 */
	public void sendPostResponse(Map<String, String> i_paramsMapPOST)
			throws InternalErrorException {
		try {
			String contentType = getContentType(path);

			byte[] entityBody = getContent(path, contentType);

			if (path.endsWith(ConfigUtil.PARAMS_INFO_KEY)) {
				// Deal with params_info
				entityBody = modifyHtmlContent(path, i_paramsMapPOST);
			}

			writeHead(ConfigUtil.OK, contentType, entityBody.length);

			// Send the content of the HTTP.
			writeBody(entityBody);

		} catch (Exception e) {
			System.err.println(e);
			throw new InternalErrorException();
		}
	}

	private byte[] modifyHtmlContent(String path,
			Map<String, String> i_paramsMapPOST) {
		String tableData = "";

		if (i_paramsMapPOST != null) {

			Iterator it = i_paramsMapPOST.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pairs = (Map.Entry) it.next();
				tableData += "<tr><td>" + pairs.getKey() + "</td><td>"
						+ pairs.getValue() + "</td></tr>";
			}
		}

		String html = "<html><body><table>" + tableData
				+ "</table></body></html>";

		return html.getBytes();
	}

	/**
	 * Sends a Head Response
	 * 
	 * @throws InternalErrorException
	 */
	public void sendHeadResponse() throws InternalErrorException {
		try {

			String contentType = getContentType(path);
			byte[] entityBody = getContent(path, contentType);

			writeHead(ConfigUtil.OK, contentType, entityBody.length);
			writeBody(entityBody);

		} catch (Exception e) {
			throw new InternalErrorException();
		}
	}

	/**
	 * Sends a Trace response
	 * 
	 * @throws InternalErrorException
	 */
	public void sendTraceResponse(String i_requestMethod,
			Map<String, String> i_headersMap) throws InternalErrorException {
		// Construct the response message.
		try {
			String headers = "";
			Iterator it = i_headersMap.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pairs = (Map.Entry) it.next();
				headers += pairs.getKey() + ":" + pairs.getValue() + "<br>";
			}

			String contentBody = "<html><body>" + headers + "</body></html>";
			writeHead(ConfigUtil.OK, ConfigUtil.CONTENT_TYPE_HTML,
					contentBody.length());
			writeBody(contentBody.getBytes());

		} catch (IOException e) {
			System.err.println(e);
			throw new InternalErrorException();
		}

	}

	/**
	 * Sends an Option Response
	 * 
	 * @throws InternalErrorException
	 */
	public void sendOptionsResponse() throws InternalErrorException {
		try {

			String supportedMethods = "";

			for (String method : ConfigUtil.SUPPORTED_METHODS) {
				supportedMethods += (method);
				supportedMethods += (",");
			}

			writeHead(ConfigUtil.OK, ConfigUtil.CONTENT_TYPE_HTML,
					supportedMethods.length());
			writeBody(supportedMethods.getBytes());

		} catch (Exception e) {
			System.err.println(e);
			throw new InternalErrorException();

		}

	}

	private String getContentType(String i_path) {

		// See if this is a HTML file
		if (i_path.endsWith(".html") || i_path.endsWith("/logs")
				|| i_path.endsWith("/policies")) {
			return ConfigUtil.CONTENT_TYPE_HTML;
		}

		// See if this is a IMG file
		for (String imgExt : ConfigUtil.SUPPORTED_IMG_FILES) {
			if (i_path.endsWith(imgExt))
				return ConfigUtil.CONTENT_TYPE_IMAGE;
		}

		// Make this a application type

		return ConfigUtil.CONTENT_TYPE_APPLICATION;
	}

	/*
	 * Gets the actual content from the file
	 */
	private byte[] getContent(String i_path, String i_contentType)
			throws InternalErrorException {
		byte[] htmlOpen = "<html><body><div><p>".getBytes();
		byte[] htmlClose = "</p></div></body></html>".getBytes();
		try {
			if (i_path.endsWith("/logs")) {
				byte[] body = ConfigUtil.getLogFile();
				byte[] fullHtml = concat(concat(htmlOpen, body), htmlClose);

				return fullHtml;
			} else if (i_path.endsWith("/policies")) {
				byte[] formOpen = ("<form method='POST' action="
						+ ConfigUtil.PROXY_URL_HOME + "><textarea name='form' rows='4' cols='50'>")
						.getBytes();
				byte[] body = PoliciesUtil.readPoliciesFile();
				byte[] formClose = "</textarea><input type='submit' value='Submit'></form>"
						.getBytes();
				byte[] fullHtml = concat(
						concat(concat(concat(htmlOpen, formOpen), body),
								formClose), htmlClose);
				return fullHtml;
			}
			return Files.readAllBytes(Paths.get(i_path));
		} catch (IOException e) {
			throw new InternalErrorException();
		}
	}

	private byte[] concat(byte[] a, byte[] b) {
		int aLen = a.length;
		int bLen = b.length;
		byte[] c = new byte[aLen + bLen];
		System.arraycopy(a, 0, c, 0, aLen);
		System.arraycopy(b, 0, c, aLen, bLen);
		return c;
	}

}
