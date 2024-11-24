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

/**
 * Just the info about a class/interface we need for the relationships puzzle.
 */
public class Type {
    private final String name;
    private final List<Relationship> relationships = new ArrayList<>();

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

    public void shuffleRelationships(PuzzleContext ctx) {
        Collections.shuffle(relationships, ctx.getRandom());
    }

    public ReferenceType buildReferenceAst() {
        return AstUtils.classNamed(getName());
    }

    public ClassOrInterfaceDeclaration buildDeclarationAst() {
        var decl = new ClassOrInterfaceDeclaration(
            AstUtils.nodes(Modifier.publicModifier()),
            false,  // All classes, no interfaces (at least for now)
            getName());
        for (var rel : relationships) {
            rel.buildDeclaration(decl);
        }
        return decl;
    }
}
