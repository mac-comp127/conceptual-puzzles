package edu.macalester.conceptual.puzzles.classes;

import com.github.javaparser.ast.expr.Expression;

import edu.macalester.conceptual.puzzles.classes.type.PropertyType;

/**
 * Expression + English + type of the expression.
 * (Still wish Java had tuple types!)
 * NB: For internal use by the class description puzzle.
 */
public record TypedExprWithDescription(PropertyType type, Expression code, String description) {
}
