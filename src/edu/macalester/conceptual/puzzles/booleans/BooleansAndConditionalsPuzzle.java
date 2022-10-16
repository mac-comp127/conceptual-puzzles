package edu.macalester.conceptual.puzzles.booleans;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.PuzzleContext;

public class BooleansAndConditionalsPuzzle implements Puzzle {
    @Override
    public byte id() {
        return 1;
    }

    @Override
    public String name() {
        return "bool";
    }

    @Override
    public String description() {
        return "Booleans and conditionals";
    }

    @Override
    public int examDifficulty() {
        return 1;
    }

    @Override
    public int maxDifficulty() {
        return 50;
    }

    public void generate(PuzzleContext ctx) {
        ctx.section(() -> SwapConditionalClausesPuzzle.generate(ctx));

        ctx.section(() -> ReturnBooleanPuzzle.generate(ctx));
    }
}
