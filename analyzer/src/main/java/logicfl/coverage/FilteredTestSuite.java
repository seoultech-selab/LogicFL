package logicfl.coverage;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

public class FilteredTestSuite extends TestSuite {

	private String fName;
	private List<String> targetTests;
	private int filterOption;
	
	public FilteredTestSuite(Class<?> theClass, List<String> targetTests, int filterOption) {
		this.targetTests = targetTests;
		this.filterOption = filterOption;
		addTestsFromTestClass(theClass);
	}
	
	private void addTestsFromTestClass(final Class<?> theClass) {
		fName= theClass.getName();
		try {
			getTestConstructor(theClass); // Avoid generating multiple error messages
		} catch (NoSuchMethodException e) {
			return;
		}

		if (!Modifier.isPublic(theClass.getModifiers())) {
			return;
		}

		Class<?> superClass= theClass;
		List<String> names= new ArrayList<String>();
		while (Test.class.isAssignableFrom(superClass)) {
			for (Method each : superClass.getDeclaredMethods()){
				String name = theClass.getName()+"#"+each.getName();
				if (filterOption == TestRunnerBuilder.EXCLUDE_TARGETS && targetTests.contains(name)
						|| filterOption == TestRunnerBuilder.ONLY_TARGETS && !targetTests.contains(name)) {
					continue;
				}
				addTestMethod(each, names, theClass);
			}
			superClass= superClass.getSuperclass();
		}
	}

	private void addTestMethod(Method m, List<String> names, Class<?> theClass) {
		String name = m.getName();
		if (names.contains(name))
			return;
		if (! isPublicTestMethod(m)) {
			if (isTestMethod(m))
				addTest(warning("Test method isn't public: "+ m.getName() + "(" + theClass.getCanonicalName() + ")"));
			return;
		}
		names.add(name);
		addTest(createTest(theClass, name));
	}
	
	private boolean isPublicTestMethod(Method m) {
		return isTestMethod(m) && Modifier.isPublic(m.getModifiers());
	}
	 
	private boolean isTestMethod(Method m) {
		return 
			m.getParameterTypes().length == 0 && 
			m.getName().startsWith("test") && 
			m.getReturnType().equals(Void.TYPE);
	}

	/**
	 * @return the fName
	 */
	public String getName() {
		return fName;
	}

	/**
	 * @param fName the fName to set
	 */
	public void setName(String fName) {
		this.fName = fName;
	}
	
}
