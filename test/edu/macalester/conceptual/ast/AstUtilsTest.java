package edu.macalester.conceptual.ast;

import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.github.javaparser.StaticJavaParser.parseExpression;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AstUtilsTest {

    @Test
    void negated() {
        assertNegation("true", "false");
        assertNegation("x", "!x");
        assertNegation("x == y", "x != y");
        assertNegation("x < y", "x >= y");
        assertNegation("x > y", "x <= y");
        assertNegation("x && y", "!x || !y");
        assertNegation("x || y", "!x && !y");
        assertNegation(
            "a < b || c && d != e",
            "a >= b && ❰!c || d == e❱");
    }

    private void assertNegation(String boolExpr0, String boolExpr1) {
        var parsed0 = parseWithForcedNesting(boolExpr0);
        var parsed1 = parseWithForcedNesting(boolExpr1);
        assertEquals(parsed0, AstUtils.negated(parsed1));
        assertEquals(parsed1, AstUtils.negated(parsed0));
    }

    @Test
    void withParensAsNeeded() {
        assertNoParensAdded("1");
        assertNoParensAdded("-1");
        assertNoParensAdded("x");
        assertNoParensAdded("-x");
        assertNoParensAdded("(x)");
        assertNoParensAdded("foo(-1)");
        assertNoParensAdded("foo(-1) + bar(x / baz(z))");
        assertNoParensAdded("x + ❰y * z❱");
        assertParensAdded("(x + y) * z", "❰x + y❱ * z");
        assertParensAdded("((x) + y) * (z)", "❰(x) + y❱ * (z)");
        assertNoParensAdded("foo[a].bar.baz(b.c[d])[e][f]");
        assertNoParensAdded(
            "❰a >= b && !c❱ || d == e");
        assertParensAdded(
            "a >= b && (!c || d == e)",
            "a >= b && ❰!c || d == e❱");
    }

    @Test
    @Disabled  // TODO: fix associativity if we ever need it
    void withParensAsNeeded_associativity() {
        assertNoParensAdded("a && b && c && d");
        assertParensAdded(
            "a && b && c && d",
            "❰a && ❰b && c❱❱ && d");  // && is truly associative, even w/short-circuiting
        assertParensAdded(
            "a + b + c + d",
            "❰❰a + b❱ + c❱ + d");  // left-branching expr doesn't need pars for left-associative +
        assertParensAdded(
            "a + (b + (c + d))",
            "a + ❰b + ❰c + d❱❱");  // + is not associative (e.g. `"foo" + (1 + 2) + "bar"`)
        assertParensAdded(
            "a + (b + c) + d",
            "❰a + ❰b + c❱❱ + d");
    }

    private void assertNoParensAdded(String expr) {
        assertParensAdded(expr.replaceAll("[❰❱]", ""), expr);
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

        var expr = parseExpression(expression);
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