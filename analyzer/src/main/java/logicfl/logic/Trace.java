package logicfl.logic;

import java.util.Arrays;
import java.util.List;

import logicfl.logic.codefacts.Line;

/**
 * {@code trace/6} predicate.
 * {@code trace(?trace_id, ?parent_id, ?method_id, ?line:Line, ?failure_id, ?is_target).}
 * <ul>
 *  <li>trace_id: the id of this stack trace.</li>
 *  <li>parent_id: the id of the parent stack trace, which calls a method in this stack trace.</li>
 *  <li>method_id: the method id which this stack trace indicates.</li>
 *  <li>line: the line of this stack trace.</li>
 *  <li>failure_id: the id of test failure which this stack trace belongs.</li>
 *  <li>is_target: {@code target} indicates that the method can be found in code base, otherwise {@code non_target}.</li>
 * </ul>
 */
public class Trace extends Predicate {

    public static final String TARGET = "target";
    public static final String NON_TARGET = "non_target";
    public static final String PREFIX = "trace_";

    private String traceId;
    private String parentId;
    private String failureId;
    private String methodId;
    private Line line;
    private String isTarget;

    public Trace(String traceId, String parentId, String methodId, Line line, String failureId, boolean isTarget) {
        this.traceId = traceId;
        this.parentId = parentId;
        this.failureId = failureId;
        this.methodId = methodId;
        this.line = line;
        this.isTarget = isTarget ? TARGET : NON_TARGET;
    }

    public String getPredicateName() {
        return "trace";
    }

    @Override
    public List<String> arguments() {
        return Arrays.asList(traceId, parentId, methodId, line.toString(), failureId, isTarget);
    }
}
