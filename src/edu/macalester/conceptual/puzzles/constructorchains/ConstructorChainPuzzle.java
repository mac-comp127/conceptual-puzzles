package edu.macalester.conceptual.puzzles.constructorchains;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.AstUtils;
import edu.macalester.conceptual.util.CodeFormatting;
import edu.macalester.conceptual.util.Evaluator;
import edu.macalester.conceptual.util.VariablePool;

import java.util.Comparator;
import java.util.stream.Collectors;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.expr.StringLiteralExpr;

public class ConstructorChainPuzzle implements Puzzle {
    @Override
    public byte id() {
        return 7;
    }

    @Override
    public String name() {
        return "ctors";
    }

    @Override
    public String description() {
        return "Tracing execution of constructor calls";
    }

    @Override
    public void generate(PuzzleContext ctx) {
        ctx.output().paragraph("What gets printed when you create an instance of [someclass]?");


        // cat file-with-class-decl | bin/astprinter class
        // use the output to inform the code below that builds your AST;
        // the AST is built bottom-up, and the text output uses indentation for the tree,
        // so "bottom-up" here means "rightmost-indented left" there, so to speak

        var msg = new StringLiteralExpr("Organism default constructor");
        var printit = new MethodCallExpr("printit", msg);
        var printblock = new BlockStmt().addStatement(printit);


        var decl = AstUtils.publicClassDecl("Organism");
        decl.addConstructor(Modifier.Keyword.PUBLIC);

        var ctor = decl.getDefaultConstructor().get().setBody(printblock);


        ctx.output().codeBlock(
            CodeFormatting.prettify(decl));

        var decl2 = AstUtils.publicClassDecl("Animal");
        String organismClass = decl.getNameAsString();
        decl2.addExtendedType(organismClass);

        ctx.output().codeBlock(
            CodeFormatting.prettify(decl2));
    }
}
