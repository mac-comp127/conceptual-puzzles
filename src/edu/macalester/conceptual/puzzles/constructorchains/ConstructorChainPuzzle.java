package edu.macalester.conceptual.puzzles.constructorchains;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.AstUtils;
import edu.macalester.conceptual.util.ChoiceDeck;
import edu.macalester.conceptual.util.CodeFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
    public byte goalDifficulty() {
        return 2;
    }

    @Override
    public byte maxDifficulty() {
        return 10;
    }

    @Override
    public void generate(PuzzleContext ctx) {

        ctx.output().paragraph("With the following class declarations:");

        // FIXME need to specify which constructor, if leaf class has more than one
        var decls = generateDeclarations(ctx);
        ctx.output().codeBlock(decls.declarationsCode);
        ctx.output().paragraph("What gets printed when you create an instance of " + decls.className + "?");


    }

    private record Declarations(
            String declarationsCode,
            String className) {
    }

    private Declarations generateDeclarations(PuzzleContext ctx) {
        Random rand = ctx.getRandom();
        int depth = classHierarchyDepth(ctx);

        StringBuilder declarationsCode = new StringBuilder();

        // cat file-with-class-decl | bin/astprinter class
        // use the output to inform the code below that builds your AST;
        // the AST is built bottom-up, and the text output uses indentation for the tree,
        // so "bottom-up" here means "rightmost-indented left" there, so to speak

        String className = RandomLoch.getTypeName(ctx);
        var decl = AstUtils.classDecl(className);
        decl.addConstructor(Modifier.Keyword.PUBLIC);
        maybeAddPrintLn(decl, rand);
        maybeAddNonDefaultCtor(decl, rand);

        declarationsCode.append(CodeFormatting.prettify(decl));
        declarationsCode.append("\n\n");

        List<ClassOrInterfaceDeclaration> classes = new ArrayList<>();
        classes.add(decl);

        for (int i = 0; i < depth; i++) {
            List<ClassOrInterfaceDeclaration> classesToAdd = new ArrayList<>();
            int numSiblings = numSiblings(ctx);
            String parentClass = classes.getLast().getNameAsString();
            for (int j = 0; j < numSiblings; j++) {
                // TODO for now, just using the last class in `classes` as the parent, but
                // what if there are two siblings in the previous level? Do we want to be able
                // to choose any one? I suspect continuing to use a list here is going to get
                // unwieldly if we want full generality...OTOH if we can detect if a class has
                // no descendants, we could just randomly choose classes until we one of those.

                className = RandomWeatherPlace.getTypeName(ctx);
                decl = AstUtils.classDecl(className);
                decl.addExtendedType(parentClass);
                decl.addConstructor(Modifier.Keyword.PUBLIC);
                maybeAddNonDefaultCtor(decl, rand);

                // TODO: for extra difficulty, shuffle the order of these
                maybeAddObjCreation(decl, classes, ctx);
                maybeAddNonDefaultCtorObjectCreation(decl, classes, ctx);
                maybeAddPrintLn(decl, rand);

                declarationsCode.append(CodeFormatting.prettify(decl));
                declarationsCode.append("\n\n");

                classesToAdd.add(decl);
            }
            classes.addAll(classesToAdd);
        }

        return new Declarations(declarationsCode.toString(), className);
    }

    // Create a statement like "Bar xyz = new Thing();"
    private static ExpressionStmt getObjectCreationStmt(String staticTypeName, String variableName, String runtimeTypeName) {

        VariableDeclarator varDecl = new VariableDeclarator(
                new ClassOrInterfaceType(null, staticTypeName),
                variableName,
                new ObjectCreationExpr(null, new ClassOrInterfaceType(null, runtimeTypeName),
                        new com.github.javaparser.ast.NodeList<>()));
        VariableDeclarationExpr varDeclExpr = new VariableDeclarationExpr(varDecl);
        return new ExpressionStmt(varDeclExpr);
    }

    private static ExpressionStmt getObjectCreationStmtWithParam(String staticTypeName, String variableName, String runtimeTypeName, String param) {

        NodeList<Expression> nodeList = new NodeList<>(new IntegerLiteralExpr(param));
        VariableDeclarator varDecl = new VariableDeclarator(
                new ClassOrInterfaceType(null, staticTypeName),
                variableName,
                new ObjectCreationExpr(null, new ClassOrInterfaceType(null, runtimeTypeName), nodeList));
        VariableDeclarationExpr varDeclExpr = new VariableDeclarationExpr(varDecl);
        return new ExpressionStmt(varDeclExpr);
    }


    private void maybeAddPrintLn(ClassOrInterfaceDeclaration decl, Random rand) {
        if (rand.nextBoolean()) {
            var msg = new StringLiteralExpr(decl.getName() + " default constructor");
            var sout = new MethodCallExpr("System.out.println", msg);
            decl.getDefaultConstructor().get().getBody().addStatement(sout);
        }
    }

    private void maybeAddNonDefaultCtor(ClassOrInterfaceDeclaration declaration, Random rand) {
        if (rand.nextBoolean()) {
            var nonDefaultCtor = declaration.addConstructor(Modifier.Keyword.PUBLIC);
            nonDefaultCtor.addParameter("int", "n");
            // TODO: do the "maybe" adds for object creation, nondefault ctor object

            if (rand.nextBoolean()) {
                var msg = new StringLiteralExpr(declaration.getName() + " constructor, n = \" + n + \".");
                var sout = new MethodCallExpr("System.out.println", msg);
                nonDefaultCtor.getBody().addStatement(sout);
            }
        }
    }


    private void maybeAddObjCreation(
            ClassOrInterfaceDeclaration decl,
            List<ClassOrInterfaceDeclaration> classes,
            PuzzleContext ctx) {

        Random rand = ctx.getRandom();
        if (rand.nextBoolean()) {

            var superclassesDeck = new ChoiceDeck<>(ctx, classes);
            // TODO: for extra difficulty, allow static and runtime types to differ, so that the runtime type is
            // a superclass of the superclass!
            var superClassName = superclassesDeck.draw().getName().toString();
            var objCreationStmt = getObjectCreationStmt(superClassName, "my" + superClassName, superClassName);
            decl.getDefaultConstructor().get().getBody().addStatement(objCreationStmt);
        }
    }

    private void maybeAddNonDefaultCtorObjectCreation(ClassOrInterfaceDeclaration decl,
                                                      List<ClassOrInterfaceDeclaration> classes,
                                                      PuzzleContext ctx) {
        Random rand = ctx.getRandom();
        if (true /* rand.nextBoolean() */) {

            var superclassesDeck = new ChoiceDeck<>(ctx, classes);
            var superClass = superclassesDeck.draw();

            if (superClass.getConstructorByParameterTypes("int").isPresent()) {
                // TODO: for extra difficulty, allow static and runtime types to differ, so that the runtime type is
                // a superclass of the superclass!
                var superClassName = superClass.getName().toString();
                String param = String.valueOf(rand.nextInt(10, 20));
                String variableName = "my" + superClassName + param;
                var objCreationStmt = getObjectCreationStmtWithParam(superClassName, variableName, superClassName, param);
                var declCtor = new ChoiceDeck<>(ctx, decl.getConstructors()).draw();
                declCtor.getBody().addStatement(objCreationStmt);
            }


        }
    }

    // parameters that depend on the difficulty level. TODO: actually figure out the logic we want.

    private int classHierarchyDepth(PuzzleContext ctx) {
        return 4 + ctx.getRandom().nextInt(ctx.getDifficulty());
    }

    private int numSiblings(PuzzleContext ctx) {
        if (ctx.getDifficulty() < 3) {
            return 1;
        } else {
            return ctx.getRandom().nextInt(1, 3);
        }
    }
}
