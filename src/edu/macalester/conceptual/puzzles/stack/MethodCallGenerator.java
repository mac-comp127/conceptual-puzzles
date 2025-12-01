package edu.macalester.conceptual.puzzles.stack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.AstUtils;
import edu.macalester.conceptual.util.ChoiceDeck;
import edu.macalester.conceptual.util.Nonsense;
import edu.macalester.conceptual.util.Randomness;

import static com.github.javaparser.ast.NodeList.nodeList;
import static edu.macalester.conceptual.util.AstUtils.nodes;

class MethodCallGenerator {
    private final GeneratorContext genCtx;
    private final PuzzleContext ctx;

    MethodCallGenerator(GeneratorContext genCtx) {
        this.genCtx = genCtx;
        this.ctx = genCtx.ctx();
    }

    /**
     * Returns the stack trace that reaches the target marker.
     */
    List<VariableContainer> generate(
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

        // Add params (including implicit `this`) to both method decl and stack frame

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

        // Generate local vars, add to method body and stack frame

        int maxLocals = 2 + Math.min(4, ctx.getDifficulty()) - stackFrame.size();
        int localCount = ctx.getRandom().nextInt(0, Math.max(1, maxLocals));
        for(int n = 0; n < localCount; n++) {
            generateLocalVar(methodBody, stackFrame, isBranchBeingTraced);
        }

        // Add optional extraneous calls

        boolean complicateBefore = shouldComplicate((callDepth + 1) * 2, isBranchBeingTraced);
        boolean complicateAfter = shouldComplicate((callDepth + 1) * 2 - 1, isBranchBeingTraced);

        if (complicateBefore) {
            addComplication(stackFrame, methodBody, callDepth);
        }

        if (isBranchBeingTraced && genCtx.complexity().hasPropAssignmentsRemaining()) {
            addPropertyAssignment(stackFrame, methodBody);
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

        // Add optional extraneous call after the real one

        if (complicateAfter) {
            addComplication(stackFrame, methodBody, callDepth);
        }

        if (isBranchBeingTraced) {
            genCtx.complexity().countReferencesAsArrows(stackFrame.getVariables());
        }

        return result;
    }

    private boolean shouldComplicate(int callsRemaining, boolean isBranchBeingTraced) {
        if (
            isBranchBeingTraced
            && ctx.getRandom().nextFloat() < (float) genCtx.complexity().getComplicationsRemaining() / callsRemaining
        ) {
            genCtx.complexity().countComplication();
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

    private void addPropertyAssignment(VariableContainer stackFrame, BlockStmt methodBody) {
        for(int attempt = 0; attempt < 3; attempt++) {
            var allowSelfConnection = (attempt >= 2);
            if (attemptPropertyAssignment(stackFrame, methodBody, allowSelfConnection)) {
                genCtx.complexity().countPropAssignment();
                return;
            }
        }
    }

    private boolean attemptPropertyAssignment(
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

        genCtx.complexity().countArrow();

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

        return generate(
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
            genCtx.complexity().hasObjectsRemaining() ? 0.7 : 0,
            () -> {
                var varName = Nonsense.variableName(ctx);
                var obj = generateObject(isBranchBeingTraced);
                methodBody.addStatement(
                    AstUtils.variableDeclarationStmt(
                        obj.type().name(),
                        varName,
                        obj.instantiationExpr()
                ));
                stackFrame.addVariable(new Variable(varName, new Value.Reference(obj)));
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
        if (genCtx.complexity().hasObjectsRemaining()) {
            // instance method call to a new instance
            possibleArgs.add(() -> {
                var obj = generateObject(isBranchBeingTraced);
                return new ExprAndValue(
                    obj.instantiationExpr(),
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
        for (var puzzleClass : genCtx.puzzleClasses()) {
            if (genCtx.complexity().hasObjectsRemaining()) {
                // instance method call to a new instance
                possibleReceivers.add(() -> {
                    var obj = generateObject(isBranchBeingTraced);
                    return new MethodCallReceiver(
                        obj.instantiationExpr(),
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
        if (isBranchBeingTraced) {  // doesn't count if it won't be in the diagram
            genCtx.complexity().countObject();
        }
        return new StackPuzzleObject(
            Randomness.choose(ctx, genCtx.puzzleClasses()),
            ctx.getRandom().nextInt(1000)
        );
    }
}
