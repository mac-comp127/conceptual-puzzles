package edu.macalester.conceptual;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import edu.macalester.conceptual.context.ConsolePuzzlePrinter;
import edu.macalester.conceptual.context.PuzzlePrinter;

public class TestPuzzlePrinters {
    public static PuzzlePrinter toStdout() {
        return new ConsolePuzzlePrinter(new PrintWriter(System.out, true, StandardCharsets.UTF_8));
    }

    public static PuzzlePrinter silent() {
        return new ConsolePuzzlePrinter(new PrintWriter(new StringWriter()));
    }
}
