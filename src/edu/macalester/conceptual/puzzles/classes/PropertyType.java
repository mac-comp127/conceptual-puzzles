package edu.macalester.conceptual.puzzles.classes;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.Nonsense;
import edu.macalester.conceptual.util.Randomness;

import static com.github.javaparser.StaticJavaParser.parseExpression;
import static edu.macalester.conceptual.util.AstUtils.classNamed;
import static edu.macalester.conceptual.util.Nonsense.word;

enum PropertyType {
    STRING(classNamed("String"), "string") {
        ExprWithDescription generateValue(PuzzleContext ctx, boolean mutable) {
            var stringConstant = "\"" + word(ctx) + "\"";
            return new ExprWithDescription(stringConstant, stringConstant);
        }

        TypedExprWithDescription generateDerivedValue(PuzzleContext ctx, String variableName) {
            return Randomness.choose(ctx,
                () -> new TypedExprWithDescription(
                    INT,
                    new MethodCallExpr(parseExpression(variableName), "length"),
                    "the length of `" + variableName + "`"),
                () -> new TypedExprWithDescription(
                    STRING,
                    parseExpression(variableName + " + \"!!\""),
                    "`" + variableName + "` with two exclamation points appended")
            );
        }

        ExprWithDescription generateMutation(PuzzleContext ctx, String variableName) {
            var word = "\"" + Nonsense.word(ctx) + "\"";
            return new ExprWithDescription(
                variableName + " += " + word,
                "adds " + word + " to `" + variableName + "`");
        }
    },

    INT(PrimitiveType.intType(), "int") {
        ExprWithDescription generateValue(PuzzleContext ctx, boolean mutable) {
            var intConstant = String.valueOf(ctx.getRandom().nextInt(1, 20));
            return new ExprWithDescription(intConstant, intConstant);
        }

        TypedExprWithDescription generateDerivedValue(PuzzleContext ctx, String variableName) {
            return Randomness.choose(ctx,
                () -> {
                    return new TypedExprWithDescription(
                        INT,
                        parseExpression(variableName + " * " + variableName),
                        "`" + variableName + "` squared");
                },
                () -> {
                    int number = ctx.getRandom().nextInt(1, 10);
                    return new TypedExprWithDescription(
                        INT,
                        parseExpression(variableName + " + " + number),
                        "`" + variableName + "` plus " + number);
                }
            );
        }

        ExprWithDescription generateMutation(PuzzleContext ctx, String variableName) {
            int delta = ctx.getRandom().nextInt(1, 10);
            return new ExprWithDescription(
                variableName + " += " + delta,
                "adds " + delta + " to `" + variableName + "`");
        }
    },

    LIST_OF_STRINGS(classNamed("List<String>"), "list of strings") {
        ExprWithDescription generateValue(PuzzleContext ctx, boolean mutable) {
            if (mutable) {
                return new ExprWithDescription("new ArrayList<>()", "an empty mutable list");
            } else {
                var words = IntStream.range(0, ctx.getRandom().nextInt(2, 4))
                    .mapToObj(i -> '"' + Nonsense.word(ctx) + '"')
                    .collect(Collectors.joining(", "));
                return new ExprWithDescription("List.of(" + words + ")", "[" + words + "]");
            }
        }

        TypedExprWithDescription generateDerivedValue(PuzzleContext ctx, String variableName) {
            return Randomness.choose(ctx,
                () -> new TypedExprWithDescription(
                    INT,
                    new MethodCallExpr(parseExpression(variableName), "size"),
                    "the size of `" + variableName + "`"),
                () -> new TypedExprWithDescription(
                    STRING,
                    parseExpression(variableName + ".get(0)"),
                    "the first element of `" + variableName + "`")
            );
        }

        ExprWithDescription generateMutation(PuzzleContext ctx, String variableName) {
            var word = "\"" + Nonsense.word(ctx) + "\"";
            return new ExprWithDescription(
                variableName + ".add(" + word + ")",
                "adds " + word + " to `" + variableName + "`");
        }
    },

    GRAPHICS(classNamed("GraphicsObject"), "graphics object") {
        ExprWithDescription generateValue(PuzzleContext ctx, boolean mutable) {
            var gobjClass = Randomness.chooseConst(ctx, "Ellipse", "Rectangle");
            int width = ctx.getRandom().nextInt(10, 50);
            int height = ctx.getRandom().nextInt(10, 50);
            return new ExprWithDescription(
                "new " + gobjClass + "(0, 0, " + width + ", " + height + ")",
                "a/an " + gobjClass.toLowerCase()
                    + " with a width of " + width
                    + " and a height of " + height);
        }

        TypedExprWithDescription generateDerivedValue(PuzzleContext ctx, String variableName) {
            return Randomness.choose(ctx,
                () -> new TypedExprWithDescription(
                    INT,
                    new MethodCallExpr(parseExpression(variableName), "getWidth"),
                    "the width of `" + variableName + "`"),
                () -> new TypedExprWithDescription(
                    INT,
                    new MethodCallExpr(parseExpression(variableName), "getX"),
                    "the x position of `" + variableName + "`")
            );
        }

        ExprWithDescription generateMutation(PuzzleContext ctx, String variableName) {
            int dx = ctx.getRandom().nextInt(1, 10);
            return new ExprWithDescription(
                variableName + ".moveBy(" + dx + ", 0)",
                "moves `" + variableName + "` to the right by " + dx + " pixels");
        }
    };

    private final Type astType;
    private final String description;

    PropertyType(Type astType, String description) {
        this.astType = astType;
        this.description = description;
    }

    Type astType() {
        return astType;
    }

    String javaName() {
        return astType().asString();
    }

    String description() {
        return description;
    }

    abstract ExprWithDescription generateValue(PuzzleContext ctx, boolean mutable);
    abstract ExprWithDescription generateMutation(PuzzleContext ctx, String variableName);
    abstract TypedExprWithDescription generateDerivedValue(PuzzleContext ctx, String variableName);
}
