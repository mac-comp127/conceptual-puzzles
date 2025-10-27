package edu.macalester.conceptual.puzzles.classes.type;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.Type;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.puzzles.classes.ExprWithDescription;
import edu.macalester.conceptual.puzzles.classes.TypedExprWithDescription;
import edu.macalester.conceptual.util.Nonsense;
import edu.macalester.conceptual.util.Randomness;

import static com.github.javaparser.StaticJavaParser.parseExpression;
import static edu.macalester.conceptual.util.AstUtils.classNamed;

class ListOfStringsPropertyType implements PropertyType {
    @Override
    public String description() {
        return "list of strings";
    }

    @Override
    public Type astType() {
        return classNamed("List<String>");
    }

    @Override
    public ExprWithDescription generateValue(PuzzleContext ctx, boolean mutable) {
        if(mutable) {
            return new ExprWithDescription("new ArrayList<>()", "an empty mutable list");
        } else {
            var words = IntStream.range(0, ctx.getRandom().nextInt(2, 4))
                .mapToObj(i -> '"' + Nonsense.word(ctx) + '"')
                .collect(Collectors.joining(", "));
            return new ExprWithDescription("List.of(" + words + ")", "[" + words + "]");
        }
    }

    @Override
    public TypedExprWithDescription generateDerivedValue(PuzzleContext ctx, String variableName) {
        return Randomness.choose(
            ctx,
            () -> new TypedExprWithDescription(
                PropertyType.INT,
                new MethodCallExpr(parseExpression(variableName), "size"),
                "the size of `" + variableName + "`"),
            () -> new TypedExprWithDescription(
                PropertyType.STRING,
                parseExpression(variableName + ".get(0)"),
                "the first element of `" + variableName + "`")
        );
    }

    @Override
    public ExprWithDescription generateMutation(PuzzleContext ctx, String variableName) {
        var word = "\"" + Nonsense.word(ctx) + "\"";
        return new ExprWithDescription(
            variableName + ".add(" + word + ")",
            "adds " + word + " to `" + variableName + "`");
    }
}
