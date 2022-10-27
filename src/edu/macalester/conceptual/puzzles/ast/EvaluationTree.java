package edu.macalester.conceptual.puzzles.ast;

import com.github.javaparser.ast.DataKey;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import edu.macalester.conceptual.ast.AstUtils;
import edu.macalester.conceptual.util.Evaluator;
import edu.macalester.conceptual.util.VariablePool;

import static com.github.javaparser.ast.expr.BinaryExpr.Operator.*;
import static edu.macalester.conceptual.ast.AstUtils.*;

/**
 * All the data necessary to draw an AST visualization with attached evaluation results.
 */
public record EvaluationTree(
    Expression expr,
    String exprAsString,
    VariablePool vars
) {
    private static final DataKey<Object> EVALUATION_RESULT = new DataKey<>() { };
    private static final Object SHORT_CIRCUITED_RESULT = new Object() {
        @Override
        public String toString() {
            return "(never evaluated)";
        }
    };

    public List<Expression> subexprs() {
        return allDescendantsOfType(Expression.class, expr());
    }

    public static Optional<Object> valueOf(Expression expr) {
        return expr.containsData(EVALUATION_RESULT)
            ? Optional.of(expr.getData(EVALUATION_RESULT))
            : Optional.empty();
    }

    public void attachEvaluationResults() {
        List<Expression> allExprs = subexprs();
        var evaluationResults =
            Evaluator.evaluate(
                List.class,
                vars(),
                "java.util.List.of(\n"
                    + allExprs.stream()
                        .map(AstUtils::withParensAsNeeded)
                        .map(Object::toString)
                        .collect(Collectors.joining(",\n"))
                + ")");
        for (int i = 0; i < allExprs.size(); i++) {
            allExprs.get(i).setData(EVALUATION_RESULT, evaluationResults.get(i));
        }
    }

    public void showShortCircuiting() {
        for (var subexpr : allDescendantsOfType(BinaryExpr.class, expr())) {
            if (!subexpr.getLeft().containsData(EVALUATION_RESULT)) {
                continue;
            }
            var leftValue = subexpr.getLeft().getData(EVALUATION_RESULT);

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
                    rightDescendant.removeData(EVALUATION_RESULT);
                }

                rhs.setData(EVALUATION_RESULT, SHORT_CIRCUITED_RESULT);
            }
        }
    }
}
