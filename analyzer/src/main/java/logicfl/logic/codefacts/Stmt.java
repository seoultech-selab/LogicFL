package logicfl.logic.codefacts;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

/**
 * {@code stmt/5} predicate represents a statement. <br><br>
 * {@code stmt/3} predicate can be used if parent information is not available. <br><br>
 * 
 * {@code stmt(?stmt_id, ?node_type, ?parent_id, ?loc_in_parent, ?range:Range).}
 * 
 * <ul>
 *  <li>stmt_id: the id of this statement.</li>
 *  <li>node_type: AST node type of this statement.</li>
 *  <li>parent_id: the id of the parent node.</li>
 *  <li>loc_in_parent: the statement's locaton in its parent ('statments', index).</li>
 *  <li>range: code range of this statement.</li>
 * </ul>
 */
public class Stmt extends CodeEntity {
    public static final String PREFIX = "stmt";

    public Stmt(String id, int nodeType, CodeBlock block, int index, Range range) {
        super(id, nodeType, range, block, new Loc("statements", index));
    }

    @SuppressWarnings("unchecked")
    public Stmt(String id, String classId, Statement stmt, CodeEntity parent) {
        super(id, classId, stmt);
        this.parent = parent;
        int index = -1;
        StructuralPropertyDescriptor desc = stmt.getLocationInParent();
        if(desc.isChildListProperty()) {
            List<ASTNode> list = (List<ASTNode>)stmt.getParent().getStructuralProperty(desc);
            index = list.indexOf(stmt);
        }
        this.locInParent = new Loc(desc.getId(), index);
    }

    public static String createId(String classId, int index) {
        return String.join("", classId, DELIM, PREFIX, String.valueOf(index));
    }

    @Override
    public String toString() {
        return String.join("", PREFIX, "(",
            id, ", ",
            nodeType, ", ",
            parent.id, ", ",
            locInParent.toString(), ", ",
            range.toString(),
        ")");
    }
}
