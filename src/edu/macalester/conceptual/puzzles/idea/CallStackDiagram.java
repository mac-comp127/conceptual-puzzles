package edu.macalester.conceptual.puzzles.idea;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.macalester.conceptual.util.DiagramUtils;
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
    private final Map<GraphicsObject, List<GraphicsObject>> incomingConnections = new HashMap<>();
    private final float hue;
    private final double marginX = 18, marginY = 12;
    private final double columnWidth = 240;
    private final double baseline;

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
    }

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
        valueRenderings.reversed().forEach(Runnable::run);

        // Size enclosing box and add to appropriate column

        background.setSize(box.getWidth() + marginX, y + marginY);

        GraphicsGroup column = getColumn(columnNum);
        box.setY(column.getBounds().getMinY() - box.getHeight() - marginY * 2);
        column.add(box);

        return box;
    }

    private GraphicsGroup getColumn(int columnNum) {
        return columns.computeIfAbsent(columnNum, __ -> {
            var column = new GraphicsGroup();
            column.setX(columnNum * columnWidth);
            graphics.add(column);
            return column;
        });
    }

    private void renderValue(Variable variable, GraphicsText varLabel, GraphicsGroup box, int columnNum) {
        if (variable.value() instanceof Value.InlineValue inlineValue) {
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

    private void addConnection(GraphicsObject from, GraphicsObject to) {
        incomingConnections
            .computeIfAbsent(to, __ -> new ArrayList<>())
            .add(from);
    }

    private void renderConnections() {
        for (var connection : incomingConnections.entrySet()) {
            var to = connection.getKey();
            int incomingCount = connection.getValue().size();
            int n = incomingCount;
            for (var from : connection.getValue()) {
                n--;
                var startPoint = convertToCanvas(from, new Point(
                    from.getBounds().getMaxX() + marginX / 2,
                    fractionOfYExtent(from, 0.5)));
                var endPoint = convertToCanvas(to, new Point(
                    to.getBounds().getMinX(),
                    fractionOfYExtent(to, (float) (n + 1) / (incomingCount + 1))));
                var path = new Path(
                    List.of(
                        startPoint,
                        startPoint.add(new Point(marginX * 2, 0)),
                        endPoint.subtract(new Point(marginX * 2, 0)),
                        endPoint
                    ),
                    false
                );
                DiagramUtils.applyArrowStrokeStyle(path, hue);
                graphics.add(path);
                graphics.add(DiagramUtils.makeArrowhead(endPoint, false, hue));
            }
        }
    }

    private double fractionOfYExtent(GraphicsObject obj, double fraction) {
        var bounds = obj.getBounds();
        return bounds.getMinY() * (1 - fraction) + bounds.getMaxY() * fraction;
    }

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

    private GraphicsObject getGraphics() {
        var positioningGroup = new GraphicsGroup();
        var bounds = graphics.getBounds();
        positioningGroup.add(graphics, -bounds.getMinX(), -bounds.getMinY());
        return positioningGroup;
    }
}
