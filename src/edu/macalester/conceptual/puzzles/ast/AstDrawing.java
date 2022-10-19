package edu.macalester.conceptual.puzzles.ast;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UnaryExpr;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

import edu.macalester.graphics.FontStyle;
import edu.macalester.graphics.GraphicsGroup;
import edu.macalester.graphics.GraphicsText;
import edu.macalester.graphics.Line;
import edu.macalester.graphics.TextAlignment;

public class AstDrawing extends GraphicsGroup {
    private final double anchorX;

    public static AstDrawing of(Expression expr) {
        if (expr instanceof EnclosedExpr enclosed) {
            return AstDrawing.of(enclosed.getInner());
        } else if (expr instanceof UnaryExpr unary) {
            return new AstDrawing(
                unary.getOperator().asString(),
                AstDrawing.of(unary.getExpression()));
        } else if (expr instanceof BinaryExpr binary) {
            return new AstDrawing(
                binary.getOperator().asString(),
                AstDrawing.of(binary.getLeft()),
                AstDrawing.of(binary.getRight()));
        } else {
            return new AstDrawing(expr.toString());
        }
    }

    public AstDrawing(String labelText, AstDrawing... children) {
        this(labelText, Arrays.asList(children));
    }

    public AstDrawing(String labelText, List<AstDrawing> children) {
        var label = new GraphicsText(labelText);
        label.setAlignment(TextAlignment.CENTER);
        label.setFont("monospaced", FontStyle.BOLD, 24);

        double childMarginX = 24, childMarginY = 8;
        double anchorX = label.getBounds().getWidth() / 2;
        double childSpan;
        if (children.isEmpty()) {
            childSpan = 0;
        } else {
            double leftmostChildAnchorX = children.get(0).anchorX;
            double rightmostChildAnchorX =
                children
                    .subList(0, children.size() - 1).stream()
                    .mapToDouble(child -> child.getWidth() + childMarginX)
                    .sum()
                + children.get(children.size() - 1).anchorX;
            childSpan = rightmostChildAnchorX - leftmostChildAnchorX;
            anchorX = Math.max(
                anchorX,
                (leftmostChildAnchorX + rightmostChildAnchorX) / 2);
        }
        this.anchorX = anchorX;

        add(label, anchorX, label.getHeight());

        double childX = 0;
        double childY = label.getPosition().getY() + Math.max(childMarginY * 6, childSpan * 0.25);
        for (var child : children) {
            add(child, childX, childY);

            var line = new Line(
                child.getPosition().getX() + child.anchorX,
                child.getPosition().getY() - childMarginY,
                label.getPosition().getX(),
                label.getPosition().getY() + childMarginY);
            line.setStrokeColor(new Color(0xFF5269FF, true));
            add(line);

            childX += child.getWidth() + childMarginX;
        }
    }
}
