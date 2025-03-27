package logicfl.logic;

import java.util.Arrays;
import java.util.List;

import logicfl.logic.codefacts.Line;

/**
 * {@code return/3} predicate.<br>
 *
 * {@code return(?ret_val, ?method_id, ?line:Line).}
 *
 * <ul>
 *  <li>ret_val: the expression representing the return value.</li>
 *  <li>method_id: the method id of the return statement.</li>
 *  <li>line: the line of the return statement.</li>
 * </ul>
 */
public class Return extends Predicate {

    private String returnValue;
    private String methodId;
    private Line line;

    public Return(String returnValue, String methodId, Line line) {
        this.returnValue = returnValue;
        this.methodId = methodId;
        this.line = line;
    }

    public String getReturnValue() {
        return returnValue;
    }

    public String getMethodId() {
        return methodId;
    }

    public Line getLine() {
        return line;
    }

    @Override
    public String getPredicateName() {
        return "return";
    }

    @Override
    public List<String> arguments() {
        return Arrays.asList(returnValue, methodId, line.toString());
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Return ret) {
            return ret.getReturnValue().equals(returnValue)
                && ret.getLine().equals(line); //if line is the same, don't need to check method name.
        }
        return false;
    }

}
