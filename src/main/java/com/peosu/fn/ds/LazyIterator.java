package com.peosu.fn.ds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.peosu.fn.ds.interfaces.Producer;
import com.peosu.fn.ds.interfaces.Function;

import static com.peosu.fn.utils.Log.log;

/**
 * An iterator that lazily applies a series of operations to items produced by a data provider.
 *
 * @param <T> the type of items produced and processed
 */
public class LazyIterator<T> implements Iterator<T> {
    private final Producer<T> dataProvider;
    private final List<Function<T, T>> operations;
    private T nextItem;

    /**
     * Constructs a new LazyIterator with the given data provider and operations.
     *
     * @param dataProvider the producer of data items
     * @param operations the list of operations to apply to each item
     */
    public LazyIterator(Producer<T> dataProvider, List<Function<T, T>> operations) {
        this.dataProvider = dataProvider;
        this.operations = new ArrayList<>(operations);
    }

    /**
     * Constructs a new LazyIterator with the given data provider and an empty list of operations.
     *
     * @param dataProvider the producer of data items
     */
    public LazyIterator(Producer<T> dataProvider) {
        this.dataProvider = dataProvider;
        this.operations = new ArrayList<>();
    }

    /**
     * Creates a copy of this LazyIterator with the same data provider and operations.
     *
     * @return a new LazyIterator instance
     */
    public LazyIterator<T> copy() {
        return new LazyIterator<>(dataProvider, operations);
    }

    /**
     * Checks if there is a next item available after applying operations.
     *
     * @return true if there is a next item, false otherwise
     */
    @Override
    public boolean hasNext() {
        if (nextItem != null) { return true; }
        try {
            T data = dataProvider.produce();
            int maxTries = 0;
            data = processItem(data);
            while (maxTries++ < 10 && data == null) {
                data = processItem(dataProvider.produce());
            }
            nextItem = data;
            return nextItem != null;
        } catch (NoSuchElementException e) { return false; }
        catch (Exception e) { log(e); return false; }
    }

    /**
     * Retrieves the next item after applying all operations.
     *
     * @return the next processed item
     * @throws NoSuchElementException if there are no more items
     */
    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        T current = nextItem;
        nextItem = null;
        return current;
    }

    /**
     * Applies all operations to the given item.
     *
     * @param item the item to process
     * @return the processed item, or null if any operation returns null
     */
    public T processItem(T item) {
        T processed = item;
        for (Function<T, T> operation : operations) {
            T copy = processed;
            if (copy == null) return null;
            processed = ex(() -> operation.invoke(copy));
        }
        return processed;
    }

    /**
     * Adds an operation to the list of operations to apply to each item.
     *
     * @param operation a function to apply to each item
     */
    public void addOperation(Function<T, T> operation) {
        operations.add(operation);
    }

    // Note: 'ex' method is private and utility-focused, so Javadoc is omitted for brevity.
    private  static <T> T ex(Producer<T> producer){
        try { return producer.produce();}
        catch (Exception e) {log(e);}
        return null;
    }
}