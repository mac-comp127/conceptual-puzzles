package edu.macalester.conceptual.puzzles.idea;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import edu.macalester.conceptual.util.AstUtils;

record IdeaClass(ClassOrInterfaceDeclaration decl) {
    static IdeaClass named(String name) {
        return new IdeaClass(AstUtils.publicClassDecl(name));
    }

    String name() {
        return decl.getNameAsString();
    }
}
