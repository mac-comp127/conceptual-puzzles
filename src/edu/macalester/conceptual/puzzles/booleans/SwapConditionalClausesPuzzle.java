package edu.macalester.conceptual.puzzles.booleans;

import com.github.javaparser.ast.stmt.IfStmt;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.Nonsense;

import static com.github.javaparser.StaticJavaParser.parseBlock;
import static edu.macalester.conceptual.ast.AstUtils.*;
import static edu.macalester.conceptual.puzzles.booleans.Generator.generateBooleanExpr;
import static edu.macalester.conceptual.util.CodeFormatting.ELIDED;

class SwapConditionalClausesPuzzle {
    static void generate(PuzzleContext ctx) {
        ctx.output().paragraph(
            """
            This if statement has a very long first clause, and a very short else clause. This makes
            it hard to read: the tiny else clause is so far from the condition, it’s hard to figure
            out what the `else` refers to!
            """);
        var ifStmt = new IfStmt(
            generateBooleanExpr(ctx, 3, true),
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
            Improve readability by refactoring this conditional so that what is now the else clause
            comes first, and the first clause comes second.
            """);
        ctx.solution(() -> {
            ctx.output().codeBlock(
                new IfStmt(
                    negated(ifStmt.getCondition()),
                    ifStmt.getElseStmt().orElseThrow(),
                    ifStmt.getThenStmt()));
        });
    }
}
