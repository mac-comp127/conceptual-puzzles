package edu.macalester.conceptual.puzzles.types;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.puzzles.ast.AnnotatedAst;
import edu.macalester.conceptual.puzzles.ast.AstDrawing;
import edu.macalester.conceptual.util.CodeSnippet;
import edu.macalester.conceptual.util.VariablePool;

public class StaticAndRuntimeTypesPuzzle implements Puzzle {
    @Override
    public byte id() {
        return 6;
    }

    @Override
    public String name() {
        return "type";
    }

    @Override
    public String description() {
        return "Static and runtime types";
    }

    @Override
    public void generate(PuzzleContext ctx) {
        var vars = new VariablePool();
        vars.add("Foo", "f", "new Zoig()");
        var annotatedAst = AnnotatedAst.create(
            CodeSnippet.build()
                .withImports("import java.util.List;")
                .withMainBody("f.bar(\"fizz\".substring(1, 3), 13).get(0)")
                .withClassMembers(vars.allDeclarations())
                .withOtherClasses(
                    """
                    public interface Foo {
                        List<String> bar(String p, int x);
                    }
                    
                    public class Zoig implements Foo {
                        public List<String> bar(String p, int x) {
                            return List.of("Zoig says " + p);
                        }
                    }
                    
                    public class Flarf implements Foo {
                        public List<String> bar(String p, int x) {
                            return List.of("Flarf says " + p);
                        }
                    }
                    """
                )
        );

        ctx.output().paragraph(
            "Given the following type declarations:");
        ctx.output().codeBlock(annotatedAst.context().otherClasses());

        ctx.output().paragraph(
            "...and given the following variable declarations:");
        ctx.output().codeBlock(annotatedAst.context().classMembers());

        ctx.section(
            () -> {
                ctx.output().paragraph(
                    "Draw an AST for the following expression, labeling the *static types*:");
                ctx.output().codeBlock(annotatedAst.ast());
                ctx.solution(() -> {
                    annotatedAst.attachStaticTypeAnnotations();
                    ctx.output().showGraphics(
                        ctx.currentSectionTitle() + " Solution",
                        AstDrawing.of(
                            annotatedAst.ast(),
                            ctx.currentSectionHue()));
                });
            }
        );
        ctx.section(
            () -> {
                ctx.output().paragraph(
                    "Draw an AST for the same expression, labeling the *runtime types*.");
                ctx.solution(() -> {
                    annotatedAst.attachRuntimeTypeAnnotations();
                    ctx.output().showGraphics(
                        ctx.currentSectionTitle() + " Solution",
                        AstDrawing.of(
                            annotatedAst.ast(),
                            ctx.currentSectionHue()));
                });
            }
        );
    }
}
