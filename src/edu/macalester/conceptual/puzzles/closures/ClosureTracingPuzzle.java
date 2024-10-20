package edu.macalester.conceptual.puzzles.closures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.puzzles.closures.ClosureExecutor.Event;
import edu.macalester.conceptual.util.CodeFormatting;
import edu.macalester.conceptual.util.Evaluator;
import edu.macalester.conceptual.util.PlaceholderGenerator;

import static edu.macalester.conceptual.util.Randomness.choose;
import static edu.macalester.conceptual.util.Randomness.insertAtRandomPosition;

public class ClosureTracingPuzzle implements Puzzle {
    @Override
    public byte id() {
        return 4;
    }

    @Override
    public String name() {
        return "closures";
    }

    @Override
    public String description() {
        return "Closure execution order";
    }

    @Override
    public byte goalDifficulty() {
        return 0;
    }

    @Override
    public byte maxDifficulty() {
        return 10;
    }

    public void generate(PuzzleContext ctx) {
        var builder = new Builder();
        builder.generateClosureCalls(ctx, 3 + ctx.getDifficulty() / 2, ctx.getDifficulty());

        String output = Evaluator.captureOutput(
            """
            import edu.macalester.conceptual.puzzles.closures.*;
            import static edu.macalester.conceptual.puzzles.closures.ClosureExecutor.Event.*;
            import java.util.List;
            """,
            String.format(
                """
                class ClosureCode extends ClosureExecutor {
                    void run() {
                        %s
                    }
                }
                """,
                builder.getClosureCodeForExecution()
            ),
            String.format(
                """
                var c = new ClosureCode();
                c.run();
                c.generateEvents(out, List.of(%s));
                """,
                builder.getEvents().stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(", "))
            )
        );

        ctx.output().paragraph("Consider the following code:");
        ctx.output().codeBlock(builder.getClosureCodeForDisplay());
        ctx.output().paragraph(
            """
            Write out the order in which all the marked statements will execute given the following
            sequence of events (showing both the marked statements and the event names):
            """
        );
        ctx.output().codeBlock(output.replaceAll("___.___\n", ""));

        ctx.solution(() -> {
            ctx.output().codeBlock(output);
        });
    }

    private class Builder {
        private final StringBuilder closureCode = new StringBuilder();
        private final List<Event> events = new ArrayList<>();
        private PlaceholderGenerator placeholders = new PlaceholderGenerator();

        void generateClosureCalls(
            PuzzleContext ctx,
            int count,
            int depth
        ) {
            closureCode.append(newPlaceholder());
            closureCode.append(";\n");
            for(int n = 0; n < count; n++) {
                choose(ctx,
                    () -> {
                        closureCode.append("onClick(() -> ");
                        generateClosureBody(ctx, count, depth);
                        closureCode.append(");\n");
                        queueAFew(ctx, events, Event.CLICK);
                    },
                    () -> {
                        closureCode.append("onKeyPress(() -> ");
                        generateClosureBody(ctx, count, depth);
                        closureCode.append(");\n");
                        queueAFew(ctx, events, Event.KEY);
                    },
                    () -> {
                        int delay = ctx.getRandom().nextInt(1, 4);
                        closureCode.append("afterDelay(")
                            .append(delay)
                            .append(", () -> ");
                        generateClosureBody(ctx, count, depth);
                        closureCode.append(");\n");

                        int tickCount = Collections.frequency(events, Event.TICK);
                        for(int m = tickCount; m <= delay + depth; m++) {
                            insertAtRandomPosition(ctx, events, Event.TICK);
                        }
                    }
                );
                closureCode.append(newPlaceholder());
                closureCode.append(";\n");
            }
        }

        private void generateClosureBody(PuzzleContext ctx, int siblingCount, int depth) {
            if (depth <= 0 || ctx.getRandom().nextFloat() < 0.6) {
                closureCode.append(newPlaceholder());
            } else {
                closureCode.append("{\n");
                generateClosureCalls(ctx, siblingCount / 2, depth - 1);
                closureCode.append("}");
            }
        }

        public String getClosureCodeForDisplay() {
            return
                CodeFormatting.prettifyStatements(
                    closureCode.toString())
                .replaceAll(
                    "System\\.out\\.println\\(\"(___.___)\"\\);?",
                    "$1");
        }

        public String getClosureCodeForExecution() {
            return closureCode.toString();
        }

        public List<Event> getEvents() {
            return events;
        }

        private String newPlaceholder() {
            placeholders.next();
            return "System.out.println(\"" + placeholders.current() + "\")";
        }
    }

    private void queueAFew(PuzzleContext ctx, List<Event> events, Event event) {
        int eventCount = 0;
        if (events.size() < 2) {
            eventCount++;
        }
        if (events.size() < 4 && ctx.getRandom().nextFloat() < 0.3) {
            eventCount++;
        }
        for(int n = 0; n < eventCount; n++) {
            insertAtRandomPosition(ctx, events, event);
        }
    }

    public static void main(String[] args) throws Exception {
        var ctx = PuzzleContext.fromPuzzleCode("Lwar-qei5-2ng9-kr6s");
        ctx.emitPuzzle(() -> new ClosureTracingPuzzle().generate(ctx));
    }
}
