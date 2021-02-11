package berlin.yuna.wiserjunit.model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static berlin.yuna.wiserjunit.model.TestCase.toPrettyDuration;

@SuppressWarnings({"unused"})
public class Report {

    private MetaData metaData = new MetaData();
    private TestCaseNode testCases = new TestCaseNode();

    public Report calculate() {
        final int notFailed = (int) stream().filter(TestCase::isSuccess).count();
        metaData.testCases = testCases.size();
        metaData.testCasesDisabled = (int) stream().filter(TestCase::isDisabled).count();
        metaData.testCasesSucceed = notFailed - metaData.testCasesDisabled;
        metaData.testCasesFailed = testCases.size() - notFailed;
        metaData.percentageSucceed = ((metaData.testCasesSucceed * 100f) / (Math.max(testCases.size(), 1)));
        metaData.percentageDisabled = ((metaData.testCasesFailed * 100f) / (Math.max(testCases.size(), 1)));
        metaData.percentageDisabled = ((metaData.testCasesDisabled * 100f) / (Math.max(testCases.size(), 1)));
        metaData.updated = nowUtc();
        metaData.ended = System.currentTimeMillis();
        metaData.duration = stream().mapToLong(TestCase::getDuration).sum();
        metaData.durationPretty = toPrettyDuration(metaData.duration);
        return this;
    }

    public MetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(final MetaData metaData) {
        this.metaData = metaData;
    }

    public TestCaseNode getTestCases() {
        return testCases;
    }

    public void setTestCases(final TestCaseNode testCases) {
        this.testCases = testCases;
    }

    public Stream<TestCase> stream() {
        return testCases.stream();
    }

    public void forEach(final Consumer<TestCase> action) {
        testCases.forEach(action);
    }

    public int size() {
        return testCases.size();
    }

    public void add(final TestCase testCase) {
        testCases.add(testCase);
    }

    public boolean remove(final TestCase testCase) {
        return testCases.remove(testCase);
    }

    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneId.of("UTC"));
    }

    public static class MetaData {

        private int testCases;
        private int testCasesFailed;
        private int testCasesSucceed;
        private int testCasesDisabled;
        private float percentageSucceed;
        private float percentageDisabled;
        private float percentageFailed;
        private LocalDateTime created = nowUtc();
        private long started = System.currentTimeMillis();
        private LocalDateTime updated = nowUtc();
        private long ended = System.currentTimeMillis();
        private String durationPretty;
        private long duration;

        public int getTestCases() {
            return testCases;
        }

        public void setTestCases(final int testCases) {
            this.testCases = testCases;
        }

        public int getTestCasesFailed() {
            return testCasesFailed;
        }

        public void setTestCasesFailed(final int testCasesFailed) {
            this.testCasesFailed = testCasesFailed;
        }

        public int getTestCasesSucceed() {
            return testCasesSucceed;
        }

        public void setTestCasesSucceed(final int testCasesSucceed) {
            this.testCasesSucceed = testCasesSucceed;
        }

        public float getPercentageSucceed() {
            return percentageSucceed;
        }

        public void setPercentageSucceed(final float percentageSucceed) {
            this.percentageSucceed = percentageSucceed;
        }

        public LocalDateTime getCreated() {
            return created;
        }

        public void setCreated(final LocalDateTime created) {
            this.created = created;
        }

        public long getStarted() {
            return started;
        }

        public void setStarted(final long started) {
            this.started = started;
        }

        public LocalDateTime getUpdated() {
            return updated;
        }

        public void setUpdated(final LocalDateTime updated) {
            this.updated = updated;
        }

        public long getEnded() {
            return ended;
        }

        public void setEnded(final long ended) {
            this.ended = ended;
        }

        public String getDurationPretty() {
            return durationPretty;
        }

        public void setDurationPretty(final String durationPretty) {
            this.durationPretty = durationPretty;
        }

        public long getDuration() {
            return duration;
        }

        public void setDuration(final long duration) {
            this.duration = duration;
        }

        public int getTestCasesDisabled() {
            return testCasesDisabled;
        }

        public void setTestCasesDisabled(final int testCasesDisabled) {
            this.testCasesDisabled = testCasesDisabled;
        }

        public float getPercentageDisabled() {
            return percentageDisabled;
        }

        public void setPercentageDisabled(float percentageDisabled) {
            this.percentageDisabled = percentageDisabled;
        }

        public float getPercentageFailed() {
            return percentageFailed;
        }

        public void setPercentageFailed(float percentageFailed) {
            this.percentageFailed = percentageFailed;
        }
    }
}
