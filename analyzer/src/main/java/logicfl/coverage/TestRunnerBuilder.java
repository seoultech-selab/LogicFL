package logicfl.coverage;

import java.util.List;

import org.junit.Ignore;
import org.junit.internal.builders.IgnoredClassRunner;
import org.junit.runner.Runner;
import org.junit.runners.Suite;
import org.junit.runners.model.RunnerBuilder;

public class TestRunnerBuilder extends RunnerBuilder {

	public static final int ONLY_TARGETS = 0;
	public static final int EXCLUDE_TARGETS = 1;
	public List<String> targetTests;
	public int filterOption;
	
	public TestRunnerBuilder(List<String> targetTests, int filterOption){
		this.targetTests = targetTests;
		this.filterOption = filterOption;
	}
	
	@Override
	public Runner runnerForClass(Class<?> testClass) throws Throwable {
		
		if(isIgnored(testClass)){
			return new IgnoredClassRunner(testClass);
		}else if(hasSuiteAnnotation(testClass)){
			RunnerBuilder builder = new TestRunnerBuilder(targetTests, filterOption);
			return new Suite(testClass, builder);
		}else if(hasSuiteMethod(testClass)){
			return new FilteredSuiteMethod(testClass, targetTests, filterOption);
		}else if(isPre4Test(testClass)){
			return new FilteredJUnit38TestRunner(testClass, targetTests, filterOption);
		}else{
			return new FilteredJUnit4TestRunner(testClass, targetTests, filterOption);
		}
	}

	public static boolean isPre4Test(Class<?> testClass) {
		return junit.framework.TestCase.class.isAssignableFrom(testClass);
	}
	
	public static boolean hasSuiteMethod(Class<?> testClass) {
		try {
			testClass.getMethod("suite");
		} catch (NoSuchMethodException e) {
			return false;
		}
		return true;
	}
	
	public static boolean hasSuiteAnnotation(Class<?> testClass){
		return testClass.getAnnotation(Suite.SuiteClasses.class) != null;
	}
	
	public static boolean isIgnored(Class<?> testClass){
		return testClass.getAnnotation(Ignore.class) != null;
	}
}
