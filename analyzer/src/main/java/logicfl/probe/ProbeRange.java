package logicfl.probe;

public class ProbeRange {
    public static final String K_START_LINE = "startLine";
    public static final String K_END_LINE = "endLine";
    public static final String K_ORG_LINE_START = "orgLineStart";
    public static final String K_ORG_LINE_END = "orgLineEnd";
    public static final String K_PROBED_LINE_END = "probedLineEnd";
    public static final String K_OFFSET = "offset";

    private int startLine;
    private int endLine;
    private int orgLineStart;
    private int orgLineEnd;
    private int probedLineEnd;
    private int offset;

    public ProbeRange(int startLine, int orgLineStart) {
        this.startLine = startLine;
        this.orgLineStart = orgLineStart;
    }

    public ProbeRange(int startLine, int endLine, int orgLineStart, int orgLineEnd, int probedLineEnd, int offset) {
        this.startLine = startLine;
        this.endLine = endLine;
        this.orgLineStart = orgLineStart;
        this.orgLineEnd = orgLineEnd;
        this.probedLineEnd = probedLineEnd;
        this.offset = offset;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
        offset = endLine - orgLineStart;
    }

    public void setOrgLineEnd(int orgLineEnd) {
        this.orgLineEnd = orgLineEnd;
    }

    public void setProbedLineEnd(int probedLineEnd) {
        this.probedLineEnd = probedLineEnd;
        //If probed code lines are different, adjust offset.
        offset -= getDiff();
    }

    public int getDiff() {
        return (orgLineEnd - orgLineStart) - (probedLineEnd - endLine);
    }

    public int getOffset() {
        return offset;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public int getOrgLineStart() {
        return orgLineStart;
    }

    public int getOrgLineEnd() {
        return orgLineEnd;
    }

    public int getProbedLineEnd() {
        return probedLineEnd;
    }

    public boolean inRange(int line) {
        return line >= startLine && line <= endLine;
    }

    public boolean hasDifferentLines() {
        return getDiff() != 0;
    }
}
