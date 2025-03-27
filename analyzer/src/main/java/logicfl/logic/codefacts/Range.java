package logicfl.logic.codefacts;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 * {@code range/5} predicate represents a range of a code entity. <br><br>
 * 
 * {@code range(?class_id, ?start_pos:int, ?length:int, ?start_line:int, ?end_line:int).}
 * 
 * <ul>
 *  <li>class_id: the class id of this range.</li>
 *  <li>start_pos: the start index of a code range in source code.</li>
 *  <li>length: the length of this code range.</li>
 *  <li>start_line: the start line of this code range.</li>
 *  <li>end_line: the end line of this code range.</li>
 * </ul>
 */
public class Range implements Comparable<Range> {
    public static final String PREFIX = "range";
    private String classId;
    private int startPos;
    private int length;
    private int startLine;
    private int endLine;
    private String key;

    public Range(String classId, int startPos, int length, int startLine, int endLine) {
        this.classId = classId;
        this.startPos = startPos;
        this.length = length;
        this.startLine = startLine;
        this.endLine = endLine;
        computeKey();
    }

    public Range(String classId, int startPos, int length) {
        this(classId, startPos, length, 0, 0);
    }

    public Range(String classId, ASTNode node) {
        this.classId = classId;
        if(node != null) {
            this.startPos = node.getStartPosition();
            this.length = node.getLength();
        }
        this.startLine = 0;
        this.endLine = 0;
        computeKey();
    }

    public Range(String classId, ASTNode node, int startLine, int endLine) {
        this(classId, node);
        this.startLine = startLine;
        this.endLine = endLine;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
        computeKey();
    }

    public int getStartPos() {
        return startPos;
    }

    public void setStartPos(int startPos) {
        this.startPos = startPos;
        computeKey();
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
        computeKey();
    }

    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    private void computeKey() {
        this.key = String.join("#", classId, String.valueOf(startPos), String.valueOf(length));
    }

    public String getKey() {
        return this.key;
    }

    @Override
    public String toString() {
        return String.join("", PREFIX, "(", classId, ", ",
                String.valueOf(startPos), ", ",
                String.valueOf(length), ", ",
                String.valueOf(startLine), ", ",
                String.valueOf(endLine), ")");
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Range r && r.getKey().equals(key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public int compareTo(Range range) {
        int cmp = this.classId.compareTo(range.getClassId());
        if(cmp == 0)
            return Integer.compare(this.startPos, range.getStartPos());
        return cmp;
    }
}
