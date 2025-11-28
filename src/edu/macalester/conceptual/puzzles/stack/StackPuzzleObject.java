package edu.macalester.conceptual.puzzles.stack;

final class StackPuzzleObject {
    private final StackPuzzleClass type;
    private final int id;
    private final VariableContainer variableContainer;

    StackPuzzleObject(StackPuzzleClass type, int id) {
        this.type = type;
        this.id = id;
        variableContainer = new VariableContainer(type.name());
        variableContainer.addVariable(new Variable(
            type.idProperty(),
            Value.makeIntValue(String.valueOf(id))));
    }

    public VariableContainer variableContainer() {
        return variableContainer;
    }

    public StackPuzzleClass type() {
        return type;
    }

    public int id() {
        return id;
    }
}
