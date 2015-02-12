import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * This class holds all the constance variables of the project
 * 
 * @author ore
 *
 */
public class ConfigUtil {

	final static String CRLF = "\r\n";

	// Config.ini values
	static int port = 8080;
	static int maxThreads = 5;
	static String root = "/serverroot/";
	static String logPath = "output.log";
	static String defaultPage = "index.html";

	// Config.ini keys
	final static String configPath = "config.ini";
	final static String ROOT_CONFIG = "root";
	final static String MAXTHREADS_CONFIG = "maxThreads";
	final static String DEFAULTPAGE_CONFIG = "defaultPage";
	final static String PORT_CONFIG = "port";
	final static String LOGS = "logPath";
	final static int WEB_PORT = 80;

	// Supported methods
	public static final String GET = "GET";
	public static final String POST = "POST";
	public static final String OPTIONS = "OPTIONS";
	public static final String HEAD = "HEAD";
	public static final String TRACE = "TRACE";
	public static final String CONNECT = "CONNECT";
	final static String[] SUPPORTED_METHODS = { GET, POST, OPTIONS, HEAD,
			TRACE, CONNECT };

	// Protocols
	public static final String DEFAULT_PROTOCOL = "HTTP/1.1";
	final static String[] SUPPORTED_PROTOCOLS = { DEFAULT_PROTOCOL, "HTTP/1.0" };

	// Status codes
	static final String BAD_REQUEST = "400 Bad Request";
	static final String OK = "200 OK";
	static final String ACCESS_DENIED = "403 Access Denied";
	static final String STATUS_OK = DEFAULT_PROTOCOL + " " + OK + CRLF;
	static final String UNIMPLEMENTED_METHOD = "501 Not Implemented";
	static final String INTERNAL_ERROR = "500 Internal Server Error";
	static final String NOT_FOUND = "404 Not Found";

	static final String[] SUPPORTED_IMG_FILES = { ".bmp", ".gif", ".png",
			".jpg", "jpeg" };
	public static final String CONTENT_TYPE_HTML = "text/html";
	public static final String CONTENT_TYPE_IMAGE = "image";
	public static final String CONTENT_TYPE_APPLICATION = "application/octet-stream";
	public static final String CHUNK_HEADER_KEY = "chunked";
	public static final String CHUNK_HEADER_VALUE = "yes";
	public static final String CONTENT_LENGTH_KEY = "Content-Length";
	// After 15 seconds, kill the TCP connection
	public static final int CONNECTION_TIMEOUT = 10 * 1000;

	public static final String CONNECTION = "Connection";
	public static final String CHUNKED_RESPONSE = "transfer-encoding: chunked";
	public static final String KEEP_ALIVE = "keep-alive";

	public static final String PARAMS_INFO_KEY = "params_info.html";

	public static final int CHUNK_SIZE = 3;
	public static final String PROXY_LOGS_URL = "http://content-proxy/logs";
	
	public static final String PROXY_POLICIES_URL = "http://content-proxy/policies";
	public static final String PROXY_URL_HOME = "http://content-proxy/";

	/**
	 * Process the config file
	 * 
	 * @param br
	 * @throws Exception
	 *             if the file reader failed or if the config file is invalid
	 */
	public static void init() throws Exception {
		// Read the config.ini file
		BufferedReader br = new BufferedReader(new FileReader(configPath));
		try {

			String line = br.readLine();

			while (line != null) {

				String[] keyValue = line.split("=");

				// Process the keys and value and put them in the fields
				if (keyValue.length == 2) {
					String key = keyValue[0];
					String value = keyValue[1];
					if (key.equals(ROOT_CONFIG))
						setRoot(value);
					else if (key.equals(PORT_CONFIG))
						setPort(Integer.parseInt(value));
					else if (key.equals(DEFAULTPAGE_CONFIG))
						setDefaultPage(value);
					else if (key.equals(MAXTHREADS_CONFIG))
						setMaxThreads(Integer.parseInt(value));
					else if (key.equals(LOGS))
						setLogPath(value);
					line = br.readLine();
				} else {
					// Problem parsing the config file
					throw new Exception();
				}
			}
		} finally {
			br.close();
		}
	}

	private static void setLogPath(String value) {
		logPath = value;

	}

	public static int getPort() {
		return port;
	}

	public static void setPort(int i_port) {
		port = i_port;
	}

	public static int getMaxThreads() {
		return maxThreads;
	}

	public static void setMaxThreads(int i_maxThreads) {
		maxThreads = i_maxThreads;
	}

	public static String getRoot() {
		return root;
	}

	public static void setRoot(String i_root) {
		root = i_root;
	}

	public static String getDefaultPage() {
		return defaultPage;
	}

	public static void setDefaultPage(String i_defaultPage) {
		defaultPage = i_defaultPage;
	}

	public static boolean isSupportedMethod(String i_method) {
		for (String method : SUPPORTED_METHODS) {
			if (i_method.equals(method))
				return true;
		}
		return false;
	}

	public static String getLogPath() {
		return logPath;
	}

	public static boolean isPoliciesPath(String path) {
		return (path.equalsIgnoreCase(PROXY_LOGS_URL) || path
				.equalsIgnoreCase(PROXY_POLICIES_URL) || path.equalsIgnoreCase(PROXY_URL_HOME));
	}
	
	public static byte[] getLogFile() throws IOException{
		return Files.readAllBytes(Paths.get(logPath));
	}

}
