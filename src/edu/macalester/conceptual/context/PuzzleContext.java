package edu.macalester.conceptual.context;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.zip.CRC32;

public final class PuzzleContext {
    private static final SecureRandom seedGenerator = new SecureRandom();

    private final long seed;
    private final Random rand;

    private State state = State.SETUP;
    private boolean solutionsVisible;
    private boolean insideSolution;

    private int curPartNum;

    private PuzzlePrinter printer;

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

    static enum State {
        SETUP,
        WORKING,
        CLOSED;
    }

    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––
    // Problem / solution separation
    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––

    public void enableSolution() {
        requireState(State.SETUP, "change solution visibility");
        solutionsVisible = true;
    }

    public void emitPuzzle(Runnable puzzleGenerator) {
        requireState(State.SETUP, "start emitting puzzle");
        try {
            state = State.WORKING;
            puzzleGenerator.run();
            printer.close();
        } finally {
            state = State.CLOSED;
        }
    }

    public void setOutput(PuzzlePrinter printer) {
        Objects.requireNonNull(printer, "printer cannot be null");
        requireState(State.SETUP, "change output");
        this.printer = printer;
    }

    public PuzzlePrinter output() {
        requireState(State.WORKING, "produce output");
        if (printer == null) {
            printer = new PuzzlePrinter();
        }
        return printer;
    }

    public void section(Runnable action) {
        requireState(State.WORKING, "produce output");
        curPartNum++;
        output().dividerLine();
        output().heading("Part " + curPartNum);
        output().dividerLine();
        output().blankLine();
        action.run();
    }

    public void solution(Runnable action) {
        requireState(State.WORKING, "produce solution");
        if (insideSolution) {
            throw new IllegalStateException("already inside a solution section");
        }

        if (!solutionsVisible) {
            return;
        }

        try {
            insideSolution = true;
            output().indented("┆", () -> {
                output().dividerLine();
                output().heading("Solution");
                output().blankLine();
                action.run();
                output().dividerLine();
            });
            output().blankLine();
        } finally {
            insideSolution = false;
        }
    }

    private void requireState(State requiredState, String action) {
        if (state != requiredState) {
            throw new IllegalStateException(
                "Cannot " + action + " in " + state + " state;"
                    + " must be in " + requiredState + " state");
        }
    }

    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––
    // Randomness
    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––

    public Random getRandom() {
        if (insideSolution) {
            // Using the RNG inside any of the conditionally executed solution sections would make
            // subsequently generated random numbers differ depending on whether the solution
            // is visible.
            throw new IllegalStateException("cannot ask for randomness while inside solution section");
        }
        return rand;
    }

    @SafeVarargs
    public final <Choice> Choice choose(Choice... choices) {
        return choices[getRandom().nextInt(choices.length)];
    }

    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––
    // Puzzle code handling
    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––

    public String getPuzzleCode() {
        return addChecksum(seed)
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
            throw new InvalidPuzzleCodeException("Invalid format; this does not look like a puzzle code");
        }
    }

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

    private static BigInteger addChecksum(long num) {
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

    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––
    // Debug
    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––

    @Override
    public String toString() {
        return "PuzzleContext{"
            + "code=" + getPuzzleCode()
            + '}';
    }
}
