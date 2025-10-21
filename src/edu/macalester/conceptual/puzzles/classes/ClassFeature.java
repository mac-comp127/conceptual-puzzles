package edu.macalester.conceptual.puzzles.classes;

import java.util.Collection;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;

interface ClassFeature {
    String describeInWords(String className);

    void addToClassDeclaration(ClassOrInterfaceDeclaration classDecl);
    void addToConstructor(ConstructorDeclaration ctor);

    Collection<StateVariable> getStateVariables();
    record StateVariable(PropertyType type, String name, boolean isMutable) {}
}
