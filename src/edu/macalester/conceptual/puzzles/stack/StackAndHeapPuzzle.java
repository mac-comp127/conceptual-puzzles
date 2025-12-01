package edu.macalester.conceptual.puzzles.stack;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.PuzzleContext;

public class StackAndHeapPuzzle implements Puzzle {
    @Override
    public byte id() {
        return 9;
    }

    @Override
    public String name() {
        return "stack";
    }

    @Override
    public String description() {
        return "Stack frames and objects (like the Idea Lab activity)";
    }

    @Override
    public byte minDifficulty() {
        return 0;
    }

    @Override
    public byte goalDifficulty() {
        return 2;
    }

    @Override
    public byte maxDifficulty() {
        return 10;
    }

    @Override
    public void generate(PuzzleContext ctx) {
        // It usually only takes 1 or 2 attempts to get an acceptable puzzle; we're just trying to
        // prevent an infinite loop here in case the balance criteria accidentally become impossible.
        for(int attemptsLeft = 100; attemptsLeft > 0; attemptsLeft--) {
            var gen = new StackPuzzleGenerator(ctx);
            if (gen.isWellBalanced()) {
                gen.outputPuzzle();
                return;
            }
        }
        throw new RuntimeException(
            "Unable to generate well-balanced stack puzzle, difficulty = " + ctx.getDifficulty());
    }
}
