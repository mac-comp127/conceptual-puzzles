package edu.macalester.conceptual.puzzles.constructorchains;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.AstUtils;
import edu.macalester.conceptual.util.CodeFormatting;
import edu.macalester.conceptual.util.Evaluator;
import edu.macalester.conceptual.util.VariablePool;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
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

        ctx.output().paragraph("With the following class declarations:");

        Random rand = ctx.getRandom();
        int depth = rand.nextInt(0, 5);


        // cat file-with-class-decl | bin/astprinter class
        // use the output to inform the code below that builds your AST;
        // the AST is built bottom-up, and the text output uses indentation for the tree,
        // so "bottom-up" here means "rightmost-indented left" there, so to speak

        String className = RandomLoch.getTypeName(ctx);
        var decl = AstUtils.publicClassDecl(className);
        decl.addConstructor(Modifier.Keyword.PUBLIC);
        maybeAddPrintLn(decl, className, rand);

        ctx.output().codeBlock(CodeFormatting.prettify(decl));

        List<ClassOrInterfaceDeclaration> classes = new ArrayList<ClassOrInterfaceDeclaration>();

        classes.add(decl);

        for (int i = 0; i < depth; i++) {

            // higher difficulties: randomly create an instance of some class earlier up the hierarchy in
            // constructor here
            var nameExpr = new NameExpr("Racadal");
            // var objcreation = new ObjectCreationExpr(classes.get(0).getClass().getTty);

            String parentClass = decl.getNameAsString();
            className = RandomWeatherPlace.getTypeName(ctx);
            decl = AstUtils.publicClassDecl(className);
            decl.addExtendedType(parentClass);
            decl.addConstructor(Modifier.Keyword.PUBLIC);
            maybeAddPrintLn(decl, className, rand);
            ctx.output().codeBlock(CodeFormatting.prettify(decl));
            classes.add(decl);
        }

        ctx.output().paragraph("What gets printed when you create an instance of " + className + "?");
    }

    private void maybeAddPrintLn(ClassOrInterfaceDeclaration decl, String className, Random rand) {
        if (rand.nextBoolean()) {
            var msg = new StringLiteralExpr(className + " default constructor");
            var sout = new MethodCallExpr("System.out.println", msg);
            var printblock = new BlockStmt().addStatement(sout);
            decl.getDefaultConstructor().get().setBody(printblock);
        }
    }
}
