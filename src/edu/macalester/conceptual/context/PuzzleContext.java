package edu.macalester.conceptual.context;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

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

    public static PuzzleContext generate(byte puzzleID) {
        return new PuzzleContext(new PuzzleCode(puzzleID, seedGenerator.nextLong()));
    }

    public static PuzzleContext fromPuzzleCode(String puzzleCode) throws InvalidPuzzleCodeException {
        return new PuzzleContext(PuzzleCode.parse(puzzleCode));
    }

    PuzzleContext(PuzzleCode code) {
        this.code = code;
        rand = new Random(code.seed);
    }

    public String getPuzzleCode() {
        return code.toString();
    }

    public byte getPuzzleID() {
        return code.puzzleID;
    }

    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––
    // Lifecycle
    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––

    enum State {
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

    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––
    // Problem / solution separation
    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––

    public void enableSolution() {
        requireState(State.SETUP, "change solution visibility");
        solutionsVisible = true;
    }

    public void emitPuzzle(Runnable puzzleGenerator) {
        requireState(State.SETUP, "start emitting puzzle");
        if (printer == null) {
            printer = new PuzzlePrinter();
        }
        try {
            state = State.WORKING;
            output().setColorTheme(getRandom().nextFloat());
            output().dividerLine(true);
            puzzleGenerator.run();
            output().dividerLine(true);
        } finally {
            printer.close();
            printer = null;
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
        return printer;
    }

    public void setPartsToShow(Set<Integer> partsToShow) {
        this.partsToShow = partsToShow;
    }

    public void section(Runnable action) {
        requireState(State.WORKING, "produce output");
        curPartNum++;

        boolean hidden = partsToShow != null && !partsToShow.contains(curPartNum);
        if (hidden) {
            output().silence();
        }
        try {
            if (curPartNum > 1) {
                output().dividerLine(false);
            }
            output().heading("Part " + curPartNum, true);
            action.run();
        } finally {
            if (hidden) {
                output().unsilence();
            }
        }
    }

    public void resetSectionCounter() {
        requireState(State.WORKING, "produce output");
        curPartNum = 0;
        output().dividerLine(true);
        output().println();
        output().dividerLine(true);
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
            // subsequently generated random numbers differ depending on whether the solution
            // is visible.
            throw new IllegalStateException("cannot ask for randomness while inside solution section");
        }
        return rand;
    }

    public final <Choice> Choice chooseWithProb(double firstProbability, Choice choice0, Choice choice1) {
        return getRandom().nextFloat() < firstProbability ? choice0 : choice1;
    }

    @SafeVarargs
    public final <Choice> Choice choose(Choice... choices) {
        return choices[getRandom().nextInt(choices.length)];
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
