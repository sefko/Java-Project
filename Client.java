package bg.sofia.uni.fmi.mjt.bookmarksmanager;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    static final String REGISTER_COMMAND = "register";
    static final String LOGIN_COMMAND = "login";

    private static final String ADDRESS = "localhost";
    private static final int PORT = 8080;

    private Socket socket;
    private boolean isConnected = false;
    private PrintWriter writer;

    public static void main(String[] args) {
        new Client().run(System.in, true);
    }

    void run(InputStream inputStream, boolean infiniteLoop) {
        try (Scanner scanner = new Scanner(inputStream)) {
            boolean hasPassed = false;

            while (true){
                if (hasPassed) {//For testing
                    return;
                }
                if (!infiniteLoop) {//For testing
                    hasPassed = true;
                }

                String input = scanner.nextLine().trim();

                if ("".equals(input)) {
                    continue;
                }

                String[] tokens = input.split("\\s+");
                String command = tokens[0];
                if (REGISTER_COMMAND.equals(command) || LOGIN_COMMAND.equals(command)) {
                    if (isConnected()) {
                        System.out.println("you are already logged in" + System.lineSeparator());
                        continue;
                    }

                    sendConnectRequest(input);
                } else { // a server command is received
                    if (isConnected()) {
                        writer.println(input);
                    } else {
                        System.out.println("you need to login first" + System.lineSeparator());
                    }
                }
            }
        }
    }

    public void logout() {
        if (socket != null && !socket.isClosed()) {
            try {
                writer.close();
                socket.close();
            } catch (IOException e) {
                System.err.println("Connection did not close properly: " + e.getMessage());
            }
        }

        if (!isConnected()){
            System.out.println("you need to login first" + System.lineSeparator());
        } else {
            isConnected = false;
            System.out.println("logged out");
        }
    }

    void sendConnectRequest(String connectionMessage) {
        try {
            socket = createSocket(ADDRESS, PORT);
            initializeWriter(socket.getOutputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            writer.println(connectionMessage);
            String response = reader.readLine();
            System.out.println(response);

            if ("successfully logged in".equals(response)) {
                new Thread(new ClientRunnable(socket, this, true)).start();
                isConnected = true;
            } else {
                System.out.println("");
                reader.close();
                writer.close();
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Problem with connection: " + e.getMessage());
        }
    }

    void initializeWriter(OutputStream outputStream) {
        writer = new PrintWriter(outputStream, true);
    }

    Socket createSocket(String address, int port) throws IOException {
        return new Socket(address, port);
    }

    boolean isConnected() {
        return isConnected;
    }
}