package edu.macalester.conceptual.context;

/**
 * Parses the mini-markdown-style delimiters within a paragraph for `code`, *bold*, and _italics_.
 */
record TextFormatter(
    Style code,
    Style bold,
    Style italics
) {
    public String format(String text) {
        return text.strip()
            .replaceAll("\\s+", " ")  // strip internal line breaks; callers should use paragraph()
            .replaceAll(inlineDelimited("`"),    code.wrap("$1"))  // `code`
            .replaceAll(inlineDelimited("\\*"),  bold.wrap("$1"))  // *bold*
            .replaceAll(inlineDelimited("_"), italics.wrap("$1")); // _italics_
    }

    private static String inlineDelimited(String delimiter) {
        return
            delimiter     // the delimiter
            + "(?!\\s)"   // not follow by a space,
            + "(.+?)"     // then the shortest section of matching text
            + "(?<!\\s)"  // not ending with a space
            + delimiter;  // and terminated with the delimiter
    }

    public record Style(
        String prefix,
        String suffix
    ) {
        String wrap(String s) {
            return prefix + "$1" + suffix;
        }
    }
}
