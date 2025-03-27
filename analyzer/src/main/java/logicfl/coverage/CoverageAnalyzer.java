package logicfl.coverage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;

import logicfl.utils.CodeUtils;
import logicfl.utils.Configuration;
import logicfl.utils.JSONUtils;
import logicfl.utils.Timer;

public class CoverageAnalyzer {

    private Configuration config;
	private String configFilePath;

	public CoverageAnalyzer(String configFilePath) {
		this.configFilePath = configFilePath;
		loadConfiguration();
	}

	public CoverageAnalyzer(String configFilePath, Configuration config) {
		this.configFilePath = configFilePath;
		this.config = config;
		config.loadTestsInfo();
	}

	public void loadConfiguration() {
		System.out.println("Load configurations from "+configFilePath);
		config = new Configuration(configFilePath);
		config.loadTestsInfo(); //Load test info. for filtering.
	}

	public static void main(String[] args) {
		String configFilePath = "config.properties";
        if(args.length > 0) {
            configFilePath = args[0];
        }
		CoverageAnalyzer analyzer = new CoverageAnalyzer(configFilePath);
		analyzer.loadConfiguration();
		analyzer.run();
	}

    public void run() {
		Timer timer = new Timer("coverage_analyzer");
        timer.setStart();
		if(config == null) {
			System.out.println("Configuraiton is not loaded properly.");
			return;
		}
		//Collecting coverage for failed tests.
		System.out.println("Collecting coverage from failed tests...");
		CoverageInfo failedCoverage = new CoverageInfo();
		try {
			deletePreviousExecutionOutputs();
			System.out.println("Running Tests...");
			TestRunner.runTestsWithCoverage(configFilePath, config, true);
			System.out.println("Analyzing Coverage Data...");
			analyzeCoverageData(failedCoverage);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Collecting coverage complete.");
		timer.setEnd();
        JSONUtils.exportExecutionTime(timer, config.execTimePath);
        System.out.println("Exec. Time - " + timer.getExecTimeStr());
    }

	private void deletePreviousExecutionOutputs() {
		File executionDataFile = config.jacocoExecPath.toFile();
		executionDataFile.delete();
		File npeTraceFile = config.npeInfoPath.toFile();
		npeTraceFile.delete();
	}

	public void analyzeCoverageData(CoverageInfo coverage) throws IOException {
		analyzeCoverageData(coverage, config.coverageInfoPath, config.npeInfoPath);
	}

	public void analyzeCoverageData(CoverageInfo coverage, Path coverageInfoPath, Path npeInfoPath,
			String classPath, boolean separateInnerClass) throws IOException {
		config.setClassPath(classPath);
		analyzeCoverageData(coverage, coverageInfoPath, npeInfoPath, separateInnerClass);
	}

	public void analyzeCoverageData(CoverageInfo coverage, Path coverageInfoPath, Path npeInfoPath)
			throws IOException {
		analyzeCoverageData(coverage, coverageInfoPath, npeInfoPath, false);
	}

	public void analyzeCoverageData(CoverageInfo coverage, Path coverageInfoPath, Path npeInfoPath,
			boolean separateInnerClass) throws IOException {
		ExecutionDataStore dataStore = readExecutionData(config.jacocoExecPath);
		CoverageBuilder coverageBuilder = new CoverageBuilder();
		Analyzer analyzer = new Analyzer(dataStore, coverageBuilder);
		List<String> classNames = dataStore.getContents().stream()
                .map(ExecutionData::getName)
				.map(CodeUtils::pathToQualified)
				.filter(className -> className.startsWith(config.targetPackagePrefix))
				.filter(className -> !isTestClass(className) || config.testsInfo.isTestClass(className))
				.filter(className -> !config.testsInfo.isFiltered(className))
                .toList();
		System.out.println("Covered Classes:"+classNames);

		//Load classes from class path.
		URL[] jarUrls = getJarURLs();
		try (URLClassLoader classLoader = new URLClassLoader(jarUrls, null)) {
			//Analyzing covered classes.
			for (String className : classNames) {
				String resourcePath = CodeUtils.qualifiedToPath(className, ".class");
				try (InputStream inputStream = classLoader.getResourceAsStream(resourcePath)) {
					analyzer.analyzeClass(inputStream, resourcePath);
				} catch (IOException e) {
					System.out.println("Error while analyzing a class - "+className);
				} catch (NullPointerException e) {
					System.out.println("Error to read class - " + className);
				}
			}
		}

		for (final IClassCoverage cc : coverageBuilder.getClasses()){
			String className = CodeUtils.pathToQualified(cc.getName(), true);
			className = separateInnerClass ? className : CodeUtils.getIncludingClass(className);
			coverage.addClass(className);
			for (int line = cc.getFirstLine(); line <= cc.getLastLine(); line++){
				int status = cc.getLine(line).getStatus();
				if(isCovered(status)){
					coverage.addCoverage(className, line);
				}
			}
			coverage.setLineCount(className, cc.getLineCounter().getTotalCount());
		}

		//Add NPE traces.
		Map<String, List<Integer>> traceMap = JSONUtils.loadTraceInfoFromJSON(npeInfoPath, true, separateInnerClass);
		traceMap.forEach(
			(className, lines) ->
				lines.forEach(line -> coverage.addCoverage(className, line)));

		JSONUtils.exportCoverageInfo(coverage, coverageInfoPath);
	}

	private boolean isTestClass(String className) {
		className = CodeUtils.getIncludingClass(className);
		return className.endsWith("Test")
			|| className.endsWith("TestCase")
			|| className.startsWith("Test");
	}

	private URL[] getJarURLs() {
		URL[] jarUrls = new URL[config.classPath.length];
		for (int i = 0; i < config.classPath.length; i++) {
			try {
				if(config.classPath[i].endsWith(".jar") || config.classPath[i].endsWith("*"))
					jarUrls[i] = new URL("file:" + config.classPath[i]);
				else
					jarUrls[i] = new URL("file:" + config.classPath[i] + "/");
			} catch (MalformedURLException e) {
				System.err.println("Error while getting URL for "+config.classPath[i]);
			}
		}
		return jarUrls;
	}

	private ExecutionDataStore readExecutionData(Path jacocoExecPath) throws IOException {
		FileInputStream in = new FileInputStream(jacocoExecPath.toFile());
		ExecutionDataStore executionDataStore = new ExecutionDataStore();
		SessionInfoStore sessionInfoStore = new SessionInfoStore();
		ExecutionDataReader reader = new ExecutionDataReader(in);
		reader.setSessionInfoVisitor(sessionInfoStore);
		reader.setExecutionDataVisitor(executionDataStore);
		while(reader.read()){ }
		in.close();

		return executionDataStore;
	}

	private static boolean isCovered(final int status) {
        return switch (status) {
            case ICounter.NOT_COVERED -> false;
            case ICounter.PARTLY_COVERED, ICounter.FULLY_COVERED -> true;
            default -> false;
        };
    }
}