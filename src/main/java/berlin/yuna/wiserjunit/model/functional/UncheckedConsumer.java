package berlin.yuna.wiserjunit.model.functional;

import java.util.function.Consumer;
import java.util.function.Function;

import static berlin.yuna.wiserjunit.model.bdd.BddCore.handleUnchecked;

@FunctionalInterface
public interface UncheckedConsumer<T> extends Consumer<T> {

    @Override
    default void accept(final T value) {
        accept(value, null);
    }

    default void accept(final T value, final Function<Exception, RuntimeException> handler) {
        try {
            acceptThrows(value);
        } catch (final Exception e) {
            throw handleUnchecked(e, null, handler);
        } catch (Error e) {
            throw handleUnchecked(null, e, handler);
        }
    }

    void acceptThrows(T value);
}
