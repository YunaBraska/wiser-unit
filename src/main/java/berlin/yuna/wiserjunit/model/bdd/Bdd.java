package berlin.yuna.wiserjunit.model.bdd;

import berlin.yuna.wiserjunit.model.functional.UncheckedConsumer;
import berlin.yuna.wiserjunit.model.functional.UncheckedFunction;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.function.Executable;

import java.util.HashSet;
import java.util.List;

import static berlin.yuna.wiserjunit.model.bdd.Bdd.BddType.AND;
import static berlin.yuna.wiserjunit.model.bdd.Bdd.BddType.BUT;
import static berlin.yuna.wiserjunit.model.bdd.Bdd.BddType.GIVEN;
import static berlin.yuna.wiserjunit.model.bdd.Bdd.BddType.MATCH;
import static berlin.yuna.wiserjunit.model.bdd.Bdd.BddType.THEN;
import static berlin.yuna.wiserjunit.model.bdd.Bdd.BddType.WHEN;
import static berlin.yuna.wiserjunit.model.bdd.Bdd.BddType.WHERE;
import static berlin.yuna.wiserjunit.model.bdd.BddCore.formatBdd;
import static berlin.yuna.wiserjunit.model.bdd.BddCore.getParents;
import static berlin.yuna.wiserjunit.model.bdd.BddCore.handleHamcrestMatcher;
import static berlin.yuna.wiserjunit.model.bdd.BddCore.handleUnchecked;
import static berlin.yuna.wiserjunit.model.bdd.BddCore.renderException;
import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings({"unused", "S1181"})
public class Bdd<T> {

    protected final T input;
    protected final String message;
    protected final Bdd<?> parentTask;
    protected final BddType type;

    public enum BddType {
        SUMMARY,
        FEATURE,
        GIVEN,
        THEN,
        WHEN,
        AND,
        BUT,
        WHERE,
        MATCH,
    }

    @SuppressWarnings("uncheckedCast")
    protected Bdd(final Bdd<?> parentTask, final BddType type, final String message, final T input) {
        this.type = type;
        this.input = input;
        this.message = message;
        this.parentTask = parentTask;
        handleHamcrest(input);
    }

    //BASE ADOPTED VARIABLE
    public <E> Bdd<E> given(final E variable) {
        return bddVariable(GIVEN, message, variable);
    }

    public <E> Bdd<E> given(final String message, final E variable) {
        return bddVariable(GIVEN, message, variable);
    }

    public <E> Bdd<E> when(final E variable) {
        return bddVariable(WHEN, message, variable);
    }

    public <E> Bdd<E> when(final String message, final E variable) {
        return bddVariable(WHEN, message, variable);
    }

    public <E> Bdd<E> then(final E variable) {
        return bddVariable(THEN, message, variable);
    }

    public <E> Bdd<E> then(final String message, final E variable) {
        return bddVariable(THEN, message, variable);
    }

    public <E> Bdd<E> and(final E variable) {
        return bddVariable(AND, message, variable);
    }

    public <E> Bdd<E> and(final String message, final E variable) {
        return bddVariable(AND, message, variable);
    }

    public <E> Bdd<E> but(final E variable) {
        return bddVariable(AND, message, variable);
    }

    public <E> Bdd<E> but(final String message, final E variable) {
        return bddVariable(AND, message, variable);
    }

    public <E> Bdd<E> where(final E variable) {
        return bddVariable(WHERE, message, variable);
    }

    public <E> Bdd<E> where(final String message, final E variable) {
        return bddVariable(WHERE, message, variable);
    }

    protected <E> Bdd<E> bddVariable(final BddType type, final String message, final E variable) {
        return new Bdd<>(this, type, message, variable);
    }

    //BASE ADOPTED FUNCTIONS
    public <F> Bdd<F> given(final UncheckedFunction<T, F> function) {
        return bddFunction(GIVEN, null, function);
    }

    public <F> Bdd<F> given(final String message, final UncheckedFunction<T, F> function) {
        return bddFunction(GIVEN, message, function);
    }

    public <F> Bdd<F> when(final UncheckedFunction<T, F> function) {
        return bddFunction(WHEN, null, function);
    }

    public <F> Bdd<F> when(final String message, final UncheckedFunction<T, F> function) {
        return bddFunction(WHEN, message, function);
    }

    public <F> Bdd<F> then(final UncheckedFunction<T, F> function) {
        return bddFunction(THEN, null, function);
    }

    public <F> Bdd<F> then(final String message, final UncheckedFunction<T, F> function) {
        return bddFunction(THEN, message, function);
    }

    public <F> Bdd<F> and(final UncheckedFunction<T, F> function) {
        return bddFunction(AND, null, function);
    }

    public <F> Bdd<F> and(final String message, final UncheckedFunction<T, F> function) {
        return bddFunction(AND, message, function);
    }

    public <F> Bdd<F> but(final UncheckedFunction<T, F> function) {
        return bddFunction(BUT, null, function);
    }

    public <F> Bdd<F> but(final String message, final UncheckedFunction<T, F> function) {
        return bddFunction(BUT, message, function);
    }

    public <F> Bdd<F> where(final UncheckedFunction<T, F> function) {
        return bddFunction(WHERE, null, function);
    }

    public <F> Bdd<F> where(final String message, final UncheckedFunction<T, F> function) {
        return bddFunction(WHERE, message, function);
    }

    //BASE ADOPTED CONSUMER
    @SafeVarargs
    public final Bdd<T> given(final UncheckedConsumer<T>... consumers) {
        return bddConsumer(GIVEN, message, asList(consumers));
    }

    @SafeVarargs
    public final Bdd<T> given(final String message, final UncheckedConsumer<T>... consumers) {
        return bddConsumer(GIVEN, message, asList(consumers));
    }

    public Bdd<T> given(final Iterable<UncheckedConsumer<T>> consumers) {
        return bddConsumer(GIVEN, message, consumers);
    }

    public Bdd<T> given(final String message, final Iterable<UncheckedConsumer<T>> consumers) {
        return bddConsumer(GIVEN, message, consumers);
    }

    @SafeVarargs
    public final Bdd<T> when(final UncheckedConsumer<T>... consumers) {
        return bddConsumer(WHEN, message, asList(consumers));
    }

    @SafeVarargs
    public final Bdd<T> when(final String message, final UncheckedConsumer<T>... consumers) {
        return bddConsumer(WHEN, message, asList(consumers));
    }

    public Bdd<T> when(final Iterable<UncheckedConsumer<T>> consumers) {
        return bddConsumer(WHEN, message, consumers);
    }

    public Bdd<T> when(final String message, final Iterable<UncheckedConsumer<T>> consumers) {
        return bddConsumer(WHEN, message, consumers);
    }

    @SafeVarargs
    public final Bdd<T> then(final UncheckedConsumer<T>... consumers) {
        return bddConsumer(THEN, message, asList(consumers));
    }

    @SafeVarargs
    public final Bdd<T> then(final String message, final UncheckedConsumer<T>... consumers) {
        return bddConsumer(THEN, message, asList(consumers));
    }

    public Bdd<T> then(final Iterable<UncheckedConsumer<T>> consumers) {
        return bddConsumer(THEN, message, consumers);
    }

    public Bdd<T> then(final String message, final Iterable<UncheckedConsumer<T>> consumers) {
        return bddConsumer(THEN, message, consumers);
    }

    @SafeVarargs
    public final Bdd<T> and(final UncheckedConsumer<T>... consumers) {
        return bddConsumer(AND, message, asList(consumers));
    }

    @SafeVarargs
    public final Bdd<T> and(final String message, final UncheckedConsumer<T>... consumers) {
        return bddConsumer(AND, message, asList(consumers));
    }

    public Bdd<T> and(final Iterable<UncheckedConsumer<T>> consumers) {
        return bddConsumer(AND, message, consumers);
    }

    public Bdd<T> and(final String message, final Iterable<UncheckedConsumer<T>> consumers) {
        return bddConsumer(AND, message, consumers);
    }

    @SafeVarargs
    public final Bdd<T> but(final UncheckedConsumer<T>... consumers) {
        return bddConsumer(BUT, message, asList(consumers));
    }

    @SafeVarargs
    public final Bdd<T> but(final String message, final UncheckedConsumer<T>... consumers) {
        return bddConsumer(BUT, message, asList(consumers));
    }

    public Bdd<T> but(final Iterable<UncheckedConsumer<T>> consumers) {
        return bddConsumer(BUT, message, consumers);
    }

    public Bdd<T> but(final String message, final Iterable<UncheckedConsumer<T>> consumers) {
        return bddConsumer(BUT, message, consumers);
    }

    @SafeVarargs
    public final Bdd<T> where(final UncheckedConsumer<T>... consumers) {
        return bddConsumer(WHERE, message, asList(consumers));
    }

    @SafeVarargs
    public final Bdd<T> where(final String message, final UncheckedConsumer<T>... consumers) {
        return bddConsumer(WHERE, message, asList(consumers));
    }

    public Bdd<T> where(final Iterable<UncheckedConsumer<T>> consumers) {
        return bddConsumer(WHERE, message, consumers);
    }

    public Bdd<T> where(final String message, final Iterable<UncheckedConsumer<T>> consumers) {
        return bddConsumer(WHERE, message, consumers);
    }

    //BASE ADOPTED MATCHERS
    public Bdd<T> match(final Matcher<? super T> matcher) {
        return bddHamcrest(message, new HashSet<>(singletonList(matcher)));
    }

    public Bdd<T> match(final String message, final Matcher<? super T> matcher) {
        return bddHamcrest(message, new HashSet<>(singletonList(matcher)));
    }

    @SafeVarargs
    public final Bdd<T> match(final Matcher<? super T>... matcher) {
        return bddHamcrest(message, asList(matcher));
    }

    @SafeVarargs
    public final Bdd<T> match(final String message, final Matcher<? super T>... matchers) {
        return bddHamcrest(message, asList(matchers));
    }

    @SafeVarargs
    public final Bdd<T> match(final UncheckedConsumer<T>... consumers) {
        return bddConsumer(MATCH, message, asList(consumers));
    }

    public Bdd<T> match(final Iterable<UncheckedConsumer<T>> consumers) {
        return bddConsumer(MATCH, message, consumers);
    }

    @SafeVarargs
    public final Bdd<T> match(final String message, final UncheckedConsumer<T>... consumers) {
        return bddConsumer(MATCH, message, asList(consumers));
    }

    public Bdd<T> match(final String message, final Iterable<UncheckedConsumer<T>> consumers) {
        return bddConsumer(MATCH, message, consumers);
    }

    public <F extends Throwable> Bdd<Class<F>> willThrow(final Class<F> expected, final Executable executable) {
        return willThrow(null, expected, executable);
    }

    public <F extends Throwable> Bdd<Class<F>> willThrow(final String message, final Class<F> expected, final Executable executable) {
        final Bdd<Class<F>> result = new Bdd<>(
                this,
                MATCH,
                "Should throw [" + expected.getSimpleName() + "]" + (message != null ? " with [" + message + "]" : ""),
                expected
        );
        try {
            assertThrows(expected, executable, message);
        } catch (final Exception e) {
            throw handleUnchecked(e, null, exception -> renderException(result, exception));
        } catch (Error e) {
            throw handleUnchecked(null, e, exception -> renderException(result, exception));
        }
        return result;
    }

    /**
     * Optional end identifier
     *
     * @return result of all configured elements
     */
    public String end() {
        final StringBuilder result = new StringBuilder();
        getParents(this).forEach(p -> result.append(formatBdd(0x2705, p)).append(lineSeparator()));
        return result.toString();
    }

    //BASE MATCHERS
    protected Bdd<T> bddHamcrest(final String message, final Iterable<Matcher<?>> matchers) {
        final Bdd<T> result = new Bdd<>(this, MATCH, message, input);
        return handleHamcrestMatcher(result, matchers, input);
    }

    protected Bdd<T> bddConsumer(final BddType type, final String message, final Iterable<UncheckedConsumer<T>> consumers) {
        final Bdd<T> result = new Bdd<>(this, type, message, input);
        consumers.forEach(consumer -> consumer.accept(input, e -> renderException(new Bdd<>(result, type, message, null), e)));
        return result;
    }


    protected <F> Bdd<F> bddFunction(final BddType type, final String message, final UncheckedFunction<T, F> function) {
        return new Bdd<>(
                this,
                type,
                message,
                function.apply(input, e -> renderException(new Bdd<>(this, type, message, null), e))
        );
    }

    private void handleHamcrest(final T input) {
        if (this.parentTask != null && input instanceof Matcher) {
            handleHamcrestMatcher(this, new HashSet<>(singletonList((Matcher<?>) input)), this.parentTask.input);
        } else if (this.parentTask != null && input instanceof Iterable && ((Iterable<?>) input).iterator().next() instanceof Matcher) {
            final List<Matcher<?>> matchers = stream(((Iterable<?>) input).spliterator(), false)
                    .filter(o -> (o instanceof Matcher))
                    .map(o -> (Matcher<?>) o)
                    .collect(toList());
            if (!matchers.isEmpty()) {
                handleHamcrestMatcher(this, matchers, this.parentTask.input);
            }
        }
    }
}