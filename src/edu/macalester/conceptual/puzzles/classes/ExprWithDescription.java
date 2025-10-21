package edu.macalester.conceptual.puzzles.classes;

import com.github.javaparser.ast.expr.Expression;

import static com.github.javaparser.StaticJavaParser.parseExpression;

record ExprWithDescription(Expression code, String description) {
    ExprWithDescription(String code, String description) {
        this(parseExpression(code), description);
    }
}

record TypedExprWithDescription(PropertyType type, Expression code, String description) {
}
