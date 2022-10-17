package edu.macalester.conceptual.puzzles.booleans;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.Nonsense;

import static com.github.javaparser.ast.expr.BinaryExpr.Operator.*;
import static com.github.javaparser.ast.expr.UnaryExpr.Operator.*;
import static edu.macalester.conceptual.ast.AstUtils.*;
import static edu.macalester.conceptual.util.CodeFormatting.*;

public class SimplifyChainedConditionalsPuzzle {
    public static void generate(PuzzleContext ctx) {
        List<ConditionAndBody> steps = new ArrayList<>();
        List<Expression> previousNegations = new ArrayList<>();
        List<IfStmt> messyChain = new ArrayList<>();
        IfStmt prevConditional = null;

        for (int n = 1 + ctx.getDifficulty(); n >= 0; n--) {
            var newStep = new ConditionAndBody(
                Generator.generateBooleanLeaf(ctx, false),
                new ExpressionStmt(
                    Nonsense.methodCallExpr(ctx)));
            steps.add(newStep);

            var obfuscatedNewCondition = obfuscated(ctx, newStep.condition);
            var curConditional =
                new IfStmt(
                    joinedWithOperator(
                        AND,
                        obfuscatedNewCondition,
                        previousNegations.stream()),
                    blockOf(newStep.body),
                    null);


            if (prevConditional == null || ctx.getRandom().nextFloat() < 0.5) {
                // Start a new conditional, no else clause
                // (Makes no difference because negated previous conditions have same effect as else)
                messyChain.add(curConditional);
            } else {
                prevConditional.setElseStmt(curConditional);
            }
            prevConditional = curConditional;

            previousNegations.add(
                negated(obfuscatedNewCondition));
        }

        ctx.output().paragraph("Simplify the following messy chain of conditionals:");

        ctx.output().codeBlock(prettifyStatements(
            messyChain.stream()
                .map(Statement::toString)
                .collect(Collectors.joining())));

        ctx.solution(() -> {
            IfStmt answer = null, lastStep = null;
            for (var step : steps) {
                var newConditional = new IfStmt(step.condition, blockOf(step.body), null);
                if (answer == null) {
                    answer = newConditional;
                } else {
                    lastStep.setElseStmt(newConditional);
                }
                lastStep = newConditional;
            }
            ctx.output().codeBlock(answer);
        });
    }

    private static Expression obfuscated(PuzzleContext ctx, Expression boolExpr) {
        boolean isBinary = boolExpr instanceof BinaryExpr;

        if (ctx.getRandom().nextFloat() < (isBinary ? 0.8 : 0.2)) {
            return boolExpr;
        }

        if (boolExpr instanceof UnaryExpr unary && unary.getOperator() == LOGICAL_COMPLEMENT) {
            // !x  →  x == false
            return new BinaryExpr(unary.getExpression(), new BooleanLiteralExpr(false), EQUALS);
        } else {
            if (isBinary && ctx.getRandom().nextFloat() < 0.8) {
                boolExpr = new EnclosedExpr(boolExpr);
            }
            // x  →  x == true
            return new BinaryExpr(boolExpr, new BooleanLiteralExpr(true), EQUALS);
        }
    }

    private record ConditionAndBody(
        Expression condition,
        Statement body
    ) { }

    private record IfElseChain(
    ) {}
}
