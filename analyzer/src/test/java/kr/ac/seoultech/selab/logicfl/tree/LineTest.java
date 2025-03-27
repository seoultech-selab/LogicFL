package kr.ac.seoultech.selab.logicfl.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import logicfl.logic.codefacts.Line;

public class LineTest {

    @Test
    public void testLine() {
        Line line = new Line("string_utils1", 3385);
        String lineStr = "line(string_utils1, 3385)";
        assertEquals(lineStr, line.toString());
        Line line1 = new Line(lineStr);
        assertEquals(line, line1);
        Line emptyLine = new Line("");
        assertEquals(0, emptyLine.getLineNum());
        assertEquals("", emptyLine.getClassId());
    }

    @Test
    public void testCompareTo() {
        Line line1 = new Line("string_utils1", 3385);
        Line line2 = new Line("string_utils1", 3390);
        assertTrue(line1.compareTo(line2) < 0);

        line1 = new Line("string_utils1", 3385);
        line2 = new Line("string_utils2", 3320);
        assertTrue(line1.compareTo(line2) < 0);
    }
}