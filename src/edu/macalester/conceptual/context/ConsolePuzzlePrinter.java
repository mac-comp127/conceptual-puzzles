package edu.macalester.conceptual.context;

import java.awt.Color;
import java.awt.Toolkit;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import edu.macalester.graphics.CanvasWindow;
import edu.macalester.graphics.GraphicsObject;


/**
 * Provides console output facilities for puzzles. Output uses ANSI escape codes for color and
 * text styling, and applies word wrapping. See also {@link HtmlPuzzlePrinter}.
 */
public class ConsolePuzzlePrinter implements PuzzlePrinter {
    private final PrintWriter out;

    private int curColumn = 0, outputWidth;
    private boolean wordWrapEnabled = true;
    private String indent = "";

    private float hue;

    private int silenceLevel;

    private final TextFormatter textFormatter = new TextFormatter(
        /* code */        new TextFormatter.Style(ansiCode('m', 4), ansiCode('m', 24)),
        /* bold */        new TextFormatter.Style(ansiCode('m', 1), ansiCode('m', 22)),
        /* italics */     new TextFormatter.Style(ansiCode('m', 3), ansiCode('m', 23)),
        /* placeholder */ new TextFormatter.Style(
            ansiCode('m', 100) + ansiCode('m', 97) + "  ",
            "  " + ansiCode('m', 39) + ansiCode('m', 49)));

    public ConsolePuzzlePrinter(PrintWriter writer) {
        out = writer;
        try {
            outputWidth = Integer.parseInt(System.getenv("COLUMNS"));
        } catch(Exception e) {
            outputWidth = 80;
        }

        // Clear to end of screen, to mop up any dangling bits of gradle’s progress bar
        print(ansiCode('J', 0));
    }

    @Override
    public void close() {
        out.print(ansiCode('m', 0));  // restore normal colors
        out.println();     // clear any dangling indents
        out.flush();       // flush, don’t close; that could close System.out!!
    }

    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––
    // Public Output API
    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––


    @Override
    public void title(String title) {
        // no title needed on console; Puzzle type is right there already
    }

    @Override
    public void dividerLine(boolean primary) {
        nowrap(() -> {
            println((primary ? "─" : "┄").repeat(outputWidth));
            println();
        });
    }

    @Override
    public void heading(String text, boolean primary) {
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

    @Override
    public void paragraph(String formatString, Object... formatArguments) {
        printFormattedText(MessageFormat.format(formatString, formatArguments));
        println();
    }

    @Override
    public void bulletList(String... items) {
        for (String item : items) {
            nowrap(() -> print("  - "));
            indented("    ", () -> printFormattedText(item));
        }
        println();
    }

    @Override
    public void numberedList(String... items) {
        numberedList(
            Arrays.stream(items)
                .map(t -> (Runnable) () -> printFormattedText(t))
                .toList());
        println();  // need a blank line here because nested items won’t emit them
    }

    @Override
    public void numberedList(List<Runnable> items) {
        int n = 0;
        for (Runnable item : items) {
            var itemMarker = String.format("%2d. ", ++n);
            nowrap(() -> print(itemMarker));
            indented("    ", item);
        }
        // no blank line here; nested items will have already generated it
    }

    @Override
    public void blockquote(String s) {
        indented("  │ ", () -> printFormattedText(s));
        println();
    }

    private void printFormattedText(String s) {
        println(textFormatter.format(s));
    }

    @Override
    public void codeBlock(String javaCode) {
        nowrap(() -> {
            indented(() -> {
                println(
                    textFormatter.formatCodePlaceholders(
                        javaCode.strip()));
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

    @Override
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
        window.setBackground(new Color(0x222222));
        window.draw();
    }

    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––
    // Silencing
    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––

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

        str = str.replace("\r\n", "\n"); // normalize Windows CRLF line breaks
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

    public float themeHue() {
        return hue;
    }

    public void setThemeHue(float hue) {
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
        try (var output = new ConsolePuzzlePrinter(new PrintWriter(System.out, true, StandardCharsets.UTF_8))) {
            output.colorTest();
        }
    }
}
