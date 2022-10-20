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

    public static AstDrawing of(Expression expr, Options options) {
        var evaluationResult = formatValue(
            Evaluator.evaluate(Object.class, expr.toString(), options.varDecls));

        if (expr instanceof EnclosedExpr enclosed) {
            return AstDrawing.of(enclosed.getInner(), options);
        } else if (expr instanceof UnaryExpr unary) {
            return new AstDrawing(
                unary.getOperator().asString(),
                evaluationResult,
                options,
                AstDrawing.of(unary.getExpression(), options));
        } else if (expr instanceof BinaryExpr binary) {
            return new AstDrawing(
                binary.getOperator().asString(),
                evaluationResult,
                options,
                AstDrawing.of(binary.getLeft(), options),
                AstDrawing.of(binary.getRight(), options));
        } else {
            return new AstDrawing(expr.toString(), evaluationResult, options);
        }
    }

    private static String formatValue(Object value) {
        if (value instanceof String) {
            return "\"" + value + "\"";
        } else {
            return value.toString();
        }
    }

    public AstDrawing(String labelText, String annotationText, Options options, AstDrawing... children) {
        this(labelText, annotationText, options, Arrays.asList(children));
    }

    public AstDrawing(String labelText, String annotationText, Options options, List<AstDrawing> children) {
        var label = new GraphicsText(labelText);
        label.setAlignment(TextAlignment.CENTER);
        label.setFont("SF Mono, Menlo, Consolas, monospaced", FontStyle.BOLD, 24);

        var annotation = new GraphicsText(annotationText);
        annotation.setAlignment(TextAlignment.LEFT);
        annotation.setFont("Helvetica Neue, Helvetica, Arial, sans", FontStyle.PLAIN, 16);
        annotation.setFillColor(Color.getHSBColor(options.annotationHue, 1f, 0.7f));

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
        add(annotation, label.getBoundsInParent().getMaxX() + 6, label.getY());

        double childX = 0;
        double childY = label.getPosition().getY() + Math.max(childMarginY * 6, childSpan * 0.25);
        for (var child : children) {
            add(child, childX, childY);

            var line = new Line(
                child.getPosition().getX() + child.anchorX,
                child.getPosition().getY() - childMarginY,
                label.getPosition().getX(),
                label.getPosition().getY() + childMarginY);
            line.setStrokeColor(Color.getHSBColor(options.lineHue, 0.5f, 1.0f));
            add(line);

            childX += child.getWidth() + childMarginX;
        }
    }

    public record Options(
        String varDecls,
        float lineHue,
        float annotationHue
    ) { }
}
