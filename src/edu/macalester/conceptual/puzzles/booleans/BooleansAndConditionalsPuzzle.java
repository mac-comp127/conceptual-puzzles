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
    public byte goalDifficulty() {
        return 1;
    }

    @Override
    public byte maxDifficulty() {
        return 50;
    }

    public void generate(PuzzleContext ctx) {
        ctx.section(() -> SwapConditionalClausesPuzzle.generate(ctx));

        ctx.section(() -> ReturnBooleanPuzzle.generate(ctx));

        ctx.section(() -> SimplifyChainedConditionalsPuzzle.generate(ctx));
    }
}
