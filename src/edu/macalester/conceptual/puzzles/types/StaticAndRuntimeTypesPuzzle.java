package edu.macalester.conceptual.puzzles.types;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ReferenceType;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.puzzles.ast.AnnotatedAst;
import edu.macalester.conceptual.puzzles.ast.AstDrawing;
import edu.macalester.conceptual.util.AstUtils;
import edu.macalester.conceptual.util.CodeSnippet;
import edu.macalester.conceptual.util.Nonsense;
import edu.macalester.conceptual.util.Randomness;

import static com.github.javaparser.ast.Modifier.Keyword.PUBLIC;
import static edu.macalester.conceptual.util.CodeFormatting.*;

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
        var supertype = AstUtils.publicTypeDecl(
            Nonsense.typeName(ctx),
            ctx.getRandom().nextBoolean());  // sometimes an interface, sometimes a class

        var subtypes = IntStream.range(0, 2).mapToObj(i -> {
            var subtype = AstUtils.publicClassDecl(Nonsense.typeName(ctx));
            if (supertype.isInterface()) {
                subtype.addImplementedType(supertype.getNameAsString());
            } else {
                subtype.addExtendedType(supertype.getNameAsString());
            }
            return subtype;
        }).toList();

        String polymorphicMethodName = Nonsense.methodName(ctx);
        String polymorphicParamName = Nonsense.variableName(ctx);
        addPolyMethod(ctx, supertype, polymorphicMethodName, supertype, polymorphicParamName, subtypes);
        for (var subtype : subtypes) {
            addPolyMethod(ctx, subtype, polymorphicMethodName, supertype, polymorphicParamName, subtypes);
        }

        var annotatedAst = AnnotatedAst.create(
            String.format(
                Randomness.chooseConst(ctx,
                    "foo.%1$s(%2$s).%1$s(bar)",
                    "foo.%1$s(%2$s.%1$s(bar))"
                ),
                polymorphicMethodName,
                Randomness.chooseConst(ctx, "foo", "bar")
            ),
            CodeSnippet.build()
                .withImports("import java.util.*;")
                .withMainBody(String.format(
                    """
                    %1$s foo = new %3$s();
                    %2$s bar = new %4$s();
                    """,
                    Randomness.chooseConst(ctx,
                        supertype.getNameAsString(),
                        subtypes.get(0).getNameAsString()),
                    Randomness.chooseConst(ctx,
                        supertype.getNameAsString(),
                        subtypes.get(1).getNameAsString()),
                    subtypes.get(0).getNameAsString(),
                    subtypes.get(1).getNameAsString()
                ))
                .withOtherClasses(
                    prettifyWholeFile(
                        supertype.toString()
                        + String.join("",
                            Randomness.shuffled(ctx,
                                subtypes.stream()
                                    .map(Objects::toString)
                                    .toList()
                            )
                        )
                    )
                )
        );

        ctx.output().paragraph(
            "Given the following type declarations:");
        ctx.output().codeBlock(annotatedAst.context().otherClasses());

        ctx.output().paragraph(
            "...and given the following setup code:");
        ctx.output().codeBlock(annotatedAst.context().mainBody());

        ctx.section(
            () -> {
                ctx.output().paragraph(
                    """
                    Draw an AST for the following expression, labeling the *static type* (a.k.a.
                    *compile-time type*) of each node in the tree:
                    """
                );
                ctx.output().codeBlock(annotatedAst.ast());
                ctx.output().paragraph(
                    """
                    (The static type of an expression is the type that the compiler uses to check
                    the code _before_ it runs.)
                    """
                );
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
                    """
                    Draw an AST for the same expression, this time labeling the *runtime type* of
                    each node in the tree.
                    """
                );
                ctx.output().paragraph(
                    """
                    (The runtime type of an expression is the type of the actual value that appears
                    when the code runs.)
                    """
                );
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

    //
    private static void addPolyMethod(
        PuzzleContext ctx,
        ClassOrInterfaceDeclaration type,
        String methodName,
        ClassOrInterfaceDeclaration paramTypeDecl,
        String paramName,
        List<ClassOrInterfaceDeclaration> subtypes
    ) {
        var method = type.addMethod(methodName, PUBLIC);
        ReferenceType paramType = AstUtils.classNamed(paramTypeDecl.getNameAsString());
        method.addParameter(paramType, paramName);
        method.setType(paramType);
        if (type.isInterface()) {
            method.removeBody();
        } else {
            method.setBody(AstUtils.blockOf(
                new ReturnStmt(
                    Randomness.choose(ctx,
                        () -> new ThisExpr(),
                        () -> new NameExpr(paramName),
                        () -> Randomness.choose(ctx,
                            subtypes.stream()
                                .map(t -> new ObjectCreationExpr(
                                    null,
                                    AstUtils.classNamed(t.getNameAsString()),
                                    AstUtils.nodes()))
                                .toList()
                        )
                    )
                )
            ));
        }
    }
}
