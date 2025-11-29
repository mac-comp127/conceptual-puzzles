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
    private int objectsRemaining, complicationsRemaining, propAssignmentsRemaining;

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
        return 10;
    }

    @Override
    public void generate(PuzzleContext ctx) {
        this.ctx = ctx;

        puzzleClasses = IntStream.range(0, Math.max(2, ctx.getDifficulty() / 3))
            .mapToObj(n -> StackPuzzleClass.generate(ctx))
            .toList();

        int propertyCount = ctx.getDifficulty() * puzzleClasses.size() / 2;
        for(int n = 0; n < propertyCount; n++) {
            var puzzleClass = Randomness.choose(ctx, puzzleClasses);
            var propType = Randomness.choose(ctx, puzzleClasses);
            puzzleClass.addProperty(Nonsense.shortPropertyName(ctx), propType);
        }

        objectsRemaining = ctx.getDifficulty() + 2;
        complicationsRemaining = ctx.getDifficulty();
        propAssignmentsRemaining = 1 + ctx.getDifficulty() / 2;

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

        int maxLocals = 2 + Math.min(4, ctx.getDifficulty()) - stackFrame.size();
        int localCount = ctx.getRandom().nextInt(0, Math.max(1, maxLocals));
        for(int n = 0; n < localCount; n++) {
            generateLocalVar(methodBody, stackFrame, isBranchBeingTraced);
        }

        // Optional extraneous calls

        boolean complicateBefore = shouldComplicate((callDepth + 1) * 2, isBranchBeingTraced);
        boolean complicateAfter = shouldComplicate((callDepth + 1) * 2 - 1, isBranchBeingTraced);

        if (complicateBefore) {
            addComplication(stackFrame, methodBody, callDepth);
        }

        if (propAssignmentsRemaining > 0 && isBranchBeingTraced) {
            for(int attempt = 0; attempt < 3; attempt++) {
                var allowSelfConnection = (attempt >= 2);
                if (addPropertyAssignment(stackFrame, methodBody, allowSelfConnection)) {
                    propAssignmentsRemaining--;
                    break;
                }
            }
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

    private boolean addPropertyAssignment(
        VariableContainer stackFrame,
        BlockStmt methodBody,
        boolean allowSelfConnection
    ) {
        // Pick a random available variable whose value is an object (and not null)

        var varsWithObjectValue = stackFrame.getVariables().stream()
            .filter(v -> v.value() instanceof Value.Reference)
            .toList();
        if (varsWithObjectValue.isEmpty()) {
            return false;
        }
        var lhsVar = Randomness.choose(ctx, varsWithObjectValue);
        var lhsObj = ((Value.Reference) lhsVar.value()).object();
        var lhsType = lhsObj.type();

        // Pick a random property from that type

        if (lhsType.properties().isEmpty()) {
            return false;
        }
        var lhsProp = Randomness.choose(ctx, lhsType.properties());

        // Pick a random available value we can assign to that property

        var availableRhsVars = varsWithObjectValue.stream()
            .filter(v -> v.value().typeName().equals(lhsProp.type().name()))
            .filter(v -> allowSelfConnection || ((Value.Reference) v.value()).object() != lhsObj)
            .toList();
        if (availableRhsVars.isEmpty()) {
            return false;
        }
        var rhsVar = Randomness.choose(ctx, availableRhsVars);

        // Assign it!

        methodBody.addStatement(
            new MethodCallExpr(
                new NameExpr(lhsVar.name()),
                lhsProp.setterName(),
                nodes(new NameExpr(rhsVar.name()))
            )
        );
        lhsObj.setProperty(lhsProp, (Value.Reference) rhsVar.value());

        return true;
    }

    private List<VariableContainer> generateMethodAndCall(
        VariableContainer stackFrame,
        BlockStmt methodBody,
        int callDepth,
        boolean isBranchBeingTraced
    ) {
        // Choose receiver next method call

        var nextReceiver = chooseMethodCallReceiver(stackFrame, isBranchBeingTraced);

        // Generate args for next method call

        var exprChoices = gatherExprChoices(stackFrame, isBranchBeingTraced);
        var nextArgs = IntStream.range(0, ctx.getRandom().nextInt(0, 4))
            .mapToObj(i -> exprChoices.draw().get())
            .toList();

        // Call the new method

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

        // Declare the new method

        return generateMethod(
            nextReceiver.type(),
            nextReceiver.object(),
            nextMethodName,
            nextArgs.stream().map(ExprAndValue::value).toList(),
            callDepth - 1,
            isBranchBeingTraced
        );
    }

    private void generateLocalVar(
        BlockStmt methodBody,
        VariableContainer stackFrame,
        boolean isBranchBeingTraced
    ) {
        Runnable choice = Randomness.chooseWithProb(ctx,
            // mostly objects, a few ints...unless we've made a lot of objects already
            (objectsRemaining > 0) ? 0.7 : 0,
            () -> {
                var varName = Nonsense.variableName(ctx);
                var obj = generateObject(isBranchBeingTraced);
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
                    varName, Value.makeIntValue(intValue)));
            }
        );
        choice.run();
    }

    private record ExprAndValue(
        Expression expression,
        Value value
    ) { }

    private ChoiceDeck<Supplier<ExprAndValue>> gatherExprChoices(
        VariableContainer stackFrame,
        boolean isBranchBeingTraced
    ) {
        var possibleArgs = new ArrayList<Supplier<ExprAndValue>>();
        for (var variable : stackFrame.getVariables()) {
            possibleArgs.add(() ->
                new ExprAndValue(
                    new NameExpr(variable.name()),
                    variable.value()
                )
            );
        }
        if (objectsRemaining > 0) {
            // instance method call to a new instance
            possibleArgs.add(() -> {
                var obj = generateObject(isBranchBeingTraced);
                return new ExprAndValue(
                    newObjectExpr(obj),
                    new Value.Reference(obj)
                );
            });
        }
        if (possibleArgs.isEmpty() || ctx.getRandom().nextFloat() < 0.2) {
            // pass an int constant
            possibleArgs.add(() -> {
                var intValue = String.valueOf(ctx.getRandom().nextInt(100));
                return new ExprAndValue(
                    new IntegerLiteralExpr(intValue),
                    Value.makeIntValue(intValue)
                );
            });
        }
        return new ChoiceDeck<>(ctx, possibleArgs);
    }

    private record MethodCallReceiver(
        Expression expression,
        StackPuzzleClass type,
        StackPuzzleObject object  // null if static method
    ) { }

    private MethodCallReceiver chooseMethodCallReceiver(
        VariableContainer stackFrame,
        boolean isBranchBeingTraced
    ) {
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
            if (objectsRemaining > 0) {
                // instance method call to a new instance
                possibleReceivers.add(() -> {
                    var obj = generateObject(isBranchBeingTraced);
                    return new MethodCallReceiver(
                        newObjectExpr(obj),
                        puzzleClass,
                        obj
                    );
                });
            }

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

    private StackPuzzleObject generateObject(boolean isBranchBeingTraced) {
        if (isBranchBeingTraced) {
            objectsRemaining--;  // doesn't count if it won't be in the diagram
        }
        return new StackPuzzleObject(
            Randomness.choose(ctx, puzzleClasses),
            ctx.getRandom().nextInt(1000)
        );
    }

    private static Expression newObjectExpr(StackPuzzleObject obj) {
        return new ObjectCreationExpr(
            null,
            classNamed(obj.type().name()),
            nodes(new IntegerLiteralExpr(String.valueOf(obj.id()))));
    }
}
