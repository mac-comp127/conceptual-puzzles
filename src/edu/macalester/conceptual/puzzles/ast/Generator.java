package edu.macalester.conceptual.puzzles.ast;

import java.util.List;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.Nonsense;
import edu.macalester.conceptual.util.Randomness;

import static edu.macalester.conceptual.util.Randomness.*;

public class Generator {
    public static String generateArithmeticExpression(PuzzleContext ctx, int numLeaves) {
        return joinExprsWithOperators(ctx,
            "+ - * / %",
            generateList(numLeaves, () -> Nonsense.word(ctx)));
    }

    public static String generateArithmeticComparisonsExpression(
        PuzzleContext ctx,
        int boolLeaves,
        int maxArithmeticLeaves
    ) {
        return joinExprsWithOperators(ctx,
            "&& ||",
            generateList(boolLeaves, () ->
                chooseWithProb(ctx, 0.2,
                    () -> Nonsense.word(ctx),
                    () -> joinExprsWithOperators(ctx,
                        "== != < <= > >=",
                        generateList(2, () ->
                            generateArithmeticExpression(ctx,
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
