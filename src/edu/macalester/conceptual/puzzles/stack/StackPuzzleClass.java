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
    List<MethodDeclaration> methods
) {
    static StackPuzzleClass generate(PuzzleContext ctx) {
        return new StackPuzzleClass(
            Nonsense.shortTypeName(ctx),
            Nonsense.propertyName(ctx),
            new ArrayList<>()
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

    ClassOrInterfaceDeclaration buildDeclaration(PuzzleContext ctx) {
        var decl = AstUtils.publicClassDecl(name());
        decl.addPrivateField(PrimitiveType.intType(), idProperty());

        var ctor = decl.addConstructor();
        ctor.addParameter(PrimitiveType.intType(), idProperty());
        ctor.getBody().addStatement(
            MessageFormat.format("this.{0} = {0};", idProperty()));

        for (var method : Randomness.shuffled(ctx, methods)) {
            decl.getMembers().add(method);
        }

        return decl;
    }
}
