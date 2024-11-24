package edu.macalester.conceptual.puzzles.relationships;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ReferenceType;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.AstUtils;
import edu.macalester.conceptual.util.Nonsense;

public class Type {
    private final String name;
    private List<Relationship> relationships = new ArrayList<>();

    public Type(String name) {
        this.name = name;
    }

    public Type(PuzzleContext ctx) {
        this(Nonsense.typeName(ctx));
    }

    public String getName() {
        return name;
    }

    public void add(Relationship rel) {
        relationships.add(rel);
    }

    public void shuffleMembers(PuzzleContext ctx) {
        Collections.shuffle(relationships, ctx.getRandom());
    }

    public ReferenceType referenceAst() {
        return AstUtils.classNamed(getName());
    }

    public ClassOrInterfaceDeclaration declarationAst() {
        var decl = new ClassOrInterfaceDeclaration(AstUtils.nodes(Modifier.publicModifier()), false, getName());
        for (var rel : relationships) {
            rel.buildDeclaration(decl);
        }
        return decl;
    }
}
