package edu.macalester.conceptual.context;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * Handles encoding and decoding of the puzzle codes the CLI provides to the user.
 *
 * @param seed The random number generator seed for the puzzle to use
 */
record PuzzleCode(
    byte puzzleID,
    byte difficulty,
    long seed
) {
    /**
     * Decodes the given puzzle code.
     */
    public static PuzzleCode parse(String puzzleCode) throws InvalidPuzzleCodeException {
        var cleanedPuzzleCode = puzzleCode.replaceAll("-", "");
        // No need to handle L → l; BigInteger is case-insensitive

        try {
            return checkAndStripChecksum(new BigInteger(cleanedPuzzleCode, 36));
        } catch (NumberFormatException nfe) {
            throw new InvalidPuzzleCodeException("Invalid format; this does not look like a puzzle code");
        }
    }

    PuzzleCode {
        if (puzzleID < 0) {
            throw new IllegalArgumentException("puzzleID must be positive (negative values would break the encoding)");
        }
    }

    /**
     * Encodes this puzzle code as a user-presentable string.
     */
    public String toString() {
        return packAndAddChecksum()
            .toString(36)
            .replaceAll("l", "L")  // Avoid ambiguous chars (lowercase i and o are fine)
            .replaceAll(".{4}(?=.)", "$0-");
    }

    private enum CodeLayout {
        // The puzzle code is a number encoded in base 36 (0-9, a-z) with hyphens for legibility.
        // Once in its numeric form, the number has the following big-endian byte layout:
        PUZZLE_ID(1),    // puzzle ID number
        DIFFICULTY(1),   // puzzle difficulty
        RANDOM_SEED(8),  // big-endian long seed for RNG
        CHECKSUM(1);     // CRC-32 checksum (just low 8 bits; this isn’t bank records)

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

    private BigInteger packAndAddChecksum() {
        var bytes = ByteBuffer.allocate(CodeLayout.totalSize());
        bytes.put(CodeLayout.PUZZLE_ID.offset(), puzzleID);
        bytes.put(CodeLayout.DIFFICULTY.offset(), difficulty);
        bytes.putLong(CodeLayout.RANDOM_SEED.offset(), seed);
        bytes.put(CodeLayout.CHECKSUM.offset(), computeChecksum(bytes));
        return new BigInteger(bytes.array());
    }

    private static PuzzleCode checkAndStripChecksum(BigInteger num) throws InvalidPuzzleCodeException {
        var rawBytes = num.toByteArray();
        if (rawBytes.length > CodeLayout.totalSize()) {
            throw new InvalidPuzzleCodeException("Seed code is too long");
        }

        ByteBuffer bytes = ByteBuffer.allocate(CodeLayout.totalSize());
        bytes.position(bytes.limit() - rawBytes.length);  // include leading zeros
        bytes.put(rawBytes);

        byte checksum = bytes.get(CodeLayout.CHECKSUM.offset());
        bytes.put(CodeLayout.CHECKSUM.offset(), (byte) 0);  // checksum was computed when field was 0
        if (checksum != computeChecksum(bytes)) {
            throw new InvalidPuzzleCodeException(
                MessageFormat.format(
                    "Checksum does not match; is there a typo? ({0} != {1})",
                    checksum, computeChecksum(bytes)));
        }

        return new PuzzleCode(
            bytes.get(CodeLayout.PUZZLE_ID.offset()),
            bytes.get(CodeLayout.DIFFICULTY.offset()),
            bytes.getLong(CodeLayout.RANDOM_SEED.offset()));
    }

    private static byte computeChecksum(ByteBuffer bytes) {
        CRC32 checksum = new CRC32();
        checksum.update(bytes.array());
        return (byte) checksum.getValue();
    }
}
