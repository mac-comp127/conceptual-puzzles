package edu.macalester.conceptual.puzzles.classes.type;

import java.util.List;

import com.github.javaparser.ast.type.Type;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.puzzles.classes.ExprWithDescription;
import edu.macalester.conceptual.puzzles.classes.TypedExprWithDescription;

/**
 * The types that properties in our model can have. These types have both Java and English
 * representations, and can generate small snippets of code (with English descriptions) related to
 * the given type.
 * <p>
 * The goal here is to produce enough variation to make the nonsense models feel like they have a
 * real structure without turning this puzzle into a test of data structure or Java API knowledge.
 * The types here are all familiar, and the mutations and derived values all use trivial operations
 * that (hopefully!) feel familiar to students.
 * <p>
 * All these types can randomly generate:
 * <ul>
 *  <li>a value that can be assigned to a variable of this type,</li>
 *  <li>an expression that computes a new value from a variable of this type, and</li>
 *  <li>an expression that mutates a variable of this type.</li>
 * </ul>
 */
public interface PropertyType {
    PropertyType
        STRING = new StringPropertyType(),
        INT = new IntPropertyType(),
        LIST_OF_STRINGS = new ListOfStringsPropertyType(),
        GRAPHICS = new GraphicsPropertyType();

    List<PropertyType> ALL = List.of(STRING, INT, LIST_OF_STRINGS, GRAPHICS);

    /**
     * English description of this type.
     */
    String description();

    /**
     * This type as Java.
     */
    Type astType();

    default String javaName() {
        return astType().asString();
    }

    /**
     * Returns a random expression that will produce a value of this type.
     */
    ExprWithDescription generateValue(PuzzleContext ctx, boolean mutable);

    /**
     * Returns random code that will mutate a variable of this type with the given name.
     */
    ExprWithDescription generateMutation(PuzzleContext ctx, String variableName);

    /**
     * Returns random code that produces a new value given a variable of this type. Note that the
     * derived value may product a different type, e.g. aString.length() produces an int.
     */
    TypedExprWithDescription generateDerivedValue(PuzzleContext ctx, String variableName);
}
