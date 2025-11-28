package edu.macalester.conceptual.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.google.common.collect.Lists;

import edu.macalester.conceptual.context.PuzzleContext;

import static com.github.javaparser.utils.Utils.capitalize;
import static edu.macalester.conceptual.util.Randomness.*;

/**
 * Utilities for generating nonsense word Java identifiers. Note that methods here come in varieties
 * that return both Strings and JavaParser AST nodes.
 */
public class Nonsense {
    private static final WeightedChoices<String>
        ONSETS = WeightedChoices.fromResource("syllable-parts/onsets"),
        NUCLEI = WeightedChoices.fromResource("syllable-parts/nuclei"),
        CODAS = WeightedChoices.fromResource("syllable-parts/codas");

    private static final ExcludedWords excludedWords = new ExcludedWords();

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
        return words(ctx, NameFormat.CAPITALIZED_CAMEL_CASE, 3, 10, 4);
    }

    public static String shortTypeName(PuzzleContext ctx) {
        return words(ctx, NameFormat.CAPITALIZED_CAMEL_CASE, 3, 5, 3);
    }

    public static String methodName(PuzzleContext ctx) {
        return words(ctx, NameFormat.LOWER_CAMEL_CASE, 3, 6, 4);
    }

    public static String verbyMethodName(PuzzleContext ctx) {
        var name = words(ctx, NameFormat.LOWER_CAMEL_CASE, 3, 5, 3);;
        if (name.matches(".*[aeiou]$")) {
            name += chooseConst(ctx, "l", "t", "n");
        }
        name += chooseConst(ctx, "ate", "ify", "ize");
        return name;
    }

    public static String methodCall(PuzzleContext ctx, String... args) {
        return methodName(ctx) + "(" + String.join(",", args) + ")";
    }

    public static String methodCall(PuzzleContext ctx, int extraArgs, String... args) {
        var paddedArgs = Lists.newArrayList(args);
        for (int n = 0; n < extraArgs; n++) {
            insertAtRandomPosition(
                ctx,
                paddedArgs,
                chooseWithProb(ctx, 0.6,
                    () -> String.valueOf(ctx.getRandom().nextInt(-3, 10)),
                    () -> Nonsense.methodName(ctx)));
        }
        return methodCall(ctx, paddedArgs.toArray(String[]::new));
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
        String result;
        do {
            StringBuilder builder = new StringBuilder();
            for (int i = ctx.getRandom().nextInt(2); i >= 0; i--) {
                builder.append(syllable(ctx));
            }
            result = builder.toString();
        } while(excludedWords.contains(result) || ctx.isIdentifierAlreadyUsed(result));
        ctx.useIdentifier(result);  // never generate the same word twice
        return result;
    }

    public static String syllable(PuzzleContext ctx) {
        return ONSETS.choose(ctx) + NUCLEI.choose(ctx) + CODAS.choose(ctx);
    }

    public static String pluralize(String word) {
        if (word.endsWith("s")) {
            return word + "es";
        } else {
            return word + "s";
        }
    }

    /**
     * Scans the text for the strings "a/an" and "A/An", replacing them with the correct indefinite
     * article for the word that follows. The following word can be wrapped in punctuation, so that
     * for example the string "a/an `Foo`" resolves to "a `Foo`".
     */
    public static String resolveIndefiniteArticles(String text) {
        var matcher = indefiniteArticlePat.matcher(text);
        var result = new StringBuilder();
        while (matcher.find()) {
            var a = matcher.group(1);   // Get these from the match rather than hard-cording "a"
            var an = matcher.group(2);  // and "an", in order to preserve case
            var separator = matcher.group(3);
            var nextWordFirstLetter = matcher.group(4);

            String article;
            var firstLetterWithoutDiacritics =
                Normalizer.normalize(nextWordFirstLetter, Normalizer.Form.NFKD);
            if (vowelPat.matcher(firstLetterWithoutDiacritics).find()) {
                article = an;
            } else {
                article = a;
            }

            matcher.appendReplacement(result, article);
            result.append(separator);
            result.append(nextWordFirstLetter);
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static final Pattern
        indefiniteArticlePat = Pattern.compile(
            "\\b(a)/(an)\\b([\\s\\p{Punct}]*)(\\p{IsAlphabetic})",
            Pattern.CASE_INSENSITIVE),
        vowelPat = Pattern.compile("[aeiou]", Pattern.CASE_INSENSITIVE);

    public enum NameFormat {
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
