package logicfl.logic.codefacts;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.StringLiteral;

import logicfl.logic.FactManager;

/**
 * {@code literal/6} predicate represents a literal.<br><br>
 * 
 * {@code literal(?literal_id, ?node_type, ?parent_id, ?loc_in_parent, ?range:Range, ?value).}
 * 
 * <ul>
 *  <li>literal: the id of this literal.</li>
 *  <li>node_type: AST node type of this literal.</li>
 *  <li>parent_id: the id of the parent node.</li>
 *  <li>loc_in_parent: the literal's locaton in its parent.</li>
 *  <li>range: a code range of this literal.</li>
 *  <li>value: an actual value of this literal.</li>
 * </ul>
 */
public class Literal extends Expr {
    public static final String PREFIX = "literal";

    private String value;

    public Literal(ASTNode node, String classId, int index, CodeEntity parent, Loc locInParent) {
        super(createId(classId, index), classId, node, parent, locInParent);
        if(node instanceof StringLiteral str) {
            this.value = str.getEscapedValue();
        } else if(node instanceof NumberLiteral number) {
            this.value = FactManager.getNumberString(number.getToken());
        } else if(node instanceof CharacterLiteral character) {
            this.value = character.getEscapedValue();
        } else if(node instanceof BooleanLiteral bool) {
            this.value = String.valueOf(bool.booleanValue());
        } else if(node instanceof NullLiteral) {
            this.value = "null";
        } else {
            this.value = FactManager.getDoubleQuotedString(FactManager.getEscapedString(node.toString()));
        }
    }

    public static String createId(String classId, int index) {
        return String.join("", classId, DELIM, PREFIX, String.valueOf(index));
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.join("", PREFIX, "(",
                id, ", ",
                nodeType, ", ",
                parent.id, ", ",
                locInParent.toString(), ", ",
                range.toString(), ", ",
                value,
            ")");
    }
}
