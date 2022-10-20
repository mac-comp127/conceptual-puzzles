package edu.macalester.conceptual.puzzles.ast;

import java.util.List;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.Randomness;

import static edu.macalester.conceptual.util.Randomness.*;

public class Generator {
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

    public static String generateArithmeticComparisonsExpression(
        PuzzleContext ctx,
        VariablePool vars,
        int numBoolLeaves,
        int maxArithmeticLeaves
    ) {
        return joinExprsWithOperators(ctx,
            "&& ||",
            generateList(numBoolLeaves, () ->
                chooseWithProb(ctx, 0.2,
                    () -> vars.generateBool(ctx),
                    () -> joinExprsWithOperators(ctx,
                        "== != < <= > >=",
                        generateList(2, () ->
                            generateArithmeticExpression(ctx, vars,
                                ctx.getRandom().nextInt(1, maxArithmeticLeaves + 1)))))));
    }

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

    public static String joinExprsWithOperators(
        PuzzleContext ctx,
        String operatorChoicesStr,
        List<String> exprs
    ) {
        var operatorChoices = operatorChoicesStr.split(" ");
        var result = new StringBuilder();
        for (var expr : exprs) {
            if (!result.isEmpty()) {
                result.append(Randomness.chooseConst(ctx, operatorChoices));
            }
            result.append(expr);
        }
        return result.toString();
    }
}
