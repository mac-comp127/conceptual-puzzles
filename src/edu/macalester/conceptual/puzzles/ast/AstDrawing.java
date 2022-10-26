package edu.macalester.conceptual.puzzles.ast;

import com.github.javaparser.ast.DataKey;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UnaryExpr;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import edu.macalester.conceptual.ast.AstUtils;
import edu.macalester.graphics.FontStyle;
import edu.macalester.graphics.GraphicsGroup;
import edu.macalester.graphics.GraphicsText;
import edu.macalester.graphics.Line;
import edu.macalester.graphics.TextAlignment;

import static com.github.javaparser.ast.expr.BinaryExpr.Operator.*;
import static edu.macalester.conceptual.ast.AstUtils.*;

public class AstDrawing extends GraphicsGroup {
    private final double anchorX;

    public static AstDrawing of(Expression expr, float hue) {
        String annotation =
            EvaluationTree.valueOf(expr)
                .map(AstDrawing::formatValue)
                .orElse(null);

        if (expr instanceof EnclosedExpr enclosed) {
            return AstDrawing.of(enclosed.getInner(), hue);
        } else if (expr instanceof UnaryExpr unary) {
            return new AstDrawing(
                unary.getOperator().asString(),
                annotation,
                hue,
                AstDrawing.of(unary.getExpression(), hue));
        } else if (expr instanceof BinaryExpr binary) {
            return new AstDrawing(
                binary.getOperator().asString(),
                annotation,
                hue,
                AstDrawing.of(binary.getLeft(), hue),
                AstDrawing.of(binary.getRight(), hue));
        } else {
            return new AstDrawing(expr.toString(), annotation, hue);
        }
    }

    private static String formatValue(Object value) {
        if (value instanceof String) {
            return "\"" + value + "\"";
        } else {
            return value.toString();
        }
    }

    public AstDrawing(String labelText, String annotationText, float hue, AstDrawing... children) {
        this(labelText, annotationText, hue, Arrays.asList(children));
    }

    public AstDrawing(String labelText, String annotationText, float hue, List<AstDrawing> children) {
        var label = new GraphicsText(labelText);
        label.setAlignment(TextAlignment.CENTER);
        label.setFont("SF Mono, Menlo, Consolas, monospaced", FontStyle.BOLD, 24);

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

        if (annotationText != null) {
            var annotation = new GraphicsText(annotationText);
            annotation.setAlignment(TextAlignment.LEFT);
            annotation.setFont("Helvetica Neue, Helvetica, Arial, sans", FontStyle.PLAIN, 16);
            annotation.setFillColor(Color.getHSBColor(hue, 1f, 0.8f));
            add(annotation, label.getBoundsInParent().getMaxX() + childMarginX / 3, label.getY());
        }

        double childX = 0;
        double childY = label.getPosition().getY() + Math.max(childMarginY * 6, childSpan * 0.25);
        for (var child : children) {
            add(child, childX, childY);

            var line = new Line(
                child.getPosition().getX() + child.anchorX,
                child.getPosition().getY() - childMarginY,
                label.getPosition().getX(),
                label.getPosition().getY() + childMarginY);
            line.setStrokeColor(Color.getHSBColor(hue, 0.2f, 0.7f));
            add(line);

            childX += child.getWidth() + childMarginX;
        }
    }
}
