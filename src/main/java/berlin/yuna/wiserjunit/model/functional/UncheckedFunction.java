package berlin.yuna.wiserjunit.model.functional;

import java.util.function.Function;

import static berlin.yuna.wiserjunit.model.bdd.BddCore.handleUnchecked;

@FunctionalInterface
public interface UncheckedFunction<T, R> extends Function<T, R> {

    @Override
    default R apply(final T value) {
        return apply(value, null);
    }

    default R apply(final T value, final Function<Exception, RuntimeException> handler) {
        try {
            return applyThrows(value);
        } catch (final Exception e) {
            throw handleUnchecked(e, null, handler);
        } catch (Error e) {
            throw handleUnchecked(null, e, handler);
        }
    }

    R applyThrows(T value);
}
