package edu.macalester.conceptual.puzzles.booleans;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.ReturnStmt;

import edu.macalester.conceptual.context.PuzzleContext;

import static com.github.javaparser.ast.expr.BinaryExpr.Operator.*;
import static edu.macalester.conceptual.ast.AstUtils.*;
import static edu.macalester.conceptual.util.CodeFormatting.*;

public class ReturnBooleanPuzzle {
    static void generate(PuzzleContext ctx) {
        ctx.output().paragraph(
            "Simplify the following conditional chain so that it is a single return statement.");

        var boolExpr = Generator.generateBooleanExpr(ctx, 2 + ctx.getDifficulty(), false);
        ctx.output().codeBlock(
            toConditionalChainReturningBool(boolExpr, false));

        ctx.solution(() -> {
            ctx.output().codeBlock(new ReturnStmt(boolExpr));
        });

        ctx.output().dividerLine(false);
        ctx.output().paragraph(
            """
            Bonus challenge: rewrite the if/else chain above so that instead of consisting of many
            `return true;` statements with one `return false;` at the end, it has many
            `return false;` statements with one `return true;` at the end.
            """);

        ctx.solution(() -> {
            ctx.output().codeBlock(
                toConditionalChainReturningBool(boolExpr, true));
        });
    }

    private static String toConditionalChainReturningBool(Expression boolExpr, boolean negated) {
        if (negated) {
            boolExpr = negated(boolExpr);
        }
        return prettifyStatements(
            joinCode(
                toConditionalChain(
                    boolExpr,
                    "return " + !negated + ";"),
                "return " + negated + ";"));
    }

    private static String toConditionalChain(Expression boolExpr, String leafStmt) {
        return toConditionalChainRaw(removeLeftBranchingAnds(boolExpr), leafStmt);
    }

    private static String toConditionalChainRaw(Expression boolExpr, String leafStmt) {
        if (boolExpr instanceof BinaryExpr binary) {
            switch(binary.getOperator()) {
                case AND -> {
                    return "if(" + binary.getLeft() + "){"
                        + toConditionalChainRaw(binary.getRight(), leafStmt)
                        + "}";
                }
                case OR -> {
                    return toConditionalChainRaw(binary.getLeft(), leafStmt)
                        + toConditionalChainRaw(binary.getRight(), leafStmt);
                }
            }
        }
        return "if(" + boolExpr + "){" + leafStmt + "}";
    }

    private static Expression removeLeftBranchingAnds(Expression boolExpr) {
        if (!(boolExpr instanceof BinaryExpr binary)) {
            return boolExpr;
        }
        var left  = removeLeftBranchingAnds(binary.getLeft());
        var right = removeLeftBranchingAnds(binary.getRight());

        if (binary.getOperator() == AND && left instanceof BinaryExpr binaryLeft) {
            switch(binaryLeft.getOperator()) {
                case AND -> {
                    // (a && b) && c  →  a && (b && c)
                    return new BinaryExpr(
                        binaryLeft.getLeft(),
                        removeLeftBranchingAnds(
                            new BinaryExpr(
                                binaryLeft.getRight(),
                                right,
                                AND)),
                        AND);
                }
                case OR -> {
                    // (a || b) && c  →  (a && c) || (b && c)
                    return new BinaryExpr(
                        removeLeftBranchingAnds(
                            new BinaryExpr(
                                binaryLeft.getLeft(),
                                right,
                                AND)),
                        removeLeftBranchingAnds(
                            new BinaryExpr(
                                binaryLeft.getRight(),
                                right.clone(),
                                AND)),
                        OR);
                }
            }
        }
        return new BinaryExpr(left, right, binary.getOperator());
    }
}
