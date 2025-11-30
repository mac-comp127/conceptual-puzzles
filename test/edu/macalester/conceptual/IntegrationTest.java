package edu.macalester.conceptual;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Chunk;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import edu.macalester.conceptual.cli.CommandLine;
import edu.macalester.conceptual.context.ConsolePuzzlePrinter;
import edu.macalester.conceptual.context.InvalidPuzzleCodeException;
import edu.macalester.conceptual.context.PuzzleContext;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.*;

public class IntegrationTest {
    private static final List<String> PUZZLE_CODES = List.of(
        "wtig-b8nz-u452-rLgu",   // type
        "rjbi-1t8x-u0h0-6cjn",   // rel, difficulty = 9
        "Lxqr-4dys-gpds-8Lfa",   // clos, difficulty = 2
        "gewc-fit8-6tgL-hatp",   // vars
        "2mgg-4Ldu-2mq7-Ld3",    // loop
        "azy1-v4Lt-rq5L-7g92",   // ast
        "5i42-dtog-jq0h-93cn",   // bool
        "5kwo-ytu2-xjbr-tuir",   // bool, difficulty = 5
        "17tn-p4ge-k8oL-0bjf-6", // class
        "c0uw-orx1-jycz-g82x"    // ast, difficulty = 50, potential operator precedence issue
    );

    @TestFactory
    List<DynamicTest> integrationTests() throws InvalidPuzzleCodeException {
        ConsolePuzzlePrinter.disableGraphics();

        var tests = new ArrayList<DynamicTest>();
        tests.add(createIntegrationTest("no args"));
        tests.add(createIntegrationTest("help", "--help"));
        tests.add(createIntegrationTest("html vars", "solve", "gem8-9kcc-zm63-yo71", "--html", "-"));
        final var inSeparateProcess = true;
        tests.add(createIntegrationTest("html loop", inSeparateProcess, "solve", "37jv-6084-d1bb-ev4", "--html", "-"));

        var puzzlesNotCovered = new HashSet<>(Puzzle.all().stream().map(Puzzle::name).toList());
        for (var code : PUZZLE_CODES) {
            var puzzle = Puzzle.findByID(PuzzleContext.fromPuzzleCode(code).getPuzzleID());
            puzzlesNotCovered.remove(puzzle.name());
            tests.add(createIntegrationTest(puzzle.name() + " " + code, "solve", code, "--repeat=6"));
        }

        tests.add(DynamicTest.dynamicTest(
            "integration test coverage",
            () -> {
                if (!puzzlesNotCovered.isEmpty()) {
                    fail(MessageFormat.format(
                        """
                        Missing integration tests for {0}
                        
                        Use the command line to generate puzzle codes, then add those codes
                        to the PUZZLE_CODES constant at the top of IntegrationTest.java.
                        Run tests once to acquire actual output, then follow the instructions
                        in the test failure to turn the actual output into the expected output.
                        """,
                        puzzlesNotCovered.stream().sorted().toList()));
                }
            }));

        return tests;
    }

    private DynamicTest createIntegrationTest(String name, String... cliArgs) {
        return createIntegrationTest(name, false, cliArgs);  // run in same process by default
    }

    private DynamicTest createIntegrationTest(String name, boolean spawn, String... cliArgs) {
        return DynamicTest.dynamicTest(
            name,
            () -> {
                var expectedOutputFile = Path.of("test", "fixtures", "integration", name + ".expected.log");
                var actualOutputFile = File.createTempFile(name + "-", ".actual.log").toPath();

                if (spawn) {
                    runInSeparateProcess(cliArgs, actualOutputFile);
                } else {
                    runInSameProcess(cliArgs, actualOutputFile);
                }

                if (!Files.exists(expectedOutputFile)) {
                    fail(MessageFormat.format(
                        """
                        No expected output file for "{0}" integration test.
                        
                        To use the actual output from this test run as the expected output:
                        
                            mv {1} {2}
                        """,
                        name,
                        fullPath(actualOutputFile),
                        fullPath(expectedOutputFile)));
                }

                var expectedOutput = readPuzzleLog(expectedOutputFile);
                var actualOutput = readPuzzleLog(actualOutputFile);

                // Despite the UTF-8 workaround above, Java 18 on Windows still clobbers the encoding of UTF-8 process
                // output. As a workaround, if we notice the divider lines are getting transformed into ??????????????,
                // we just turn all non-ASCII chars on this platform in question marks.
                if (isWindows() && actualOutput.stream().anyMatch(line -> line.contains("?????????"))) {
                    System.out.println("Windows encoding woes; ignoring special chars");
                    actualOutput = stripNonASCII(actualOutput);
                    expectedOutput = stripNonASCII(expectedOutput);
                }

                if (!expectedOutput.equals(actualOutput)) {
                    fail(
                        MessageFormat.format(
                            """
                            Mismatched integration test output
                            Expected output file: {0}
                            Actual output file: {1}

                            To replace the expected output with the actual output from this run:
                        
                            mv {1} {0}
                            
                            Diff (printing max 10 changes, 10 lines each):
                            {2}
                            """,
                            fullPath(expectedOutputFile),
                            fullPath(actualOutputFile),
                            formattedDiff(expectedOutput, actualOutput)));
                }
            });
    }

    private static void runInSameProcess(String[] args, Path actualOutputFile) throws IOException {
        try (var out = new FileOutputStream(actualOutputFile.toFile())) {
            new CommandLine(out, out).invoke(args);
        }
    }

    private static void runInSeparateProcess(String[] args, Path actualOutputFile) throws IOException {
        var command = new ArrayList<String>();
        command.add(Path.of("bin", "puzzle" + (isWindows() ? ".bat" : "")).toString());
        command.addAll(Arrays.asList(args));
        System.out.println(String.join(" ", command));

        var builder = new ProcessBuilder()
            .command(command)
            .redirectErrorStream(true);
        builder.environment().put("PUZZLE_EXIT_IMMEDIATELY", "1");
        builder.environment().put("IGNORE_CONSOLE_WIDTH", "1");
        builder.environment().put("COLORTERM", "");
        builder.environment().put("_JAVA_OPTIONS", "-Dfile.encoding=UTF-8");

        var process = builder.start();

        // ProcessBuilder.redirectOutput does not reliably use UTF-8 in all Java versions on
        // all platforms, even if we set file.encoding=UTF-8 on JVM launch, so we manually
        // pipe output to a UTF-8-encoded file:
        try (
            var pipe = new BufferedReader(new InputStreamReader(process.getInputStream(), UTF_8));
            var fileOutput = new PrintWriter(new FileWriter(actualOutputFile.toFile(), UTF_8))
        ) {
            String line;
            while ((line = pipe.readLine()) != null) {
                if (
                    line.contains("Picked up _JAVA_OPTIONS")  // Java unhelpfully logs this
                    || line.contains("+[IMK")  // macOS UI logging generates spurious diffs
                ) {
                    continue;
                }
                fileOutput.println(line);
            }
        }
    }

    private static List<String> readPuzzleLog(Path file) throws IOException {
        try (var lines = Files.lines(file, UTF_8)) {
            return lines
                .map(line -> line.replaceAll("bin[/\\\\]puzzle", "puzzle"))
                .filter(line -> !line.matches(".*(GL pipe|_JAVA_OPTIONS).*"))  // CI prints GL warnings
                .toList();
        }
    }

    private static String formattedDiff(List<String> left, List<String> right) {
        return DiffUtils.diff(left, right)
            .getDeltas().stream()
            .limit(10)
            .map(delta -> MessageFormat.format(
                """
                {0} → {1}:
                {2}
                –––
                {3}
                 
                """,
                delta.getSource().getPosition() + 1,
                delta.getTarget().getPosition() + 1,
                formatDiffChunk("< ", delta.getSource()),
                formatDiffChunk("> ", delta.getTarget())))
            .collect(joining());
    }

    private static String formatDiffChunk(String prefix, Chunk<String> chunk) {
        return chunk
            .getLines().stream()
            .limit(10)
            .map(line -> prefix + line)
            .collect(joining(System.lineSeparator()));
    }

    private List<String> stripNonASCII(List<String> lines) {
        return lines.stream()
            .map(line -> line.replaceAll("([^ -~]|\\?)+", "?"))
            .toList();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").contains("Windows");
    }

    private String fullPath(Path path) {
        return "'" + path.toAbsolutePath().toString() + "'";
    }
}
