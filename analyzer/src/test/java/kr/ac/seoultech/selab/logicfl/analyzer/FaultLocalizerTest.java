package kr.ac.seoultech.selab.logicfl.analyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import logicfl.analyzer.FaultLocalizer;
import logicfl.coverage.StackTrace;

public class FaultLocalizerTest {
    @Test
    void testRun() {
        String configFilePath = "src/test/resources/config.properties";
        FaultLocalizer localizer = new FaultLocalizer(configFilePath);
        localizer.run();
    }

    @Test
    void testLoadTracesFromJSON() {
        String configFilePath = "src/test/resources/config.properties";
        FaultLocalizer localizer = new FaultLocalizer(configFilePath);
        localizer.loadTracesFromJSON();
        List<StackTrace> traces = localizer.getCandidates();
        assertEquals(1, traces.size());
        assertEquals("sample.Example", traces.get(0).className);
    }
}