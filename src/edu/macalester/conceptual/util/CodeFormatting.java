package edu.macalester.conceptual.util;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.google.common.collect.Streams;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static edu.macalester.conceptual.util.AstUtils.*;

/**
 * A variety of helper methods for working with code in string form.
 * <p>
 * Note that all the prettify methods require code that is <b>syntactically correct</b>: it does not
 * need to be well-typed, refer to variables that actually exist, etc., but it does need to parse.
 * <p>
 * Note that the {@link #prettify(Node)} method to <i>format</i> an AST node is also in this class.
 * For help <i>constructing</i> JavaPaser ASTs, see {@link AstUtils}.
 */
public enum CodeFormatting {
    ; // static utility class; no cases

    /**
     * A Java comment that transforms into “...” when passed through the various prettify methods.
     * This allows you to insert it into code without stopping the code from parsing, but still
     * have it show up as an ellipsis in the output:
     * <code>prettifyExpression("foo(" + ELIDED + ")")</code> → <code>foo(...)</code>
     */
    public static final String ELIDED = "/* <<ELIDED>> */";

    /**
     * Joins the given strings with spaces, discarding nulls. Useful for constructing expressions.
     */
    public static String joinCode(String... parts) {
        return joinCode(Arrays.asList(parts));
    }

    /**
     * Joins the given strings with spaces, discarding nulls. Useful for constructing expressions.
     */
    public static String joinCode(List<String> parts) {
        return String.join(" ", removeNulls(parts));
    }

    /**
     * Concatenates the given strings, adding trailing semicolons.
     */
    public static String joinStatements(String... parts) {
        return joinStatements(Arrays.asList(parts));
    }

    /**
     * Concatenates the given strings, adding trailing semicolons.
     */
    public static String joinStatements(List<String> parts) {
        return String.join(";", removeNulls(parts)) + ";";
    }

    private static Iterable<String> removeNulls(Iterable<String> parts) {
        return Streams.stream(parts)
            .filter(Objects::nonNull)
            .toList();
    }

    /**
     * Reformats one or more complete Java statements. Here “statement” means anything from one line
     * terminated by a semicolon to loops and conditionals with nested code blocks.
     */
    public static String prettifyStatements(String code) {
        return
            prettify(
                "{" + code + "}",                // Wrap statements in braces
                StaticJavaParser::parseBlock)    // so they parse as one block.
            .replaceAll("^\\{|\\}$", "")         // Then strip outer braces
            .replaceAll("(^|\r?\n) {4}", "$1");  // and unindent.
    }

    /**
     * Reformats a single Java expression (no semicolon!).
     */
    public static String prettifyExpression(String code) {
        return prettify(code, StaticJavaParser::parseExpression);
    }

    /**
     * Reformats the declaration for a single Java method, including the body.
     */
    public static String prettifyMethodDecl(String code) {
        return prettify(code, StaticJavaParser::parseMethodDeclaration);
    }

    /**
     * Reformats the declaration for a whole Java class, interface, enum, or record.
     */
    public static String prettifyTypeDecl(String code) {
        return prettify(code, StaticJavaParser::parseTypeDeclaration);
    }

    /**
     * Reformats an entire Java file.
     */
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

    /**
     * Turns the given AST node into a well-formatted string.
     */
    public static String prettify(Node node) {
        return withParensAsNeeded(node)
            .toString()
            .replace(ELIDED, "...");
    }
}
