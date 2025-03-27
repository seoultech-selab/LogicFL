package logicfl.logic.codefacts;

import org.eclipse.jdt.core.dom.ASTNode;

import logicfl.logic.FactManager;

/**
 * {@code expr/6} predicate represents an expression. <br><br>
 * {@code expr/4} predicate can be used if parent information is not available. <br><br>
 * 
 * {@code expr(?expr_id, ?node_type, ?parent_id, ?loc_in_parent, ?range:Range, ?code).}
 * 
 * <ul>
 *  <li>expr_id: the id of this expression.</li>
 *  <li>node_type: AST node type of this expression.</li>
 *  <li>parent_id: the id of the parent node.</li>
 *  <li>loc_in_parent: the expression's locaton in its parent.</li>
 *  <li>range: code range of this expression.</li>
 *  <li>code: actual code of the expression.</li>
 * </ul>
 */
public class Expr extends CodeEntity {
    private static final Expr NONE = new ExprNone();
    public static final String PREFIX = "expr";

    public Expr(String id) {
        super(id);
    }

    public Expr(ASTNode node, String classId, int index) {
        super(createId(classId, index), classId, node);
    }

    public Expr(String id, String classId, ASTNode node, CodeEntity parent, Loc locInParent) {
        super(id, classId, node, parent, locInParent);
    }

    public Expr(ASTNode node, String classId, int index, CodeEntity parent, Loc locInParent) {
        super(createId(classId, index), classId, node, parent, locInParent);
    }

    public static String createId(String classId, int index) {
        return String.join("", classId, DELIM, PREFIX, String.valueOf(index));
    }

    @Override
    public String toString() {
        if(node != null)
            return toString(node.toString());
        return toString("N/A");
    }

    public String toString(String code) {
        if(parent != null && locInParent != null) {
            return String.join("", PREFIX, "(",
                id, ", ",
                nodeType, ", ",
                parent.id, ", ",
                locInParent.toString(), ", ",
                range.toString(), ", ",
                FactManager.getDoubleQuotedString(FactManager.getEscapedString(code)),
            ")");
        }
        return String.join("", PREFIX, "(",
            id, ", ",
            nodeType, ", ",
            range.toString(), ", ",
            FactManager.getDoubleQuotedString(FactManager.getEscapedString(code)),
        ")");
    }

    public static Expr getNone() {
        return NONE;
    }
}