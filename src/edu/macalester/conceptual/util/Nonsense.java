package edu.macalester.conceptual.util;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import edu.macalester.conceptual.ast.AstUtils;
import edu.macalester.conceptual.random.PuzzleContext;
import edu.macalester.conceptual.random.WeightedChoices;

import static com.github.javaparser.utils.Utils.capitalize;

public class Nonsense {
    private static final WeightedChoices<String>
        ONSETS = WeightedChoices.fromResource("syllable-parts/onsets"),
        NUCLEI = WeightedChoices.fromResource("syllable-parts/nuclei"),
        CODAS = WeightedChoices.fromResource("syllable-parts/codas");

    public static VariableDeclarator variable(PuzzleContext ctx) {
        return variable(ctx, type(ctx));
    }

    public static VariableDeclarator variable(PuzzleContext ctx, Type type) {
        return new VariableDeclarator(type, new SimpleName(variableName(ctx)));
    }

    private static Type type(PuzzleContext ctx) {
        return new ClassOrInterfaceType(null, typeName(ctx));
    }

    public static String typeName(PuzzleContext ctx) {
        return words(ctx, NameFormat.CAPITALIZED_CAMEL_CASE, 2, 12, 5);
    }

    public static String methodName(PuzzleContext ctx) {
        return words(ctx, NameFormat.LOWER_CAMEL_CASE, 3, 6, 4);
    }

    public static MethodCallExpr methodCallExpr(PuzzleContext ctx, Expression... args) {
        return new MethodCallExpr(
            null,
            methodName(ctx),
            AstUtils.nodes(args));
    }

    public static String propertyName(PuzzleContext ctx) {
        return words(ctx, NameFormat.LOWER_CAMEL_CASE, 2, 5, 3);
    }

    public static String variableName(PuzzleContext ctx) {
        return words(ctx, NameFormat.LOWER_CAMEL_CASE, 1, 4, 1);
    }

    public static String constantName(PuzzleContext ctx) {
        return words(ctx, NameFormat.SCREAMING_SNAKE_CASE, 2, 10, 6);
    }

    public static NameExpr variableNameExpr(PuzzleContext ctx) {
        return new NameExpr(variableName(ctx));
    }

    public static NameExpr propertyNameExpr(PuzzleContext ctx) {
        return new NameExpr(propertyName(ctx));
    }

    public static String words(
        PuzzleContext ctx,
        NameFormat format,
        int minWordLen,
        int maxWordLen, int minTotalLen
    ) {
        StringBuilder result = new StringBuilder();
        while (result.length() < minTotalLen) {
            String nextWord = word(ctx);
            if (nextWord.length() < minWordLen || nextWord.length() > maxWordLen) {
                continue;
            }
            result.append(
                result.isEmpty()
                    ? format.formatFirstWord(nextWord)
                    : format.formatSubsequentWord(nextWord));
        }
        return result.toString();
    }

    public static String word(PuzzleContext ctx) {
        StringBuilder result = new StringBuilder();
        for (int i = ctx.getRandom().nextInt(2); i >= 0; i--) {
            result.append(syllable(ctx));
        }
        return result.toString();
    }

    public static String syllable(PuzzleContext ctx) {
        return ONSETS.choose(ctx) + NUCLEI.choose(ctx) + CODAS.choose(ctx);
    }

    public static enum NameFormat {
        LOWER_CAMEL_CASE {
            String formatFirstWord(String word) {
                return word;
            }
            String formatSubsequentWord(String word) {
                return capitalize(word);
            }
        },

        CAPITALIZED_CAMEL_CASE {
            String formatFirstWord(String word) {
                return capitalize(word);
            }
            String formatSubsequentWord(String word) {
                return capitalize(word);
            }
        },

        SCREAMING_SNAKE_CASE {
            String formatFirstWord(String word) {
                return word.toUpperCase();
            }
            String formatSubsequentWord(String word) {
                return "_" + word.toUpperCase();
            }
        };

        abstract String formatFirstWord(String word);
        abstract String formatSubsequentWord(String word);
    }
}
