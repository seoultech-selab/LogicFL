package logicfl.logic;

import java.util.Arrays;
import java.util.List;

import logicfl.logic.codefacts.Line;

/**
 * <p>
 * {@code throw/2} predicate.
 * {@code throw(?method, ?exception).}
 * </p>
 * <p>
 * {@code throw/3} predicate.
 * {@code throw(?method, ?exception, ?line:Line).}
 * </p>
 * <ul>
 *  <li>method: either the method name or the expression of the method invocation.</li>
 *  <li>exception: the name of the callee.</li>
 *  <li>line: the line which the method is invoked.</li>
 * </ul>
 */
public class Throw extends Predicate {

    private String method;
    private String exception;
    private Line line;

    /**
     * {@code throw(?methodId, ?exception).}
     * 
     * @param methodId the method identifier.
     * @param exception the exception thrown by the method.
     */
    public Throw(String methodId, String exception) {
        this.method = methodId;
        this.exception = exception;
        this.line = null;
    }

    /**
     * {@code throw(?method, ?exception, ?line:Line).}
     * 
     * @param method method is either a method invocation expression or a method name.
     * @param exception the exception thrown by the method.
     * @param line line
     */
    public Throw(String method, String exception, Line line) {
        this.method = method;
        this.exception = exception;
        this.line = line;
    }

    public String getMethod() {
        return method;
    }

    public String getException() {
        return exception;
    }

    public Line getLine() {
        return line;
    }

    @Override
    public String getPredicateName() {
        return "throw";
    }

    @Override
    public List<String> arguments() {
        if(line == null)
            return Arrays.asList(method, exception);
        return Arrays.asList(method, exception, line.toString());
    }
}
