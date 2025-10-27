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

class RelationshipDiagramBuilder {
    private final Collection<Type> includedTypes;
    private final float hue;
    private final double innerMargin = 18, spacing = 24;
    private final double arrowWidth = 12, arrowLen = 12;

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

        Relationship topEdge = null;
        List<Relationship> rightAndBottomEdge = new ArrayList<>();

        for (var rel : type.getRelationships()) {
            if (includedTypes.contains(rel.getTargetType())) {
                if (topEdge == null && rel instanceof Relationship.IsA) {
                    topEdge = rel;
                } else {
                    rightAndBottomEdge.add(rel);
                }
            }
        }

        if (topEdge != null) {
            var superDiagram = buildSubtree(topEdge);
            var arrow = makeSupertypeArrow(
                new Point(spacing, 0),
                topEdge.getArrowLabel(),
                superDiagram.graphics().getBounds().getMaxY() - superDiagram.bottomAnchor().getY() + spacing);
            arrowLayer.add(arrow);

            boxLayer.add(
                superDiagram.graphics(),
                0,
                arrow.getBoundsInParent().getMinY() - superDiagram.bottomAnchor().getY());
        }

        int index = 0;
        double childY = 0;
        double arrowSpacing = Math.min(
            spacing,
            Math.max(0, box.getWidth() - spacing * 2) / (rightAndBottomEdge.size() - 2));
        for (var rel : rightAndBottomEdge) {
            var childDiagram = buildSubtree(rel);
            boxLayer.add(childDiagram.graphics());

            boolean rightEdge = (index == 0);
            Point anchor = new Point(
                box.getBounds().getMaxX() - arrowSpacing * index,
                rightEdge ? box.getCenter().getY() : box.getBounds().getMaxY());
            double targetY = childDiagram.leftAnchor().getY();
            double leftHookSize = rightEdge ? innerMargin : 0;
            var arrow = makeHorizontalArrow(
                anchor, targetY, rel.getArrowLabel(), 0, leftHookSize, childY, false);
            arrowLayer.add(arrow);

            childDiagram.graphics().setPosition(
                new Point(anchor.getX() + arrow.getWidth(), childY));
            childY += childDiagram.graphics().getBounds().getMaxY() + spacing;

            index++;
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

    private GraphicsObject makeHorizontalArrow(
        Point anchor,
        double targetY,
        String labelText,
        double minPathLen,
        double leftHookSize,
        double childY,
        boolean hollowArrowhead
    ) {
        var arrow = new GraphicsGroup();
        arrow.setPosition(anchor);

        var label = new GraphicsText(labelText);
        DiagramUtils.applyArrowLabelFont(label, hue);
        arrow.add(label);

        double pathLen = Math.max(
            minPathLen,
            label.getWidth() + leftHookSize + innerMargin * 2 + arrowLen);
        var endPoint = new Point(pathLen, targetY - anchor.getY() + childY);

        var connectingPath = new Path(
            List.of(
                Point.ORIGIN,
                new Point(leftHookSize, 0),
                new Point(leftHookSize, endPoint.getY()),
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
            ), hollowArrowhead
        );
        styleArrowStroke(arrowHead);
        if (hollowArrowhead) {
            arrowHead.setFillColor(Color.BLACK);
        }
        arrow.add(arrowHead);

        return arrow;
    }

    private GraphicsObject makeSupertypeArrow(Point anchor, String labelText, double minPathLen) {
        var arrow = makeHorizontalArrow(anchor, 0, labelText, minPathLen, 0, 0, true);
        arrow.setAnchor(0, 0);
        arrow.setRotation(-90);
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
