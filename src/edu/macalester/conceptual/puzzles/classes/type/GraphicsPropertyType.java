package edu.macalester.conceptual.puzzles.classes.type;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.Type;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.puzzles.classes.ExprWithDescription;
import edu.macalester.conceptual.puzzles.classes.TypedExprWithDescription;
import edu.macalester.conceptual.util.Randomness;

import static com.github.javaparser.StaticJavaParser.parseExpression;
import static edu.macalester.conceptual.util.AstUtils.classNamed;

class GraphicsPropertyType implements PropertyType {
    @Override
    public String description() {
        return "graphics object";
    }

    @Override
    public Type astType() {
        return classNamed("GraphicsObject");
    }

    @Override
    public ExprWithDescription generateValue(PuzzleContext ctx, boolean mutable) {
        var gobjClass = Randomness.chooseConst(ctx, "Ellipse", "Rectangle");
        int width = ctx.getRandom().nextInt(10, 50);
        int height = ctx.getRandom().nextInt(10, 50);
        return new ExprWithDescription(
            "new " + gobjClass + "(0, 0, " + width + ", " + height + ")",
            "a/an " + gobjClass.toLowerCase()
                + " with a width of " + width
                + " and a height of " + height);
    }

    @Override
    public TypedExprWithDescription generateDerivedValue(PuzzleContext ctx, String variableName) {
        return Randomness.choose(
            ctx,
            () -> new TypedExprWithDescription(
                PropertyType.INT,
                new MethodCallExpr(parseExpression(variableName), "getWidth"),
                "the width of `" + variableName + "`"),
            () -> new TypedExprWithDescription(
                PropertyType.INT,
                new MethodCallExpr(parseExpression(variableName), "getX"),
                "the x position of `" + variableName + "`")
        );
    }

    @Override
    public ExprWithDescription generateMutation(PuzzleContext ctx, String variableName) {
        int dx = ctx.getRandom().nextInt(1, 10);
        return new ExprWithDescription(
            variableName + ".moveBy(" + dx + ", 0)",
            "moves `" + variableName + "` to the right by " + dx + " pixels"
                + " (using the `moveBy` method)");  // students shouldn't have to have this memorized
    }
}
