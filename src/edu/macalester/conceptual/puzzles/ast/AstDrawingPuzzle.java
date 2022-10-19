package edu.macalester.conceptual.puzzles.ast;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.ast.AstUtils;
import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.puzzles.booleans.Generator;
import edu.macalester.graphics.CanvasWindow;

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
    public void generate(PuzzleContext ctx) {
        var expr = Generator.generateBooleanExpr(ctx, 10, true);
        String exprAsString = AstUtils.withParensAsNeeded(expr).toString();

        ctx.output().paragraph("Draw an AST for the following expression:");
        ctx.output().codeBlock(exprAsString);
        ctx.solution(() -> {
            ctx.output().paragraph("<< drawn in graphics window >>");

            var ast = AstDrawing.of(expr);

            double margin = 24;
            var window = new CanvasWindow(
                exprAsString,
                (int) Math.ceil(ast.getWidth() + margin * 2),
                (int) Math.ceil(ast.getHeight() + margin * 2));
            window.add(ast, margin, margin);
        });
    }
}
