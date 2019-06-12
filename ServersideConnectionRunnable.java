package bg.sofia.uni.fmi.mjt.bookmarksmanager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class ServersideConnectionRunnable implements Runnable {

    static final String MAKE_COLLECTION_COMMAND = "make-collection";
    static final String ADD_COMMAND = "add";
    static final String ADD_TO_COMMAND = "add-to";
    static final String REMOVE_FROM_COMMAND = "remove-from";
    static final String LIST_ALL_COMMAND = "list-all";
    static final String LIST_ALL_WITH_TAGS_COMMAND = "list-all-tags";
    static final String LIST_ALL_FROM_COLLECTION_COMMAND = "list";
    static final String SEARCH_COMMAND = "search";
    static final String SEARCH_BY_TAGS_DASH = "-tags";
    static final String SEARCH_BY_TITLE_DASH = "-title";
    static final String IMPORT_BOOKMARKS_FROM_CHROME_COMMAND = "import-from-chrome";
    static final String INFORMATION_COMMAND = "help";
    static final String LOGOUT_COMMAND = "logout";


    private static final String ALL_COMMANDS = MAKE_COLLECTION_COMMAND + " <collection-name>" + System.lineSeparator()
            + ADD_COMMAND + " <link>" + System.lineSeparator()
            + ADD_TO_COMMAND + " <collection> <link>" + System.lineSeparator()
            + REMOVE_FROM_COMMAND + " <collection> <link>" + System.lineSeparator()
            + LIST_ALL_COMMAND + System.lineSeparator()
            + LIST_ALL_WITH_TAGS_COMMAND + System.lineSeparator()
            + LIST_ALL_FROM_COLLECTION_COMMAND + " <collection>" + System.lineSeparator()
            + SEARCH_COMMAND + " " + SEARCH_BY_TAGS_DASH + " <tag1> <tag2> <tag3> ..." + System.lineSeparator()
            + SEARCH_COMMAND + " " + SEARCH_BY_TITLE_DASH + " <title>" + System.lineSeparator()
            + IMPORT_BOOKMARKS_FROM_CHROME_COMMAND + System.lineSeparator()
            + INFORMATION_COMMAND + System.lineSeparator()
            + LOGOUT_COMMAND + System.lineSeparator();

    private Executor savingExecutor;
    private String username;
    private Socket clientSocket;
    private Server server;
    private boolean infiniteLoop;

    ServersideConnectionRunnable(Executor savingExecutor
            , String username, Socket clientSocket, Server server, boolean infiniteLoop) {

        this.savingExecutor = savingExecutor;
        this.username = username;
        this.clientSocket = clientSocket;
        this.server = server;
        this.infiniteLoop = infiniteLoop;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));

             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

            writer.println("type '" + INFORMATION_COMMAND + "' to see all commands");
            writer.println("");

            boolean hasPassed = false;

            while (true) {
                if (hasPassed) {
                    return;
                }
                if (!infiniteLoop) {
                    hasPassed = true;
                }

                savingExecutor.execute(new SavingThread(server));
                String commandInput = reader.readLine().trim();

                String[] tokens = commandInput.split("\\s+");
                String command = tokens[0];

                if (MAKE_COLLECTION_COMMAND.equals(command)) {

                    writer.println(makeBookmarkCollection(tokens) + System.lineSeparator());
                } else if (ADD_COMMAND.equals(command)) {

                    writer.println(addBookmark(tokens, true) + System.lineSeparator());
                } else if (ADD_TO_COMMAND.equals(command)) {

                    writer.println(addBookmark(tokens, false) + System.lineSeparator());
                } else if (REMOVE_FROM_COMMAND.equals(command)) {

                    writer.println(removeBookmark(tokens) + System.lineSeparator());
                } else if (LIST_ALL_COMMAND.equals(commandInput)) {

                    writer.println(listAllBookmarks());
                } else if (LIST_ALL_FROM_COLLECTION_COMMAND.equals(command)) {

                    writer.println(listBookmarks(tokens));
                } else if (SEARCH_COMMAND.equals(command)) {
                    if (tokens.length >= 3) {
                        String dash = tokens[1];

                        if (SEARCH_BY_TAGS_DASH.equals(dash)) {

                            writer.println(searchByTags(commandInput, command, dash));
                        } else if (SEARCH_BY_TITLE_DASH.equals(dash)) {

                            writer.println(searchByTitle(tokens));
                        } else {
                            writer.println("unknown parameters," + System.lineSeparator()
                                    + " try: search -tags <tag1> <tag2> <tag3> ..." + System.lineSeparator()
                                    + "or: search -title <title>");
                        }
                    } else {
                        writer.println("unknown parameters," + System.lineSeparator()
                                + " try: search -tags <tag1> <tag2> <tag3> ..." + System.lineSeparator()
                                + "or: search -title <title>");
                    }
                } else if (IMPORT_BOOKMARKS_FROM_CHROME_COMMAND.equals(commandInput)) {

                    writer.println("import-from-my-chrome");
                } else if ("make-collection-auto".equals(command)) {

                    makeBookmarkCollection(tokens);
                } else if ("add-to-auto".equals(command)) {

                    writer.println(addBookmark(tokens, false));
                } else if (LIST_ALL_WITH_TAGS_COMMAND.equals(commandInput)) {

                    writer.println(listAllBookmarksWithTags(tokens));
                } else if (LOGOUT_COMMAND.equals(commandInput)) {

                    server.getUsers().get(username).logout();
                    writer.println("disconnect");

                    reader.close();
                    clientSocket.close();
                } else if (INFORMATION_COMMAND.equals(commandInput.trim())) {

                    writer.println(ALL_COMMANDS);
                } else {

                    writer.println("unknown command '" + commandInput
                            + "', type 'help' to see all commands");

                    writer.println("");
                }
            }
        } catch (IOException e) { //When a client is shut down, the client is marked logged out
            server.logoutUser(username);
        }
    }

    private String listAllBookmarksWithTags(String[] tokens) {

        return server.getUsers().get(username).listAllBookmarksWithTags();
    }

    private String searchByTitle(String[] tokens) {
        if (tokens.length == 3) {
            String title = tokens[2];

            return server.getUsers().get(username).searchByTitle(title);
        }

        return "unknown parameters, try: search -title <title>";
    }

    private String searchByTags(String commandInput, String command, String dash) {
        String tagsString = commandInput.replaceAll("\\s+", " ")
                .replace(command + " " + dash, "");

        String[] tagsArray = tagsString.trim().split("\\s+");

        Set<String> tags = Arrays.stream(tagsArray).collect(Collectors.toSet());

        return server.getUsers().get(username).searchByTags(tags);
    }

    private String listBookmarks(String[] tokens) {
        if (tokens.length == 2) {
            String collectionName = tokens[1];

            return server.getUsers().get(username).listBookmarks(collectionName);
        }

        return "unknown parameters, try: list <collection>";
    }

    private String listAllBookmarks() {
        return server.getUsers().get(username).listAllBookmarks();
    }

    private String removeBookmark(String[] tokens) {
        if (tokens.length == 3) {
            String collectionName = tokens[1];
            String url = tokens[2];
            url = removeProtocol(url);

            return server.getUsers().get(username).removeBookmark(collectionName, url);
        }

        return "unknown parameters, try: remove-from <collection> <link>";
    }

    private String addBookmark(String[] tokens, boolean addToDefault) {
        if (tokens.length == 2 && addToDefault) {
            String collectionName = "default";
            String url = tokens[1];
            url = removeProtocol(url);

            return server.getUsers().get(username).addBookmark(collectionName, url);
        } else if (tokens.length == 3 && !addToDefault) {
            String collectionName = tokens[1];
            String url = tokens[2];
            url = removeProtocol(url);

            return server.getUsers().get(username).addBookmark(collectionName, url);
        }

        if (addToDefault) {
            return "unknown parameters, use: add <link>";
        }
        return "unknown parameters, use: add-to <collection> <link>";
    }

    private String makeBookmarkCollection(String[] tokens) {
        if (tokens.length == 2) {
            String collectionName = tokens[1];
            return server.getUsers().get(username).makeBookmarkCollection(collectionName);
        }

        return "unknown parameters, use: make-collection <collection-name>";
    }

    private String removeProtocol(String url) {
        if (url.startsWith("https://")) {
            return url.replaceFirst("https://", "");
        } else if (url.startsWith("http://")) {
            return url.replaceFirst("http://", "");
        }
        return url;
    }
}