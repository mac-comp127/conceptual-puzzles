package edu.macalester.conceptual.puzzles.idea;

final class IdeaObject {
    private final IdeaClass type;
    private final int id;
    private final VariableContainer variableContainer;

    IdeaObject(IdeaClass type, int id) {
        this.type = type;
        this.id = id;
        variableContainer = new VariableContainer(type.name());
        variableContainer.addVariable(new Variable(
            type.idProperty(),
            new Value.InlineValue("int", String.valueOf(id))));
    }

    public VariableContainer variableContainer() {
        return variableContainer;
    }

    public IdeaClass type() {
        return type;
    }

    public int id() {
        return id;
    }
}
