package edu.macalester.conceptual;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.BufferedReader;
import java.io.File;
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
import java.util.stream.Collectors;

import edu.macalester.conceptual.context.InvalidPuzzleCodeException;
import edu.macalester.conceptual.context.PuzzleContext;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class IntegrationTest {
    private static final List<String> PUZZLE_CODES = List.of(
        "7yg5-04ba-rfo9-hi",    // loop
        "azy1-v4Lt-rq5L-7g92",  // ast
        "5i42-dtog-jq0h-93cn",  // bool
        "5kwo-ytu2-xjbr-tuir"   // bool, difficulty = 5
    );

    @TestFactory
    List<DynamicTest> integrationTests() throws InvalidPuzzleCodeException {
        var tests = new ArrayList<DynamicTest>();
        tests.add(createIntegrationTest("no args"));
        tests.add(createIntegrationTest("help", "--help"));

        var puzzlesNotCovered = new HashSet<>(Puzzle.ALL);
        for (var code : PUZZLE_CODES) {
            var puzzle = Puzzle.findByID(PuzzleContext.fromPuzzleCode(code).getPuzzleID());
            puzzlesNotCovered.remove(puzzle);
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
                        puzzlesNotCovered.stream().map(Puzzle::name).sorted().toList()));
                }
            }));

        return tests;
    }

    private DynamicTest createIntegrationTest(String name, String... cliArgs) {
        return DynamicTest.dynamicTest(
            name,
            () -> {
                var expectedOutputFile = Path.of("test", "fixtures", "integration", name + ".expected.log");
                var actualOutputFile = File.createTempFile(name + "-", ".actual.log").toPath();

                var command = new ArrayList<String>();
                command.add(Path.of("bin", "puzzle" + (isWindows() ? ".bat" : "")).toString());
                command.addAll(Arrays.asList(cliArgs));
                System.out.println(String.join(" ", command));
                var builder = new ProcessBuilder()
                    .command(command.toArray(new String[0]))
                    .redirectErrorStream(true);
                builder.environment().put("PUZZLE_EXIT_IMMEDIATELY", "1");
                builder.environment().put("IGNORE_CONSOLE_WIDTH", "1");
                builder.environment().put("COLORTERM", "");

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
                        fileOutput.println(line);
                    }
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
                if (isWindows() && actualOutput.contains("?????????")) {
                    actualOutput = stripNonASCII(actualOutput);
                    expectedOutput = stripNonASCII(expectedOutput);
                }

                assertEquals(
                    expectedOutput,
                    actualOutput,
                    MessageFormat.format(
                        """
                        Mismatched integration test output
                        Expected output file: {0}
                        Actual output file: {1}
                        """,
                        fullPath(expectedOutputFile),
                        fullPath(actualOutputFile)));
            });
    }

    private static String readPuzzleLog(Path file) throws IOException {
        try (var lines = Files.lines(file, UTF_8)) {
            return lines
                .map(line -> line.replaceAll("bin[/\\\\]puzzle", "puzzle"))
                .filter(line -> !line.contains("GL pipe is running in software mode")) // CI prints this
                .collect(Collectors.joining(System.lineSeparator()));
        }
    }

    private String stripNonASCII(String str) {
        return str.replaceAll("([^ -~]|\\?)+", "?");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").contains("Windows");
    }

    private String fullPath(Path path) {
        return "'" + path.toAbsolutePath().toString() + "'";
    }
}
