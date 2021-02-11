package berlin.yuna.wiserjunit.config;

import berlin.yuna.wiserjunit.model.bdd.Bdd;
import berlin.yuna.wiserjunit.model.bdd.BddCore;
import berlin.yuna.wiserjunit.model.exception.WiserExtensionException;
import berlin.yuna.wiserjunit.model.functional.UncheckedConsumer;
import berlin.yuna.wiserjunit.model.functional.UncheckedFunction;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static berlin.yuna.wiserjunit.config.WiserJunitConfig.Format.JSON;
import static berlin.yuna.wiserjunit.config.WiserJunitConfig.Format.YAML;

@SuppressWarnings({"unused", "ResultOfMethodCallIgnored", "java:S108"})
public class WiserJunitConfig {


    public enum Format {
        YAML,
        JSON
    }

    private String name = "Report";
    private WiserJunitConfig.Format format = JSON;
    private boolean generateNew = true;
    private int errorPreviewLines = 1;
    private String projectDir = System.getProperty("user.dir");
    private Path output = Paths.get(projectDir, TARGET_FOLDER, "wiser-report.out");
    private Path outputNested = Paths.get(projectDir, TARGET_FOLDER, "wiser-report-nested.out");
    private Path outputHtml = Paths.get(projectDir, TARGET_FOLDER, "wiser-report.html");
    private Path outputLock = Paths.get(projectDir, TARGET_FOLDER, "wiser-report.lock");
    private Set<String> classesIgnore = new HashSet<>();

    public static final ObjectMapper MAPPER_YAML = configure(new ObjectMapper(new YAMLFactory()));
    private static final ObjectMapper MAPPER_JSON = configure(new ObjectMapper());
    private static final String TARGET_FOLDER = "target";

    public WiserJunitConfig() {
        classesIgnore.addAll(new HashSet<>(Arrays.asList(
                Bdd.class.getSimpleName(),
                BddCore.class.getSimpleName(),
                UncheckedConsumer.class.getSimpleName(),
                UncheckedFunction.class.getSimpleName()
        )));
        deletePrevious();
        createReport();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    public boolean isGenerateNew() {
        return generateNew;
    }

    public void setGenerateNew(boolean generateNew) {
        this.generateNew = generateNew;
    }

    public String getProjectDir() {
        return projectDir;
    }

    public void setProjectDir(String projectDir) {
        this.projectDir = projectDir;
    }

    public Path getOutput() {
        return output;
    }

    public void setOutput(final String output) {
        this.output = replaceEnvs(output);
    }

    public Path getOutputLock() {
        return outputLock;
    }

    public void setOutputLock(final String outputLock) {
        this.outputLock = replaceEnvs(outputLock);
    }

    public int getErrorPreviewLines() {
        return errorPreviewLines;
    }

    public void setErrorPreviewLines(int errorPreviewLines) {
        this.errorPreviewLines = errorPreviewLines;
    }

    public Path getOutputNested() {
        return outputNested;
    }

    public Path getOutputHtml() {
        return outputHtml;
    }

    public void setOutputHtml(final String outputHtml) {
        this.outputHtml = replaceEnvs(outputHtml);
    }

    public void setOutputNested(final String outputNested) {
        this.outputNested = replaceEnvs(outputNested);
    }

    public Set<String> getClassesIgnore() {
        return classesIgnore;
    }

    public void setClassesIgnore(Set<String> classesIgnore) {
        this.classesIgnore = classesIgnore;
    }

    private void createReport() {
        if (!Files.exists(output)) {
            output.getParent().toFile().mkdirs();
            try {
                Files.createFile(output);
            } catch (IOException e) {
                throw new WiserExtensionException("Could not create file [" + output + "]", e);
            }
        }
    }

    public ObjectMapper getMapper() {
        return format == YAML ? MAPPER_YAML : MAPPER_JSON;
    }

    @SuppressWarnings("BusyWait")
    public synchronized void tryUnlock(final Consumer<Path> supplier) {
        try {
            final long start = System.currentTimeMillis();
            while (Files.exists(outputLock)) {
                Thread.sleep(2);
                if (start + 2000 > System.currentTimeMillis()) {
                    Files.delete(outputLock);
                    break;
                }
            }
            Files.createFile(outputLock);
            supplier.accept(output);
            Files.deleteIfExists(outputLock);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Path replaceEnvs(final String path) {
        if (path.contains("%")) {
            final AtomicReference<String> result = new AtomicReference<>(path);
            System.getProperties().forEach(
                    (key, value) -> result.set(result.get().replace("%" + key + "%", (CharSequence) value))
            );
            return Paths.get(result.get());
        }
        return path.equals("false") ? null : Paths.get(path);
    }

    private void deletePrevious() {
        if (generateNew && Files.exists(output)) {
            deleteFile(output);
            deleteFile(outputHtml);
            deleteFile(outputNested);
        }
        if (Files.exists(outputLock)) {
            deleteFile(outputLock);
        }
    }

    private static void deleteFile(final Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
    }

    private static ObjectMapper configure(final ObjectMapper mapper) {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }
}
