package logicfl.coverage;

import java.util.List;

import junit.framework.Test;

import org.junit.internal.runners.JUnit38ClassRunner;

public class FilteredJUnit38TestRunner extends JUnit38ClassRunner {

	public FilteredJUnit38TestRunner(Class<?> klass, List<String> targetTests, int filterOption) {
		this(new FilteredTestSuite(klass, targetTests, filterOption));
	}

	public FilteredJUnit38TestRunner(Test test) {
		super(test);
	}
	
}
