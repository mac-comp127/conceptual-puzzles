package edu.macalester.conceptual.puzzles.constructorchains;

import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.AstUtils;
import edu.macalester.conceptual.util.ChoiceDeck;
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
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.symbolsolver.javaparsermodel.contexts.VariableDeclarationExprContext;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;


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
            String parentClass = decl.getNameAsString();
            className = RandomWeatherPlace.getTypeName(ctx);
            decl = AstUtils.publicClassDecl(className);
            decl.addExtendedType(parentClass);
            decl.addConstructor(Modifier.Keyword.PUBLIC);

            // TODO: for extra difficulty, shuffle the order of these
            maybeAddObjCreation(decl, classes, ctx);
            maybeAddPrintLn(decl, className, rand);

            ctx.output().codeBlock(CodeFormatting.prettify(decl));
            classes.add(decl);
        }

        ctx.output().paragraph("What gets printed when you create an instance of " + className + "?");
    }

    // Create a statement like "Bar xyz = new Thing();"
    private ExpressionStmt getObjectCreationStmt(String staticTypeName, String variableName, String runtimeTypeName) {

        VariableDeclarator varDecl = new VariableDeclarator(
            new ClassOrInterfaceType(null, staticTypeName),
            variableName,
            new ObjectCreationExpr(null, new ClassOrInterfaceType(null, runtimeTypeName),
                new com.github.javaparser.ast.NodeList<>()));
        VariableDeclarationExpr varDeclExpr = new VariableDeclarationExpr(varDecl);
        return new ExpressionStmt(varDeclExpr);
    }

    private void maybeAddPrintLn(ClassOrInterfaceDeclaration decl, String className, Random rand) {
        if (rand.nextBoolean()) {
            var msg = new StringLiteralExpr(className + " default constructor");
            var sout = new MethodCallExpr("System.out.println", msg);
            decl.getDefaultConstructor().get().getBody().addStatement(sout);
        }
    }

    private void maybeAddObjCreation(
        ClassOrInterfaceDeclaration decl,
        List<ClassOrInterfaceDeclaration> classes,
        PuzzleContext ctx) {

        Random rand = ctx.getRandom();
        if (true /* rand.nextBoolean() */) {

            var superclassesDeck = new ChoiceDeck<>(ctx, classes);
            // TODO: for extra difficulty, allow static and runtime types to differ, so that the runtime type is
            // a superclass of the superclass!
            var superClassName = superclassesDeck.draw().getName().toString();
            var objCreationStmt = getObjectCreationStmt(superClassName, "my" + superClassName, superClassName);
            decl.getDefaultConstructor().get().getBody().addStatement(objCreationStmt);
        }
    }
}
