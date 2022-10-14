package edu.macalester.conceptual.context;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.Statement;

import java.awt.Color;
import java.io.Closeable;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import static edu.macalester.conceptual.util.CodeFormatting.ELIDED;
import static edu.macalester.conceptual.util.CodeFormatting.prettify;

public class PuzzlePrinter implements Closeable {
    private final PrintWriter out;
    private String indent = "";
    private float hue;

    public PuzzlePrinter() {
        out = new PrintWriter(System.out);
    }
    public PuzzlePrinter(PrintWriter writer) {
        out = writer;
    }

    public void dividerLine(boolean primary) {
        println((primary ? "━" : "┄").repeat(80));
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
            ansiCode('m', 1);  // bold
            if (primary) {
                textColor(Color.BLACK, true);
                textColor(Color.getHSBColor(hue, 0.8f, 1), false);
            } else {
                textColor(Color.getHSBColor(hue, 0.6f, 1), true);
                textColor(Color.getHSBColor(hue, 0.5f, 0.2f), false);
            }
            print(line);
            resetAnsiStyling();
            println();
        }

        println();
    }

    public void paragraph(String s) {
        println(s.strip());
        println();
    }

    public void println() {
        println("");
    }

    public void codeBlock(Node astNode) {
        codeBlock(prettify(astNode));
    }

    public void codeBlock(String javaCode) {
        indented(() -> {
            println(javaCode.strip());
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

    private void println(String str) {
        print(str + "\n");
    }

    // Handles indentation and line break normalization
    private void print(String str) {
        out.print(
            str
                .replaceAll("^|\\r?\\n(?!$)", "$0" + indent)
                .replaceAll("\\r?\\n", System.lineSeparator()));
    }

    public void setColorTheme(float hue) {
        this.hue = ((hue % 1) + 1) % 1;
    }

    private void textColor(Color color, boolean foreground) {
        String terminalColorMode = System.getenv("COLORTERM");
        if (terminalColorMode != null && terminalColorMode.matches("truecolor|24bit")) {
            // 24-bit (true color) ANSI code
            // Only some terminals support it (VS Code = yes, Apple Terminal = no)
            ansiCode('m', foreground ? 38 : 48, 2, color.getRed(), color.getGreen(), color.getBlue());
        } else {
            // 256-color ANSI code: better compatibility
            ansiCode(
                'm',
                foreground ? 38 : 48,
                5,
                16  + scale256To6(color.getBlue())
                    + scale256To6(color.getGreen()) * 6
                    + scale256To6(color.getRed()) * 36);
        }
    }

    private int scale256To6(int component) {
        return component * 6 / 256;
    }

    private void resetAnsiStyling() {
        ansiCode('m', 0);
    }

    private void colorTest() {
        for(float b = 0; b < 1; b += 0.2) {
            for(float s = 0; s < 1; s += 0.03) {
                for(float h = 0; h < 1; h += 0.02) {
                    textColor(Color.getHSBColor(h, s, b), false);
                    out.print(' ');
                }
                resetAnsiStyling();
                out.println();
            }
            out.println();
        }
    }

    private void ansiCode(char terminator, int... params) {
        out.print(
            "\u001b["
                + Arrays.stream(params)
                    .mapToObj(String::valueOf)
                    .collect(Collectors.joining(";"))
                + terminator);
    }

    public void close() {
        ansiCode('m', 0);  // restore normal colors
        out.println();     // clear any dangling indents
        out.flush();       // do not close; this may close System.out!!
    }

    public static void main(String[] args) {
        try (PuzzlePrinter output = new PuzzlePrinter()) {
            output.colorTest();
        }
    }
}
