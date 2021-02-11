package berlin.yuna.wiserjunit.model.exception;

public class WiserExtensionException extends RuntimeException {

    public WiserExtensionException(final String message) {
        super(message);
    }

    public WiserExtensionException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
