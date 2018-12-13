import java.io.*;
import java.net.*;
import java.util.Properties;

public class Sender {

	private File file = null;

	private BufferedReader reader 	= null;
	private PrintWriter writer 		= null;

	public Sender(Properties config) throws Exception {
		String server = config.getProperty(Client.SERVER_PARAM);
		int port = Integer.parseInt(config.getProperty(Client.PORT_PARAM));
		String path = config.getProperty(Client.FILE_PARAM);

		file = new File(path);
		if (!file.exists()) {
			throw new IOException("file \"" + path + "\" not found.");
		}

		Socket socket = new Socket(server, port);

		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
	}

	public void sendFile() throws Exception {

		sendFileName(file.getName());

		int speed = getSpeed();
		int bytesRead = 0;
		int total = 0;

		FileReader fileReader = new FileReader(file.getPath());
		char[] buffer = new char[speed];
		int lengthFile = (int) file.length();
		while ((bytesRead = fileReader.read(buffer, 0, buffer.length)) > 0) {
			total += bytesRead;
			writer.write(buffer, 0, bytesRead);
			writer.flush();
			System.out.printf("bytes transfered: %,d; bytes left: %,d\n", total, lengthFile - total);
			Thread.sleep(1000);
		}
		fileReader.close();

		reader.close();
		writer.close();
	}

	private void sendFileName(String name) throws Exception {
		writer.println(name);
	}

	private int getSpeed() throws Exception {
		String speedValue = reader.readLine();
		return Integer.parseInt(speedValue);
	}

}