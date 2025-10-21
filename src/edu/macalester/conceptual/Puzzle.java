package edu.macalester.conceptual;

import java.util.List;
import java.util.function.Function;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.puzzles.ast.AstDrawingPuzzle;
import edu.macalester.conceptual.puzzles.booleans.BooleansAndConditionalsPuzzle;
import edu.macalester.conceptual.puzzles.classes.ClassDeclarationsPuzzle;
import edu.macalester.conceptual.puzzles.closures.ClosuresPuzzle;
import edu.macalester.conceptual.puzzles.loops.LoopPuzzle;
import edu.macalester.conceptual.puzzles.relationships.RelationshipsPuzzle;
import edu.macalester.conceptual.puzzles.types.StaticAndRuntimeTypesPuzzle;
import edu.macalester.conceptual.puzzles.vars.VariablesPuzzle;

/**
 * A single conceptual puzzle type. Note that one instance of this class is a puzzle <i>type</i>,
 * not a <i>single puzzle</i>, i.e. “AST Drawing” is one <code>Puzzle</code> object, which can
 * generate many distinct AST Drawing puzzles given different <code>PuzzleContext</code> objects
 * containing different random seeds.
 * <p>
 * The <code>ALL</code> constant in this class serves as the central repository of available puzzle
 * types. To make a new puzzle type available, add it to that list.
 */
public interface Puzzle {
    /**
     * All available puzzle types. Anything listed here will show up as an option in the CLI.
     * We create new puzzles instances from scratch every time this method is caleld to prevent
     * state pollution when generating multiple puzzles in succession.
     */
    static List<Puzzle> all() {
        return List.of(
            new AstDrawingPuzzle(),
            new BooleansAndConditionalsPuzzle(),
            new LoopPuzzle(),
            new VariablesPuzzle(),
            new ClassDeclarationsPuzzle(),
            new RelationshipsPuzzle(),
            new StaticAndRuntimeTypesPuzzle(),
            new ClosuresPuzzle()
        );
    }

    /**
     * A unique positive ID for this type of puzzle. Used in generating and decoding puzzle codes.
     */
    byte id();

    /**
     * The user-visible short name for this puzzle, e.g. “ast” or “bool”.
     */
    String name();

    /**
     * A user-visible one-phrase description of this puzzle. Used by the CLI’s <code>list</code>
     * command.
     */
    String description();

    /**
     * Determines whether the puzzle type shows up in the list of available puzzles. It is still
     * possible to generate hidden puzzles and view their solutions.
     */
    default boolean isVisible() {
        return true;
    }

    /**
     * The difficultly level necessary to receive credit for this puzzle. This is the default
     * difficulty if the user does not specify the <code>--difficulty</code> option.
     * <p>
     * Default implementation: returns <code>minDifficulty()</code>.
     */
    default byte goalDifficulty() {
        return minDifficulty();
    }

    /**
     * The minimum difficulty level for puzzles of this type.
     * <p>
     * Default implementation: 0
     */
    default byte minDifficulty() {
        return 0;
    }

    /**
     * The maximum difficulty level for puzzles of this type.
     * <p>
     * Default implementation: 0
     */
    default byte maxDifficulty() {
        return 0;
    }

    /**
     * Generates a puzzle for the given context, which contains the already-seeded random number
     * generator, the difficulty level, and the output pipe.
     */
    void generate(PuzzleContext ctx);

    /**
     * Finds the unique <code>Puzzle</code> whose <code>id()</code> matches <code>id</code>, or
     * null if no such puzzle exists. Raises an error if there are duplicate puzzle IDs.
     */
    static Puzzle findByID(byte id) {
        return find(id, Puzzle::id, "id");
    }

    /**
     * Finds the unique <code>Puzzle</code> whose <code>name()</code> matches <code>name</code>, or
     * null if no such puzzle exists. Raises an error if there are duplicate puzzle names.
     */
    static Puzzle findByName(String name) {
        return find(name, Puzzle::name, "name");
    }

    private static <T> Puzzle find(T target, Function<Puzzle,T> property, String propertyName) {
        List<Puzzle> results = Puzzle.all().stream()
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
