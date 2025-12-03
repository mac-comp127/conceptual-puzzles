package edu.macalester.conceptual.puzzles.classes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.javaparser.ast.Modifier;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.puzzles.classes.feature.ClassFeature;
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
        return "class";
    }

    @Override
    public String description() {
        return "Class declarations and object modeling";
    }

    @Override
    public byte goalDifficulty() {
        return 2;
    }

    @Override
    public byte maxDifficulty() {
        return 100;
    }

    @Override
    public void generate(PuzzleContext ctx) {
        var className = typeName(ctx);

        // A ClassFeature is anything that shows up as a bullet-listed item in the specification
        // and translates into class declaration code.

        var features = new ArrayList<ClassFeature>();

        // First we add several random property features to the class, all of which are independent
        // of each other:

        var featureDeck = new ChoiceDeck<Supplier<ClassFeature>>(ctx, List.of(
            () -> ClassFeature.generateImmutableProperty(ctx),
            () -> ClassFeature.generateMutableProperty(ctx),
            () -> ClassFeature.generateInternalState(ctx),
            () -> ClassFeature.generateStaticVariable(ctx),
            () -> ClassFeature.generateStaticConstant(ctx)
        ));

        int numFeatures = ctx.getDifficulty();
        for (int n = 0; n < numFeatures; n++) {
            features.add(featureDeck.draw().get());
        }

        // We then add “complications,” features that depend on other features:

        int numComplications = Math.max(0, ctx.getDifficulty() - 1);
        var complicationsDeck = new ChoiceDeck<Supplier<ClassFeature>>(ctx, List.of(
            () -> withRandomStateVariable(ctx, features, false,
                stateVariable -> ClassFeature.generateMutatingBehavior(ctx, stateVariable)),
            () -> withRandomStateVariable(ctx, features, true,
                stateVariable -> ClassFeature.generateComputedProperty(ctx, stateVariable))
        ));
        for (int n = 0; n < numComplications; ) {
            var feature = complicationsDeck.draw().get();
            if (feature != null) {
                features.add(feature);
                n++;
            }
        }

        // We have a class spec!

        ctx.output().paragraph(
            "Translate the specification below into an idiomatic Java class definition.");
        ctx.output().paragraph(
            """
            (In this context, "idiomatic" means following the common style and conventions of
            the language.)
            """);

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
                """
                Are the class members in the correct order?
                It should be: (1) static variables, (2) instance variables,
                (3) constructors, (4) methods.
                """,
                "Do members have the correct capitalization?");
            ctx.output().paragraph("Acceptable variations in the solution:");
            ctx.output().bulletList(
                """
                It is OK if you initialized *non-static* variables"
                in the constructor instead of in the declaration.
                (Ask your instructor if you aren’t clear what this means.)
                Note that *static* variables _must_ be initialized in the declaration.
                """,
                """
                It is OK if you used a slightly different but equivalent form of an expression,
                such as `+= 1` instead of `++`.
                """);
        });
    }

    /**
     * A utility for generating features that depend on state variables from other features.
     * We check for available state variables matching the parameters, and call the generator with
     * a random one only if any were available.
     */
    private <T> T withRandomStateVariable(
        PuzzleContext ctx,
        List<ClassFeature> features,
        boolean allowImmutable,
        Function<ClassFeature.StateVariable, T> featureGenerator
    ) {
        var stateVariables = features.stream()
            .flatMap(feature -> feature.getStateVariables().stream())
            .filter(v -> allowImmutable || v.isMutable())
            .toList();

        // Bail if no variables available: for example, if there are no mutable variables and we’re
        // trying to generate a mutating behavior
        if (stateVariables.isEmpty()) {
            return null;
        }

        var stateVariable = Randomness.choose(ctx, stateVariables);
        return featureGenerator.apply(stateVariable);
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
        var ctor = classDecl.addConstructor(Modifier.Keyword.PUBLIC);

        for (var feature : features) {
            feature.addToCode(classDecl, ctor);
        }

        AstUtils.orderMembersByJavaConventions(classDecl);

        ctx.output().codeBlock(classDecl);
    }
}
