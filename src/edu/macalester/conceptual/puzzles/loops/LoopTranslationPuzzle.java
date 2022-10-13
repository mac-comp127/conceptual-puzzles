package edu.macalester.conceptual.puzzles.loops;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.CodeFormatting;

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

            generateTranslationPuzzle(ctx, sourceForm, targetForm, true);
        });

        ctx.section(() -> {
            generateTranslationPuzzle(ctx, LoopForm.NATURAL_LANGUAGE, LoopForm.FOR, false);
        });
    }

    private static void generateTranslationPuzzle(
        PuzzleContext ctx,
        LoopForm sourceForm,
        LoopForm targetForm,
        boolean includeBody
    ) {
        ctx.output().paragraph(
            "Translate the following " + sourceForm.description()
            + " into a " + targetForm.description() + ":");
        var loop = SimpleLoop.generateNumericLoop(ctx);
        if (!includeBody) {
            loop.setBody(CodeFormatting.ELIDED);
        }
        ctx.output().codeBlock(sourceForm.format(loop));
        ctx.solution(() ->
            ctx.output().codeBlock(targetForm.format(loop)));
    }
}
