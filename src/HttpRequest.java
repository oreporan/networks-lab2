import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.util.HashMap;

/**
 * This class is initiated by the TCP connection class and parses/handles a
 * request. It reads the input stream, validates the request and the headers In
 * the case where the request is not valid, or any of the validations fail (i.e
 * file not found, or unsupported method) - an error is thrown which calls for
 * an Http Error Response method with the appropriate response. In the case
 * where the connection with the client is lost at some time, an exception is
 * thrown.
 * 
 * @author ore
 *
 */
final class HttpRequest {

	HashMap<String, String> headersMap;

	HashMap<String, String> paramsMapGET;

	HashMap<String, String> paramsMapPOST;
	String requestMethod;
	String requestPath;
	InputStream sis;
	String requestProtocol;
	public boolean isPersistent = false;
	private String errorMessage;

	/*
	 * Constructor - accepts a InputStream and OutputStream for sending to the
	 * response
	 */
	public HttpRequest(InputStream inputStream) throws Exception {
		this.sis = inputStream;
		headersMap = new HashMap<String, String>();
		processRequest();
	}

	/*
	 * Validate the request, save the headers, the body (in case of POST) and
	 * finally create a response and send
	 */
	public void processRequest() throws IOException {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(sis));
			String request = null;
			if (br.ready()) {
				request = br.readLine(); // Get the request
				Logger.newLog(request);
			} else {
				throw new BadRequestException();
			}

			// Loop through the request headers
			if (br.ready()) {
				String requestHeaders = br.readLine();
				while (requestHeaders != ConfigUtil.CRLF
						&& requestHeaders.split(": ").length >= 2) {
					String[] keyValue = requestHeaders.split(": ");
					headersMap.put(keyValue[0], keyValue[1]);
					requestHeaders = br.readLine();
					// Print request to Console
					Logger.log(requestHeaders);
				}
			}

			// Validate the request
			validateRequest(request);

			// Get content-length of the body of the request
			String contentLength = headersMap
					.get(ConfigUtil.CONTENT_LENGTH_KEY);
			// If this is a POST request, save the body
			if (contentLength != null && requestMethod.equals(ConfigUtil.POST)) {
				processRequestBody(br, contentLength);

			}

		} catch (WebServerException ex) {
			this.setErrorMessage(ex.getMessage());
		} catch (Exception e) {
			System.err.println("Error processing request:"
					+ e.getLocalizedMessage());
		}

	}

	/*
	 * When the request is invalid - set the error message
	 */
	private void setErrorMessage(String i_ex) {
		this.errorMessage = i_ex;

	}

	/*
	 * Get the error message of this request - when one exists
	 */
	public String getErrorMessage() {
		return this.errorMessage;
	}

	private void processRequestBody(BufferedReader br, String contentLength)
			throws Exception {
		int length = Integer.parseInt(contentLength);

		paramsMapPOST = new HashMap<String, String>();

		// Get the body of this request
		StringBuilder requestContent = new StringBuilder();
		for (int i = 0; i < length; i++) {
			requestContent.append((char) br.read());
		}
		Logger.log(requestContent.toString());
		String bodyContent = requestContent.toString();
		String postInfo = bodyContent.split("form=")[1];
		//Re-initialize the Policies File
		
		PoliciesUtil.writeToPoliciesFile(postInfo.getBytes());
		if(!PoliciesUtil.reInit()){
			//Bad request by user!
			this.errorMessage = "It seems you don't know how to write Policies! need help?";
			
		}

	}

	private void validateRequest(String request) throws WebServerException {

		// Begin validation on request
		String[] requestArr = request.split(" ");
		if (requestArr.length != 3) {
			// Invalid HTTP request
			throw new BadRequestException();
		} else {
			validateHttpProtocol(requestArr[2]);
			validateHttpMethod(requestArr[0]);
			validateHttpPath(requestArr[1]);
		}

	}

	private void validateHttpPath(String i_path) throws WebServerException {
		String root = ConfigUtil.getRoot();
		String defaultPage = ConfigUtil.getDefaultPage();
		String pathToFetch = root;
		String path = i_path;
		// In case there are Params in the path
		if (i_path.contains("?")) {
			String[] pathArr = i_path.split("\\?");
			path = pathArr[0];
			String params = pathArr[1];

			// Init the GET params map
			paramsMapGET = new HashMap<String, String>();

			for (String param : params.split("&")) {
				String[] keyValueArr = param.split("=");
				if (keyValueArr.length == 2) {
					paramsMapGET.put(keyValueArr[0].toString(),
							keyValueArr[1].toString());
				} else {
					// The params are not legal
					throw new BadRequestException();
				}
			}
		}

		if (path.startsWith("../") || path.equals(ConfigUtil.PROXY_URL_HOME)) {
			// The case where the default page is called
			pathToFetch = pathToFetch + "/" + defaultPage;
		} else {
			// User requests a page different from default page
			pathToFetch = path;
		}
		// Sets this path
		setRequestPath(pathToFetch);

	}

	private void validateHttpProtocol(String protocol)
			throws BadRequestException {
		boolean found = false;
		for (String supportedProtocol : ConfigUtil.SUPPORTED_PROTOCOLS) {
			if (supportedProtocol.equalsIgnoreCase(protocol)) {
				setRequestProtocol(protocol);
				found = true;
			}
		}
		if (!found)
			throw new BadRequestException();
	}

	private void validateHttpMethod(String i_method)
			throws UnimplementedMethodException {
		boolean found = false;
		for (String supportedMethod : ConfigUtil.SUPPORTED_METHODS) {
			if (supportedMethod.equalsIgnoreCase(i_method)) {
				// Save the method
				setRequestMethod(i_method);
				found = true;
			}
		}
		if (!found)
			throw new UnimplementedMethodException();
	}

	public String getRequestMethod() {
		return requestMethod;
	}

	public void setRequestMethod(String requestMethod) {
		this.requestMethod = requestMethod;
	}

	public String getRequestPath() {
		return requestPath;
	}

	public void setRequestPath(String requestPath) {
		this.requestPath = requestPath;
	}

	public String getRequestProtocol() {
		return requestProtocol;
	}

	public void setRequestProtocol(String requestProtocol) {
		this.requestProtocol = requestProtocol;
	}

	public HashMap<String, String> getParamsMapGET() {
		return paramsMapGET;
	}

	public HashMap<String, String> getParamsMapPOST() {
		return paramsMapPOST;
	}

	public HashMap<String, String> getHeadersMap() {
		return headersMap;
	}

	/*
	 * Returns true if the TCP connection wants to stay alive
	 */
	public boolean isPersistent() {
		String connection = headersMap.get(ConfigUtil.CONNECTION);
		return (connection != null && headersMap.get(ConfigUtil.CONNECTION)
				.equalsIgnoreCase(ConfigUtil.KEEP_ALIVE));
	}

	/*
	 * Mark if this request wants response in chunks
	 */
	public boolean supportChunks() {
		String chunk = headersMap.get(ConfigUtil.CHUNK_HEADER_KEY);
		return (chunk != null && chunk.equals(ConfigUtil.CHUNK_HEADER_VALUE));
	}
}
