package edu.macalester.conceptual.cli;

import java.io.PrintWriter;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.InvalidPuzzleCodeException;
import edu.macalester.conceptual.context.PuzzleContext;

public class CommandLine {
    public static void main(String[] args) throws InvalidPuzzleCodeException {
        var options = new PuzzleOptions(args);

        if (options.help() || options.commandAndArgs().isEmpty()) {
            printHelp(options);
            return;
        }

        String command = options.commandAndArgs().get(0);
        switch(command) {
            case "list" -> {
                requireCommandArgs(0, options);
                listPuzzles();
            }
            case "gen" -> {
                requireCommandArgs(1, options);
                var puzzleName = options.commandAndArgs().get(1);
                var puzzle = Puzzle.find(puzzleName, Puzzle::name, "name");
                if(puzzle == null) {
                    System.err.println("Unknown puzzle type: " + puzzleName);
                    System.err.println("Use `puzzle list` to see available options");
                    return;
                }

                var ctx = PuzzleContext.generate(puzzle.id());
                if (options.includeSolutions()) {
                    ctx.enableSolution();
                }
                emitPuzzle(puzzle, ctx, options);

                System.out.println();
                System.out.println("Puzzle code: \u001b[7m " + ctx.getPuzzleCode() + " \u001b[0m");
                System.out.println();
                System.out.println("To see solution:");
                System.out.println("  " + executableName() + " solve " + ctx.getPuzzleCode());
            }
            case "solve" -> {
                requireCommandArgs(1, options);
                var ctx = PuzzleContext.fromPuzzleCode(options.commandAndArgs().get(1));
                var puzzle = Puzzle.find(ctx.getPuzzleID(), Puzzle::id, "id");

                ctx.enableSolution();
                emitPuzzle(puzzle, ctx, options);
            }
            default -> options.usageError("Unknown command: " + command);
        }
    }

    private static void printHelp(PuzzleOptions options) {
        PrintWriter out = new PrintWriter(System.err, true);
        printCommands(out);
        if (options.help()) {
            options.printOptions(out);
            printExamples(out);
        } else {
            out.println("To see all options:");
            out.println("  " + executableName() + " --help");
        }
    }

    public static String executableName() {
        // wrapper script passes the name + path with which it was invoked
        return System.getenv().getOrDefault("puzzle_command", "puzzle");
    }

    private static void requireCommandArgs(int expectedArgCount, PuzzleOptions options) {
        int actualArgCount = options.commandAndArgs().size() - 1;
        if (actualArgCount != expectedArgCount) {
            options.usageError(
                "Expected " + expectedArgCount
                    + " argument" + (expectedArgCount == 1 ? "" : "s")
                    + " for '" + options.commandAndArgs().get(0)
                    + "' command, but got " + actualArgCount + ": "
                    + options.commandAndArgs().subList(1, options.commandAndArgs().size()));
        }
    }

    private static void listPuzzles() {
        System.out.println("Available puzzle types:");
        System.out.println();

        int nameWidth = Puzzle.ALL.stream()
            .map(Puzzle::name)
            .mapToInt(String::length)
            .max().orElse(0);

        for (var puzzle : Puzzle.ALL) {
            System.out.printf("  %-" + nameWidth + "s  %s",
                puzzle.name(),
                puzzle.description());
            System.out.println();
        }
        System.out.println();
        System.out.println("To generate a puzzle:");
        System.out.println("  " + executableName() + " gen <puzzletype>");
    }

    private static void emitPuzzle(Puzzle puzzle, PuzzleContext ctx, PuzzleOptions options) {
        ctx.emitPuzzle(() -> {
            for (int repeat = options.repeat(); repeat > 0; repeat--) {
                puzzle.generate(ctx);
            }
        });
    }

    public static void printCommands(PrintWriter out) {
        out.println(
            """
            Commands:
              puzzle list           List available puzzle types
              puzzle gen <type>     Generate a new puzzle
              puzzle solve <code>   Print the solution to a puzzle
            """);
    }

    public static void printExamples(PrintWriter out) {
        out.println(
            """
            Usage examples:
              puzzle gen loops
              puzzle solve 1454-1234-1234
              puzzle gen loops --part 3 --repeat 5
            """.trim());
    }
}
