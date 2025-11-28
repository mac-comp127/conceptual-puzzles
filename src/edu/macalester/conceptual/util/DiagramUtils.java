package edu.macalester.conceptual.util;

import java.awt.Color;
import java.util.List;

import edu.macalester.graphics.Fillable;
import edu.macalester.graphics.FontStyle;
import edu.macalester.graphics.GraphicsText;
import edu.macalester.graphics.Path;
import edu.macalester.graphics.Point;
import edu.macalester.graphics.Strokable;

public class DiagramUtils {
    public static final double ARROW_WIDTH = 12, ARROW_LEN = 12;

    public static void applyNodeLabelFont(GraphicsText label) {
        label.setFont("SF Mono, Menlo, Consolas, monospaced", FontStyle.BOLD, 24);
        label.setFillColor(Color.WHITE);  // diagrams are always in dark mode
    }

    public static void applyNodeDetailFont(GraphicsText label) {
        applyNodeLabelFont(label);
        label.setFontStyle(FontStyle.PLAIN);
    }

    public static Path makeArrowhead(Point tip, boolean hollow, float hue) {
        var arrowHead = new Path(
            List.of(
                tip.add(new Point(-ARROW_LEN, -ARROW_WIDTH / 2)),
                tip,
                tip.add(new Point(-ARROW_LEN, ARROW_WIDTH / 2))
            ), hollow
        );
        DiagramUtils.applyArrowStrokeStyle(arrowHead, hue);
        if (hollow) {
            arrowHead.setFillColor(Color.BLACK);
        }
        return arrowHead;
    }

    public static void applyArrowStrokeStyle(Strokable path, float hue) {
        path.setStrokeColor(connectingLineColor(hue));
        path.setStrokeWidth(2);
    }

    public static void applyArrowLabelFont(GraphicsText label, float hue) {
        label.setFont("Helvetica Neue, Tahoma, sans serif", FontStyle.ITALIC, 18);
        label.setFillColor(connectingLineColor(hue));
    }

    public static Color connectingLineColor(float hue) {
        return Color.getHSBColor(hue, 0.2f, 0.7f);
    }

    public static <GraphicsType extends Fillable & Strokable> void applyBoxStyle(GraphicsType box) {
        box.setFillColor(Color.BLACK);
        box.setStrokeColor(Color.GRAY);
        box.setStrokeWidth(1);
    }

    public static void applyDividerStyle(Strokable divider) {
        divider.setStrokeColor(Color.DARK_GRAY);
        divider.setStrokeWidth(1);
    }
}
