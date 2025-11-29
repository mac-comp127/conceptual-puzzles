package edu.macalester.conceptual.puzzles.stack;

interface Value {
    String typeName();

    record InlineValue(
        String typeName,
        String value
    ) implements Value {
        @Override
        public String typeName() {
            return typeName;
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

    static Value makeNullValue(StackPuzzleClass type) {
        return new InlineValue(type.name(), "null");
    }
}
