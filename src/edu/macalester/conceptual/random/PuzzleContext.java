package edu.macalester.conceptual.random;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
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
    // The seed format is as follows:
    //
    //     1 byte: leading zero
    //     8 bytes: actual long seed (big-endian)
    //     2 bytes: CRC-32 checksum
    //

    // We convert that to and from base 36 (0-9, a-z) with hyphens for legibility.

    private static BigInteger addChucksum(long num) {
        var bytes = ByteBuffer.allocate(11);
        bytes.put((byte) 0);  // treat long as unsigned
        bytes.putLong(num);
        bytes.putShort(computeChecksum(bytes));
        return new BigInteger(bytes.array());
    }

    private static long checkAndStripChecksum(BigInteger num) throws InvalidPuzzleCodeException {
        var rawBytes = num.toByteArray();
        if (rawBytes.length > 11) {
            throw new InvalidPuzzleCodeException("Seed code is too long");
        }

        ByteBuffer bytes = ByteBuffer.allocate(11);
        bytes.position(bytes.limit() - rawBytes.length);  // leading zeros
        bytes.put(rawBytes);

        short checksum = bytes.getShort(9);
        bytes.putShort(9, (short) 0);
        if (checksum != computeChecksum(bytes)) {
            throw new InvalidPuzzleCodeException("Checksum does not match; is there a typo?");
        }

        return bytes.getLong(1);
    }

    private static short computeChecksum(ByteBuffer bytes) {
        CRC32 checksum = new CRC32();
        checksum.update(bytes.array());
        return (short) checksum.getValue();
    }
}
