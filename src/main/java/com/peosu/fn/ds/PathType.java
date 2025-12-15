package com.peosu.fn.ds;

import java.util.regex.Pattern;

/**
 * Enumerates different types of JSON paths with their corresponding regular expression patterns.
 */
public enum PathType {
    /** Represents an integer path component (e.g., "123"). */
    INTEGER(Pattern.compile("^\\d+$")),

    /** Represents a regular path component (e.g., "store.book.title"). */
    REGULAR_PATH(Pattern.compile("(?:\\w+|\"[^\"]+\")(?:\\.(?:\\w+|\"[^\"]+\"))*")),

    /** Represents an array access (e.g., "[0]", "[*]", "[1:3]"). */
    ARRAY(Pattern.compile("\\[(?:(\\??\\(.+\\))|(-?\\d*:?-?\\d*(?:,-?\\d*:?-?\\d*)*)|(\\*)|(([`\"'])(.+?)\\5))]")),

    /** Represents a globed path with wildcards (e.g., "store.*.title"). */
    GLOBED_PATH(Pattern.compile("(?=.*\\*)(?=.*\\w)[^.\\[\\]()?'\"]*")),

    /** Represents a wildcard path (e.g., "..store" or "..."). */
    WILDCARD(Pattern.compile("\\.{2,3}(?:" + "(?:\\w+|\"[^\"]+\")(?:\\.(?:\\w+|\"[^\"]+\"))*" + ")?")),

    /** Represents a path with an expression (e.g., "[?(...)]"). */
    PATH_EXPRESSION(Pattern.compile(".*\\[?\\??\\(.*\\)]?")),

    /** Represents possible path components for parsing. */
    PATH_POSSIBILITIES(Pattern.compile("(" + "(?=.*\\*)(?=.*\\w)[^.\\[\\]()?'\"]*" + ")?(" +
            "(?:\\w+|\"[^\"]+\")(?:\\.(?:\\w+|\"[^\"]+\"))*" + ")?(" +
            "\\.{2,3}(?:" + "(?:\\w+|\"[^\"]+\")(?:\\.(?:\\w+|\"[^\"]+\"))*" + ")?" + ")?(\\[[^]]+])?")),

    /** Represents a JSON variable (e.g., "@.property"). */
    JSON_VARIABLE(Pattern.compile("@\\.(\\w+)")),

    /** Represents a true value (e.g., "yes", "true"). */
    VALUED_TRUE(Pattern.compile("(?i)yes|ndio|ndiyo|true")),

    /** Represents a false value (e.g., "no", "false"). */
    VALUED_FALSE(Pattern.compile("(?i)hapana|no|false")),

    /** Represents no match (e.g., "null"). */
    NONE(Pattern.compile("null"));

    private final Pattern pattern;

    PathType(Pattern pattern) { this.pattern = pattern; }

    /**
     * Returns the regular expression pattern associated with this path type.
     *
     * @return the pattern
     */
    public Pattern getPattern() { return pattern; }
}