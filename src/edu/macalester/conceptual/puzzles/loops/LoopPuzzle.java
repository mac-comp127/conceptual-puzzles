package edu.macalester.conceptual.puzzles.loops;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.PuzzleContext;

public class LoopPuzzle implements Puzzle {

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

    @Override
    public byte minDifficulty() {
        return 1;
    }

    @Override
    public byte goalDifficulty() {
        return 3;
    }

    @Override
    public byte maxDifficulty() {
        return 4;
    }

    public void generate(PuzzleContext ctx) {
        List<Runnable> sections = new ArrayList<>(List.of(
            () -> LoopTranslationPuzzle.generateForAndWhile(ctx),
            () -> LoopTranslationPuzzle.generateNaturalLanguage(ctx),
            () -> LoopTranslationPuzzle.generateForEach(ctx),
            () -> LoopTracingPuzzle.generateRandomType(ctx)
        ));

        Collections.shuffle(sections, ctx.getRandom());

        int sectionCount = ctx.getDifficulty();
        while (sections.size() > sectionCount) {
            sections.remove(0);
        }

        for (var section : sections) {
            ctx.section(section);
        }
    }

}
