import java.util.Properties;
import java.net.ServerSocket;

public class Server {
	public static final String PORT_PARAM 	= "-port";
	public static final String SPEED_PARAM 	= "-speed";

	public static void main(String[] args) {
		if (args.length != 4) {
			System.out.println("usage: java Server -port <port number> ");
			System.out.println("                   -speed <speednumber> KB/s");
			return;
		}

		Properties config = new Properties();
		config.setProperty(SPEED_PARAM, "");
		config.setProperty(PORT_PARAM, "");

		try {
			checkParameter(args[0], config);
			checkParameter(args[2], config);

			config.setProperty(args[0], args[1]);
			config.setProperty(args[2], args[3]);

			ServerSocket socket = new ServerSocket(
					Integer.parseInt(
							config.getProperty(PORT_PARAM)));

			Receiver receiver = new Receiver(Integer.parseInt(
					config.getProperty(SPEED_PARAM)));

			while (true) {
				receiver.receive(socket.accept());
			}

		} catch (java.io.IOException e) {
			System.err.println("I/O ERROR: " + e.getMessage());
			e.printStackTrace(System.out);
		} catch (Exception e) {
			System.err.println("ERROR: " + e.getMessage());
			e.printStackTrace(System.out);
		}
	}

	private static void checkParameter(String paramName, Properties config) {
		if (config.getProperty(paramName) == null) {
			throw new RuntimeException("undefined parameter name \"" + paramName + "\".");
		}
	}
}