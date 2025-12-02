package edu.macalester.conceptual.puzzles.stack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import com.github.javaparser.ast.body.MethodDeclaration;
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

/**
 * The heart of the generation process for this puzzle. The constructor generates a single method
 * call <i>and</i> all of its callees: an entire subtree of the method call tree.
 * <p>
 * Note that this code both generates the code <i>and</i> traces the specific objects that will be
 * passed when the code runs. Because this puzzle generates code that has no recursion and a
 * unique path to the target marker, every parameter and every local variable will only ever have
 * one value. We thus don't need any separation between declaration and execution for this puzzle.
 * <p>
 * The code structure here not so much object-oriented as it is functional: the instance variables
 * serve only to hold all the parameters related to a single method call that would otherwise have
 * to be passed repeatedly between all these functions. If those ivars all become parameters, these
 * could all be static methods. It might be possible to rethink that and break apart this behemoth
 * class, but...it works! -PPC
 */
class MethodCall {
    private final GeneratorContext genCtx;
    private final PuzzleContext ctx;  // convenience shortcut for genCtx.puzzleContext()

    // Accumulators for the code and stack variables associated with the method we're building.
    private final VariableContainer stackFrame;
    private final BlockStmt methodBody;

    // Output
    private final List<VariableContainer> targetStackTrace;

    MethodCall(
        GeneratorContext genCtx,
        StackPuzzleClass type,
        StackPuzzleObject receiver,  // null if static method
        String name,
        List<Value> args,
        int callDepth,
        boolean isOnTargetPath  // Are we on the path to the HERE marker? or on a side quest?
    ) {
        this.genCtx = genCtx;
        this.ctx = genCtx.puzzleContext();

        // Start with an empty method and empty stack frame

        stackFrame = new VariableContainer(name);

        var methodDecl = type.addMethod(name);
        if (receiver == null) {
            methodDecl.setStatic(true);
        }
        methodBody = new BlockStmt();
        methodDecl.setBody(methodBody);

        // Create all the variables that appear in the stack frame

        addParams(receiver, args, methodDecl);

        generateLocalVars(isOnTargetPath);

        // The puzzle asks the student to find the path to a specific line of code, and that path is
        // unique. We add some “complications” -- extraneous method calls not on the 
        //
        // Calling shouldComplicate() for both the pre- and post-call
        // complications

        boolean complicateBefore = shouldComplicate((callDepth + 1) * 2, isOnTargetPath);
        boolean complicateAfter = shouldComplicate((callDepth + 1) * 2 - 1, isOnTargetPath);

        if (complicateBefore) {
            addComplication(callDepth);
        }

        if (isOnTargetPath && genCtx.complexity().hasPropAssignmentsRemaining()) {
            addPropertyAssignment();
        }

        // Generate either the next call in the chain or the leaf node of this chain

        var stack = new ArrayList<VariableContainer>();
        stack.add(stackFrame);

        if (callDepth <= 0) {
            // We've reached the end of the call chain
            if (isOnTargetPath) {
                methodBody.addStatement(new ExpressionStmt(new NameExpr("___HERE___")));
            }
        } else {
            // Generate next method call in chain
            var calleeStack = generateMethodAndCall(callDepth, isOnTargetPath);
            stack.addAll(calleeStack);
        }

        // Add optional extraneous call after the real one

        if (complicateAfter) {
            addComplication(callDepth);
        }

        if (isOnTargetPath) {
            genCtx.complexity().countReferencesAsArrows(stackFrame.getVariables());
        }

        // Shockingly, this is the only result we need to return! The ultimate diagram only shows
        // stack frames on the path from the initial call to the HERE marker, and it can gather
        // all the objects from those frames as it goes. In other words, the stack diagrammer is
        // basically most of the way to being a garbage collector. /s

        this.targetStackTrace = stack;
    }

    List<VariableContainer> getTargetStackTrace() {
        return targetStackTrace;
    }

    /**
     * Add params (including implicit `this`) to both the method decl and the stack frame.
     */
    private void addParams(
        StackPuzzleObject receiver,
        List<Value> args,
        MethodDeclaration methodDecl
    ) {
        if (receiver != null) {
            stackFrame.addVariable(new Variable("this", new Value.Reference(receiver)));
        }
        for (var arg : args) {
            String paramName = Nonsense.variableName(ctx);
            methodDecl.addParameter(AstUtils.classNamed(arg.typeName()), paramName);
            stackFrame.addVariable(new Variable(paramName, arg));
        }
    }

    /**
     * Generate local vars, adding them to both the method body and the stack frame.
     */
    private void generateLocalVars(boolean isOnTargetPath) {
        int maxLocals = 2 + Math.min(4, ctx.getDifficulty()) - stackFrame.size();
        int localCount = ctx.getRandom().nextInt(0, Math.max(1, maxLocals));
        for(int n = 0; n < localCount; n++) {
            generateLocalVar(isOnTargetPath);
        }
    }

    private void generateLocalVar(boolean isOnTargetPath) {
        Runnable choice = Randomness.chooseWithProb(ctx,
            // mostly objects, a few ints...unless we've made a lot of objects already
            genCtx.complexity().hasObjectsRemaining() ? 0.7 : 0,
            () -> {
                var varName = Nonsense.variableName(ctx);
                var obj = generateObject(isOnTargetPath);
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

    private boolean shouldComplicate(int callsRemaining, boolean isOnTargetPath) {
        if (
            isOnTargetPath
            && ctx.getRandom().nextFloat() < (float) genCtx.complexity().getComplicationsRemaining() / callsRemaining
        ) {
            genCtx.complexity().countComplication();
            return true;
        }
        return false;
    }

    private void addComplication(int callDepth) {
        generateMethodAndCall(callDepth / 2, false);
        // Note that we ignore the returned stack trace: this isn't a call we're visualizing!
    }

    private void addPropertyAssignment() {
        // Rather than figuring out the logical combinations of local variables and reachable
        // properties, we just make a few random attempts to find a match and then give up.

        for(int attempt = 0; attempt < 3; attempt++) {
            var allowSelfConnection = (attempt >= 2);  // object points to self only as a last effort
            if (attemptPropertyAssignment(allowSelfConnection)) {
                genCtx.complexity().countPropAssignment();
                return;
            }
        }
    }

    private boolean attemptPropertyAssignment(boolean allowSelfConnection) {
        // Pick a random available variable whose value is an object (and not null)

        var varsWithObjectValue = stackFrame.getVariables().stream()
            .filter(v -> v.value() instanceof Value.Reference)
            .toList();
        if (varsWithObjectValue.isEmpty()) {
            return false;  // every local var is a primitive or is null
        }
        var lhsVar = Randomness.choose(ctx, varsWithObjectValue);
        var lhsObj = ((Value.Reference) lhsVar.value()).object();
        var lhsType = lhsObj.type();

        // Pick a random property from that type

        if (lhsType.properties().isEmpty()) {
            return false;  // we picked an object with no properties
        }
        var lhsProp = Randomness.choose(ctx, lhsType.properties());

        // Pick a random available value we can assign to that property

        var availableRhsVars = varsWithObjectValue.stream()
            .filter(v -> v.value().typeName().equals(lhsProp.type().name()))
            .filter(v -> allowSelfConnection || ((Value.Reference) v.value()).object() != lhsObj)
            .toList();
        if (availableRhsVars.isEmpty()) {
            return false;  // either no matching types, or only self matches and self-ref is prohibited
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

        // Keep track of how hard it's going to be for the student to diagram this. We'll regenerate
        // if it gets too hard.

        genCtx.complexity().countArrow();

        return true;
    }

    private List<VariableContainer> generateMethodAndCall(int callDepth, boolean isOnTargetPath) {
        // Choose receiver for next method call

        var nextReceiver = chooseMethodCallReceiver(isOnTargetPath);

        // Generate args for next method call

        var exprChoices = gatherExprChoices(isOnTargetPath);
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

        return new MethodCall(
            genCtx,
            nextReceiver.type(),
            nextReceiver.object(),
            nextMethodName,
            nextArgs.stream().map(ExprAndValue::value).toList(),
            callDepth - 1,
            isOnTargetPath
        ).getTargetStackTrace();
    }

    /**
     * Gathers available values we could pass to a method
     */
    private ChoiceDeck<Supplier<ExprAndValue>> gatherExprChoices(boolean isOnTargetPath) {
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
                var obj = generateObject(isOnTargetPath);
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

    private record ExprAndValue(
        Expression expression,
        Value value
    ) { }

    /**
     * Gathers available objects and classes that could be the receiver of a method call
     */
    private MethodCallReceiver chooseMethodCallReceiver(boolean isOnTargetPath) {
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
                    var obj = generateObject(isOnTargetPath);
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

    private record MethodCallReceiver(
        Expression expression,
        StackPuzzleClass type,
        StackPuzzleObject object  // null if static method
    ) { }

    /**
     * Instantiates a new object. Used throughout the code above.
     */
    private StackPuzzleObject generateObject(boolean isOnTargetPath) {
        if (isOnTargetPath) {  // doesn't count if it won't be in the diagram
            genCtx.complexity().countObject();
        }
        return new StackPuzzleObject(
            Randomness.choose(ctx, genCtx.puzzleClasses()),
            ctx.getRandom().nextInt(1000)
        );
    }
}
