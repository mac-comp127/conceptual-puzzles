package edu.macalester.conceptual.puzzles.classes.feature;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;

import edu.macalester.conceptual.puzzles.classes.ExprWithDescription;
import edu.macalester.conceptual.puzzles.classes.type.PropertyType;

class InternalState implements ClassFeature {
    private final String name;
    private final PropertyType type;
    private final ExprWithDescription initialValue;

    InternalState(String name, PropertyType type, ExprWithDescription initialValue) {
        this.name = name;
        this.type = type;
        this.initialValue = initialValue;
    }

    @Override
    public String describeInWords(String className) {
        return MessageFormat.format(
            """
            Each {0} has a {1}, which is a/an {2}.
            A/An {1} is part of the internal state of a/an {0}:
            no other classes can see the value of {1} or directly change it.
            When a/an {0} is first created, the value of its {1} starts out as {3}.
            """,
            "`" + className + "`",
            "`" + name + "`",
            type.description(),
            initialValue.description());
    }

    @Override
    public void addToCode(ClassOrInterfaceDeclaration classDecl, ConstructorDeclaration ctor) {
        // private String foo = "bar";

        classDecl.addFieldWithInitializer(
            type.astType(),
            name,
            initialValue.code(),
            Modifier.Keyword.PRIVATE);
    }

    @Override
    public Collection<StateVariable> getStateVariables() {
        final boolean mutable = true;
        return List.of(
            new StateVariable(type, name, mutable));
    }
}
