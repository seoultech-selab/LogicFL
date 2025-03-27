package kr.ac.seoultech.selab.logicfl.utils;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import logicfl.coverage.CoverageInfo;
import logicfl.coverage.NPETrace;
import logicfl.coverage.TestInfo;
import logicfl.probe.LineMatcher;
import logicfl.probe.ProbeRange;
import logicfl.utils.Configuration;
import logicfl.utils.JSONUtils;

public class JSONUtilsTest {

    private static Configuration config = null;
    private static final String LINE_INFO_FILE = "src/test/resources/result/line.info.test.json";

    @BeforeAll
    static void loadConfiguration() {
        String configFilePath = "src/test/resources/config.properties";
        config = new Configuration(configFilePath);
    }

    @Test
    void testExportCoverageInfo() {
        Path orgFile = config.coverageInfoPath;
        Path newFile = Paths.get(orgFile.getParent().toString(), "test."+orgFile.getFileName().toString());
        try {
            CoverageInfo coverage = JSONUtils.loadCoverage(orgFile.toString());
            JSONUtils.exportCoverageInfo(coverage, newFile);
            assertEquals(Files.readString(orgFile), Files.readString(newFile));
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testLoadExportLineInfo() {
        Map<String, LineMatcher> matchers = new HashMap<>();
        LineMatcher matcher = new LineMatcher();
        matcher.addProbeRange(new ProbeRange(13, 16, 13, 16, 16, 3));
        matcher.addProbeRange(new ProbeRange(17, 20, 14, 14, 20, 6));
        matchers.put("person1", matcher);

        matcher = new LineMatcher();
        matcher.addProbeRange(new ProbeRange(12, 15, 12, 12, 15, 3));
        matcher.addProbeRange(new ProbeRange(16, 20, 13, 13, 20, 7));
        matchers.put("example1", matcher);

        JSONUtils.exportLineInfo(matchers, Paths.get(LINE_INFO_FILE));

        Map<String, LineMatcher> storedMatchers = new HashMap<>();
        JSONUtils.loadLineMatcher(storedMatchers, Paths.get(LINE_INFO_FILE));

        assertTrue(storedMatchers.containsKey("person1"));
        assertEquals(2, storedMatchers.get("person1").getProbeRanges().size());
        assertTrue(storedMatchers.containsKey("example1"));
    }

    @Test
    void testLoadTestsInfo() {
        try {
            TestInfo testInfo = JSONUtils.loadTestsInfo(config.testsInfoPath);
            assertTrue(testInfo.failedClasses.contains("sample.ExampleTest"));
            assertTrue(testInfo.failedTests.containsKey("sample.ExampleTest"));
            assertTrue(testInfo.failedTests.get("sample.ExampleTest").contains("test1"));
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testLoadTraceInfoFromJSON() {
        Map<String, List<Integer>> coverage = JSONUtils.loadTraceInfoFromJSON(config.npeInfoPath, true, true);
        assertTrue(coverage.containsKey("sample.Example"));
        assertTrue(coverage.get("sample.Example").contains(17));
    }

    @Test
    void testLoadTracesFromJSON() {
        List<NPETrace> traces = JSONUtils.loadTracesFromJSON(config.npeInfoPath);
        assertTrue(traces.size() > 0);
        NPETrace trace = traces.get(0);
        assertTrue(trace.traces.size() > 5);
        assertTrue(trace.traces.get(0).isTarget);
        assertEquals("sample.Example", trace.traces.get(0).className);
    }

    @Test
    void testLoadCoverageInfo() {
        CoverageInfo coverage = JSONUtils.loadCoverage(config.coverageInfoPath);
        assertTrue(coverage.getClasses().contains("sample.Example"));
        assertTrue(coverage.isCovered("sample.Example", 17));
    }
}
