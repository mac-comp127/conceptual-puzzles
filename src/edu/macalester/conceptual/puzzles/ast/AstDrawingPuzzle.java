package edu.macalester.conceptual.puzzles.ast;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.Expression;

import java.awt.Toolkit;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.graphics.CanvasWindow;

import static edu.macalester.conceptual.puzzles.ast.Generator.generateArithmeticComparisonsExpression;
import static edu.macalester.conceptual.puzzles.ast.Generator.generateArithmeticExpression;
import static edu.macalester.conceptual.puzzles.ast.Generator.generateStringAdditionExpression;
import static edu.macalester.conceptual.util.CodeFormatting.*;

public class AstDrawingPuzzle implements Puzzle {
    @Override
    public byte id() {
        return 2;
    }

    @Override
    public String name() {
        return "ast";
    }

    @Override
    public String description() {
        return "Drawing ASTs for expressions";
    }

    @Override
    public int goalDifficulty() {
        return 2;
    }

    @Override
    public int maxDifficulty() {
        return 50;
    }

    @Override
    public void generate(PuzzleContext ctx) {

        ctx.output().paragraph(
            "For each of the Java expressions below:");
        ctx.output().bulletList(
            "Draw an AST for the expression.",
            "Show how the expression evaluates at each node in the tree.");
        ctx.output().paragraph(
            "Be sure that your tree accurately reflects how Java would evaluate the expression.");

        ctx.section(() -> {
            var vars = new VariablePool();
            outputPuzzle(ctx, vars,
                generateArithmeticExpression(ctx, vars, ctx.getDifficulty() + 2),
                "Do you have the left and right branches on the correct side?",
                "Did you indicate the difference between ints and floating point values?");
        });

        ctx.section(() -> {
            var vars = new VariablePool();
            int totalLeaves = ctx.getDifficulty() + 2;
            int arithmeticLeaves = (int) Math.pow(totalLeaves, 0.2);
            outputPuzzle(ctx, vars,
                generateArithmeticComparisonsExpression(ctx, vars,
                    totalLeaves / arithmeticLeaves, arithmeticLeaves),
                "Does your tree correctly reflect the precedence of && and ||?");
        });

        ctx.section(() -> {
            outputPuzzle(ctx, new VariablePool(),
                generateStringAdditionExpression(
                    ctx, ctx.getDifficulty() + 4),
                "Did you indicate the distinction between ints and Strings?",
                "Are your left/right branches correct?");
        });
    }

    private static void outputPuzzle(PuzzleContext ctx, VariablePool vars, String exprAsString, String... solutionChecklist) {
        Expression expr = StaticJavaParser.parseExpression(exprAsString);

        if (vars.isEmpty()) {
            ctx.output().paragraph("Draw the AST and evaluation tree for the following expression:");
        } else {
            ctx.output().paragraph("Given the following variables:");
            ctx.output().codeBlock(prettifyStatements(vars.allDeclarations()));
            ctx.output().paragraph("...draw the AST and evaluation tree for the following expression:");
        }
        ctx.output().codeBlock(expr);

        ctx.solution(() -> {
            ctx.output().paragraph("<< drawing in canvas window >>");

            var ast = AstDrawing.of(expr, vars.allDeclarations());

            double margin = 24;
            var screensize = Toolkit.getDefaultToolkit().getScreenSize();
            double scale = Math.min(1,
                Math.min((screensize.getWidth() - 50 - margin * 2) / ast.getWidth(),
                    (screensize.getHeight() - 50 - margin * 2) / ast.getHeight()));
            var window = new CanvasWindow(
                exprAsString,
                (int) Math.ceil(ast.getWidth() * scale + margin * 2),
                (int) Math.ceil(ast.getHeight() * scale + margin * 2));
            window.add(ast, margin, margin);
            ast.setScale(scale);
            ast.setAnchor(0, 0);

            ctx.solutionChecklist(solutionChecklist);
        });
    }

    public static void main(String[] args) {
        var puzzle = new AstDrawingPuzzle();
        PuzzleContext ctx = PuzzleContext.generate(puzzle.id());
        ctx.enableSolution();
        ctx.setDifficulty(puzzle.goalDifficulty());
        ctx.emitPuzzle(() -> puzzle.generate(ctx));
    }
}
