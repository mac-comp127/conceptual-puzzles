package edu.macalester.conceptual.puzzles.stack;

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;

import static edu.macalester.conceptual.util.AstUtils.classNamed;
import static edu.macalester.conceptual.util.AstUtils.nodes;

/**
 * One object instance in the puzzle.
 */
final class StackPuzzleObject {
    private final StackPuzzleClass type;
    private final int id;
    private final Map<String, Value.Reference> propertyValues = new HashMap<>();

    private final VariableContainer variableContainer;

    StackPuzzleObject(StackPuzzleClass type, int id) {
        this.type = type;
        this.id = id;

        // We regenerate the variableContainer on demand, but the way the diagrammer relies on
        // object identity means that we always have to return the same (newly updated) object.
        variableContainer = new VariableContainer(type.name());
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

    /**
     * Returns the AST for an expression that would create this object.
     */
    Expression instantiationExpr() {
        return new ObjectCreationExpr(
            null,
            classNamed(type().name()),
            nodes(new IntegerLiteralExpr(String.valueOf(
                id()
            )))
        );
    }
}
