package logicfl.coverage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import logicfl.utils.Configuration;
import logicfl.utils.JSONUtils;

public class TestRunner {

    public static final long DEFAULT_TIMEOUT = 10000;
    public static final String JUNIT5_TEST_RUNNER = "logicfl.coverage.JUnit5TestRunner";
	public static final String JUNIT4_TEST_RUNNER = "logicfl.coverage.JUnit4TestRunner";

    protected static Configuration config;
    protected static String npeInfoPath = null;
    protected static String testsInfoPath = null;
    protected static String outputPath = null;

    protected static boolean prepareTestInfo(String[] args) {
        String configFilePath = "config.properties";
        boolean failedOnly = true;
        switch(args.length) {
            case 5:
                outputPath = args[4];
            case 4:
                testsInfoPath = args[3];
            case 3:
                npeInfoPath = args[2];
            case 2:
                failedOnly = Boolean.parseBoolean(args[1]);
            case 1:
                configFilePath = args[0];
                break;
            default:
                System.out.println("TestRunner {config_file} {failed_only} {npe_info_file} [tests_info_file] [output_file]");
        }
        System.out.println("Load configurations from "+configFilePath);
        config = new Configuration(configFilePath);
        if(npeInfoPath == null)
            npeInfoPath = config.npeInfoPath.toString();
        if(testsInfoPath == null)
            testsInfoPath = config.testsInfoPath.toString();

        System.out.println("Loading test information from "+testsInfoPath);
        try {
            config.testsInfo = JSONUtils.loadTestsInfo(Paths.get(testsInfoPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return failedOnly;
    }

    protected static void storeNPEInfo(List<NPETrace> traces, String npeInfoPath) {
        System.out.println("Store NPE traces to " + npeInfoPath);
        try {
            JSONObject root = new JSONObject();
            JSONArray exceptions = new JSONArray();
            root.put("npe.traces", exceptions);
            for (NPETrace trace : traces) {
                exceptions.put(trace.getJSONObject(false));
            }
            try (FileWriter fileWriter = new FileWriter(new File(npeInfoPath))) {
                fileWriter.write(root.toString(4)); // Pretty print with an indent of 4
            }
            System.out.println("NPE Stack Traces exported to " + npeInfoPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void runTests(String configFilePath, String classPath, String jacocoPath,
        String jacocoExecPath, String npeInfoPath, String testsInfoPath, String outputPath, String jvm, String junitVersion, boolean failedOnly, boolean withCoverage) {
        CommandLine command = CommandLine.parse(jvm);
        command.addArgument("-Xms256m");
        command.addArgument("-Xmx1024m");
        command.addArgument("-classpath");
        command.addArgument(classPath);
        if(withCoverage)
            command.addArgument("-javaagent:"+jacocoPath+"=excludes=org.junit.*,append=false,destfile="+jacocoExecPath);
        if(junitVersion.equals(Configuration.JUNIT4))
            command.addArgument(JUNIT4_TEST_RUNNER);
        else
            command.addArgument(JUNIT5_TEST_RUNNER);
        command.addArgument(configFilePath);
        command.addArgument(String.valueOf(failedOnly));
        command.addArgument(npeInfoPath);
        command.addArgument(testsInfoPath);
        if(outputPath != null)
            command.addArgument(outputPath);
        System.out.println("Executing command - "+String.join(" ", command.toStrings()));

        ExecuteWatchdog watchdog = new ExecuteWatchdog(TestRunner.DEFAULT_TIMEOUT);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setWatchdog(watchdog);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        executor.setExitValue(0);
        executor.setStreamHandler(new PumpStreamHandler(out));

        try {
            executor.execute(command);
            System.out.println(out.toString(StandardCharsets.UTF_8));
        } catch (ExecuteException e) {
            System.err.println("Exit Value:"+e.getExitValue());
            e.printStackTrace();
            System.out.println(out.toString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void runTests(String configFilePath, String classPath, Configuration config, boolean failedOnly) {
        runTests(configFilePath, classPath, config.jacocoPath, config.jacocoExecPath.toString(),
            config.npeInfoPath.toString(), config.testsInfoPath.toString(), null, config.jvm, config.junitVersion, failedOnly, false);
    }

    public static void runTestsWithCoverage(String configFilePath, String classPath, Configuration config, boolean failedOnly) {
        runTests(configFilePath, classPath, config.jacocoPath, config.jacocoExecPath.toString(),
            config.npeInfoPath.toString(), config.testsInfoPath.toString(), null, config.jvm, config.junitVersion, failedOnly, true);
    }

    public static void runTestsWithCoverage(String configFilePath, String classPath, String npeInfoPath, String testsInfoPath, String outputPath, Configuration config, boolean failedOnly) {
        runTests(configFilePath, classPath, config.jacocoPath, config.jacocoExecPath.toString(),
            npeInfoPath, testsInfoPath, outputPath, config.jvm, config.junitVersion, failedOnly, true);
    }

    public static void runTestsWithCoverage(String configFilePath, Configuration config, boolean failedOnly) {
        runTestsWithCoverage(configFilePath, config.classPathStr, config, failedOnly);
    }
}
