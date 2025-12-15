
package com.peosu.fn.ds.interfaces;

/**
 * Represents an operation that combines an accumulator value and an input value into a new accumulator value.
 * This is a functional interface whose functional method is {@link #combine(Object, Object)}.
 *
 * @param <A> the type of the accumulator
 * @param <T> the type of the input value
 */
@FunctionalInterface
public interface Accumulator<A, T> {

    /**
     * Combines the given accumulator value and input value into a new accumulator value.
     *
     * @param value1 the accumulator value
     * @param value2 the input value
     * @return the new accumulator value
     * @throws Exception if unable to combine the values
     */
    A combine(A value1, T value2) throws Exception;
}
