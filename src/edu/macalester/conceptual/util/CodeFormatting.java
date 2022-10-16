package edu.macalester.conceptual.util;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.google.common.collect.Streams;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import edu.macalester.conceptual.context.PuzzleContext;

import static edu.macalester.conceptual.ast.AstUtils.*;

public enum CodeFormatting {
    ; // static utility class; no cases

    public static final String ELIDED = "/* <<ELIDED>> */";

    public static String joinCode(String... parts) {
        return joinCode(Arrays.asList(parts));
    }

    public static String joinCode(List<String> parts) {
        return String.join(" ", removeNulls(parts));
    }

    public static String joinStatements(String... parts) {
        return joinStatements(Arrays.asList(parts));
    }

    public static String joinStatements(List<String> parts) {
        return String.join(";", removeNulls(parts)) + ";";
    }

    private static Iterable<String> removeNulls(Iterable<String> parts) {
        return Streams.stream(parts)
            .filter(Objects::nonNull)
            .toList();
    }

    public static String prettifyStatements(String code) {
        return
            prettify(
                "{" + code + "}",                // Wrap statements in braces
                StaticJavaParser::parseBlock)    // so they parse as one block.
            .replaceAll("^\\{|\\}$", "")         // Then strip outer braces
            .replaceAll("(^|\r?\n) {4}", "$1");  // and unindent.
    }

    public static String prettifyExpression(String code) {
        return prettify(code, StaticJavaParser::parseExpression);
    }

    public static String prettifyMethodDecl(String code) {
        return prettify(code, StaticJavaParser::parseMethodDeclaration);
    }


    public static String prettifyTypeDecl(String code) {
        return prettify(code, StaticJavaParser::parseTypeDeclaration);
    }

    public static String prettifyWholeFile(String code) {
        return prettify(code, StaticJavaParser::parse);
    }

    private static String prettify(String javaCode, Function<String, Node> parser) {
        try {
            return prettify(
                parser.apply(javaCode));
        } catch (ParseProblemException parseError) {
            throw new RuntimeException(
                "\n\nUnable to parse code:\n\n"
                    + javaCode + "\n\n"
                    + parseError.getMessage());
        }
    }

    public static String prettify(Node node) {
        return withParensAsNeeded(node)
            .toString()
            .replace(ELIDED, "...");
    }


    public static <Elem> void insertAtRandomPosition(
        PuzzleContext ctx,
        List<Elem> mutableList,
        Elem elem
    ) {
        mutableList.add(ctx.getRandom().nextInt(mutableList.size() + 1), elem);
    }
}
