package logicfl.logic;

import java.util.Arrays;
import java.util.List;

/**
 * {@code argument/3} predicate.
 *
 * {@code argument(?arg_expr, ?index, ?m_expr).}
 *
 * <ul>
 *  <li>arg_expr: the argument expression.</li>
 *  <li>index: the argument is i-th argument of the method invocation.</li>
 *  <li>m_expr: the method invocation which the argument is used.</li>
 * </ul>
 */
public class Argument extends Predicate {

    private String argExpr;
    private int index;
    private String methodInvocExpr;

    public Argument(String argExpr, int index, String methodInvocExpr) {
        this.argExpr = argExpr;
        this.index = index;
        this.methodInvocExpr = methodInvocExpr;
    }

    @Override
    public String getPredicateName() {
        return "argument";
    }

    @Override
    public List<String> arguments() {
        return Arrays.asList(argExpr, String.valueOf(index), methodInvocExpr);
    }

}
