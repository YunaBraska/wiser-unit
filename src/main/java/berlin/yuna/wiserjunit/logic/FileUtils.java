package berlin.yuna.wiserjunit.logic;

import berlin.yuna.wiserjunit.config.WiserJunitConfig;
import berlin.yuna.wiserjunit.model.exception.WiserExtensionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static berlin.yuna.wiserjunit.config.WiserJunitConfig.MAPPER_YAML;
import static java.lang.System.getProperty;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.toSet;

public class FileUtils {

    private static final WiserJunitConfig CONFIG = readFile(Paths.get(getProperty("user.dir"), "wiser_report.yaml"), WiserJunitConfig.class, MAPPER_YAML).orElseGet(WiserJunitConfig::new);

    private FileUtils() {
    }

    public static <T> Optional<T> readFile(final Path file, final Class<T> type, final ObjectMapper objectMapper) {
        try {
            if (Files.exists(file) && Files.isRegularFile(file)) {
                return Optional.ofNullable(objectMapper.readValue(file.toFile(), type));
            }
        } catch (MismatchedInputException e) {
            if (!e.getMessage().contains("No content")) {
                throw new WiserExtensionException("Error while reading file [" + file + "] " + e.getMessage());
            }
        } catch (IOException ignored) {
        }
        return Optional.empty();
    }

    public static Optional<Path> validatePath(final Path path) {
        return Optional.ofNullable(path != null && path.getParent() != null && Files.exists(path.getParent()) ? path : null);
    }

    public static Optional<Path> getPhysicalPath(final Path workDir, final Class<?> clazz) {
        final String name = clazz.getSimpleName();
        final String dir = clazz.getClassLoader().getResource(clazz.getPackage().getName().replace(".", "/")).getPath();
        final Optional<Path> result = getPathCombinations(name, dir).stream().filter(Files::exists).filter(Files::isRegularFile).findFirst();
        return result.isPresent() ? result : findFileByName(workDir, clazz.getSimpleName());
    }

    public static Optional<Path> findFileByName(final Path workDir, final String name) {
        try (Stream<Path> walk = Files.walk(workDir, 32)) {
            return walk.filter(Files::isDirectory)
                    .flatMap(path -> getPathCombinations(name, path.toString()).stream())
                    .filter(Files::exists).filter(Files::isRegularFile).findFirst();
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public static String removeExtension(final String fileName) {
        int pos = fileName.lastIndexOf(".");
        return pos > 0 ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
    }

    public static Optional<String> readLine(final Path file, final int threshold, final int lineCount) {
        final AtomicReference<String> result = new AtomicReference<>(null);
        try (Stream<String> lines = Files.lines(file)) {
            int start = Math.max(threshold - (lineCount / 2), 1) - 1;
            result.set(lines.skip(start).limit(lineCount).collect(Collectors.joining(lineSeparator())));
        } catch (IOException ignored) {
        }
        return Optional.ofNullable(result.get());
    }

    private static Set<Path> getPathCombinations(final String name, String dir) {
        return CONFIG.getTestFileExtensions().stream().map(extension -> Paths.get(dir, name + "." + extension)).collect(toSet());
    }
}
