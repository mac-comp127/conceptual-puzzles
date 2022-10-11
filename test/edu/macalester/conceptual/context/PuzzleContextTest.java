package edu.macalester.conceptual.context;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PuzzleContextTest {
    @Test
    void contextShouldSurvivePuzzleCodeRoundTrip() throws Exception {
        verifySeedRoundTrip(new PuzzleContext(0));
        verifySeedRoundTrip(new PuzzleContext(1));
        verifySeedRoundTrip(new PuzzleContext(-1));
        verifySeedRoundTrip(new PuzzleContext(Long.MAX_VALUE));
        verifySeedRoundTrip(new PuzzleContext(Long.MIN_VALUE));
        for(int i = 0; i < 100; i++) {
            verifySeedRoundTrip(PuzzleContext.generate());
        }
    }

    @Test
    void seedsAreCaseInsensitive() throws Exception {
        assertContextsEqual(
            PuzzleContext.fromPuzzleCode("2QFJ-dt51-3IGM-pyk8"),
            PuzzleContext.fromPuzzleCode("2qfJ-DT51-3igm-PYK8"));
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
            original.getRandom().nextLong(),
            recreated.getRandom().nextLong(),
            "random output mismatch for " + original);
    }
}
