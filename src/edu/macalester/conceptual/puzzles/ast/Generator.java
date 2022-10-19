package edu.macalester.conceptual.puzzles.ast;

import java.util.List;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.Nonsense;
import edu.macalester.conceptual.util.Randomness;

import static edu.macalester.conceptual.util.Randomness.*;

public class Generator {
    public static String generateArithmeticExpression(PuzzleContext ctx, int numLeaves) {
        return joinExprsWithOperators(ctx,
            "+ - * / %".split(" "),
            generateList(numLeaves, () -> Nonsense.word(ctx)));
    }

    public static String generateArithmeticComparisonsExpression(
        PuzzleContext ctx,
        int boolLeaves,
        int maxArithLeaves
    ) {
        return joinExprsWithOperators(ctx,
            "&& ||".split(" "),
            generateList(boolLeaves, () ->
                chooseWithProb(ctx, 0.2,
                    () -> Nonsense.word(ctx),
                    () -> joinExprsWithOperators(ctx,
                        "== != < <= > >=".split(" "),
                        generateList(2, () ->
                            generateArithmeticExpression(ctx,
                                ctx.getRandom().nextInt(1, maxArithLeaves + 1)))))));
    }

    public static String joinExprsWithOperators(
        PuzzleContext ctx,
        String[] operatorChoices,
        List<String> exprs
    ) {
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
