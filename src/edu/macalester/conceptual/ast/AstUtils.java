package edu.macalester.conceptual.ast;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.javaparser.ast.expr.BinaryExpr.Operator.*;
import static com.github.javaparser.ast.expr.UnaryExpr.Operator.*;

public enum AstUtils {
    ; // no cases; static methods only

    public static IntegerLiteralExpr intLiteral(int i) {
        return new IntegerLiteralExpr(String.valueOf(i));
    }

    public static ExpressionStmt variableDeclarationStmt(VariableDeclarator variable) {
        return new ExpressionStmt(
            new VariableDeclarationExpr(variable));
    }

    @SafeVarargs
    public static <NodeType extends Node> NodeList<NodeType> nodes(NodeType... nodes) {
        return new NodeList<>(nodes);
    }

    public static BlockStmt blockOf(Statement... statements) {
        return new BlockStmt(nodes(statements));
    }

    public static String nodesToString(Node... nodes) {
        return Arrays.stream(nodes)
            .map(Node::toString)
            .collect(Collectors.joining("\n"));
    }

    public static Expression joinedWithOperator(BinaryExpr.Operator operator, Stream<Expression> exprs) {
        return exprs
            .reduce((lhs, rhs) -> new BinaryExpr(lhs, rhs, AND))
            .orElseThrow();
    }

    /**
     * Add parentheses as necessary within the given AST to ensure that the emitted code respects
     * the actual structure of the tree.
     *
     * (Somewhat surprising JavaParser doesn't already provide this!)
     *
     * WARNING: Does not properly account for associativity. Add explicit parens to distinguish
     *          e.g. (a + b) + c from a + (b + c).
     */
    public static <NodeType extends Node> NodeType withParensAsNeeded(NodeType expr) {
        @SuppressWarnings("unchecked")
        NodeType copy = (NodeType) expr.clone();
        insertParensAsNeeded(copy);
        return copy;
    }

    private static void insertParensAsNeeded(Node node) {
        new ArrayList<>(node.getChildNodes())
            .forEach(AstUtils::insertParensAsNeeded);

        if (node instanceof Expression expr
            && node.getParentNode().orElse(null) instanceof Expression parentExpr
            && getPrecedence(parentExpr, false) > getPrecedence(expr, true)
        ) {
            parentExpr.replace(expr, new EnclosedExpr(expr));
        }
    }

    private static int getPrecedence(Expression expr, boolean asChild) {
        // Precedences taken from https://introcs.cs.princeton.edu/java/11precedence/
        if (expr instanceof UnaryExpr unary) {
            return switch(unary.getOperator()) {
                case POSTFIX_INCREMENT, POSTFIX_DECREMENT -> 15;
                default -> 14;
            };
        } else if (expr instanceof CastExpr || expr instanceof ObjectCreationExpr) {
            return 13;
        } else if (expr instanceof BinaryExpr binary) {
            return switch(binary.getOperator()) {
                case MULTIPLY, DIVIDE, REMAINDER -> 12;
                case PLUS, MINUS -> 11;
                case LEFT_SHIFT, SIGNED_RIGHT_SHIFT, UNSIGNED_RIGHT_SHIFT -> 10;
                case LESS, GREATER, LESS_EQUALS, GREATER_EQUALS -> 9;
                case EQUALS, NOT_EQUALS -> 8;
                case BINARY_AND -> 7;
                case XOR -> 6;
                case BINARY_OR -> 5;
                case AND -> 4;
                case OR -> 3;
            };
        } else if (expr instanceof InstanceOfExpr) {
            return 9;
        } else if (expr instanceof AssignExpr) {
            return 1;
        } else if (expr instanceof ConditionalExpr) {
            return 2;
        } else {  // explicit parens, method calls, etc. should never force parens above or below
            return asChild ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        }
    }

    public static Expression negated(Expression node) {
        if (node instanceof UnaryExpr expr) {
            if (expr.getOperator() == LOGICAL_COMPLEMENT) {
                return expr.getExpression();
            } else {
                return expr;
            }
        } else if (node instanceof BinaryExpr expr) {
            return switch(expr.getOperator()) {
                case AND -> deMorgan(expr, OR);
                case OR -> deMorgan(expr, AND);

                case EQUALS -> replaceOperator(expr, NOT_EQUALS);
                case NOT_EQUALS -> replaceOperator(expr, EQUALS);
                case GREATER -> replaceOperator(expr, LESS_EQUALS);
                case GREATER_EQUALS -> replaceOperator(expr, LESS);
                case LESS -> replaceOperator(expr, GREATER_EQUALS);
                case LESS_EQUALS -> replaceOperator(expr, GREATER);

                default -> new UnaryExpr(new EnclosedExpr(expr), LOGICAL_COMPLEMENT);
            };
        } else {
            return new UnaryExpr(node, LOGICAL_COMPLEMENT);
        }
    }

    private static Expression deMorgan(BinaryExpr expr, BinaryExpr.Operator operator) {
        return new BinaryExpr(negated(expr.getLeft()), negated(expr.getRight()), operator);
    }

    private static Expression replaceOperator(BinaryExpr expr, BinaryExpr.Operator operator) {
        return new BinaryExpr(expr.getLeft(), expr.getRight(), operator);
    }
}
