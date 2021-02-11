package berlin.yuna.wiserjunit.model;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Stream;

@SuppressWarnings({"unused"})
public class TestCaseNode implements Comparable<TestCaseNode> {

    private String name = "";
    private Set<TestCase> testCases = new TreeSet<>();
    private Set<TestCaseNode> children = new TreeSet<>();

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Set<TestCaseNode> getChildren() {
        return children;
    }

    public void setChildren(final Set<TestCaseNode> children) {
        this.children = new TreeSet<>(children);
    }

    public void addChildNode(final TestCaseNode node) {
        children.add(node);
    }

    public void add(final TestCase testCase) {
        testCases.add(testCase);
    }

    public boolean remove(final TestCase testCase) {
        return testCases.remove(testCase);
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

    public Set<TestCase> getTestCases() {
        return testCases;
    }

    public void setTestCases(final Set<TestCase> testCases) {
        this.testCases = new TreeSet<>(testCases);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestCaseNode that = (TestCaseNode) o;

        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "TestCaseNode{" +
                "name='" + name + '\'' +
                ", testCases=" + testCases.size() +
                ", children=" + children.size() +
                '}';
    }

    @Override
    public int compareTo(final TestCaseNode o) {
        return String.CASE_INSENSITIVE_ORDER.compare(name, o.name);
    }
}
