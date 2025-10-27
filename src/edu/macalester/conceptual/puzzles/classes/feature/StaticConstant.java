package edu.macalester.conceptual.puzzles.classes.feature;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;

import edu.macalester.conceptual.puzzles.classes.ExprWithDescription;
import edu.macalester.conceptual.puzzles.classes.type.PropertyType;

/**
 * A public static final CONSTANT_VALUE.
 */
class StaticConstant implements ClassFeature {
    private final String name;
    private final PropertyType type;
    private final ExprWithDescription value;
    private final boolean isPrivate;

    StaticConstant(String name, PropertyType type, ExprWithDescription value, boolean isPrivate) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.isPrivate = isPrivate;
    }

    @Override
    public String describeInWords(String className) {
        return MessageFormat.format(
            """
            All {0}s share a single {1}, which is a/an {2}.
            It is a constant.
            Its value is {3}.
            Other classes *{4}* see its value.
            """,
            "`" + className + "`",
            "`" + name + "`",
            type.description(),
            value.description(),
            isPrivate ? "cannot" : "can");
    }

    @Override
    public void addToCode(ClassOrInterfaceDeclaration classDecl, ConstructorDeclaration ctor) {
        classDecl.addFieldWithInitializer(
            type.astType(),
            name,
            value.code(),
            isPrivate ? Modifier.Keyword.PRIVATE : Modifier.Keyword.PUBLIC,
            Modifier.Keyword.STATIC,
            Modifier.Keyword.FINAL);
    }

    @Override
    public Collection<ClassFeature.StateVariable> getStateVariables() {
        final boolean mutable = false;
        return List.of(
            new StateVariable(type, name, mutable));
    }
}
