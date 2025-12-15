
package com.peosu.fn.ds.interfaces;

/**
 * Represents an operation that accepts a single input argument and performs an action.
 * This is a functional interface whose functional method is {@link #apply(Object)}.
 *
 * @param <T> the type of the input to the operation
 */
@FunctionalInterface
public interface Action<T> {

    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     * @throws Exception if unable to perform the action
     */
    void apply(T t) throws Exception;
}
