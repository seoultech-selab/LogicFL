package logicfl.logic.codefacts;

import org.eclipse.jdt.core.dom.ASTNode;

import logicfl.logic.FactManager;

/**
 * {@code name/6} predicate represents a name node in an AST.<br><br>
 * 
 * {@code name(?name_ref_id, ?node_type, ?parent_id, ?loc_in_parent, ?range:Range, ?name).}
 * 
 * <ul>
 *  <li>name_ref_id: the id of this name.</li>
 *  <li>node_type: AST node type of this name.</li>
 *  <li>parent_id: the id of the parent node.</li>
 *  <li>loc_in_parent: the name's locaton in its parent.</li>
 *  <li>range: code range of this name.</li>
 *  <li>name: actual string name.</li>
 * </ul>
 */
public class CodeName extends Expr {
    public static final String PREFIX = "name";

    private NameRef nameRef;

    public CodeName(NameRef nameRef, String classId, ASTNode node, CodeEntity parent, Loc locInParent) {
        super(nameRef.getId(), classId, node, parent, locInParent);
        this.nameRef = nameRef;
    }

    public NameRef getNameRef() {
        return nameRef;
    }

    @Override
    public String toString() {
        return String.join("", PREFIX, "(",
                id, ", ",
                nodeType, ", ",                
                parent.id, ", ",
                locInParent.toString(), ", ",
                range.toString(), ", ",
                FactManager.getQuotedString(nameRef.getName()), ")");
    }
}