package edu.macalester.conceptual.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.Nonsense;
import edu.macalester.conceptual.util.Randomness;

/**
 * A collection of variable declarations. Useful for constructing code context for examples, and
 * for using {@link Evaluator}.
 */
public class VariablePool {
    private final List<Variable> vars = new ArrayList<>();

    public String generateBool(PuzzleContext ctx) {
        return generate(ctx, "boolean", Randomness.chooseConst(ctx, "true", "false"));
    }

    public String generateInt(PuzzleContext ctx) {
        return generate(ctx, "int", String.valueOf(ctx.getRandom().nextInt(1, 10)));
    }

    public String generateDouble(PuzzleContext ctx) {
        return generate(ctx, "double", String.valueOf((double) ctx.getRandom().nextInt(1, 6)));
    }

    private String generate(PuzzleContext ctx, String type, String value) {
        var newVar = new Variable(Nonsense.word(ctx), type, value);
        vars.add(newVar);
        return newVar.name;
    }

    public String allDeclarations() {
        return vars.stream()
            .map(Variable::declaration)
            .collect(Collectors.joining("\n"));
    }

    public boolean isEmpty() {
        return vars.isEmpty();
    }

    private record Variable(
        String name,
        String type,
        String value
    ) {
        String declaration() {
            return type + " " + name + "=" + value + ";";
        }
    }
}
