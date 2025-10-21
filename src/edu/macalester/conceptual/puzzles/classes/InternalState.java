package edu.macalester.conceptual.puzzles.classes;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;

import edu.macalester.conceptual.context.PuzzleContext;

import static edu.macalester.conceptual.util.Nonsense.propertyName;
import static edu.macalester.conceptual.util.Randomness.chooseConst;

class InternalState implements ClassFeature {
    private final String name;
    private final PropertyType type;
    private final ExprWithDescription initialValue;

    static InternalState generate(PuzzleContext ctx) {
        var type = chooseConst(ctx, PropertyType.values());
        ExprWithDescription initialValue = type.generateValue(ctx, true);
        return new InternalState(propertyName(ctx), type, initialValue);
    }

    InternalState(String name, PropertyType type, ExprWithDescription initialValue) {
        this.name = name;
        this.type = type;
        this.initialValue = initialValue;
    }

    @Override
    public String describeInWords(String className) {
        return MessageFormat.format(
            "Each {0} has a {1}, which is a/an {2}. "
                + "A/An {1} is part of the internal state of a/an {0}: "
                + "no other classes can see the value of {1} or directly change it. "
                + "When a/an {0} is first created, the value of its {1} starts out as {3}.",
            "`" + className + "`",
            "`" + name + "`",
            type.description(),
            initialValue.description());
    }

    @Override
    public void addToClassDeclaration(ClassOrInterfaceDeclaration classDecl) {
        classDecl.addFieldWithInitializer(
            type.astType(),
            name,
            initialValue.code(),
            Modifier.Keyword.PRIVATE);
    }

    @Override
    public void addToConstructor(ConstructorDeclaration ctor) {
        // nothing to do
    }

    @Override
    public Collection<StateVariable> getStateVariables() {
        final boolean mutable = true;
        return List.of(
            new StateVariable(type, name, mutable));
    }
}
