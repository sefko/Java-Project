package bg.sofia.uni.fmi.mjt.bookmarksmanager;

import opennlp.tools.stemmer.PorterStemmer;
import org.jsoup.nodes.Document;

import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.PriorityQueue;

import static java.lang.Math.min;

public class Bookmark {

    private static final int NUMBER_OF_TAGS_PER_BOOKMARK = 20;
    private static final String PATH_TO_STOPWORDS_FILE = "resources/stopwords.txt";
    private static Set<String> stopwords = null;

    private static final PorterStemmer ps = new PorterStemmer();

    private String url;
    private String title;
    private Set<String> tags;

    Bookmark(String url, Document document) {
        this.url = url;

        title = document.title();
        tags = extractTags(document);
    }

    boolean containsTags(Set<String> tags) {
        for (String tag : tags) {
            if (this.tags.contains(ps.stem(tag.toLowerCase()))) {
                return true;
            }
        }
        return false;
    }

    boolean containsInTitle(String title) {
        if (this.title.toLowerCase().contains(title.toLowerCase())) {
            return true;
        }
        return false;
    }

    private static Set<String> extractTags(Document document) {
        if (stopwords == null) {

            stopwords = new HashSet<>();

            try (BufferedReader stopwordsBR = new BufferedReader(new InputStreamReader(
                    new FileInputStream(PATH_TO_STOPWORDS_FILE), StandardCharsets.UTF_8))) {

                stopwordsBR.lines().forEach(line -> stopwords.add(line));
            } catch (FileNotFoundException e) {
                System.err.println("File " + PATH_TO_STOPWORDS_FILE + " was not found: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Problem with input/output: " + e.getMessage());
            }
        }

        Set<String> finalKeywords = new LinkedHashSet<>();

        if (document.body() == null) {
            return finalKeywords;
        }

        String text = document.body().text();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))))) {

            Map<String, Word> wordEncounters = new HashMap<>();

            String line;
            while ((line = reader.readLine()) != null) {

                String[] words = line.trim().toLowerCase().split("[\\p{Punct}\\s]+");

                for (String word : words) {
                    String stemmedWord = ps.stem(word);

                    if (!stopwords.contains(word)) {
                        if (wordEncounters.get(stemmedWord) == null) {
                            wordEncounters.put(stemmedWord, new Word(stemmedWord));
                        } else {
                            wordEncounters.get(stemmedWord).newOccurrence();
                        }
                    }
                }
            }

            PriorityQueue<Word> sortedWordEncounter = new PriorityQueue<>();
            sortedWordEncounter.addAll(wordEncounters.values());

            int tagsToExtract = min(NUMBER_OF_TAGS_PER_BOOKMARK, sortedWordEncounter.size());

            for (int i = 0; i < tagsToExtract; ++i) {
                Word word = sortedWordEncounter.poll();
                if (word.occurrences > 1) { //Don't add useless tags
                    finalKeywords.add(word.word);
                }
            }
        } catch (IOException e) {
            System.err.println("Problem with input/output: " + e.getMessage());
        }

        return finalKeywords;
    }

    String getUrl() {
        return url;
    }

    String getTitle() {
        return title;
    }

    String printAllTags() {
        StringBuilder stringBuilder = new StringBuilder();

        for (String tag : tags) {
            stringBuilder.append(tag + ", ");
        }

        return stringBuilder.toString();
    }
}

class Word implements Comparable<Word>{

    String word;
    int occurrences;

    Word(String word) {
        this.word = word;
        occurrences = 1;
    }

    void newOccurrence() {
        ++occurrences;
    }

    /*
     * Compares the words by their occurrences, not by the word itself
     */
    @Override
    public int compareTo(Word word) {
        if (this.occurrences >= word.occurrences) {
            return -1;
        } else {
            return 1;
        }
    }
}
