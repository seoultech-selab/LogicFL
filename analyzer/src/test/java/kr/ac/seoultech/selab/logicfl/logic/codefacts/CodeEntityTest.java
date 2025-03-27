package kr.ac.seoultech.selab.logicfl.logic.codefacts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import logicfl.logic.codefacts.CodeBlock;
import logicfl.logic.codefacts.CodeEntity;
import logicfl.logic.codefacts.Expr;
import logicfl.logic.codefacts.Loc;
import logicfl.logic.codefacts.Range;

public class CodeEntityTest {
    @Test
    void testCreateId() {
        String classId = "class_1";
        String id = CodeEntity.createId(classId, 10);
        CodeEntity ce = new CodeEntity(id, new Range(classId, 0, 200, 1, 10));
        CodeBlock block = new CodeBlock(id, new Range(classId, 10, 100, 5, 6), ce, new Loc("body"));
        assertTrue(block.toString().startsWith("block"));
        assertEquals("class_1_code10", id);
        id = Expr.createId(classId, 5);
        assertEquals("class_1_expr5", id);
    }
}
