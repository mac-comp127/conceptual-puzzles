package edu.macalester.conceptual.util;

import org.junit.jupiter.api.Test;

import edu.macalester.conceptual.context.ConsolePuzzlePrinter;
import edu.macalester.conceptual.context.InvalidPuzzleCodeException;
import edu.macalester.conceptual.context.PuzzleContext;

import static org.junit.jupiter.api.Assertions.*;

class NonsenseTest {
    @Test
    void word() throws Exception {
        var ctx = PuzzleContext.fromPuzzleCode("gewc-fit8-6tgL-hatp");
        ctx.setOutput(new ConsolePuzzlePrinter());
        ctx.emitPuzzle(() -> {
            for (int i = 0; i < 10000; i++) {
                var word = Nonsense.word(ctx);
                assertNotEquals("if", word);          // No short Java reserved words
                assertNotEquals("do", word);
                assertNotEquals("instanceof", word);  // No non-English Java reserved words
                assertNotEquals("const", word);
                assertNotEquals("cat", word);         // No English words
                assertNotEquals("dog", word);
            }
        });
    }
}
