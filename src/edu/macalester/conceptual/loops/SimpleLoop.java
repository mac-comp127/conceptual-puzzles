package edu.macalester.conceptual.loops;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
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

        loopVariable.setInitializer(
            ctx.getRandom().nextBoolean()
                ? new NameExpr(Nonsense.variableName(ctx))
                : new IntegerLiteralExpr(String.valueOf(ctx.getRandom().nextInt(100))));
        boolean growing = ctx.getRandom().nextBoolean();

        Expression endCondition =
            new BinaryExpr(
                new NameExpr(loopVariable.getNameAsString()),
                new NameExpr(Nonsense.propertyName(ctx)),
                growing
                    ? ctx.choose(Operator.LESS, Operator.LESS_EQUALS)
                    : ctx.choose(Operator.GREATER, Operator.GREATER_EQUALS, Operator.NOT_EQUALS));

        Expression nextStep =
            ctx.getRandom().nextBoolean()
                ? new UnaryExpr(
                    new NameExpr(loopVariable.getNameAsString()),
                    growing
                        ? UnaryExpr.Operator.POSTFIX_INCREMENT
                        : UnaryExpr.Operator.POSTFIX_DECREMENT)
                : new AssignExpr(
                    new NameExpr(loopVariable.getNameAsString()),
                    new IntegerLiteralExpr(String.valueOf(ctx.getRandom().nextInt(2, 5))),
                    growing
                        ? ctx.choose(AssignExpr.Operator.PLUS, AssignExpr.Operator.MULTIPLY)
                        : ctx.choose(AssignExpr.Operator.MINUS, AssignExpr.Operator.DIVIDE));

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

    public static void main(String[] args) throws Exception {
        PuzzleContext ctx = PuzzleContext.generate();
        for(int i = 0; i < 10; i++) {
            var loop = numericLoop(ctx);
            System.out.println("–––––––––––––––––––––––––––––––––––");
            System.out.println(new BlockStmt(Form.WHILE.format(loop)));
            System.out.println("–––––––––––––––––––––––––––––––––––");
            System.out.println(new BlockStmt(Form.FOR.format(loop)));
            System.out.println("–––––––––––––––––––––––––––––––––––");
            System.out.println(ctx.getPuzzleCode());
        }
    }
}
