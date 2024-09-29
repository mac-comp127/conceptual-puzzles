package edu.macalester.conceptual.context;

import com.google.common.html.HtmlEscapers;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import edu.macalester.graphics.GraphicsGroup;
import edu.macalester.graphics.GraphicsObject;
import edu.macalester.graphics.Point;

/**
 * Handles HTML puzzle output. See also {@link ConsolePuzzlePrinter}.
 */
public class HtmlPuzzlePrinter implements PuzzlePrinter {

    private final PrintWriter out;
    private int silenceLevel;
    private float hue;
    private boolean copyPasteObfuscation;
    private Random obfuscationRand = new Random(0);

    private final TextFormatter textFormatter = new TextFormatter(
        /* code */        new TextFormatter.Style("<code>", "</code>"),
        /* bold */        new TextFormatter.Style("<b>", "</b>"),
        /* italics */     new TextFormatter.Style("<i>", "</i>"),
        /* placeholder */ new TextFormatter.Style("<span class='placeholder'>", "</span>"));

    public HtmlPuzzlePrinter() {
        this(System.out);
    }

    public HtmlPuzzlePrinter(OutputStream out) {
        this(new PrintWriter(out, false, StandardCharsets.UTF_8));
    }

    public HtmlPuzzlePrinter(PrintWriter out) {
        this.out = out;
        out.write(DOC_PREFIX);
    }

    /**
     * Inserts text fragments throughout code blocks that aren’t visible in the browser, but show up
     * when copying text out of the browser. The hope is to discourage students from casually
     * bending the rules about not using the computer for help.
     */
    public void enableCopyPasteObfuscation() {
        copyPasteObfuscation = true;
    }

    @Override
    public void title(String title) {
        colorHeading("h1", title);
    }

    @Override
    public void heading(String text, boolean primary) {
        colorHeading(primary ? "h2" : "h3", text);
    }

    private void colorHeading(String tag, String text) {
        wrapInTag(
            tag,
            "style='color: " + htmlColor(Color.getHSBColor(hue, 0.7f, 1f)) + "'",
            () -> out.write(
                processText(text)));
    }

    @Override
    public void dividerLine(boolean primary) {
        out.println();
        out.println("<hr class='" + (primary ? "primary" : "secondary" ) + "'>");
        out.println();
    }

    @Override
    public void paragraph(String formatString, Object... formatArguments) {
        wrapInTag("p",
            () -> out.write(
                processText(
                    MessageFormat.format(formatString, formatArguments))));
    }

    @Override
    public void bulletList(String... items) {
        wrapInTag("ul", () -> {
            for (var item : items) {
                wrapInTag("li", () -> out.write(
                    processText(item)));
            }
        });
    }

    @Override
    public void numberedList(String... items) {
        wrapInTag("ol", () -> {
            for (var item : items) {
                wrapInTag("li", () -> out.write(
                    processText(item)));
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
            () -> out.write(
                processText(text)));
    }

    @Override
    public void codeBlock(String javaCode) {
        wrapInTag("pre",
            () -> out.write(
                processCode(javaCode)));
    }

    @Override
    public void showGraphics(String title, GraphicsObject graphics) {
        // Create bitmap
        int width = (int) Math.ceil(graphics.getWidth());
        int height = (int) Math.ceil(graphics.getHeight());
        int scale = 3;
        BufferedImage image = new BufferedImage(
            width * scale,
            height * scale,
            BufferedImage.TYPE_INT_ARGB);

        // Set up positioning & retina scaling
        GraphicsGroup scaledGroup = new GraphicsGroup();
        scaledGroup.add(
            graphics,
            -graphics.getBounds().getMinX(),  // Some graphics extend into negative coords
            -graphics.getBounds().getMinY());
        scaledGroup.setScale(scale);
        scaledGroup.setAnchor(Point.ORIGIN);

        // Render graphic
        scaledGroup.renderToBuffer(image);
        
        // Encode to PNG
        ByteArrayOutputStream pngBytes = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", pngBytes);
        } catch (IOException e) {
            throw new RuntimeException("Unable to encode png", e);
        }

        // Embed PNG in <img> tag as data URL
        out.write("<img width='" + width + "' height='" + height + "'");
        out.write("src='data:image/png;base64,");
        out.write(Base64.getEncoder().encodeToString(pngBytes.toByteArray()));
        out.write("'>");
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

    private String processText(String text) {
        return textFormatter.format(
            htmlEscape(text));
    }

    private String processCode(String code) {
        return textFormatter.formatCodePlaceholders(
            insertCopyPasteObfuscation(
                htmlEscape(
                    code.trim())));
    }

    private String htmlEscape(String text) {
        return
            Pattern.compile("[^\0-~]")
                .matcher(
                    HtmlEscapers.htmlEscaper().escape(text))  // only handles ASCII chars: < & etc
                .replaceAll(match ->
                    "&#" + Character.codePointAt(match.group(), 0) + ";");  // handle non-ASCII
    }

    private String insertCopyPasteObfuscation(String html) {
        // This pattern prevents obfuscation text from being placed inside:
        //  -  ___placeholders__
        //  -  &entities;
        //  -  &#hexentities;
        //  -  indentation

        return Pattern.compile("(?<!(___|&#?)\\w*)(?! |\\w*(___))").matcher(html).replaceAll(blank -> {
            if (copyPasteObfuscation && obfuscationRand.nextInt(4) == 0) {
                String obfuscationText =
                    (obfuscationRand.nextInt(10) == 0)
                        ? " Remember: using the computer to help is strictly prohibited"
                            + " for official attempts! The answer has to come from your own head. "
                        : ("" + (char) (' ' + obfuscationRand.nextInt('~' - ' ' + 1)))
                            .replace("$", "\\$");
                return "<div class='hidden-warning' aria-hidden='true'>" + obfuscationText + "</div>";
            } else {
                return "";
            }
        });
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
                    html {
                        background: #222;
                        color: #bbb;
                        font-size: 13pt;
                        font-family: Charter, Georgia, serif;
                        padding: 1em;
                    }
                    body {
                        max-width: 42em;
                        margin: 0 auto;
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
                        overflow-x: scroll;
                        scrollbar-width: none;
                    }
                    code {
                        background: #444;
                        padding: 0 0.5ex;
                        margin: 0 0.125ex;
                        border: 0.5px solid #777;
                        border-radius: 0.25ex;
                    }
                    .placeholder {
                        display: inline-block;
                        padding: 0 2ex;
                        margin: 1px 0;
                        border-radius: 1ex;
                        background: #999;
                        color: #000;
                    }
                    p {
                        padding: 0;
                        margin: 1em 0;
                    }
                    b {
                        color: #eee;
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
                    .hidden-warning {
                        display: inline-block;
                        margin: -1ex -1px;
                        width: 2px;
                        overflow: hidden;
                        color: transparent;
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
