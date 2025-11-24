package edu.macalester.conceptual.puzzles.classes.feature;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;

import edu.macalester.conceptual.puzzles.classes.TypedExprWithDescription;
import edu.macalester.conceptual.util.AstUtils;

class ComputedProperty implements ClassFeature {
    private final String name;
    private final TypedExprWithDescription derivedValue;

    ComputedProperty(String name, TypedExprWithDescription derivedValue) {
        this.name = name;
        this.derivedValue = derivedValue;
    }

    @Override
    public String describeInWords(String className) {
        return MessageFormat.format(
            """
            Each {0} has a {1}, which is a/an {2}.
            The value of {1} is not part of a/an {0}’s internal state;
            instead, it is computed on demand.
            The computed value of {1} is {3}.
            """,
            "`" + className + "`",
            "`" + name + "`",
            derivedValue.type().description(),
            derivedValue.description());
    }

    @Override
    public void addToCode(ClassOrInterfaceDeclaration classDecl, ConstructorDeclaration ctor) {
        // public String getFoo() {
        //     return << value derived from bar >>;
        // }

        AstUtils.addGetter(
            classDecl,
            derivedValue.type().javaName(),
            name,
            derivedValue.code());
    }

    @Override
    public Collection<StateVariable> getStateVariables() {
        return List.of();
    }
}
