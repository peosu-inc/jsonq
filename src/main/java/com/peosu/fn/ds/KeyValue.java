package com.peosu.fn.ds;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a key-value pair with generic types for the key and value.
 *
 * @param <K> the type of the key
 * @param <V> the type of the value
 */
public class KeyValue<K, V> {
    public K key;
    public V value;
    private static final Pattern PATTERN_KEY_VALUE = Pattern.compile("^\\W*([a-z]\\w+)\\W+(\\w.*)");

    /**
     * Constructs a new KeyValue instance with the specified key and value.
     *
     * @param key the key
     * @param value the value
     */
    public KeyValue(K key, V value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Creates a KeyValue instance from the given input string.
     * <p>
     * The input string should be in the format "key value", where key starts with a letter
     * and is followed by word characters, and value is any sequence of word characters.
     * </p>
     *
     * @param input the input string to parse
     * @return a KeyValue instance with the parsed key and value, or an empty pair if parsing fails
     */
    public static KeyValue<String, String> create(String input) {
        Matcher m = PATTERN_KEY_VALUE.matcher(input != null ? input : "");
        return m.find()
                ? new KeyValue<>(m.group(1), m.group(2))
                : new KeyValue<>("", "");
    }
}