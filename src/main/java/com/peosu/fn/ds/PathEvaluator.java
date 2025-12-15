package com.peosu.fn.ds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Evaluates and parses JSON paths into components.
 */
final class PathEvaluator {
    private static final PathEvaluator INSTANCE = new PathEvaluator();

    private PathEvaluator() {}

    /**
     * Returns the singleton instance of PathEvaluator.
     *
     * @return the PathEvaluator instance
     */
    public static PathEvaluator getInstance() {
        return INSTANCE;
    }

    /**
     * Evaluates the given JSON path and returns a list of its components.
     *
     * @param jsonPath the JSON path to evaluate (e.g., "$.store.book[0].title")
     * @return a list of path components (e.g., ["store", "book[0]", "title"])
     */
    public List<String> evaluatePath(String jsonPath) {
        List<String> paths = new ArrayList<>();
        Matcher parts = PathType.PATH_POSSIBILITIES.getPattern().matcher(jsonPath.replaceAll("^[$.]+", ""));
        while (parts.find()) {
            for (int i = 1, len = parts.groupCount(); i <= len; i++) {
                String x = parts.group(i);
                if (x == null || x.isEmpty()) continue;
                paths.add(x);
            }
        }
        return paths;
    }

    /**
     * Determines the type of the given JSON path.
     *
     * @param jsonPath the JSON path to analyze
     * @return the matching PathType, or PathType.NONE if no match is found
     */
    public PathType getMatching(String jsonPath) {
        PathType type = FnList.from(Arrays.asList(PathType.REGULAR_PATH, PathType.GLOBED_PATH,
                        PathType.PATH_EXPRESSION, PathType.WILDCARD, PathType.ARRAY))
                .filter(it -> it.getPattern().matcher(jsonPath).matches())
                .first();
        return type != null ? type : PathType.NONE;
    }
}