package edu.macalester.conceptual.cli;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Properties;

import com.google.common.io.Files;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.ConsolePuzzlePrinter;
import edu.macalester.conceptual.context.HtmlPuzzlePrinter;
import edu.macalester.conceptual.context.InvalidPuzzleCodeException;
import edu.macalester.conceptual.context.PuzzleContext;

/**
 * The main entry point for the puzzle command line interface. Typically invoked from the
 * <code>bin/puzzle</code> script, which in turn triggers the <code>run-cli</code> Gradle task.
 */
public class CommandLine {
    private final PrintWriter stdout, stderr;  // customizable output for testing

    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––
    // Parsing Commands
    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––

    public static void main(String[] args) {
        new CommandLine(System.out, System.err).invoke(args);
    }

    public CommandLine(OutputStream stdout, OutputStream stderr) {
        this.stdout = new PrintWriter(stdout, true, StandardCharsets.UTF_8);
        this.stderr = new PrintWriter(stderr, true, StandardCharsets.UTF_8);
    }

    public void invoke(String[] args) {
        try {
            var options = new PuzzleOptions(args);

            if (options.version()) {
                printVersion();
                return;
            }

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
            } catch(Exception e) {
                e.printStackTrace();
                stderr.println();
                stderr.println("Command line args: " + String.join(" ", args));
                stderr.println();
                System.exit(1);
            }
        } finally {
            stdout.flush();
            stderr.flush();
        }

        if (System.getenv().containsKey("PUZZLE_EXIT_IMMEDIATELY")) {
            System.exit(0);  // so integration tests don't hang waiting for CanvasWindows to close
        }
    }

    private void requireCommandArgs(int expectedArgCount, PuzzleOptions options) {
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

    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––
    // Generating and Solving Puzzles
    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––

    private void generate(PuzzleOptions options) throws IOException {
        requireCommandArgs(1, options);
        var puzzleName = options.commandAndArgs().get(1);
        var puzzle = Puzzle.findByName(puzzleName);
        if(puzzle == null) {
            stderr.println("Unknown puzzle type: " + puzzleName);
            stderr.println("Use `puzzle list` to see available options");
            return;
        }

        var ctx = PuzzleContext.generate(
            puzzle.id(),
            options.difficulty() != null
                ? options.difficulty()
                : puzzle.goalDifficulty());

        applyOptionsToContext(options, ctx, puzzle, false);
        emitPuzzle(puzzle, ctx, options);

        if (options.solutionHtml() != null) {
            // We need a new context and a new instance of the puzzle class, so that we don't get
            // leftover state from the first pass
            var solveCtx = ctx.cleanCopy();
            var puzzleForSolution = Puzzle.findByName(puzzleName);
            applyOptionsToContext(options, solveCtx, puzzleForSolution, true);
            emitPuzzle(puzzleForSolution, solveCtx, options);
        }

        stdout.println();
        stdout.println("Puzzle code: \u001b[7m " + ctx.getPuzzleCode() + " \u001b[0m");
        stdout.println();
        stdout.println("To see solution:");
        stdout.println("  " + executableName() + " solve " + ctx.getPuzzleCode());
    }

    private void solve(PuzzleOptions options) throws InvalidPuzzleCodeException, IOException {
        requireCommandArgs(1, options);
        var ctx = PuzzleContext.fromPuzzleCode(options.commandAndArgs().get(1));
        var puzzle = Puzzle.findByID(ctx.getPuzzleID());
        if(puzzle == null) {
            stderr.println("This puzzle code refers to a puzzle type that no longer exists.");
            stderr.println("Are you using an outdated code from a previous semester?");
            return;
        }

        applyOptionsToContext(options, ctx, puzzle, true);
        ctx.setPuzzleTitle(puzzle.description() + ": Solution");
        emitPuzzle(puzzle, ctx, options);

        if (ctx.getDifficulty() != puzzle.goalDifficulty()) {
            stdout.println(MessageFormat.format(
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
            stdout.println("Want to practice more basics first? Try a simpler puzzle:");
            stdout.println();
            stdout.println("  " + executableName() + " gen " + puzzle.name()
                + " --difficulty " + (ctx.getDifficulty() - 1));
            stdout.println();
        }
        if (ctx.getDifficulty() < puzzle.maxDifficulty()) {
            stdout.println("Want a bigger challenge? Try a harder difficulty level:");
            stdout.println();
            stdout.println("  " + executableName() + " gen " + puzzle.name()
                + " --difficulty " + (ctx.getDifficulty() + 1));
            stdout.println();
        }
    }

    private void applyOptionsToContext(
        PuzzleOptions options,
        PuzzleContext ctx,
        Puzzle puzzle,
        boolean solutionOutput
    ) throws IOException {
        ctx.setOutput(new ConsolePuzzlePrinter(stdout));

        ctx.setPuzzleTitle(puzzle.description());

        if (options.includeSolutions() || solutionOutput) {
            ctx.enableSolution();
        }

        if (ctx.getDifficulty() < puzzle.minDifficulty() || ctx.getDifficulty() > puzzle.maxDifficulty()) {
            stderr.println("Illegal difficulty level: " + ctx.getDifficulty());
            stderr.println("The `" + puzzle.name() + "` puzzle must have a difficulty in the range "
                + puzzle.minDifficulty() + "..." + puzzle.maxDifficulty() + ".");
            stderr.println("(The difficulty level to get credit is " + puzzle.goalDifficulty() + ".)");
            System.exit(0);
        }

        ctx.setPartsToShow(options.partsToShow());

        String htmlOutput =
            solutionOutput && options.solutionHtml() != null
                ? options.solutionHtml()
                : options.html();
        if (htmlOutput != null) {
            var htmlPrinter =
                "-".equals(htmlOutput)
                    ? new HtmlPuzzlePrinter(stdout)
                    : new HtmlPuzzlePrinter(new FileOutputStream(htmlOutput));
            if (!ctx.isSolutionEnabled()) {
                htmlPrinter.enableCopyPasteObfuscation();
            }
            ctx.setOutput(htmlPrinter);
        }

        if (options.saveCode() != null) {
            try (
                var out = new PrintWriter(
                    new FileOutputStream(options.saveCode()), false, StandardCharsets.UTF_8)
            ) {
                out.println("Puzzle type: " + puzzle.name());
                out.println("Puzzle code: " + ctx.getPuzzleCode());
                out.println();
                out.println("Options: " + String.join(" ", options.rawArgs()));
                out.println("Saving puzzle code and metadata to " + options.saveCode());

                ctx.addInstructions(() -> {
                    ctx.output().paragraph("Submit your solution on paper.");
                    ctx.output().paragraph("Be sure to *write the following information* on your submission:");
                    ctx.output().bulletList(
                        "Your name",
                        "Today’s date",
                        "“Puzzle *" + Files.getNameWithoutExtension(options.saveCode())
                            + "*” ← _Very important! We can't grade your submission without this!_");
                });
            }
        }
    }

    private void emitPuzzle(Puzzle puzzle, PuzzleContext ctx, PuzzleOptions options) throws IOException {
        ctx.emitPuzzle(() -> {
            for (int repeat = options.repeat(); repeat > 0; repeat--) {
                puzzle.generate(ctx);
                if (repeat > 1) {
                    ctx.resetSectionCounter();
                }
            }
        });
    }

    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––
    // CLI Help
    // –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––

    private void printHelp(PuzzleOptions options, boolean forceFullHelp) {
        printCommands(stderr);
        if (forceFullHelp || options.help()) {
            options.printOptions(stderr);
            printExamples(stderr);
        } else {
            stderr.println("To see all options:");
            stderr.println("  " + executableName() + " --help");
        }
    }

    private void printVersion() {
        var properties = new Properties();
        try (var stream = CommandLine.class.getResourceAsStream("/git.properties")) {
            properties.load(stream);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        stdout.println("puzzle generator version:");
        stdout.print("  commit: " + properties.get("git.commit.id.abbrev"));
        if (properties.get("git.dirty").equals("true")) {
            stdout.print(" + uncommitted changes");
        }
        stdout.println();
        stdout.println("    date: " + properties.get("git.commit.time"));
    }

    /**
     * The wrapper script can optionally provide a path to the <code>puzzle</code> script for usage
     * examples.
     */
    private String executableName() {
        // wrapper script passes the name + path with which it was invoked
        return System.getenv().getOrDefault("puzzle_command", "puzzle");
    }

    private void listPuzzles(PuzzleOptions options) {
        requireCommandArgs(0, options);

        stdout.println("Available puzzle types:");
        stdout.println();

        int nameWidth = Puzzle.all().stream()
            .map(Puzzle::name)
            .mapToInt(String::length)
            .max().orElse(0);

        for (var puzzle : Puzzle.all()) {
            if (puzzle.isVisible()) {
                stdout.printf("  %-" + nameWidth + "s  %s",
                    puzzle.name(),
                    puzzle.description());
                stdout.println();
            }
        }
        stdout.println();
        stdout.println("To generate a puzzle:");
        stdout.println("  " + executableName() + " gen <puzzletype>");
    }

    public void printCommands(PrintWriter out) {
        out.println(
            """
            Commands:
              puzzle list           List available puzzle types
              puzzle gen <type>     Generate a new puzzle
              puzzle solve <code>   Print the solution to a puzzle
            """);
    }

    public void printExamples(PrintWriter out) {
        out.println(
            """
            Usage examples:
              %1$s gen loops
              %1$s solve 1454-1234-1234
              %1$s gen loops --part 3 --repeat 5
            """.trim().formatted(executableName()));
    }
}
