package edu.macalester.conceptual.puzzles.booleans;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UnaryExpr;

import java.util.ArrayList;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.Nonsense;

import static com.github.javaparser.StaticJavaParser.parseExpression;
import static com.github.javaparser.ast.expr.BinaryExpr.Operator.*;
import static com.github.javaparser.ast.expr.UnaryExpr.Operator.*;
import static edu.macalester.conceptual.ast.AstUtils.*;
import static edu.macalester.conceptual.util.Randomness.*;

public enum Generator {
    ; // static methods only

    public static Expression generateBooleanExpr(
        PuzzleContext ctx,
        int numLeaves,
        boolean allowNegations
    ) {
        var nodes = new ArrayList<Expression>();
        for(int n = 0; n < numLeaves; n++) {
            nodes.add(generateLeaf(ctx));
        }

        while (nodes.size() > 1) {
            int i = ctx.getRandom().nextInt(nodes.size());
            if (allowNegations
                && ctx.getRandom().nextFloat() < 0.2
                && !(nodes.get(i) instanceof UnaryExpr)  // avoid double negatives
            ) {
                nodes.add(
                    new UnaryExpr(
                        nodes.remove(i),
                        LOGICAL_COMPLEMENT));
            } else {
                int j = ctx.getRandom().nextInt(nodes.size() - 1) % nodes.size();
                nodes.add(
                    new BinaryExpr(
                        nodes.remove(i),
                        nodes.remove(j),
                        chooseConst(ctx, OR, AND)));
            }
        }
        return nodes.get(0);
    }

    private static Expression generateLeaf(PuzzleContext ctx) {
        return parseExpression(
            chooseWithProb(ctx, 0.3,
                () -> atom(ctx, false) + comparisonOperator(ctx) + atom(ctx, true),
                () -> atom(ctx, false)));
    }

    private static String comparisonOperator(PuzzleContext ctx) {
        return switch(ctx.getRandom().nextInt(9)) {
            case 0 -> "<";
            case 1 -> ">";
            case 2 -> "<=";
            case 3 -> ">=";
            case 4, 5 -> "!=";
            default -> "==";
        };
    }

    private static String atom(PuzzleContext ctx, boolean allowInt) {
        return chooseWithProb(ctx, allowInt ? 0.6 : 0,
            () -> String.valueOf(ctx.getRandom().nextInt(10)),
            () -> chooseWithProb(ctx, 0.4,
                () -> Nonsense.methodName(ctx) + "()",
                () -> chooseWithProb(ctx, 0.2, "!", "") + Nonsense.variableName(ctx)));
    }

    public static void main(String[] args) {
        var ctx = PuzzleContext.generate((byte) 0);
        ctx.emitPuzzle(() -> {
            for(int n = 1; n <= 10; n++) {
                System.out.println();
                Expression expr = generateBooleanExpr(ctx, n, true);
                System.out.println(withParensAsNeeded(expr));
                System.out.println(withParensAsNeeded(negated(expr)));
            }
        });
    }
}
