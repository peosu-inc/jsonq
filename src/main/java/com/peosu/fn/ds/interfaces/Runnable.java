
package com.peosu.fn.ds.interfaces;

/**
 * Represents a command that can be executed.
 * This is a functional interface whose functional method is {@link #run()}.
 */
@FunctionalInterface
public interface Runnable {

    /**
     * Executes the command.
     *
     * @throws Exception if unable to execute the command
     */
    void run() throws Exception;
}

