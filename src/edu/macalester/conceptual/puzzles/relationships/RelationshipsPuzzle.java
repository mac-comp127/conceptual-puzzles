package edu.macalester.conceptual.puzzles.relationships;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.AstUtils;
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

    private List<Type> allTypes = new ArrayList<>();  // All the types puzzler will have to diagram
    private List<Relationship> relationshipChain = new ArrayList<>();  // Types to traverse in code
    private Type startOfChain, endOfChain;

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
        return 100;
    }

    @Override
    public byte goalDifficulty() {
        return 3;
    }

    @Override
    public void generate(PuzzleContext ctx) {
        buildChain(ctx);

        addExtraProperties(ctx);

        for (var type : allTypes) {
            type.shuffleMembers(ctx);
        }

        var finalProperty = new Relationship.HasA(
            Nonsense.propertyName(ctx),
            Randomness.chooseConst(ctx, OUTSIDE_TYPES));
        endOfChain.add(finalProperty);

        ctx.output().paragraph("Consider the follow class declarations:");
        Collections.shuffle(allTypes, ctx.getRandom());
        ctx.output().codeBlock(
            allTypes.stream()
                .map(Type::declarationAst)
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
                    chainDescription = rel.buildDecription(chainDescription);
                }
                ctx.output().paragraph(
                    "...write code to process the " + finalProperty.getPropertyName()
                    + " of " + chainDescription + ".");

                ctx.solution(() -> {
                    var builder = new TraversalChainBuilder(new NameExpr(startVar));
                    for (var rel : relationshipChain) {
                        rel.buildCode(builder);
                    }
                    finalProperty.buildCode(builder);
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

        List<Function<Type, Relationship>> relationshipBuilders = new ArrayList<>();
        endOfChain = startOfChain;
        while (relationshipChain.size() < chainLength) {
            if (relationshipBuilders.isEmpty()) {
                relationshipBuilders.addAll(List.of(
                    type -> new Relationship.HasA(Nonsense.propertyName(ctx), type),
                    type -> new Relationship.IsA(type),
                    type -> new Relationship.HasMany(
                        Nonsense.propertyName(ctx), type, ctx.getRandom().nextBoolean())
                ));
                Collections.shuffle(relationshipBuilders, ctx.getRandom());
            }

            var nextType = new Type(ctx);
            allTypes.add(nextType);

            var rel = relationshipBuilders.removeLast().apply(nextType);
            relationshipChain.add(rel);
            endOfChain.add(rel);

            endOfChain = nextType;
        }
    }

    private void addExtraProperties(PuzzleContext ctx) {
        for (var type : allTypes) {
            type.add(
                new Relationship.HasA(
                    Nonsense.propertyName(ctx),
                    Randomness.chooseConst(ctx, OUTSIDE_TYPES)
                )
            );
        }
    }

    public static void main(String[] args) throws IOException {
        var ctx = PuzzleContext.generate((byte) 5, (byte) 3);
        ctx.enableSolution();
        ctx.emitPuzzle(
            () -> new RelationshipsPuzzle().generate(ctx));
    }
}
