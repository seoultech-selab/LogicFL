package logicfl.coverage;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.junit.internal.runners.JUnit38ClassRunner;

public class FilteredSuiteMethod extends JUnit38ClassRunner {

	public FilteredSuiteMethod(Class<?> klass, List<String> targetTests, int filterOption) throws Throwable{
		super(testFromSuiteMethod(klass, targetTests, filterOption));
	}

	public static Test testFromSuiteMethod(Class<?> klass, List<String> targetTests, int filterOption) throws Throwable{
		Method suiteMethod= null;
		TestSuite suite= null;
		try {
			suiteMethod= klass.getMethod("suite");
			if (!Modifier.isStatic(suiteMethod.getModifiers())) {
				throw new Exception(klass.getName() + ".suite() must be static");
			}
			suite= (TestSuite) suiteMethod.invoke(null); // static method
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
		suite = getTestsFromTestSuite(klass.getName(), suite, targetTests, filterOption);
		
		return suite;
	}
	
	private static TestSuite getTestsFromTestSuite(String testClassName, TestSuite suite, List<String> targetTests, int filterOption) {
		Enumeration<Test> suiteTests = suite.tests();
		TestSuite newSuite = new TestSuite();
		while(suiteTests.hasMoreElements()){
			Test test = suiteTests.nextElement();
			if(test instanceof TestCase){
				String name = test.getClass().getName()+"#"+((TestCase) test).getName();
				if (filterOption == TestRunnerBuilder.EXCLUDE_TARGETS && targetTests.contains(name)
						|| filterOption == TestRunnerBuilder.ONLY_TARGETS && !targetTests.contains(name)) {
					continue;
				}
				newSuite.addTest(test);
			}else if(test instanceof TestSuite){
				TestSuite testSuite = getTestsFromTestSuite(testClassName, (TestSuite)test, targetTests, filterOption);
				Enumeration<Test> tests = testSuite.tests();
				while(tests.hasMoreElements()){
					newSuite.addTest(tests.nextElement());
				}
			}
		}
		
		return newSuite;
	}
}
