package edu.macalester.conceptual.puzzles.loops;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.Nonsense;

import static edu.macalester.conceptual.util.CodeFormatting.*;
import static edu.macalester.conceptual.util.Randomness.*;

class GeneralizedLoop {
    private final String varName, varType, initializer;
    private final String endCondition;
    private final String nextStep;
    private String body;

    public static GeneralizedLoop generateNumericLoop(PuzzleContext ctx) {
        String varName = Nonsense.variableName(ctx);
        String varType = chooseConst(ctx, "int", "int", "int", "long", "short", "double");
        String initializer =
            choose(ctx,
                () -> Nonsense.variableName(ctx),
                () -> String.valueOf(ctx.getRandom().nextInt(100)));

        boolean growing = ctx.getRandom().nextBoolean();

        String endCondition = joinCode(
            varName,
            choose(ctx,
                () -> chooseConst(ctx, "<", "<="),
                () -> chooseConst(ctx, ">", ">=", "!=")),
            Nonsense.propertyName(ctx));

        String nextStep =
            choose(ctx,
                () -> joinCode(
                    varName,
                    growing ? "++" : "--"),
                () -> joinCode(
                    varName,
                    growing
                        ? chooseConst(ctx, "+=", "*=")
                        : chooseConst(ctx, "-=", "/="),
                    String.valueOf(ctx.getRandom().nextInt(2, 5))));

        int extraStatementPosition = ctx.getRandom().nextInt(4);
        String body =
            joinStatements(
                extraStatementPosition == 0
                    ? joinCode(Nonsense.methodName(ctx), "()")
                    : null,
                joinCode(
                    Nonsense.methodName(ctx),
                    "(",
                    varName,
                    choose(ctx,
                        () -> ", " + ctx.getRandom().nextInt(50),
                        () -> ""),
                    ")"),
                extraStatementPosition == 1
                    ? joinCode(Nonsense.methodName(ctx), "()")
                    : null);

        return new GeneralizedLoop(varType, varName, initializer, endCondition, nextStep, body);
    }

    public static GeneralizedLoop makeCounterLoop(
        String varName,
        String min,
        String max,
        String body
    ) {
        return new GeneralizedLoop("int", varName, min, varName + "<" + max, varName + "++", body);
    }

    public GeneralizedLoop(
        String varType,
        String varName,
        String initializer,
        String endCondition,
        String nextStep,
        String body
    ) {
        this.varType = varType;
        this.varName = varName;
        this.initializer = initializer;
        this.endCondition = endCondition;
        this.nextStep = nextStep;
        this.body = body;
    }

    public String getVarName() {
        return varName;
    }

    public String getVarType() {
        return varType;
    }

    public String getInitializer() {
        return initializer;
    }

    public String getVarDeclaration() {
        return joinCode(getVarType(), getVarName(), "=", getInitializer());
    }

    public String getEndCondition() {
        return endCondition;
    }

    public String getNextStep() {
        return nextStep;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
