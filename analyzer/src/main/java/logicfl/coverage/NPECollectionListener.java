package logicfl.coverage;

import java.util.ArrayList;
import java.util.List;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

public class NPECollectionListener implements TestExecutionListener {

    private String targetPrefix;
    private List<NPETrace> traces;

    public NPECollectionListener(String targetPrefix) {
        this.targetPrefix = targetPrefix;
        this.traces = new ArrayList<>();
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if(testExecutionResult.getThrowable().isPresent()) {
            TestId info = new TestId(testIdentifier.getUniqueIdObject());
            NPETrace npeTrace = new NPETrace(info);
            traces.add(npeTrace);

            Throwable throwable = testExecutionResult.getThrowable().get();
            if(throwable instanceof NullPointerException) {
                npeTrace.addTraces(throwable, targetPrefix);
            } else if(throwable.getCause() instanceof NullPointerException) {
                npeTrace.addTraces(throwable.getCause(), targetPrefix);
            }
        }
    }

    public List<NPETrace> getTraces() {
        return this.traces;
    }
}
