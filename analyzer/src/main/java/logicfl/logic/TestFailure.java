package logicfl.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@code test_failure/3} predicate.
 *
 * {@code test_failure(?failure_id, ?test_class, ?test_method).}
 *
 * <ul>
 *  <li>failure_id: the id of test failure.</li>
 *  <li>test_class: the name of test class.</li>
 *  <li>test_method: the name of test method.</li>
 * </ul>
 */
public class TestFailure extends Predicate {

    public static final String PREFIX = "failure_";
    private String failureId;
    private String testClass;
    private String testMethod;
    private List<Trace> traces;

    public TestFailure(String failureId, String testClass, String testMethod) {
        this.failureId = failureId;
        this.testClass = testClass;
        this.testMethod = testMethod;
        this.traces = new ArrayList<>();
    }

    public String getFailureId() {
        return failureId;
    }

    public void setFailureId(String failureId) {
        this.failureId = failureId;
    }

    public String getTestClass() {
        return testClass;
    }

    public void setTestClass(String classId) {
        this.testClass = classId;
    }

    public String getTestMethod() {
        return testMethod;
    }

    public void setTestMethod(String methodId) {
        this.testMethod = methodId;
    }

    public List<Trace> getTraces() {
        return traces;
    }

    public void addTrace(Trace trace) {
        traces.add(trace);
    }

    public String getPredicateName() {
        return "test_failure";
    }

    @Override
    public List<String> arguments() {
        return Arrays.asList(failureId, testClass, testMethod);
    }

    @Override
    public String createTerm() {
        String argStr = String.join(", ", failureId, FactManager.getQuotedString(testClass),
            FactManager.getQuotedString(testMethod));
        return String.join("", getPredicateName(), "(", argStr, ")");
    }
}
