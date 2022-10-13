package edu.macalester.conceptual.puzzles.loops;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.PuzzleContext;

public class LoopTranslationPuzzle implements Puzzle {
    @Override
    public byte id() {
        return 0;
    }

    @Override
    public String name() {
        return "loop";
    }

    @Override
    public String description() {
        return "While loops and for loops";
    }

    public void generate(PuzzleContext ctx) {
        ctx.section(() -> {
            boolean direction = ctx.getRandom().nextBoolean();
            LoopForm sourceForm = direction ? LoopForm.FOR : LoopForm.WHILE;
            LoopForm targetForm = direction ? LoopForm.WHILE : LoopForm.FOR;

            generateTranslationPuzzle(ctx, sourceForm, targetForm);
        });

        ctx.section(() -> {
            generateTranslationPuzzle(ctx, LoopForm.NATURAL_LANGUAGE, LoopForm.FOR);
        });
    }

    private static void generateTranslationPuzzle(PuzzleContext ctx, LoopForm sourceForm, LoopForm targetForm) {
        ctx.output().paragraph(
            "Translate the following " + sourceForm.description()
            + " into a " + targetForm.description() + ":");
        var loop = SimpleLoop.generateNumericLoop(ctx);
        ctx.output().codeBlock(sourceForm.format(loop));
        ctx.solution(() ->
            ctx.output().codeBlock(targetForm.format(loop)));
    }
}
