package edu.macalester.conceptual.puzzles.ast;

import com.github.javaparser.ast.expr.Expression;

public record ExprWithVars(
    Expression expr,
    String exprAsString,
    VariablePool vars
) { }
