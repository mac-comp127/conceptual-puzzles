package edu.macalester.conceptual.puzzles.stack;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.macalester.conceptual.util.DiagramUtils;
import edu.macalester.conceptual.util.IdentityKey;
import edu.macalester.graphics.GraphicsGroup;
import edu.macalester.graphics.GraphicsObject;
import edu.macalester.graphics.GraphicsText;
import edu.macalester.graphics.Line;
import edu.macalester.graphics.Path;
import edu.macalester.graphics.Point;
import edu.macalester.graphics.Rectangle;

class CallStackDiagram {
    private final GraphicsGroup graphics = new GraphicsGroup();
    private final Map<VariableContainer, GraphicsObject> boxes = new HashMap<>();
    private final Map<Integer, GraphicsGroup> columns = new HashMap<>();
    private final Map<IdentityKey<GraphicsObject>, List<GraphicsObject>> incomingConnections =
        new HashMap<>();
    private final double marginX = 18, marginY = 12, arrowTipGap = 2.5;
    private final double columnWidth = 240;
    private final double baseline;
    private float hue;

    public static GraphicsObject of(List<VariableContainer> stack, float hue) {
        return new CallStackDiagram(stack, hue).getGraphics();
    }

    private CallStackDiagram(List<VariableContainer> stack, float hue) {
        this.hue = hue;
        
        var baselineComputation = new GraphicsText("Mi", 0, 0);
        DiagramUtils.applyNodeLabelFont(baselineComputation);
        baseline = baselineComputation.getHeight();

        for (var stackFrame : stack) {
            render(stackFrame, 0);
        }

        centerColumns();
        renderConnections();
        labelColumns();
    }

    /**
     * Renders one stack frame or object
     */
    private GraphicsObject render(VariableContainer container, int columnNum) {
        // Reuse existing if present

        var existing = boxes.get(container);
        if (existing != null) {
            return existing;
        }

        // Create group, border, background

        GraphicsGroup box = new GraphicsGroup();
        boxes.put(container, box);

        Rectangle background = new Rectangle(0, 0, 0, 0);  // will size at the end
        DiagramUtils.applyBoxStyle(background);
        box.add(background);

        // Add title

        double y = marginY;

        var title = new GraphicsText(container.getTitle(), marginX, y + baseline);
        DiagramUtils.applyNodeLabelFont(title);
        box.add(title);
        y += title.getLineHeight();

        var divider = new Line(marginX, y, marginX + title.getWidth(), y);
        DiagramUtils.applyDividerStyle(divider);
        box.add(divider);
        y += marginY;

        // Add variables

        var valueRenderings = new ArrayList<Runnable>();
        for (var variable : container.getVariables()) {
            var varLabel = new GraphicsText(variable.name(), marginX, y + baseline);
            DiagramUtils.applyNodeDetailFont(varLabel);
            box.add(varLabel);
            y += title.getLineHeight();

            // We add the var labels top to bottom, but the object boxes get stacked from bottom
            // to top. We thus want to add the object boxes in the reverse order we create the
            // labels to reduce confusing arrow crossings, so we queue up the creation of the
            // object boxes and then run them in reverse.
            valueRenderings.add(
                () -> renderValue(variable, varLabel, box, columnNum));
        }
        // TODO: When we move to Java 21, this can become:
        // valueRenderings.reversed().forEach(Runnable::run);
        Collections.reverse(valueRenderings);
        valueRenderings.forEach(Runnable::run);

        // Size enclosing box and add to appropriate column

        background.setSize(box.getWidth() + marginX, y + marginY);

        GraphicsGroup column = getColumn(columnNum);
        box.setY(column.getBounds().getMinY() - box.getHeight() - marginY * 2);
        column.add(box);

        return box;
    }

    /**
     * The renderer groups the items into columns: stack frames in column 0, objects in 1+
     */
    private GraphicsGroup getColumn(int columnNum) {
        return columns.computeIfAbsent(columnNum, __ -> {
            var column = new GraphicsGroup();
            column.setX(columnNum * columnWidth);
            graphics.add(column);
            return column;
        });
    }

    /**
     * One row of a variable container
     */
    private void renderValue(Variable variable, GraphicsText varLabel, GraphicsGroup box, int columnNum) {
        if (variable.value() instanceof Value.Inline inlineValue) {
            var valueLabel = new GraphicsText(
                inlineValue.value(),
                varLabel.getBoundsInParent().getMaxX() + marginX,
                varLabel.getY());
            DiagramUtils.applyNodeDetailFont(valueLabel);
            valueLabel.setFillColor(Color.GRAY);
            box.add(valueLabel);
        } else if (variable.value() instanceof Value.Reference ref) {
            var valueBox = render(ref.object().variableContainer(), columnNum + 1);
            addConnection(varLabel, valueBox);
        } else {
            throw new IllegalArgumentException("Don't know how to render value of type "
                + variable.value().getClass());
        }
    }

    /**
     * Gather an arrow connecting two items for later rendering
     */
    private void addConnection(GraphicsObject from, GraphicsObject to) {
        var toIdentity = new IdentityKey<>(to);
        incomingConnections
            .computeIfAbsent(toIdentity, __ -> new ArrayList<>())
            .add(from);
    }

    /**
     * We draw all the arrows after the boxes, both so that they are in the foreground and so that
     * we can space out the incoming connections evenly for each box.
     */
    private void renderConnections() {
        for (var connection : incomingConnections.entrySet()) {
            var to = connection.getKey().object();
            int incomingCount = connection.getValue().size();
            int index = incomingCount;
            for (var from : connection.getValue()) {
                index--;
                renderConnection(from, to, index, incomingCount);
            }
        }
    }

    /**
     * Draws one arrow
     */
    private void renderConnection(
        GraphicsObject from,
        GraphicsObject to,
        int index,
        int incomingCount
    ) {
        var startPoint = convertToCanvas(
            from, new Point(
                from.getBounds().getMaxX() + marginX / 2,
                fractionOfYExtent(from, 0.5)
            )
        );
        boolean pointingRight = convertToCanvas(to, to.getPosition()).getX() > startPoint.getX();
        var endPoint = convertToCanvas(
            to, new Point(
                pointingRight
                    ? to.getBounds().getMinX() - arrowTipGap
                    : to.getBounds().getMaxX() + arrowTipGap,
                fractionOfYExtent(to, (float) (index + 1) / (incomingCount + 1))
            )
        );

        addConnectingPath(startPoint, endPoint, pointingRight);

        addArrowhead(endPoint, pointingRight);

        hue = (hue + 0.382f) % 1;
    }

    /**
     * Helper to space arrows along edge of box
     */
    private double fractionOfYExtent(GraphicsObject obj, double fraction) {
        var bounds = obj.getBounds();
        return bounds.getMinY() * (1 - fraction) + bounds.getMaxY() * fraction;
    }

    private void addConnectingPath(Point startPoint, Point endPoint, boolean pointingRight) {
        var startElbow = startPoint.add(new Point(marginX * 2, 0));
        var endElbow = endPoint.add(new Point(marginX * (pointingRight ? -2 : 2), 0));
        if (!pointingRight) {
            // Clean up back-pointing arrows, especially to self
            double minElbowGap = DiagramUtils.ARROW_WIDTH * 2;
            double startElbowY;
            if (startElbow.getY() < endElbow.getY()) {
                startElbowY = Math.min(
                    startElbow.getY(),
                    endElbow.getY() - minElbowGap
                );
            } else {
                startElbowY = Math.max(
                    startElbow.getY(),
                    endElbow.getY() + minElbowGap
                );
            }

            startElbow = new Point(Math.max(startElbow.getX(), endElbow.getX()), startElbowY);
        }

        var path = new Path(List.of(startPoint, startElbow, endElbow, endPoint), false);
        DiagramUtils.applyArrowStrokeStyle(path, hue);
        graphics.add(path);
    }

    private void addArrowhead(Point tip, boolean pointingRight) {
        // Placing the actual arrowhead path inside a group cleans up the rotation logic
        var arrowhead = new GraphicsGroup();
        arrowhead.add(DiagramUtils.makeArrowhead(Point.ORIGIN, false, hue));
        if (!pointingRight) {
            arrowhead.setAnchor(Point.ORIGIN);
            arrowhead.setRotation(180);
        }
        arrowhead.setPosition(tip);
        graphics.add(arrowhead);
    }

    /**
     * Converts a point from the given GraphicsObject’s local coordinates to canvas coordinate.
     * This really ought to live in Kilt Graphics, but it doesn't...yet!
     */
    private Point convertToCanvas(GraphicsObject context, Point p) {
        while (context.getParent() != null) {
            p = p.add(context.getPosition());
            context = context.getParent();
        }
        return p;
    }

    private void centerColumns() {
        for (var column : columns.values()) {
            column.setCenter(column.getCenter().getX(), 0);
        }
    }

    private void labelColumns() {
        double colTop = columns.values().stream()
            .mapToDouble(col -> col.getBoundsInParent().getMinY())
            .min().orElseThrow();

        double x = 0;
        for (var labelText : List.of("Stack", "Heap")) {
            var label = new GraphicsText(labelText, x, colTop - marginY * 2);
            DiagramUtils.applyLabelFont(label);
            graphics.add(label);
            x += columnWidth;
        }
    }

    private GraphicsObject getGraphics() {
        var positioningGroup = new GraphicsGroup();
        var bounds = graphics.getBounds();
        positioningGroup.add(graphics, -bounds.getMinX(), -bounds.getMinY());
        return positioningGroup;
    }
}
