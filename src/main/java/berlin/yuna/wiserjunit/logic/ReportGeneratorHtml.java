package berlin.yuna.wiserjunit.logic;

import berlin.yuna.wiserjunit.config.WiserJunitConfig;
import berlin.yuna.wiserjunit.model.Report;
import berlin.yuna.wiserjunit.model.TestCase;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static berlin.yuna.wiserjunit.logic.WiserReportExtension.prepareIoException;
import static berlin.yuna.wiserjunit.logic.WiserReportExtension.writeFile;
import static berlin.yuna.wiserjunit.model.Report.nowUtc;
import static berlin.yuna.wiserjunit.model.TestCase.DECIMAL_FORMATTER;
import static berlin.yuna.wiserjunit.model.TestCase.testCaseGroupSorted;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

public class ReportGeneratorHtml {

    private ReportGeneratorHtml() {
    }

    //FIXME: too ugly implemented :(
    @SuppressWarnings({"java:S1192", "StringConcatenationInsideStringBufferAppend"})
    public static void generateHtml(final Report report, final Path target, final WiserJunitConfig config) {
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
        html.append("<td class=\"fit\">").append(config.getName()).append("</td>\n");
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
        html.append("<th>Flow</th>\n");
        html.append("<th>Line Preview</th>\n");
        html.append("</tr>\n");

        final List<TestCase> testCases = report.stream().sorted(testCaseGroupSorted()).collect(Collectors.toList());
        testCases.stream().filter(TestCase::isFailed).forEach(testCase -> addTestCase(html, testCase));
        testCases.stream().filter(TestCase::isDisabled).forEach(testCase -> addTestCase(html, testCase));
        testCases.stream().filter(TestCase::isSuccess).filter(TestCase::isNotDisabled).forEach(testCase -> addTestCase(html, testCase));
        html.append("</table>\n");
        html.append("<table style=\"text-align: center;\">\n<tr>\n");
        for (Path output : config.getOutputAllExistent()) {
            html.append("<td>").append("<a title=\"Report\" href=\"" + output.getFileName().toString() + "\">" + output.getFileName().toString() + "</a>").append("</td>\n");
        }
        html.append("</tr>\n</table>\n");
        html.append("</body>\n</html>");
        try {
            writeFile(target, html.toString());
        } catch (Exception e) {
            throw prepareIoException(e, target);
        }
    }

    private static void addTestCase(StringBuilder html, TestCase testCase) {
        html.append("<tr>\n");
        html.append("<td class=\"").append(
                testCase.isDisabled() ? "disabled" :
                        testCase.getErrorType().length() > 2 ? "failed" : "success"
        ).append("\">&#183;</td>\n");
        html.append("<td>").append(String.join(", ", testCase.getTags())).append("</td>\n");
        html.append("<td>").append(testCase.getNameDisplay()).append("</td>\n");
        html.append("<td style=\"text-align: right; padding-right: 2%;\">").append(testCase.getDurationPretty()).append("</td>\n");
        html.append("<td>").append(toHtml(testCase.getBddText())).append("</td>\n");
        html.append("<td>").append(toHtml(testCase.getPreviewText().trim())
        ).append("</td>\n");
        html.append("</tr>\n");
    }

    private static String toHtml(final String input) {
        return escapeHTML(input).trim()
                .replace("\n", "<br>")
                .replace("\r", "<br>");
    }

    public static String escapeHTML(String s) {
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > 127 || c == '"' || c == '\'' || c == '<' || c == '>' || c == '&') {
                out.append("&#");
                out.append((int) c);
                out.append(';');
            } else {
                out.append(c);
            }
        }
        return out.toString();
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
}
