package logicfl.probe;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import logicfl.utils.CodeUtils;

public class ProbeLocation {
    public static final int INSERT_BEFORE = 0;
    public static final int INSERT_AFTER = 1;
    public static final int INSERT_FIRST = 2;
    public static final int INSERT_LAST = 3;

    private ASTNode locNode;
    private int direction;

    public ProbeLocation(ASTNode locNode) {
        this.locNode = locNode;
        this.direction = INSERT_BEFORE;
    }

    public ProbeLocation(ASTNode locNode, int direction) {
        this.locNode = locNode;
        this.direction = direction;
    }

    public ASTNode getLocNode() {
        return locNode;
    }

    public void setLocNode(ASTNode locNode) {
        this.locNode = locNode;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public boolean isInsertLast() {
        return direction == INSERT_LAST;
    }

    public boolean isInsertFirst() {
        return direction == INSERT_FIRST;
    }

    public int getStartLine(CompilationUnit cu) {
        return CodeUtils.getStartLine(locNode, cu);
    }

    public int getEndLine(CompilationUnit cu) {
        return CodeUtils.getEndLine(locNode, cu);
    }

    public void insert(ASTNode node, ListRewrite listRewrite) {
        switch(direction) {
            case INSERT_BEFORE:
                listRewrite.insertBefore(node, locNode, null);
                break;
            case INSERT_AFTER:
                listRewrite.insertAfter(node, locNode, null);
                break;
            case INSERT_FIRST:
                listRewrite.insertFirst(node, null);
                break;
            case INSERT_LAST:
                listRewrite.insertLast(node, null);
                break;
        }
    }

    public boolean isField() {
        return locNode instanceof FieldDeclaration;
    }
}
