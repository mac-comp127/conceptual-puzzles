package edu.macalester.conceptual.puzzles.stack;

import java.util.List;
import java.util.stream.IntStream;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.Nonsense;
import edu.macalester.conceptual.util.Randomness;

/**
 * Context and state recursively passed done through the entire method call tree.
 */
record GeneratorContext(
    PuzzleContext puzzleContext,
    ComplexityTracker complexity,
    List<StackPuzzleClass> puzzleClasses
) {
    public static GeneratorContext generate(PuzzleContext ctx) {
        var complexity = new ComplexityTracker(ctx.getDifficulty());

        // Generate some classes
        var puzzleClasses = IntStream.range(0, Math.max(2, ctx.getDifficulty() / 3))
            .mapToObj(n -> StackPuzzleClass.generate(ctx))
            .toList();

        // Give them a few properties
        int propertyCount = ctx.getDifficulty() * puzzleClasses.size() / 2;
        for(int n = 0; n < propertyCount; n++) {
            var puzzleClass = Randomness.choose(ctx, puzzleClasses);
            var propType = Randomness.choose(ctx, puzzleClasses);
            puzzleClass.addProperty(Nonsense.shortPropertyName(ctx), propType);
        }

        return new GeneratorContext(ctx, complexity, puzzleClasses);
    }
}
