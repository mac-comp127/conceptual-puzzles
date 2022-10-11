package edu.macalester.conceptual.puzzles.loops;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.WhileStmt;

import static edu.macalester.conceptual.ast.AstUtils.*;

enum LoopForm {
    WHILE {
        public String format(SimpleLoop loop) {
            return nodesToString(
                variableDeclarationStmt(loop.getLoopVariable()),
                new WhileStmt(
                    loop.getEndCondition(),
                    blockOf(
                        loop.getBody(),
                        new ExpressionStmt(
                            loop.getNextStep()))));
        }
    },

    FOR {
        public String format(SimpleLoop loop) {
            return nodesToString(
                new ForStmt(
                    nodes(new VariableDeclarationExpr(
                        loop.getLoopVariable())),
                    loop.getEndCondition(),
                    nodes(
                        loop.getNextStep()),
                    blockOf(
                        loop.getBody())));
        }
    },

    ENGLISH {
        public String format(SimpleLoop loop) {
            return "Declare a variable named `"
                + loop.getLoopVariable().getNameAsString()
                + "` of type "
                + loop.getLoopVariable().getTypeAsString()
                + ", initialized to "
                + loop.getLoopVariable().getInitializer().orElseThrow()
                + ". Then, until "
                + loop.getLoopVariable().getNameAsString()
                + " "
                + describeConditionNegation(loop.getEndCondition())
                + ", "
                + describeStep(loop.getNextStep())
                + ".";
        }

        private String describeConditionNegation(Expression cond) {
            if (cond instanceof BinaryExpr binary) {
                String operator = switch (binary.getOperator()) {
                    case LESS -> "is greater than or equal to";
                    case LESS_EQUALS -> "is greater than";
                    case GREATER -> "is less than or equal to";
                    case GREATER_EQUALS -> "is less than";
                    case NOT_EQUALS -> "equals";
                    default -> throw unsupported(cond, "endCondition", "operator");
                };
                return operator + " " + binary.getRight();
            } else {
                throw unsupported(cond, "endCondition", "structure");
            }
        }

        private String describeStep(Expression step) {
            if (step instanceof UnaryExpr unary) {
                String operator = switch(unary.getOperator()) {
                    case POSTFIX_INCREMENT -> "increment";
                    case POSTFIX_DECREMENT -> "decrement";
                    default -> throw unsupported(step, "nextStep", "operator");
                };
                return operator + " " + unary.getExpression();
            } else if (step instanceof AssignExpr assign) {
                return switch(assign.getOperator()) {
                    case PLUS     -> "add "      + assign.getValue()  + " to "   + assign.getTarget();
                    case MINUS    -> "subtract " + assign.getValue()  + " from " + assign.getTarget();
                    case MULTIPLY -> "multiply " + assign.getTarget() + " by "   + assign.getValue();
                    case DIVIDE   -> "divide "   + assign.getTarget() + " by "   + assign.getValue();
                    default -> throw unsupported(step, "nextStep", "operator");
                };
            } else {
                throw unsupported(step, "nextStep", "structure");
            }
        }

        private RuntimeException unsupported(Node node, String part, String subpart) {
            return new UnsupportedOperationException(
                "Cannot describe " + subpart + " of " + part + " node in English: " + node);
        }
    };

    public abstract String format(SimpleLoop loop);
}
