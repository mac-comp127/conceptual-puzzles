package edu.macalester.conceptual.util;

import java.io.PrintStream;
import java.util.function.Supplier;

import org.joor.Reflect;

/**
 * A utility to dynamically compile and evaluate a Java expression.
 */
public class Evaluator {
    public static <T> T evaluate(Class<T> resultType, VariablePool vars, String javaExpression) {
        var javaSource = String.format(
            """
            public class DynamicCode implements java.util.function.Supplier<%1$s> {
                public %1$s get() {
                    %2$s
                    return %3$s;
                }
            }
            """,
            resultType.getName(),
            vars.allDeclarations(),
            javaExpression);

        return run(javaSource);
    }

    public static String captureOutput(String classDecls, String entryPoint) {
        return captureOutput("", classDecls, entryPoint);
    }

    public static String captureOutput(String imports, String classDecls, String entryPoint) {
        var javaSource =
            String.format(
                """
                import java.io.PrintWriter;
                import java.io.StringWriter;
                %s

                public class DynamicCode implements java.util.function.Supplier<String> {
                    private static StringWriter capturedOutput = new StringWriter();
                    public static PrintWriter out = new PrintWriter(capturedOutput);
                
                    public String get() {
                        %s;
                        return capturedOutput.toString();
                    }
                }
                
                %s
                """,
                imports,
                entryPoint,
                classDecls.replaceAll("public\\s+(class|interface|enum|record)", "$1")
            ).replace("System.out", "DynamicCode.out");

        return run(javaSource);
    }

    private static <T> T run(String javaSource) {
        try {
            Supplier<T> evaluator = Reflect.compile("DynamicCode", javaSource).create().get();
            return evaluator.get();
        } catch(RuntimeException e) {
            throw new EvaluationException(e, javaSource);
        }
    }

    public static class EvaluationException extends RuntimeException {
        private final String javaSource;

        public EvaluationException(Exception cause, String javaSource) {
            super(cause);
            this.javaSource = javaSource;
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
