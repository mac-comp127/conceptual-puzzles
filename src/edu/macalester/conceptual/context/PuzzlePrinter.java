package edu.macalester.conceptual.context;

import java.io.PrintWriter;

public class PuzzlePrinter {
    private final PrintWriter out = new PrintWriter(System.out);
    private String indent = "";

    public void dividerLine() {
        println("––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––");
    }

    public void heading(String s) {
        println(s.toUpperCase());
    }

    public void paragraph(String s) {
        println(s.strip());
        blankLine();
    }

    public void blankLine() {
        println("");
    }

    public void codeBlock(String javaCode) {
        indented(() ->
            println(javaCode.strip()));
        blankLine();
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

    public void close() {
        out.flush();  // do not close; this may close System.out!!
    }
}
