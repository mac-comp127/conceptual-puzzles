package edu.macalester.conceptual.puzzles.constructorchains;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.utils.Utils;
import edu.macalester.conceptual.Puzzle;
import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.AstUtils;
import edu.macalester.conceptual.util.ChoiceDeck;
import edu.macalester.conceptual.util.CodeSnippet;
import edu.macalester.conceptual.util.Evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ConstructorChainPuzzle implements Puzzle {

    private ConstructorChainParameters params;
    private RandomLoch randomLoch;
    private RandomWeatherPlace randomWeatherPlace;

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
        this.randomLoch = new RandomLoch(ctx);
        this.randomWeatherPlace = new RandomWeatherPlace(ctx);

        ctx.output().paragraph("With the following class declarations:");
        var decls = generateDeclarations(ctx);
        ctx.output().codeBlock(decls);
        ctx.output().paragraph(getPrompt(ctx, (TypeDeclaration<ClassOrInterfaceDeclaration>) decls.getTypes().getLast().orElseThrow()));
        ctx.solution(() -> ctx.output().codeBlock(getSolution(decls)));
    }

    String constructorArgs = "";
    private String getPrompt(PuzzleContext ctx, TypeDeclaration<ClassOrInterfaceDeclaration> classDeclaration) {
        var constructor = new ChoiceDeck<>(ctx, classDeclaration.getConstructors()).draw();

        String prompt = "What gets printed when you create an instance of " + classDeclaration.getName() + " ";
        if (constructor.getParameters().isEmpty()) {
            prompt += "using the default constructor?";
        } else {
            constructorArgs = String.valueOf(ctx.getRandom().nextInt(1, 10));
            prompt += "using the non-default constructor with an argument of " + constructorArgs + "?";
        }
        return prompt;
    }

    private String getSolution(CompilationUnit decls) {
        var bottomClass = decls.getTypes().getLast().orElseThrow();
        return Evaluator.captureOutput(
                CodeSnippet.build()
                        .withMainBody("var x = new " + bottomClass.getName() + "(" + constructorArgs + ");")
                        .withOtherClasses(decls.toString()
                        ));
    }

    private CompilationUnit generateDeclarations(PuzzleContext ctx) {
        int depth = this.params.hierarchyDepth();
        CompilationUnit declarations = new CompilationUnit();
        appendClass(ctx, declarations);

        // FIXME TODO see notes.org for things to fix with the hierarchy generation

        for (int i = 0; i < depth; i++) {
            int numSiblings = 1; // this.params.numSiblings();
            // disabling the "siblings" stuff for now -- we will, in effect, handle those when we fully
            // implement the idea of first generating the actual chain, and *then* sprinkling in distractors.

            for (int j = 0; j < numSiblings; j++) {
                appendClass(ctx, declarations);
            }
        }
        return declarations;
    }

    private ClassOrInterfaceDeclaration appendClass(PuzzleContext ctx, CompilationUnit declarations) {
        String className = randomLoch.draw();
        var decl = getDefaultDeclaration(className, declarations, ctx);
        maybeAddNonDefaultCtor(decl, ctx, AstUtils.classesInCompilationUnit(declarations));
        declarations.addType(decl);
        return decl;
    }

    /**
     * Create a class declaration with a default constructor that may or may include print statements, object creation,
     * or other code related to the difficulty level.
     *
     * @param className    name of the class
     * @param declarations list of ancestor classes
     * @param ctx          puzzle context, used for difficulty level and random generator
     * @return class declaration object
     */
    private ClassOrInterfaceDeclaration getDefaultDeclaration(String className, CompilationUnit declarations, PuzzleContext ctx) {
        var decl = AstUtils.classDecl(className);
        decl.addConstructor(Modifier.Keyword.PUBLIC);

        List<Statement> ctorstatements = new ArrayList<>();
        maybePrintLn(decl.getName() + " default constructor").ifPresent(ctorstatements::add);
        var classes = AstUtils.classesInCompilationUnit(declarations);
        if (!classes.isEmpty()) {

            var parentClass = classes.getLast().getNameAsString();
            decl.addExtendedType(parentClass);

            maybeAddSuperCall(ctx, classes.getLast()).ifPresent(decl.getDefaultConstructor().orElseThrow().getBody()::addStatement);
            maybeAddObjCreation(classes, ctx).ifPresent(ctorstatements::add);
            maybeAddNonDefaultCtorObjectCreation(classes, ctx).ifPresent(ctorstatements::add);
        }
        if (!ctorstatements.isEmpty()) {
            Collections.shuffle(ctorstatements, ctx.getRandom());
            for (var statement : ctorstatements) {
                decl.getDefaultConstructor().orElseThrow().getBody().addStatement(statement);
            }
        }

        return decl;

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

    private Optional<Statement> maybeAddSuperCall(PuzzleContext ctx, ClassOrInterfaceDeclaration parentClass) {
        if (this.params.addSuperCall()) {
            var ctor = (new ChoiceDeck<>(ctx, parentClass.getConstructors())).draw();
            if (ctor.getParameters().isEmpty()) {
                return Optional.of(new ExpressionStmt(new MethodCallExpr("super")));
            } else {
                return Optional.of(new ExpressionStmt(new MethodCallExpr("super", new IntegerLiteralExpr("123"))));
            }
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

            String variable = Utils.decapitalize(randomWeatherPlace.draw());
            return Optional.of(AstUtils.getObjectCreationStmt(superClassName, variable, superClassName));
        }
        return Optional.empty();
    }

    private Optional<ExpressionStmt> maybeAddNonDefaultCtorObjectCreation(List<ClassOrInterfaceDeclaration> classes, PuzzleContext ctx) {
        Optional<ClassOrInterfaceDeclaration> maybeSuperClass = superClassWithNonDefaultCtor(classes, ctx);
        if (maybeSuperClass.isPresent() && this.params.addNonDefaultCtorObjectCreation()) {
            // TODO: for extra difficulty, allow static and runtime types to differ, so that the runtime type is
            // a superclass of the superclass!
            var superClass = maybeSuperClass.get();
            var superClassName = superClass.getName().toString();
            String paramString = String.valueOf(ctx.getRandom().nextInt(10, 20));
            NodeList<Expression> params = new NodeList<>(new IntegerLiteralExpr(paramString));
            String variableName = Utils.decapitalize(randomWeatherPlace.draw());
            return Optional.of(AstUtils.getObjectCreationStmtWithParam(superClassName, variableName, superClassName, params));
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
        List<ClassOrInterfaceDeclaration> superClasses = classes.stream().filter(c -> c.getConstructorByParameterTypes("int").isPresent()).toList();
        if (superClasses.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(new ChoiceDeck<>(ctx, superClasses).draw());
        }
    }


}
