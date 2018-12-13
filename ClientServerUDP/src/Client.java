import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

/**
 * Created by yaroslavkohun on 1/28/18.
 */

class Client {
    public static final String  SERVER_PARAM        = "-server";
    public static final String  PORT_PARAM          = "-port";
    public static final String  FILE_PARAM          = "-file";
    private int                 MAX_DATAGRAM_SIZE   = 508;
    private DatagramSocket      datagramSocket;
    private int                 point               = 0;
    private int                 tmpPoint            = 0;
    private int                 port;
    private String              serverIP;
    private int                 speed;
    private byte[]              sum;
    private String              path;
    private long                fileSize;
    private Timer               timer;
    private long                startTime;
    private long                sendedSize          = 0;
    private ArrayList<Double>   speed10sec          = new ArrayList<>();
    private ArrayList<Double>   allSpeed            = new ArrayList<>();

    private Client(int port, String server, String file) {
        startTime = System.currentTimeMillis();
        try {
            this.port       = port;
            serverIP        = server;
            datagramSocket  = new DatagramSocket();
            path            = file;
            fileSize        = new File(path).length();

            setSum();
            connect(server, port, file);
            getSpeed();
            sendFile(server, port, file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        int port;
        String server;
        String file;
        if (args.length != 6) {
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

            port = Integer.parseInt(config.getProperty(PORT_PARAM));
            server = config.getProperty(SERVER_PARAM);
            file = config.getProperty(FILE_PARAM);
            Client client = new Client(port, server, file);

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


    private void connect(String host, int port, String file) throws Exception {
        File file1 = new File(file);
        String connectStr = file + " " + file1.length();
        DatagramPacket datagramPacket = new DatagramPacket(connectStr.getBytes(),
                0,
                connectStr.getBytes().length,
                InetAddress.getByName(host), port);
        datagramSocket.send(datagramPacket);
    }

    private void sendFile(String host, int port, String filePath) throws Exception {
        File file = new File(filePath);

        FileInputStream fileInputStream = new FileInputStream(filePath);
        byte[] buffer = new byte[(int) file.length()];
        fileInputStream.read(buffer);
        fileInputStream.close();

        timer = new Timer(1000, (ActionEvent e) -> {
            byte[] currentBuffer;
            boolean flag1 = false;
            double speed1 = -1;
            if (buffer.length > point) {
                speed1 = speed;
                if ((buffer.length - point) < speed) {
                    speed1 = buffer.length - point;
                }
                currentBuffer = getCurrentBuffer(buffer, point, speed1);
                point += speed;
                sendedSize += currentBuffer.length;
            } else {
                currentBuffer = "END".getBytes();
                flag1 = true;
            }
            if (!flag1) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Sent: " +sendedSize + " B\tRemained: " + (fileSize - sendedSize) + " B");
                if (speed1 != -1) {
                    stringBuilder.append("\tSpeed: " + String.format("%(.2f",speed1/1024) + " KB/s");
                    speed10sec.add(speed1);
                    allSpeed.add(speed1);
                }
                if (speed10sec.size() == 10) {
                    double sum = 0;
                    for (double i : speed10sec) {
                        sum += i;
                    }
                    stringBuilder.append("\tSpeed/10s: " + String.format("%(.2f",sum/10240) + " KB/s");
                    speed10sec.remove(0);
                } else {
                    stringBuilder.append("\tSpeed/10s: <no info> ");
                }
                double sum = 0;
                for (double i : allSpeed) {
                    sum += i;
                }
                stringBuilder.append("\tAVG Speed: " + String.format("%(.2f",sum/allSpeed.size()/1024.0) + " KB/s");
                System.out.println(stringBuilder);
            }
            try {
                if (currentBuffer.length > 507) {
                    while (tmpPoint < currentBuffer.length) {
                        int step = 507;
                        if ((currentBuffer.length - tmpPoint) < step) {
                            step = currentBuffer.length - tmpPoint;
                        }
                        byte[] tempBuffer = getCurrentBuffer(currentBuffer, tmpPoint, step);

                        DatagramPacket datagramPacket = new DatagramPacket(tempBuffer, 0,
                                tempBuffer.length, InetAddress.getByName(host), port);

                        datagramSocket.send(datagramPacket);

                        tmpPoint += step;
                    }
                    tmpPoint = 0;
                } else {
                    DatagramPacket datagramPacket = new DatagramPacket(currentBuffer, 0,
                            currentBuffer.length, InetAddress.getByName(host), port);

                    datagramSocket.send(datagramPacket);
                }

                if (flag1) {
                    timer.stop();
                }

            } catch (Exception ex) {
                System.err.println(ex.getMessage());
            }
        });

        timer.start();
        while (true) {
            byte[] message = getMessage();
            try {
                double d = Double.parseDouble(new String(message, 0, message.length));
                speed = (int) (d * 1024.0);
            } catch (Exception ex) {
                checkControlSumMD5(message);

                long endTime = System.currentTimeMillis();
                long totalTime = endTime - startTime;
                System.out.println("    Transfer time: " + (double) totalTime / 1000.0 + "s");

                System.exit(0);
            }

        }
    }

    public void checkControlSumMD5(byte[] message){
        boolean flag2 = true;
        if (message.length == sum.length) {
            for (int i = 0; i < message.length; i++) {
                if (message[i] != sum[i]) {
                    flag2 = false;
                    break;
                }
            }
        } else {
            flag2 = false;
        }
        System.out.println("\nTransfer info:");
        try {
            if (flag2) {
                System.out.println("    MD5 sum is correct.");
                DatagramPacket datagramPacket = new DatagramPacket(
                        "YES".getBytes(),
                        0,
                        "YES".getBytes().length,
                        InetAddress.getByName(serverIP), port);
                datagramSocket.send(datagramPacket);
            } else {
                DatagramPacket datagramPacket = new DatagramPacket(
                        "NO".getBytes(),
                        0,
                        "NO".getBytes().length,
                        InetAddress.getByName(serverIP), port);
                datagramSocket.send(datagramPacket);
                System.err.println("    MD5 sum is not correct.");
            }
        }catch (Exception ex){
            System.err.println(ex.getMessage());
        }
        System.out.println("    MD5 sum: ");
        StringBuilder sumStr = new StringBuilder();
        sumStr.append("[ ");
        for (byte b : sum) { sumStr.append(b+", "); }
        sumStr.delete(sumStr.length()-2,sumStr.length()-1);
        sumStr.append("]");
        System.out.println("    "+sumStr);
    }

    public void setSum() {
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            final FileInputStream fileInputStream = new FileInputStream(path);
            byte[] dataBytes = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(dataBytes)) > 0) {
                messageDigest.update(dataBytes, 0, bytesRead);
            }
            sum = messageDigest.digest();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public byte[] getMessage() throws IOException {
        byte[] buffer = new byte[MAX_DATAGRAM_SIZE];
        final DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);

        datagramSocket.receive(datagram);

        return Arrays.copyOf(datagram.getData(), datagram.getLength());
    }

    private byte[] getCurrentBuffer(byte[] buffer, int start, double step) {
        byte[] newBuffer = new byte[(int)step];
        int count = 0;
        for (int i = start; i < start + step; i++) {
            try {
                newBuffer[count] = buffer[i];
            } catch (ArrayIndexOutOfBoundsException ex) {
                ex.printStackTrace();
                newBuffer[count] = 0;
            }
            count++;
        }

        return newBuffer;
    }

    public void getSpeed() throws Exception {
        byte[] buffer = new byte[MAX_DATAGRAM_SIZE];
        final DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);

        datagramSocket.receive(datagram);

        double d = Double.parseDouble(new String(
                Arrays.copyOf(datagram.getData(), datagram.getLength()),
                0, Arrays.copyOf(datagram.getData(),
                datagram.getLength()).length))*1024;
        speed = (int)d;
    }

}
