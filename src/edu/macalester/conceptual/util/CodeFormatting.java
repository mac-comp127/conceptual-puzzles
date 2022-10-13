package edu.macalester.conceptual.util;

import java.util.Arrays;
import java.util.Objects;

public enum CodeFormatting {
    ; // static utility class; no cases

    public static String joinCode(String... parts) {
        return String.join(" ", removeNulls(parts));
    }

    public static String joinStatements(String... parts) {
        return String.join(";", removeNulls(parts)) + ";";
    }

    private static Iterable<String> removeNulls(String[] parts) {
        return Arrays.stream(parts)
            .filter(Objects::nonNull)
            .toList();
    }
}
