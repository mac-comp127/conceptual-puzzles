package edu.macalester.conceptual.puzzles.stack;

import java.util.HashMap;
import java.util.Map;

final class StackPuzzleObject {
    private final StackPuzzleClass type;
    private final int id;
    private final VariableContainer variableContainer;
    private final Map<String, Value.Reference> propertyValues = new HashMap<>();

    StackPuzzleObject(StackPuzzleClass type, int id) {
        this.type = type;
        this.id = id;
        variableContainer = new VariableContainer(type.name());
    }

    VariableContainer variableContainer() {
        variableContainer.clear();
        variableContainer.addVariable(new Variable(
            type.idProperty(),
            Value.makeIntValue(String.valueOf(id))
        ));
        for (var prop : type.properties()) {
            var value = propertyValues.get(prop.name());
            variableContainer.addVariable(new Variable(
                prop.name(),
                (value != null)
                    ? value
                    : Value.makeNullValue(prop.type())
            ));
        }
        return variableContainer;
    }

    StackPuzzleClass type() {
        return type;
    }

    int id() {
        return id;
    }

    void setProperty(StackPuzzleClass.Property prop, Value.Reference value) {
        propertyValues.put(prop.name(), value);
    }
}
