package edu.macalester.conceptual;

import java.util.List;
import java.util.function.Function;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.puzzles.booleans.BooleansAndConditionalsPuzzle;
import edu.macalester.conceptual.puzzles.loops.LoopTranslationPuzzle;

public interface Puzzle {

    byte id();

    String name();

    String description();

    void generate(PuzzleContext ctx);

    public static final List<Puzzle> ALL = List.of(
        new BooleansAndConditionalsPuzzle(),
        new LoopTranslationPuzzle()
    );

    public static <T> Puzzle find(T target, Function<Puzzle,T> property, String propertyName) {
        List<Puzzle> results = Puzzle.ALL.stream()
            .filter(puzzle -> property.apply(puzzle).equals(target))
            .toList();

        if (results.isEmpty()) {
            return null;
        }

        if (results.size() > 1) {
            throw new AssertionError(
                "Multiple puzzles have " + propertyName + "=" + target
                + ", which should be unique: " + results);
        }

        return results.get(0);
    }
}
