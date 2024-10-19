package edu.macalester.conceptual.context;

import java.util.regex.Pattern;

/**
 * Parses the mini-markdown-style delimiters within a paragraph for `code`, *bold*, and _italics_.
 */
record TextFormatter(
    Style code,
    Style bold,
    Style italics,
    Style placeholder
) {
    private static final Pattern
        CODE_DELIM        = inlineDelimited("`"),  
        BOLD_DELIM        = inlineDelimited("\\*"),
        ITALICS_DELIM     = inlineDelimited("_"),
        PLACEHOLDER_DELIM = inlineDelimited("___", "(?:/\\*)?", "(?:\\*/)?");

    private static Pattern inlineDelimited(String delimiter) {
        return inlineDelimited(delimiter, "", "");
    }

    private static Pattern inlineDelimited(String delimiter, String prefix, String suffix) {
        return Pattern.compile(
            prefix        // The prefix-only delimiter (usually blank) and
            + delimiter   // the delimiter
            + "(?!\\s)"   // not follow by a space,
            + "(.+?)"     // then the shortest section of matching text
            + "(?<!\\s)"  // not ending with a space
            + delimiter   // and terminated with the delimiter
            + suffix      // and the suffix (also usually blank)
        );
    }

    public String format(String text) {
        // strip internal line breaks; callers should use paragraph()
        text = text.strip().replaceAll("\\s+", " ");

        text = formatCodePlaceholders(text);

        text = CODE_DELIM.matcher(text).replaceAll((match) ->
            code.wrap(match.group(1)));

        text = BOLD_DELIM .matcher(text).replaceAll((match) ->
            bold.wrap(match.group(1)));

        text = ITALICS_DELIM.matcher(text).replaceAll((match) ->
            italics.wrap(match.group(1)));

        return text;
    }

    public String formatCodePlaceholders(String code) {
        return PLACEHOLDER_DELIM.matcher(code).replaceAll((match) ->
            placeholder.wrap(match.group(1)));
    }

    public record Style(
        String prefix,
        String suffix
    ) {
        String wrap(String s) {
            return prefix + s + suffix;
        }
    }
}
