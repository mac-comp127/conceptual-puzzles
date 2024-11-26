package edu.macalester.conceptual.puzzles.relationships;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;

import edu.macalester.conceptual.util.AstUtils;

public class TraversalChainBuilder {
    private ExpressionStmt curStmt;
    private final Statement resultNode;

    TraversalChainBuilder(Expression seed) {
        curStmt = new ExpressionStmt(seed);
        resultNode = AstUtils.blockOf(curStmt);
    }

    public void replaceExpression(Function<Expression, Expression> transform) {
        curStmt.setExpression(
            transform.apply(
                curStmt.getExpression()));
    }

    public void wrapCurrentStatement(Expression seed, BiFunction<Expression, Statement, Statement> transform) {
        var newStmt = new ExpressionStmt(seed);
        curStmt.replace(
            transform.apply(
                curStmt.getExpression(), newStmt));
        curStmt = newStmt;
    }

    public Node getResult() {
        return resultNode.getChildNodes().get(0);
    }
}
