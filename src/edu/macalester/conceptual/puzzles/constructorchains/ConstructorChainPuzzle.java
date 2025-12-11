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
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        return "ctor";
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
        var declarations = generateDeclarations(ctx);
        ctx.output().codeBlock(declarations);
        ctx.output().paragraph(getPrompt(ctx, (TypeDeclaration<ClassOrInterfaceDeclaration>) declarations.getTypes().getLast().orElseThrow()));
        ctx.solution(() -> ctx.output().codeBlock(getSolution(declarations)));
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

    private String getSolution(CompilationUnit declarations) {
        var bottomClass = declarations.getTypes().getLast().orElseThrow();
        return Evaluator.captureOutput(
                CodeSnippet.build()
                        .withMainBody("var x = new " + bottomClass.getName() + "(" + constructorArgs + ");")
                        .withOtherClasses(declarations.toString()
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

    /**
     * Create a new class declaration and append it to the provided compilation unit.
     * @param ctx - puzzle context
     * @param declarations - current compilation unit
     * @return declaration of the new class; side effect: compilation unit has new class declaration appended
     */
    private ClassOrInterfaceDeclaration appendClass(PuzzleContext ctx, CompilationUnit declarations) {
        String className = randomLoch.draw();
        var declaration = getDefaultDeclaration(className, declarations, ctx);
        maybeAddNonDefaultCtor(declaration, ctx, AstUtils.classesInCompilationUnit(declarations));
        declarations.addType(declaration);
        return declaration;
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
        var declaration = AstUtils.classDecl(className);
        declaration.addConstructor(Modifier.Keyword.PUBLIC);

        List<Statement> constructorStatements = new ArrayList<>();
        maybePrintLn(declaration.getName() + " default constructor").ifPresent(constructorStatements::add);
        var classes = AstUtils.classesInCompilationUnit(declarations);

        if (!classes.isEmpty()) {
            String parentClass = classes.getLast().getNameAsString();
            declaration.addExtendedType(parentClass);

            maybeSuperCall(ctx, classes.getLast()).ifPresent(declaration.getDefaultConstructor().orElseThrow().getBody()::addStatement);
            maybeObjCreation(classes, ctx).ifPresent(constructorStatements::add);
            maybeNonDefaultCtorObjectCreation(classes, ctx).ifPresent(constructorStatements::add);
        }
        if (!constructorStatements.isEmpty()) {
            Collections.shuffle(constructorStatements, ctx.getRandom());
            for (var statement : constructorStatements) {
                declaration.getDefaultConstructor().orElseThrow().getBody().addStatement(statement);
            }
        }
        return declaration;
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
     * Maybe return an explicit <code>super()</code> call. Randomly chooses among the constructors
     * of <code>superClass</code>.
     * @param ctx puzzle context
     * @param superClass declaration for the superclass.
     * @return Optional expression statement <code>super()</code> or <code>super(123)</code>.
     */
    private Optional<Statement> maybeSuperCall(PuzzleContext ctx, ClassOrInterfaceDeclaration superClass) {
        if (this.params.addSuperCall()) {
            var constructor = (new ChoiceDeck<>(ctx, superClass.getConstructors())).draw();
            if (constructor.getParameters().isEmpty()) {
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

            List<Statement> constructorStatements = new ArrayList<>();
            if (!classes.isEmpty()) {
                maybeSuperCall(ctx, classes.getLast()).ifPresent(nonDefaultCtor.getBody()::addStatement);
            }

            maybePrintLn(declaration.getName() + " constructor, n = \" + n + \".").ifPresent(constructorStatements::add);
            maybeObjCreation(classes, ctx).ifPresent(constructorStatements::add);
            maybeNonDefaultCtorObjectCreation(classes, ctx).ifPresent(constructorStatements::add);

            if (!constructorStatements.isEmpty()) {
                Collections.shuffle(constructorStatements, ctx.getRandom());
                for (var statement : constructorStatements) {
                    nonDefaultCtor.getBody().addStatement(statement);
                }
            }
        }
    }

    /**
     * Add an object creation statement to the provided constructor
     *
     * @param classes list of classes to choose from for object creation statement
     * @param ctx     puzzle context
     * @return an Optional object creation statement
     */
    private Optional<ExpressionStmt> maybeObjCreation(List<ClassOrInterfaceDeclaration> classes, PuzzleContext ctx) {
        if (!classes.isEmpty() && this.params.addObjectCreationStatement()) {
            TypeNames names = getTypeNames(classes, ctx);
            String variable = Utils.decapitalize(randomWeatherPlace.draw());
            return Optional.of(AstUtils.getObjectCreationStmt(names.staticTypeName(), variable, names.dynamicTypeName()));
        }
        return Optional.empty();
    }

    /**
     * Get the type names to be used in an object creation statement: the static and dynamic (compile-time and run-time) types.
     *
     * @param classes list of classes to choose from for object creation statement
     * @param ctx     puzzle context
     * @return TypeNames record with static and dynamic type names
     */
    private @NonNull TypeNames getTypeNames(List<ClassOrInterfaceDeclaration> classes, PuzzleContext ctx) {
        if (this.params.typeNamesDiffer()) {
            var indices = new ChoiceDeck<>(ctx, IntStream.range(0, classes.size()).boxed().collect(Collectors.toList()));
            int indexA = indices.draw();
            int indexB = indices.draw();
            int dynamicTypeIndex = Math.max(indexA, indexB);
            int staticTypeIndex = Math.min(indexA, indexB);
            String dynamicTypeName = classes.get(dynamicTypeIndex).getName().toString();
            String staticTypeName = classes.get(staticTypeIndex).getName().toString();
            return new TypeNames(dynamicTypeName, staticTypeName);
        } else {
            String typeName = (new ChoiceDeck<>(ctx, classes)).draw().getName().toString().toString();
            return new TypeNames(typeName, typeName);
        }
    }

    private record TypeNames(String dynamicTypeName, String staticTypeName) {
    }

    /**
     * Same as {@link #maybeObjCreation(List, PuzzleContext) maybeAddObjCreation}, but creates an
     * object using a non-default constructor.
     *
     * The handling here is a bit different because we need to first find a class that *has* a
     * non-default constructor (if one even exists); only then can we choose a class for the static
     * type.
     * @param classes list of classes to choose from for object creation statement
     * @param ctx     puzzle context
     * @return an Optional object creation statement that calls a non-default constructor
     */
    private Optional<ExpressionStmt> maybeNonDefaultCtorObjectCreation(List<ClassOrInterfaceDeclaration> classes, PuzzleContext ctx) {
        List<ClassOrInterfaceDeclaration> superClasses = superClassesWithNonDefaultConstructor(classes, ctx);
        if (!superClasses.isEmpty() && this.params.addNonDefaultCtorObjectCreation()) {
            var superClass = new ChoiceDeck<>(ctx, superClasses).draw();
            String dynamicTypeName = superClass.getName().toString();
            String staticTypeName;
            if (this.params.typeNamesDiffer()) {
                int dynamicTypeIndex = classes.indexOf(superClass);
                int staticTypeIndex = ctx.getRandom().nextInt(dynamicTypeIndex + 1);
                staticTypeName = classes.get(staticTypeIndex).getName().toString();
            } else {
                staticTypeName = dynamicTypeName;
            }

            String paramString = String.valueOf(ctx.getRandom().nextInt(10, 20));
            NodeList<Expression> params = new NodeList<>(new IntegerLiteralExpr(paramString));
            String variableName = Utils.decapitalize(randomWeatherPlace.draw());

            return Optional.of(AstUtils.getObjectCreationStmtWithParam(staticTypeName, variableName, dynamicTypeName, params));
        }
        return Optional.empty();
    }

    /**
     * Return the list of classes that have a non-default constructor. Can be empty, of course.
     *
     * @param classes list of classes from which to draw
     * @param ctx     PuzzleContext so we can pass random generator to ChoiceDeck
     * @return list of class declarations that have a non-default constructor
     */
    private List<ClassOrInterfaceDeclaration> superClassesWithNonDefaultConstructor(List<ClassOrInterfaceDeclaration> classes, PuzzleContext ctx) {
        return classes.stream().filter(c -> c.getConstructorByParameterTypes("int").isPresent()).toList();
    }
}
