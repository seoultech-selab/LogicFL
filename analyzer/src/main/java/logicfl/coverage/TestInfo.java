package logicfl.coverage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logicfl.utils.CodeUtils;

public class TestInfo {
    public List<String> passedClasses;
    public List<String> failedClasses;
    public List<String> filteredClasses;
    public Map<String, List<String>> failedTests;

    public TestInfo() {
        passedClasses = new ArrayList<>();
        failedClasses = new ArrayList<>();
        filteredClasses = new ArrayList<>();
        failedTests = new HashMap<>();
    }

    public TestInfo(List<String> passedClasses, List<String> failedClasses, List<String> filteredClasses,
            Map<String, List<String>> failedTests) {
        this.passedClasses = passedClasses != null ? passedClasses : new ArrayList<>();
        this.failedClasses = failedClasses != null ? failedClasses : new ArrayList<>();
        this.filteredClasses = filteredClasses != null ? filteredClasses : new ArrayList<>();
        this.failedTests = failedTests;
    }

    public void addFailedTest(String failedClass, String failedTest) {
        failedTests.putIfAbsent(failedClass, new ArrayList<>());
        failedTests.get(failedClass).add(failedTest);
    }

    public boolean isPassed(String className, String methodName) {
        if(className != null && methodName != null) {
            List<String> failed = failedTests.get(className);
            for(String failedTest : failed) {
                if(methodName.equals(failedTest))
                    return false;
            }
            return true;
        }
        return false;
    }

    public boolean isPassed(TestId info) {
        return isPassed(info.getClassName(), info.getMethodName());
    }

    public boolean isTestClass(String className) {
        if(className.indexOf('/') >= 0) {
            className = CodeUtils.pathToQualified(className, false);
        }
        return failedClasses.contains(className) || passedClasses.contains(className);
    }

    public boolean isFiltered(String className) {
        if(className.indexOf('/') >= 0) {
            className = CodeUtils.pathToQualified(className, false);
        }
        return filteredClasses.contains(className);
    }
}