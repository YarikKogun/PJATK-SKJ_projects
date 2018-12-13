import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

/**
 * Created by yaroslavkohun on 1/28/18.
 */

class Server {
    public static final String  PORT_PARAM           = "-port";
    public static final String  SPEED_PARAM          = "-speed";
    private int                 MAX_DATAGRAM_SIZE    = 508;
    private DatagramSocket      datagramSocket;
    private ArrayList<byte[]>   data                 = new ArrayList<>();
    private String              fileName;
    private int                 fileSize;
    private InetAddress         ip;
    private int                 port;
    private double              speed;

    public Server(int port, double speed) throws SocketException {
        this.speed      = speed;
        datagramSocket  = new DatagramSocket(port);
        try {
            System.out.println("DatagramSocket address: " + InetAddress.getLocalHost().getHostAddress());
            System.out.println("DatagramSocket port:    " + datagramSocket.getLocalPort());
            System.out.println("Waiting for connections...");
        }catch (Exception ex){
            ex.printStackTrace();
        }

    }

    public static void main(String[] args) {
        int port1;
        double speed1;

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

            port1   = Integer.parseInt(config.getProperty(PORT_PARAM));
            speed1  = Double.parseDouble(config.getProperty(SPEED_PARAM));

            Server server = new Server(port1, speed1);

            while (true) { server.receive(); }

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

    private void receive() throws Exception {
        byte[] buff = new byte[MAX_DATAGRAM_SIZE];
        final DatagramPacket datagramPacket = new DatagramPacket(buff, buff.length);

        datagramSocket.receive(datagramPacket);

        byte[] data = Arrays.copyOf(datagramPacket.getData(), datagramPacket.getLength());

        String[] tab = new String(data, 0, data.length).split(" ");

        fileName    = getFileName(tab[0]);
        fileSize    = Integer.parseInt(tab[1]);
        ip          = datagramPacket.getAddress();
        port = datagramPacket.getPort();

        sendSpeed(datagramPacket.getAddress(), datagramPacket.getPort());

        System.out.println("Receiving file:         " + fileName);
        System.out.println("File size:              " + fileSize/1024+" KB");
        System.out.println("Current speed:          " + speed +" KB/s");

        Thread thread = new Thread(() -> {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                System.out.print("Enter new speed (KB/s): ");
                try {
                    this.speed = Double.parseDouble(bufferedReader.readLine());
                    sendSpeed(ip, port);
                    System.out.println("Speed changed.");
                    System.out.println("Current speed:          "+this.speed+" KB/s");
                } catch (Exception ex) {
                    System.err.println("ERROR!!!");
                }
                System.out.println();
            }
        });
        thread.start();

        byte[] mes = getMessage();

        while (!(new String(mes, 0, mes.length).equals("END"))) {
            this.data.add(mes);
            mes = getMessage();
        }

        writeToFile();

        mes = getMessage();

        if (new String(mes, 0, mes.length).equals("YES")) {
            System.out.println("    Transfer complete succefully.");
            System.out.println("\nWaiting for connections...");
        } else {
            System.out.println("    Transfer doesn`t complete succefully.");
            System.out.println("\nWaiting for connections...");
        }

    }

    private void writeToFile() throws Exception {
        byte[] file = new byte[fileSize];
        int count = 0;
        for (byte[] b : data) {
            for (int i = 0; i < b.length; i++) {
                try {
                    file[count] = b[i];
                    count++;
                } catch (ArrayIndexOutOfBoundsException ex) {
                    break;
                }
            }
        }

        File file1 = new File(fileName);

        if (file1.exists()) { file1.delete(); }

        FileOutputStream fileOutputStream = new FileOutputStream(file1);

        fileOutputStream.write(file, 0, file.length);

        fileOutputStream.close();
        data.clear();
        setSum(fileName);
    }

    public void setSum(String path) {
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            final FileInputStream fileInputStream = new FileInputStream(path);
            byte[] dataBytes = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(dataBytes)) > 0) {
                messageDigest.update(dataBytes, 0, bytesRead);
            }
            byte[] sum = messageDigest.digest();

            System.out.println("\n\nTransfer info:");
            System.out.println("    MD5 sum sent.");

            System.out.println("    MD5 sum: ");
            StringBuilder sumStr = new StringBuilder();
            sumStr.append("[ ");
            for (byte b : sum) { sumStr.append(b+", "); }
            sumStr.delete(sumStr.length()-2,sumStr.length()-1);
            sumStr.append("]");
            System.out.println("    "+sumStr);

            DatagramPacket datagramPacket = new DatagramPacket(sum, 0, sum.length, ip, port);
            datagramSocket.send(datagramPacket);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendSpeed(InetAddress host, int port) throws Exception {
        byte[] current = ("" + speed).getBytes();
        DatagramPacket query = new DatagramPacket(current, 0, current.length, host, port);

        datagramSocket.send(query);
    }

    public byte[] getMessage() throws IOException {
        byte[] buffer = new byte[MAX_DATAGRAM_SIZE];
        final DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);

        datagramSocket.receive(datagram);

        return Arrays.copyOf(datagram.getData(), datagram.getLength());
    }

    public String getFileName(String path){
        String[] tab = path.split("/");

        return path.split("/")[tab.length - 1];
    }
}