package logicfl.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import logicfl.coverage.CoverageInfo;
import logicfl.coverage.TestInfo;

public class Configuration {

    public static final String K_SOURCE_PATH = "source.path";
    public static final String K_CLASS_PATH = "class.path";
    public static final String K_COVERAGE_INFO = "coverage.info";

    public static final String JUNIT4 = "junit4";
    public static final String JUNIT5 = "junit5";

    public static final String MONITOR_TRACE = "trace";
    public static final String MONITOR_COVERAGE = "coverage";
    public static final String MONITOR_NULL_ONLY = "null_only";
    public static final String MONITOR_NULL_BOOLEAN = "null_boolean";
    public static final String MONITOR_ALL_VISIBLE = "all_visible";
    public static final String MONITOR_TARGET_ONLY = "target_only";

    public CoverageInfo coverage;
    public String baseDir;
    public String outputDir;
    public String[] srcPath;
    public String[] classPath;
    public String classPathStr;
    public String jvm;
    public Path testsInfoPath;
    public String junitVersion;
    public String jacocoPath;
    public Path jacocoExecPath;
    public String targetPackagePrefix;
    public Path coverageInfoPath;
    public Path npeInfoPath;
    public Path flFactsPath;
    public Path codeFactsPath;
    public Path rulesPath;
    public boolean printDebugInfo;
    public TestInfo testsInfo;
    public Path rootCausePath;
    public Path lineInfoPath;
    public String monitorTarget;
    public String monitorValues;
    public String monitorMethod;
    public boolean coveredOnly;
    public Path faultyLinesPath;
    public Path monitorTargetPath;
    public Path execTimePath;

    public Configuration(String filePath) {
        loadConfig(filePath);
    }

    public void loadConfig(String filePath) {
        try(FileInputStream fis = new FileInputStream(Paths.get(filePath).toFile())){
            Properties prop = new Properties();
            prop.load(fis);

            //Path
            baseDir = prop.getProperty("base.dir", "");
            outputDir = prop.getProperty("output.dir", ".");
            String srcPathStr = prop.getProperty(K_SOURCE_PATH, "");
            srcPath = srcPathStr != null ?
                    Arrays.stream(srcPathStr.split(File.pathSeparator))
                        .map(p -> baseDir + File.separator + p)
                        .toArray(String[]::new)
                    : new String[]{};
            classPathStr = prop.getProperty(K_CLASS_PATH, "");
            setClassPath(classPathStr);

            //Test and Coverage
            jvm = prop.getProperty("jvm", "/usr/bin/java");
            junitVersion = prop.getProperty("junit.version", JUNIT5);
            jacocoPath = prop.getProperty("jacoco.path", "jacocoagent.jar");
            jacocoExecPath = getOutputFilePath(prop.getProperty("jacoco.exec", "jacoco.exec"));
            testsInfoPath = getBaseDirFilePath(prop.getProperty("tests.info", "tests.json"));
            targetPackagePrefix = prop.getProperty("target.prefix", "");
            coverageInfoPath = getBaseDirFilePath(prop.getProperty(K_COVERAGE_INFO, "coverage.json"));
            loadCoverage();

            rulesPath = Paths.get(prop.getProperty("rules.pl", "npe-rules.pl"));

            //Output directory files.
            npeInfoPath = getOutputFilePath(prop.getProperty("npe.info.path", "npe.traces.json"));
            flFactsPath = getOutputFilePath(prop.getProperty("facts.pl", "logic-fl.pl"));
            codeFactsPath = getOutputFilePath(prop.getProperty("code.facts.pl", "code-facts.pl"));
            rootCausePath = getOutputFilePath(prop.getProperty("root.cause", "root_cause.txt"));
            faultyLinesPath = getOutputFilePath(prop.getProperty("fault.loc", "fault_locs.txt"));
            lineInfoPath = getOutputFilePath(prop.getProperty("line.info", "line.info.json"));
            monitorTargetPath = getOutputFilePath(prop.getProperty("monitor.target.path", "monitor.targets.json"));
            execTimePath = getOutputFilePath(prop.getProperty("exec.time.path", "exec.time.json"));

            //Options
            printDebugInfo = Boolean.parseBoolean(prop.getProperty("print.debug.info", "false"));
            monitorTarget = prop.getProperty("monitor.target", MONITOR_TRACE);
            monitorValues = prop.getProperty("monitor.value", MONITOR_NULL_ONLY);
            monitorMethod = prop.getProperty("monitor.method", MONITOR_ALL_VISIBLE);
            coveredOnly = Boolean.parseBoolean(prop.getProperty("covered.only", "true"));
        }catch(IOException e) {
            System.out.println("Error while loading coverage information.");
            e.printStackTrace();
        }
    }

    public void loadCoverage() {
        coverage = JSONUtils.loadCoverage(coverageInfoPath);
    }

    public void setClassPath(String classPathStr) {
        List<String> classPathEntries = new ArrayList<>();
        if(classPathStr != null) {
            for(String entry : classPathStr.split(File.pathSeparator)) {
                if(entry.endsWith("*")) {
                    classPathEntries.addAll(getAllJars(entry.substring(0, entry.length()-1)));
                } else {
                    classPathEntries.add(entry);
                }
            }
        }
        classPath = classPathEntries.toArray(new String[classPathEntries.size()]);
    }

    public Path getBaseDirFilePath(String filePath) {
        Path path = Paths.get(filePath);
        if(path.isAbsolute() || path.startsWith(Paths.get(baseDir)))
            return path;
        return Paths.get(baseDir, filePath);
    }

    public Path getOutputFilePath(String filePath) {
        Path path = Paths.get(filePath);
        if(path.isAbsolute() || path.startsWith(Paths.get(baseDir)))
            return path;
        return Paths.get(baseDir, outputDir, filePath);
    }

    public String getTotalClassPathStr() {
        return String.join(File.pathSeparator, classPath);
    }

    public List<String> getAllJars(String path) {
        List<String> fileList = new ArrayList<>();
        File dir = new File(path);

        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().toLowerCase().endsWith("jar")) {
                        fileList.add(file.getAbsolutePath());
                    }
                }
            }
        }

        return fileList;
    }

    public boolean monitorAllVisible() {
        return this.monitorMethod.equals(MONITOR_ALL_VISIBLE);
    }

    public boolean monitorTargetOnly() {
        return this.monitorMethod.equals(MONITOR_TARGET_ONLY);
    }

    public boolean monitorCoverage() {
        return this.monitorTarget.equals(MONITOR_COVERAGE);
    }

    public boolean monitorNull() {
        return this.monitorValues.equals(MONITOR_NULL_ONLY)
            || this.monitorValues.equals(MONITOR_NULL_BOOLEAN);
    }

    public boolean monitorBoolean() {
        return this.monitorValues.equals(MONITOR_NULL_BOOLEAN);
    }

    public void loadTestsInfo() {
        try {
            testsInfo = JSONUtils.loadTestsInfo(testsInfoPath);
        } catch (IOException e) {
            System.out.println("Cannot load test information from " + testsInfoPath);
            e.printStackTrace();
            System.exit(1);
        }
    }
}