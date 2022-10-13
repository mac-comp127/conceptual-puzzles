package edu.macalester.conceptual.context;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

final class PuzzleCode {
    final byte puzzleID;
    final long seed;

    public static PuzzleCode parse(String puzzleCode) throws InvalidPuzzleCodeException {
        var cleanedPuzzleCode = puzzleCode
            .replaceAll("-", "");
        // No need to handle L → l; BigInteger is case-insensitive
        try {
            return checkAndStripChecksum(new BigInteger(cleanedPuzzleCode, 36));
        } catch (NumberFormatException nfe) {
            throw new InvalidPuzzleCodeException("Invalid format; this does not look like a puzzle code");
        }
    }

    public PuzzleCode(byte puzzleID, long seed) {
        if (puzzleID < 0) {
            throw new IllegalArgumentException("puzzleID must be positive (negative values would break the encoding)");
        }
        this.puzzleID = puzzleID;
        this.seed = seed;
    }

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
        RANDOM_SEED(8),  // big-endian long seed for RNG
        CHECKSUM(2);     // CRC-32 checksum (low 16 bits)

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
        bytes.putLong(CodeLayout.RANDOM_SEED.offset(), seed);
        bytes.putShort(CodeLayout.CHECKSUM.offset(), computeChecksum(bytes));
        return new BigInteger(bytes.array());
    }

    private static PuzzleCode checkAndStripChecksum(BigInteger num) throws InvalidPuzzleCodeException {
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

        return new PuzzleCode(
            bytes.get(CodeLayout.PUZZLE_ID.offset()),
            bytes.getLong(CodeLayout.RANDOM_SEED.offset()));
    }

    private static short computeChecksum(ByteBuffer bytes) {
        CRC32 checksum = new CRC32();
        checksum.update(bytes.array());
        return (short) checksum.getValue();
    }
}
