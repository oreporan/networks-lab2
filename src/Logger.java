import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Date;
import java.text.SimpleDateFormat;

public class Logger {
	static File file;

	public static void init(String i_file) {

		// writer = new FileWriter(i_file, true);
		file = new File(i_file);
		System.out.println("Writing Logs at: " + i_file);

	}

	public static void error(String msg) {
		writeMessage(false, false, msg);
		System.err.println(msg);
	}

	public static void log(String msg) {
		appendMessage(msg);
		System.out.println(msg);
	}

	private static void appendMessage(String msg) {
		writeMessage(true, true, msg);
	}

	public static void newLog(String msg) {
		writeMessage(false, true, msg);
		System.out.println(msg);
	}

	private static String timeStamp() {
		Date myDate = new Date(System.currentTimeMillis());
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
		String myDateString = sdf.format(myDate);
		return "[" + myDateString + "]";
	}

	private static synchronized void writeMessage(boolean append, boolean log, String msg) {
		FileWriter writer = null;
		try {
			writer = new FileWriter(file, true);
			if (!append) {
				String logStatus = "[LOG]";
				if (!log)
					logStatus = "[ERROR]";
				writer.write("<h5> ========" + timeStamp() + logStatus
						+ "========" + "</h5>" + ConfigUtil.CRLF);
			}
			writer.write("<p>" + msg + "</p>" + ConfigUtil.CRLF);
		} catch (IOException e) {
			System.err.println("Could not write to Logger: " + e);
		} finally {
			try {
				writer.close();
			} catch (Exception e) {
				System.err.println("Corrupt Log file: " + e);
			}
		}
	}

	public static String getLogFile() {
		BufferedReader reader = null;
		String str = null;
		try {
			String s = null;
			reader = new BufferedReader(new FileReader(file));
			while ((s = reader.readLine()) != null) {
				str += s;
			}
		} catch (IOException e) {
			return null;
		} finally {
			try {
				reader.close();
				return str;
			} catch (IOException e) {
				System.err.println("Could not read Log file:" + e);
			}
		}
		return null;
	}
}
