package edu.macalester.conceptual.puzzles.relationships;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.utils.Utils;

import edu.macalester.conceptual.util.AstUtils;
import edu.macalester.conceptual.util.CodeFormatting;
import edu.macalester.conceptual.util.Nonsense;

public interface Relationship {
    void buildDeclaration(ClassOrInterfaceDeclaration decl);

    void buildCode(TraversalChainBuilder builder);

    default String buildDecription(String desc) {
        // Most descriptions are implicit: we make sure there is only way to get from one type to
        // another in our little class graph, and thus “get to <endpoint>” is unambiguous except
        // in the “has many” case.
        return desc;
    }

    class IsA implements Relationship {
        private final Type supertype;

        public IsA(Type supertype) {
            this.supertype = supertype;
        }

        @Override
        public void buildDeclaration(ClassOrInterfaceDeclaration decl) {
            decl.addExtendedType(supertype.getName());
        }

        @Override
        public void buildCode(TraversalChainBuilder builder) {
            // no code needed to traverse inheritance relationship
        }
    }

    class HasA implements Relationship {
        private final String propertyName;
        private final Type propertyType;

        public HasA(String propertyName, Type propertyType) {
            this.propertyName = propertyName;
            this.propertyType = propertyType;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public Type getPropertyType() {
            return propertyType;
        }

        private String getMethodName() {
            return "get" + Utils.capitalize(propertyName);
        }

        @Override
        public void buildDeclaration(ClassOrInterfaceDeclaration decl) {
            buildMethod(decl, getMethodName(), propertyType.getName());
        }

        @Override
        public void buildCode(TraversalChainBuilder builder) {
            builder.replaceExpression(expr ->
                new MethodCallExpr(expr, getMethodName()));
        }
    }

    class HasMany implements Relationship {
        private final String singularElementName;
        private final Type propertyType;
        private final boolean forEach;

        public HasMany(String singularElementName, Type propertyType, boolean forEach) {
            this.singularElementName = singularElementName;
            this.propertyType = propertyType;
            this.forEach = forEach;
        }

        private String getMethodName() {
            return "get" + Utils.capitalize(Nonsense.pluralize(singularElementName));
        }

        @Override
        public void buildDeclaration(ClassOrInterfaceDeclaration decl) {
            buildMethod(decl, getMethodName(), "List<" + propertyType.getName() + ">");
        }

        @Override
        public String buildDecription(String desc) {
            return (forEach ? "each " : "the first ")
                + singularElementName
                + " of " + desc;
        }

        @Override
        public void buildCode(TraversalChainBuilder builder) {
            if (forEach) {
                buildForEach(builder);
            } else {
                buildGetFirst(builder);
            }
        }

        private void buildGetFirst(TraversalChainBuilder builder) {
            builder.replaceExpression(curExpr ->
                new MethodCallExpr(
                    new MethodCallExpr(
                        curExpr,
                        getMethodName()),
                    "get",
                    AstUtils.nodes(
                        new IntegerLiteralExpr("0"))));
        }

        private void buildForEach(TraversalChainBuilder builder) {
            var loopVarExpr = new NameExpr(singularElementName);
            builder.wrapCurrentStatement(loopVarExpr, (curExpr, newStmt) ->
                new ForEachStmt(
                    // For each...
                    new VariableDeclarationExpr(
                        propertyType.referenceAst(),
                        loopVarExpr.getNameAsString()),

                    // ...in...
                    new MethodCallExpr(
                        curExpr,
                        getMethodName()),

                    // ..do:
                    AstUtils.blockOf(
                        newStmt)));
        }
    }

    private static void buildMethod(ClassOrInterfaceDeclaration decl, String methodName, String returnType) {
        var method = decl.addMethod(methodName, Modifier.Keyword.PUBLIC);
        method.setType(returnType);
        method.setBody(CodeFormatting.elidedMethodBody());
    }
}
