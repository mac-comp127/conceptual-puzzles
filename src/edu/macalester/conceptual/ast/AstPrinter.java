package edu.macalester.conceptual.ast;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;

import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Dumps javaparser’s AST in a human-friendly form.
 *
 * The main function parses and dumps Java source files given as command-line args.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class AstPrinter {

    private final int tabSize;

    public static void main(String[] args) throws Exception {
        AstPrinter astPrinter = new AstPrinter(3);
        for(String file : args) {
            System.out.println();
            System.out.println("––––––––––––––––––––––––––––––––––");
            System.out.println(file);
            System.out.println("––––––––––––––––––––––––––––––––––");
            System.out.println();
            System.out.flush();
            CompilationUnit cu = StaticJavaParser.parse(new FileInputStream(file));
            astPrinter.dump(null, cu, 0, true);
        }
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

    public void dump(Node node) {
        dump(null, node, 0, true);
    }

    /**
     * Recursively dumps the AST indented by the given number of spaces.
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
