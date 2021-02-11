package berlin.yuna.wiserjunit.model.exception;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.join;
import static java.lang.System.lineSeparator;

public class BddException extends RuntimeException {

    final List<String> messages;

    public BddException(final String message, final Throwable cause) {
        super(lineSeparator() + message, cause);
        this.messages = new ArrayList<>();
    }

    public BddException(final List<String> messages, final Throwable cause) {
        super(lineSeparator() + join("", messages), cause);
        this.messages = messages;
    }

    public List<String> getMessages() {
        return messages;
    }
}
