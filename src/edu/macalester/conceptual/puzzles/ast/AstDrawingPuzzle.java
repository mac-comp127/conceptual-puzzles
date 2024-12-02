package edu.macalester.conceptual.puzzles.ast;

import java.util.function.Function;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.Evaluator;
import edu.macalester.conceptual.util.VariablePool;

import static edu.macalester.conceptual.puzzles.ast.Generator.generateArithmeticComparisonsExpression;
import static edu.macalester.conceptual.puzzles.ast.Generator.generateArithmeticExpression;
import static edu.macalester.conceptual.puzzles.ast.Generator.generateStringAdditionExpression;
import static edu.macalester.conceptual.util.CodeFormatting.*;

public class AstDrawingPuzzle implements Puzzle {
    static final int
        DIFFICULTY_FOR_NEGATIONS = 3,
        DIFFICULTY_FOR_EQUALITY_OPERATORS_ON_BOOLS = 4;

    @Override
    public byte id() {
        return 2;
    }

    @Override
    public String name() {
        return "ast";
    }

    @Override
    public String description() {
        return "Drawing ASTs for expressions";
    }

    @Override
    public byte goalDifficulty() {
        return 2;
    }

    @Override
    public byte maxDifficulty() {
        return 50;
    }

    @Override
    public void generate(PuzzleContext ctx) {

        ctx.output().paragraph(
            "For each of the Java expressions below:");
        ctx.output().bulletList(
            "Draw an AST for the expression.",
            "Show how the expression evaluates at each node in the tree.");
        ctx.output().paragraph(
            "Be sure that your tree accurately reflects how Java would evaluate the expression.");

        ctx.section(() -> {
            outputPuzzle(ctx,
                (vars) -> generateArithmeticExpression(ctx, vars, ctx.getDifficulty() + 2),
                "Do you have the left and right branches on the correct side?",
                "Did you indicate the difference between ints and floating point values?");
        });

        ctx.section(() -> {
            int totalLeaves = ctx.getDifficulty() + 1;
            int arithmeticLeaves = (int) Math.pow(totalLeaves, 0.2);
            outputPuzzle(ctx,
                (vars) -> generateArithmeticComparisonsExpression(ctx, vars,
                    totalLeaves / arithmeticLeaves, arithmeticLeaves),
                "Does your tree correctly reflect the precedence of && and ||?",
                "Did you correctly show short-circuiting? (That is when the right branch of && or ||"
                    + " is never evaluated because the left branch already determines the answer.)");
        });

        ctx.section(() -> {
            outputPuzzle(ctx,
                (vars) -> generateStringAdditionExpression(
                    ctx, ctx.getDifficulty() + 3),
                "Did you indicate the distinction between ints and Strings?",
                "Are your left/right branches correct?");
        });
    }

    private static void outputPuzzle(
        PuzzleContext ctx,
        Function<VariablePool,String> exprGenerator,
        String... solutionChecklist
    ) {
        var code = generateValidExpr(exprGenerator);

        if (code.context().classMembers().isEmpty()) {
            ctx.output().paragraph("Draw the AST and evaluation results for the following expression:");
        } else {
            ctx.output().paragraph("Given the following variables:");
            ctx.output().codeBlock(prettifyStatements(code.context().classMembers()));
            ctx.output().paragraph("...draw the AST and evaluation results for the following expression:");
        }
        ctx.output().codeBlock(code.ast());

        ctx.solution(() -> {
            ctx.output().showGraphics(
                ctx.currentSectionTitle() + " Solution",
                AstDrawing.of(
                    code.ast(),
                    ctx.currentSectionHue()));  // coordinate graphics with section heading color in console

            ctx.solutionChecklist(solutionChecklist);
        });
    }

    /**
     * Repeatedly attempts to generate an expression that does not cause division by zero, NaNs, etc.
     */
    private static AnnotatedAst generateValidExpr(Function<VariablePool, String> exprGenerator) {
        VariablePool vars;
        String exprAsString;

        do {
            // Generate a random expression
            vars = new VariablePool();
            exprAsString = exprGenerator.apply(vars);
            var tree = AnnotatedAst.create(exprAsString, vars);

            try {
                // Try evaluating it. Does it fail parsing? Cause a division by zero error? etc.
                tree.attachValueAnnotations();  // Attaches evaluation results to tree
                tree.showShortCircuiting();     // Removes bool roads not taken

                // For heaven’s sake, don’t make students deal with NaN yet
                for (var subexpr : tree.subexprs()) {
                    AnnotatedAst.valueOf(subexpr).ifPresent(val -> {
                        if (val instanceof Double doubleVal && doubleVal.isNaN()) {
                            throw new Evaluator.EvaluationException(
                                new ArithmeticException("expr generates NaN"));
                        }
                    });
                }

                return tree;
            } catch (Evaluator.EvaluationException e) {
                if (e.getCause() instanceof ArithmeticException) {
                    // expression causes division by zero, NaN, or other eval issue; try again!
                } else {
                    throw e;  // code apparently didn’t compile, or is more severely broken
                }
            }
        } while(true);
    }
}
