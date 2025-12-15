package com.peosu.fn.ds;

import com.peosu.fn.ds.interfaces.Accumulator;
import com.peosu.fn.ds.interfaces.Action;
import com.peosu.fn.ds.interfaces.Function;
import com.peosu.fn.ds.interfaces.Producer;
import com.peosu.fn.ds.interfaces.Predicate;
import com.peosu.fn.ds.interfaces.Runnable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import static com.peosu.fn.utils.Log.log;

/**
 * Class FnList provides functional operations on a list of items.
 * <p>
 * WARNING: This class is designed to swallow both nulls and exceptions occurring during its operations.
 * Any exceptions thrown during operations such as map, filter, etc. will be caught and logged,
 * and will not propagate to the calling code.
 * <p>
 * This behavior ensures smooth operation of the pipeline but can potentially hide issues
 * or unexpected behaviors. When debugging or troubleshooting, be sure to check the logs
 * for any relevant error messages from this class.
 * <p>
 * It is important to be aware of this when using this class, especially if operations have
 * side effects or if there's a need to be notified of failures.
 *
 * @param <T> the type of elements maintained by this FnList.
 */
@SuppressWarnings({"unchecked", "unused"})
public class FnList<T> implements Iterable<T> {

    /**
     * The underlying lazy iterator that manages the operations chain.
     */
    private final LazyIterator<T> lazy;

    /**
     * When true, exceptions are thrown instead of being logged and swallowed.
     */
    private static boolean strictMode = false;

    /**
     * Enables or disables strict mode globally.
     * In strict mode, exceptions are thrown instead of being logged and swallowed.
     *
     * @param enabled true to enable strict mode, false to disable
     */
    public static void setStrictMode(boolean enabled) {
        strictMode = enabled;
    }

    /**
     * Returns whether strict mode is currently enabled.
     *
     * @return true if strict mode is enabled
     */
    public static boolean isStrictMode() {
        return strictMode;
    }

    /**
     * Private constructor that initializes the FnList with a given LazyIterator.
     * A copy of the provided lazy iterator is made to ensure isolation.
     *
     * @param lazyIterator the LazyIterator instance to initialize the FnList.
     */
    private FnList(LazyIterator<T> lazyIterator) {
        lazy = lazyIterator.copy();
    }

    /**
     * Creates a new FnList from a Producer.
     *
     * @param producer the Producer that supplies elements.
     * @param <T>      the type of elements.
     * @return a new FnList instance.
     */
    public static <T> FnList<T> from(Producer<T> producer) {
        return new FnList<>(new LazyIterator<>(producer));
    }

    /**
     * Creates a new FnList from an array of data.
     *
     * @param data an array of elements.
     * @param <T>  the type of elements.
     * @return a new FnList instance.
     */
    public static <T> FnList<T> from(T[] data) {
        return new FnList<>(new LazyIterator<>(Arrays.asList(data).iterator()::next));
    }

    /**
     * Creates a new FnList from a Collection.
     *
     * @param collection the Collection of elements.
     * @param <T>        the type of elements.
     * @return a new FnList instance.
     */
    public static <T> FnList<T> from(Collection<T> collection) {
        return new FnList<>(new LazyIterator<>(collection.iterator()::next));
    }

    /**
     * Generates a FnList using a generator function that takes an index.
     * The generator function is repeatedly invoked with an incrementing counter until
     * it returns null, signaling the end of the sequence.
     *
     * @param generator the generator function that produces elements based on an index.
     * @param <T>       the type of elements.
     * @return a new FnList instance.
     */
    public static <T> FnList<T> generate(Function<Integer, T> generator) {
        Mutable<Integer> mutable = new Mutable<>(0);
        return FnList.from(() -> {
            try {
                T t = generator.invoke(mutable.value++);
                if (t == null) {
                    throw new NoSuchElementException();
                }
                return t;
            } catch (IndexOutOfBoundsException e) {
                throw new NoSuchElementException();
            }
        });
    }

    /**
     * Creates a FnList representing a range of integers from lower (inclusive) to upper (exclusive).
     *
     * @param lower the starting integer (inclusive).
     * @param upper the ending integer (exclusive).
     * @return a new FnList of integers in the specified range.
     */
    public static FnList<Integer> range(int lower, int upper) {
        return generate(x -> lower + x < upper ? lower + x : null);
    }

    /**
     * Creates a FnList representing a range of integers from 0 (inclusive) to upper (exclusive).
     *
     * @param upper the ending integer (exclusive).
     * @return a new FnList of integers in the range [0, upper).
     */
    public static FnList<Integer> range(int upper) {
        return FnList.range(0, upper);
    }

    /**
     * Filters the FnList based on the provided predicate.
     * Only elements for which the predicate returns true are retained.
     *
     * @param predicate the condition to filter elements.
     * @return a new FnList instance containing only the elements that satisfy the predicate.
     */
    public FnList<T> filter(Predicate<T> predicate) {
        lazy.addOperation(input -> predicate.test(input) ? input : null);
        return new FnList<>(lazy);
    }

    /**
     * Transforms each element of the FnList using the given function.
     *
     * @param function the function to apply to each element.
     * @param <S>      the type of the resulting elements.
     * @return a new FnList containing the transformed elements.
     */
    public <S> FnList<S> map(Function<T, S> function) {
        lazy.addOperation(input -> (T) function.invoke(input));
        // Cast is safe here because we're just transforming data.
        return (FnList<S>) new FnList<>(lazy);
    }

    /**
     * Applies a mapping function to each element that returns a Collection,
     * then flattens the resulting collections into a single FnList.
     *
     * @param function the function that returns a Collection of elements.
     * @param <S>      the type of the elements in the resulting collections.
     * @return a new FnList containing the flattened elements.
     */
    public <S> FnList<S> flatMap(Function<T, Collection<S>> function) {
        lazy.addOperation(input -> (T) function.invoke(input));
        return (FnList<S>) flat();
    }

    /**
     * Applies a mapping function to each element that returns an array,
     * then flattens the resulting arrays into a single FnList.
     *
     * @param function the function that returns an array of elements.
     * @param <S>      the type of the elements in the resulting arrays.
     * @return a new FnList containing the flattened elements.
     */
    public <S> FnList<S> flatMapArrays(Function<T, S[]> function) {
        lazy.addOperation(input -> (T) Arrays.asList(function.invoke(input)));
        return (FnList<S>) flat();
    }

    /**
     * Flattens nested structures within the FnList.
     * If an element is a Collection or an array, its contents are iterated and returned individually.
     *
     * @return a new FnList with flattened elements.
     */
    public FnList<Object> flat() {
        LazyIterator<T> lazyCopy = lazy.copy();
        Mutable<Iterator<?>> mutable = new Mutable<>(null);
        return FnList.generate(i -> {
            while (mutable.value == null || !mutable.value.hasNext()) {
                T nextItem = lazyCopy.next();
                if (nextItem instanceof Collection) {
                    mutable.value = ((Collection<?>) nextItem).iterator();
                } else if (nextItem instanceof Object[]) {
                    mutable.value = (Arrays.asList((T[]) nextItem)).iterator();
                } else {
                    return nextItem;
                }
            }
            return mutable.value.next();
        });
    }

    /**
     * Reduces the elements of the FnList into a single value by iteratively combining elements.
     * Any exceptions thrown during the combination are caught and logged.
     *
     * @param identity    the initial value of the reduction.
     * @param accumulator the function that combines two elements.
     * @param <A>         the type of the resulting accumulated value.
     * @return the result of the reduction.
     */
    public <A> A reduce(A identity, Accumulator<A, T> accumulator) {
        A result = identity;
        for (T item : this) {
            try {
                result = accumulator.combine(result, item);
            } catch (Exception e) {
                if (strictMode) {
                    throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
                }
                log(e);
            }
        }
        return result;
    }

    /**
     * Returns a new FnList containing only unique elements.
     * This method uses the identity of the elements for uniqueness.
     *
     * @return a new FnList with unique elements.
     */
    public FnList<T> unique() {
        return unique(x -> x);
    }

    /**
     * Returns a new FnList containing only unique elements based on a key produced by the given function.
     *
     * @param giveId the function that provides an identifier for each element.
     * @param <S>    the type of the identifier.
     * @return a new FnList with unique elements.
     */
    public <S> FnList<T> unique(Function<T, S> giveId) {
        Set<S> ids = new LinkedHashSet<>();
        ids.add(null);
        return this.filter(x -> ids.add(ex(() -> giveId.invoke(x))));
    }

    /**
     * Groups the elements of the FnList by a key produced by the provided function.
     *
     * @param giveId the function that produces a key for each element.
     * @param <S>    the type of the key.
     * @return a Map where each key is associated with a List of elements that share that key.
     */
    public <S> Map<S, List<T>> group(Function<T, S> giveId) {
        Map<S, List<T>> map = new HashMap<>();
        return this.reduce(map, (m, t) -> {
            S id = giveId.invoke(t);
            List<T> list = m.computeIfAbsent(id, k -> new ArrayList<>());
            list.add(t);
            return m;
        });
    }

    /**
     * Converts the FnList into a standard List.
     *
     * @return a List containing all elements from the FnList.
     */
    public List<T> list() {
        return reduce(new ArrayList<>(), (a, b) -> {
            a.add(b);
            return a;
        });
    }

    /**
     * Converts the FnList into a Set containing unique elements.
     *
     * @return a Set with unique elements from the FnList.
     */
    public Set<T> toSet() {
        return new LinkedHashSet<>(unique().list());
    }

    /**
     * Returns the string representation of the FnList.
     *
     * @return a String representation of the FnList.
     */
    public String toString() {
        return list().toString();
    }

    /**
     * Converts the FnList into a pretty-printed JSON string.
     *
     * @return a JSON string representing the FnList.
     */
    public String toJson() {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create();
        return gson.toJson(list());
    }

    /**
     * Iterates over each element in the FnList and applies the given action.
     * Any exceptions thrown during the action are caught and logged.
     *
     * @param action the action to be applied to each element.
     */
    public void forEachItem(Action<T> action) {
        for (T t : this) {
            ex(() -> action.apply(t));
        }
    }

    /**
     * Returns a Spliterator over the elements in this FnList.
     *
     * @return a Spliterator for the FnList.
     */
    public Spliterator<T> spliterator() {
        return Iterable.super.spliterator();
    }

    /**
     * Helper method to execute a Producer while catching and logging any exceptions.
     * In strict mode, exceptions are rethrown as RuntimeExceptions.
     *
     * @param producer the Producer to execute.
     * @param <T>      the type of the produced element.
     * @return the produced element or null if an exception occurred (in non-strict mode).
     * @throws RuntimeException if strict mode is enabled and an exception occurs
     */
    private static <T> T ex(Producer<T> producer) {
        try {
            return producer.produce();
        } catch (Exception e) {
            if (strictMode) {
                throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
            }
            log(e);
        }
        return null;
    }

    /**
     * Helper method to execute a Runnable while catching and logging any exceptions.
     *
     * @param runnable the Runnable to execute.
     */
    private static void ex(Runnable runnable) {
        ex(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Filters out "empty" elements from the FnList. An element is pruned if it is:
     * <ul>
     *   <li>null</li>
     *   <li>An empty String (after trimming)</li>
     *   <li>An empty Collection</li>
     *   <li>An empty Map</li>
     *   <li>An empty CharSequence</li>
     *   <li>An array with length zero</li>
     * </ul>
     *
     * @return a new FnList with pruned elements.
     */
    public FnList<T> prune() {
        return filter(it -> it != null
                && (!(it instanceof String) || !((String) it).trim().isEmpty())
                && (!(it instanceof Collection) || !((Collection<?>) it).isEmpty())
                && (!(it instanceof Map) || !((Map<?, ?>) it).isEmpty())
                && (!(it instanceof CharSequence) || !((CharSequence) it).isEmpty())
                && (!it.getClass().isArray() || java.lang.reflect.Array.getLength(it) > 0)
        );
    }

    /**
     * Returns an iterator over the elements in the FnList.
     *
     * @return an Iterator for the FnList.
     */
    public @NotNull Iterator<T> iterator() {
        return lazy;
    }

    /**
     * Returns the first non-null element of the FnList.
     * If no such element exists, returns null.
     *
     * @return the first non-null element or null if none exists.
     */
    public T first() {
        while (lazy.hasNext()) {
            T val = lazy.next();
            if (val != null) {
                return val;
            }
        }
        return null;
    }
}
