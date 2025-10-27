package edu.macalester.conceptual.puzzles.classes.feature;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.stmt.ExpressionStmt;

import edu.macalester.conceptual.puzzles.classes.ExprWithDescription;
import edu.macalester.conceptual.puzzles.classes.type.PropertyType;

/**
 * A private static variable.
 */
class StaticVariable implements ClassFeature {
    private final String name;
    private final PropertyType type;
    private final ExprWithDescription initialValue;
    private final ExprWithDescription mutationInConstructor;

    StaticVariable(String name, PropertyType type, ExprWithDescription initialValue, ExprWithDescription mutationInConstructor) {
        this.name = name;
        this.type = type;
        this.initialValue = initialValue;
        this.mutationInConstructor = mutationInConstructor;
    }

    @Override
    public String describeInWords(String className) {
        return MessageFormat.format(
            """
            All {0}s share a single {1}, which is a/an {2}.
            No other classes can directly ask for the value of {1}.
            The value of {1} starts out as {3} when the program starts.
            Every time a new {0} is created, it {4}.
            """,
            "`" + className + "`",
            "`" + name + "`",
            type.description(),
            initialValue.description(),
            mutationInConstructor.description());
    }

    @Override
    public void addToCode(ClassOrInterfaceDeclaration classDecl, ConstructorDeclaration ctor) {
        classDecl.addFieldWithInitializer(
            type.astType(),
            name,
            initialValue.code(),
            Modifier.Keyword.PRIVATE,
            Modifier.Keyword.STATIC);

        ctor.getBody().addStatement(
            new ExpressionStmt(
                mutationInConstructor.code()));
    }

    @Override
    public Collection<StateVariable> getStateVariables() {
        final boolean mutable = true;
        return List.of(
            new StateVariable(type, name, mutable));
    }
}
