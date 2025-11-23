package edu.macalester.conceptual.puzzles.constructorchains;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.AstUtils;
import edu.macalester.conceptual.util.ChoiceDeck;
import edu.macalester.conceptual.util.CodeFormatting;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ConstructorChainPuzzle implements Puzzle {

    private ConstructorChainParameters params;

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
        this.params = new ConstructorChainParameters(goalDifficulty(), ctx.getDifficulty(), ctx.getRandom());

        ctx.output().paragraph("With the following class declarations:");

        // FIXME need to specify which constructor, if leaf class has more than one
        var decls = generateDeclarations(ctx);
        ctx.output().codeBlock(decls.declarationsCode);

        // FIXME: class could have more than one constructor!
        ctx.output().paragraph("What gets printed when you create an instance of " + decls.className + "?");

        /*        ctx.solution(() -> {
            ctx.output().showGraphics(
                ctx.currentSectionTitle() + " Solution",
                AstDrawing.of(
                    code.ast(),
                    ctx.currentSectionHue()));  // coordinate graphics with section heading color in console

            ctx.solutionChecklist(solutionChecklist);
        }); */

        ctx.solution(() -> {
            System.out.print(getSolution(decls));

        });
    }

    private String getSolution(Declarations decls) {
        try {
            File outputFile = File.createTempFile("CtorPuzzle", ".java");

            String className = outputFile.getName();
            className = className.substring(0, className.length() - 5);
            StringBuilder sb = new StringBuilder();
            sb.append(decls.declarationsCode());
            sb.append("""
            public class\s""");
            sb.append(className);
            sb.append("""
            { 
                 public static void main(String[] args) {
                     var foo = new\s""");

            sb.append(decls.className);
            sb.append("(); } }\n");

            var foo = new FileWriter(outputFile);
            foo.write(sb.toString());
            foo.close();


            ProcessBuilder pbCompile = new ProcessBuilder("javac", outputFile.toString());
            Process processCompile = pbCompile.start();
            processCompile.waitFor();

            // Execute
            ProcessBuilder pbRun = new ProcessBuilder("java", className);
            pbRun.directory(new File(outputFile.getParent()));
            pbRun.redirectErrorStream(true);
            Process processRun = pbRun.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(processRun.getInputStream()));
            StringBuilder outputSB = new StringBuilder();
            reader.lines().forEach(line -> { outputSB.append(line); outputSB.append("\n"); });
//            String line;
//            while ((line = reader.readLine()) != null) {
//                System.out.println("Program Output: " + line);
//            }

            return outputSB.toString();
        }
        catch(Exception exc){
            System.err.println("Could not generate temporary file to get solution!");
            return "";
        }
    }

    private record Declarations(String declarationsCode, String className) {
    }

    private Declarations generateDeclarations(PuzzleContext ctx) {
        int depth = this.params.hierarchyDepth();
        StringBuilder declarationsCode = new StringBuilder();

        // cat file-with-class-decl | bin/astprinter class
        // use the output to inform the code below that builds your AST;
        // the AST is built bottom-up, and the text output uses indentation for the tree,
        // so "bottom-up" here means "rightmost-indented left" there, so to speak

        List<ClassOrInterfaceDeclaration> classes = new ArrayList<>();
        String className = RandomLoch.getTypeName(ctx);
        var decl = getDefaultDeclaration(className, classes, ctx);
        maybeAddNonDefaultCtor(decl, ctx, classes);
        declarationsCode.append(CodeFormatting.prettify(decl));
        declarationsCode.append("\n\n");
        classes.add(decl);

        for (int i = 0; i < depth; i++) {
            List<ClassOrInterfaceDeclaration> classesToAdd = new ArrayList<>();
            int numSiblings = this.params.numSiblings();
            String parentClass = classes.getLast().getNameAsString();
            for (int j = 0; j < numSiblings; j++) {
                // TODO for now, just using the last class in `classes` as the parent, but
                // what if there are two siblings in the previous level? Do we want to be able
                // to choose any one? I suspect continuing to use a list here is going to get
                // unwieldly if we want full generality...OTOH if we can detect if a class has
                // no descendants, we could just randomly choose classes until we one of those.

                className = RandomLoch.getTypeName(ctx);
                decl = AstUtils.classDecl(className);
                decl.addExtendedType(parentClass);
                decl.addConstructor(Modifier.Keyword.PUBLIC);
                maybeAddNonDefaultCtor(decl, ctx, classes);

                List<Statement> ctorstatements = new ArrayList<>();
                maybeAddObjCreation(classes, ctx).ifPresent(ctorstatements::add);
                maybeAddNonDefaultCtorObjectCreation(classes, ctx).ifPresent(ctorstatements::add);
                maybePrintLn(decl.getName() + " default constructor").ifPresent(ctorstatements::add);
                if (!ctorstatements.isEmpty()) {
                    Collections.shuffle(ctorstatements, ctx.getRandom());
                    for (var statement : ctorstatements) {
                        decl.getDefaultConstructor().get().getBody().addStatement(statement);
                    }
                }

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
     * @param ctx       puzzle context, used for difficulty level and random generator
     * @param classes   list of ancestor classes
     * @return class declaration object
     */
    private ClassOrInterfaceDeclaration getDefaultDeclaration(String className, List<ClassOrInterfaceDeclaration> classes, PuzzleContext ctx) {
        var decl = AstUtils.classDecl(className);
        decl.addConstructor(Modifier.Keyword.PUBLIC);

        List<Statement> ctorstatements = new ArrayList<>();
        maybePrintLn(decl.getName() + " default constructor").ifPresent(ctorstatements::add);
        if (!classes.isEmpty()) {
            maybeAddObjCreation(classes, ctx).ifPresent(ctorstatements::add);
            maybeAddNonDefaultCtorObjectCreation(classes, ctx).ifPresent(ctorstatements::add);
        }
        if (!ctorstatements.isEmpty()) {
            Collections.shuffle(ctorstatements, ctx.getRandom());
            for (var statement : ctorstatements) {
                decl.getDefaultConstructor().get().getBody().addStatement(statement);
                System.out.println("line 159 okay");

            }
        }


        return decl;
    }

    /**
     * Create a statement like "Bar xyz = new Thing();"
     *
     * @param staticTypeName  static / LHS type name -- "Bar" above
     * @param variableName    variable name
     * @param runtimeTypeName runtiome / RHS type -- "Thing" above
     * @return expression statement object
     */
    // FIXME: would this belong in AstUtils?
    private static ExpressionStmt getObjectCreationStmt(String staticTypeName, String variableName, String runtimeTypeName) {

        VariableDeclarator varDecl = new VariableDeclarator(new ClassOrInterfaceType(null, staticTypeName), variableName, new ObjectCreationExpr(null, new ClassOrInterfaceType(null, runtimeTypeName), new com.github.javaparser.ast.NodeList<>()));
        VariableDeclarationExpr varDeclExpr = new VariableDeclarationExpr(varDecl);
        return new ExpressionStmt(varDeclExpr);
    }

    /**
     * Create a statement like "Bar xyz = new Thing(5);"
     *
     * @param staticTypeName  static / LHS type name -- "Bar" above
     * @param variableName    variable name
     * @param runtimeTypeName runtiome / RHS type -- "Thing" above
     * @param param           the parameter to the object creation -- "5" above.
     * @return expression statement object
     */
    // FIXME: would this belong in AstUtils?
    private static ExpressionStmt getObjectCreationStmtWithParam(String staticTypeName, String variableName, String runtimeTypeName, String param) {

        NodeList<Expression> nodeList = new NodeList<>(new IntegerLiteralExpr(param));
        VariableDeclarator varDecl = new VariableDeclarator(new ClassOrInterfaceType(null, staticTypeName), variableName, new ObjectCreationExpr(null, new ClassOrInterfaceType(null, runtimeTypeName), nodeList));
        VariableDeclarationExpr varDeclExpr = new VariableDeclarationExpr(varDecl);
        return new ExpressionStmt(varDeclExpr);
    }


    /**
     * Maybe return a println statement for the provided message,
     * if the parameters object says we should.
     *
     * @param message - argument to the println statement
     */
    private Optional<Statement> maybePrintLn(String message) {
        if (this.params.addPrintLn()) {
            return Optional.of(new ExpressionStmt(new MethodCallExpr("System.out.println", new StringLiteralExpr(message))));
        }
        return Optional.empty();
    }

    /**
     * Maybe add a non-default constructor to the provided class. The constructor accepts a single
     * int parameter.
     *
     * @param declaration class declaration
     * @param ctx         puzzle context
     * @param classes     list of classes to choose from for object creation statements
     */
    private void maybeAddNonDefaultCtor(ClassOrInterfaceDeclaration declaration, PuzzleContext ctx, List<ClassOrInterfaceDeclaration> classes) {
        if (this.params.addNonDefaultCtor()) {
            var nonDefaultCtor = declaration.addConstructor(Modifier.Keyword.PUBLIC);
            nonDefaultCtor.addParameter("int", "n");
            maybePrintLn(declaration.getName() + " constructor, n = \" + n + \".").ifPresent(nonDefaultCtor.getBody()::addStatement);
            maybeAddObjCreation(classes, ctx).ifPresent(nonDefaultCtor.getBody()::addStatement);
            maybeAddNonDefaultCtorObjectCreation(classes, ctx).ifPresent(nonDefaultCtor.getBody()::addStatement);
        }
    }

    /**
     * Format provided name as a variable name -- lowercase the first letter.
     * FIXME would this belong in Nonsense, or somehow with NameFormat?
     *
     * @param name variable name
     * @return name, but first letter will be lowercase
     */
    private String variableName(String name) {
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }

    /**
     * add an object creation statement to the provided constructor
     *
     * @param classes list of classes to choose from for object creation statement
     * @param ctx     puzzle context
     */
    private Optional<ExpressionStmt> maybeAddObjCreation(List<ClassOrInterfaceDeclaration> classes, PuzzleContext ctx) {
        if (!classes.isEmpty() && this.params.addObjectCreationStatement()) {
            var superclassesDeck = new ChoiceDeck<>(ctx, classes);
            // TODO: for extra difficulty, allow static and runtime types to differ, so that the runtime type is
            // a superclass of the superclass!
            var superClassName = superclassesDeck.draw().getName().toString();

            String variable = variableName(RandomWeatherPlace.getTypeName(ctx));
            System.out.println("object creation of type " + superClassName + " var name " + variable);
            return Optional.of(getObjectCreationStmt(superClassName, variable, superClassName));
        }
        return Optional.empty();
    }

    private Optional<ExpressionStmt> maybeAddNonDefaultCtorObjectCreation(List<ClassOrInterfaceDeclaration> classes, PuzzleContext ctx) {
        Optional<ClassOrInterfaceDeclaration> maybeSuperClass = superClassWithNonDefaultCtor(classes, ctx);
        if (maybeSuperClass.isPresent() && this.params.addNonDefaultCtorObjectCreation()) {
            // TODO: for extra difficulty, allow static and runtime types to differ, so that the runtime type is
            // a superclass of the superclass!

            // ugh, wish Java did Typescript-style control flow analysis
            var superClass = maybeSuperClass.get();

            var superClassName = superClass.getName().toString();
            String param = String.valueOf(ctx.getRandom().nextInt(10, 20));
            String variableName = variableName(RandomWeatherPlace.getTypeName(ctx));
            return Optional.of(getObjectCreationStmtWithParam(superClassName, variableName, superClassName, param));
        }
        return Optional.empty();
    }

    /**
     * Get a superclass from the list of classes that has a non-default constructor. Use Optional since there may not be such
     * a class in the list.
     *
     * @param classes list of classes from which to draw
     * @param ctx     PuzzleContext so we can pass random generator to ChoiceDeck
     * @return class declaration that has a non-default constructor
     */
    private Optional<ClassOrInterfaceDeclaration> superClassWithNonDefaultCtor(List<ClassOrInterfaceDeclaration> classes, PuzzleContext ctx) {
        var superclassesDeck = new ChoiceDeck<>(ctx, classes);
        ClassOrInterfaceDeclaration superClass = null;
        while (superClass == null) {
            if (superclassesDeck.isEmpty()) {
                return Optional.empty();
            }
            var candidate = superclassesDeck.draw();
            if (candidate.getConstructorByParameterTypes("int").isPresent()) {
                superClass = candidate;
            }
        }
        return Optional.of(superClass);
    }


}
