package logicfl.coverage;

import org.json.JSONObject;

public class StackTrace {
    public String className;
    public String methodName;
    public int lineNum;
    public boolean isTarget;

    public StackTrace(String className, String methodName, int lineNum, boolean isTarget) {
        this.className = className;
        this.methodName = methodName;
        this.lineNum = lineNum;
        this.isTarget = isTarget;
    }

    public JSONObject getJSONObject() {
        JSONObject entry = new JSONObject();
        entry.put("class", className);
        entry.put("method", methodName);
        entry.put("line", lineNum);
        entry.put("is_target", isTarget);

        return entry;
    }
}