package edu.macalester.conceptual.puzzles.classes.feature;

import java.util.Collection;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.puzzles.classes.ExprWithDescription;
import edu.macalester.conceptual.puzzles.classes.type.PropertyType;
import edu.macalester.conceptual.util.Nonsense;
import edu.macalester.conceptual.util.Randomness;

import static edu.macalester.conceptual.util.Nonsense.constantName;
import static edu.macalester.conceptual.util.Nonsense.propertyName;
import static edu.macalester.conceptual.util.Randomness.choose;

/**
 * One semantic unit of an object model that can be attached to a class.
 * Examples: a property, a behavior, a constant.
 */
public interface ClassFeature {
    /**
     * Provides an English specification for this feature, assuming that it belongs to a class with
     * the given name.
     */
    String describeInWords(String className);

    /**
     * Adds this feature to the given class declaration, of which the given constructor must be a
     * member. This method may both add members to the class and expand the constructor.
     */
    void addToCode(ClassOrInterfaceDeclaration classDecl, ConstructorDeclaration ctor);

    /**
     * Returns the instance variables, if any, declared by this class feature.
     */
    Collection<StateVariable> getStateVariables();
    record StateVariable(PropertyType type, String name, boolean isMutable) {}

    static ClassFeature generateImmutableProperty(PuzzleContext ctx) {
        final boolean mutable = false;
        return new SimpleProperty(
            propertyName(ctx),
            choose(ctx, PropertyType.ALL),
            mutable,
            null);  // initial value for immutable always passed to constructor
    }

    static ClassFeature generateMutableProperty(PuzzleContext ctx) {
        var type = choose(ctx, PropertyType.ALL);
        final boolean mutable = true;
        ExprWithDescription initialValue = Randomness.chooseWithProb(ctx, 0.3,
            null, // specified in constructor
            type.generateValue(ctx, mutable));
        return new SimpleProperty(propertyName(ctx), type, mutable, initialValue);
    }

    static ClassFeature generateStaticVariable(PuzzleContext ctx) {
        var name = propertyName(ctx);
        var type = choose(ctx, PropertyType.ALL);
        var initialValue = type.generateValue(ctx, true);
        var mutationInConstructor = type.generateMutation(ctx, name);
        return new StaticVariable(name, type, initialValue, mutationInConstructor);
    }

    static ClassFeature generateStaticConstant(PuzzleContext ctx) {
        var type = choose(ctx, PropertyType.ALL);
        ExprWithDescription value = type.generateValue(ctx, false);
        boolean isPrivate = ctx.getRandom().nextBoolean();
        return new StaticConstant(constantName(ctx), type, value, isPrivate);
    }

    static ClassFeature generateInternalState(PuzzleContext ctx) {
        var type = choose(ctx, PropertyType.ALL);
        ExprWithDescription initialValue = type.generateValue(ctx, true);
        return new InternalState(propertyName(ctx), type, initialValue);
    }

    static ClassFeature generateComputedProperty(PuzzleContext ctx, StateVariable stateVariable) {
        return new ComputedProperty(
            Nonsense.propertyName(ctx),
            stateVariable.type().generateDerivedValue(ctx, stateVariable.name()));
    }

    static ClassFeature generateMutatingBehavior(PuzzleContext ctx, StateVariable stateVariable) {
        return new MutatingBehavior(
            Nonsense.verbyMethodName(ctx),
            stateVariable.type().generateMutation(ctx, stateVariable.name()));
    }
}
