package bg.sofia.uni.fmi.mjt.bookmarksmanager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class User {

    private String username;
    private String passwordHash;
    private Map<String, BookmarkCollection> bookmarkCollections;
    private boolean isLoggedIn;

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
        bookmarkCollections = new HashMap<>();
        bookmarkCollections.put("default", new BookmarkCollection("default"));
        isLoggedIn = false;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public boolean logIn(String passwordHash) {
        if (this.passwordHash.equals(passwordHash)) {
            isLoggedIn = true;
            return true; //Successfully logged in
        }
        else {
            return false; //Did not log in
        }
    }

    public void logout() {
        isLoggedIn = false;
    }

    public String makeBookmarkCollection(String collectionName) {
        if (bookmarkCollections.containsKey(collectionName)) {

            return "collection with the name '" + collectionName + "' already exists";
        }
        bookmarkCollections.put(collectionName, new BookmarkCollection(collectionName));

        return "collection '" + collectionName + "' successfully added";
    }

    public String addBookmark(String collectionName, String url) {
        if (bookmarkCollections.containsKey(collectionName)) {
            Document document = getDocument(url);

            if (document == null) {
                return "bookmark was not added, could not connect to '" + url + "'";
            }

            String newUrl = document.baseUri();

            if (!bookmarkCollections.get(collectionName).getBookmarks().containsKey(newUrl)) {
                bookmarkCollections.get(collectionName).addBookmark(document);

                return "bookmark '" + newUrl + "' successfully added to '" + collectionName + "'";
            }

            return "bookmark '" + newUrl + "' already exists in '" + collectionName + "'";
        }

        return "collection '" + collectionName + "' does not exist";
    }

    public String removeBookmark(String collectionName, String url) {
        if (bookmarkCollections.containsKey(collectionName)) {
            Document document = getDocument(url);

            String urlToRemove;

            if (document == null) {
                urlToRemove = url;
            } else {
                urlToRemove = document.baseUri();
            }

            if (bookmarkCollections.get(collectionName).getBookmarks().containsKey(urlToRemove)) {
                bookmarkCollections.get(collectionName).removeBookmark(urlToRemove);
                return "bookmark '" + urlToRemove + "' successfully removed from '" + collectionName + "'";
            }

            return "bookmark '" + url + "' does not exist in collection '" + collectionName + "'";
        }

        return "collection '" + collectionName + "' does not exist";
    }

    public String listAllBookmarks() {
        StringBuilder stringBuilder = new StringBuilder();
        int counter = 0;

        for (BookmarkCollection collection : bookmarkCollections.values()) {
            for (Bookmark bookmark : collection.getBookmarks().values()) {
                stringBuilder.append("In collection '" + collection.getCollectionName()
                        + "' : " + bookmark.getTitle() + " : " + bookmark.getUrl());
                stringBuilder.append(System.lineSeparator());
                ++counter;
            }
        }

        if (stringBuilder.length() >= 1) {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }

        return "You have " + counter + " bookmarks:" + System.lineSeparator()
                + stringBuilder.toString();
    }

    public String listBookmarks(String collectionName) {
        if (bookmarkCollections.containsKey(collectionName)) {

            return bookmarkCollections.get(collectionName).listBookmarks();
        }
        return "no collection with the name '" + collectionName + "'";
    }

    public String searchByTags(Set<String> tags) {
        StringBuilder stringBuilder = new StringBuilder();
        int counter = 0;

        for (BookmarkCollection collection : bookmarkCollections.values()) {
            for (Bookmark bookmark : collection.getBookmarks().values()) {
                if (bookmark.containsTags(tags)) {
                    stringBuilder.append("In collection '" + collection.getCollectionName()
                            + "' : " + bookmark.getTitle() + " : " + bookmark.getUrl());
                    stringBuilder.append(System.lineSeparator());

                    ++counter;
                }
            }
        }

        if (stringBuilder.length() >= 1) {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }

        return "Found " + counter + " bookmarks:" + System.lineSeparator() + stringBuilder.toString();
    }

    public String searchByTitle(String title) {
        StringBuilder stringBuilder = new StringBuilder();
        int counter = 0;

        for (BookmarkCollection collection : bookmarkCollections.values()) {
            for (Bookmark bookmark : collection.getBookmarks().values()) {
                if (bookmark.containsInTitle(title)) {
                    stringBuilder.append("In collection '" + collection.getCollectionName()
                            + "' : " + bookmark.getTitle() + " : " + bookmark.getUrl());
                    stringBuilder.append(System.lineSeparator());

                    ++counter;
                }
            }
        }

        if (stringBuilder.length() >= 1) {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }

        return "Found " + counter + " bookmarks:" + System.lineSeparator() + stringBuilder.toString();
    }

    public String listAllBookmarksWithTags() {
        StringBuilder stringBuilder = new StringBuilder();
        int counter = 0;

        for (BookmarkCollection collection : bookmarkCollections.values()) {
            for (Bookmark bookmark : collection.getBookmarks().values()) {
                stringBuilder.append("In collection '" + collection.getCollectionName()
                        + "' : " + bookmark.getTitle()
                        + " : " + bookmark.getUrl()
                        + " : " + bookmark.printAllTags());
                stringBuilder.append(System.lineSeparator());
                ++counter;
            }
        }

        if (stringBuilder.length() >= 1) {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }

        return "You have " + counter + " bookmarks:" + System.lineSeparator()
                + stringBuilder.toString();
    }

    private static Document getDocument(String url) {
        Document document = null;

        try {
            document = Jsoup.connect("https://" + url).get();
            return document;
        } catch (IOException e) {
            try {
                document = Jsoup.connect("http://" + url).get();
                return document;
            } catch (IOException e1) {
                return document;
            }
        }
    }
}
