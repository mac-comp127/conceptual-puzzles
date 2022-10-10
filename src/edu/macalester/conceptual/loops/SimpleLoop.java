package edu.macalester.conceptual.loops;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.PrimitiveType;

import edu.macalester.conceptual.helper.Nonsense;
import edu.macalester.conceptual.helper.PuzzleContext;

public class SimpleLoop {
    private final VariableDeclarator loopVariable;
    private final Expression endCondition;
    private final Expression nextStep;
    private final Statement body;

    public static SimpleLoop numericLoop(PuzzleContext ctx) {
        VariableDeclarator loopVariable = Nonsense.variable(ctx, PrimitiveType.intType());
        int min = ctx.getRandom().nextInt(1000);
        int max = ctx.getRandom().nextInt(1000);
        loopVariable.setInitializer(new IntegerLiteralExpr(String.valueOf(min)));

        Expression endCondition =
            new BinaryExpr(
                new NameExpr(loopVariable.getNameAsString()),
                new IntegerLiteralExpr(String.valueOf(max)),
                (min < max)
                    ? ctx.choose(Operator.LESS, Operator.LESS_EQUALS)
                    : ctx.choose(Operator.GREATER, Operator.GREATER_EQUALS));

        Expression nextStep =
            new UnaryExpr(
                new NameExpr(loopVariable.getNameAsString()),
                (min < max)
                    ? UnaryExpr.Operator.POSTFIX_INCREMENT
                    : UnaryExpr.Operator.POSTFIX_DECREMENT);

        return new SimpleLoop(loopVariable, endCondition, nextStep, arbitraryBody(ctx, loopVariable));
    }

    private static Statement arbitraryBody(PuzzleContext ctx, VariableDeclarator loopVariable) {
        return new ExpressionStmt(
            new MethodCallExpr(
                null,
                Nonsense.methodName(ctx),
                new NodeList<>(new NameExpr(loopVariable.getNameAsString()))));
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

    enum Form {
        WHILE {
            public NodeList<Statement> format(SimpleLoop loop) {
                return new NodeList<>(
                    new ExpressionStmt(
                        new VariableDeclarationExpr(loop.loopVariable)),
                    new WhileStmt(
                        loop.endCondition,
                        new BlockStmt(new NodeList<>(
                            loop.body,
                            new ExpressionStmt(
                                loop.nextStep)))));
            }
        },

        FOR {
            public NodeList<Statement> format(SimpleLoop loop) {
                return new NodeList<>(
                    new ForStmt(
                        new NodeList<>(
                            new VariableDeclarationExpr(loop.loopVariable)),
                        loop.endCondition,
                        new NodeList<>(
                            loop.nextStep),
                        new BlockStmt(new NodeList<>(
                            loop.body))));
            }
        };

        public abstract NodeList<Statement> format(SimpleLoop loop);
    }

    public static void main(String[] args) {
        PuzzleContext ctx = PuzzleContext.generate();
        var loop = numericLoop(ctx);
        System.out.println("–––––––––––––––––––––––––––––––––––");
        System.out.println(new BlockStmt(Form.WHILE.format(loop)));
        System.out.println("–––––––––––––––––––––––––––––––––––");
        System.out.println(new BlockStmt(Form.FOR.format(loop)));
        System.out.println("–––––––––––––––––––––––––––––––––––");
    }
}
