package edu.macalester.conceptual.context;

import com.github.javaparser.ast.Node;

import java.io.Closeable;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import edu.macalester.conceptual.util.CodeFormatting;
import edu.macalester.graphics.GraphicsObject;

import static edu.macalester.conceptual.util.CodeFormatting.*;

/**
 * Provides output facilities for puzzle generators. Puzzles use <b>structured output,</b>
 * meaning that all output methods describe the <i>role</i> of the output — heading, paragraph,
 * code block — and there is no directly accessible <code>println()</code> method.
 * <p>
 * Several output methods in this class support <b>textual formatting</b> with the following
 * features:
 * <ul>
 *   <li>Line breaks in the text are ignored, which means that you can use a Java multiline
 *       string in your code with code-appropriate line breaks, and the PuzzlePrinter will reflow
 *       your text to fit the console. To output blank lines, use multiple paragraph() calls
 *       (or other adjoining lists, code blocks, etc).</li>
 *   <li>Text enclosed in backticks is visually styled as `code`. This is appropriate for English
 *       text mentioning variable names, short expressions, etc. For whole chunks of code, however,
 *       you should use {@link #codeBlock(String)}.</li>
 *   <li>Asterisks make text *bold*.</li>
 *   <li>Underlines make text _italic_.</li>
 * </ul>
 */
public interface PuzzlePrinter extends Closeable {
    /**
     * Optionally prints the title of the whole puzzle. This is a noop for some PuzzlePrinters.
     */
    void title(String title);

    /**
     * Prints a horizontal divider line to visually separate items.
     *
     * @param primary Determines whether the line is solid / prominent (true) or thin /
     *                light / dashed (false).
     */
    void dividerLine(boolean primary);

    /**
     * Prints the given text in a visually prominent way.
     *
     * @param primary Determines whether the heading is more (true) or less (false) prominent.
     */
    void heading(String text, boolean primary);

    /**
     * Emits a paragraph of text, with word wrapping and appropriate inter-paragraph spacing.
     * This method uses <b>textual formatting</b>; see the docs at the top of this class for
     * formatting options.
     * <p>
     * Unlike the other textual methods, this method’s format string and its arguments use
     * {@link MessageFormat}. For example:
     * <p>
     * <code>paragraph("{0} before {1}", "dessert", "dinner")</code>
     * <p>
     * This means that you <b>must escape any single quotes and curly braces</b> that appear in the
     * paragraph:
     * <p>
     * <code>paragraph("This isn''t a square brace: '{'")</code>
     * <p>
     * For apostrophes, it is better to use the curved Unicode apostrophe character than to use a
     * single quote:
     * <p>
     * <code>paragraph("This doesn’t require escaping")</code>
     */
    void paragraph(String formatString, Object... formatArguments);

    /**
     * Formats the given arguments in a list, each one with its own bullet, with smart word wrapping
     * and appropriate inter-paragraph spacing.
     * <p>
     * This method uses <b>textual formatting</b>; see the docs at the top of this class for
     * formatting options.
     */
    void bulletList(String... items);

    /**
     * Formats the given arguments in a compact list (no blank lines between items), with each item
     * numbered starting from 1, and with word wrapping and appropriate inter-paragraph spacing.
     * <p>
     * This method uses <b>textual formatting</b>; see the docs at the top of this class for
     * formatting options.
     */
    void numberedList(String... items);

    /**
     * Formats the given arguments in a widely spaced numbered list, with each item able to
     * accommodate multiple paragraphs, code blocks, nested lists, etc.
     * <p>
     * This method uses <b>textual formatting</b>; see the docs at the top of this class for
     * formatting options.
     */
    default void numberedList(Runnable... items) {
        numberedList(Arrays.asList(items));
    }

    void numberedList(List<Runnable> items);

    default void numberedList(Stream<Runnable> items) {
        numberedList(items.toList());
    }

    /**
     * Prints the given text in an indented block, with smart word wrapping and appropriate
     * inter-paragraph spacing.
     * <p>
     * This method uses <b>textual formatting</b>; see the docs at the top of this class for
     * formatting options.
     */
    void blockquote(String s);

    /**
     * Prints the given JavaParser AST as an indented and well-formatted block of code, adding
     * parentheses as necessary to preserve expression tree structures within the AST.
     */
    default void codeBlock(Node astNode) {
        codeBlock(prettify(astNode));
    }

    /**
     * Prints the given string as an indented block of code, <b>as is</b>, neither prettified nor
     * with text formatting. To indent and format code properly, call one of the
     * <code>prettify*</code> methods in {@link CodeFormatting} first.
     */
    void codeBlock(String javaCode);

    /**
     * Displays the given graphics to the user, either embedded in the text or in a separate window.
     */
    void showGraphics(String title, GraphicsObject graphics);

    /**
     * Suppresses further output until balanced by a call to {@link #unsilence()}. Calls to
     * silence/unsilence are nestable.
     */
    void silence();

    void unsilence();

    boolean isSilenced();

    /**
     * Returns the color scheme for the current section. Useful for coordinating graphics with
     * the puzzle text.
     */
    float themeHue();

    void setThemeHue(float hue);
}
