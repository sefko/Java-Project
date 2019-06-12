package bg.sofia.uni.fmi.mjt.bookmarksmanager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;


public class Server {
    private static final int PORT = 8080;
    private static final int THREAD_COUNT = 100;
    public static final String PATH_TO_REGISTERED_USERS_FILE = "resources/registered_users.json";
    private static final Type TYPE_OF_USERS = new TypeToken<ConcurrentHashMap<String, User>>() {}.getType();

    static final String REGISTER_COMMAND = "register";
    static final String LOGIN_COMMAND = "login";

    private static final Gson gson = new Gson();

    private ExecutorService savingExecutor;

    private ExecutorService executor;
    private ServerSocket serverSocket;
    private ConcurrentMap<String, User> users;

    public Server(ServerSocket serverSocket, String filePath) {
        savingExecutor = Executors.newFixedThreadPool(1);
        executor = Executors.newFixedThreadPool(THREAD_COUNT);
        this.serverSocket = serverSocket;

        JsonReader reader = null;
        try {
            reader = new JsonReader(new FileReader(filePath));
        } catch (FileNotFoundException e) {
            File dir = new File("resources");
            dir.mkdirs();
            File tmp = new File(dir, "registered_users.json");

            try {
                tmp.createNewFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(tmp));
                writer.write("{}");
                writer.close();
            } catch (IOException e1) {
                System.err.println("Could not create file: " + e1.getMessage());
            }
        }

        if (reader != null) {
            users = gson.fromJson(reader, TYPE_OF_USERS);

            for (User user : getUsers().values()) {

                user.logout();
            }
        } else {
            users = new ConcurrentHashMap<>();
        }
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            new Server(serverSocket, PATH_TO_REGISTERED_USERS_FILE).run(true);
        } catch (IOException e) {
            System.out.println("there is another instance of the server running\nshutting down");
        }
    }

    public void run(boolean infiniteLoop) {
        System.out.println("server is running");

        boolean hasPassed = false;

        try {
            while (true) {
                if (hasPassed) {//Used for testing
                    return;
                }
                if (!infiniteLoop) {//Used for testing
                    hasPassed = true;
                }

                Socket clientSocket = serverSocket.accept();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));

                String loginOrRegisterRequest = reader.readLine(); //Here the client will always send request
                String[] tokens = loginOrRegisterRequest.split("\\s+");

                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

                if (REGISTER_COMMAND.equals(tokens[0]) && tokens.length == 3) {

                    writer.println(tryRegister(tokens));
                    closeConnection(clientSocket, writer, reader);
                } else if (LOGIN_COMMAND.equals(tokens[0]) && tokens.length == 3) {

                    String response = tryLogin(tokens, clientSocket);
                    writer.println(response);
                    String username = tokens[1];

                    if (getUsers().containsKey(username) && getUsers().get(username).isLoggedIn()) {
                        continue;
                    }
                    closeConnection(clientSocket, writer, reader);
                } else if (LOGIN_COMMAND.equals(tokens[0])) {

                    writer.println("unknown parameters, use command: login <username> <password>");
                    closeConnection(clientSocket, writer, reader);
                } else if (REGISTER_COMMAND.equals(tokens[0])) {

                    writer.println("unknown parameters, use command: register <username> <password>");
                    closeConnection(clientSocket, writer, reader);
                }
            }
        } catch (IOException e) {
            System.err.println("Problem with input/output: " + e.getMessage());
        }
    }

    private String tryRegister(String[] tokens) {
        String username = tokens[1];
        String passwordHash = DigestUtils.sha256Hex(tokens[2]);

        if (getUsers().containsKey(username)) {
            return "the username '" + username + "' is already taken";
        }

        users.put(username, new User(username, passwordHash));
        savingExecutor.execute(new SavingThread(this));
        return ("successfully registered with username '" + username
                + "', login with command: login <username> <password>");
    }

    private String tryLogin(String[] tokens, Socket clientSocket) {
        String username = tokens[1];
        String passwordHash = DigestUtils.sha256Hex(tokens[2]);

        if (getUsers().containsKey(username) && !getUsers().get(username).isLoggedIn()) {

            if(getUsers().get(username).logIn(passwordHash)) {

                executor.execute(new ServersideConnectionRunnable(savingExecutor
                        , username, clientSocket, this, true));

                return "successfully logged in";
            } else {
                return "wrong password";
            }
        } else if (getUsers().containsKey(username)) {

            return "a user with username '" + username + "' is already logged in";
        }

        return "the username '" + username
                + "' is not recognized, register with command: register <username> <password>";
    }

    private void closeConnection(Socket socket, Writer writer, Reader reader) throws IOException {
        reader.close();
        socket.close();
    }

    public void logoutUser(String username) {
        getUsers().get(username).logout();
    }

    public Map<String, User> getUsers() {
        return users;
    }
}