package logicfl.coverage;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.List;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

public class JUnit5TestRunner extends TestRunner {

    public static void main(String[] args) {
        boolean failedOnly = prepareTestInfo(args);

        LauncherDiscoveryRequestBuilder builder = LauncherDiscoveryRequestBuilder.request();
        discoverTests(builder, failedOnly);
        LauncherDiscoveryRequest request = builder.build();

        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        NPECollectionListener npeListener = new NPECollectionListener(config.targetPackagePrefix);

        try (LauncherSession session = LauncherFactory.openSession()) {
            Launcher launcher = session.getLauncher();
            launcher.registerTestExecutionListeners(listener);
            launcher.registerTestExecutionListeners(npeListener);
            try{
                launcher.execute(request);
            }catch(Exception e) {
                e.printStackTrace();
            }
        }

        TestExecutionSummary summary = listener.getSummary();
        System.out.println("Test execution summary:");
        System.out.println("Tests found - "+summary.getTestsFoundCount());
        System.out.println("Tests succeeded - "+summary.getTestsSucceededCount());
        System.out.println("Tests failed - "+summary.getTestsFailedCount());
        if(outputPath != null) {
            try (PrintWriter out = new PrintWriter(new FileOutputStream(Paths.get(outputPath).toFile()))){
                summary.printFailuresTo(out);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        PrintWriter out = new PrintWriter(System.out, true);
        summary.printFailuresTo(out);

        //Store NPE information.
        List<NPETrace> traces = npeListener.getTraces();
        storeNPEInfo(traces, TestRunner.npeInfoPath);
    }

    private static void discoverTests(LauncherDiscoveryRequestBuilder builder, boolean failedOnly) {
        LauncherDiscoveryRequestBuilder requestBuilder = builder;

        if(failedOnly) {
            config.testsInfo.failedTests.forEach((failedClass, failedMethods) ->
                failedMethods.forEach(failedMethod ->
                    requestBuilder.selectors(
                            selectMethod(failedClass, failedMethod)
                )));
        } else {
            for (String passedClass : config.testsInfo.passedClasses) {
                requestBuilder.selectors(DiscoverySelectors.selectClass(passedClass));
            }

            //Getting all tests from failed classes.
            LauncherDiscoveryRequestBuilder failedBuilder = LauncherDiscoveryRequestBuilder.request();
            for (String failedClass : config.testsInfo.failedClasses) {
                failedBuilder.selectors(DiscoverySelectors.selectClass(failedClass));
            }

            Launcher launcher = LauncherFactory.create();
            TestPlan testPlan = launcher.discover(failedBuilder.build());
            for(TestIdentifier root : testPlan.getRoots()) {
                for(TestIdentifier t : testPlan.getDescendants(root)) {
                    if(t.isTest()) {
                        TestId info = new TestId(t.getUniqueIdObject());
                        if(config.testsInfo.isPassed(info))
                            requestBuilder.selectors(
                                selectMethod(info.getClassName(), info.getMethodName()));
                    }
                }
            }
        }
    }
}
