package edu.macalester.conceptual.ast;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LiteralStringValueExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static com.github.javaparser.StaticJavaParser.parseExpression;
import static edu.macalester.conceptual.ast.AstUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AstUtilsTest {
    @Test
    void astBuilders() {
        var code = blockOf(
            variableDeclarationStmt(
                new VariableDeclarator(
                    classNamed("Foo"),
                    "bar",
                    joinedWithOperator(
                        BinaryExpr.Operator.PLUS,
                        intLiteral(2),
                        intLiteral(3),
                        intLiteral(5)))));
        assertEquals("{\n    Foo bar = 2 + 3 + 5;\n}", code.toString());
    }

    @Test
    void treeSearch() {
        var code = StaticJavaParser.parseTypeDeclaration(
            """
            class Frotzle {
                private int z = 0;
                void fleep(int x) {
                    int y = x + 1;
                }
                boolean bloob() {
                    return z > -10 && z < 10;
                }
            }
            """);
        assertDescendantsOfTypeEqual(  // deeply nested type
            code, VariableDeclarator.class,
            "z = 0", "y = x + 1");
        assertDescendantsOfTypeEqual(  // abstract supertype
            code, LiteralStringValueExpr.class,
            "0", "1", "10", "10");
        assertDescendantsOfTypeEqual(  // recursively self-containing type
            code, BinaryExpr.class,
            "x + 1", "z > -10 && z < 10", "z > -10", "z < 10");
    }

    private void assertDescendantsOfTypeEqual(Node code, Class<? extends Node> type, String... expected) {
        assertEquals(
            Arrays.asList(expected),
            allDescendantsOfType(type, code).stream()
                .map(Object::toString)
                .toList(),
            "allDescendantsOfType mismatch for " + type);
    }

    @Test
    void negation() {
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
        assertEquals(parsed0, negated(parsed1));
        assertEquals(parsed1, negated(parsed0));
    }

    @Test
    void withParensAsNeeded_precedence() {
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
            "a + (b + (c + d))",
            "a + ❰b + ❰c + d❱❱");  // + is not associative (e.g. `"foo" + (1 + 2) + "bar"`)
        assertParensAdded(
            "a + b + c + d",
            "❰❰a + b❱ + c❱ + d");  // left-branching expr doesn't need parens for left-associative +
        assertParensAdded(
            "a + (b + c) + d",
            "❰a + ❰b + c❱❱ + d");  // mix of parens necessary and parens unnecessary
        assertParensAdded(
            "a + (b + c) + d",
            "a + ❰❰b + c❱ + d❱");
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