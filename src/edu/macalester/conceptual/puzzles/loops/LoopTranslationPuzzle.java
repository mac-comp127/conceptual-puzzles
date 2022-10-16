package edu.macalester.conceptual.puzzles.loops;

import java.util.ArrayList;
import java.util.function.Consumer;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.CodeFormatting;
import edu.macalester.conceptual.util.Nonsense;

import static edu.macalester.conceptual.util.CodeFormatting.ELIDED;
import static edu.macalester.conceptual.util.CodeFormatting.insertAtRandomPosition;
import static edu.macalester.conceptual.util.CodeFormatting.joinStatements;
import static edu.macalester.conceptual.util.CodeFormatting.prettifyStatements;

public class LoopTranslationPuzzle implements Puzzle {
    @Override
    public byte id() {
        return 0;
    }

    @Override
    public String name() {
        return "loop";
    }

    @Override
    public String description() {
        return "While loops and for loops";
    }

    public void generate(PuzzleContext ctx) {
        ctx.section(() -> {
            boolean direction = ctx.getRandom().nextBoolean();
            var sourceForm = direction ? LoopForm.FOR : LoopForm.WHILE;
            var targetForm = direction ? LoopForm.WHILE : LoopForm.FOR;

            generateTranslationPuzzle(ctx, sourceForm, targetForm, true, null);
        });

        ctx.section(() -> {
            generateTranslationPuzzle(
                ctx, LoopForm.NATURAL_LANGUAGE, LoopForm.FOR, false,
                (loop) -> ctx.solutionChecklist(
                    "Did you use the correct operator in the loop’s end condition"
                    + " (`" + loop.getEndCondition() + "`)?"));
        });

        ctx.section(() -> {
            generateForEachTranslationPuzzle(ctx);
        });
    }

    private static void generateTranslationPuzzle(
        PuzzleContext ctx,
        LoopForm sourceForm,
        LoopForm targetForm,
        boolean includeBody,
        Consumer<SimpleLoop> extraSolutionAction
    ) {
        ctx.output().paragraph(
            "Translate the following " + sourceForm.description()
            + " into a " + targetForm.description() + ":");
        var loop = SimpleLoop.generateNumericLoop(ctx);
        if (!includeBody) {
            loop.setBody(CodeFormatting.ELIDED);
        }
        sourceForm.print(loop, ctx.output());
        ctx.solution(() -> {
            targetForm.print(loop, ctx.output());
            if (extraSolutionAction != null) {
                extraSolutionAction.accept(loop);
            }
        });
    }

    private void generateForEachTranslationPuzzle(PuzzleContext ctx) {
        ctx.output().paragraph(
            "Translate the following loop into a for-each loop:");

        var collType = Nonsense.typeName(ctx);
        var elemVar = Nonsense.withMinLength(3, () -> Nonsense.variableName(ctx));
        var collVar = Nonsense.pluralize(elemVar);
        var indexVar = ctx.choose("n", "i");
        String lengthExpr, indexedElemExpr;
        if (ctx.getRandom().nextBoolean()) {
            lengthExpr = collVar + ".length";
            indexedElemExpr = collVar + "[" + indexVar + "]";
        } else {
            lengthExpr = collVar + ".size()";
            indexedElemExpr = collVar + ".get(" + indexVar + ")";
        }

        var curElemPlaceholder = "•";
        var bodyParts = new ArrayList<String>();
        for (int n = ctx.getRandom().nextInt(2, 3); n > 0; n--) {
            var statementWithElem = ctx.choose(
                Nonsense.methodCall(ctx, ctx.getRandom().nextInt(3), curElemPlaceholder),
                curElemPlaceholder + "." + Nonsense.methodCall(ctx, ctx.getRandom().nextInt(3)));
            insertAtRandomPosition(ctx, bodyParts, statementWithElem);
        }
        for (int n = 0; n < ctx.getRandom().nextInt(3); n++) {
            String irrelevantStatement = Nonsense.methodCall(ctx, ctx.getRandom().nextInt(2));
            insertAtRandomPosition(ctx, bodyParts, irrelevantStatement);
        }
        var body = joinStatements(bodyParts);

        ctx.output().codeBlock(prettifyStatements(
            "List<" + collType + "> " + collVar + ";"
            + ELIDED));
        LoopForm.FOR.print(
            SimpleLoop.makeCounterLoop(indexVar, "0", lengthExpr,
            body.replaceAll("•", indexedElemExpr)),
            ctx.output());

        ctx.solution(() -> {
            ctx.output().codeBlock(prettifyStatements(
                "for(" + collType + " " + elemVar + ":" + collVar + "){"
                + body.replaceAll("•", elemVar)
                + "}"
                ));
            ctx.output().paragraph(
                """
                It is OK if you gave the variable for the individual collection element (`{0}`)
                a different name, such as `elem`. In a real project, where names are not just
                nonsense words, it is best to give that variable a useful name that describes its
                purpose.
                """,
                elemVar);
        });
    }

}
