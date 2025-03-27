package logicfl.coverage;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class JUnit4TestRunner extends TestRunner {

    private static class TestResult {
        public int failedCount = 0;
        public int passedCount = 0;
        public int runCount = 0;
        public List<Failure> failures = new ArrayList<>();
    }

    public static void main(String[] args) {
        int filterOption = prepareTestInfo(args) ? TestRunnerBuilder.ONLY_TARGETS : TestRunnerBuilder.EXCLUDE_TARGETS;

        //Execute Tests.
        try {
            TestResult testResult = new TestResult();

            JUnitCore core = new JUnitCore();
            NPERunListener listener = new NPERunListener(config.targetPackagePrefix);
            core.addListener(listener);
            //Execute Failed Tests.
            for(String testClass : config.testsInfo.failedTests.keySet()) {
                List<String> targetTests = config.testsInfo.failedTests.get(testClass).stream().map(testMethod -> testClass + "#" + testMethod).toList();
                TestRunnerBuilder builder = new TestRunnerBuilder(targetTests, filterOption);
                Result result = core.run(builder.runnerForClass(Class.forName(testClass)));
                combineResults(testResult, result);
            }

            //Execute Passed Tests.
            if (filterOption == TestRunnerBuilder.EXCLUDE_TARGETS) {
                for(String testClass : config.testsInfo.passedClasses) {
                    List<String> targetTests = new ArrayList<>();
                    TestRunnerBuilder builder = new TestRunnerBuilder(targetTests, filterOption);
                    Result result = core.run(builder.runnerForClass(Class.forName(testClass)));
                    combineResults(testResult, result);
                }
            }

            System.out.println("Test execution summary:");
            System.out.println("Tests executed - " + testResult.runCount);
            System.out.println("Tests succeeded - " + testResult.passedCount);
            System.out.println("Tests failed - " + testResult.failedCount);
            if(outputPath != null) {
                try (PrintWriter out = new PrintWriter(new FileOutputStream(Paths.get(outputPath).toFile()))){
                    testResult.failures.forEach(f -> {
                        out.println(f.toString());
                        out.println(f.getMessage());
                        out.println(f.getTrace());
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            testResult.failures.forEach(f -> {
                System.out.println(f.toString());
                System.out.println(f.getMessage());
                System.out.println(f.getTrace());
            });

            List<NPETrace> traces = listener.getTraces();
            storeNPEInfo(traces, TestRunner.npeInfoPath);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void combineResults(TestResult testResult, Result result) {
        testResult.failedCount += result.getFailureCount();
        testResult.passedCount += result.getRunCount() - result.getFailureCount();
        testResult.runCount += result.getRunCount();
        testResult.failures.addAll(result.getFailures());
    }
}
