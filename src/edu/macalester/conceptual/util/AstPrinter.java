package edu.macalester.conceptual.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;

/**
 * Dumps javaparser’s AST in a human-friendly form.
 * Use this class via the <code>bin/astprinter</code> script.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class AstPrinter {

    private final int tabSize;

    public static void main(String[] args) throws Exception {
        if (args.length != 1 && args.length != 2) {
            System.err.println(
                """
                usage: astprinter <parse-unit> [<file>]
                
                Examples:
                    echo '3 + foo.bar(17 * 2)' | bin/astprinter expr
                    echo '{ int x = 3; return x; }' | bin/astprinter stmt
                    echo 'int foo(String bar) { return 0; }' | bin/astprinter method
                    echo 'public interface Foo { void bar(); }' | bin/astprinter class
                    bin/astprinter file src/edu/macalester/conceptual/Puzzle.java
                """);
            System.exit(0);
        }
        Function<String,Node> parser = switch (args[0]) {
            case "expr" -> StaticJavaParser::parseExpression;
            case "stmt" -> StaticJavaParser::parseStatement;
            case "method" -> StaticJavaParser::parseMethodDeclaration;
            case "class" -> StaticJavaParser::parseTypeDeclaration;
            case "file" -> StaticJavaParser::parse;
            default -> {
                System.err.println();
                System.err.println("Unknown parse-unit: " + args[0]);
                System.err.println("Must be one of: expr stmt method class file");
                System.err.println();
                System.exit(0);
                yield null;
            }
        };

        Reader input;
        if (args.length == 2) {
            input = new FileReader(args[1]);
        } else {
            input = new InputStreamReader(System.in, StandardCharsets.UTF_8);
        }

        new AstPrinter(3).dump(
            parser.apply(
                new BufferedReader(input)
                    .lines()
                    .collect(Collectors.joining(System.lineSeparator()))));
    }

    public AstPrinter(int tabSize) {
        this.tabSize = tabSize;
    }

    /**
     * Getter methods on various nodes that clutter the output.
     */
    private static final Set<String> ignoredNodeAttrs;
    static {
        Set<String> attrs = new HashSet<>();
        attrs.addAll(
            Arrays.stream(Node.class.getMethods())
                .map(Method::getName)
                .collect(Collectors.toSet()));
        attrs.addAll(Arrays.asList(
            "getComments", "getJavadoc", "getJavadocComment", "getSignature", "getId",
            "getDeclarationAsString"
        ));
        ignoredNodeAttrs = attrs;
    }

    /**
     * Prints the AST for the given node.
     */
    public void dump(Node node) {
        dump(null, node, 0, true);
    }

    /**
     * Prints the AST for the given node indented by the given number of spaces.
     *
     * @param descend If false, show node but do not show children.
     */
    public void dump(String label, Node node, int indentation, boolean descend) {
        String prefix = (label == null) ? "" : label + ": ";
        printIndented(
            indentation,
            prefix
                + describeSimpleAttrs(node)
                + (!descend && !node.getChildNodes().isEmpty() ? " ..." : ""));

        if (!descend) {
            return;
        }

        Set<Node> toVisit = new HashSet<>(node.getChildNodes());

        forEachNodeAttribute(node, (childLabel, value) -> {
            if (value instanceof Node child) {
                dump(childLabel, child, indentation + tabSize, toVisit.contains(child));
            }

            if (value instanceof NodeList list) {
                printIndented(indentation + tabSize, childLabel + ": NodeList (" + list.size() + ")");
                for (Node child : (Collection<Node>) list) {
                    dump(null, child, indentation + tabSize * 2, toVisit.contains(child));
                }
            }
        });
    }

    private String describeSimpleAttrs(Node node) {
        StringBuilder desc = new StringBuilder(unqualifiedName(node.getClass()));
        desc.append(" {");
        AtomicBoolean first = new AtomicBoolean(true);

        forEachNodeAttribute(node, (name, value) -> {
            if(value == null || value instanceof Node || value instanceof NodeList)
                return;

            if(value instanceof Collection collection) {
                boolean emptyOrAllNodes = true;
                for(Object elem : collection)
                    if(!(elem instanceof Node))
                        emptyOrAllNodes = false;
                if(emptyOrAllNodes)
                    return;
            }

            if(name.equals("arrayLevel") && value.equals(0))
                return;

            if(value instanceof String)
                value = "\"" + value + "\"";

            if (first.get()) {
                first.set(false);
            } else {
                desc.append(", ");
            }
            desc.append(name)
                .append("=")
                .append(value);
        });
        desc.append("}");
        return desc.toString();
    }

    private void forEachNodeAttribute(Node node, BiConsumer<String, Object> visitor) {
        for(Method method : node.getClass().getMethods()) {
            if(method.getName().startsWith("get")  // getters only
                && method.getParameterCount() == 0
                && !ignoredNodeAttrs.contains(method.getName())
            ) {
                String name = uncapitalize(method.getName().replaceFirst("get", ""));
                Object value;
                try {
                    value = method.invoke(node);
                } catch(Exception e) {
                    continue;  // ignore anything that fails
                }

                while(value instanceof Optional) {
                    //noinspection rawtypes
                    value = ((Optional) value).orElse(null);
                }

                visitor.accept(name, value);
            }
        }
    }

    private static void printIndented(int indentation, Object obj) {
        for(int n = 0; n < indentation; n++)
            System.out.print(" ");
        System.out.println(obj);
    }

    // Strips the package name
    private static String unqualifiedName(Class aClass) {
        String[] parts = aClass.getName().split("\\.");
        return parts[parts.length - 1];
    }

    private static String uncapitalize(String s) {
        if(s == null || s.isEmpty())
            return s;
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }
}
