package logicfl.coverage;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class NPETrace {

    public String testClass;
    public String testMethod;
    public List<StackTrace> traces;

    public NPETrace(String jsonString) {
        loadFromJSON(jsonString);
    }

    public NPETrace(String testClass, String testMethod) {
        this.testClass = testClass;
        this.testMethod = testMethod;
        this.traces = new ArrayList<>();
    }

    public NPETrace(TestId info) {
        this(info.getClassName(), info.getMethodName());
    }

    public NPETrace(JSONObject jsonObject) {
        updateTraces(jsonObject);
    }

    public void addTrace(StackTrace trace) {
        this.traces.add(trace);
    }

    public void addTraces(Throwable throwable, String targetPrefix) {
        for(StackTraceElement e : throwable.getStackTrace()) {
            this.addTrace(new StackTrace(e.getClassName(), e.getMethodName(), e.getLineNumber(), e.getClassName().startsWith(targetPrefix)));
        }
    }

    public JSONObject getJSONObject(boolean targetOnly) {
        JSONObject test = new JSONObject();
        test.put("test.class", testClass);
        test.put("test.method", testMethod);
        JSONArray traceArray = new JSONArray();
        test.put("traces", traceArray);
        for (StackTrace trace : traces) {
            if (targetOnly && !trace.isTarget)
                continue;
            traceArray.put(trace.getJSONObject());
        }
        return test;
    }

    public void loadFromJSON(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            updateTraces(jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateTraces(JSONObject jsonObject) {
        testClass = jsonObject.getString("test.class");
        testMethod = jsonObject.getString("test.method");
        traces = new ArrayList<>();
        JSONArray tracesArray = jsonObject.getJSONArray("traces");
        for (int i = 0; i < tracesArray.length(); i++) {
            JSONObject traceObject = tracesArray.getJSONObject(i);
            StackTrace trace = new StackTrace(
                traceObject.getString("class"),
                traceObject.getString("method"),
                traceObject.getInt("line"),
                traceObject.optBoolean("is_target", true));
            traces.add(trace);
        }
    }

}
