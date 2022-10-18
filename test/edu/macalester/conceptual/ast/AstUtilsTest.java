package edu.macalester.conceptual.ast;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AstUtilsTest {

    @Test
    void withParensAsNeeded() {
        assertNoParensAdded("1");
        assertNoParensAdded("-1");
        assertNoParensAdded("x");
        assertNoParensAdded("-x");
        assertNoParensAdded("(x)");
        assertNoParensAdded("foo(-1)");
        assertNoParensAdded("foo(-1) + bar(x / baz(z))");
        assertParensAdded("x + y * z", "x + ❰y * z❱");
        assertParensAdded("(x + y) * z", "❰x + y❱ * z");
        assertParensAdded("((x) + y) * (z)", "❰(x) + y❱ * (z)");
    }

    private void assertNoParensAdded(String expr) {
        assertParensAdded(expr, expr);
    }

    private void assertParensAdded(String expected, String rawExpr) {
        assertEquals(
            expected,
            AstUtils
                .withParensAsNeeded(
                    parseWithForcedNesting(rawExpr))
                .toString());
    }

    /**
     * Parses code with ❰ ❱ as parentheses, but then removes the forced parens from the AST.
     */
    private static Expression parseWithForcedNesting(String expression) {
        final String removalMarker = "•";

        expression = expression
            .replace("❰", "(")
            .replace("❱", "/*" + removalMarker + "*/)");

        var expr = StaticJavaParser.parseExpression(expression);
        expr.accept(new ModifierVisitor<>() {
            @Override
            public Visitable visit(EnclosedExpr parens, Object arg) {
                var result = super.visit(parens, arg);
                if (parens
                    .getOrphanComments().stream()
                    .anyMatch(c -> c.getContent().equals(removalMarker))
                ) {
                    return parens.getInner();
                } else {
                    return result;
                }
            }
        }, null);
        return expr;
    }
}