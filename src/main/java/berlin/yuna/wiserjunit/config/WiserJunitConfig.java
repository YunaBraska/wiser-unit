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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static berlin.yuna.wiserjunit.logic.FileUtils.validatePath;

@SuppressWarnings({"unused", "ResultOfMethodCallIgnored", "java:S108"})
public class WiserJunitConfig {


    //TODO configure multiple output formats

    private String name = "Report";
    private boolean generateNew = true;
    private boolean generateFlow = true;
    private boolean generateHtml = true;
    private boolean generateCsv = true;
    private boolean generateYaml = true;
    private int errorPreviewLines = 1;
    private String projectDir = System.getProperty("user.dir");
    private Path outputDir = Paths.get(projectDir, TARGET_FOLDER, "wiser-unit");
    private Set<String> classesIgnore = new HashSet<>();

    public static final ObjectMapper MAPPER_YAML = configure(new ObjectMapper(new YAMLFactory()));
    public static final ObjectMapper MAPPER_JSON = configure(new ObjectMapper());
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

    public Path getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(final String outputDir) {
        this.outputDir = replaceEnvs(outputDir);
    }

    public boolean isGenerateHtml() {
        return generateHtml;
    }

    public void setGenerateHtml(final boolean generateHtml) {
        this.generateHtml = generateHtml;
    }

    public boolean isGenerateCsv() {
        return generateCsv;
    }

    public void setGenerateCsv(final boolean generateCsv) {
        this.generateCsv = generateCsv;
    }

    public boolean isGenerateYaml() {
        return generateYaml;
    }

    public void setGenerateYaml(final boolean generateYaml) {
        this.generateYaml = generateYaml;
    }

    public Path getOutputLockRaw() {
        return Paths.get(outputDir.toString(), "wiser-report.lock");
    }

    public Path getOutputJsonRaw() {
        return Paths.get(outputDir.toString(), "report.json");
    }

    public Optional<Path> getOutputJson() {
        return validatePath(getOutputJsonRaw());
    }

    public Optional<Path> getOutputLock() {
        return validatePath(getOutputLockRaw());
    }


    public Optional<Path> getOutputYaml() {
        return generateYaml ? validatePath(Paths.get(outputDir.toString(), "report.yaml")) : Optional.empty();
    }

    public Optional<Path> getOutputCsv() {
        return generateCsv ? validatePath(Paths.get(outputDir.toString(), "report.csv")) : Optional.empty();
    }

    public Optional<Path> getOutputHtml() {
        return generateHtml ? validatePath(Paths.get(outputDir.toString(), "report.html")) : Optional.empty();
    }

    public List<Path> getOutputAll() {
        final List<Path> result = new ArrayList<>();
        for (Optional<Path> path : new Optional[]{getOutputJson(), getOutputYaml(), getOutputHtml(), getOutputCsv(), getOutputLock()}) {
            path.ifPresent(result::add);
        }
        return result;
    }

    public List<Path> getOutputAllExistent() {
        return getOutputAll().stream().filter(Files::exists).collect(Collectors.toList());
    }

    public int getErrorPreviewLines() {
        return errorPreviewLines;
    }

    public void setErrorPreviewLines(int errorPreviewLines) {
        this.errorPreviewLines = errorPreviewLines;
    }

    public Set<String> getClassesIgnore() {
        return classesIgnore;
    }

    public void setClassesIgnore(Set<String> classesIgnore) {
        this.classesIgnore = classesIgnore;
    }

    public boolean isGenerateFlow() {
        return generateFlow;
    }

    public void setGenerateFlow(boolean generateFlow) {
        this.generateFlow = generateFlow;
    }

    private void createReport() {
        final Path outputJsonRaw = getOutputJsonRaw();
        if (!Files.exists(outputJsonRaw)) {
            outputJsonRaw.getParent().toFile().mkdirs();
            try {
                Files.createFile(outputJsonRaw);
            } catch (IOException e) {
                throw new WiserExtensionException("Could not create file [" + outputJsonRaw + "]", e);
            }
        }
    }

    public ObjectMapper getMapperJson() {
        return MAPPER_JSON;
    }

    public ObjectMapper getMapperYaml() {
        return MAPPER_YAML;
    }

    @SuppressWarnings("BusyWait")
    public synchronized void tryUnlock(final Consumer<Path> supplier) {
        final Path outputLock = getOutputLockRaw();
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
            supplier.accept(getOutputJsonRaw());
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
        if (generateNew) {
            for (Path path : getOutputAllExistent()) {
                deleteFile(path);
            }
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
