package edu.macalester.conceptual.puzzles.loops;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UnaryExpr;

import java.text.MessageFormat;

import edu.macalester.conceptual.context.PuzzlePrinter;

import static edu.macalester.conceptual.util.CodeFormatting.joinCode;
import static edu.macalester.conceptual.util.CodeFormatting.prettifyStatements;

enum LoopForm {
    WHILE {
        public String description() {
            return "while loop";
        }

        public void print(SimpleLoop loop, PuzzlePrinter output) {
            output.codeBlock(
                prettifyStatements(
                    joinCode(
                        loop.getVarDeclaration(), ";",
                        "while(", loop.getEndCondition(), ") {",
                            loop.getBody(),
                            loop.getNextStep(), ";",
                        "}")));
        }
    },

    FOR {
        public String description() {
            return "for loop";
        }

        public void print(SimpleLoop loop, PuzzlePrinter output) {
            output.codeBlock(
                prettifyStatements(
                    joinCode(
                        "for(",
                            loop.getVarDeclaration(), ";",
                            loop.getEndCondition(), ";",
                            loop.getNextStep(),
                        ") {",
                            loop.getBody(),
                        "}")));
        }
    },

    NATURAL_LANGUAGE {
        public String description() {
            return "natural language description of a loop";
        }

        public void print(SimpleLoop loop, PuzzlePrinter output) {
            output.blockquote(
                "Declare a variable named `"
                    + loop.getVarName()
                    + "` of type `"
                    + loop.getVarType()
                    + "`, initialized to `"
                    + loop.getInitializer()
                    + "`.\nThen, until `"
                    + loop.getVarName()
                    + "` "
                    + describeConditionNegation(loop.getEndCondition())
                    + ", "
                    + describeStep(loop.getNextStep())
                    + ".");
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
                return operator + " `" + binary.getRight() + "`";
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
                return operator + " `" + unary.getExpression() + "`";
            } else if (step instanceof AssignExpr assign) {
                return MessageFormat.format(
                    switch(assign.getOperator()) {
                        case PLUS     -> "add `{1}` to `{0}`";
                        case MINUS    -> "subtract `{1}` from `{0}`";
                        case MULTIPLY -> "multiply `{0}` by `{1}`";
                        case DIVIDE   -> "divide `{0}` by `{1}`";
                        default -> throw unsupported(step, "nextStep", "operator");
                    },
                    assign.getTarget(),
                    assign.getValue());
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

    public abstract void print(SimpleLoop loop, PuzzlePrinter output);
}
