import java.util.Properties;

public class Client {
    public static final String SERVER_PARAM = "-server";
    public static final String PORT_PARAM   = "-port";
    public static final String FILE_PARAM   = "-file";

    public static void main(String[] args) {
	if ( args.length != 6 ) {
	   System.out.println("usage: java Client -server <address>");
	   System.out.println("                   -port <port number>");
	   System.out.println("                   -file <file name>");
	   return;
	}

	Properties config = new Properties();
	config.setProperty(SERVER_PARAM, "");
	config.setProperty(PORT_PARAM, "");
	config.setProperty(FILE_PARAM, "");

	try {
	   checkParameter(args[0], config);
	   checkParameter(args[2], config);
	   checkParameter(args[4], config);

	   config.setProperty(args[0], args[1]);	   
	   config.setProperty(args[2], args[3]);	   
	   config.setProperty(args[4], args[5]);

	   Sender sender = new Sender(config);
	   sender.sendFile();

	} catch (java.io.IOException e) {
	   System.err.println("I/O ERROR: " + e.getMessage());	
	   e.printStackTrace(System.out);
	} catch (Exception e) {
	   System.err.println("ERROR: " + e.getMessage());	
	   e.printStackTrace(System.out);
 	}	

    }

    private static void checkParameter(String paramName, Properties config) {	
	if ( config.getProperty(paramName) == null ) {
	   throw new RuntimeException("undefined parameter name \"" + paramName + "\".");
	}
    }
}