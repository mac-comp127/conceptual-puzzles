package edu.macalester.conceptual.puzzles.loops;

import com.github.javaparser.StaticJavaParser;
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
import static edu.macalester.conceptual.util.CodeFormatting.joinCode;
import static edu.macalester.conceptual.util.Prettifier.prettifyStatements;

enum LoopForm {
    WHILE {
        public String description() {
            return "while loop";
        }

        public String format(SimpleLoop loop) {
            return prettifyStatements(
                joinCode(
                    loop.getVarDeclaration(), ";",
                    "while(", loop.getEndCondition(), ") {",
                        loop.getBody(),
                        loop.getNextStep(), ";",
                    "}"));
        }
    },

    FOR {
        public String description() {
            return "for loop";
        }

        public String format(SimpleLoop loop) {
            return prettifyStatements(
                joinCode(
                    "for(",
                        loop.getVarDeclaration(), ";",
                        loop.getEndCondition(), ";",
                        loop.getNextStep(),
                    ") {",
                        loop.getBody(),
                    "}"));
        }
    },

    NATURAL_LANGUAGE {
        public String description() {
            return "natural language description of a loop";
        }

        public String format(SimpleLoop loop) {
            return "Declare a variable named `"
                + loop.getVarName()
                + "` of type "
                + loop.getVarType()
                + ", initialized to "
                + loop.getInitializer()
                + ".\nThen, until "
                + loop.getVarName()
                + " "
                + describeConditionNegation(loop.getEndCondition())
                + ", "
                + describeStep(loop.getNextStep())
                + ".";
        }

        private String describeConditionNegation(String condStr) {
            Expression cond = StaticJavaParser.parseExpression(condStr);
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

        private String describeStep(String stepStr) {
            Expression step = StaticJavaParser.parseExpression(stepStr);
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

    public abstract String description();

    public abstract String format(SimpleLoop loop);
}
