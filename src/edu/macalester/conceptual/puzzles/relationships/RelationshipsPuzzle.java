package edu.macalester.conceptual.puzzles.relationships;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.CodeFormatting;
import edu.macalester.conceptual.util.Nonsense;
import edu.macalester.conceptual.util.Randomness;

public class RelationshipsPuzzle implements Puzzle {
    private static final Type[] OUTSIDE_TYPES = {
        new Type("String"),
        new Type("int"),
        new Type("List<String>"),
        new Type("File"),
        new Type("byte[]")
    };

    // All the types for the puzzler to diagram in part 1
    private final List<Type> allTypes = new ArrayList<>();

    // Path for which puzzler will write traversal code in part 2
    private final List<Relationship> relationshipChain = new ArrayList<>();
    private Type startOfChain, endOfChain;

    private RelationshipGenerator relGenerator = new RelationshipGenerator();

    @Override
    public byte id() {
        return 5;
    }

    @Override
    public String name() {
        return "rel";
    }

    @Override
    public String description() {
        return "Class relationships";
    }

    @Override
    public byte minDifficulty() {
        return 1;
    }

    @Override
    public byte maxDifficulty() {
        return 50;
    }

    @Override
    public byte goalDifficulty() {
        return 3;
    }

    @Override
    public void generate(PuzzleContext ctx) {
        // The chain of relationships we’ll ask the puzzler to traverse in part 2
        buildChain(ctx);

        // Additional random relationships so that solution requires actual thought
        addExtraRelatedTypes(ctx);
        addExtraProperties(ctx);

        for (var type : allTypes) {
            // So it’s not possible to find the solution by always traversing the first one
            type.shuffleRelationships(ctx);
        }

        var finalProperty = new Relationship.HasA(
            Nonsense.propertyName(ctx),
            Randomness.chooseConst(ctx, OUTSIDE_TYPES));
        endOfChain.add(finalProperty);

        ctx.output().paragraph("Consider the follow class declarations:");
        Collections.shuffle(allTypes, ctx.getRandom());
        ctx.output().codeBlock(
            allTypes.stream()
                .sorted(Comparator.comparing(Type::getName))
                .map(Type::buildDeclarationAst)
                .map(CodeFormatting::prettify)
                .collect(Collectors.joining("\n\n"))
        );

        ctx.output().numberedList(
            () -> {
                ctx.output().paragraph("Draw a diagram showing the class relationships.");
                ctx.output().paragraph(
                    """
                    You *only* need to diagram the classes listed above. You only need to show the
                    *name* of each class; do not show their methods or properties.
                    """);
                ctx.output().paragraph(
                    """
                    Draw arrows between the classes that have relationships, and label each arrow
                    with one of the following:
                    """);
                ctx.output().bulletList(
                    "*has a*",
                    "*has many*",
                    "*is a*");
                ctx.output().paragraph("Make sure your arrows point in the correct direction!");

                ctx.solution(() -> {
                    ctx.output().showGraphics(
                        "Class Diagram",
                        new RelationshipDiagramBuilder(allTypes, ctx.output().themeHue())
                            .build(startOfChain));  // startOfChain is tree root
                });
            },

            () -> {
                ctx.output().paragraph("Given the following variable:");
                var startVar = Nonsense.variableName(ctx);
                ctx.output().codeBlock(
                    startOfChain.getName() + " " + startVar);

                ctx.output().paragraph("...and the following method:");
                ctx.output().codeBlock(
                    "public void process(" + finalProperty.getPropertyType().getName() + " item)");

                String chainDescription = "`" + startVar + "`";
                for (var rel : relationshipChain) {
                    chainDescription = rel.buildDescription(chainDescription);
                }
                ctx.output().paragraph(
                    "...write code to process the " + finalProperty.getPropertyName()
                    + " of " + chainDescription + ".");

                ctx.solution(() -> {
                    var builder = new TraversalChainBuilder(new NameExpr(startVar));
                    for (var rel : relationshipChain) {
                        rel.buildTraversalCode(builder);
                    }
                    finalProperty.buildTraversalCode(builder);
                    builder.replaceExpression(expr ->
                        new MethodCallExpr("process", expr));

                    ctx.output().codeBlock(builder.getResult());
                });
            }
        );
    }

    private void buildChain(PuzzleContext ctx) {
        int chainLength = ctx.getDifficulty();

        startOfChain = new Type(ctx);
        allTypes.add(startOfChain);

        endOfChain = startOfChain;
        while (relationshipChain.size() < chainLength) {
            var rel = relGenerator.generate(ctx);
            allTypes.add(rel.getTargetType());
            relationshipChain.add(rel);
            endOfChain.add(rel);

            endOfChain = rel.getTargetType();
        }
    }

    /**
     * Adds class relationships for diagramming that will not appear in final method chain.
     */
    private void addExtraRelatedTypes(PuzzleContext ctx) {
        for (int n = ctx.getDifficulty() - 1; n > 0; ) {
            var sourceType = Randomness.choose(ctx, allTypes);
            var rel = relGenerator.generate(ctx);
            if (sourceType.canAdd(rel)) {
                sourceType.add(rel);
                allTypes.add(rel.getTargetType());
                n--;
            }
        }
    }

    /**
     * Adds simple properties (that won’t require diagramming) to make sure no classes are empty.
     */
    private void addExtraProperties(PuzzleContext ctx) {
        for (var type : allTypes) {
            if (type.getRelationships().size() < 2) {
                type.add(
                    new Relationship.HasA(
                        Nonsense.propertyName(ctx),
                        Randomness.chooseConst(ctx, OUTSIDE_TYPES)
                    )
                );
            }
        }
    }

    /**
     * Generates an endless sequence of random relationships to new types, cycling through all the
     * available kinds of relationships in a random order before repeating.
     */
    private static class RelationshipGenerator {
        private final List<Function<Type, Relationship>> generatorQueue = new ArrayList<>();

        Relationship generate(PuzzleContext ctx) {
            if (generatorQueue.isEmpty()) {
                generatorQueue.addAll(List.of(
                    type -> new Relationship.HasA(Nonsense.propertyName(ctx), type),
                    type -> new Relationship.IsA(type),
                    type -> new Relationship.HasMany(
                        Nonsense.propertyName(ctx), type, ctx.getRandom().nextBoolean())
                ));
                Collections.shuffle(generatorQueue, ctx.getRandom());
            }
            return generatorQueue.remove(0).apply(new Type(ctx));
        }
    }
}
