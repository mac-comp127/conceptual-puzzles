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

    public static void applyLabelFont(GraphicsText label) {
        label.setFont("Helvetica Neue, Tahoma, sans serif", FontStyle.PLAIN, 24);
        label.setFillColor(Color.LIGHT_GRAY);
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
        label.setFillColor(connectingLineColor(hue, true));
    }

    public static Color connectingLineColor(float hue) {
        return connectingLineColor(hue, false);
    }

    public static Color connectingLineColor(float hue, boolean brighter) {
        return Color.getHSBColor(hue, 0.5f, brighter ? 1.0f : 0.8f);
    }

    public static <GraphicsType extends Fillable & Strokable> void applyBoxStyle(GraphicsType box) {
        box.setFillColor(Color.BLACK);
        box.setStrokeColor(Color.LIGHT_GRAY);
        box.setStrokeWidth(1);
    }

    public static void applyDividerStyle(Strokable divider) {
        divider.setStrokeColor(Color.GRAY);
        divider.setStrokeWidth(1);
    }
}
