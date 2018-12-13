import java.io.*;
import java.net.*;

public class Receiver {

    private int speed = 0;

    private BufferedReader reader   = null;
    private PrintWriter writer      = null;

    public Receiver(int speed) throws Exception {
        this.speed = speed;
    }

    public void receive(Socket socket) throws Exception {
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

        String fileName = getFileName();
        System.out.println("Receiving file name: " + fileName);

        sendSpeed();

        FileWriter fileWriter = new FileWriter(fileName);
        int fileReadBytes = 0;
        char[] buffer = new char[speed];

        while ((fileReadBytes = reader.read(buffer, 0, buffer.length)) > 0) {
            fileWriter.write(buffer, 0, fileReadBytes);
        }

        System.out.println("File: " + fileName + "  -received");

        fileWriter.close();
        reader.close();
        writer.close();
    }

    private void sendSpeed() throws Exception {
        writer.println(speed + "");
    }

    private String getFileName() throws Exception {
        return reader.readLine();
    }

}