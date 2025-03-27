package kr.ac.seoultech.selab.logicfl.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import logicfl.logic.Assign;
import logicfl.logic.codefacts.Line;

public class AssignTest {

    @Test
    public void testCreateTerm() {
        Line line = new Line("person1", 10);
        Assign assign = new Assign("name1", "expr1", line);
        assertEquals("assign(name1, expr1, "+line.toString()+")", assign.createTerm());
    }
}