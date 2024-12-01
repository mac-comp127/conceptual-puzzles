package edu.macalester.conceptual.util;

public record CodeSnippet<T> (
    String imports,
    String mainBody,
    Class<T> returnType,
    String classMembers,
    String otherClasses
) {
    public static CodeSnippet<Void> build() {
        return new CodeSnippet<>("", "", Void.class, "", "");
    }

    public CodeSnippet<T> withImports(String imports) {
        return new CodeSnippet<>(imports, mainBody, returnType, classMembers, otherClasses);
    }

    public CodeSnippet<T> withMainBody(String mainBody) {
        return new CodeSnippet<>(imports, mainBody, returnType, classMembers, otherClasses);
    }

    public <U> CodeSnippet<U> withReturnType(Class<U> returnType) {
        return new CodeSnippet<>(imports, mainBody, returnType, classMembers, otherClasses);
    }

    public CodeSnippet<T> withClassMembers(String classMembers) {
        return new CodeSnippet<>(imports, mainBody, returnType, classMembers, otherClasses);
    }

    public CodeSnippet<T> withOtherClasses(String otherClasses) {
        return new CodeSnippet<>(imports, mainBody, returnType, classMembers, otherClasses);
    }

    public String generateCode(String className) {
        return String.format(
            """
            %1$s

            public class %6$s implements java.util.function.Supplier<%4$s> {
                %2$s
                public %4$s get() {
                    %3$s
                }
            }
            
            %5$s
            """,
            imports,
            classMembers,
            mainBody,
            returnType.getName(),
            otherClasses.replaceAll("public\\s+(class|interface|enum|record)", "$1"),
            className
        );
    }
}
