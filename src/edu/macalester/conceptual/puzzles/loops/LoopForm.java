package edu.macalester.conceptual.puzzles.loops;

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
            StringBuilder result = new StringBuilder();
            result.append("Start the ");
            result.append(loop.getLoopVariable().getTypeAsString());
            result.append(" variable named `");
            result.append(loop.getLoopVariable().getNameAsString());
            result.append("` at ");
            result.append(loop.getLoopVariable().getInitializer().orElseThrow());
            result.append(", and then until ");
            result.append("<not>");
            result.append(loop.getEndCondition().toString());
            result.append(loop.getNextStep().toString());
            return result.toString();
        }
    };

    public abstract String format(SimpleLoop loop);
}
