package edu.macalester.conceptual.util;

import java.io.PrintStream;
import java.util.List;
import java.util.function.Supplier;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import org.joor.Reflect;

/**
 * A utility to dynamically compile and evaluate Java code, or extract its static types.
 */
public enum Evaluator {
    ;  // static methods only

    public static <T> T evaluate(CodeSnippet<T> snippet) {
        var code = snippet.generateCode("DynamicCode");
        try {
            Supplier<T> evaluator = Reflect.compile("DynamicCode", code)
                .create().get();
            return evaluator.get();
        } catch(RuntimeException e) {
            throw new EvaluationException(e, code);
        }
    }

    public static String captureOutput(CodeSnippet<?> snippet) {
        return Evaluator.evaluate(
            snippet
                .withImports(snippet.imports() +
                    """
                    import java.io.PrintWriter;
                    import java.io.StringWriter;
                    """
                )
                .withClassMembers(snippet.classMembers() +
                    """
                    private static StringWriter capturedOutput = new StringWriter();
                    public static PrintWriter out = new PrintWriter(capturedOutput);
                    """
                )
                .withMainBody(
                    snippet.mainBody().replace("System.out", "DynamicCode.out") +
                        "return capturedOutput.toString();"
                )
                .withReturnType(String.class)
                .withOtherClasses(
                    snippet.otherClasses().replace("System.out", "DynamicCode.out")
                )
        );
    }

    public static List<?> analyzeStaticTypes(CodeSnippet<?> snippet) {
        var code = snippet
            .withClassMembers(snippet.classMembers() +
                """
                private <T> staticType(T val) {  // We'll search for calls to this method after parsing
                    return val;
                }
                """
            )
            .generateCode("DynamicCode");

        var parserConfig = new ParserConfiguration();
        parserConfig.setSymbolResolver(
            new JavaSymbolSolver(
                new ReflectionTypeSolver()));

        var parseResult = new JavaParser(parserConfig).parse(code);
        if (!parseResult.isSuccessful()) {
            throw new EvaluationException(
                new ParseProblemException(parseResult.getProblems()), code);
        }

        return parseResult
            .getResult().orElseThrow()
            .findAll(
                MethodCallExpr.class,
                methodCall -> methodCall.getName().asString().equals("staticType")
            )
            .stream()
            .map(call -> call.getArgument(0).calculateResolvedType())
            .toList();
    }

    public static class EvaluationException extends RuntimeException {
        private final String javaSource;

        public <T> EvaluationException(Exception cause, String javaSource) {
            super(cause);
            this.javaSource = javaSource;
        }

        public EvaluationException(Exception cause) {
            super(cause);
            this.javaSource = null;
        }

        public EvaluationException(String message) {
            super(message);
            this.javaSource = null;
        }

        @Override
        public void printStackTrace(PrintStream s) {
            s.println("Error while evaluating expression. Expression wrapper:");
            s.println();
            s.println(javaSource);
            super.printStackTrace(s);
        }
    }
}
