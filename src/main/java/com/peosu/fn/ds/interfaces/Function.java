
package com.peosu.fn.ds.interfaces;

/**
 * Represents a function that accepts one argument and produces a result.
 * This is a functional interface whose functional method is {@link #invoke(Object)}.
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 */
@FunctionalInterface
public interface Function<T, R> {

    /**
     * Applies this function to the given argument.
     *
     * @param t the input argument
     * @return the function result
     * @throws Exception if unable to compute the result
     */
    R invoke(T t) throws Exception;
}
