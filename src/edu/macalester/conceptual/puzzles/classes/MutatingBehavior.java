package edu.macalester.conceptual.puzzles.classes;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.stmt.ExpressionStmt;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.Nonsense;

import static edu.macalester.conceptual.util.AstUtils.blockOf;

/**
 * A method that changes internal state.
 */
class MutatingBehavior implements ClassFeature {
    private final String name;
    private final ExprWithDescription mutation;

    static ClassFeature generate(PuzzleContext ctx, StateVariable stateVariable) {
        return new MutatingBehavior(
            Nonsense.verbyMethodName(ctx),
            stateVariable.type().generateMutation(ctx, stateVariable.name()));
    }

    private MutatingBehavior(String name, ExprWithDescription mutation) {
        this.name = name;
        this.mutation = mutation;
    }

    @Override
    public String describeInWords(String className) {
        return MessageFormat.format(
            """
            A/An {0} can {1}.
            This behavior {2}.
            Anyone can ask a/an {0} to {1}.
            """,
            "`" + className + "`",
            "`" + name + "`",
            mutation.description());
    }

    @Override
    public void addToClassDeclaration(ClassOrInterfaceDeclaration classDecl) {
        var method = classDecl.addMethod(name, Modifier.Keyword.PUBLIC);
        method.setBody(blockOf(
            new ExpressionStmt(mutation.code())
        ));
    }

    @Override
    public void addToConstructor(ConstructorDeclaration ctor) {
        // nothing to do
    }

    @Override
    public Collection<StateVariable> getStateVariables() {
        return List.of();
    }
}
