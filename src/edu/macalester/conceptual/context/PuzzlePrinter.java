package edu.macalester.conceptual.context;

import com.github.javaparser.ast.Node;

import java.awt.Color;
import java.io.Closeable;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import static edu.macalester.conceptual.util.CodeFormatting.*;

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

    public void dividerLine(boolean primary) {
        nowrap(() -> {
            println((primary ? "─" : "┄").repeat(outputWidth));
            println();
        });
    }

    public void heading(String s, boolean primary) {
        if (primary) {
            hue += 0.382;
            hue %= 1;
        }

        var lines = new ArrayList<String>();
        String sideMargin = "   ";
        String center = sideMargin + s.toUpperCase() + sideMargin;
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

    public void paragraph(String s, Object... formatArguments) {
        printFormattedText(MessageFormat.format(s, formatArguments));
        println();
    }


    public void bulletList(String... items) {
        for (String item : items) {
            nowrap(() -> print("  - "));
            indented("    ", () -> printFormattedText(item));
        }
        println();
    }

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

    public void codeBlock(Node astNode) {
        codeBlock(prettify(astNode));
    }

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

    public void indented(Runnable block) {
        indented("    ", block);
    }

    public void indented(String newIndent, Runnable block) {
        String prevIndent = indent;
        try {
            indent += newIndent;
            block.run();
        } finally {
            indent = prevIndent;
        }
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

    public void silence() {
        silenceLevel--;
    }

    public void unsilence() {
        silenceLevel++;
    }

    public boolean isSilenced() {
        return silenceLevel < 0;
    }

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
        for (String part : str.split("(?=\\r?\\n)|(?<=\\n)")) { // lines + terminators as separate matches
            if (part.endsWith("\n")) {
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

    private int visibleWidth(String word) {
        return word
            .replaceAll("\u001b\\[[0-9;]*[a-z]", "")
            .length();
    }

    private void newline() {
        out.println();
        curColumn = 0;
    }

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

    public void close() {
        out.print(ansiCode('m', 0));  // restore normal colors
        out.println();     // clear any dangling indents
        out.flush();       // do not close; this may close System.out!!
    }

    public static void main(String[] args) {
        try (PuzzlePrinter output = new PuzzlePrinter()) {
            output.colorTest();
        }
    }
}
