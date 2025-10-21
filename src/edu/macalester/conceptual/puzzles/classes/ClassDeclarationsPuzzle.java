package edu.macalester.conceptual.puzzles.classes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import com.github.javaparser.ast.Modifier;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.AstUtils;
import edu.macalester.conceptual.util.ChoiceDeck;
import edu.macalester.conceptual.util.Randomness;

import static edu.macalester.conceptual.util.AstUtils.publicTypeDecl;
import static edu.macalester.conceptual.util.Nonsense.resolveIndefiniteArticles;
import static edu.macalester.conceptual.util.Nonsense.typeName;

public class ClassDeclarationsPuzzle implements Puzzle {
    @Override
    public byte id() {
        return 8;
    }

    @Override
    public String name() {
        return "clas";
    }

    @Override
    public String description() {
        return "Class declarations and object modeling";
    }

    @Override
    public byte goalDifficulty() {
        return 3;
    }

    @Override
    public byte maxDifficulty() {
        return 100;
    }

    @Override
    public void generate(PuzzleContext ctx) {
        var className = typeName(ctx);

        var featureDeck = new ChoiceDeck<Supplier<ClassFeature>>(ctx, List.of(
            () -> SimpleProperty.generateImmutable(ctx),
            () -> SimpleProperty.generateMutable(ctx),
            () -> InternalState.generate(ctx),
            () -> StaticVariable.generate(ctx),
            () -> StaticConstant.generate(ctx)
        ));

        int numFeatures = ctx.getDifficulty();
        var features = new ArrayList<>(
            IntStream.range(0, numFeatures)
                .mapToObj(n -> featureDeck.draw().get())
                .toList());

        var allStateVariables = features.stream()
            .flatMap(feature -> feature.getStateVariables().stream())
            .toList();
        var stateVariable = Randomness.choose(ctx, allStateVariables);
        features.add(ComputedProperty.generate(ctx, stateVariable));

        ctx.output().paragraph(
            "Translate the specification below into an idiomatic Java class definition:");

        outputSpecification(ctx, className, features);

        ctx.solution(() -> {
            outputClassDeclaration(ctx, className, features);

            ctx.output().dividerLine(false);
            ctx.output().paragraph("Things to check in your solution:");
            ctx.output().bulletList(
                "Is every `public` and `private` modifier correct?",
                "Are the correct items `static`?",
                "Are the correct items `final`?",
                "Are all the variable types and method return types correct?",
                "Are the class members in the correct order?"
                    + " It should be: (1) static variables, (2) instance variables,"
                    + " (3) constructors, (4) methods.",
                "Do members have the correct capitalization?");
            ctx.output().paragraph("Acceptable variations in the solution:");
            ctx.output().bulletList(
                "It is OK if you initialized *non-static* variables"
                    + " in the constructor instead of in the declaration."
                    + " (Ask your instructor if you aren’t clear what this means.)"
                    + " Note that *static* variables _must_ be initialized in the declaration.",
                "It is OK if you used a slightly different but equivalent form of an expression,"
                    + " such as `+= 1` instead of `++`.");
        });
    }

    private static void outputSpecification(PuzzleContext ctx, String className, List<ClassFeature> features) {
        List<Runnable> specItems = new ArrayList<>();
        specItems.add(() ->
            ctx.output().paragraph(
                resolveIndefiniteArticles(
                    "One kind of thing that exists in our model is a/an `" + className + "`.")));

        for (var feature : features) {
            specItems.add(() ->
                ctx.output().paragraph(
                    resolveIndefiniteArticles(
                        feature.describeInWords(className))));
        }

        ctx.output().numberedList(specItems);
    }

    private static void outputClassDeclaration(PuzzleContext ctx, String className, List<ClassFeature> features) {
        var classDecl = publicTypeDecl(className, false);

        for (var feature : features) {
            feature.addToClassDeclaration(classDecl);
        }

        var ctor = classDecl.addConstructor(Modifier.Keyword.PUBLIC);
        for (var feature : features) {
            feature.addToConstructor(ctor);
        }

        AstUtils.orderMembersByJavaConventions(classDecl);

        ctx.output().codeBlock(classDecl);
    }
}
