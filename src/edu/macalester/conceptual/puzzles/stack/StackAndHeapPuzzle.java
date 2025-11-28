package edu.macalester.conceptual.puzzles.stack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.AstUtils;
import edu.macalester.conceptual.util.ChoiceDeck;
import edu.macalester.conceptual.util.Nonsense;
import edu.macalester.conceptual.util.Randomness;

import static com.github.javaparser.ast.NodeList.nodeList;
import static edu.macalester.conceptual.util.AstUtils.classNamed;
import static edu.macalester.conceptual.util.AstUtils.nodes;

public class StackAndHeapPuzzle implements Puzzle {
    private PuzzleContext ctx;
    private List<StackPuzzleClass> puzzleClasses;
    private int complicationsRemaining;

    @Override
    public byte id() {
        return 9;
    }

    @Override
    public String name() {
        return "stack";
    }

    @Override
    public String description() {
        return "Stack frames and objects (like the Idea Lab activity)";
    }

    @Override
    public byte minDifficulty() {
        return 0;
    }

    @Override
    public byte goalDifficulty() {
        return 2;
    }

    @Override
    public byte maxDifficulty() {
        return 30;
    }

    @Override
    public void generate(PuzzleContext ctx) {
        this.ctx = ctx;

        puzzleClasses = IntStream.range(0, Math.max(2, ctx.getDifficulty()))
            .mapToObj(n -> StackPuzzleClass.generate(ctx))
            .toList();

        complicationsRemaining = ctx.getDifficulty();

        var entryPointName = Nonsense.methodName(ctx);

        var stack = generateMethod(
            puzzleClasses.getFirst(),
            null,  // static method
            entryPointName,
            List.of(),  // no args
            ctx.getDifficulty(),
            true  // trace this branch: place marker when we reach leaf node, return stack trace
        );

        for (var aClass : puzzleClasses) {
            ctx.output().codeBlock(aClass.buildDeclaration(ctx));
        }

        ctx.solution(() -> {
            ctx.output().showGraphics(
                "Solution",
                CallStackDiagram.of(stack, ctx.currentSectionHue()));
        });
    }

    /**
     * Returns the stack trace that reaches the target marker.
     */
    private List<VariableContainer> generateMethod(
        StackPuzzleClass type,
        StackPuzzleObject receiver,  // null if static method
        String name,
        List<Value> args,
        int callDepth,
        boolean isBranchBeingTraced
    ) {
        var stackFrame = new VariableContainer(name);

        var methodDecl = type.addMethod(name);
        var methodBody = new BlockStmt();
        methodDecl.setBody(methodBody);

        // Params (including implicit `this`)

        if (receiver != null) {
            stackFrame.addVariable(new Variable("this", new Value.Reference(receiver)));
        } else {
            methodDecl.setStatic(true);
        }
        for (var arg : args) {
            String paramName = Nonsense.variableName(ctx);
            methodDecl.addParameter(AstUtils.classNamed(arg.typeName()), paramName);
            stackFrame.addVariable(new Variable(paramName, arg));
        }

        // Local vars

        int localCount = ctx.getRandom().nextInt(0, Math.max(2, 4 - stackFrame.size()));
        for(int n = 0; n < localCount; n++) {
            generateLocalVar(methodBody, stackFrame);
        }

        // Optional extraneous calls

        boolean complicateBefore = shouldComplicate((callDepth + 1) * 2, isBranchBeingTraced);
        boolean complicateAfter = shouldComplicate((callDepth + 1) * 2 - 1, isBranchBeingTraced);

        if (complicateBefore) {
            addComplication(stackFrame, methodBody, callDepth);
        }

        // Generate either the next call in the chain or the leaf node of this chain

        var result = new ArrayList<VariableContainer>();
        result.add(stackFrame);

        if (callDepth <= 0) {
            // We've reached the end of the call chain
            if (isBranchBeingTraced) {
                methodBody.addStatement(new ExpressionStmt(new NameExpr("___HERE___")));
            }
        } else {
            // Generate next method call in chain
            var calleeStack = generateMethodAndCall(
                stackFrame, methodBody, callDepth, isBranchBeingTraced);
            result.addAll(calleeStack);
        }

        // Optional extraneous call after the real one

        if (complicateAfter) {
            addComplication(stackFrame, methodBody, callDepth);
        }

        return result;
    }

    private boolean shouldComplicate(int callsRemaining, boolean isBranchBeingTraced) {
        if (
            isBranchBeingTraced
            && ctx.getRandom().nextFloat() < (float) complicationsRemaining / callsRemaining
        ) {
            complicationsRemaining--;
            return true;
        }
        return false;
    }

    private void addComplication(
        VariableContainer stackFrame,
        BlockStmt methodBody,
        int callDepth
    ) {
        generateMethodAndCall(stackFrame, methodBody, callDepth / 2, false);
        // Note that we ignore the returned stack trace: this isn't a call we're visualizing!
    }

    private List<VariableContainer> generateMethodAndCall(
        VariableContainer stackFrame,
        BlockStmt methodBody,
        int callDepth,
        boolean isBranchBeingTraced
    ) {
        // Choose receiver next method call

        var nextReceiver = chooseMethodCallReceiver(stackFrame);

        // Generate args for next method call

        var exprChoices = gatherExprChoices(stackFrame);
        var nextArgs = IntStream.range(0, ctx.getRandom().nextInt(0, 4))
            .mapToObj(i -> exprChoices.draw().get())
            .toList();

        // Generate next method call

        var nextMethodName = Nonsense.methodName(ctx);

        methodBody.addStatement(new ExpressionStmt(
            new MethodCallExpr(
                nextReceiver.expression(),
                nextMethodName,
                nodeList(
                    nextArgs.stream()
                        .map(ExprAndValue::expression)
                        .toList()
                )
            )
        ));

        // Generate method to be called

        return generateMethod(
            nextReceiver.type(),
            nextReceiver.object(),
            nextMethodName,
            nextArgs.stream().map(ExprAndValue::value).toList(),
            callDepth - 1,
            isBranchBeingTraced
        );
    }

    private void generateLocalVar(BlockStmt methodBody, VariableContainer stackFrame) {
        Runnable choice = Randomness.chooseWithProb(ctx,
            0.7,  // mostly objects, a few ints
            () -> {
                var varName = Nonsense.variableName(ctx);
                var obj = generateObject();
                methodBody.addStatement(
                    AstUtils.variableDeclarationStmt(
                        obj.type().name(),
                        varName,
                        newObjectExpr(obj)
                ));
                stackFrame.addVariable(new Variable(
                    varName, new Value.Reference(obj)));
            },
            () -> {
                var varName = Nonsense.variableName(ctx);
                var intValue = String.valueOf(ctx.getRandom().nextInt(100));
                methodBody.addStatement(
                    AstUtils.variableDeclarationStmt(
                        "int", varName, new IntegerLiteralExpr(intValue)));
                stackFrame.addVariable(new Variable(
                    varName, new Value.InlineValue("int", intValue)));
            }
        );
        choice.run();
    }

    private record ExprAndValue(
        Expression expression,
        Value value
    ) { }

    private ChoiceDeck<Supplier<ExprAndValue>> gatherExprChoices(VariableContainer stackFrame) {
        var possibleArgs = new ArrayList<Supplier<ExprAndValue>>();
        for (var variable : stackFrame.getVariables()) {
            possibleArgs.add(() ->
                new ExprAndValue(
                    new NameExpr(variable.name()),
                    variable.value()
                )
            );
        }
        // instance method call to a new instance
        possibleArgs.add(() -> {
            var obj = generateObject();
            return new ExprAndValue(
                newObjectExpr(obj),
                new Value.Reference(obj)
            );
        });
        return new ChoiceDeck<>(ctx, possibleArgs);
    }

    private record MethodCallReceiver(
        Expression expression,
        StackPuzzleClass type,
        StackPuzzleObject object  // null if static method
    ) { }

    private MethodCallReceiver chooseMethodCallReceiver(VariableContainer stackFrame) {
        var possibleReceivers = new ArrayList<Supplier<MethodCallReceiver>>();
        for (var variable : stackFrame.getVariables()) {
            if (variable.value() instanceof Value.Reference ref) {
                // instance method call to a local var
                possibleReceivers.add(() ->
                    new MethodCallReceiver(
                        new NameExpr(variable.name()),
                        ref.object().type(),
                        ref.object()
                    )
                );
            }
        }
        for (var puzzleClass : puzzleClasses) {
            // instance method call to a new instance
            possibleReceivers.add(() -> {
                var obj = generateObject();
                return new MethodCallReceiver(
                    newObjectExpr(obj),
                    puzzleClass,
                    obj
                );
            });

            // static method call
            possibleReceivers.add(() ->
                new MethodCallReceiver(
                    new NameExpr(puzzleClass.name()),
                    puzzleClass,
                    null
                )
            );
        }
        return Randomness.choose(ctx, possibleReceivers).get();
    }

    private StackPuzzleObject generateObject() {
        return new StackPuzzleObject(Randomness.choose(ctx, puzzleClasses), ctx.getRandom().nextInt(1000));
    }

    private static Expression newObjectExpr(StackPuzzleObject obj) {
        return new ObjectCreationExpr(
            null,
            classNamed(obj.type().name()),
            nodes(new IntegerLiteralExpr(String.valueOf(obj.id()))));
    }
}
