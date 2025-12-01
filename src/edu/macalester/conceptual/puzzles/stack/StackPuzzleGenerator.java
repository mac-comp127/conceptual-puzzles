package edu.macalester.conceptual.puzzles.stack;

import java.util.List;
import java.util.stream.IntStream;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.Nonsense;
import edu.macalester.conceptual.util.Randomness;

import static com.github.javaparser.ast.NodeList.nodeList;

class StackPuzzleGenerator {
    private final PuzzleContext ctx;
    private final GeneratorContext genCtx;

    // Generator output
    private final StackPuzzleClass entryPointClass;
    private final String entryPointMethod;
    private final List<VariableContainer> stack;

    StackPuzzleGenerator(PuzzleContext ctx) {
        this.ctx = ctx;

        genCtx = GeneratorContext.generate(ctx);

        entryPointClass = genCtx.puzzleClasses().getFirst();
        entryPointMethod = Nonsense.methodName(ctx);

        var methodGenerator = new MethodCallGenerator(genCtx);

        stack = methodGenerator.generate(
            entryPointClass,
            null,  // static method
            entryPointMethod,
            List.of(),  // no args
            ctx.getDifficulty(),
            true  // trace this branch: place marker when we reach leaf node, return stack trace
        );
    }

    public boolean isWellBalanced() {
        return genCtx.complexity().isWellBalanced();
    }

    public void outputPuzzle() {
        ctx.output().paragraph("Given the code below, this method call:");
        ctx.output().codeBlock(entryPointClass + "." + entryPointMethod + "();");
        ctx.output().paragraph("...will eventually reach the point marked ___HERE___.");
        ctx.output().paragraph(
            """
            Draw a diagram of the stack frames and objects (not classes, but _objects_) that exist
            at that point. In your diagram:
            """
        );
        ctx.output().bulletList(
            "Label each stack frame with the name of the method.",
            "Label each object with the name of its class.",
            """
            Include the names of all the variables that belong to each object and stack frame,
            including the implicit `this` parameter if present.
            (You do not need to write the types of any variables.)
            """,
            """
            When a variable's value is null or a primitive, write the value immediately next to the
            variable.
            """,
            """
            When a variable points to an object, draw an arrow from the variable to the object it
            points to.
            """
        );

        for (var aClass : genCtx.puzzleClasses()) {
            ctx.output().codeBlock(aClass.buildDeclaration(ctx));
        }

        ctx.solution(() -> {
            ctx.output().showGraphics(
                "Solution",
                CallStackDiagram.of(stack, ctx.currentSectionHue())
            );

            ctx.output().paragraph("Hints for practicing this puzzle:");
            ctx.output().bulletList(
                """
                Practice with difficulty 0, then work your way up. This will help you get a handle
                on what the puzzle is asking.
                """,
                """
                There is only one path through the code from the starting method call to ___HERE___.
                Start by finding that path.
                """,
                """
                Be thorough. Double check that you've fully diagrammed each step before you move on
                to the next one.
                """
            );
        });
    }
}
