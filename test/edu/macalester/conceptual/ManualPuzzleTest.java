package edu.macalester.conceptual;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.puzzles.ast.AstDrawingPuzzle;

public class ManualPuzzleTest {
    public static void main(String[] args) throws Exception {
        var puzzle = new AstDrawingPuzzle();
        //               ^^^^^^^^^^^^^^^^^^ change this to test a different puzzle

        // To generate random tests:
        PuzzleContext ctx = PuzzleContext.generate(puzzle.id(), puzzle.goalDifficulty());

        // To replay a specific test:
        // PuzzleContext ctx = PuzzleContext.fromPuzzleCode("1d9L-m1xq-a4t0-ypys-7");

        ctx.enableSolution();
        ctx.emitPuzzle(() -> puzzle.generate(ctx));
        System.out.println("Puzzle code: " + ctx.getPuzzleCode());
    }
}
