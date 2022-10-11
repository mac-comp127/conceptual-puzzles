package edu.macalester.conceptual.ast;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.util.Arrays;
import java.util.stream.Collectors;

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
}
