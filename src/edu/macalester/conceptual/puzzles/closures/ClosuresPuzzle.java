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
        return "Closures and event handling";
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
        ctx.output().paragraph(
            """
            Suppose we are in an environment where `CLICK`, `TICK`, and `KEY` events can occur, and
            suppose we have the following functions available:
            """
        );

        ctx.output().paragraph("`twice(closure)`");
        ctx.output().blockquote("Immediately executes `closure` two times.");

        ctx.output().paragraph("`onClick(closure)`");
        ctx.output().blockquote("Executes `closure` after every subsequent `CLICK` event.");

        ctx.output().paragraph("`onKeyPress(closure)`");
        ctx.output().blockquote("Executes `closure` after every subsequent `KEY` event.");

        ctx.output().paragraph("`afterDelay(tickCount, closure)`");
        ctx.output().blockquote(
            "Executes `closure` once after exactly `tickCount` `TICK` events have occurred."
        );

        ctx.output().paragraph(
            """
            Multiple closures can be registered to handle the same event. If this happens, they
            execute in the order they were registered.
            """
        );
        ctx.output().dividerLine(false);

        ctx.section(() -> {
            new ClosureTracingPuzzle(ctx).generate();
        });
    }
}
