package berlin.yuna.wiserjunit.model.bdd;

import berlin.yuna.wiserjunit.model.exception.BddException;
import berlin.yuna.wiserjunit.model.functional.UncheckedSupplier;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static berlin.yuna.wiserjunit.model.bdd.Bdd.BddType.AND;
import static berlin.yuna.wiserjunit.model.bdd.Bdd.BddType.BUT;
import static berlin.yuna.wiserjunit.model.bdd.Bdd.BddType.FEATURE;
import static berlin.yuna.wiserjunit.model.bdd.Bdd.BddType.GIVEN;
import static berlin.yuna.wiserjunit.model.bdd.Bdd.BddType.SUMMARY;
import static berlin.yuna.wiserjunit.model.bdd.Bdd.BddType.THEN;
import static berlin.yuna.wiserjunit.model.bdd.Bdd.BddType.WHEN;
import static berlin.yuna.wiserjunit.model.bdd.Bdd.BddType.WHERE;
import static java.lang.Character.toChars;
import static java.lang.String.format;
import static java.lang.System.lineSeparator;

@SuppressWarnings("unused")
public class BddCore {

    private BddCore() {
    }

    //ADOPTED STATICS
    public static <E> Bdd<E> feature(final String message) {
        return bdd(FEATURE, message, null);
    }

    public static <E> Bdd<E> summary(final String message) {
        return bdd(SUMMARY, message, null);
    }

    public static <E> Bdd<E> given(final E variable) {
        return bdd(GIVEN, null, variable);
    }

    public static <E> Bdd<E> given(final String message, final E variable) {
        return bdd(GIVEN, message, variable);
    }

    public static <E> Bdd<E> given(final UncheckedSupplier<E> supplier) {
        return bddSupplier(GIVEN, null, supplier);
    }

    public static <E> Bdd<E> given(final String message, final UncheckedSupplier<E> supplier) {
        return bddSupplier(GIVEN, null, supplier);
    }

    private static <E> Bdd<E> and(final E variable) {
        return bdd(AND, null, variable);
    }

    public static <E> Bdd<E> and(final String message, final E variable) {
        return bdd(AND, message, variable);
    }

    public static <E> Bdd<E> and(final UncheckedSupplier<E> supplier) {
        return bddSupplier(AND, null, supplier);
    }

    public static <E> Bdd<E> and(final String message, final UncheckedSupplier<E> supplier) {
        return bddSupplier(AND, null, supplier);
    }

    public static <E> Bdd<E> but(final E variable) {
        return bdd(BUT, null, variable);
    }

    public static <E> Bdd<E> but(final String message, final E variable) {
        return bdd(BUT, message, variable);
    }

    public static <E> Bdd<E> but(final UncheckedSupplier<E> supplier) {
        return bddSupplier(BUT, null, supplier);
    }

    public static <E> Bdd<E> but(final String message, final UncheckedSupplier<E> supplier) {
        return bddSupplier(BUT, null, supplier);
    }

    public static <E> Bdd<E> when(final E variable) {
        return bdd(WHEN, null, variable);
    }

    public static <E> Bdd<E> when(final String message, final E variable) {
        return bdd(WHEN, message, variable);
    }

    public static <E> Bdd<E> when(final UncheckedSupplier<E> supplier) {
        return bddSupplier(WHEN, null, supplier);
    }

    public static <E> Bdd<E> when(final String message, final UncheckedSupplier<E> supplier) {
        return bddSupplier(WHEN, null, supplier);
    }

    public static <E> Bdd<E> then(final E variable) {
        return bdd(THEN, null, variable);
    }

    public static <E> Bdd<E> then(final String message, final E variable) {
        return bdd(THEN, message, variable);
    }

    public static <E> Bdd<E> then(final UncheckedSupplier<E> supplier) {
        return bddSupplier(THEN, null, supplier);
    }

    public static <E> Bdd<E> then(final String message, final UncheckedSupplier<E> supplier) {
        return bddSupplier(THEN, null, supplier);
    }

    public static <E> Bdd<E> where(final E variable) {
        return bdd(THEN, null, variable);
    }

    public static <E> Bdd<E> where(final String message, final E variable) {
        return bdd(WHERE, message, variable);
    }

    public static <E> Bdd<E> where(final UncheckedSupplier<E> supplier) {
        return bddSupplier(WHERE, null, supplier);
    }

    public static <E> Bdd<E> where(final String message, final UncheckedSupplier<E> supplier) {
        return bddSupplier(WHERE, null, supplier);
    }

    public static BddException renderException(final Bdd<?> bdd, final Throwable throwable) {
        return renderException(bdd, "", throwable);
    }

    public static BddException renderException(final Bdd<?> bdd, final String message) {
        return renderException(bdd, message, null);
    }

    public static BddException renderException(final Bdd<?> bdd, final String suffix, final Throwable throwable) {
        final List<String> result = new ArrayList<>();
        getParents(bdd.parentTask).forEach(p -> result.add(formatBdd(0x2705, p) + lineSeparator()));
        result.add(
                formatBdd(0x274C, bdd) + lineSeparator()
                        + "   [FAILED]" + (throwable != null ? " with: " + throwable.getMessage() : "")
                        + lineSeparator()
        );
        if (suffix != null) {
            result.add(suffix.trim() + lineSeparator());
        }
        return new BddException(result, throwable);
    }

    protected static <E> Bdd<E> bdd(final Bdd.BddType type, final E variable) {
        return bdd(type, null, variable);
    }

    protected static <E> Bdd<E> bdd(final Bdd.BddType type, final String message, final E variable) {
        return new Bdd<>(null, type, message, variable);
    }

    protected static <F> Bdd<F> bddSupplier(final Bdd.BddType type, final String message, final UncheckedSupplier<F> function) {
        return new Bdd<>(
                null,
                type,
                message,
                function.get(e -> renderException(new Bdd<>(null, type, message, null), e))
        );
    }

    protected static String formatBdd(final int emoji, final Bdd<?> bdd) {
        return formatBdd(emoji, bdd.type.toString(), bdd.message);
    }

    public static String formatBdd(final int emoji, final String type, final String message) {
        return format(
                "%-7s:%s", (emoji > 0 ? new String(toChars(emoji)) + " " : "")
                        + type, message == null ? "" : " " + message
        );
    }

    public static <T> Bdd<T> handleHamcrestMatcher(final Bdd<T> result, final Iterable<Matcher<?>> matchers, final Object input) {
        for (Matcher<?> matcher : matchers) {
            if (!matcher.matches(input)) {
                final Description description = new StringDescription();
                description
                        .appendText(System.lineSeparator())
                        .appendText("Expected: ")
                        .appendDescriptionOf(matcher)
                        .appendText(System.lineSeparator())
                        .appendText("     but: ");
                matcher.describeMismatch(input, description);

                throw renderException(result, description.toString());
            }
        }
        return result;
    }

    public static RuntimeException handleUnchecked(final Exception t, final Error e, final Function<Exception, RuntimeException> handler) {
        if (handler == null) {
            return new BddException(e != null ? e.getMessage() : null, e != null ? e.getCause() : t);
        } else {
            return handler.apply(e != null ? new RuntimeException(e.getMessage(), e.getCause()) : t);
        }
    }

    protected static List<Bdd<?>> getParents(final Bdd<?> bdd) {
        final List<Bdd<?>> result = new ArrayList<>();
        if (bdd != null) {
            if (bdd.parentTask != null) {
                result.addAll(getParents(bdd.parentTask));
            }
            result.add(bdd);
        }
        return result;
    }
}
