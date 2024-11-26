package edu.macalester.conceptual.puzzles.relationships;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.macalester.conceptual.util.DiagramUtils;
import edu.macalester.graphics.GraphicsGroup;
import edu.macalester.graphics.GraphicsObject;
import edu.macalester.graphics.GraphicsText;
import edu.macalester.graphics.Path;
import edu.macalester.graphics.Point;
import edu.macalester.graphics.Rectangle;
import edu.macalester.graphics.Strokable;

public class RelationshipDiagramBuilder {
    private final Collection<Type> includedTypes;
    private final float hue;
    private final double innerMargin = 18, spacing = 24;
    private double arrowWidth = 12, arrowLen = 12;

    public RelationshipDiagramBuilder(List<Type> includedTypes, float hue) {
        this.includedTypes = List.copyOf(includedTypes);
        this.hue = hue;
    }

    public GraphicsObject build(Type root) {
        return buildSubtree(root).graphics();
    }

    private SubtreeDiagram buildSubtree(Relationship rel) {
        return buildSubtree(rel.getTargetType());
    }

    private SubtreeDiagram buildSubtree(Type type) {
        var boxLayer = new GraphicsGroup();
        var arrowLayer = new GraphicsGroup();

        var label = new GraphicsText(type.getName());
        DiagramUtils.applyNodeLabelFont(label);
        var box = new Rectangle(
            0, 0,
            label.getWidth() + innerMargin * 2,
            label.getLineHeight() + innerMargin * 2);
        box.setFillColor(Color.BLACK);
        box.setStrokeColor(Color.GRAY);
        box.setStrokeWidth(1);
        label.setCenter(box.getCenter());
        boxLayer.add(box);
        boxLayer.add(label);

        Relationship topEdge = null, rightEdge = null;
        List<Relationship> bottomEdge = new ArrayList<>();

        for (var rel : type.getRelationships()) {
            if (includedTypes.contains(rel.getTargetType())) {
                if (topEdge == null && rel instanceof Relationship.IsA) {
                    topEdge = rel;
//                } else if (rightEdge == null) {
//                    rightEdge = rel;
                } else {
                    bottomEdge.add(rel);
                }
            }
        }

        if (topEdge != null) {
            var superDiagram = buildSubtree(topEdge);

            var arrow = makeSupertypeArrow(box, superDiagram, topEdge.getArrowLabel());
            arrowLayer.add(arrow);

            boxLayer.add(
                superDiagram.graphics(),
                0,
                arrow.getBoundsInParent().getMinY() - superDiagram.bottomAnchor().getY());
        }

        double childX = box.getWidth(), childY = 0;
        for (var rel : bottomEdge) {
            var childDiagram = buildSubtree(rel);
            boxLayer.add(childDiagram.graphics());

            var arrow = makeHorizontalArrow(box, childDiagram, childY, rel.getArrowLabel());
            arrowLayer.add(arrow);

            childDiagram.graphics().setPosition(
                new Point(childX + arrow.getWidth(), childY));
            childY += childDiagram.graphics().getBounds().getMaxY() + spacing;
        }

        var group = new GraphicsGroup();
        var yOffset = -boxLayer.getBounds().getMinY();
        group.add(arrowLayer, 0, yOffset);
        group.add(boxLayer, 0, yOffset);

        return new SubtreeDiagram(
            group,
            new Point(box.getX(), box.getCenter().getY() + yOffset),
            new Point(box.getX() + spacing, box.getBounds().getMaxY() + yOffset)
        );
    }

    private GraphicsObject makeSupertypeArrow(Rectangle sourceBox, SubtreeDiagram target, String labelText) {
        var arrow = new GraphicsGroup();
        arrow.setPosition(target.bottomAnchor().getX(), sourceBox.getY());

        var label = new GraphicsText(labelText);
        DiagramUtils.applyArrowLabelFont(label, hue);
        label.rotateBy(-90);
        arrow.add(label);

        double pathLen = Math.max(
            label.getWidth() + innerMargin * 2 + arrowLen,
            target.graphics().getBounds().getMaxY() - target.bottomAnchor().getY() + spacing);
        var endPoint = new Point(0, -pathLen);

        var connectingPath = new Path(
            List.of(Point.ORIGIN, endPoint),
            false
        );
        styleArrowStroke(connectingPath);
        arrow.add(connectingPath);

        label.setCenter(connectingPath.getCenter().add(new Point(-label.getHeight(), arrowLen / 2)));

        var arrowHead = new Path(
            List.of(
                endPoint.add(new Point(-arrowWidth / 2, arrowLen)),
                endPoint,
                endPoint.add(new Point(arrowWidth / 2, arrowLen))
            ), true
        );
        styleArrowStroke(arrowHead);
        arrowHead.setFillColor(Color.BLACK);
        arrow.add(arrowHead);

        return arrow;
    }

    private GraphicsObject makeHorizontalArrow(
        Rectangle sourceBox,
        SubtreeDiagram target,
        double targetY,
        String labelText
    ) {
        var arrow = new GraphicsGroup();
        arrow.setPosition(sourceBox.getBounds().getMaxX(), sourceBox.getCenter().getY());

        var label = new GraphicsText(labelText);
        DiagramUtils.applyArrowLabelFont(label, hue);
        arrow.add(label);

        double pathLen = label.getWidth() + innerMargin * 2 + arrowLen;
        var endPoint = new Point(pathLen, target.leftAnchor().getY() + targetY - sourceBox.getHeight() / 2);

        var connectingPath = new Path(
            List.of(
                Point.ORIGIN,
                new Point(innerMargin, 0),
                new Point(innerMargin, endPoint.getY()),
                endPoint
            ),
            false
        );
        styleArrowStroke(connectingPath);
        arrow.add(connectingPath);

        label.setCenter(
            pathLen / 2,
            endPoint.getY() - label.getHeight());

        var arrowHead = new Path(
            List.of(
                endPoint.add(new Point(-arrowLen, -arrowWidth / 2)),
                endPoint,
                endPoint.add(new Point(-arrowLen, arrowWidth / 2))
            ), false
        );
        styleArrowStroke(arrowHead);
        arrow.add(arrowHead);

        return arrow;
    }

    private void styleArrowStroke(Strokable path) {
        path.setStrokeColor(DiagramUtils.connectingLineColor(hue));
        path.setStrokeWidth(2);
    }

    private record SubtreeDiagram(
        GraphicsGroup graphics,
        Point leftAnchor,
        Point bottomAnchor
    ) { }
}
