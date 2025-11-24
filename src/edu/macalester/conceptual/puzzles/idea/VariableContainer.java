package edu.macalester.conceptual.puzzles.idea;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Either an object or a stack frame, something that holds variables.
 */
class VariableContainer {
    private final String title;
    private final List<Variable> variables = new ArrayList<>();

    VariableContainer(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public List<Variable> getVariables() {
        return Collections.unmodifiableList(variables);
    }

    public void addVariable(Variable v) {
        variables.add(v);
    }

    public int size() {
        return variables.size();
    }
}
