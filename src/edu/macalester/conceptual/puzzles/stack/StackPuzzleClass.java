package edu.macalester.conceptual.puzzles.stack;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.VoidType;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.AstUtils;
import edu.macalester.conceptual.util.Nonsense;
import edu.macalester.conceptual.util.Randomness;

import static com.github.javaparser.ast.Modifier.createModifierList;

record StackPuzzleClass(
    String name,
    String idProperty,
    List<Property> properties,
    List<MethodDeclaration> methods
) {
    static StackPuzzleClass generate(PuzzleContext ctx) {
        return new StackPuzzleClass(
            Nonsense.shortTypeName(ctx),
            Nonsense.propertyName(ctx),
            new ArrayList<>(),  // no properties yet
            new ArrayList<>()   // no methods yet
        );
    }

    MethodDeclaration addMethod(String name) {
        var method = new MethodDeclaration(
            createModifierList(Modifier.Keyword.PUBLIC),
            new VoidType(),
            name
        );
        methods.add(method);
        return method;
    }

    Property addProperty(String name, StackPuzzleClass type) {
        var prop = new Property(name, type);
        properties().add(prop);
        return prop;
    }

    ClassOrInterfaceDeclaration buildDeclaration(PuzzleContext ctx) {
        var decl = AstUtils.publicClassDecl(name());
        decl.addPrivateField(PrimitiveType.intType(), idProperty());
        for (var prop : properties) {
            decl.addPrivateField(prop.type().name(), prop.name());
        }

        var ctor = decl.addConstructor();
        ctor.addParameter(PrimitiveType.intType(), idProperty());
        ctor.getBody().addStatement(
            MessageFormat.format("this.{0} = {0};", idProperty()));

        for (var prop : properties) {
            AstUtils.addSetter(decl, prop.type().name(), prop.name());
        }

        for (var method : Randomness.shuffled(ctx, methods)) {
            decl.getMembers().add(method);
        }

        return decl;
    }

    record Property(String name, StackPuzzleClass type) {
        String setterName() {
            return AstUtils.setterName(name());
        }
    }

    @Override
    public String toString() {
        return name();
    }
}
