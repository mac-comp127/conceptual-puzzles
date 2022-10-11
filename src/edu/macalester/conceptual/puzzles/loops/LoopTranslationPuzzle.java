package edu.macalester.conceptual.puzzles.loops;

import edu.macalester.conceptual.random.InvalidPuzzleCodeException;
import edu.macalester.conceptual.random.PuzzleContext;

public class LoopTranslationPuzzle {
    public void generate(PuzzleContext ctx) {
        ctx.section(() -> {
            ctx.output().paragraph("Translate the following for loop into a while loop:");
            var loop = SimpleLoop.generateNumericLoop(ctx);
            ctx.output().codeBlock(LoopForm.FOR.format(loop));
            ctx.solution(() ->
                ctx.output().codeBlock(LoopForm.WHILE.format(loop)));
        });

        ctx.section(() -> {
            ctx.output().paragraph("Translate the following while loop into a for loop:");
            var loop = SimpleLoop.generateNumericLoop(ctx);
            ctx.output().codeBlock(LoopForm.WHILE.format(loop));
            ctx.solution(() ->
                ctx.output().codeBlock(LoopForm.FOR.format(loop)));
        });

        ctx.section(() -> {
            ctx.output().paragraph("Create a for loop with the following description:");
            var loop = SimpleLoop.generateNumericLoop(ctx);
            ctx.output().indented(() ->
                ctx.output().paragraph(LoopForm.ENGLISH.format(loop)));
            ctx.solution(() ->
                ctx.output().codeBlock(LoopForm.FOR.format(loop)));
        });
    }

    public static void main(String[] args) throws InvalidPuzzleCodeException {
        PuzzleContext original = PuzzleContext.generate();
        PuzzleContext duplicate = PuzzleContext.fromPuzzleCode(original.getPuzzleCode());

        System.out.println(original.getPuzzleCode());
        original.emitPuzzle(() -> {
            new LoopTranslationPuzzle().generate(original);
        });
        System.out.println();
        System.out.println();
        System.out.println();
        duplicate.enableSolution();
        duplicate.emitPuzzle(() -> {
            new LoopTranslationPuzzle().generate(duplicate);
        });
    }
}
