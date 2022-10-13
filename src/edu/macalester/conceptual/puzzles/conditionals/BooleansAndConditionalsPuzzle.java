package edu.macalester.conceptual.puzzles.conditionals;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.Nonsense;

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

    public void generate(PuzzleContext ctx) {
        ctx.output().paragraph("pretend like this is a puzzle: " + Nonsense.typeName(ctx));
        String solution = Nonsense.typeName(ctx);
        ctx.solution(() ->
            ctx.output().paragraph("pretend like this is a solution: " + solution));
    }
}
