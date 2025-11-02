package edu.macalester.conceptual.util;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static com.github.javaparser.StaticJavaParser.parseExpression;
import static com.github.javaparser.StaticJavaParser.parseStatement;
import static com.github.javaparser.ast.expr.BinaryExpr.Operator.*;
import static com.github.javaparser.ast.expr.UnaryExpr.Operator.*;
import static com.github.javaparser.utils.Utils.capitalize;
import static java.util.stream.Collectors.groupingBy;

/**
 * Assorted utilities for creating and working with JavaParser ASTs.
 */
public enum AstUtils {
    ; // no cases; static methods only

    //––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––
    // AST Creation
    //––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––

    /**
     * Creates an empty declaration for a public class.
     */
    public static ClassOrInterfaceDeclaration publicClassDecl(String name) {
        return publicTypeDecl(name, false);
    }

    public static ClassOrInterfaceDeclaration classDecl(String name) {
        return typeDecl(name, false);
    }

    /**
     * Creates an empty declaration for a public class or interface.
     */
    public static ClassOrInterfaceDeclaration publicTypeDecl(String name, boolean isInterface) {
        return new ClassOrInterfaceDeclaration(
            AstUtils.nodes(Modifier.publicModifier()), isInterface, name);
    }

    /**
     * Adds a standard getter method for the given property name (not including the "get"), whose
     * implementation returns the instance variable with the same name as the property.
     *
     * @return
     */
    public static MethodDeclaration addGetter(
        ClassOrInterfaceDeclaration classDecl,
        String type,
        String name
    ) {
        return addGetter(classDecl, type, name, parseExpression(name));
    }
    /**
     * Adds a getter method for the given property name that returns an arbitrary expression.
     */
    public static MethodDeclaration addGetter(
        ClassOrInterfaceDeclaration classDecl,
        String type,
        String name,
        Expression returnValue
    ) {
        var getter = classDecl.addMethod(getterName(name), Modifier.Keyword.PUBLIC);
        getter.setType(type);
        getter.setBody(blockOf(new ReturnStmt(returnValue)));
        return getter;
    }

    public static String getterName(String propName) {
        return "get" + capitalize(propName);
    }

    /**
     * Adds a standard setter method for the given property name (not including the "get"), whose
     * implementation sets the instance variable with the same name as the property to the
     * value of the method’s single parameter.
     */
    public static MethodDeclaration addSetter(
        ClassOrInterfaceDeclaration classDecl,
        String type,
        String name
    ) {
        var setter = classDecl.addMethod(setterName(name), Modifier.Keyword.PUBLIC);
        setter.addParameter(type, name);
        setter.setBody(blockOf(
            buildSetterStatement(name)
        ));
        return setter;
    }

    public static String setterName(String propName) {
        return "set" + capitalize(propName);
    }

    /**
     * Generates a statement of the form `this.name = name;`. Useful for both setter methods
     * and constructors.
     */
    public static Statement buildSetterStatement(String name) {
        return parseStatement("this." + name + " = " + name + ";");
    }

    /**
     * Creates an empty declaration for a class or interface.
     */
    public static ClassOrInterfaceDeclaration typeDecl(String name, boolean isInterface) {
        return new ClassOrInterfaceDeclaration(
                AstUtils.nodes(), isInterface, name);
    }

    /**
     * Creates an IntegerLiteralExpr for the given int value.
     */
    public static IntegerLiteralExpr intLiteral(int i) {
        return new IntegerLiteralExpr(String.valueOf(i));
    }

    /**
     * Creates a reference to the object type with the given name, suitable for use in
     * a declaration, type cast expression, etc.
     */
    public static ClassOrInterfaceType classNamed(String name) {
        return new ClassOrInterfaceType(null, name);
    }

    /**
     * Wraps a VariableDeclarator (e.g. <code>int x = 3</code>) inside a statement.
     */
    public static ExpressionStmt variableDeclarationStmt(VariableDeclarator variable) {
        return new ExpressionStmt(
            new VariableDeclarationExpr(variable));
    }

    /**
     * Builds a variable declarations .
     */
    public static ExpressionStmt variableDeclarationStmt(String type, String varName, Expression initializer) {
        return variableDeclarationStmt(
            new VariableDeclarator(
                classNamed(type),
                new SimpleName(varName),
                initializer));
    }

    /**
     * Convenience for creating a NodeList.
     */
    @SafeVarargs
    public static <NodeType extends Node> NodeList<NodeType> nodes(NodeType... nodes) {
        return new NodeList<>(nodes);
    }

    /**
     * Convenience for creating a block statement (i.e. many statements inside curly braces).
     */
    public static BlockStmt blockOf(Statement... statements) {
        return new BlockStmt(nodes(statements));
    }

    /**
     * Returns the given expressions joined by the given operator to form a left-branching tree,
     * e.g. <code>joinedWithOperator(+, 1, 2, 3, 4)</code> → <code>((1 + 2) + 3) + 4</code>
     */
    public static Expression joinedWithOperator(BinaryExpr.Operator operator, Expression... exprs) {
        return joinedWithOperator(operator, Arrays.stream(exprs));
    }

    public static Expression joinedWithOperator(BinaryExpr.Operator operator, Stream<Expression> exprs) {
        return exprs
            .reduce((lhs, rhs) -> new BinaryExpr(lhs, rhs, operator))
            .orElseThrow();
    }

    //––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––
    // AST Analysis
    //––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––

    /**
     * Finds all the descendants of <code>node</code> whose class is <code>type</code> (or a subtype),
     * in DFS preorder. Example: <code>allDescendantsOfType(UnaryExpr.class, someMethodDecl)</code>
     */
    public static <NodeType extends Node> List<NodeType> allDescendantsOfType(Class<NodeType> type, Node node) {
        var results = new ArrayList<NodeType>();
        addDescendantsOfType(type, node, results);
        return results;
    }

    @SuppressWarnings("unchecked")
    private static <NodeType extends Node> void addDescendantsOfType(
        Class<NodeType> type,
        Node node,
        List<NodeType> results
    ) {
        if (type.isInstance(node)) {
            results.add((NodeType) node);
        }
        for (var child : node.getChildNodes()) {
            addDescendantsOfType(type, child, results);
        }
    }

    /**
     * Add parentheses as necessary within the given AST to ensure that the emitted code respects
     * the actual structure of the tree.
     * <p>
     * (Somewhat surprising JavaParser doesn't already provide this!)
     * <p>
     * WARNING: Does not properly account for associativity. Add explicit parens to distinguish
     *          e.g. (a + b) + c from a + (b + c).
     */
    public static <NodeType extends Node> NodeType withParensAsNeeded(NodeType expr) {
        @SuppressWarnings("unchecked")
        NodeType copy = (NodeType) expr.clone();
        insertParensAsNeeded(copy);
        return copy;
    }

    private static void insertParensAsNeeded(Node node) {
        new ArrayList<>(node.getChildNodes())
            .forEach(AstUtils::insertParensAsNeeded);

        if (node instanceof Expression expr
            && node.getParentNode().orElse(null) instanceof Expression parentExpr
            && getPrecedence(parentExpr, false) > getPrecedence(expr, true)
        ) {
            parentExpr.replace(expr, new EnclosedExpr(expr));
        }
    }

    private static int getPrecedence(Expression expr, boolean asChild) {
        // Precedences taken from https://introcs.cs.princeton.edu/java/11precedence/
        if (expr instanceof UnaryExpr unary) {
            return switch(unary.getOperator()) {
                case POSTFIX_INCREMENT, POSTFIX_DECREMENT -> 15;
                default -> 14;
            };
        } else if (expr instanceof CastExpr || expr instanceof ObjectCreationExpr) {
            return 13;
        } else if (expr instanceof BinaryExpr binary) {
            return switch(binary.getOperator()) {
                case MULTIPLY, DIVIDE, REMAINDER -> 12;
                case PLUS, MINUS -> 11;
                case LEFT_SHIFT, SIGNED_RIGHT_SHIFT, UNSIGNED_RIGHT_SHIFT -> 10;
                case LESS, GREATER, LESS_EQUALS, GREATER_EQUALS -> 9;
                case EQUALS, NOT_EQUALS -> 8;
                case BINARY_AND -> 7;
                case XOR -> 6;
                case BINARY_OR -> 5;
                case AND -> 4;
                case OR -> 3;
            };
        } else if (expr instanceof InstanceOfExpr) {
            return 9;
        } else if (expr instanceof AssignExpr) {
            return 1;
        } else if (expr instanceof ConditionalExpr) {
            return 2;
        } else {  // explicit parens, method calls, etc. should never force parens above or below
            return asChild ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        }
    }

    /**
     * Returns the logical negation of the given boolean expression.
     */
    public static Expression negated(Expression node) {
        if (node instanceof UnaryExpr expr) {
            if (expr.getOperator() == LOGICAL_COMPLEMENT) {
                return expr.getExpression();
            } else {
                return expr;
            }
        } else if (node instanceof BinaryExpr expr) {
            return switch(expr.getOperator()) {
                case AND -> deMorgan(expr, OR);
                case OR -> deMorgan(expr, AND);

                case EQUALS -> replaceOperator(expr, NOT_EQUALS);
                case NOT_EQUALS -> replaceOperator(expr, EQUALS);
                case GREATER -> replaceOperator(expr, LESS_EQUALS);
                case GREATER_EQUALS -> replaceOperator(expr, LESS);
                case LESS -> replaceOperator(expr, GREATER_EQUALS);
                case LESS_EQUALS -> replaceOperator(expr, GREATER);

                default -> new UnaryExpr(new EnclosedExpr(expr), LOGICAL_COMPLEMENT);
            };
        } else if (node instanceof BooleanLiteralExpr literal) {
            return new BooleanLiteralExpr(!literal.getValue());
        } else {
            return new UnaryExpr(node, LOGICAL_COMPLEMENT);
        }
    }

    private static Expression deMorgan(BinaryExpr expr, BinaryExpr.Operator operator) {
        return new BinaryExpr(negated(expr.getLeft()), negated(expr.getRight()), operator);
    }

    private static Expression replaceOperator(BinaryExpr expr, BinaryExpr.Operator operator) {
        return new BinaryExpr(expr.getLeft(), expr.getRight(), operator);
    }

    /**
     * Reorders the member variables and methods of the given declaration to appear in standard
     * Java order: variables, then methods, then constructors, static before instance.
     */
    public static void orderMembersByJavaConventions(ClassOrInterfaceDeclaration classDecl) {
        var sortedMembers = classDecl.getMembers().stream()
            .sorted(Comparator.comparing(
                m -> {
                    if (m.isFieldDeclaration()) {
                        var field = m.asFieldDeclaration();
                        if (field.isStatic()) {
                            if (field.isPublic() && field.isFinal()) {
                                return 0;  // static constants
                            } else {
                                return 1;  // other static vars
                            }
                        } else {
                            return 2;  // instance vars
                        }
                    } else if (m.isConstructorDeclaration()) {
                        return 3;
                    } else {
                        return 4;
                    }
                }
            ))
            .toList();

        classDecl.setMembers(new NodeList<>(sortedMembers));
    }
}
