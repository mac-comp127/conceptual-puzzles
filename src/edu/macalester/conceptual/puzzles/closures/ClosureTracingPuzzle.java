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
import static edu.macalester.conceptual.util.Randomness.chooseConst;
import static edu.macalester.conceptual.util.Randomness.insertAtRandomPosition;

public class ClosureTracingPuzzle {
    private final PuzzleContext ctx;
    private final StringBuilder closureCode = new StringBuilder();
    private final List<Event> events = new ArrayList<>();
    private final PlaceholderGenerator placeholders = new PlaceholderGenerator();

    public ClosureTracingPuzzle(PuzzleContext ctx) {
        this.ctx = ctx;
    }

    public void generate() {
        generateClosureCalls(3 + ctx.getDifficulty() / 2, ctx.getDifficulty());

        while (events.size() < 3) {
            insertAtRandomPosition(ctx, events, chooseConst(ctx, Event.values()));
        }

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
                getClosureCodeForExecution()
            ),
            String.format(
                """
                var c = new ClosureCode();
                c.run();
                c.generateEvents(out, List.of(%s));
                """,
                getEvents().stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(", "))
            )
        );

        ctx.output().paragraph("Consider the following code:");
        ctx.output().codeBlock(getClosureCodeForDisplay());
        ctx.output().paragraph(
            """
            Write out the order in which all the marked statements will execute given the following
            sequence of events (showing both the marked statements and the event names):
            """
        );
        ctx.output().codeBlock(
            getEvents().stream()
                .map(Enum::name)
                .collect(Collectors.joining("\n"))
        );

        ctx.solution(() -> {
            ctx.output().codeBlock(output);

            if (ctx.getDifficulty() == 0) {
                ctx.output().dividerLine(false);
                ctx.output().paragraph(
                    """
                    Although it is not necessary for getting credit for the conceptual mastery
                    puzzles, it is a good idea to study this puzzle at least once at the next
                    highest difficulty level. That level introduces a new twist: event handlers
                    adding other event handlers. Understanding what happens you do that will help
                    you avoid common mistakes on the later homeworks and the course project.
                    """
                );
            }
        });
    }

    void generateClosureCalls(int count, int depth) {
        closureCode.append(newPlaceholder());
        closureCode.append(";\n");
        for(int n = 0; n < count; n++) {
            choose(ctx,
                () -> choose(ctx,
                    () -> {
                        closureCode.append("onClick(() -> ");
                        generateClosureBody(count, depth);
                        closureCode.append(");\n");
                        queueAFew(Event.CLICK);
                    },
                    () -> {
                        closureCode.append("onKeyPress(() -> ");
                        generateClosureBody(count, depth);
                        closureCode.append(");\n");
                        queueAFew(Event.KEY);
                    }
                ),
                () -> {
                    closureCode.append("twice(() -> ");
                    generateClosureBody(count, depth);
                    closureCode.append(");\n");
                },
                () -> {
                    int delay = ctx.getRandom().nextInt(1, 4);
                    closureCode.append("afterDelay(")
                        .append(delay)
                        .append(", () -> ");
                    generateClosureBody(count, depth);
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

    private void generateClosureBody(int siblingCount, int depth) {
        if (depth <= 0 || ctx.getRandom().nextFloat() < 0.4) {
            closureCode.append(newPlaceholder());
        } else {
            closureCode.append("{\n");
            generateClosureCalls(siblingCount / 2, depth - 1);
            closureCode.append("}");
        }
    }

    public String getClosureCodeForDisplay() {
        return
            CodeFormatting.prettifyStatements(
                closureCode.toString())
            .replaceAll(
                "System\\.out\\.println\\(\"(___[A-Z]+___)\"\\);?",
                "$1");
    }

    public String getClosureCodeForExecution() {
        return closureCode.toString();
    }

    public List<Event> getEvents() {
        return events;
    }

    private void queueAFew(Event event) {
        int numToGenerate = 0;
        int userEventCount = events.size() - Collections.frequency(events, Event.TICK);
        if (userEventCount < 2 + ctx.getDifficulty()) {
            numToGenerate++;
        }
        if (userEventCount < 3 + ctx.getDifficulty() && ctx.getRandom().nextFloat() < 0.3) {
            numToGenerate++;
        }
        for(int n = 0; n < numToGenerate; n++) {
            insertAtRandomPosition(ctx, events, event);
        }
    }

    private String newPlaceholder() {
        placeholders.next();
        return "System.out.println(\"" + placeholders.current() + "\")";
    }
}
