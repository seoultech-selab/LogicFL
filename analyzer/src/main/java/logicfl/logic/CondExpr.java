package logicfl.logic;

import java.util.Arrays;
import java.util.List;

import logicfl.logic.codefacts.Line;

/**
 * {@code cond_expr/4} predicate.
 *
 * {@code cond_expr(?boolExpr, ?then_expr, ?else_expr, ?line:Line).}
 *
 * <ul>
 *  <li>bool_expr: the boolean expression (condition) of this conditional expression.</li>
 *  <li>then_expr: the expression when bool_expr is true.</li>
 *  <li>else_expr: the expression when bool_expr is flase.</li>
 *  <li>line: the line of the conditional expression.</li>
 * </ul>
 */
public class CondExpr extends Predicate {

    private String boolExpr;
    private String thenExpr;
    private String elseExpr;
    private Line line;

    /**
     * {@code cond_expr(?boolExpr, ?then_expr, ?else_expr, ?line:Line).}
     * 
     * @param boolExpr the boolean expression.
     * @param thenExpr the expression for the boolExpr is true.
     * @param elseExpr the expression for the boolExpr is false.
     * @param line line
     */
    public CondExpr(String boolExpr, String thenExpr, String elseExpr, Line line) {
        this.boolExpr = boolExpr;
        this.thenExpr = thenExpr;
        this.elseExpr = elseExpr;
        this.line = line;
    }

    public Line getLine() {
        return line;
    }

    @Override
    public String getPredicateName() {
        return "cond_expr";
    }

    @Override
    public List<String> arguments() {
        return Arrays.asList(boolExpr, thenExpr, elseExpr, line.toString());
    }
}
