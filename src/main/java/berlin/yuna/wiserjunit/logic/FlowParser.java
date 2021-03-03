package berlin.yuna.wiserjunit.logic;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static berlin.yuna.wiserjunit.model.bdd.Bdd.BddType.MATCH;
import static berlin.yuna.wiserjunit.model.bdd.BddCore.formatBdd;
import static java.lang.System.lineSeparator;

public class FlowParser {

    private static final Pattern PATTERN_METHOD = Pattern.compile("\\s*(public|private|protected|void|static|final|native|synchronized|abstract|transient).*\\s(?<name>\\w+)\\s*\\(\\)");
    private static final Pattern PATTERN_BDD = Pattern.compile("\\s*(?<name>summary|feature|given|when|then|and|but|where|match|willThrow)\\s*\\(\\s*\"(?<description>(.|\\s)*?)(\"\\s*,|\"\\s*\\))");


    private FlowParser() {
    }

    public static List<String> parseFlowFromFile(final Method method, final Path classPath) {
        final Matcher matcher = parseMatcherFromFile(method, classPath);
        final List<String> result = new ArrayList<>();
        while (matcher.find()) {
            try {
                final String name = matcher.group("name").toUpperCase().replace("WILLTHROW", MATCH.toString());
                final String description = matcher.group("description");
                result.add(formatBdd(0x2705, name, description) + lineSeparator());
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    public static Matcher parseMatcherFromFile(final Method method, final Path classPath) {
        return PATTERN_BDD.matcher(parseMethodFromFile(method, classPath));
    }

    public static String parseMethodFromFile(final Method method, final Path classPath) {
        final StringBuilder sb = new StringBuilder();
        final boolean[] activeMatch = {false};
        try (Stream<String> lines = Files.lines(classPath)) {
            lines.forEach(line -> {
                final Optional<String> methodMatch = getMatch(line, PATTERN_METHOD, "name");
                if (methodMatch.isPresent() && activeMatch[0]) {
                    activeMatch[0] = false;
                } else if (methodMatch.isPresent()) {
                    activeMatch[0] = methodMatch.filter(s -> s.trim().equalsIgnoreCase(method.getName())).isPresent();
                }
                if (activeMatch[0]) {
                    sb.append(line).append(System.lineSeparator());
                }
            });
        } catch (IOException ignored) {
        }
        return sb.toString();
    }

    private static Optional<String> getMatch(final String text, final Pattern pattern, final String group) {
        final Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return Optional.ofNullable(matcher.group(group));
        }
        return Optional.empty();
    }
}
