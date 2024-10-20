package edu.macalester.conceptual.puzzles.loops;

import static edu.macalester.conceptual.util.Randomness.chooseConst;
import static edu.macalester.conceptual.util.Randomness.chooseWithProb;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.PlaceholderGenerator;

public enum LoopTracingPuzzle {
    C_STYLE_FOR(
        """
        «BEFORE_LOOP,3»
        for («BEFORE_LOOP»; «EACH_ITER»; «AFTER_EACH_ITER») {
            «EACH_ITER_EXCEPT_LAST,2»
        }
        «AFTER_LOOP,3»
        """,
        "Assume the body of the loop executes *«LOOP_COUNT»* time«LOOP_COUNT_PLURALITY»."
    ),

    SIMPLE_WHILE(
        """
        «BEFORE_LOOP,3»
        while («EACH_ITER») {
            «EACH_ITER_EXCEPT_LAST,2»
        }
        «AFTER_LOOP,3»
        """,
        "Assume the body of the loop executes *«LOOP_COUNT»* time«LOOP_COUNT_PLURALITY»."
    ),

    WHILE_BREAK(
        """
        «BEFORE_LOOP,3»
        while («EACH_ITER») {
            «EACH_ITER,2»
            if («EACH_ITER») {
                «AFTER_LOOP,2»
                break;
            }
            «EACH_ITER_EXCEPT_LAST,2»
        }
        «AFTER_LOOP,3»
        """,
        "Assume the loop breaks on iteration *«LOOP_COUNT_PLUS_ONE»*."
    ),

    WHILE_STOP_WITHOUT_BREAK(
        """
        «BEFORE_LOOP,3»
        while («EACH_ITER») {
            «EACH_ITER_EXCEPT_LAST,2»
            if («EACH_ITER_EXCEPT_LAST») {
                «NEVER,2»
                break;
            }
            «EACH_ITER_EXCEPT_LAST,2»
        }
        «AFTER_LOOP,3»
        """,
        """
        Assume the loop ends because the test condition of the loop
        is false on iteration *«LOOP_COUNT_PLUS_ONE»*.
        """
    );

    private enum LoopPhase {
        BEFORE_LOOP,
        EACH_ITER,
        EACH_ITER_EXCEPT_LAST,
        AFTER_EACH_ITER,
        AFTER_LOOP,
        NEVER
    }

    private static final Pattern PLACEHOLDER_PATTERN =
        Pattern.compile("(?<indent> *)«(?<phase>[A-Z0-9_]+)(,(?<count>\\d+))?»");

    private final String template;
    private final String iterDescription;
        
    LoopTracingPuzzle(String template, String iterDescription) {
        this.template = template;
        this.iterDescription = iterDescription;
    }

    public static void generateRandomType(PuzzleContext ctx) {
        chooseConst(ctx, values()).generate(ctx);
    }

    void generate(PuzzleContext ctx) {
        ctx.output().paragraph("Consider the following code:");

        // Generate code, assign placeholders

        PlaceholderGenerator placeholders = new PlaceholderGenerator();
        Map<String,LoopPhase> placeholderPhases = new LinkedHashMap<>();
        var code = new StringBuilder();
        var matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            int maxReps = Integer.parseInt(
                Objects.requireNonNullElse(matcher.group("count"), "1"));
            int reps = ctx.getRandom().nextInt(maxReps) + 1;
            matcher.appendReplacement(code, "");
            for (int n = 0; n < reps; n++) {
                placeholders.next();
                placeholderPhases.put(placeholders.currentName(), LoopPhase.valueOf(matcher.group("phase")));
                if (n > 0) {
                    code.append("\n");
                }
                code.append(matcher.group("indent"));
                code.append(placeholders.current());
            }
        }
        matcher.appendTail(code);
        ctx.output().codeBlock(code.toString());

        // Choose iteration numbers

        var iterCounts = List.of(
            chooseWithProb(ctx, 0.7, 0, 1),
            chooseWithProb(ctx, 0.7, 2, 3)
        );

        // Give iter count and directions

        ctx.output().numberedList(
            iterCounts.stream().map(iterCount -> () -> {
                var directions =
                    iterDescription
                        .replaceAll("«LOOP_COUNT»", String.valueOf(iterCount))
                        .replaceAll("«LOOP_COUNT_PLUS_ONE»", String.valueOf(iterCount + 1))
                        .replaceAll("«LOOP_COUNT_PLURALITY»", iterCount == 1 ? "" : "s")
                    + " Write out the the order in which the statements will execute.";

                ctx.output().paragraph("{0}", directions);
            })
        );

        // Generate solution

        ctx.solution(() -> {
            ctx.output().numberedList(
                iterCounts.stream().map(iterCount -> () -> {
                    // For some reason, browsers like to hide the list number if the list item
                    // contains nothing but an x-scrolling element. This text is a workaround:
                    ctx.output().paragraph("Order:");

                    var solution = new ArrayList<String>();
                    appendPhases(solution, placeholderPhases, LoopPhase.BEFORE_LOOP);
                    for (int iter = 0; iter < iterCount; iter++) {
                        appendPhases(solution, placeholderPhases, LoopPhase.EACH_ITER, LoopPhase.EACH_ITER_EXCEPT_LAST);
                        appendPhases(solution, placeholderPhases, LoopPhase.AFTER_EACH_ITER);
                    }
                    appendPhases(solution, placeholderPhases, LoopPhase.EACH_ITER);
                    appendPhases(solution, placeholderPhases, LoopPhase.AFTER_LOOP);

                    ctx.output().codeBlock(String.join(" ", solution));
                })
            );
        });
    }

    void appendPhases(List<String> solution, Map<String,LoopPhase> placeholderPhases, LoopPhase... phases) {
        var includedPhases = List.of(phases);
        for (var entry : placeholderPhases.entrySet()) {
            String placeholder = entry.getKey();
            LoopPhase phase = entry.getValue();
            if (includedPhases.contains(phase)) {
                solution.add(placeholder);
            }
        }
    }
}
