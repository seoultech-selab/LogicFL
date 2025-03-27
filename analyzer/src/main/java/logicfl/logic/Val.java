package logicfl.logic;

import java.util.Arrays;
import java.util.List;

import logicfl.logic.codefacts.Line;

/**
 * {@code value/3} predicate.
 *
 * {@code val(?expr, ?val, ?line:Line).}
 *
 * This predicate indicates that an expression {@code expr},
 * has the value {@code val} at {@code line}.
 * 
 * <ul>
 *  <li>expr: an expression</li>
 *  <li>val: the value of the expression</li>
 *  <li>line: the line where the expression has the value</lib>
 * </ul>
 */
public class Val extends Predicate {

    private String expr;
    private String val;
    private Line line;

    /**
     * {@code val(+expr:Expr, +val:Any, +line:Line).}
     *
     * @param expr the expression.
     * @param value the expression's value at the {@code line}.
     * @param line the line which the expression appears.
     */
    public Val(String expr, String value, Line line) {
        this.expr = expr;
        this.val = value;
        this.line = line;
    }

    @Override
    public String getPredicateName() {
        return "val";
    }

    @Override
    public List<String> arguments() {
        return Arrays.asList(expr, val, line.toString());
    }

    public Line getLine() {
        return line;
    }
}
