package edu.macalester.conceptual.random;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.CRC32;

public class PuzzleContext {
    private static final SecureRandom seedGenerator = new SecureRandom();

    private final long seed;
    private final Random rand;

    public static PuzzleContext generate() {
        return new PuzzleContext(seedGenerator.nextLong());
    }

    public static PuzzleContext fromPuzzleCode(String puzzleCode) throws InvalidPuzzleCodeException {
        return new PuzzleContext(parsePuzzleCode(puzzleCode));
    }

    PuzzleContext(long seed) {
        this.seed = seed;
        rand = new Random(seed);
    }

    public Random getRandom() {
        return rand;
    }

    public <Choice> Choice choose(Choice... choices) {
        return choices[getRandom().nextInt(choices.length)];
    }

    public String getPuzzleCode() {
        return addChucksum(seed)
            .toString(36)
            .replaceAll("l", "L")  // Avoid ambiguous chars (lowercase i and o are fine)
            .replaceAll(".{4}(?=.)", "$0-");
    }

    private static long parsePuzzleCode(String puzzleCode) throws InvalidPuzzleCodeException {
        var cleanedPuzzleCode = puzzleCode
            .replaceAll("-", "");
            // No need to handle L → l; BigInteger is case-insensitive
        try {
            return checkAndStripChecksum(new BigInteger(cleanedPuzzleCode, 36));
        } catch (NumberFormatException nfe) {
            throw new InvalidPuzzleCodeException("Invalid seed code format");
        }
    }

    @Override
    public String toString() {
        return "PuzzleContext{"
            + "seed=" + seed
            + '}';
    }

    // ------ Checksum computation ------

    private enum CodeLayout {
        // The puzzle code is a number encoded in base 36 (0-9, a-z) with hyphens for legibility.
        // Once in its numeric form, the number has the following big-endian byte layout:
        PUZZLE_NUMBER(1), // puzzle ID number
        RANDOM_SEED(8),   // big-endian long seed for RNG
        CHECKSUM(2);      // CRC-32 checksum (low 16 bits)

        private final int size;

        CodeLayout(int size) {
            this.size = size;
        }

        int size() {
            return size;
        }

        int offset() {
            return offsetTo(this);
        }

        static int totalSize() {
            return offsetTo(null);
        }

        private static int offsetTo(CodeLayout target) {
            return Arrays.stream(values())
                .takeWhile(component -> component != target)
                .mapToInt(CodeLayout::size)
                .sum();
        }
    }

    private static BigInteger addChucksum(long num) {
        var bytes = ByteBuffer.allocate(CodeLayout.totalSize());
        bytes.putLong(CodeLayout.RANDOM_SEED.offset(), num);
        bytes.putShort(CodeLayout.CHECKSUM.offset(), computeChecksum(bytes));
        return new BigInteger(bytes.array());
    }

    private static long checkAndStripChecksum(BigInteger num) throws InvalidPuzzleCodeException {
        var rawBytes = num.toByteArray();
        if (rawBytes.length > CodeLayout.totalSize()) {
            throw new InvalidPuzzleCodeException("Seed code is too long");
        }

        ByteBuffer bytes = ByteBuffer.allocate(CodeLayout.totalSize());
        bytes.position(bytes.limit() - rawBytes.length);  // leading zeros
        bytes.put(rawBytes);

        short checksum = bytes.getShort(CodeLayout.CHECKSUM.offset());
        bytes.putShort(CodeLayout.CHECKSUM.offset(), (short) 0);  // checksum was computed when field was 0
        if (checksum != computeChecksum(bytes)) {
            throw new InvalidPuzzleCodeException("Checksum does not match; is there a typo?");
        }

        return bytes.getLong(CodeLayout.RANDOM_SEED.offset());
    }

    private static short computeChecksum(ByteBuffer bytes) {
        CRC32 checksum = new CRC32();
        checksum.update(bytes.array());
        return (short) checksum.getValue();
    }
}
