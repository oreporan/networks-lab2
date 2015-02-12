import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

public class PoliciesUtil {
	public static final String BLOCK_SITE_KEY = "block-site";
	public static final String BLOCK_RESOURCE_KEY = "block-resource";
	public static final String BLOCK_IP_MASK_KEY = "block-ip-mask";
	public static ArrayList<String> BLOCK_SITE_VALUE;
	public static ArrayList<String> BLOCK_RESOURCE_VALUE;
	public static ArrayList<String> BLOCK_IP_MASK_VALUE;
	static String policiesPath = null;

	public static boolean reInit() {
		try{
		init(policiesPath);
		return true;
		}catch(Exception e){
			return false;
		}
	}

	public static void init(String path) throws Exception {
		// Read the policies.ini file
		policiesPath = path;
		BufferedReader br = new BufferedReader(new FileReader(path));
		try {
			String line = br.readLine();
			BLOCK_IP_MASK_VALUE = new ArrayList<String>();
			BLOCK_RESOURCE_VALUE = new ArrayList<String>();
			BLOCK_SITE_VALUE = new ArrayList<String>();

			while (line != null) {

				String[] keyValue = line.split(" ");
				// Process the keys and value and put them in the fields
				if (keyValue.length == 2) {
					String key = keyValue[0];
					String value = keyValue[1];
					if (value.contains("\"")) {
						value = value.substring(value.indexOf("\"") + 1,
								value.lastIndexOf("\""));
					} else {
						throw new Exception(); // Invalid policy
					}
					if (key.equals(BLOCK_IP_MASK_KEY))
						BLOCK_IP_MASK_VALUE.add(value);
					else if (key.equals(BLOCK_RESOURCE_KEY))
						BLOCK_RESOURCE_VALUE.add(value);
					else if (key.equals(BLOCK_SITE_KEY))
						BLOCK_SITE_VALUE.add(value);
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

	public static byte[] readPoliciesFile() throws IOException {
		return Files.readAllBytes(Paths.get(policiesPath));
	}

	public static boolean legalPath(String hostname) {
		return isLegalSite(hostname) && isLegalResource(hostname)
				&& isLegalIP(hostname);
	}

	private static boolean isLegalSite(String i_host) {
		for (String policy : BLOCK_SITE_VALUE) {
			if (i_host.contains(policy))
				return false;
		}
		return true;
	}

	private static boolean isLegalResource(String i_ext) {
		for (String policy : BLOCK_RESOURCE_VALUE) {
			if (i_ext.contains(policy))
				return false;
		}
		return true;
	}

	private static boolean isLegalIP(String i_host) {
		try {
			// Get the IP of the host (DNS)
			InetAddress address = InetAddress.getByName(new URL(i_host).getHost());
			String ipToCheck = address.getHostAddress();
			for (String policy : BLOCK_IP_MASK_VALUE) {
				String[] ipAndMask = policy.split("/");
				if (matchSubnet(ipAndMask[0], ipToCheck, ipAndMask[1])) {
					return false;
				}
			}
			
			return true;
		} catch (UnknownHostException | MalformedURLException e) {
			return false;
		}
	}

	public static void writeToPoliciesFile(byte[] data) throws IOException {
		String dataString = new String(data);
		dataString = dataString.replaceAll("%22", "\"");
		dataString = dataString.replaceAll("%2F", "/");
		dataString = dataString.replaceAll("\\+", " ");
		dataString = dataString.replaceAll("%0D%0A", ConfigUtil.CRLF);
		//String newString = dataString;
		Files.deleteIfExists(Paths.get(policiesPath));
		Files.write(Paths.get(policiesPath), dataString.getBytes(),
				StandardOpenOption.CREATE_NEW);
	}

	/*
	 * Returns true if this IP is contained in the the policy IP/mask
	 */
	public static boolean matchSubnet(String i_policyIP, String i_ipToCheck,
			String i_mask) {
		String[] separatedToCheck = i_ipToCheck.split("\\.");
		String[] separatedPolicy = i_policyIP.split("\\.");
		int mask = Integer.parseInt(i_mask);
		int numOfBits = 8;
		int numOfWholeSeperators = mask / numOfBits;
		int remainder = mask % numOfBits;
		for (int i = 0; i < numOfWholeSeperators; i++) {
			// Deal with the easy part
			if (!separatedPolicy[i].equals(separatedToCheck[i])) {
				return false;
			}
		}
		// Deal with hard part
		if (remainder > 0) {
			int limit = (int) Math.pow(2, numOfBits - remainder);
			int remainingSeparatorToCheck = Integer
					.parseInt(separatedToCheck[numOfWholeSeperators]);
			int remaingSeparatorPolicy = Integer
					.parseInt(separatedPolicy[numOfWholeSeperators]);
			if (remainingSeparatorToCheck >= remaingSeparatorPolicy
					&& remainingSeparatorToCheck <= Math.min(
							remaingSeparatorPolicy + limit - 1, 255)) {
				return true;
			} else {
				return false;
			}

		}

		return true;
	}

}
