package edu.macalester.conceptual.puzzles.classes.feature;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;

import edu.macalester.conceptual.puzzles.classes.ExprWithDescription;
import edu.macalester.conceptual.puzzles.classes.type.PropertyType;
import edu.macalester.conceptual.util.AstUtils;

import static com.github.javaparser.StaticJavaParser.parseExpression;
import static edu.macalester.conceptual.util.AstUtils.addGetter;
import static edu.macalester.conceptual.util.AstUtils.addSetter;

/**
 * An instance variable + getter + optional setter. If there is no setter, the ivar is final.
 * Immutable properties always take a value passed to the constructur. Mutable properties may also
 * take a hard-coded initial value instead.
 */
class SimpleProperty implements ClassFeature {
    private final String name;
    private final PropertyType type;
    private final boolean mutable;
    private final ExprWithDescription initialValue;

    SimpleProperty(String name, PropertyType type, boolean mutable, ExprWithDescription initialValue) {
        this.name = name;
        this.type = type;
        this.mutable = mutable;
        this.initialValue = initialValue;
    }

    @Override
    public String describeInWords(String className) {
        return MessageFormat.format(
            "Each {0} has its own {1}, which is a/an {2}. "
                + (initialValue == null
                    ? "The value of {1} is specified when a {0} is created. "
                    : "The value of {1} starts out as {3}. ")
                + "Anyone can ask a/an {0} for the value of its {1}. "
                + (mutable
                    ? "Anyone can set {1} to a new value. "
                    : "The value of {1} for a specific {0} can never change. "),
            "`" + className + "`",
            "`" + name + "`",
            type.description(),
            initialValue == null ? null : initialValue.description());
    }

    @Override
    public void addToCode(ClassOrInterfaceDeclaration classDecl, ConstructorDeclaration ctor) {
        // Instrance variable

        FieldDeclaration field;
        if (initialValue == null) {
            field = classDecl.addPrivateField(type.astType(), name);
        } else {
            field = classDecl.addFieldWithInitializer(
                type.astType(),
                name,
                initialValue.code(),
                Modifier.Keyword.PRIVATE);
        }
        if (!mutable) {
            field.addModifier(Modifier.Keyword.FINAL);
        }

        // Getter and (maybe) setter

        addGetter(classDecl, type.javaName(), name, parseExpression(name));

        if (mutable) {
            addSetter(classDecl, type.javaName(), name);
        }

        // Constructor

        if(initialValue == null) {
            ctor.addParameter(type.astType(), name);
            ctor.getBody().addStatement(
                AstUtils.buildSetterStatement(name));
        }
    }

    @Override
    public Collection<StateVariable> getStateVariables() {
        return List.of(
            new StateVariable(type, name, mutable));
    }
}
