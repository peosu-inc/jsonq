package com.peosu.fn.ds;

/**
 * A mutable wrapper for a value of any type.
 *
 * @param <V> the type of the value
 */
public class Mutable<V> {
    public V value;

    /**
     * Constructs a new Mutable instance with the given initial value.
     *
     * @param value the initial value to wrap
     */
    public Mutable(V value) {
        this.value = value;
    }
}