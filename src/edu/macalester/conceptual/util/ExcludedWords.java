package edu.macalester.conceptual.util;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Tracks prohibited and previous used nonsense words. Backed by a bloom filter, populated from a
 * file that is pre-seeded with all Java reserved words plus not-too-long words from the dictionary.
 */
@SuppressWarnings("UnstableApiUsage")
class ExcludedWords {
    private static final String BLOOM_FILE_NAME = "nonsense-prohibited.bloom";

    private final BloomFilter<String> excludedWords;

    ExcludedWords() {
        BloomFilter<String> bloom = null;
        try (var bloomInput = Nonsense.class.getClassLoader().getResourceAsStream(BLOOM_FILE_NAME)) {
            if (bloomInput != null) {
                bloom = BloomFilter.readFrom(bloomInput, Funnels.unencodedCharsFunnel());
            }
        } catch(IOException e) {
            e.printStackTrace();
        }

        if (bloom == null) {
            System.out.println("WARNING: Unable to read res/" + BLOOM_FILE_NAME
                + "; no nonsense words will be prohibited");
            bloom = BloomFilter.create(Funnels.unencodedCharsFunnel(), 100);
        }
        excludedWords = bloom;
    }

    /*
     * Regenerates the serialized prohibited words file from the macOS dict file + all Java keywords.
     */
    public static void main(String[] args) throws IOException {
        var words = allDictWords();
        System.out.println(words.size() + " prohibited words");
        BloomFilter<String> bloom = BloomFilter.create(Funnels.unencodedCharsFunnel(), words.size(), 0.01);
        words.forEach(bloom::put);
        bloom.writeTo(new FileOutputStream("res/" + BLOOM_FILE_NAME));
        System.out.println("New prohibited words file created");
    }

    @SuppressWarnings("resource")
    private static List<String> allDictWords() throws IOException {
        var dicts = Stream.of(
            Path.of("res/java-reserved-words.txt").toUri().toURL(),
            new URL("https://raw.githubusercontent.com/dwyl/english-words/master/words_alpha.txt"));

        return dicts.flatMap(url -> {
            try {
                System.out.println("Reading words from " + url);
                return new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))
                    .lines()
                    .filter(word -> word.length() >= 3 && word.length() <= 8);
            } catch(IOException e) {
                throw new RuntimeException("Unable to read words from " + url, e);
            }
        }).toList();  // Bloom needs a count _before_ the traversal, so just slurp them all into memory
    }

    public boolean contains(String word) {
        return word.length() > 1 && excludedWords.mightContain(word);
    }

    public void add(String word) {
        excludedWords.put(word);
    }
}
