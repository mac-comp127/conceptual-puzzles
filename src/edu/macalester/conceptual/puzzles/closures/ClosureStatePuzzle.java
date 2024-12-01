package edu.macalester.conceptual.puzzles.closures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.CodeSnippet;
import edu.macalester.conceptual.util.Evaluator;

import static edu.macalester.conceptual.util.Randomness.chooseConst;

public class ClosureStatePuzzle {
    public void generate(PuzzleContext ctx) {
        var codeBuilder = new StringBuilder();
        codeBuilder.append(
            """
            public class StatefulPuzzle {
                private int x = 0;
            
                public void run() {
                    onClick(() -> {
                        x++;
                        System.out.println("x=" + x);
                    });
            """
        );
        codeBuilder.append("\n");

        var methods = new ArrayList<>(List.of(
            """
                    if (x %s) {
                        onClick(() -> {
                            System.out.println("%s");
                        });
                    }
            """,
            """
                    onClick(() -> {
                        if (x %s) {
                            System.out.println("%s");
                        }
                    });
            """
        ));
        Collections.shuffle(methods, ctx.getRandom());

        var messages = List.of("Hi!", "Bye!").iterator();
        for (var method : methods) {
            codeBuilder.append(String.format(
                method,
                chooseConst(ctx, "> 0", "> 1", "< 1", "< 2"),
                messages.next()
            ));
            codeBuilder.append("\n");
        }

        codeBuilder.append(
            """
                    System.out.println("End of run()");
                }
            }
            """
        );
        var code = codeBuilder.toString();

        ctx.output().paragraph("Consider the following code:");
        ctx.output().codeBlock(code);
        ctx.output().paragraph(
            "Given a call to `run()` followed by two `CLICK` events, what would this code print?"
        );

        var output = Evaluator.captureOutput(
            CodeSnippet.build()
                .withImports(
                    """
                    import edu.macalester.conceptual.puzzles.closures.*;
                    import static edu.macalester.conceptual.puzzles.closures.ClosureExecutor.Event.*;
                    """
                )
                .withMainBody(
                    """
                    new StatePuzzleContainer().go();
                    """
                )
                .withOtherClasses(
                    String.format(
                        """
                        class StatePuzzleContainer extends ClosureExecutor {
                            void go() {
                                var p = new StatefulPuzzle();
                                p.run();
                                generateEvent(CLICK);
                                generateEvent(CLICK);
                            }
                        
                            %s
                        }
                        """,
                        code
                    )
                )
        );

        ctx.solution(() -> {
            ctx.output().codeBlock(output);
        });
    }

    public static void main(String[] args) throws Exception {
        var ctx = PuzzleContext.generate((byte) 1, (byte) 1);
        ctx.enableSolution();
        ctx.emitPuzzle(() ->
            new ClosureStatePuzzle().generate(ctx));
    }
}
