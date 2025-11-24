package edu.macalester.conceptual.puzzles.classes;

import com.github.javaparser.ast.expr.Expression;

import static com.github.javaparser.StaticJavaParser.parseExpression;

/**
 * A bit of code paired with a bit of English.
 * Wish Java had tuple types!
 * NB: For internal use by the class description puzzle.
 */
public record ExprWithDescription(Expression code, String description) {
    public ExprWithDescription(String code, String description) {
        this(parseExpression(code), description);
    }
}

