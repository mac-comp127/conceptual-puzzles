package edu.macalester.conceptual.util;

import org.joor.Reflect;

import java.io.PrintStream;
import java.util.function.Supplier;

/**
 * A utility to dynamically compile and evaluate a Java expression.
 */
public class Evaluator {
    public static <T> T evaluate(Class<T> resultType, VariablePool vars, String javaExpression) {
        var javaSource = String.format(
            """
            public class ExpressionWrapper implements java.util.function.Supplier<%1$s> {
                public %1$s get() {
                    %2$s
                    return %3$s;
                }
            }
            """,
            resultType.getName(),
            vars.allDeclarations(),
            javaExpression);

        try {
            Supplier<T> evaluator = Reflect.compile("ExpressionWrapper", javaSource).create().get();
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
