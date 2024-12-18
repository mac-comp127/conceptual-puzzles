package edu.macalester.conceptual.puzzles.ast;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.resolution.types.ResolvedType;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import edu.macalester.conceptual.util.DiagramUtils;
import edu.macalester.graphics.FontStyle;
import edu.macalester.graphics.GraphicsGroup;
import edu.macalester.graphics.GraphicsText;
import edu.macalester.graphics.Line;
import edu.macalester.graphics.TextAlignment;

/**
 * A graphical representation of an abstract syntax tree.
 */
public class AstDrawing extends GraphicsGroup {
    private final double rootNodeX;

    public static AstDrawing of(Expression expr, float hue) {
        String annotation =
            AnnotatedAst.valueOf(expr)
                .map(AstDrawing::formatValue)
                .orElse(null);

        if (expr instanceof EnclosedExpr enclosed) {     // Parens don't show in the tree at all
            return AstDrawing.of(enclosed.getInner(), hue);
        } else if (expr instanceof UnaryExpr unary) {    // Single child
            return new AstDrawing(
                unary.getOperator().asString(),
                annotation,
                hue,
                AstDrawing.of(unary.getExpression(), hue));
        } else if (expr instanceof BinaryExpr binary) {  // Two children
            return new AstDrawing(
                binary.getOperator().asString(),
                annotation,
                hue,
                AstDrawing.of(binary.getLeft(), hue),
                AstDrawing.of(binary.getRight(), hue));
        } else if (expr instanceof MethodCallExpr methodCall) {  // Tricky!
            return new AstDrawing(
                "." + methodCall.getNameAsString() + "()",
                annotation,
                hue,
                true,
                Stream.concat(
                    Stream.of(methodCall.getScope().orElse(new NameExpr("this"))),
                    methodCall.getArguments().stream()
                ).map(ast -> AstDrawing.of(ast, hue)).toList()
            );
        } else {                                         // ¯\_(ツ)_/¯ Just show the code
            return new AstDrawing(expr.toString(), annotation, hue);
        }
    }

    private static String formatValue(Object value) {
        if (value instanceof String) {
            return "\"" + value + "\"";
        } else if (value instanceof Double) {
            return value.toString().replaceAll("(\\.\\d\\d)\\d+", "$1…");  // Just 2 decimal places
        } else if (value instanceof Class<?> runtimeType) {
            return runtimeType.getSimpleName();
        } else if (value instanceof ResolvedType staticType) {
            return staticType.describe().replaceAll("(\\w+\\.)", "");  // strip package names
        } else {
            return value.toString();
        }
    }

    public AstDrawing(String labelText, String annotationText, float hue, AstDrawing... children) {
        this(labelText, annotationText, hue, false, Arrays.asList(children));
    }

    public AstDrawing(
        String labelText,
        String annotationText,
        float hue,
        boolean methodCallChildSpacing,
        List<AstDrawing> children
    ) {
        // Root node
        var label = makeLabel(labelText);
        label.setAlignment(TextAlignment.CENTER);
        label.setFillColor(Color.WHITE);

        // Children
        double childMarginX = 24, childMarginY = 8;
        double rootNodeX = label.getBounds().getWidth() / 2;
        double childSpan;
        if (children.isEmpty()) {
            childSpan = 0;
        } else {
            // Center the root node between the leftmost and rightmost children’s root nodes
            double leftmostChildAnchorX = children.get(0).rootNodeX;
            double rightmostChildAnchorX =
                children
                    .subList(0, children.size() - 1).stream()
                    .mapToDouble(child -> child.getWidth() + childMarginX)
                    .sum()
                + children.get(children.size() - 1).rootNodeX;
            childSpan = rightmostChildAnchorX - leftmostChildAnchorX;
            rootNodeX = Math.max(
                rootNodeX,
                (leftmostChildAnchorX + rightmostChildAnchorX) / 2);
        }
        this.rootNodeX = rootNodeX;

        add(label, rootNodeX, label.getHeight());

        // Annotation (currently always the node's value at runtime; could add types in future)
        if (annotationText != null) {
            var annotation = new GraphicsText(annotationText);
            annotation.setAlignment(TextAlignment.LEFT);
            annotation.setFont("Helvetica Neue, Helvetica, Arial, sans", FontStyle.PLAIN, 16);
            annotation.setFillColor(Color.getHSBColor(hue, 0.6f, 1.0f));
            add(annotation, label.getBoundsInParent().getMaxX() + childMarginX / 3, label.getY());
        }

        // Root node is in position; stack children horizontally underneath
        double childX = 0;
        double childY = label.getPosition().getY()
            + Math.max(childMarginY * 6, childSpan * 0.25);  // avoid lines that are too flat or too steep
        for (var child : children) {
            add(child, childX, childY);

            double lineAnchorX;
            if (methodCallChildSpacing) {
                if (childX == 0) {
                    lineAnchorX = label.getBoundsInParent().getMinX();
                } else {
                    double parentWidth = makeLabel("()").getWidth() / 2 - 1;
                    lineAnchorX = label.getBoundsInParent().getMaxX() - parentWidth;
                }
            } else {
                lineAnchorX = label.getPosition().getX();
            }

            var line = new Line(
                child.getPosition().getX() + child.rootNodeX,
                child.getPosition().getY() - childMarginY,
                lineAnchorX,
                label.getPosition().getY() + childMarginY);
            line.setStrokeColor(DiagramUtils.connectingLineColor(hue));
            add(line);

            childX += child.getWidth() + childMarginX;
        }
    }

    private static GraphicsText makeLabel(String labelText) {
        var label = new GraphicsText(labelText);
        DiagramUtils.applyNodeLabelFont(label);
        return label;
    }
}
