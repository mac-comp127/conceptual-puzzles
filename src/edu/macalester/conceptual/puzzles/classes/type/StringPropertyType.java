package edu.macalester.conceptual.puzzles.classes.type;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.Type;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.puzzles.classes.ExprWithDescription;
import edu.macalester.conceptual.puzzles.classes.TypedExprWithDescription;
import edu.macalester.conceptual.util.Nonsense;
import edu.macalester.conceptual.util.Randomness;

import static com.github.javaparser.StaticJavaParser.parseExpression;
import static edu.macalester.conceptual.util.AstUtils.classNamed;
import static edu.macalester.conceptual.util.Nonsense.word;

class StringPropertyType implements PropertyType {
    @Override
    public String description() {
        return "string";
    }

    @Override
    public Type astType() {
        return classNamed("String");
    }

    @Override
    public ExprWithDescription generateValue(PuzzleContext ctx, boolean mutable) {
        var stringConstant = "\"" + word(ctx) + "\"";
        return new ExprWithDescription(stringConstant, stringConstant);
    }

    @Override
    public TypedExprWithDescription generateDerivedValue(PuzzleContext ctx, String variableName) {
        return Randomness.choose(
            ctx,
            () -> new TypedExprWithDescription(
                PropertyType.INT,
                new MethodCallExpr(parseExpression(variableName), "length"),
                "the length of `" + variableName + "`"),
            () -> new TypedExprWithDescription(
                PropertyType.STRING,
                parseExpression(variableName + " + \"!!\""),
                "`" + variableName + "` with two exclamation points appended")
        );
    }

    @Override
    public ExprWithDescription generateMutation(PuzzleContext ctx, String variableName) {
        var word = "\"" + Nonsense.word(ctx) + "\"";
        return new ExprWithDescription(
            variableName + " += " + word,
            "adds " + word + " to `" + variableName + "`");
    }
}
