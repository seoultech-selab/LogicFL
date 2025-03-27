package kr.ac.seoultech.selab.logicfl.coverage;

import org.junit.jupiter.api.Test;

import logicfl.coverage.JUnit4TestRunner;

class JUnit4TestRunnerTest {

    @Test
    void main() {
        JUnit4TestRunner.main(new String[]{ "src/test/resources/config.junit4.properties", "false" });
        JUnit4TestRunner.main(new String[]{ "src/test/resources/config.junit4.properties", "true" });
    }
}