package edu.macalester.conceptual.puzzles.stack;

/**
 * The value of a variable. Here we don't distinguish primitive vs object types; rather, we
 * distinguish things that need an arrow (Reference) vs. things that we'll diagram as text next to
 * the variable name (Inline).
 */
interface Value {
    String typeName();

    record Inline(
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

    static Inline makeIntValue(String n) {
        return new Inline("int", n);
    }

    static Value makeNullValue(StackPuzzleClass type) {
        return new Inline(type.name(), "null");
    }
}
