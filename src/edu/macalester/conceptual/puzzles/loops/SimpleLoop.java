package edu.macalester.conceptual.puzzles.loops;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.PrimitiveType;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.Nonsense;

import static edu.macalester.conceptual.ast.AstUtils.*;

public class SimpleLoop {
    private final VariableDeclarator loopVariable;
    private final Expression endCondition;
    private final Expression nextStep;
    private final Statement body;

    public static SimpleLoop generateNumericLoop(PuzzleContext ctx) {
        VariableDeclarator loopVariable = Nonsense.variable(ctx, PrimitiveType.intType());

        loopVariable.setInitializer(
            ctx.getRandom().nextBoolean()
                ? Nonsense.variableNameExpr(ctx)
                : intLiteral(ctx.getRandom().nextInt(100)));
        boolean growing = ctx.getRandom().nextBoolean();

        Expression endCondition =
            new BinaryExpr(
                loopVariable.getNameAsExpression(),
                Nonsense.propertyNameExpr(ctx),
                growing
                    ? ctx.choose(Operator.LESS, Operator.LESS_EQUALS)
                    : ctx.choose(Operator.GREATER, Operator.GREATER_EQUALS, Operator.NOT_EQUALS));

        Expression nextStep =
            ctx.getRandom().nextBoolean()
                ? new UnaryExpr(
                    loopVariable.getNameAsExpression(),
                    growing
                        ? UnaryExpr.Operator.POSTFIX_INCREMENT
                        : UnaryExpr.Operator.POSTFIX_DECREMENT)
                : new AssignExpr(
                    loopVariable.getNameAsExpression(),
                    intLiteral(ctx.getRandom().nextInt(2, 5)),
                    growing
                        ? ctx.choose(AssignExpr.Operator.PLUS, AssignExpr.Operator.MULTIPLY)
                        : ctx.choose(AssignExpr.Operator.MINUS, AssignExpr.Operator.DIVIDE));

        Statement body =
            new ExpressionStmt(
                Nonsense.methodCallExpr(ctx, loopVariable.getNameAsExpression()));

        return new SimpleLoop(loopVariable, endCondition, nextStep, body);
    }

    private SimpleLoop(
        VariableDeclarator loopVariable,
        Expression endCondition,
        Expression nextStep,
        Statement body
    ) {
        this.loopVariable = loopVariable;
        this.endCondition = endCondition;
        this.nextStep = nextStep;
        this.body = body;
    }

    public VariableDeclarator getLoopVariable() {
        return loopVariable;
    }

    public Expression getEndCondition() {
        return endCondition;
    }

    public Expression getNextStep() {
        return nextStep;
    }

    public Statement getBody() {
        return body;
    }
}
