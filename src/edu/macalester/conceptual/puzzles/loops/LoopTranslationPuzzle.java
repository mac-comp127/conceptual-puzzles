package edu.macalester.conceptual.puzzles.loops;

import static edu.macalester.conceptual.util.CodeFormatting.ELIDED;
import static edu.macalester.conceptual.util.CodeFormatting.joinStatements;
import static edu.macalester.conceptual.util.CodeFormatting.prettifyStatements;
import static edu.macalester.conceptual.util.Randomness.choose;
import static edu.macalester.conceptual.util.Randomness.chooseConst;
import static edu.macalester.conceptual.util.Randomness.insertAtRandomPosition;

import java.util.ArrayList;
import java.util.function.Consumer;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.CodeFormatting;
import edu.macalester.conceptual.util.Nonsense;
import edu.macalester.conceptual.util.Randomness;

public class LoopTranslationPuzzle {

    public static void generateNaturalLanguage(PuzzleContext ctx) {
        generate(
            ctx, LoopForm.NATURAL_LANGUAGE, LoopForm.FOR, false,
            (loop) -> ctx.solutionChecklist(
                "Note that the problem says “until,” not “while.”"
                + " Did you use the correct operator in the loop’s end condition"
                + " (`" + loop.getEndCondition() + "`)?"));
    }

    public static void generateForAndWhile(PuzzleContext ctx) {
        boolean direction = ctx.getRandom().nextBoolean();
        var sourceForm = direction ? LoopForm.FOR : LoopForm.WHILE;
        var targetForm = direction ? LoopForm.WHILE : LoopForm.FOR;

        generate(ctx, sourceForm, targetForm, true, null);
    }

    private static void generate(
        PuzzleContext ctx,
        LoopForm sourceForm,
        LoopForm targetForm,
        boolean includeBody,
        Consumer<GeneralizedLoop> extraSolutionAction
    ) {
        ctx.output().paragraph(
            "Translate the following " + sourceForm.description()
            + " into a " + targetForm.description() + ":");
        var loop = GeneralizedLoop.generateNumericLoop(ctx);
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

    public static void generateForEach(PuzzleContext ctx) {
        ctx.output().paragraph(
            "Translate the following loop into a for-each loop:");

        var elemType = Nonsense.typeName(ctx);
        var elemVar = Randomness.withMinLength(3, () -> Nonsense.variableName(ctx));
        var collVar = Nonsense.pluralize(elemVar);
        var indexVar = chooseConst(ctx, "n", "i");
        String collectionType, lengthExpr, indexedElemExpr;
        if (ctx.getRandom().nextBoolean()) {
            collectionType = elemType + "[]";
            lengthExpr = collVar + ".length";
            indexedElemExpr = collVar + "[" + indexVar + "]";
        } else {
            collectionType = "List<" + elemType + ">";
            lengthExpr = collVar + ".size()";
            indexedElemExpr = collVar + ".get(" + indexVar + ")";
        }

        var curElemPlaceholder = "•";
        var bodyParts = new ArrayList<String>();
        for (int n = ctx.getRandom().nextInt(2, 3); n > 0; n--) {
            var statementWithElem = choose(ctx,
                () -> Nonsense.methodCall(ctx, ctx.getRandom().nextInt(3), curElemPlaceholder),
                () -> curElemPlaceholder + "." + Nonsense.methodCall(ctx, ctx.getRandom().nextInt(3)));
            insertAtRandomPosition(ctx, bodyParts, statementWithElem);
        }
        for (int n = 0; n < ctx.getRandom().nextInt(3); n++) {
            String irrelevantStatement = Nonsense.methodCall(ctx, ctx.getRandom().nextInt(2));
            insertAtRandomPosition(ctx, bodyParts, irrelevantStatement);
        }
        var body = joinStatements(bodyParts);

        ctx.output().codeBlock(prettifyStatements(
            collectionType + " " + collVar + ";"
            + ELIDED));
        LoopForm.FOR.print(
            GeneralizedLoop.makeCounterLoop(indexVar, "0", lengthExpr,
            body.replaceAll("•", indexedElemExpr)),
            ctx.output());

        ctx.solution(() -> {
            ctx.output().codeBlock(prettifyStatements(
                "for(" + elemType + " " + elemVar + ":" + collVar + "){"
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
