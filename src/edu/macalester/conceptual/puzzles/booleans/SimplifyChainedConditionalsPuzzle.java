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
import java.util.stream.Stream;

import edu.macalester.conceptual.ast.AstUtils;
import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.Nonsense;

import static com.github.javaparser.ast.expr.BinaryExpr.Operator.*;
import static com.github.javaparser.ast.expr.UnaryExpr.Operator.*;
import static edu.macalester.conceptual.ast.AstUtils.*;
import static edu.macalester.conceptual.util.CodeFormatting.*;

public class SimplifyChainedConditionalsPuzzle {
    public static void generate(PuzzleContext ctx) {
        List<ConditionAndBody> steps = new ArrayList<>();
        for (int n = 1 + ctx.getDifficulty(); n >= 0; n--) {
            steps.add(
                new ConditionAndBody(
                    (n == 0 && ctx.getRandom().nextFloat() < 0.5)
                        ? null  // last one can be just `else`, no additional `if`
                        : Generator.generateBooleanLeaf(ctx, false),
                    new ExpressionStmt(
                        Nonsense.methodCallExpr(ctx))));
        }

        List<IfStmt> messyChain = createMessyChain(ctx, steps);
        IfStmt tidyChain = createTidyChain(steps);

        ctx.output().paragraph("Simplify the following messy chain of conditionals:");

        ctx.output().codeBlock(prettifyStatements(
            messyChain.stream()
                .map(Statement::toString)
                .collect(Collectors.joining())));

        ctx.solution(() -> {
            ctx.output().codeBlock(tidyChain);

            ctx.solutionChecklist(
                "Did you remove / simplify all the `== true` and `== false` checks?",
                steps.get(steps.size() - 1).condition == null
                    ? "This particular conditional chain can end with just an `else`, no final `if`."
                    : "This particular conditional chain must end with an `else if`, not just `else`.");
        });
    }

    private static List<IfStmt> createMessyChain(PuzzleContext ctx, List<ConditionAndBody> steps) {
        List<Expression> previousNegations = new ArrayList<>();
        List<IfStmt> messyChain = new ArrayList<>();
        IfStmt prevConditional = null;
        for (var step : steps) {
            List<Expression> additionalMessyConditions;
            if (step.condition == null) {
                // Nothing new to add; just use all the previous negations!
                // (This is an `else` with no additional `if` in the simplified version.)
                additionalMessyConditions = List.of();
            } else {
                additionalMessyConditions = List.of(
                    obfuscated(ctx, step.condition));
            }

            var curConditional =
                new IfStmt(
                    joinedWithOperator(
                        AND,
                        Stream.concat(
                            additionalMessyConditions.stream(),
                            previousNegations.stream())),
                    blockOf(step.body),
                    null);  // `else` will be attached in subsequent iterations if needed

            if (prevConditional == null || ctx.getRandom().nextFloat() < 0.5) {
                // Sometimes just start a new conditional chain, no else clause
                // (Makes no difference because negated previous conditions have same effect as else)
                messyChain.add(curConditional);
            } else {
                prevConditional.setElseStmt(curConditional);
            }
            prevConditional = curConditional;

            additionalMessyConditions.stream()
                .map(AstUtils::negated)
                .forEachOrdered(previousNegations::add);
        }
        return messyChain;
    }

    private static IfStmt createTidyChain(List<ConditionAndBody> steps) {
        IfStmt tidyChain = null, lastStep = null;
        for (var step : steps) {
            if (step.condition == null) {  // just an else, no additional if
                assert lastStep != null : "conditional chain cannot begin with dangling else clause";
                lastStep.setElseStmt(
                    blockOf(step.body));
            } else {
                var newConditional = new IfStmt(step.condition, blockOf(step.body), null);
                if (tidyChain == null) {
                    tidyChain = newConditional;  // start of chain!
                } else {
                    lastStep.setElseStmt(newConditional);
                }
                lastStep = newConditional;
            }
        }
        return tidyChain;
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
