
package com.peosu.fn.ds.interfaces;

/**
 * Represents a supplier of results.
 * This is a functional interface whose functional method is {@link #produce()}.
 *
 * @param <T> the type of results supplied by this producer
 */
@FunctionalInterface
public interface Producer<T> {

    /**
     * Gets a result.
     *
     * @return a result
     * @throws Exception if unable to produce a result
     */
    T produce() throws Exception;
}
