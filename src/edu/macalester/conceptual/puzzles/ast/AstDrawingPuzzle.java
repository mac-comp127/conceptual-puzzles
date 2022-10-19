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
            outputPuzzle(ctx, generateArithmeticExpression(ctx, ctx.getDifficulty() + 2));
        });

        ctx.section(() -> {
            int totalLeaves = ctx.getDifficulty() + 2;
            int arithmeticLeaves = (int) Math.pow(totalLeaves, 0.2);
            outputPuzzle(ctx, generateArithmeticComparisonsExpression(
                ctx, totalLeaves / arithmeticLeaves, arithmeticLeaves));
        });

        ctx.section(() -> {
            outputPuzzle(ctx, generateStringAdditionExpression(
                ctx, ctx.getDifficulty() + 4));
        });
    }

    private static void outputPuzzle(PuzzleContext ctx, String exprAsString) {
        Expression expr = StaticJavaParser.parseExpression(exprAsString);
        ctx.output().codeBlock(expr);
        ctx.solution(() -> {
            ctx.output().paragraph("<< drawn in graphics window >>");

            var ast = AstDrawing.of(expr);

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
        });
    }
}
