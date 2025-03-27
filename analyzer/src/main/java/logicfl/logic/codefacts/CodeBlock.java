package logicfl.logic.codefacts;

import org.eclipse.jdt.core.dom.Block;

/**
 * {@code block/5} predicate represents a code block. <br><br>
 * {@code block/3} predicate can be used if parent information is not available. <br><br>
 * 
 * {@code block(?block_id, ?node_type, ?parent_id, ?loc_in_parent, ?range:Range).}
 * 
 * <ul>
 *  <li>block_id: the id of this block.</li>
 *  <li>node_type: AST node type of this block.</li>
 *  <li>parent_id: the id of the parent node.</li>
 *  <li>loc_in_parent: the block's locaton in its parent.</li>
 *  <li>range: code range of this block.</li>
 * </ul>
 */
public class CodeBlock extends CodeEntity {
    public static final String PREFIX = "block";
    
    public CodeBlock(String id, String classId, Block block, CodeEntity parent, Loc locInParent) {
        super(id, classId, block);
        this.parent = parent;
        this.locInParent = locInParent;
    }

    public CodeBlock(String id, Range range, CodeEntity parent, Loc locInParent) {
        super(id, range);
        this.parent = parent;
        this.locInParent = locInParent;
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

    public static String createId(String classId, int index) {
        return String.join("", classId, DELIM, PREFIX, String.valueOf(index));
    }
}