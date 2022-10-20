package edu.macalester.conceptual.puzzles.ast;

import org.joor.Reflect;

import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Evaluator {
    public static <T> T evaluate(Class<T> type, String javaExpression, String varDeclarations) {
        var javaSource = String.format(
            """
            public class ExpressionWrapper implements java.util.function.Supplier<%1$s> {
                public %1$s get() {
                    %2$s
                    return %3$s;
                }
            }
            """,
            type.getName(),
            varDeclarations,
            javaExpression);

        try {
            Supplier<T> evaluator = Reflect.compile("ExpressionWrapper", javaSource).create().get();
            return evaluator.get();
        } catch(RuntimeException e) {
            System.err.println("Error while evaluating expression. Expression wrapper:");
            System.err.println();
            System.err.println(javaSource);
            throw e;
        }
    }
}
