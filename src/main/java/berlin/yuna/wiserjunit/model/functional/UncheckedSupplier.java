package berlin.yuna.wiserjunit.model.functional;

import java.util.function.Function;
import java.util.function.Supplier;

import static berlin.yuna.wiserjunit.model.bdd.BddCore.handleUnchecked;

@FunctionalInterface
public interface UncheckedSupplier<T> extends Supplier<T> {

    @Override
    default T get() {
        return get(null);
    }

    default T get(final Function<Exception, RuntimeException> handler) {
        try {
            return getThrows();
        } catch (final Exception e) {
            throw handleUnchecked(e, null, handler);
        } catch (Error e) {
            throw handleUnchecked(null, e, handler);
        }
    }

    T getThrows();
}
