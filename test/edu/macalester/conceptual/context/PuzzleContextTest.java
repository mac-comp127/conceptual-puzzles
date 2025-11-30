package edu.macalester.conceptual.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"Convert2MethodRef", "ResultOfMethodCallIgnored"})
class PuzzleContextTest {
    private final PuzzleContext ctx;
    private final StringWriter outputWriter;

    PuzzleContextTest() {
        ctx = PuzzleContext.generate((byte) 0, (byte) 0);
        outputWriter = new StringWriter();
        ctx.setOutput(new ConsolePuzzlePrinter(new PrintWriter(outputWriter)));
    }

    @Test
    void contextShouldSurvivePuzzleCodeRoundTrip() throws Exception {
        verifySeedRoundTrip(new PuzzleContext(new PuzzleCode((byte) 0, (byte) 0, 0)));
        verifySeedRoundTrip(new PuzzleContext(new PuzzleCode((byte) 1, (byte) 1, 1)));
        verifySeedRoundTrip(new PuzzleContext(new PuzzleCode((byte) 127, (byte) -1, -1)));
        verifySeedRoundTrip(new PuzzleContext(new PuzzleCode((byte) 127, (byte) 127, Long.MAX_VALUE)));
        verifySeedRoundTrip(new PuzzleContext(new PuzzleCode((byte) 0, (byte) -128, Long.MIN_VALUE)));
        for(int i = 0; i < 100; i++) {
            verifySeedRoundTrip(PuzzleContext.generate((byte) i, (byte) (50 - i)));
        }
    }

    @Test
    void seedsAreCaseInsensitive() throws Exception {
        assertContextsEqual(
            PuzzleContext.fromPuzzleCode("azL3-gi7s-cR4p-zYm2"),
            PuzzleContext.fromPuzzleCode("AZl3-GI7s-Cr4p-ZyM2"));
    }

    @Test
    void invalidPuzzleCodesShouldFailCleanly() {
        List.of(
            // bad format
            "",
            "--------",
            "🤪",
            "q#$%y",

            // too large
            "99999999999999999999999999999",

            // invalid checksums ("2qfj-dt51-3igm-pyk8" is valid)
            "0",
            "2qfj-dt51-3igm-pyk9",
            "2fqj-dt51-3igm-pyk8"
        ).forEach(code ->
            assertThrows(
                InvalidPuzzleCodeException.class,
                () -> PuzzleContext.fromPuzzleCode(code),
                "For seed code \"" + code + "\"")
        );
    }

    private static void verifySeedRoundTrip(PuzzleContext original) throws InvalidPuzzleCodeException, IOException {
        var recreated = PuzzleContext.fromPuzzleCode(original.getPuzzleCode());
        assertContextsEqual(original, recreated);
    }

    private static void assertContextsEqual(PuzzleContext original, PuzzleContext recreated) throws IOException {
        assertEquals(
            original.getPuzzleCode(),
            recreated.getPuzzleCode(),
            "seed code mismatch for " + original);
        assertEquals(
            original.getDifficulty(),
            recreated.getDifficulty(),
            "difficulty mismatch for " + original);
        assertEquals(
            original.getPuzzleID(),
            recreated.getPuzzleID(),
            "puzzle ID mismatch for " + original);

        PuzzlePrinter silent = new ConsolePuzzlePrinter(new PrintWriter(new StringWriter()));
        original.setOutput(silent);
        recreated.setOutput(silent);

        original.emitPuzzle(() -> {
            try {
                recreated.emitPuzzle(() -> {
                    assertEquals(
                        original.getRandom().nextLong(),
                        recreated.getRandom().nextLong(),
                        "random output mismatch for " + original);
                });
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // -––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––
    // Showing / Hiding
    // -––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––

    @Test
    void solutionBlocksRunIfSolutionsEnabled() throws IOException {
        ctx.enableSolution();
        ctx.emitPuzzle(() -> {
            ctx.output().paragraph("problem!");
            ctx.solution(() -> {
                ctx.output().paragraph("solution!");
            });
        });
        var output = outputWriter.toString();
        assertTrue(output.contains("problem!"));
        assertTrue(output.contains("solution!"));
    }

    @Test
    void solutionBlocksDoNotRunIfSolutionsDisabled() throws IOException {
        ctx.emitPuzzle(() -> {
            ctx.output().paragraph("problem!");
            ctx.solution(() -> {
                throw new RuntimeException("solution block should not run");
            });
        });
        assertTrue(outputWriter.toString().contains("problem!"));
    }


    @Test
    void allPartsVisibleIfNoneHidden() throws IOException {
        var output = emitPuzzleWithThreeSections();
        assertTrue(output.contains("part one"));
        assertTrue(output.contains("part two"));
        assertTrue(output.contains("part three"));
    }

    @Test
    void hiddenPartsRunButDoNotProduceOutput() throws IOException {
        ctx.setPartsToShow(Set.of(1, 3));
        var output = emitPuzzleWithThreeSections();
        assertTrue(output.contains("part one"));
        assertFalse(output.contains("part two"));
        assertTrue(output.contains("part three"));
    }

    private String emitPuzzleWithThreeSections() throws IOException {
        ctx.emitPuzzle(() -> {
            ctx.section(() -> {
                ctx.output().paragraph("part one");
            });
            ctx.section(() -> {
                ctx.output().paragraph("part two");
            });
            ctx.section(() -> {
                ctx.output().paragraph("part three");
            });
        });
        return outputWriter.toString();
    }

    // -––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––
    // State Machine
    // -––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––

    @Test
    void cannotEmitPuzzleTwice() throws IOException {
        ctx.emitPuzzle(() -> {});
        assertIllegalStateTransition(() -> ctx.emitPuzzle(() -> {}));
    }

    @Test
    void canOnlyConfigureOptionsBeforeEmitting() throws IOException {
        ctx.setOutput(createTestPrinter());
        ctx.setPartsToShow(Set.of(1, 3));
        ctx.enableSolution();

        ctx.emitPuzzle(() -> {
            assertIllegalStateTransition(() -> ctx.setOutput(createTestPrinter()));
            assertIllegalStateTransition(() -> ctx.setPartsToShow(Set.of(1, 3)));
            assertIllegalStateTransition(() -> ctx.enableSolution());
        });
        assertIllegalStateTransition(() -> ctx.setOutput(createTestPrinter()));
        assertIllegalStateTransition(() -> ctx.setPartsToShow(Set.of(1, 3)));
        assertIllegalStateTransition(() -> ctx.enableSolution());
    }

    @Test
    void canOnlyGetOutputWhileEmitting() throws IOException {
        assertIllegalStateTransition(() -> ctx.output());
        ctx.emitPuzzle(() -> {
            ctx.output().paragraph("puzzling");
        });
        assertIllegalStateTransition(() -> ctx.output());
    }

    @Test
    void canOnlyGetRandomWhileEmitting() throws IOException {
        assertIllegalStateTransition(() -> ctx.getRandom());
        ctx.emitPuzzle(() -> {
            ctx.getRandom().nextInt();
        });
        assertIllegalStateTransition(() -> ctx.getRandom());
    }

    @Test
    void cannotAccessRandomnessInSolutionSection() throws IOException {
        ctx.emitPuzzle(() -> {
            ctx.getRandom();
            ctx.solution(() -> {
                assertIllegalStateTransition(() -> ctx.getRandom());
            });
            ctx.getRandom();
        });
    }

    @Test
    void canOnlyProvideSolutionWhileEmitting() throws IOException {
        assertIllegalStateTransition(() -> ctx.solution(() -> {}));
        assertIllegalStateTransition(() -> ctx.solutionChecklist("do all the things"));
        ctx.emitPuzzle(() -> {
            assertIllegalStateTransition(() -> ctx.solutionChecklist("do all the things"));
            ctx.solution(() -> {
                ctx.solutionChecklist("do all the things"); // at last, it is legal!
            });
        });
        assertIllegalStateTransition(() -> ctx.solution(() -> {}));
        assertIllegalStateTransition(() -> ctx.solutionChecklist("do all the things"));
    }

    @Test
    void canOnlyManageSectionsWhileEmitting() throws IOException {
        assertIllegalStateTransition(() -> ctx.section(() -> {}));
        assertIllegalStateTransition(() -> ctx.currentSectionTitle());
        assertIllegalStateTransition(() -> ctx.currentSectionHue());
        assertIllegalStateTransition(() -> ctx.resetSectionCounter());

        ctx.emitPuzzle(() -> {
            ctx.section(() -> {});
            ctx.currentSectionTitle();
            ctx.currentSectionHue();
            ctx.resetSectionCounter();
        });

        assertIllegalStateTransition(() -> ctx.section(() -> {}));
        assertIllegalStateTransition(() -> ctx.currentSectionTitle());
        assertIllegalStateTransition(() -> ctx.currentSectionHue());
        assertIllegalStateTransition(() -> ctx.resetSectionCounter());
    }

    private void assertIllegalStateTransition(Executable action) {
        assertThrows(IllegalStateException.class, action);
    }

    private static ConsolePuzzlePrinter createTestPrinter() {
        return new ConsolePuzzlePrinter(new PrintWriter(new StringWriter()));
    }
}
