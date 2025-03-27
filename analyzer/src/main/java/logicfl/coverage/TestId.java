package logicfl.coverage;

import org.junit.platform.engine.UniqueId;

public class TestId {
    private String className;
    private String methodName;

    public TestId(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }

    public TestId(UniqueId uniqueId) {
        className = null;
        methodName = null;
        for(UniqueId.Segment s : uniqueId.getSegments()) {
            if("class".equals(s.getType())) {
                className = s.getValue();
            } else if("method".equals(s.getType())) {
                methodName = s.getValue();
                methodName = methodName.substring(0, methodName.length()-2); //remove ()
            }
        }
    }

    public String getClassName() {
        return className;
    }
    public void setClassName(String className) {
        this.className = className;
    }
    public String getMethodName() {
        return methodName;
    }
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
}
