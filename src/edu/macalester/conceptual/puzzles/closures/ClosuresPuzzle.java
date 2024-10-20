package edu.macalester.conceptual.puzzles.closures;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.PuzzleContext;

public class ClosuresPuzzle implements Puzzle {
    @Override
    public byte id() {
        return 4;
    }

    @Override
    public String name() {
        return "closures";
    }

    @Override
    public String description() {
        return "Closure execution order";
    }

    @Override
    public byte goalDifficulty() {
        return 0;
    }

    @Override
    public byte maxDifficulty() {
        return 10;
    }

    @Override
    public void generate(PuzzleContext ctx) {
        new ClosureTracingPuzzle(ctx).generate();
    }
}
