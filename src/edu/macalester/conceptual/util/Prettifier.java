package edu.macalester.conceptual.util;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;

import java.util.function.Function;

public class Prettifier {
    public static String prettifyStatements(String code) {
        return
            prettify(
                "{" + code + "}",                // Wrap statements in braces
                StaticJavaParser::parseBlock)    // so they parse as one block.
            .replaceAll("^\\{|\\}$", "")         // Then strip outer braces
            .replaceAll("(^|\r?\n)    ", "$1");  // and unindent.
    }

    public static String prettifyExpression(String code) {
        return prettify(code, StaticJavaParser::parseExpression);
    }

    public static String prettifyType(String code) {
        return prettify(code, StaticJavaParser::parseTypeDeclaration);
    }

    public static String prettifyWholeFile(String code) {
        return prettify(code, StaticJavaParser::parse);
    }

    public static String prettifyRaw(String code) {
        return code;
    }

    private static String prettify(String javaCode, Function<String,Node> parser) {
        try {
            return parser.apply(javaCode).toString();
        } catch (ParseProblemException parseError) {
            throw new RuntimeException(
                "\n\nUnable to parse code:\n\n"
                    + javaCode + "\n\n"
                    + parseError.getMessage());
        }
    }
}
