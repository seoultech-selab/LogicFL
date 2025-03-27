package logicfl.probe;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;

import logicfl.utils.CodeUtils;

public class Probe {

    private String name;
    private Expression target;
    private ProbeLocation location;
    private List<Probe> children;
    private Probe parent;
    private TreePath path;
    private ASTNode probeNode;
    private ASTNode targetInProbe;

    /**
     * This is a probe named <code>name</code> to monitor a certain expression <code>target</code>,
     * at a certain point <code>location</code> in source code.
     *
     * @param name the name of this probe.
     * @param target the target expression to be monitored.
     * @param location the location expression containing the target expression.
     */
    public Probe(String name, Expression target, ProbeLocation location) {
        this.name = name;
        this.target = target;
        this.location = location;
        this.children = new ArrayList<>();
        this.parent = null;
        this.path = null;
        this.probeNode = null;
        this.targetInProbe = null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Expression getTarget() {
        return target;
    }

    public void setTarget(Expression target) {
        this.target = target;
    }

    public ProbeLocation getLocation() {
        return location;
    }

    public void setLocation(ProbeLocation location) {
        this.location = location;
    }

    public void addChild(Probe child) {
        this.children.add(child);
        child.setParent(this);
    }

    public List<Probe> getProbeList() {
        List<Probe> probes = new ArrayList<>();
        getProbeList(probes);
        return probes;
    }

    public void getProbeList(List<Probe> probes) {
        probes.add(this);
        for(Probe child : children) {
            child.getProbeList(probes);
        }
    }

    public Probe getParent() {
        return parent;
    }

    public void setParent(Probe parent) {
        this.parent = parent;
    }

    /**
     * This is a simple getter method to return the stored path.
     * If there is none, find a default path using location's locNode or parent probe's target to avoid null.
     * This method should be only used to retrieve the stored path.
     *
     * To obtain a path to a certain node {@link #getPath(ASTNode)} should be used.
     *
     * @return a {@code TreePath} instance stored in the probe.
     */
    public TreePath getPath() {
        if(path == null)
            this.path = getPath(parent == null ? location.getLocNode() : parent.getTarget());
        return path;
    }

    /**
     * Returns a tree path from the target of this probe to the given node.
     * The returned tree path indicates the direct location of the given node which the target belongs.
     * You can obtain a corresponding target inside another node by using {@link TreePath#getNode(ASTNode)}.
     *
     * @param node the node which the tree path ends.
     * @return a tree path from probe's target (bottom) to the given node (top).
     */
    public TreePath getPath(ASTNode node) {
        return CodeUtils.findTreePath(target, node);
    }

    public void setPath(TreePath path) {
        this.path = path;
    }

    public ASTNode getProbeNode() {
        return probeNode;
    }

    public void setProbeNode(ASTNode probeNode, ASTNode targetInProbe) {
        this.probeNode = probeNode;
        this.targetInProbe = targetInProbe;
    }

    public ASTNode getTargetInProbe() {
        return targetInProbe;
    }

    @Override
    public String toString() {
        return name + " = " + target.toString() + " from " + location.toString();
    }

    public List<Probe> getChildren() {
        return children;
    }

    public Probe copy() {
        Probe p = new Probe(this.name, target, location);
        for(Probe child : children) {
            p.addChild(child.copy());
        }
        return p;
    }
}
