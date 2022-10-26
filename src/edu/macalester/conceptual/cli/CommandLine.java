package edu.macalester.conceptual.cli;

import java.io.PrintWriter;
import java.text.MessageFormat;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.InvalidPuzzleCodeException;
import edu.macalester.conceptual.context.PuzzleContext;

public class CommandLine {
    public static void main(String[] args) throws InvalidPuzzleCodeException {
        var options = new PuzzleOptions(args);

        if (options.help() || options.commandAndArgs().isEmpty()) {
            printHelp(options, false);
            return;
        }

        try {
            String command = options.commandAndArgs().get(0);
            switch(command) {
                case "help" -> {
                    printHelp(options, true);
                }
                case "list" -> {
                    listPuzzles(options);
                }
                case "gen" -> {
                    generate(options);
                }
                case "solve" -> {
                    solve(options);
                }
                default -> options.usageError("Unknown command: " + command);
            }
        } catch(RuntimeException e) {
            e.printStackTrace();
            System.err.println();
            System.err.println("Command line args: " + String.join(" ", args));
            System.err.println();
        }

        if (System.getenv().containsKey("PUZZLE_EXIT_IMMEDIATELY")) {
            System.exit(0);  // so integration tests don't hang waiting for CanvasWindows to close
        }
    }

    private static void generate(PuzzleOptions options) {
        requireCommandArgs(1, options);
        var puzzleName = options.commandAndArgs().get(1);
        var puzzle = Puzzle.findByName(puzzleName);
        if(puzzle == null) {
            System.err.println("Unknown puzzle type: " + puzzleName);
            System.err.println("Use `puzzle list` to see available options");
            return;
        }

        var ctx = PuzzleContext.generate(
            puzzle.id(),
            options.difficulty() != null
                ? options.difficulty()
                : puzzle.goalDifficulty());
        applyOptionsToContext(options, ctx, puzzle);
        emitPuzzle(puzzle, ctx, options);

        System.out.println();
        System.out.println("Puzzle code: \u001b[7m " + ctx.getPuzzleCode() + " \u001b[0m");
        System.out.println();
        System.out.println("To see solution:");
        System.out.println("  " + executableName() + " solve " + ctx.getPuzzleCode());
    }

    private static void solve(PuzzleOptions options) throws InvalidPuzzleCodeException {
        requireCommandArgs(1, options);
        var ctx = PuzzleContext.fromPuzzleCode(options.commandAndArgs().get(1));
        var puzzle = Puzzle.findByID(ctx.getPuzzleID());
        if(puzzle == null) {
            System.err.println("This puzzle code refers to a puzzle type that no longer exists.");
            System.err.println("Are you using an outdated code from a previous semester?");
            return;
        }

        applyOptionsToContext(options, ctx, puzzle);
        ctx.enableSolution();
        emitPuzzle(puzzle, ctx, options);

        if (ctx.getDifficulty() != puzzle.goalDifficulty()) {
            System.out.println(MessageFormat.format(
                """
                ***************** PLEASE NOTE ******************
                ***                                          ***
                *** The puzzle above has a difficulty of {0}.  ***
                *** The difficulty level to get credit is {1}. ***
                ***                                          ***
                ************************************************

                To try the puzzle at the goal difficulty, generate a puzzle without the --difficulty option.
                """,
                ctx.getDifficulty(),
                puzzle.goalDifficulty()));
        }
        if (ctx.getDifficulty() > puzzle.minDifficulty()
            && puzzle.minDifficulty() < puzzle.goalDifficulty()
        ) {
            System.out.println("Want to practice more basics first? Try a simpler puzzle:");
            System.out.println();
            System.out.println("  " + executableName() + " gen " + puzzle.name()
                + " --difficulty " + (ctx.getDifficulty() - 1));
            System.out.println();
        }
        if (ctx.getDifficulty() < puzzle.maxDifficulty()) {
            System.out.println("Want a bigger challenge? Try a harder difficulty level:");
            System.out.println();
            System.out.println("  " + executableName() + " gen " + puzzle.name()
                + " --difficulty " + (ctx.getDifficulty() + 1));
            System.out.println();
        }
    }

    private static void applyOptionsToContext(PuzzleOptions options, PuzzleContext ctx, Puzzle puzzle) {
        if (options.includeSolutions()) {
            ctx.enableSolution();
        }

        if (ctx.getDifficulty() < puzzle.minDifficulty() || ctx.getDifficulty() > puzzle.maxDifficulty()) {
            System.err.println("Illegal difficult level: " + ctx.getDifficulty());
            System.err.println("The `" + puzzle.name() + "` puzzle must have a difficulty in the range "
                + puzzle.minDifficulty() + "..." + puzzle.maxDifficulty() + ".");
            System.err.println("(The difficulty level to get credit is " + puzzle.goalDifficulty() + ".)");
            System.exit(0);
        }

        ctx.setPartsToShow(options.partsToShow());
    }

    private static void printHelp(PuzzleOptions options, boolean forceFullHelp) {
        PrintWriter out = new PrintWriter(System.err, true);
        printCommands(out);
        if (forceFullHelp || options.help()) {
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

    private static void listPuzzles(PuzzleOptions options) {
        requireCommandArgs(0, options);

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
                if (repeat > 1) {
                    ctx.resetSectionCounter();
                }
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
              %1$s gen loops
              %1$s solve 1454-1234-1234
              %1$s gen loops --part 3 --repeat 5
            """.trim().formatted(executableName()));
    }
}
