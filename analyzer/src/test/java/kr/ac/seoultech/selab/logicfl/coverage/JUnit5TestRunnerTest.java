package kr.ac.seoultech.selab.logicfl.coverage;

import org.junit.jupiter.api.Test;

import logicfl.coverage.JUnit5TestRunner;

public class JUnit5TestRunnerTest {
    @Test
    void testMain() {
        JUnit5TestRunner.main(new String[]{ "src/test/resources/config.properties", "false" });
        JUnit5TestRunner.main(new String[]{ "src/test/resources/config.properties", "true" });
    }
}
