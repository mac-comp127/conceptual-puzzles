package edu.macalester.conceptual.context;

import com.github.javaparser.ast.Node;

import java.awt.Color;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;

import edu.macalester.graphics.GraphicsObject;

public class HtmlPuzzlePrinter implements PuzzlePrinter {

    private final PrintWriter out;
    private int silenceLevel;
    private float hue;

    private final TextFormatter textFormatter = new TextFormatter(
        /* code */    new TextFormatter.Style("<code>", "</code>"),
        /* bold */    new TextFormatter.Style("<b>", "</b>"),
        /* italics */ new TextFormatter.Style("<i>", "</i>"));

    public HtmlPuzzlePrinter() {
        this(new PrintWriter(System.out, false, StandardCharsets.UTF_8));
    }

    public HtmlPuzzlePrinter(PrintWriter out) {
        this.out = out;
        out.write(DOC_PREFIX);
    }

    @Override
    public void dividerLine(boolean primary) {
        out.println();
        out.println("<hr class='" + (primary ? "primary" : "secondary" ) + "'>");
        out.println();
    }

    @Override
    public void heading(String text, boolean primary) {
        wrapInTag(
            primary ? "h1" : "h2",
            "style='color: " + htmlColor(Color.getHSBColor(hue, 0.8f, 1f)) + "'",
            () -> out.write(text));
    }

    @Override
    public void paragraph(String formatString, Object... formatArguments) {
        wrapInTag("p",
            () -> out.write(
                textFormatter.format(
                    MessageFormat.format(formatString, formatArguments))));
    }

    @Override
    public void bulletList(String... items) {
        wrapInTag("ul", () -> {
            for (var item : items) {
                wrapInTag("li", () -> out.write(
                    textFormatter.format(item)));
            }
        });
    }

    @Override
    public void numberedList(String... items) {
        wrapInTag("ol", () -> {
            for (var item : items) {
                wrapInTag("li", () -> out.write(
                    textFormatter.format(item)));
            }
        });
    }

    @Override
    public void numberedList(List<Runnable> items) {
        wrapInTag("ol", () -> {
            for (var item : items) {
                wrapInTag("li", item);
            }
        });
    }

    @Override
    public void blockquote(String text) {
        wrapInTag("blockquote",
            () -> out.write(text));
    }

    @Override
    public void codeBlock(String javaCode) {
        wrapInTag("pre", () -> out.write(javaCode.trim()));
    }

    @Override
    public void showGraphics(String title, GraphicsObject graphics) {
        throw new UnsupportedOperationException("HTML output does not support graphics");
    }

    @Override
    public void silence() {
        silenceLevel--;
    }

    @Override
    public void unsilence() {
        silenceLevel++;
    }

    @Override
    public boolean isSilenced() {
        return silenceLevel < 0;
    }

    @Override
    public float themeHue() {
        return hue;
    }

    @Override
    public void setThemeHue(float hue) {
        this.hue = hue;
    }

    private void wrapInTag(String tag, Runnable body) {
        wrapInTag(tag, null, body);
    }

    private void wrapInTag(String tag, String attrs, Runnable body) {
        out.write("<" + tag);
        if (attrs != null) {
            out.write(" " + attrs);
        }
        out.write(">");
        body.run();
        out.write("</" + tag + ">");
        out.println();
    }

    private String htmlColor(Color color) {
        return String.format("#%06x", color.getRGB() & 0xFFFFFF);
    }

    @Override
    public void close() {
        out.write(DOC_SUFFIX);
        out.flush();
    }

    private static final String
        DOC_PREFIX =
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Conceptual Mastery Puzzle</title>
                <style type="text/css">
                    body {
                        background: #222;
                        color: #bbb;
                        font-size: 14pt;
                        font-family: Palatino, Georgia, serif;
                        max-width: 42em;
                        margin: 1em auto;
                    }
                    h1, h2, h3 {
                        font-family: 'Helvetica Neue', Helvetica, Arial, sans;
                        font-weight: bold;
                    }
                    pre, code {
                        font-size: 90%;
                        font-family: Menlo, Consolas, monospace;
                    }
                    pre, code, blockquote {
                        color: #eee;
                    }
                    code {
                        background: #444;
                        padding: 0 0.5ex;
                        margin: 0 0.125ex;
                        border: 0.5px solid #777;
                        border-radius: 0.25ex;
                    }
                    p {
                        padding: 0;
                        margin: 1em 0;
                    }
                    pre, blockquote {
                        margin: 1em 1em 1em 2em;
                    }
                    hr {
                        border: none;
                    }
                    hr.primary {
                        border-top: 1px solid rgba(255, 255, 255, 0.8);
                    }
                    hr.secondary {
                        border-top: 1px solid rgba(255, 255, 255, 0.4);
                    }
                </style>
            </head>
            <body>
            """,

    DOC_SUFFIX =
            """
            </body>
            </html>
            """;
}
