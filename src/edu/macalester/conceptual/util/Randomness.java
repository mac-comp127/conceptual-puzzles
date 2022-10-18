package edu.macalester.conceptual.util;

import java.util.List;
import java.util.function.Supplier;

import edu.macalester.conceptual.context.PuzzleContext;

public enum Randomness {
    ; // static utility class

    @SafeVarargs
    public static <Choice> Choice chooseConst(PuzzleContext ctx, Choice... choices) {
        return choices[ctx.getRandom().nextInt(choices.length)];
    }

    @SafeVarargs
    public static <Choice> Choice choose(PuzzleContext ctx, Supplier<Choice>... choices) {
        return chooseConst(ctx, choices).get();
    }

    public static <Choice> Choice chooseWithProb(
        PuzzleContext ctx,
        double firstProbability,
        Choice choice0,
        Choice choice1
    ) {
        return chooseWithProb(ctx, firstProbability, () -> choice0, () -> choice1);
    }

    public static <Choice> Choice chooseWithProb(
        PuzzleContext ctx,
        double firstProbability,
        Supplier<Choice> choice0,
        Supplier<Choice> choice1
    ) {
        return
            (ctx.getRandom().nextFloat() < firstProbability
                ? choice0
                : choice1)
            .get();
    }

    public static <Elem> void insertAtRandomPosition(
        PuzzleContext ctx,
        List<Elem> mutableList,
        Elem elem
    ) {
        mutableList.add(ctx.getRandom().nextInt(mutableList.size() + 1), elem);
    }

    public static String withMinLength(int minLength, Supplier<String> supplier) {
        while (true) {
            String result = supplier.get();
            if (result.length() >= minLength) {
                return result;
            }
        }
    }
}
