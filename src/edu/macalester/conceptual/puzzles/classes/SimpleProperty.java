package edu.macalester.conceptual.puzzles.classes;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.AstUtils;
import edu.macalester.conceptual.util.Randomness;

import static com.github.javaparser.StaticJavaParser.parseExpression;
import static edu.macalester.conceptual.util.AstUtils.addGetter;
import static edu.macalester.conceptual.util.AstUtils.addSetter;
import static edu.macalester.conceptual.util.Nonsense.propertyName;
import static edu.macalester.conceptual.util.Randomness.chooseConst;

/**
 * Final ivar, getter, constructor param
 */
class SimpleProperty implements ClassFeature {
    private final String name;
    private final PropertyType type;
    private final boolean mutable;
    private final ExprWithDescription initialValue;

    static SimpleProperty generateImmutable(PuzzleContext ctx) {
        final boolean mutable = false;
        return new SimpleProperty(
            propertyName(ctx),
            chooseConst(ctx, PropertyType.values()),
            mutable,
            null);  // initial value for immutable always passed to constructor
    }

    static SimpleProperty generateMutable(PuzzleContext ctx) {
        var type = chooseConst(ctx, PropertyType.values());
        final boolean mutable = true;
        ExprWithDescription initialValue = Randomness.chooseWithProb(ctx, 0.3,
            null, // specified in constructor
            type.generateValue(ctx, mutable));
        return new SimpleProperty(propertyName(ctx), type, mutable, initialValue);
    }

    private SimpleProperty(String name, PropertyType type, boolean mutable, ExprWithDescription initialValue) {
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
    public void addToClassDeclaration(ClassOrInterfaceDeclaration classDecl) {
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

        addGetter(classDecl, type.javaName(), name, parseExpression(name));

        if (mutable) {
            addSetter(classDecl, type.javaName(), name);
        }
    }

    @Override
    public void addToConstructor(ConstructorDeclaration ctor) {
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
