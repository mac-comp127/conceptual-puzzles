package edu.macalester.conceptual.puzzles.vars;

import java.util.Collections;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.Evaluator;
import edu.macalester.conceptual.util.Nonsense;

import static edu.macalester.conceptual.util.CodeFormatting.*;
import static edu.macalester.conceptual.util.Randomness.*;
import static java.util.stream.Collectors.joining;

public class VariablesPuzzle implements Puzzle {
    @Override
    public byte id() {
        return 3;
    }

    @Override
    public String name() {
        return "vars";
    }

    @Override
    public String description() {
        return "Variable scope and lifetime";
    }

    @Override
    public void generate(PuzzleContext ctx) {
        var className = Nonsense.typeName(ctx);
        var twiddleMethodName = Nonsense.methodName(ctx);
        var parameterName = Nonsense.variableName(ctx);
        var staticVarName = Nonsense.variableName(ctx);
        var instanceVarName = Nonsense.variableName(ctx);
        var localVarName = Nonsense.variableName(ctx);

        var threeVars = shuffledListOf(ctx, staticVarName, instanceVarName, localVarName);

        var mainVar = className.substring(0, 1).toLowerCase();
        var twiddling = generateList(4, (i, j) ->
            mainVar + (i % 2) + "." + twiddleMethodName + "(" + (int) Math.pow(10, i) + ")");
        int n = ctx.getRandom().nextInt(2), m = 1 - n;
        insertAtRandomPosition(ctx, twiddling, mainVar + n + "=" + mainVar + m);

        var members = shuffledListOf(ctx,
            "private static int " + staticVarName + " = 0;",
            "private int " + instanceVarName + " = 0;",
            "public void " + twiddleMethodName + "(int " + parameterName + ") {"
                + "\n// [a]\n"
                + "int " + localVarName + " = 0;"
                // Increment static, instance, and local var, in random order
                + threeVars.stream()
                    .map(v -> v + "+=" + parameterName + ";")
                    .collect(joining())
                // Print all three in same order we incremented
                + "System.out.println("
                    + threeVars.stream()
                        .map(v -> "\"" + v + "=\"+" + v)
                        .collect(joining("+"))
                        .replace("+\"", "+\"  ")
                + ");"
                + "\n// [b]\n"
            + "}",
            "public static void main(String[] args) {"
                + className + " " + mainVar + "0 = new " + className + "();"
                + className + " " + mainVar + "1 = new " + className + "();"
                + joinStatements(twiddling)
                + "\n// [c]\n"
            + "}");
        Collections.shuffle(members, ctx.getRandom());

        var classDecl = prettifyTypeDecl(
            "public class " + className + "{"
                + joinCode(members)
            + "}");

        ctx.output().paragraph("Given the following code:");
        ctx.output().codeBlock(classDecl);
        ctx.output().numberedList(
            "What does the main method print?",
            "Which of the three variables "
                + threeVars.stream().map(s -> "`" + s + "`").toList()
                + " are in scope at *[a]*?",
            "Which are in scope at *[b]*?",
            "Which are in scope at *[c]*?");

        ctx.solution(() -> {
            ctx.output().numberedList(
                () -> {
                    ctx.output().paragraph("The output is:");
                    ctx.output().codeBlock(
                        Evaluator.captureOutput(classDecl, className + ".main(null)"));
                },
                () -> ctx.output().paragraph(
                    "`{0}` and `{1}` are in scope at [a]."
                        + " _(`{2}` is out of scope because it is not declared yet.)_",
                    staticVarName, instanceVarName, localVarName),
                () -> ctx.output().paragraph(
                    "All three are in scope at [b]."),
                () -> ctx.output().paragraph(
                    "Only `{0}` is in scope at [c]. _(`{1}` is an instance variable, but `main` is a"
                        + " static method. `{2}` is local to the `{3}` function.)_",
                    staticVarName, instanceVarName, localVarName, twiddleMethodName));
            ctx.output().paragraph(
                """
                (In the solutions above, the explanatory text in parentheses is just to help you
                understand the solution as you study. You do *not* need to write out all of that
                text when you submit your solutions for puzzle of this type! For example, it would
                be fine to submit just “`{0}`, `{1}`” for your solution to question 1 above.)
                """,
                staticVarName,
                instanceVarName);
        });
    }
}
