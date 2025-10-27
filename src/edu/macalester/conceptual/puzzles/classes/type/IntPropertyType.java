package edu.macalester.conceptual.puzzles.classes.type;

import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.puzzles.classes.ExprWithDescription;
import edu.macalester.conceptual.puzzles.classes.TypedExprWithDescription;
import edu.macalester.conceptual.util.Randomness;

import static com.github.javaparser.StaticJavaParser.parseExpression;

class IntPropertyType implements PropertyType {
    @Override
    public String description() {
        return "int";
    }

    @Override
    public Type astType() {
        return PrimitiveType.intType();
    }

    @Override
    public ExprWithDescription generateValue(PuzzleContext ctx, boolean mutable) {
        var intConstant = String.valueOf(ctx.getRandom().nextInt(1, 20));
        return new ExprWithDescription(intConstant, intConstant);
    }

    @Override
    public TypedExprWithDescription generateDerivedValue(PuzzleContext ctx, String variableName) {
        return Randomness.choose(
            ctx,
            () -> {
                return new TypedExprWithDescription(
                    PropertyType.INT,
                    parseExpression(variableName + " * " + variableName),
                    "`" + variableName + "` squared");
            },
            () -> {
                int number = ctx.getRandom().nextInt(1, 10);
                return new TypedExprWithDescription(
                    PropertyType.INT,
                    parseExpression(variableName + " + " + number),
                    "`" + variableName + "` plus " + number);
            }
        );
    }

    @Override
    public ExprWithDescription generateMutation(PuzzleContext ctx, String variableName) {
        int delta = ctx.getRandom().nextInt(1, 10);
        return new ExprWithDescription(
            variableName + " += " + delta,
            "adds " + delta + " to `" + variableName + "`");
    }
}
