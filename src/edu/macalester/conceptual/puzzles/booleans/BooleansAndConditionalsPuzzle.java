package edu.macalester.conceptual.puzzles.booleans;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.IfStmt;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.Nonsense;

import static com.github.javaparser.StaticJavaParser.parseBlock;
import static com.github.javaparser.StaticJavaParser.parseStatement;
import static edu.macalester.conceptual.ast.AstUtils.*;
import static edu.macalester.conceptual.puzzles.booleans.Generator.generateBooleanExpr;
import static edu.macalester.conceptual.util.CodeFormatting.ELIDED;

public class BooleansAndConditionalsPuzzle implements Puzzle {
    @Override
    public byte id() {
        return 1;
    }

    @Override
    public String name() {
        return "bool";
    }

    @Override
    public String description() {
        return "Booleans and conditionals";
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void generate(PuzzleContext ctx) {
        ctx.section(() -> {
            ctx.output().paragraph(
                """
                This if statement has a very long first clause, and a very short else clause. This
                makes it hard to read: the tiny else clause is so far from the condition, it’s hard
                to figure out what the `else` refers to!
                """);
            var ifStmt = new IfStmt(
                generateBooleanExpr(ctx, 3),
                parseBlock(
                    "{"
                        + ELIDED
                        + "\n// Lots of code here\n"
                        + ELIDED
                    + "}"),
                parseBlock(
                    "{" + Nonsense.methodName(ctx) + "();}"));
            ctx.output().codeBlock(ifStmt);
            ctx.output().paragraph(
                """
                Improve readability by refactoring this conditional so that what is now the else
                clause comes first, and the first clause comes second.
                """);
            ctx.solution(() -> {
                ctx.output().codeBlock(
                    new IfStmt(
                        negated(ifStmt.getCondition()),
                        ifStmt.getElseStmt().get(),
                        ifStmt.getThenStmt()));
            });
        });
    }
}
