package edu.macalester.conceptual.puzzles.ast;

import com.github.javaparser.ast.DataKey;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import edu.macalester.conceptual.ast.AstUtils;
import edu.macalester.conceptual.util.Evaluator;

import static com.github.javaparser.ast.expr.BinaryExpr.Operator.*;
import static edu.macalester.conceptual.ast.AstUtils.*;

public record EvaluationTree(
    Expression expr,
    String exprAsString,
    VariablePool vars
) {
    private static final DataKey<Object> EVALUATION_RESULT = new DataKey<>() { };

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
        for (var expr : allDescendantsOfType(BinaryExpr.class, expr())) {
            if (!expr.getLeft().containsData(EVALUATION_RESULT)) {
                continue;
            }
            var leftValue = expr.getLeft().getData(EVALUATION_RESULT);

            if (
                expr.getOperator() == AND && leftValue.equals("false")
                || expr.getOperator() == OR && leftValue.equals("true")
            ) {
                for (var rightDescendant : allDescendantsOfType(Expression.class, expr.getRight())) {
                    rightDescendant.removeData(EVALUATION_RESULT);
                }
                expr.getRight().setData(EVALUATION_RESULT, "never evaluated");
            }
        }
    }
}
