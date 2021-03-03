package berlin.yuna.wiserjunit.model;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;

@SuppressWarnings("unused")
public class TestCase implements Comparable<TestCase> {
    private String id;
    private String group;
    private String pkg;
    private TreeSet<String> tags;
    private String nameDisplay;
    private String nameClass;
    private String nameTest;
    private List<String> bddMsgList = new ArrayList<>();
    private String errorMsg = "";
    private String previewText = "";
    private String bddText = "";
    private long errorLine = -1;
    private String errorType = "";
    private String durationPretty = "";
    private boolean success = true;
    private boolean disabled = true;
    private long duration = -1;
    private long timeStart = currentTimeMillis();
    private long timeEnd = -1;
    public static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("0.00");

    public void setId(String id) {
        this.id = id;
    }

    public String getPkg() {
        return pkg;
    }

    public void setPkg(String pkg) {
        this.pkg = pkg;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(final TreeSet<String> tags) {
        this.tags = tags;
    }

    public String getNameDisplay() {
        return nameDisplay;
    }

    public void setNameDisplay(String nameDisplay) {
        this.nameDisplay = nameDisplay;
    }

    public String getNameClass() {
        return nameClass;
    }

    public void setNameClass(String nameClass) {
        this.nameClass = nameClass;
    }

    public String getNameTest() {
        return nameTest;
    }

    public void setNameTest(String nameTest) {
        this.nameTest = nameTest;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getPreviewText() {
        return previewText;
    }

    public void setPreviewText(String previewText) {
        this.previewText = previewText;
    }

    public long getErrorLine() {
        return errorLine;
    }

    public void setErrorLine(long errorLine) {
        this.errorLine = errorLine;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailed() {
        return !isSuccess();
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public long getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(long timeStart) {
        this.timeStart = timeStart;
    }

    public long getTimeEnd() {
        return timeEnd;
    }

    public String getDurationPretty() {
        return durationPretty;
    }

    public void setDurationPretty(String durationPretty) {
        this.durationPretty = durationPretty;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public List<String> getBddMsgList() {
        return bddMsgList;
    }

    public void setBddMsgList(List<String> bddMsgList) {
        this.bddMsgList = bddMsgList;
    }

    public void addBddMeg(final String errorMsg) {
        bddMsgList.add(errorMsg);
    }

    public boolean isDisabled() {
        return disabled;
    }

    public boolean isNotDisabled() {
        return !isDisabled();
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public void setTimeEnd(long timeEnd) {
        this.timeEnd = timeEnd;
        this.duration = this.timeEnd - this.timeStart;
        this.durationPretty = toPrettyDuration(duration);
    }

    public String getId() {
        if (id == null) {
            id = format(
                    "%s.%s.%s.%s",
                    pkg,
                    nameClass,
                    tags.isEmpty() ? "default" : String.join(".", tags),
                    nameTest
            );
        }
        return id;
    }

    public void setGroup(final String group) {
        this.group = group;
    }

    public String getGroup() {
        if (group == null) {
            group = tags.isEmpty() ? nameClass : String.join(".", tags) + "." + nameTest;
        }
        return group;
    }

    public static String toPrettyDuration(final long duration) {
        return duration < 1000
                ? duration + "ms"
                : duration < 60000
                ? DECIMAL_FORMATTER.format(duration / 60f) + "s"
                : duration < 3600000
                ? DECIMAL_FORMATTER.format(duration / 60f / 60f) + "m"
                : duration < 86400000
                ? DECIMAL_FORMATTER.format(duration / 60f / 60f / 24f) + "h"
                : duration < 604800000
                ? DECIMAL_FORMATTER.format(duration / 60f / 60f / 24f / 7f) + "d"
                : DECIMAL_FORMATTER.format(duration / 60f / 60f / 24f / 7f / 30) + "w";
    }

    public String getBddText() {
        return bddText;
    }

    public void setBddText(String bddText) {
        this.bddText = bddText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestCase testCase = (TestCase) o;

        return getId() != null ? getId().equals(testCase.getId()) : testCase.getId() == null;
    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
    }

    @Override
    public String toString() {
        return "TestCase{" +
                "id='" + id + '\'' +
                ", timeStart=" + timeStart +
                ", timeEnd=" + timeEnd +
                '}';
    }

    @Override
    public int compareTo(final TestCase o) {
        return String.CASE_INSENSITIVE_ORDER.compare(getId(), o.getId());
    }

    public static Comparator<TestCase> testCaseGroupSorted() {
        return (o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getGroup(), o2.getGroup());
    }
}
