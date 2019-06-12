package bg.sofia.uni.fmi.mjt.bookmarksmanager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class SavingThread implements Runnable {

    Server server;

    public SavingThread(Server server) {
        this.server = server;
    }

    @Override
    public void run() {
        try (Writer fileWriter = new FileWriter(server.PATH_TO_REGISTERED_USERS_FILE)) {
            Gson gsonBuilder = new GsonBuilder().create();
            gsonBuilder.toJson(server.getUsers(), fileWriter);
        } catch (IOException e) {
            System.err.println("The data cannot be saved: " + e.getMessage());
        }
    }
}
