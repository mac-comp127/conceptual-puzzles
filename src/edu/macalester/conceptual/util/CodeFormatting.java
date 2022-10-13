package edu.macalester.conceptual.util;

public enum CodeFormatting {
    ; // static utility class; no cases

    public static String joinCode(String... parts) {
        return String.join(" ", parts);
    }

    public static String joinStatements(String... parts) {
        return String.join(";", parts) + ";";
    }
}
