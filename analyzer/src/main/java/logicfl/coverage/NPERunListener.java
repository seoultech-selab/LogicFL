package logicfl.coverage;

import java.util.ArrayList;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class NPERunListener extends RunListener {
    private String targetPrefix;
    private List<NPETrace> traces;

    public NPERunListener(String targetPrefix) {
        this.targetPrefix = targetPrefix;
        this.traces = new ArrayList<>();
    }

    public List<NPETrace> getTraces() {
        return this.traces;
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        super.testFailure(failure);
        storeNullPointerExceptions(failure);
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
        super.testAssumptionFailure(failure);
        storeNullPointerExceptions(failure);
    }

    @Override
    public void testFinished(Description description) throws Exception {
        super.testFinished(description);
    }

    @Override
    public void testRunStarted(Description description) throws Exception {
        super.testRunStarted(description);
    }

    private void storeNullPointerExceptions(Failure failure) {
        Throwable throwable = failure.getException();
        Description desc = failure.getDescription();
        if(throwable !=  null) {
            TestId info = new TestId(desc.getClassName(), desc.getMethodName());
            NPETrace npeTrace = new NPETrace(info);
            traces.add(npeTrace);

            if(throwable instanceof NullPointerException) {
                npeTrace.addTraces(throwable, targetPrefix);
            } else if(throwable.getCause() instanceof NullPointerException) {
                npeTrace.addTraces(throwable.getCause(), targetPrefix);
            }
        }
    }
}
