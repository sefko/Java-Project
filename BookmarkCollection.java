package bg.sofia.uni.fmi.mjt.bookmarksmanager;

import org.jsoup.nodes.Document;

import java.util.HashMap;
import java.util.Map;

class BookmarkCollection {

    private String collectionName;
    private Map<String, Bookmark> bookmarks;

    BookmarkCollection(String collectionName) {
        this.collectionName = collectionName;
        bookmarks = new HashMap<>();
    }

    void addBookmark(Document document) {
        bookmarks.put(document.baseUri(), new Bookmark(document.baseUri(), document));
    }

    void removeBookmark(String url) {
        bookmarks.remove(url);
    }

    String listBookmarks() {
        StringBuilder stringBuilder = new StringBuilder();
        int counter = 0;

        for (Bookmark bookmark : bookmarks.values()) {
            stringBuilder.append(bookmark.getTitle() + " : " + bookmark.getUrl());
            stringBuilder.append(System.lineSeparator());

            ++counter;
        }

        if (stringBuilder.length() >= 1) {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }

        return "Your collection '" + collectionName + "' has "
                + counter + " bookmarks:" + System.lineSeparator()
                + stringBuilder.toString();
    }

    Map<String, Bookmark> getBookmarks() {
        return bookmarks;
    }

    String getCollectionName() {
        return collectionName;
    }

}
