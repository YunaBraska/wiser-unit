package berlin.yuna.wiserjunit.logic;

import berlin.yuna.wiserjunit.config.WiserJunitConfig;
import berlin.yuna.wiserjunit.model.Report;
import berlin.yuna.wiserjunit.model.TestCase;
import berlin.yuna.wiserjunit.model.exception.BddException;
import berlin.yuna.wiserjunit.model.exception.WiserExtensionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.junit.platform.commons.util.AnnotationUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import static berlin.yuna.wiserjunit.config.WiserJunitConfig.MAPPER_YAML;
import static berlin.yuna.wiserjunit.logic.FileUtils.readFile;
import static berlin.yuna.wiserjunit.logic.FileUtils.readLine;
import static berlin.yuna.wiserjunit.logic.FileUtils.removeExtension;
import static berlin.yuna.wiserjunit.logic.ReportGeneratorCsv.generateCsv;
import static berlin.yuna.wiserjunit.logic.ReportGeneratorHtml.generateHtml;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;

@SuppressWarnings("ALL")
public class WiserReportExtension implements BeforeAllCallback, BeforeTestExecutionCallback, AfterTestExecutionCallback, AfterAllCallback, TestWatcher {

    private static final String TEST_CASES = "TEST_CASE";
    private static final String USER_DIR = getProperty("user.dir");
    private static final WiserJunitConfig CONFIG = readFile(Paths.get(USER_DIR, "wiser_report.yaml"), WiserJunitConfig.class, MAPPER_YAML).orElseGet(WiserJunitConfig::new);
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create("berlin", "yuna", "WISER_REPORT");

    @Override
    public void testDisabled(final ExtensionContext context, final Optional<String> reason) {
        final Optional<Method> testMethod = context.getTestMethod();
        testMethod.ifPresent(method -> {
            final TestCase testCase = toTestCase(context, method);
            reason.ifPresent(testCase::setPreviewText);
            testCase.setTimeEnd(currentTimeMillis());
            saveTestCase(testCase);
        });
        if (!testMethod.isPresent()) {
            final TestCase testCase = new TestCase();
            testCase.setDisabled(true);
            testCase.setNameTest(upperCaseFirst(context.getDisplayName()));
            testCase.setNameClass(context.getRequiredTestClass().getSimpleName());
            testCase.setNameDisplay(upperCaseFirst(context.getDisplayName()));
            testCase.setPkg(context.getRequiredTestClass().getPackage().getName());
            testCase.setTags(new TreeSet<>(context.getTags()));
            saveTestCase(testCase);
        }
    }

    @Override
    public void beforeAll(final ExtensionContext context) {
        //Do nothing
    }

    @Override
    public void beforeTestExecution(final ExtensionContext context) {
        context.getTestMethod().ifPresent(method -> context.getStore(NAMESPACE).put(TEST_CASES, toTestCase(context, method)));
    }

    @Override
    public void afterTestExecution(final ExtensionContext context) {
        context.getTestMethod().ifPresent(method -> {
            final TestCase testCase = context.getStore(NAMESPACE).get(TEST_CASES, TestCase.class);
            final Optional<Path> classPath = FileUtils.getPhysicalPath(Paths.get(CONFIG.getProjectDir()), method.getDeclaringClass());
            testCase.setTimeEnd(currentTimeMillis());
            testCase.setDisabled(isDisabled(context));
            context.getExecutionException().ifPresent(throwable -> {
                testCase.setSuccess(false);
                testCase.setErrorType(throwable.getClass().getSimpleName());
                if (classPath.isPresent() && CONFIG.getErrorPreviewLines() > 0) {
                    testCase.setPreviewText(getErrorPreviewLines(classPath.get(), throwable.getStackTrace()));
                }
                testCase.setErrorLine(getErrorLine(classPath.get(), throwable.getStackTrace()));
                setErrorMessage(testCase, throwable);
            });
            if (CONFIG.isGenerateFlow() && testCase.getBddMsgList().isEmpty() && !context.getExecutionException().isPresent() && classPath.isPresent()) {
                FlowParser.parseFlowFromFile(method, classPath.get()).forEach(testCase::addBddMeg);
                testCase.setBddText(String.join("", testCase.getBddMsgList()));
            }
            saveTestCase(testCase);
        });
    }


    private void setErrorMessage(final TestCase testCase, final Throwable throwable) {
        if (throwable instanceof BddException) {
            final BddException bddException = (BddException) throwable;
            testCase.setBddMsgList(bddException.getMessages());
            testCase.setBddText(bddException.getMessage());
            testCase.setErrorMsg(bddException.getCause() != null ? bddException.getCause().getMessage() : bddException.getMessage());
        } else {
            testCase.setErrorMsg(throwable.getMessage());
        }
    }

    private String getErrorPreviewLines(final Path path, final StackTraceElement... stackTraceElements) {
        final String fileName = removeExtension(path.getFileName().toString());
        for (StackTraceElement trace : stackTraceElements) {
            if (fileName.equalsIgnoreCase(removeExtension(trace.getFileName()))) {
                return readLine(path, trace.getLineNumber(), CONFIG.getErrorPreviewLines()).map(String::trim).orElse("");
            }
        }
        return "";
    }

    private int getErrorLine(final Path path, final StackTraceElement... stackTraceElements) {
        final String fileName = path == null ? "#InvalidFileName#" : path.getFileName().toString();
        for (StackTraceElement trace : stackTraceElements) {
            if (fileName.equalsIgnoreCase(removeExtension(trace.getFileName()))) {
                return trace.getLineNumber();
            }
        }
        return -1;
    }

    @Override
    public void afterAll(final ExtensionContext context) {
        writeToFile();
    }

    private boolean isDisabled(final ExtensionContext context) {
        return context.getElement()
                .map(el -> AnnotationUtils.isAnnotated(el, Disabled.class))
                .orElse(false);
    }

    private void saveTestCase(final TestCase testCase) {
        final Report caseList = getTestCaseList();
        caseList.remove(testCase);
        caseList.add(testCase);
        CONFIG.tryUnlock(path -> {
                    try {
                        CONFIG.getMapperJson().writeValue(path.toFile(), caseList);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    private static synchronized void writeToFile() {
        CONFIG.tryUnlock(output -> {
            try {
                output.getParent().toFile().mkdirs();
                final Report report = getTestCaseList().calculate();
                CONFIG.getOutputJson().ifPresent(target -> writeReport(report, target, CONFIG.getMapperJson()));
                CONFIG.getOutputYaml().ifPresent(target -> writeReport(report, target, CONFIG.getMapperYaml()));
                CONFIG.getOutputCsv().ifPresent(target -> generateCsv(report, target));
                CONFIG.getOutputHtml().ifPresent(target -> generateHtml(report, target, CONFIG));
            } catch (Exception e) {
                throw prepareIoException(e, output);
            }
        });
    }

    private static void writeReport(final Report report, final Path target, final ObjectMapper mapper) {
        try {
            mapper.writer().withDefaultPrettyPrinter().writeValue(target.toFile(), report);
        } catch (Exception e) {
            throw prepareIoException(e, target);
        }
    }

    private static synchronized Report getTestCaseList() {
        final AtomicReference<Report> testCases = new AtomicReference<>();
        CONFIG.tryUnlock(path -> testCases.set(readFile(path, Report.class, CONFIG.getMapperJson()).orElseGet(Report::new)));
        return testCases.get();
    }


    private static TestCase toTestCase(final ExtensionContext context, final Method method) {
        final String displayName = context.getElement().flatMap(el -> AnnotationUtils.findAnnotation(el, DisplayName.class).map(DisplayName::value)).orElseGet(context::getDisplayName);
        TestCase testCase = new TestCase();
        testCase.setPkg(method.getDeclaringClass().getPackage().getName());
        testCase.setNameClass(method.getDeclaringClass().getSimpleName());
        testCase.setNameDisplay(upperCaseFirst(displayName).replace("()", ""));
        testCase.setNameTest(upperCaseFirst(method.getName()));
        testCase.setTags(new TreeSet<>(context.getTags()));
        return testCase;
    }

    private static String upperCaseFirst(final String input) {
        return input.length() < 1 ? input
                : input.substring(0, 1).toUpperCase() + input.substring(1);
    }


    static WiserExtensionException prepareIoException(final Exception e, final Path output) {
        return new WiserExtensionException("Error while saving [ " + output + "]", e);
    }

    public static void writeFile(final Path path, final String... lines) {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (String line : lines) {
                for (String ln : line.split("[\\r\\n]+")) {
                    writer.append(ln);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}