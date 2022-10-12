package edu.macalester.conceptual;

import java.util.Arrays;

import edu.macalester.conceptual.context.InvalidPuzzleCodeException;
import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.puzzles.loops.LoopTranslationPuzzle;

public class CommandLine {
    public static void main(String[] args) throws InvalidPuzzleCodeException {
        System.out.println("args: " + Arrays.toString(args));

        PuzzleContext original = PuzzleContext.generate();
        System.out.println(original.getPuzzleCode());
        original.emitPuzzle(() -> {
            new LoopTranslationPuzzle().generate(original);
        });

        System.out.println();
        System.out.println("Solution should appear here");
        System.out.println();

        PuzzleContext duplicate = PuzzleContext.fromPuzzleCode(original.getPuzzleCode());
        duplicate.enableSolution();
        duplicate.emitPuzzle(() -> {
            new LoopTranslationPuzzle().generate(duplicate);
        });
    }
}
