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

    private record Declarations(String declarationsCode, String className) {
    }

    private Declarations generateDeclarations(PuzzleContext ctx) {
        int depth = classHierarchyDepth(ctx);
        StringBuilder declarationsCode = new StringBuilder();


        var params = new ConstructorChainParameters(goalDifficulty(), ctx.getDifficulty(), ctx.getRandom());





        // cat file-with-class-decl | bin/astprinter class
        // use the output to inform the code below that builds your AST;
        // the AST is built bottom-up, and the text output uses indentation for the tree,
        // so "bottom-up" here means "rightmost-indented left" there, so to speak

        List<ClassOrInterfaceDeclaration> classes = new ArrayList<>();
        String className = RandomLoch.getTypeName(ctx);
        var decl = getDefaultDeclaration(className, classes, ctx);
        maybeAddNonDefaultCtor(decl,ctx);
        declarationsCode.append(CodeFormatting.prettify(decl));
        declarationsCode.append("\n\n");
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
                maybeAddNonDefaultCtor(decl, ctx);

                // TODO: for extra difficulty, shuffle the order of these
                maybeAddObjCreation(decl, classes, ctx);
                maybeAddNonDefaultCtorObjectCreation(decl, classes, ctx);
                maybeAddPrintLn(decl, ctx);

                declarationsCode.append(CodeFormatting.prettify(decl));
                declarationsCode.append("\n\n");

                classesToAdd.add(decl);
            }
            classes.addAll(classesToAdd);
        }

        return new Declarations(declarationsCode.toString(), className);
    }

    /**
     * Create a class declaration with a default constructor that may or may include print statements, object creation,
     * or other code related to the difficulty level.
     *
     * @param className name of the class
     * @param ctx puzzle context, used for difficulty level and random generator
     * @param classes list of ancestor classes
     * @return class declaration object
     */
    private ClassOrInterfaceDeclaration getDefaultDeclaration(String className, List<ClassOrInterfaceDeclaration> classes, PuzzleContext ctx ) {
        var decl = AstUtils.classDecl(className);
        decl.addConstructor(Modifier.Keyword.PUBLIC);
        maybeAddPrintLn(decl, ctx);
        if (!classes.isEmpty()) {
            maybeAddObjCreation(decl, classes, ctx);
            maybeAddNonDefaultCtorObjectCreation(decl, classes, ctx);
        }
        return decl;
    }

    /**
     * Create a statement like "Bar xyz = new Thing();"
     * @param staticTypeName static / LHS type name -- "Bar" above
     * @param variableName variable name
     * @param runtimeTypeName runtiome / RHS type -- "Thing" above
     * @return expression statement object
     */
    private static ExpressionStmt getObjectCreationStmt(String staticTypeName, String variableName, String runtimeTypeName) {

        VariableDeclarator varDecl = new VariableDeclarator(new ClassOrInterfaceType(null, staticTypeName), variableName, new ObjectCreationExpr(null, new ClassOrInterfaceType(null, runtimeTypeName), new com.github.javaparser.ast.NodeList<>()));
        VariableDeclarationExpr varDeclExpr = new VariableDeclarationExpr(varDecl);
        return new ExpressionStmt(varDeclExpr);
    }

    /**
     * Create a statement like "Bar xyz = new Thing(5);"
     * @param staticTypeName
     * @param variableName
     * @param runtimeTypeName
     * @param param the parameter to the object creation -- "5" above.
     * @return expression statement object
     */
    private static ExpressionStmt getObjectCreationStmtWithParam(String staticTypeName, String variableName, String runtimeTypeName, String param) {

        NodeList<Expression> nodeList = new NodeList<>(new IntegerLiteralExpr(param));
        VariableDeclarator varDecl = new VariableDeclarator(new ClassOrInterfaceType(null, staticTypeName), variableName, new ObjectCreationExpr(null, new ClassOrInterfaceType(null, runtimeTypeName), nodeList));
        VariableDeclarationExpr varDeclExpr = new VariableDeclarationExpr(varDecl);
        return new ExpressionStmt(varDeclExpr);
    }


    private static void maybeAddPrintLn(ClassOrInterfaceDeclaration decl, PuzzleContext ctx) {
        double probability = difficultyToPrintProbability(ctx);
        if (ctx.getRandom().nextDouble() < probability) {
            var msg = new StringLiteralExpr(decl.getName() + " default constructor");
            var sout = new MethodCallExpr("System.out.println", msg);
            decl.getDefaultConstructor().get().getBody().addStatement(sout);
        }
    }

    private void maybeAddNonDefaultCtor(ClassOrInterfaceDeclaration declaration, PuzzleContext ctx) {
        double probability = difficultyToNonDefaultCtorProbability(ctx);
        if (ctx.getRandom().nextDouble() < probability) {
            var nonDefaultCtor = declaration.addConstructor(Modifier.Keyword.PUBLIC);
            nonDefaultCtor.addParameter("int", "n");
            // TODO: do the "maybe" adds for object creation, nondefault ctor object

            if (ctx.getRandom().nextBoolean()) {
                var msg = new StringLiteralExpr(declaration.getName() + " constructor, n = \" + n + \".");
                var sout = new MethodCallExpr("System.out.println", msg);
                nonDefaultCtor.getBody().addStatement(sout);
            }
        }
    }

    private double difficultyToNonDefaultCtorProbability(PuzzleContext ctx) {
        if (ctx.getDifficulty() < goalDifficulty()) {
            return 0.25;
        } else if (ctx.getDifficulty() < 7) {
            return 0.5;
        } else {
            return 0.75;
        }
    }


    private void maybeAddObjCreation(ClassOrInterfaceDeclaration decl, List<ClassOrInterfaceDeclaration> classes, PuzzleContext ctx) {

        double probability = difficultyToAddObjCreationProbability(ctx);
        if (ctx.getRandom().nextDouble() < probability) {
            var superclassesDeck = new ChoiceDeck<>(ctx, classes);
            // TODO: for extra difficulty, allow static and runtime types to differ, so that the runtime type is
            // a superclass of the superclass!
            var superClassName = superclassesDeck.draw().getName().toString();
            var objCreationStmt = getObjectCreationStmt(superClassName, "my" + superClassName, superClassName);
            decl.getDefaultConstructor().get().getBody().addStatement(objCreationStmt);
        }
    }

    private double difficultyToAddObjCreationProbability(PuzzleContext ctx) {
        if (ctx.getDifficulty() < goalDifficulty()) {
            return 0.25;
        } else if (ctx.getDifficulty() < 5) {
            return 0.5;
        } else {
            return 0.75;
        }
    }

    private double difficultyToNonDefaultCtorObjectCreationProbability(PuzzleContext ctx) {
        return 0.8 * difficultyToAddObjCreationProbability(ctx);
    }

    private void maybeAddNonDefaultCtorObjectCreation(ClassOrInterfaceDeclaration decl, List<ClassOrInterfaceDeclaration> classes, PuzzleContext ctx) {
        double probability = difficultyToNonDefaultCtorProbability(ctx);
        if (ctx.getRandom().nextDouble() < probability) {
            var superclassesDeck = new ChoiceDeck<>(ctx, classes);
            var superClass = superclassesDeck.draw();

            if (superClass.getConstructorByParameterTypes("int").isPresent()) {
                // TODO: for extra difficulty, allow static and runtime types to differ, so that the runtime type is
                // a superclass of the superclass!
                var superClassName = superClass.getName().toString();
                String param = String.valueOf(ctx.getRandom().nextInt(10, 20));
                String variableName = "my" + superClassName + param;
                var objCreationStmt = getObjectCreationStmtWithParam(superClassName, variableName, superClassName, param);
                var declCtor = new ChoiceDeck<>(ctx, decl.getConstructors()).draw();
                declCtor.getBody().addStatement(objCreationStmt);
            }
        }
    }

    // parameters that depend on the difficulty level. TODO: actually figure out the logic we want.

    private static int classHierarchyDepth(PuzzleContext ctx) {
        return 4 + ctx.getRandom().nextInt(ctx.getDifficulty());
    }

    private static int numSiblings(PuzzleContext ctx) {
        if (ctx.getDifficulty() < 3) {
            return 1;
        } else {
            return ctx.getRandom().nextInt(1, 3);
        }
    }

    /**
     * for now, just 50-50 chance we add a print statement. Higher difficulty may be more likely, so there's more output?
     *
     * @param ctx
     * @return
     */
    private static double difficultyToPrintProbability(PuzzleContext ctx) {
        return 0.5;
    }
}
