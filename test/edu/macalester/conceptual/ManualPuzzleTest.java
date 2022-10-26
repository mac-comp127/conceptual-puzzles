package edu.macalester.conceptual;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.puzzles.ast.AstDrawingPuzzle;

public class ManualPuzzleTest {
    public static void main(String[] args) {
        var puzzle = new AstDrawingPuzzle();
        PuzzleContext ctx = PuzzleContext.generate(puzzle.id(), puzzle.goalDifficulty());
        ctx.enableSolution();
        ctx.emitPuzzle(() -> puzzle.generate(ctx));
    }
}
