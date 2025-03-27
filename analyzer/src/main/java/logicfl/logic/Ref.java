package logicfl.logic;

import java.util.Arrays;
import java.util.List;

import logicfl.logic.codefacts.Line;

/**
 * {@code ref/3} predicate. 
 * {@code ref(refExpr, expr, line)} indicates that {@code refExpr} is referenced in {@code expr} at {@code line}.
 *
 * {@code ref(?refExpr, ?expr, ?line:Line).}
 *
 * <ul>
 *  <li>refExpr: the referenced expression.</li>
 *  <li>expr: the expression which the {@code refExpr} was referenced.</li>
 *  <li>line: line which the {@code refExpr} is referenced.</li>
 * </ul>
 */
public class Ref extends Predicate {

    private String refExpr;
    private String expr;
    private Line line;

    public Ref(String refExpr, String expr, Line line) {
        this.refExpr = refExpr;
        this.expr = expr;
        this.line = line;
    }

    @Override
    public String getPredicateName() {
        return "ref";
    }

    @Override
    public List<String> arguments() {
        return Arrays.asList(refExpr, expr, line.toString());
    }

}
