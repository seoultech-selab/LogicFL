package logicfl.logic.codefacts;

import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

import logicfl.utils.CodeUtils;

public class Loc {
    private String location;
    private int index;

    public Loc(String location) {
        this(Objects.requireNonNull(location), -1);
    }

    public Loc(String location, int index) {
        this.location = location;
        this.index = index;
    }

    @SuppressWarnings("unchecked")
    public Loc(ASTNode node) {
        StructuralPropertyDescriptor desc = node.getLocationInParent();
        location = CodeUtils.camelToLower(desc.getId());
        if(desc.isChildListProperty()) {
            List<ASTNode> nodes = (List<ASTNode>)node.getParent().getStructuralProperty(desc);
            index = nodes.indexOf(node);
        } else {
           index = -1;
        }
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        if(index >= 0)
            return String.join("", "(", location, ", ", String.valueOf(index), ")");
        return location;
    }
}
