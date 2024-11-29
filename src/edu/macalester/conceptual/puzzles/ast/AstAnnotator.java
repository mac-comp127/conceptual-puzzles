package edu.macalester.conceptual.puzzles.ast;

import com.github.javaparser.ast.DataKey;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.macalester.conceptual.util.AstUtils;
import edu.macalester.conceptual.util.Evaluator;
import edu.macalester.conceptual.util.VariablePool;

import static com.github.javaparser.ast.expr.BinaryExpr.Operator.*;
import static edu.macalester.conceptual.util.AstUtils.*;

/**
 * Computes all the data necessary to draw an AST visualization with attached evaluation results.
 */
public record AstAnnotator(
    Expression expr,
    String exprAsString,
    VariablePool vars
) {
    private static final DataKey<Object> DIAGRAM_ANNOTATION = new DataKey<>() { };
    private static final Object SHORT_CIRCUITED_RESULT = new Object() {
        @Override
        public String toString() {
            return "(never evaluated)";
        }
    };

    public List<Expression> subexprs() {
        return allDescendantsOfType(Expression.class, expr());
    }

    private Stream<String> codeForSubexprs() {
        return subexprs().stream()
            .map(AstUtils::withParensAsNeeded)
            .map(Object::toString);
    }

    /**
     * Retrieves an evaluation annotation previously attached to an AST node with
     * {@link #attachValueAnnotations()}.
     */
    public static Optional<Object> valueOf(Expression expr) {
        return expr.containsData(DIAGRAM_ANNOTATION)
            ? Optional.of(expr.getData(DIAGRAM_ANNOTATION))
            : Optional.empty();
    }

    /**
     * Runs the code and attaches the evaluation result to all the subexpressions of the AST.
     */
    public void attachValueAnnotations() {
        attachAnnotationsFromEvaluation(Function.identity());
    }

    /**
     * Runs the code and attaches the type of evaluation result to all subexpressions.
     */
    public void attachRuntimeTypeAnnotations() {
        attachAnnotationsFromEvaluation(Object::getClass);
    }

    private void attachAnnotationsFromEvaluation(Function<Object, Object> valueTransform) {
        List<?> evaluationResults =
            new Evaluator<>(
                "",
                "",
                vars().allDeclarations()
                    + "return java.util.List.of(\n"
                    + codeForSubexprs()
                        .collect(Collectors.joining(",\n"))
                    + ");",
                List.class,
                ""
            ).evaluate();
        attachAnnotations(evaluationResults.stream().map(valueTransform).toList());
    }

    /**
     * Parses the code and attaches evaluation results to all the subexpressions of the AST.
     */
    public void attachStaticTypeAnnotations() {
        var evaluationResults =
            new Evaluator<>(
                "",
                "",
                vars().allDeclarations()
                    + codeForSubexprs()
                        .map(expr -> "staticType(" + expr + ");")
                        .collect(Collectors.joining("\n")),
                List.class,
                ""
            ).analyzeStaticTypes();
        attachAnnotations(evaluationResults);
    }

    private void attachAnnotations(List<?> annotations) {
        var exprs = subexprs();
        if (exprs.size() != annotations.size()) {
            throw new IllegalArgumentException(
                "There are " + exprs.size() + " total subexpressions,"
                + " but attachAnnotations() received " + annotations.size() + " annotations");
        }
        for (int i = 0; i < exprs.size(); i++) {
            exprs.get(i).setData(DIAGRAM_ANNOTATION, annotations.get(i));
        }
    }

    /**
     * Removed existing annotations for any expression nodes that would never be evaluated due to
     * boolean operator short-circuiting.
     */
    public void showShortCircuiting() {
        for (var subexpr : allDescendantsOfType(BinaryExpr.class, expr())) {
            if (!subexpr.getLeft().containsData(DIAGRAM_ANNOTATION)) {
                continue;
            }
            var leftValue = subexpr.getLeft().getData(DIAGRAM_ANNOTATION);

            if (
                subexpr.getOperator() == AND && Boolean.FALSE.equals(leftValue)
                || subexpr.getOperator() == OR && Boolean.TRUE.equals(leftValue)
            ) {
                // parens not shown in tree, so label child “never evaluated”
                var rhs = subexpr.getRight();
                while (rhs instanceof EnclosedExpr rhsInParens) {
                    rhs = rhsInParens.getInner();
                }

                // unlabel all descendants
                for (var rightDescendant : allDescendantsOfType(Expression.class, rhs)) {
                    rightDescendant.removeData(DIAGRAM_ANNOTATION);
                }

                rhs.setData(DIAGRAM_ANNOTATION, SHORT_CIRCUITED_RESULT);
            }
        }
    }
}
