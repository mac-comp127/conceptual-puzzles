package edu.macalester.conceptual.context;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PuzzleContextTest {
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

    private static void verifySeedRoundTrip(PuzzleContext original) throws InvalidPuzzleCodeException {
        var recreated = PuzzleContext.fromPuzzleCode(original.getPuzzleCode());
        assertContextsEqual(original, recreated);
    }

    private static void assertContextsEqual(PuzzleContext original, PuzzleContext recreated) {
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

        PuzzlePrinter silent = new PuzzlePrinter(new PrintWriter(new StringWriter()));
        original.setOutput(silent);
        recreated.setOutput(silent);

        original.emitPuzzle(() -> {
            recreated.emitPuzzle(() -> {
                assertEquals(
                    original.getRandom().nextLong(),
                    recreated.getRandom().nextLong(),
                    "random output mismatch for " + original);
            });
        });
    }
}
