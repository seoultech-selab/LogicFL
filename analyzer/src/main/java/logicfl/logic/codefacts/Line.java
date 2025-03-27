package logicfl.logic.codefacts;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code line/2} predicate to represent a certain code line in a class. <br><br>
 *
 * {@code line(?class_id, ?line_num:int).}
 *
 * <ul>
 *  <li>{@code class_id}: the class id of this code line.</li>
 *  <li>{@code line_num}: the line number of this code line.</li>
 * </ul>
 */
public class Line implements Comparable<Line> {
    public static final String PREFIX = "line";
    public static final Pattern REGEX_PATTERN = Pattern.compile(PREFIX+"\\(([_a-z0-9]+),\s*([0-9]+)\\)");
    private String classId;
    private int lineNum;

    public Line(String className, int lineNum) {
        this.classId = className;
        this.lineNum = lineNum;
    }

    public Line(String lineStr) {
        Matcher m = REGEX_PATTERN.matcher(lineStr);
        if(m.matches()) {
            this.classId = m.group(1);
            this.lineNum = Integer.parseInt(m.group(2));
        } else {
            this.classId = "";
            this.lineNum = 0;
        }
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String className) {
        this.classId = className;
    }

    public int getLineNum() {
        return lineNum;
    }

    public void setLineNum(int lineNum) {
        this.lineNum = lineNum;
    }

    @Override
    public String toString() {
        return String.join("", PREFIX, "(", classId, ", ", String.valueOf(lineNum), ")");
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Line line) {
            return line.classId != null
                && line.classId.equals(classId)
                && line.lineNum == lineNum;
        }
        return false;
    }

    @Override
    public int compareTo(Line line) {
        if(this.classId != null) {
            int cmp = this.classId.compareTo(line.getClassId());
            if(cmp == 0)
                return this.getLineNum() - line.getLineNum();
            return cmp;
        }
        return 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(classId, lineNum);
    }
}
