package edu.macalester.conceptual.context;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * Provides a Puzzle with the information it needs to: (1) generate a random puzzle in a
 * reproducible way, (2) created structured, nicely formatted output, and (3) respond to options
 * such as solution visibility and difficulty.
 * <p>
 * A PuzzleContext holds a random number generator, available via {@link #getRandom()}. Puzzle
 * should use that and <i>only</i> that as their source of randomness. Doing so ensures that the
 * same puzzle code produces the same puzzle.
 * <p>
 * Puzzles should use {@link #output()} to handle all puzzle output — no <code>System.out</code>!
 */
public final class PuzzleContext {
    private static final SecureRandom seedGenerator = new SecureRandom();

    private final PuzzleCode code;
    private final Random rand;

    private State state = State.SETUP;
    private boolean solutionsVisible;
    private boolean insideSolution;

    private int curPartNum;

    private PuzzlePrinter printer;
    private Set<Integer> partsToShow;

    /**
     * Creates a new, randomly seeded puzzle context for generating a new puzzle.
     */
    public static PuzzleContext generate(byte puzzleID, byte difficulty) {
        return new PuzzleContext(new PuzzleCode(puzzleID, difficulty, seedGenerator.nextLong()));
    }

    /**
     * Recreates a previously created puzzle context from a puzzle code.
     */
    public static PuzzleContext fromPuzzleCode(String puzzleCode) throws InvalidPuzzleCodeException {
        return new PuzzleContext(PuzzleCode.parse(puzzleCode));
    }

    PuzzleContext(PuzzleCode code) {
        this.code = code;
        rand = new Random(code.seed());
    }

    public String getPuzzleCode() {
        return code.toString();
    }

    public byte getPuzzleID() {
        return code.puzzleID();
    }

    public byte getDifficulty() {
        return code.difficulty();
    }

    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––
    // Lifecycle
    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––

    private enum State {
        SETUP,
        WORKING,
        CLOSED
    }

    private void requireState(State requiredState, String action) {
        if (state != requiredState) {
            throw new IllegalStateException(
                "Cannot " + action + " in " + state + " state;"
                    + " must be in " + requiredState + " state");
        }
    }

    /**
     * Individual {@link edu.macalester.conceptual.Puzzle}s do not call this method.
     */
    public void emitPuzzle(Runnable puzzleGenerator) throws IOException {
        requireState(State.SETUP, "start emitting puzzle");
        if (printer == null) {
            printer = new ConsolePuzzlePrinter();
        }
        PuzzlePrinter printer = this.printer;
        try (printer) {
            state = State.WORKING;
            output().setThemeHue(getRandom().nextFloat());
            output().dividerLine(true);
            puzzleGenerator.run();
            output().dividerLine(true);
        } catch(RuntimeException e) {
            System.out.println();
            System.out.println("Puzzle code caused exception: " + getPuzzleCode());
            throw e;
        } finally {
            this.printer = null;
            state = State.CLOSED;
        }
    }

    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––
    // Output and Structure
    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––

    public PuzzlePrinter output() {
        requireState(State.WORKING, "produce output");
        return printer;
    }

    public void setOutput(PuzzlePrinter printer) {
        Objects.requireNonNull(printer, "printer cannot be null");
        requireState(State.SETUP, "change output");
        this.printer = printer;
    }

    public void setPartsToShow(Set<Integer> partsToShow) {
        requireState(State.SETUP, "change parts to show");
        this.partsToShow = partsToShow;
    }

    public void section(Runnable action) {
        requireState(State.WORKING, "produce output");
        curPartNum++;

        if (curPartNum > 1) {
            output().dividerLine(false);
        }

        boolean hidden = partsToShow != null && !partsToShow.contains(curPartNum);
        if (hidden) {
            output().paragraph("(Skipping part " + curPartNum + ")");
            output().silence();
        }
        try {
            output().heading(currentSectionTitle(), true);
            action.run();
        } finally {
            if (hidden) {
                output().unsilence();
            }
        }
    }

    public String currentSectionTitle() {
        requireState(State.WORKING, "get section title");
        return "Part " + curPartNum;
    }

    public float currentSectionHue() {
        return output().themeHue();
    }

    public void resetSectionCounter() {
        requireState(State.WORKING, "produce output");
        curPartNum = 0;
        output().dividerLine(true);
        output().dividerLine(true);
    }

    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––
    // Problem / solution separation
    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––

    public void enableSolution() {
        requireState(State.SETUP, "change solution visibility");
        solutionsVisible = true;
    }

    public void solution(Runnable action) {
        requireState(State.WORKING, "produce solution");
        if (insideSolution) {
            throw new IllegalStateException("already inside a solution section");
        }

        if (!solutionsVisible || output().isSilenced()) {
            return;
        }

        try {
            insideSolution = true;
            output().heading("Solution", false);
            action.run();
        } finally {
            insideSolution = false;
        }
    }

    public void solutionChecklist(String... items) {
        if (!insideSolution) {
            throw new IllegalStateException("must be inside solution to print solution checklist");
        }
        if (items.length == 0) {
            return;
        }
        output().paragraph(
            "{0,choice,1#Something|1<Things} to double-check in your solution:",
            items.length);
        output().bulletList(items);
    }

    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––
    // Randomness
    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––

    public Random getRandom() {
        requireState(State.WORKING, "generate random numbers");
        if (insideSolution) {
            // Using the RNG inside any of the conditionally executed solution sections would make
            // subsequently generated random numbers differ depending on whether the solution is
            // visible. We want to ensure subsequent puzzle parts are identical regardless of
            // whether solutions are visible.
            throw new IllegalStateException("cannot ask for randomness while inside solution section");
        }
        return rand;
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
