package kr.ac.seoultech.selab.logicfl.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import logicfl.logic.FactManager;
import logicfl.logic.codefacts.NameRef;

public class FactManagerTest {
    @Test
    void testGetEscapedString() {
        assertEquals("\"new String(\\\"\\\")\"", FactManager.getDoubleQuotedString(FactManager.getEscapedString("new String(\"\")")));
        assertEquals("\"this.setSizeStartText(\\\"\\\\\\\"<size=\\\")\"", FactManager.getDoubleQuotedString(FactManager.getEscapedString("this.setSizeStartText(\"\\\"<size=\")")));
        assertEquals("\"s = \\\"line1\\nline2\\\"\"", FactManager.getDoubleQuotedString(FactManager.getEscapedString("s = \"line1\nline2\"")));
        assertEquals("\"s = \\\"line1\\\\nline2\\\"\"", FactManager.getDoubleQuotedString(FactManager.getEscapedString("s = \"line1\\nline2\"")));
    }

    @Test
    void testParseNamePredicate() {
        String predicate1 = NameRef.PREFIX + "(name_4, field, 'name', 'Lsample/Person;.name)Ljava/lang/String;').";
        NameRef name1 = new NameRef(4, NameRef.K_FIELD, "name", "Lsample/Person;.name)Ljava/lang/String;");
        String predicate2 = NameRef.PREFIX + "(p_1, param, 'p', 'Lsample/Example;.decorate(Lsample/Person;)Ljava/lang/String;#p#0#0').";
        NameRef name2 = new NameRef(1, NameRef.K_PARAM, "p", "Lsample/Example;.decorate(Lsample/Person;)Ljava/lang/String;#p#0#0");
        String predicate3 = NameRef.PREFIX + "(index_5, var, 'index', 'Lsample/Example;.firstName(Ljava/lang/String;)Ljava/lang/String;#index').";
        NameRef name3 = new NameRef(5, NameRef.K_VAR, "index", "Lsample/Example;.firstName(Ljava/lang/String;)Ljava/lang/String;#index");
        assertEquals(name1, new NameRef(predicate1));
        assertEquals(name2, new NameRef(predicate2));
        assertEquals(name3, new NameRef(predicate3));
    }

    @Test
    void testParseClassPredicate() {
        String predicate1 = "class(example_1, 'sample.Example').";
        String predicate2 = "class(person_1, 'sample.Person').";
        String predicate3 = "class(node_visitor_1, 'NodeVisitor').";
        Map<String, String> map = new HashMap<>();
        FactManager.parseClassPredicate(predicate1, map);
        FactManager.parseClassPredicate(predicate2, map);
        FactManager.parseClassPredicate(predicate3, map);
        assertEquals("example_1", map.get("sample.Example"));
        assertEquals("person_1", map.get("sample.Person"));
        assertEquals("node_visitor_1", map.get("NodeVisitor"));
    }

    @Test
    void testGetNumberString() {
        assertEquals("0.012", FactManager.getNumberString("0.012d"));
        assertEquals("0.012", FactManager.getNumberString("0.012f"));
        assertEquals("1231", FactManager.getNumberString("1231L"));
        assertEquals("0.123", FactManager.getNumberString(".123d"));
        assertEquals("0.412", FactManager.getNumberString("0.412f"));
        assertEquals("5.412", FactManager.getNumberString("5.412"));
        assertEquals("1.2e3", FactManager.getNumberString("1.2e3"));
        assertEquals("'0x1.0p-53'", FactManager.getNumberString("0x1.0p-53"));
        assertEquals("0x80000", FactManager.getNumberString("0x80000"));
        assertEquals("0xF", FactManager.getNumberString("0xF"));
        assertEquals("0xd", FactManager.getNumberString("0xd"));
        assertEquals("0.0", FactManager.getNumberString("0.f"));
        assertEquals("1.0", FactManager.getNumberString("1."));
        assertEquals("1.0", FactManager.getNumberString("1.f"));
        assertEquals("1.0", FactManager.getNumberString("1.d"));
    }
}
