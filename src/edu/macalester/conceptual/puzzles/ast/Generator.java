package edu.macalester.conceptual.puzzles.ast;

import java.util.List;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.Randomness;
import edu.macalester.conceptual.util.VariablePool;

import static edu.macalester.conceptual.util.Randomness.*;

class Generator {
    /**
     * Generates an expression consisting of nothing but arithmetic operations.
     */
    public static String generateArithmeticExpression(
        PuzzleContext ctx,
        VariablePool vars,
        int numLeaves
    ) {
        return joinExprsWithOperators(ctx,
            "+ - * / %",
            generateList(numLeaves, () ->
                choose(ctx,
                    () -> choose(ctx,
                        () -> vars.generateInt(ctx),
                        () -> vars.generateDouble(ctx)),
                    () -> String.valueOf(ctx.getRandom().nextInt(10)))));
    }

    /**
     * Generates an expression consisting of arithmetic operations compared with equality /
     * inequality operators, which are then joined with boolean operators.
     */
    public static String generateArithmeticComparisonsExpression(
        PuzzleContext ctx,
        VariablePool vars,
        int numBoolLeaves,
        int maxArithmeticLeaves
    ) {
        return joinExprsWithOperators(ctx,
            "&& ||" + (
                ctx.getDifficulty() >= AstDrawingPuzzle.DIFFICULTY_FOR_EQUALITY_OPERATORS_ON_BOOLS
                    ? " == !="
                    : ""),
            maybeAddNegations(ctx,
                generateList(numBoolLeaves, () ->
                    chooseWithProb(ctx, 0.3,
                        () -> vars.generateBool(ctx),
                        () -> joinExprsWithOperators(ctx,
                            "== != < <= > >=",
                            generateList(2, () ->
                                generateArithmeticExpression(ctx, vars,
                                    ctx.getRandom().nextInt(1, maxArithmeticLeaves + 1))))))));
    }

    private static List<String> maybeAddNegations(PuzzleContext ctx, List<String> booleanExprs) {
        if (ctx.getDifficulty() >= AstDrawingPuzzle.DIFFICULTY_FOR_NEGATIONS) {
            int negationCount = ctx.getRandom().nextInt(booleanExprs.size() / 2);
            for (int n = 0; n < negationCount; n++) {
                int index = ctx.getRandom().nextInt(booleanExprs.size());
                var expr = booleanExprs.get(index);
                if (expr.matches(".*[^a-zA-Z0-9!()].*")) {
                    expr = "(" + expr + ")";
                }
                booleanExprs.set(index, "!" + expr);
            }
        }
        return booleanExprs;
    }

    /**
     * Generates a chain of additions involving ints and strings.
     */
    public static String generateStringAdditionExpression(PuzzleContext ctx, int numLeaves) {
        var stringsAndInts = generateList(numLeaves, (n, m) -> String.valueOf(n));
        for (int n = 0; n < numLeaves / 4; n++) {
            int index = ctx.getRandom().nextInt(stringsAndInts.size() / 3, stringsAndInts.size());
            String leaf = stringsAndInts.get(index);
            if (!leaf.startsWith("\"")) {
                stringsAndInts.set(index, '"' + leaf + '"');
            }
        }
        return joinExprsWithOperators(ctx, "+", stringsAndInts);
    }

    /**
     * Joins the given expressions with random selections from the given operators, sometimes
     * grouping a random subsequence in parentheses.
     */
    public static String joinExprsWithOperators(
        PuzzleContext ctx,
        String operatorChoicesStr,
        List<String> exprs
    ) {
        var operatorChoices = operatorChoicesStr.split(" ");

        // Sometimes add parens around a random subsequence
        int openParen, closeParen;
        if (exprs.size() > 2 && ctx.getRandom().nextBoolean()) {
            int pos0 = ctx.getRandom().nextInt(exprs.size()),
                pos1 = (pos0 + 1 + ctx.getRandom().nextInt(exprs.size() - 2)) % exprs.size();
            openParen = Math.min(pos0, pos1);
            closeParen = Math.max(pos0, pos1);
        } else {
            openParen = closeParen = -1;
        }

        // Stick it all together!
        var result = new StringBuilder();
        for (int n = 0; n < exprs.size(); n++) {
            var nextExpr = exprs.get(n);

            if (!result.isEmpty()) {
                var operator = Randomness.chooseConst(ctx, operatorChoices);
                result.append(operator);

                // When operatorChoices are boolean operators (&& || == !=), the assumption is that
                // the subexpressions are boolean exprs. When a subexpression is `int == int`,
                // we are in danger of generating `bool == int == int`, which will fail to compile.
                // Thus if rhs has a boolean equality operator, we force `bool == (int == int)`.
                // Doing this with ASTs instead of strings might have been better, but...oh well!

                if (operator.matches("==|!=") && nextExpr.matches(".*(==|!=).*")) {
                    nextExpr = "(" + nextExpr + ")";
                }
            }

            if (n == openParen) {
                result.append('(');
            }
            result.append(nextExpr);
            if (n == closeParen) {
                result.append(')');
            }
        }
        return result.toString();
    }
}
