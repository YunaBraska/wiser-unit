package berlin.yuna.wiserjunit.logic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.System.lineSeparator;
import static java.util.Arrays.stream;

public class FileUtils {

    private FileUtils() {
    }

    public static Optional<Path> validatePath(final Path path) {
        return Optional.ofNullable(path != null && path.getParent() != null && Files.exists(path.getParent()) ? path : null);
    }

    public static Optional<Path> getPhysicalPath(final Path workDir, final Class<?> clazz) {
        final String name = clazz.getSimpleName();
        final String dir = clazz.getClassLoader().getResource(clazz.getPackage().getName().replace(".", "/")).getPath();
        final Optional<Path> result = stream(getPathCombinations(name, dir)).filter(Files::exists).filter(Files::isRegularFile).findFirst();
        return result.isPresent() ? result : findFileByName(workDir, clazz.getSimpleName());
    }

    public static Optional<Path> findFileByName(final Path workDir, final String name) {
        try (Stream<Path> walk = Files.walk(workDir, 32)) {
            return walk.filter(Files::isDirectory)
                    .flatMap(path -> stream(getPathCombinations(name, path.toString())))
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

    private static Path[] getPathCombinations(final String name, String dir) {
        return new Path[]{
                //TODO: configurable set of suffix
                Paths.get(dir, name + ".java")
//                Paths.get(dir, name + ".class")
        };
    }
}
