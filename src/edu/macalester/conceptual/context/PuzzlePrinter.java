package edu.macalester.conceptual.context;

import com.github.javaparser.ast.Node;

import java.awt.Color;
import java.io.Closeable;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import static edu.macalester.conceptual.util.CodeFormatting.prettify;

public class PuzzlePrinter implements Closeable {
    private final boolean colorCode = false;  // print code in color? (doesn't handle white BG well)
    private final PrintWriter out;
    private String indent = "";
    private boolean atStartOfLine = true;
    private float hue;
    private int silenceLevel;

    public PuzzlePrinter() {
        out = new PrintWriter(System.out);
    }
    public PuzzlePrinter(PrintWriter writer) {
        out = writer;
    }

    public void dividerLine(boolean primary) {
        println((primary ? "━" : "┄").repeat(80));
        println();
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
    }

    public void paragraph(String s) {
        printFormattedText(s);
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
            .replaceAll("`([^`]+)`", codeStyle + "$1" + endCodeStyle));
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

        indented(() -> {
            print(codeStyle);
            print(javaCode.strip());
            println(endCodeStyle);
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

    public void silence() {
        silenceLevel--;
    }

    public void unsilence() {
        silenceLevel++;
    }

    public void println() {
        println("");
    }

    private void println(String str) {
        print(str);
        print("\n");
    }

    // Handles indentation and line break normalization
    private void print(String str) {
        if (silenceLevel < 0) {
            return;
        }
        for (String part : str.split("(?=\\r?\\n)|(?<=\\n)")) { // lines + terminators as separate matches
            if (part.endsWith("\n")) {
                atStartOfLine = true;
                out.println();
            } else {
                if (atStartOfLine) {
                    atStartOfLine = false;
                    out.print(indent);
                }
                out.print(part);
            }
        }
    }

    public void setColorTheme(float hue) {
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
