package edu.macalester.conceptual.puzzles.booleans;

import com.github.javaparser.ast.stmt.IfStmt;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.Nonsense;

import static com.github.javaparser.StaticJavaParser.parseBlock;
import static edu.macalester.conceptual.puzzles.booleans.Generator.generateBooleanExpr;
import static edu.macalester.conceptual.util.AstUtils.negated;
import static edu.macalester.conceptual.util.CodeFormatting.*;

class SwapConditionalClausesPuzzle {
    static void generate(PuzzleContext ctx) {
        ctx.output().paragraph(
            """
            This if statement has a very long first clause, and a very short else clause. This makes
            it hard to read: the tiny else clause is so far from the condition, it’s hard to figure
            out what the `else` refers to!
            """);
        var ifStmt = new IfStmt(
            generateBooleanExpr(ctx, 1 + ctx.getDifficulty(), true),
            parseBlock(
                "{"
                    + ELIDED
                    + ELIDED
                    + "\n// Pretend there is lots of code here\n"
                    + ELIDED
                    + ELIDED
                + "}"),
            parseBlock(
                "{" + Nonsense.methodName(ctx) + "();}"));
        ctx.output().codeBlock(ifStmt);
        ctx.output().paragraph(
            """
            Improve readability by refactoring this conditional so that its *two clauses are
            swapped*: what is now the second clause (the `else` clause) comes first, and the first
            clause comes second.
            """);
        ctx.solution(() -> {
            ctx.output().codeBlock(
                new IfStmt(
                    negated(ifStmt.getCondition()),
                    ifStmt.getElseStmt().orElseThrow(), // second clause first!
                    ifStmt.getThenStmt()));
            ctx.solutionChecklist(
                """
                Do not just negate the condition by wrapping it all in a *not* operator like this:
                `!(...)` Instead, make sure you negate the condition by changing each part of it.
                """,
                """
                You do not actually have to write out the words `Pretend there is lots of code here`
                when you write out your solution! Just draw three dots; that’s enough.
                """);
        });
    }
}
