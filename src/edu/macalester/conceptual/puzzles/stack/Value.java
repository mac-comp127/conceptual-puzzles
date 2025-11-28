package edu.macalester.conceptual.puzzles.stack;

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
        StackPuzzleObject object
    ) implements Value {
        @Override
        public String typeName() {
            return object.type().name();
        }
    }

    static InlineValue makeIntValue(String n) {
        return new InlineValue("int", n);
    }
}
