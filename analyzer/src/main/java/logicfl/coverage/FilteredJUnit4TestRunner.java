package logicfl.coverage;

import java.util.ArrayList;
import java.util.List;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

public class FilteredJUnit4TestRunner extends BlockJUnit4ClassRunner {

	private List<String> targetTests;
	private int filterOption;
	
	public FilteredJUnit4TestRunner(Class<?> testClass, List<String> targetTests, int filterOption) throws InitializationError {
		super(testClass);
		this.targetTests = targetTests;
		this.filterOption = filterOption;
	}

	@Override
	protected List<FrameworkMethod> computeTestMethods() {
		List<FrameworkMethod> sampleMethods;
		if (targetTests == null) {
			return super.computeTestMethods();
		}else{
			sampleMethods = new ArrayList<FrameworkMethod>();
			for (FrameworkMethod method : super.computeTestMethods()) {
				String name = this.getTestClass().getName() + "#"
						+ method.getName();
				if (filterOption == TestRunnerBuilder.EXCLUDE_TARGETS && targetTests.contains(name)
					|| filterOption == TestRunnerBuilder.ONLY_TARGETS && !targetTests.contains(name)) {
					continue;
				}
				sampleMethods.add(method);
				
			}
			return sampleMethods;
		}		
	}

	
	
}
