package edu.macalester.conceptual.util;

import java.awt.Color;

import edu.macalester.graphics.FontStyle;
import edu.macalester.graphics.GraphicsText;

public class DiagramUtils {
    public static void applyNodeLabelFont(GraphicsText label) {
        label.setFont("SF Mono, Menlo, Consolas, monospaced", FontStyle.BOLD, 24);
        label.setFillColor(Color.WHITE);  // diagrams are always in dark mode
    }

    public static void applyArrowLabelFont(GraphicsText label, float hue) {
        label.setFont("Helvetica Neue, Tahoma, sans serif", FontStyle.ITALIC, 18);
        label.setFillColor(connectingLineColor(hue));
    }

    public static Color connectingLineColor(float hue) {
        return Color.getHSBColor(hue, 0.2f, 0.7f);
    }
}
