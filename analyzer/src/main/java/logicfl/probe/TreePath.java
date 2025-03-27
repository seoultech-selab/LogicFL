package logicfl.probe;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

import logicfl.utils.CodeUtils;

public class TreePath {

    private StructuralPropertyDescriptor location;
    private int index;
    private TreePath parent;
    private TreePath child;

    public TreePath() {
        this(null, -1);
    }

    @SuppressWarnings("unchecked")
    public TreePath(ASTNode node) {
        location = node.getLocationInParent();
        if(location != null && location.isChildListProperty()) {
            List<ASTNode> nodes = (List<ASTNode>)node.getParent().getStructuralProperty(location);
            index = nodes.indexOf(node);
        } else {
            index = -1;
        }
    }

    public TreePath(StructuralPropertyDescriptor location) {
        this(location, -1);
    }

    public TreePath(StructuralPropertyDescriptor location, int index) {
        this.location = location;
        this.index = index;
    }

    public StructuralPropertyDescriptor getLocation() {
        return location;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public TreePath getParent() {
        return parent;
    }

    public void setParent(TreePath parent) {
        this.parent = parent;
        parent.setChild(this);
    }

    public TreePath getChild() {
        return child;
    }

    public void setChild(TreePath child) {
        this.child = child;
        if(child.getParent() != this)
            child.setParent(this);
    }

    public TreePath getBottom() {
        TreePath bottom = this;
        while(bottom.getChild() != null)
            bottom = bottom.getChild();
        return bottom;
    }

    public TreePath getTop() {
        TreePath top = this;
        while(top.getParent() != null)
            top = top.getParent();
        return top;
    }

    public ASTNode getBottomNode(ASTNode origin) {
        TreePath curr = this;
        ASTNode node = curr.getNode(origin);
        while(curr.getChild() != null && node != null) {
            curr = curr.getChild();
            node = curr.getNode(node);
        }
        return node;
    }

    public void setBottomNode(ASTNode newNode, ASTNode origin) {
        TreePath curr = this;
        ASTNode child = origin;
        while(curr.getChild() != null
            && curr.getChild().getLocation() != null) {
            child = curr.getNode(child);
            curr = curr.getChild();
        }
        curr.setNode(newNode, child);
    }

    @SuppressWarnings("unchecked")
    public ASTNode getNode(ASTNode origin) {
        if(origin != null && location != null) {
            try {
                if(location.isChildProperty()) {
                    return (ASTNode)origin.getStructuralProperty(location);
                } else if(location.isChildListProperty() && index >= 0) {
                    List<ASTNode> list = (List<ASTNode>)origin.getStructuralProperty(location);
                    if(list != null && index < list.size()) {
                        return list.get(index);
                    }
                }
            } catch (Exception e) {
                System.out.printf("Failed to get %s from %s (%s).\n",
                        location, origin, CodeUtils.getNodeType(origin.getNodeType()));
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public void setNode(ASTNode node, ASTNode parent) {
        if(parent != null && node != null) {
            if(location.isChildProperty()) {
                parent.setStructuralProperty(location, node);
            } else if(location.isChildListProperty() && index >= 0) {
                List<ASTNode> list = (List<ASTNode>)parent.getStructuralProperty(location);
                if(list != null && index < list.size()) {
                    list.set(index, node);
                }
            }
        }
    }

    public void setLocation(StructuralPropertyDescriptor location) {
        this.location = location;
    }
}
