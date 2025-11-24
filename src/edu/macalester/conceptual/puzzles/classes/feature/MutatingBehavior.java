package edu.macalester.conceptual.puzzles.classes.feature;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.stmt.ExpressionStmt;

import edu.macalester.conceptual.puzzles.classes.ExprWithDescription;

import static edu.macalester.conceptual.util.AstUtils.blockOf;

/**
 * A method that changes internal state.
 */
class MutatingBehavior implements ClassFeature {
    private final String name;
    private final ExprWithDescription mutation;

    MutatingBehavior(String name, ExprWithDescription mutation) {
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
    public void addToCode(ClassOrInterfaceDeclaration classDecl, ConstructorDeclaration ctor) {
        // public void fooify() {
        //     << mutate bar >>
        // }
        var method = classDecl.addMethod(name, Modifier.Keyword.PUBLIC);
        method.setBody(blockOf(
            new ExpressionStmt(mutation.code())
        ));
    }

    @Override
    public Collection<StateVariable> getStateVariables() {
        return List.of();
    }
}
