package edu.macalester.conceptual.context;

import com.github.javaparser.ast.Node;

import java.awt.Color;
import java.awt.Toolkit;
import java.io.Closeable;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import edu.macalester.conceptual.util.CodeFormatting;
import edu.macalester.graphics.CanvasWindow;
import edu.macalester.graphics.GraphicsObject;

import static edu.macalester.conceptual.util.CodeFormatting.*;

/**
 * Provides console output facilities for the puzzle generator. Puzzle use <b>structured output,</b>
 * meaning that all output methods describe the <i>role</i> of the output — heading, paragraph,
 * code block — and there is no directly accessible <code>println()</code> method.
 * <p>
 * Several output methods in this class support <b>textual formatting</b> with the following
 * features:
 * <ul>
 *   <li>Words wrap to the console size.</li>
 *   <li>Single line breaks in the input are ignored, which means that you can use a Java multiline
 *       string in your code with code-appropriate line breaks, and the PuzzlePrinter will reflow
 *       your text to fit the console.</li>
 *   <li>Text enclosed in `backticks` is visually styled as code. This is appropriate for English
 *       text mentioning variable names, short expressions, etc. For whole chunks of code, however,
 *       you should use {@link #codeBlock(String)}.</li>
 * </ul>
 */
public class PuzzlePrinter implements Closeable {
    private final boolean colorCode = false;  // print code in color? (doesn't handle white BG well)
    private final PrintWriter out;

    private int curColumn = 0, outputWidth;
    private boolean wordWrapEnabled = true;
    private String indent = "";

    private float hue;

    private int silenceLevel;

    public PuzzlePrinter() {
        this(new PrintWriter(System.out, true, StandardCharsets.UTF_8));
    }

    public PuzzlePrinter(PrintWriter writer) {
        out = writer;
        try {
            outputWidth = Integer.parseInt(System.getenv("COLUMNS"));
        } catch(Exception e) {
            outputWidth = 80;
        }

        // Clear to end of screen, to mop up any dangling bits of gradle’s progress bar
        print(ansiCode('J', 0));
    }

    public void close() {
        out.print(ansiCode('m', 0));  // restore normal colors
        out.println();     // clear any dangling indents
        out.flush();       // flush, don’t close; that could close System.out!!
    }

    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––
    // Public Output API
    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––

    /**
     * Prints a horizontal divider line to visually separate items.
     *
     * @param primary Determines whether the line is solid / prominent (true) or thin /
     *                light / dashed (false).
     */
    public void dividerLine(boolean primary) {
        nowrap(() -> {
            println((primary ? "─" : "┄").repeat(outputWidth));
            println();
        });
    }

    /**
     * Prints the given text in a visually prominent way.
     *
     * @param primary Determines whether the heading is more (true) or less (false) prominent.
     */
    public void heading(String text, boolean primary) {
        if (primary) {
            hue += 0.382;
            hue %= 1;
        }

        var lines = new ArrayList<String>();
        String sideMargin = "   ";
        String center = sideMargin + text.toUpperCase() + sideMargin;
        String outerSpacing = " ".repeat(center.length());
        lines.add(outerSpacing);
        lines.add(center);
        lines.add(outerSpacing);

        nowrap(() -> {
            for (var line : lines) {
                print(ansiCode('m', 1));  // bold
                if (primary) {
                    print(textColorCode(Color.BLACK, true));
                    print(textColorCode(Color.getHSBColor(hue, 0.8f, 1), false));
                } else {
                    print(textColorCode(Color.getHSBColor(hue, 0.6f, 1), true));
                    print(textColorCode(Color.getHSBColor(hue, 0.5f, 0.2f), false));
                }
                print(line);
                resetAnsiStyling();
                println();
            }

            println();
        });
    }

    /**
     * Emits a paragraph of text, with word wrapping and appropriate inter-paragraph spacing.
     * This method uses <b>textual formatting</b>; see the docs at the top of this class for
     * formatting options.
     * <p>
     * Unlike the other textual methods, this method’s format string and its arguments use
     * {@link MessageFormat}. For example:
     * <p>
     * <code>paragraph("{0} before {1}", "dessert", "dinner")</code>
     * <p>
     * This means that you <b>must escape any single quotes and curly braces</b> that appear in the
     * paragraph:
     * <p>
     * <code>paragraph("This isn''t a square brace: '{'")</code>
     * <p>
     * For apostrophes, it is better to use the curved Unicode apostrophe character than to use a
     * single quote:
     * <p>
     * <code>paragraph("This doesn’t require escaping")</code>
     */
    public void paragraph(String formatString, Object... formatArguments) {
        printFormattedText(MessageFormat.format(formatString, formatArguments));
        println();
    }

    /**
     * Formats the given arguments in a list, each one with its own bullet, with smart word wrapping
     * and appropriate inter-paragraph spacing.
     * <p>
     * This method uses <b>textual formatting</b>; see the docs at the top of this class for
     * formatting options.
     */
    public void bulletList(String... items) {
        for (String item : items) {
            nowrap(() -> print("  - "));
            indented("    ", () -> printFormattedText(item));
        }
        println();
    }

    public void numberedList(String... items) {
        numberedList(
            Arrays.stream(items)
                .map(t -> (Runnable) () -> printFormattedText(t))
                .toList());
    }

    public void numberedList(Runnable... items) {
        numberedList(Arrays.asList(items));
    }

    public void numberedList(List<Runnable> items) {
        int n = 0;
        for (Runnable item : items) {
            var itemMarker = String.format("%2d. ", ++n);
            nowrap(() -> print(itemMarker));
            indented("    ", item);
        }
        // no blank line here; nested items will have already generated it
    }

    /**
     * Prints the given text in an indented block, with smart word wrapping and appropriate
     * inter-paragraph spacing.
     * <p>
     * This method uses <b>textual formatting</b>; see the docs at the top of this class for
     * formatting options.
     */
    public void blockquote(String s) {
        indented("  │ ", () -> printFormattedText(s));
        println();
    }

    private void printFormattedText(String s) {
        String codeStyle, endCodeStyle;
        if (colorCode) {
            codeStyle = textColorCode(Color.getHSBColor(hue, 0.8f, 1), true);
            endCodeStyle = plainTextColorCode();
        } else {
            codeStyle = ansiCode('m', 4);  // underline
            endCodeStyle = ansiCode('m', 24);
        }

        println(s.strip()
            .replaceAll("`([^`]+)`", codeStyle + "$1" + endCodeStyle)  // style `code` snippets
            .replaceAll("(?<!\\n)\\s*\\n\\s*(?!\\n)", " "));  // remove single \n (but preserve \n\n)
    }

    /**
     * Prints the given JavaParser AST as an indented and well-formatted block of code, adding
     * parentheses as necessary to preserve expression tree structures within the AST.
     */
    public void codeBlock(Node astNode) {
        codeBlock(prettify(astNode));
    }

    /**
     * Prints the given string as an indented block of code, <b>as is</b>, neither prettified nor
     * with text formatting. To indent and format code properly, call one of the
     * <code>prettify*</code> methods in {@link CodeFormatting} first.
     */
    public void codeBlock(String javaCode) {
        String codeStyle, endCodeStyle;
        if (colorCode) {
            codeStyle = textColorCode(Color.getHSBColor(hue, 0.5f, 1), true);
            endCodeStyle = plainTextColorCode();
        } else {
            endCodeStyle = codeStyle = "";
        }

        nowrap(() -> {
            indented(() -> {
                print(codeStyle);
                print(javaCode.strip());
                println(endCodeStyle);
            });
        });
        println();
    }

    private void indented(Runnable block) {
        indented("    ", block);
    }

    private void indented(String newIndent, Runnable block) {
        String prevIndent = indent;
        try {
            indent += newIndent;
            block.run();
        } finally {
            indent = prevIndent;
        }
    }

    /**
     * Displays the graphics
     */
    public void showGraphics(String title, GraphicsObject graphics) {
        paragraph(ansiCode('m', 3) + "<< See window titled “" + title + "” >>" + ansiCode('m', 23));
        out.flush();

        double margin = 24;
        var screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double scale = Math.min(
            1, Math.min(
                (screenSize.getWidth() - 50 - margin * 2) / graphics.getWidth(),
                (screenSize.getHeight() - 50 - margin * 2) / graphics.getHeight()));
        var window = new CanvasWindow(
            title,
            (int) Math.ceil(graphics.getWidth() * scale + margin * 2),
            (int) Math.ceil(graphics.getHeight() * scale + margin * 2));
        window.add(graphics, margin, margin);
        graphics.setScale(scale);
        graphics.setAnchor(0, 0);
        window.draw();
    }

    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––
    // Silencing
    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––

    /**
     * Suppresses further output until balanced by a call to {@link #unsilence()}. Calls to
     * silence/unsilence are nestable.
     *
     */
    public void silence() {
        silenceLevel--;
    }

    public void unsilence() {
        silenceLevel++;
    }

    public boolean isSilenced() {
        return silenceLevel < 0;
    }

    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––
    // Raw Output / Word Wrapping
    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––

    private void println() {
        println("");
    }

    private void println(String str) {
        print(str);
        print("\n");
    }

    // Handles indentation and line break normalization
    private void print(String str) {
        if (isSilenced()) {
            return;
        }

        str = str.replace("\r\n", "\n"); // normalize line breaks on Windows
        for (String part : str.split("(?=\n)|(?<=\n)")) { // lines + terminators as separate matches
            if (part.equals("\n")) {
                newline();
            } else {
                var wrappingUnit = wordWrapEnabled
                    ? part.split(" ")
                    : new String[] { part };
                boolean firstUnit = true;
                for (var unit : wrappingUnit) {
                    if (wordWrapEnabled && curColumn + visibleWidth(unit) >= outputWidth) {
                        newline();
                    }

                    if (curColumn == 0) {
                        out.print(indent);
                        curColumn += indent.length();
                    } else if (!firstUnit) {
                        out.print(" ");
                        curColumn++;
                    }

                    out.print(unit);

                    curColumn += visibleWidth(unit);
                    firstUnit = false;
                }
            }
        }
    }

    private void newline() {
        out.println();
        curColumn = 0;
    }

    private int visibleWidth(String word) {
        return word
            .replaceAll("\u001b\\[[0-9;]*[a-z]", "")
            .length();
    }

    private void nowrap(Runnable block) {
        var wasEnabled = wordWrapEnabled;
        try {
            wordWrapEnabled = false;
            block.run();
        } finally {
            wordWrapEnabled = wasEnabled;
        }
    }

    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––
    // Colors & Styling
    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––

    float themeHue() {
        return hue;
    }

    void setThemeHue(float hue) {
        this.hue = ((hue % 1) + 1) % 1;
    }

    private String textColorCode(Color color, boolean foreground) {
        String terminalColorMode = System.getenv("COLORTERM");
        if (terminalColorMode != null && terminalColorMode.matches("truecolor|24bit")) {
            // 24-bit (true color) ANSI code
            // Only some terminals support it (VS Code = yes, Apple Terminal = no)
            return ansiCode('m',
                foreground ? 38 : 48, 2,
                color.getRed(), color.getGreen(), color.getBlue());
        } else {
            // 256-color ANSI code: better compatibility
            return ansiCode(
                'm',
                foreground ? 38 : 48,
                5,
                16  + scale256To6(color.getBlue())
                    + scale256To6(color.getGreen()) * 6
                    + scale256To6(color.getRed()) * 36);
        }
    }

    private String plainTextColorCode() {
        return ansiCode('m', 39) + ansiCode('m', 49);
    }

    private int scale256To6(int component) {
        return component * 6 / 256;
    }

    private void resetAnsiStyling() {
        print(ansiCode('m', 0));
    }

    private void colorTest() {
        for(float b = 0; b < 1; b += 0.2) {
            for(float s = 0; s < 1; s += 0.03) {
                for(float h = 0; h < 1; h += 0.02) {
                    out.print(textColorCode(Color.getHSBColor(h, s, b), false));
                    out.print(' ');
                }
                resetAnsiStyling();
                out.println();
            }
            out.println();
        }
    }

    private static String ansiCode(char terminator, int... params) {
        return "\u001b["
            + Arrays.stream(params)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(";"))
            + terminator;
    }

    public static void main(String[] args) {
        try (PuzzlePrinter output = new PuzzlePrinter()) {
            output.colorTest();
        }
    }
}
