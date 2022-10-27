package edu.macalester.conceptual.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import edu.macalester.conceptual.context.PuzzleContext;

/**
 * Utilities for adding random variation to puzzles.
 */
public enum Randomness {
    ; // static utility class

    /**
     * Randomly selects one of the given choices, with equal probability. Note that using this method
     * will cause Java to evaluate <i>all</i> of the choices, even the ones that are not selected.
     * It is therefore best to use this method only if the choices are all constants (thus the name)
     * or otherwise have zero construction cost. To select among choices that are expensive to
     * generate, use {@link #choose} instead.
     */
    @SafeVarargs
    public static <Choice> Choice chooseConst(PuzzleContext ctx, Choice... choices) {
        return choices[ctx.getRandom().nextInt(choices.length)];
    }

    /**
     * Randomly selects one of the given choices, with equal probability, evaluating only the one
     * that is actually selected. Example:
     * <pre>
     * Randomness.choose(ctx,
     *   () -> doOneBigThing(...),
     *   () -> doOtherBigThing(...),
     *   () -> {
     *     for (bigLoop) {
     *       lotsOfStuff();
     *     }
     *   });
     * </pre>
     */
    @SafeVarargs
    public static <Choice> Choice choose(PuzzleContext ctx, Supplier<Choice>... choices) {
        return chooseConst(ctx, choices).get();
    }

    /**
     * Chooses between one of two choices, with unequal probability. For choices with greater than
     * zero construction cost, use the lazy-evaluating {@link #chooseWithProb(PuzzleContext, double, Supplier, Supplier)}
     * variant instead.
     * <p>
     * For weighted probability selection of >2 choices, see {@link WeightedChoices}.
     *
     * @param firstProbability The probability of choosing choice0, in the range 0...1.
     */
    public static <Choice> Choice chooseWithProb(
        PuzzleContext ctx,
        double firstProbability,
        Choice choice0,
        Choice choice1
    ) {
        return chooseWithProb(ctx, firstProbability, () -> choice0, () -> choice1);
    }

    /**
     * Chooses between one of two choices, with unequal probability, lazily evaluating only the one
     * selected choice.
     * <p>
     * For weighted probability selection of >2 choices, see {@link WeightedChoices}.
     *
     * @param firstProbability The probability of choosing choice0, in the range 0...1.
     */
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

    /**
     * Inserts <code>elem</code> at a random position in <code>mutableList</code>, including the
     * very start and the very end.
     */
    public static <Elem> void insertAtRandomPosition(
        PuzzleContext ctx,
        List<Elem> mutableList,
        Elem elem
    ) {
        mutableList.add(ctx.getRandom().nextInt(mutableList.size() + 1), elem);
    }

    /**
     * Repeatedly attempts <code>supplier</code> until it returns a String of at least <code>minLength</code>.
     */
    public static String withMinLength(int minLength, Supplier<String> supplier) {
        while (true) {
            String result = supplier.get();
            if (result.length() >= minLength) {
                return result;
            }
        }
    }

    /**
     * Calls <code>generator</code> repeatedly <code>count</code> times, returning a list of the
     * results.
     */
    public static <T> List<T> generateList(int count, Supplier<T> generator) {
        return generateList(count, (done, left) -> generator.get());
    }

    /**
     * Calls <code>generator</code> repeatedly <code>count</code> times, passing it the element
     * index and the number of items remaining to generate after this one, and returning a list of
     * the results.
     */
    public static <T> List<T> generateList(int count, BiFunction<Integer, Integer, T> generator) {
        var result = new ArrayList<T>(count);
        for (int n = 0; n < count; n++) {
            result.add(generator.apply(n, count - 1 - n));
        }
        return result;
    }
}
