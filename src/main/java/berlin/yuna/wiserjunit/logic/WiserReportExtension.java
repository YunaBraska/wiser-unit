package berlin.yuna.wiserjunit.logic;

import berlin.yuna.wiserjunit.config.WiserJunitConfig;
import berlin.yuna.wiserjunit.model.Report;
import berlin.yuna.wiserjunit.model.TestCase;
import berlin.yuna.wiserjunit.model.TestCaseNode;
import berlin.yuna.wiserjunit.model.exception.BddException;
import berlin.yuna.wiserjunit.model.exception.WiserExtensionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
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
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static berlin.yuna.wiserjunit.config.WiserJunitConfig.MAPPER_YAML;
import static berlin.yuna.wiserjunit.model.Report.nowUtc;
import static berlin.yuna.wiserjunit.model.TestCase.DECIMAL_FORMATTER;
import static berlin.yuna.wiserjunit.model.TestCase.testCaseGroupSorted;
import static java.io.File.separatorChar;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static java.lang.System.lineSeparator;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.util.Collections.reverseOrder;
import static java.util.Objects.requireNonNull;

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
            reason.ifPresent(testCase::setErrorPreview);
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
    //TODO: detect skipped/ignored tests && add to html progress bar
    public void beforeTestExecution(final ExtensionContext context) {
        context.getTestMethod().ifPresent(method -> context.getStore(NAMESPACE).put(TEST_CASES, toTestCase(context, method)));
    }

    @Override
    public void afterTestExecution(final ExtensionContext context) {
        context.getTestMethod().ifPresent(method -> {
            final TestCase testCase = context.getStore(NAMESPACE).get(TEST_CASES, TestCase.class);
            testCase.setTimeEnd(currentTimeMillis());
            testCase.setDisabled(isDisabled(context));
            context.getExecutionException().ifPresent(throwable -> {
                testCase.setSuccess(false);
                testCase.setErrorMsg(throwable.getMessage());
                testCase.setErrorType(throwable.getClass().getSimpleName());
                if (CONFIG.getErrorPreviewLines() > 0) {
                    testCase.setErrorPreview(getErrorPreviewLines(throwable));
                }
                testCase.setErrorLine(getErrorLine(throwable));
                getErrorPreviewLines(throwable);
                if (throwable instanceof BddException) {
                    testCase.setErrorMsgList(((BddException) throwable).getMessages());
                }
            });
            saveTestCase(testCase);
        });
    }

    private String getErrorPreviewLines(final Throwable throwable) {
        final AtomicReference<String> result = new AtomicReference<>(null);
        for (StackTraceElement trace : throwable.getStackTrace()) {
            if (trace.getFileName() != null && trace.getFileName().contains(".")
                    && CONFIG.getClassesIgnore().contains(trace.getFileName().split("\\.")[0])
            ) {
                continue;
            }
            final String filePath = trace.getClassName().replace('.', separatorChar) + ".java";
            try (Stream<Path> walk = Files.walk(Paths.get(CONFIG.getProjectDir()), 4)) {
                walk.filter(Files::isDirectory)
                        .filter(path -> Files.exists(Paths.get(path.toString(), filePath))).findFirst()
                        .map(path -> Paths.get(path.toString(), filePath)).ifPresent(file -> {
                    try (Stream<String> lines = Files.lines(file)) {
                        int start = Math.max(trace.getLineNumber() - (CONFIG.getErrorPreviewLines() / 2), 1) - 1;
                        result.set(lines.skip(start).limit(CONFIG.getErrorPreviewLines()).collect(Collectors.joining(lineSeparator())));
                    } catch (IOException ignored) {
                    }

                });
            } catch (IOException ignored) {
            }
            if (result.get() != null) {
                return result.get().trim();
            }
        }
        return "";
    }

    private int getErrorLine(final Throwable throwable) {
        final AtomicReference<Integer> result = new AtomicReference<>(null);
        for (StackTraceElement trace : throwable.getStackTrace()) {
            if (trace.getFileName() != null && trace.getFileName().contains(".")
                    && CONFIG.getClassesIgnore().contains(trace.getFileName().split("\\.")[0])
            ) {
                continue;
            }
            final String filePath = trace.getClassName().replace('.', separatorChar) + ".java";
            try (Stream<Path> walk = Files.walk(Paths.get(CONFIG.getProjectDir()), 4)) {
                walk.filter(Files::isDirectory)
                        .filter(path -> Files.exists(Paths.get(path.toString(), filePath))).findFirst()
                        .map(path -> Paths.get(path.toString(), filePath)).ifPresent(file -> result.set(trace.getLineNumber()));
            } catch (IOException ignored) {
            }
            if (result.get() != null) {
                return result.get();
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
                        CONFIG.getMapper().writeValue(path.toFile(), caseList);
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
                validatePath(output).ifPresent(target -> writeReport(report, target));
                validatePath(CONFIG.getOutputNested()).ifPresent(target -> writeReport(tree(report), target));
                validatePath(CONFIG.getOutputHtml()).ifPresent(target -> generateHtml(report, target));
            } catch (Exception e) {
                throw prepareIoException(e, CONFIG.getOutput());
            }
        });
    }

    private static void writeReport(final Report report, final Path target) {
        try {
            CONFIG.getMapper().writer().withDefaultPrettyPrinter().writeValue(target.toFile(), report);
        } catch (Exception e) {
            throw prepareIoException(e, target);
        }
    }

    //FIXME: too ugly implemented :(
    @SuppressWarnings({"java:S1192", "StringConcatenationInsideStringBufferAppend"})
    private static void generateHtml(final Report report, final Path target) {
        final StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n<title>WiserReport</title>" + CSS + "\n</head>\n<body>");

        html.append("<table style=\"text-align: center;\">\n<tr>\n");
        html.append("<th class=\"fit\">Name</th>\n");
        html.append("<th class=\"fit\">Test cases</th>\n");
        html.append("<th class=\"fit\">Duration</th>\n");
        html.append("<th>Progress</th>\n");
        html.append("<th class=\"fit\">Success</th>\n");
        html.append("<th class=\"fit\">Percentage</th>\n");
        html.append("<th class=\"fit\">Date</th>\n");
        html.append("</tr><tr>\n");
        html.append("<td class=\"fit\">").append(CONFIG.getName()).append("</td>\n");
        html.append("<td class=\"fit\">").append(report.getMetaData().getTestCases()).append("</td>\n");
        html.append("<td class=\"fit\">").append(report.getMetaData().getDurationPretty()).append("</td>\n");
        html.append("<td>\n<div class=\"bar_wrapper\">\n<div title=\"Failed\" class=\"bar_bg\">\n" +
                " <span class=\"bar_success\" title=\"Success\" style=\"width: " + report.getMetaData().getPercentageSucceed() + "%;\"></span>\n" +
                " <span class=\"bar_disabled\" title=\"Disabled\" style=\"width: " + (report.getMetaData().getPercentageSucceed() + report.getMetaData().getPercentageDisabled()) + "%;\"></span>\n" +
                "</div>\n</div>\n</td>\n");
        html.append("<td class=\"fit\">").append(report.getMetaData().getTestCasesSucceed()).append("/").append(report.getMetaData().getTestCases()).append("</td>\n");
        html.append("<td class=\"fit\">").append(DECIMAL_FORMATTER.format(report.getMetaData().getPercentageSucceed() - report.getMetaData().getPercentageDisabled())).append("%</td>\n");
        html.append("<td class=\"fit\">").append(ISO_LOCAL_DATE_TIME.format(nowUtc()).replace("T", " <br>"));
        html.append("</tr>\n</table style=\"text-align: center;\">\n");
        html.append("<table>\n");
        html.append("<tr>\n");
        html.append("<th></th>\n");
        html.append("<th>Tags</th>\n");
        html.append("<th>Display Name</th>\n");
        html.append("<th>Duration</th>\n");
        html.append("<th>Error</th>\n");
        html.append("<th>Line Preview</th>\n");
        html.append("</tr>\n");

        report.stream().sorted(testCaseGroupSorted()).forEach(testCase -> {
            html.append("<tr>\n");
            html.append("<td class=\"").append(
                    testCase.isDisabled() ? "disabled" :
                            testCase.getErrorType().length() > 2 ? "failed" : "success"
            ).append("\">&#183;</td>\n");
            html.append("<td>").append(String.join(", ", testCase.getTags())).append("</td>\n");
            html.append("<td>").append(testCase.getNameDisplay()).append("</td>\n");
            html.append("<td style=\"text-align: right; padding-right: 2%;\">").append(testCase.getDurationPretty()).append("</td>\n");
            html.append("<td>").append(toHtml(testCase.getErrorMsg())).append("</td>\n");
            html.append("<td>").append(toHtml(testCase.getErrorPreview().trim())
            ).append("</td>\n");
            html.append("</tr>\n");

        });
        html.append("</table>\n");
        html.append("<table style=\"text-align: center;\">\n<tr>\n");
        validatePath(CONFIG.getOutput()).ifPresent(path -> html.append("<td>").append("<a title=\"Report\" href=\"" + path.getFileName().toString() + "\">" + path.getFileName().toString() + "</a>").append("</td>\n"));
        validatePath(CONFIG.getOutputNested()).ifPresent(path -> html.append("<td>").append("<a title=\"ReportNested\" href=\"" + path.getFileName().toString() + "\">" + path.getFileName().toString() + "</a>").append("</td>\n"));
        validatePath(CONFIG.getOutputHtml()).ifPresent(path -> html.append("<td>").append("<a title=\"HtmlReport\" href=\"" + path.getFileName().toString() + "\">" + path.getFileName().toString() + "</a>").append("</td>\n"));
        html.append("</tr>\n</table>\n");
        html.append("</body>\n</html>");
        try {
            writeFile(target, html.toString());
        } catch (Exception e) {
            throw prepareIoException(e, target);
        }
    }

    private static String toHtml(final String input) {
        return input.trim()
                .replace("\n", "<br>")
                .replace("\r", "<br>");
    }

    private static Optional<Path> validatePath(final Path path) {
        return Optional.ofNullable(path != null && path.getParent() != null && Files.exists(path.getParent()) ? path : null);
    }

    //FIXME: too ugly implemented :(
    @SuppressWarnings("java:S1192")
    final static String CSS = "<style>\n" +
            "*{\n" +
            "font-family:arial,helvetica;\n" +
            "}\n" +
            "body{\n" +
            "background-color: #e0e0e0;\n" +
            "}\n" +
            ".bar_wrapper {\n" +
            "width: 100%;\n" +
            "background-color: #e0e0e0;\n" +
            "padding: 2px;\n" +
            "border-radius: 2px;\n" +
            "box-shadow: inset 0 1px 3px rgba(0, 0, 0, .2);\n" +
            "}\n" +
            ".bar_bg {\n" +
            "height: 10px;\n" +
            "width: 100%;\n" +
            "position: relative;\n" +
            "background-color: #db6f6f;\n" +
            "}\n" +
            ".bar_success,\n" +
            ".bar_disabled {\n" +
            "display: block;\n" +
            "height: 10px;\n" +
            "top: 0;\n" +
            "left: 0;\n" +
            "position: absolute;\n" +
            "transition: width 500ms ease-in-out;\n" +
            "}\n" +
            ".bar_success {\n" +
            "background-color: #B2FF59;\n" +
            "z-index: 10;\n" +
            "}\n" +
            ".bar_disabled {\n" +
            "background-color: #F4FF81;\n" +
            "z-index: 1;\n" +
            "}\n" +
            ".disabled {\n" +
            "font-size: 18px;\n" +
            "color: #F4FF81;\n" +
            "}\n" +
            ".success {\n" +
            "font-size: 18px;\n" +
            "color: #B2FF59;\n" +
            "}\n" +
            ".failed {\n" +
            "font-size: 18px;\n" +
            "color: #db6f6f;\n" +
            "}\n" +
            ".failed,\n" +
            ".disabled,\n" +
            ".success {\n" +
            "font-size: 64px;\n" +
            "height: 32px;\n" +
            "max-height: 32px;\n" +
            "line-height: 32px;\n" +
            "text-shadow: 0 1px 3px rgba(0, 0, 0, 0.5);\n" +
            "}\n" +
            ".fit {\n" +
            "width: 1%;\n" +
            "white-space: nowrap;\n" +
            "}\n" +
            "table {\n" +
            "padding: 15px;\n" +
            "background-color: #f6f7f8;\n" +
            "margin-bottom: 20px;\n" +
            "width: 100%;\n" +
            "box-shadow: 0 1px 3px rgba(0, 0, 0, 0.5);\n" +
            "}\n" +
            "table td {\n" +
            "padding: 5px;\n" +
            "}\n" +
            "</style>";

    private static Report tree(final Report report) {
        final Map<String, TestCaseNode> groups = new TreeMap<>(reverseOrder());
        final TestCaseNode root = new TestCaseNode();
        root.setName("/");
        report.getTestCases().forEach(testCase -> {
            String[] split = testCase.getGroup().split("\\.");
            TestCaseNode parent = null;
            for (int i = 0; i < split.length - 1; i++) {
                String groupName = split[i];
                final TestCaseNode currentNode = groups.getOrDefault(groupName, new TestCaseNode());
                currentNode.setName(groupName);
                if (!groups.containsKey(groupName)) {
                    if (parent == null) {
                        root.getChildren().add(currentNode);
                    } else {
                        parent.addChildNode(currentNode);
                    }
                    groups.put(groupName, currentNode);
                }
                parent = currentNode;
            }
            requireNonNull(parent).add(testCase);
        });
        final Report result = new Report();
        result.setTestCases(root);
        result.setMetaData(report.getMetaData());
        return result;
    }

    private static synchronized Report getTestCaseList() {
        final AtomicReference<Report> testCases = new AtomicReference<>();
        CONFIG.tryUnlock(path -> testCases.set(readFile(path, Report.class, CONFIG.getMapper()).orElseGet(Report::new)));
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


    private static <T> Optional<T> readFile(final Path file, final Class<T> type, final ObjectMapper mapperYaml) {
        try {
            if (Files.exists(file) && Files.isRegularFile(file)) {
                return Optional.ofNullable(mapperYaml.readValue(file.toFile(), type));
            }
        } catch (MismatchedInputException e) {
            if (!e.getMessage().contains("No content")) {
                throw new WiserExtensionException("Error while reading file [" + file + "] " + e.getMessage());
            }
        } catch (IOException ignored) {
        }
        return Optional.empty();
    }


    private static WiserExtensionException prepareIoException(final Exception e, final Path output) {
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