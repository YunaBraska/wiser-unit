package berlin.yuna.wiserjunit.logic;

import berlin.yuna.wiserjunit.model.Report;
import berlin.yuna.wiserjunit.model.TestCase;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static berlin.yuna.wiserjunit.logic.WiserReportExtension.prepareIoException;
import static berlin.yuna.wiserjunit.logic.WiserReportExtension.writeFile;
import static berlin.yuna.wiserjunit.model.TestCase.testCaseGroupSorted;

public class ReportGeneratorCsv {

    private ReportGeneratorCsv() {
    }

    public static void generateCsv(final Report report, final Path target) {
        final StringBuilder csv = new StringBuilder();
        final List<TestCase> testCases = report.stream().sorted(testCaseGroupSorted()).collect(Collectors.toList());
        csv.append("Execution;Success;Failed;Disabled;Id;Group;Pkg;ClassName;DisplayName;TestName;Flow;ErrorMessage;" +
                "ErrorPreview;ErrorLine;ErrorType;DurationMs;DurationPretty;StartMs;EndMs;");
        csv.append(System.lineSeparator());
        testCases.forEach(testCase -> {
            csv.append(escapeCsv(testCase.isFailed() ? "FAILED" : testCase.isDisabled() ? "DISABLED" : "SUCCESS"));
            csv.append(escapeCsv(testCase.isSuccess()));
            csv.append(escapeCsv(testCase.isFailed()));
            csv.append(escapeCsv(testCase.isDisabled()));
            csv.append(escapeCsv(testCase.getId()));
            csv.append(escapeCsv(testCase.getGroup()));
            csv.append(escapeCsv(testCase.getPkg()));
            csv.append(escapeCsv(testCase.getNameClass()));
            csv.append(escapeCsv(testCase.getNameDisplay()));
            csv.append(escapeCsv(testCase.getNameTest()));
            csv.append(escapeCsv(testCase.getBddText()));
            csv.append(escapeCsv(testCase.getErrorMsg()));
            csv.append(escapeCsv(testCase.getPreviewText()));
            csv.append(escapeCsv(testCase.getErrorLine()));
            csv.append(escapeCsv(testCase.getErrorType()));
            csv.append(escapeCsv(testCase.getDuration()));
            csv.append(escapeCsv(testCase.getDurationPretty()));
            csv.append(escapeCsv(testCase.getTimeStart()));
            csv.append(escapeCsv(testCase.getTimeEnd()));
            csv.append(System.lineSeparator());
        });
        try {
            writeFile(target, csv.toString());
        } catch (Exception e) {
            throw prepareIoException(e, target);
        }
    }

    private static String escapeCsv(final long input) {
        return escapeCsv("" + input);
    }

    private static String escapeCsv(final boolean input) {
        return escapeCsv("" + input);
    }

    private static String escapeCsv(final String input) {
        return "\"" + (
                input == null ? "" : input.trim()
                .replace("\"", "\"\"")
                .replace("\n", "\t")
                .replace("\r", "\t")
        ) + "\";";
    }
}