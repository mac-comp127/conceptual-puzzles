package edu.macalester.conceptual.puzzles.loops;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.Nonsense;

import static edu.macalester.conceptual.util.CodeFormatting.joinCode;
import static edu.macalester.conceptual.util.CodeFormatting.joinStatements;

class SimpleLoop {
    private final String varName, varType, initializer;
    private final String endCondition;
    private final String nextStep;
    private final String body;

    public static SimpleLoop generateNumericLoop(PuzzleContext ctx) {
        String varName = Nonsense.variableName(ctx);
        String varType = ctx.choose("int", "int", "int", "long", "short", "double");
        String initializer =
            ctx.getRandom().nextBoolean()
                ? Nonsense.variableName(ctx)
                : String.valueOf(ctx.getRandom().nextInt(100));

        boolean growing = ctx.getRandom().nextBoolean();

        String endCondition = joinCode(
            varName,
            growing
                ? ctx.choose("<", "<=")
                : ctx.choose(">", ">=", "!="),
            Nonsense.propertyName(ctx));

        String nextStep =
            ctx.getRandom().nextBoolean()
                ? joinCode(
                    varName,
                    growing ? "++" : "--")
                : joinCode(
                    varName,
                    growing
                        ? ctx.choose("+=", "*=")
                        : ctx.choose("-=", "/="),
                    String.valueOf(ctx.getRandom().nextInt(2, 5)));

        String body =
            joinStatements(
                joinCode(
                    Nonsense.methodName(ctx),
                    "(", varName, ")"));

        return new SimpleLoop(varType, varName, initializer, endCondition, nextStep, body);
    }

    public SimpleLoop(
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
}
