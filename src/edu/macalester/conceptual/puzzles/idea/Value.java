package edu.macalester.conceptual.puzzles.idea;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

interface Value {
    String typeName();

    record InlineValue(
        String primitiveTypeName,
        String value
    ) implements Value {
        @Override
        public String typeName() {
            return primitiveTypeName;
        }
    }

    record Reference(
        IdeaObject object
    ) implements Value {
        @Override
        public String typeName() {
            return object.type().name();
        }
    }
}
