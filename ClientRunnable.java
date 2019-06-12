package bg.sofia.uni.fmi.mjt.bookmarksmanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.FileReader;
import java.net.Socket;

public class ClientRunnable implements Runnable {

    //Command from the server to the client to log itself out
    static final String DISCONNECT_COMMAND = "disconnect";
    //Command from the server to the client to import its bookmarks
    static final String IMPORT_FROM_CHROME_COMMAND = "import-from-my-chrome";

    private Client client;
    private Socket socket;
    private boolean infiniteLoop;

    ClientRunnable(Socket socket, Client client, boolean infiniteLoop) {
        this.socket = socket;
        this.client = client;
        this.infiniteLoop = infiniteLoop;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            boolean hasPassed = false;

            while(true) {
                if (hasPassed) {//Used for testing
                    return;
                }
                if (!infiniteLoop) {//Used for testing
                    hasPassed = true;
                }

                String readLine = reader.readLine().trim();
                if (DISCONNECT_COMMAND.equals(readLine)) {

                    if (!socket.isClosed()) {
                        socket.close();
                    }
                    client.logout();
                    return;
                }

                if (IMPORT_FROM_CHROME_COMMAND.equals(readLine)) {
                    importFromChrome();
                    continue;
                }

                System.out.println(readLine);
            }
        } catch (IOException e) { //When server is shut down
            if (!socket.isClosed()) {
                try {
                    socket.close();
                } catch (Exception e1) {
                    System.err.println("could not close socket correctly: " + e1.getMessage());
                }
            }

            client.logout();
            System.out.println("connection lost, try to login again" + System.lineSeparator());
        }
    }

    void importFromChrome() {
        String pathToChrome = null;
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("windows")) {

            String userName = System.getProperty("user.name");
            pathToChrome =
                    "C:/Users/" + userName + "/AppData/Local/Google/Chrome/User Data/Default/Bookmarks";
        } else if (osName.contains("linux")) {

            pathToChrome = //Not sure about this, cannot test it
                    "~/.config/google-chrome/Default/Bookmarks";
        } else if (osName.contains("macos")) {

            String userName = System.getProperty("user.name");
            pathToChrome = //Not sure about this, cannot test it
                    "/Users/" + userName + "/Library/Application/Support/Google/Chrome/Bookmarks";
        } else {
            System.out.println("Not supported os: " + System.getProperty("os.name"));
        }

        if (pathToChrome != null) {
            File f = new File(pathToChrome);
            if(f.exists() && !f.isDirectory()) {
                try (BufferedReader bookmarkReader = new BufferedReader(new FileReader(pathToChrome))) {

                    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                    writer.println("make-collection-auto chrome");

                    String line;
                    while ((line = bookmarkReader.readLine()) != null) {
                        if (line.contains("\"url\"") && !line.contains("\"type\"")) {
                            String url = line.replace("\"url\"", "")
                                    .replaceAll("\"", "")
                                    .replaceFirst(":", "");

                            writer.println("add-to-auto chrome " + url.trim());
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Problem appeared when importing bookmarks from chrome: "
                            + e.getMessage());
                }
            } else {
                System.out.println("There are no chrome bookmarks.");
            }
        }
    }
}