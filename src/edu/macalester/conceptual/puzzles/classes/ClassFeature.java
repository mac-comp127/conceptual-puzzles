package edu.macalester.conceptual.puzzles.classes;

import java.util.Collection;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;

/**
 * One semantic unit of an object model that can be attached to a class.
 * Examples: a property, a behavior, a constant.
 */
interface ClassFeature {
    String describeInWords(String className);

    void addToClassDeclaration(ClassOrInterfaceDeclaration classDecl);
    void addToConstructor(ConstructorDeclaration ctor);

    /**
     * Returns the instance variables, if any, declared by this class feature.
     */
    Collection<StateVariable> getStateVariables();
    record StateVariable(PropertyType type, String name, boolean isMutable) {}
}
