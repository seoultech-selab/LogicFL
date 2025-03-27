package logicfl.logic;

import java.util.Arrays;
import java.util.List;

import logicfl.logic.codefacts.Line;

/**
 * {@code assign/3} predicate.
 *
 * {@code assign(?lhs, ?rhs, ?line:Line).}
 * 
 * <ul>
 *  <li>lhs: the left hand side of the assignment.</li>
 *  <li>rhs: the right hand side of the assignment.</li>
 *  <li>line: the line which the assignment appears.</li>
 * </ul>
 */
public class Assign extends Predicate {

    private String lhs;
    private String rhs;
    private Line line;

    public Assign(String lhs, String rhs, Line line) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.line = line;
    }

    public String getLhs() {
        return lhs;
    }

    public String getRhs() {
        return rhs;
    }

    public Line getLine() {
        return line;
    }

    @Override
    public String getPredicateName() {
        return "assign";
    }

    @Override
    public List<String> arguments() {
        return Arrays.asList(lhs, rhs, line.toString());
    }
}
