package logicfl.logic.codefacts;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;

import logicfl.utils.CodeUtils;

/**
 * {@code code/5} predicate represents a code entity. <br><br>
 * {@code code/3} predicate can be used if parent information is not available. <br><br>
 * 
 * {@code code(?code_id, ?node_type, ?parent_id, ?loc_in_parent, ?range:Range).}
 * 
 * <ul>
 *  <li>code_id: the id of this code entity.</li>
 *  <li>node_type: AST node type of this code entity.</li>
 *  <li>parent_id: the id of the parent node.</li>
 *  <li>loc_in_parent: the code entity's locaton in its parent.</li>
 *  <li>range: code range of this code entity.</li>
 * </ul>
 */
public class CodeEntity implements Comparable<CodeEntity> {
    public static final String PREFIX = "code";
    public static final String DELIM = "_";

    protected String id;
    protected Range range;
    protected CodeEntity parent;
    protected List<CodeEntity> children;
    protected ASTNode node;
    protected String nodeType;
    protected Loc locInParent;

    public CodeEntity() {
        this.id = null;
        this.range = null;
        this.parent = null;
        this.children = new ArrayList<>();
        this.node = null;
        this.nodeType = null;
    }

    public CodeEntity(String id) {
        this(id, -1, null);
    }

    public CodeEntity(String id, Range range) {
        this(id, -1, range);
    }

    public CodeEntity(String id, int nodeType, Range range) {
        this(id, nodeType, range, null, null);
    }

    public CodeEntity(String id, String classId, ASTNode node) {
        this(id, classId, node, null, null);
    }

    public CodeEntity(String id, String classId, ASTNode node, CodeEntity parent, Loc locInParent) {
        this(id, node.getNodeType(), new Range(classId, node), parent, locInParent);
    }

    public CodeEntity(String id, int nodeType, Range range, CodeEntity parent, Loc locInParent) {
        this.id = id;
        this.nodeType = CodeUtils.camelToLower(CodeUtils.getNodeType(nodeType));
        this.range = range;
        this.parent = parent;
        this.locInParent = locInParent;
        this.children = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Range getRange() {
        return range;
    }

    public void setRange(Range range) {
        this.range = range;
    }

    public CodeEntity getParent() {
        return parent;
    }

    public void setParent(CodeEntity parent) {
        this.parent = parent;
    }

    public void addChild(CodeEntity child) {
        child.setParent(this);
        children.add(child);
    }

    public List<CodeEntity> getChildren() {
        return children;
    }

    public String getClassId() {
        return this.range.getClassId();
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public Loc getLocInParent() {
        return locInParent;
    }

    public void setLocInParent(Loc locInParent) {
        this.locInParent = locInParent;
    }

    public static String createId(String classId, int index) {
        return String.join("", classId, DELIM, PREFIX, String.valueOf(index));
    }

    @Override
    public String toString() {
        if(parent != null) {
            return String.join("", PREFIX, "(",
                id, ", ",
                nodeType, ", ",
                parent.id, ", ",
                locInParent.toString(), ", ",
                range.toString(), 
            ")");
        }
        return String.join("", PREFIX, "(",
            id, ", ",
            nodeType, ", ",
            range.toString(),
        ")");
    }

    @Override
    public int compareTo(CodeEntity entity) {
        return this.range.compareTo(entity.range);
    }

    @Override
    public int hashCode() {
        if(id != null)
            return this.id.hashCode();
        return super.hashCode();
    }
}
