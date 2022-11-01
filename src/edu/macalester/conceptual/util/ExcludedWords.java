package edu.macalester.conceptual.util;

import com.google.common.collect.Streams;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        long wordCount = allDictWords().count();
        System.out.println(wordCount + " prohibited words");
        BloomFilter<String> bloom = BloomFilter.create(Funnels.unencodedCharsFunnel(), wordCount, 0.01);
        allDictWords().forEach(bloom::put);
        bloom.writeTo(new FileOutputStream("res/" + BLOOM_FILE_NAME));
        System.out.println("New prohibited words file created");
    }

    @SuppressWarnings("resource")
    private static Stream<String> allDictWords() throws IOException {
        return Streams.concat(
            // Java reserved words
            Stream.of(
                "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
                "continue", "default", "double", "do", "else", "enum", "extends", "false", "final",
                "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
                "interface", "long", "native", "new", "null", "package", "private", "protected", "public",
                "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw",
                "throws", "transient", "true", "try", "void", "volatile", "while"),

            // All words of reasonable length in dictionary
            Files.lines(Path.of("/usr/share/dict/words"))  // macOS dictionary file
                .filter(word -> word.length() >= 3 && word.length() <= 8));
    }

    public boolean contains(String word) {
        return word.length() > 1 && excludedWords.mightContain(word);
    }

    public void add(String word) {
        excludedWords.put(word);
    }
}
