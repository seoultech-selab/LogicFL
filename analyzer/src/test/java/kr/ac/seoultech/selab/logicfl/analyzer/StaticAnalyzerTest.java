package kr.ac.seoultech.selab.logicfl.analyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import logicfl.analyzer.StaticAnalyzer;
import logicfl.coverage.CoverageAnalyzer;
import logicfl.utils.Configuration;

public class StaticAnalyzerTest {

    @Test
    void testGetClassId() {
        StaticAnalyzer analyzer = new StaticAnalyzer("src/test/resources/config.properties");
        assertEquals("test_sample_1", analyzer.getClassId("example.TestSample"));
        assertEquals("test_sample_2", analyzer.getClassId("sample.TestSample"));
        assertEquals("test_sample_3", analyzer.getClassId("sample2.TestSample"));
        assertEquals("person_1", analyzer.getClassId("sample.Person"));
    }

    @Test
    void testExample() {
        String configFilePath = "src/test/resources/config.properties";
        Configuration config = new Configuration(configFilePath);
        checkCoverageInfo(configFilePath, config);
        File factsFile = config.flFactsPath.toFile();
        Path classDir = config.getOutputFilePath("classes");
        StaticAnalyzer analyzer = new StaticAnalyzer(configFilePath);
        analyzer.run();
        try {
            List<String> lines = Files.readAllLines(factsFile.toPath());
            assertTrue(lines.size() > 10);
            assertTrue(lines.stream().anyMatch(s -> s.startsWith("ref")));
            assertTrue(lines.stream().anyMatch(s -> s.startsWith("throw")));
            assertTrue(lines.stream().anyMatch(s -> s.startsWith("cond_expr")));
            assertTrue(classDir.toFile().exists());
            verifyCodeFacts(config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testSample() {
        String configFilePath = "src/test/resources/config.sample.properties";
        Configuration config = new Configuration(configFilePath);
        checkCoverageInfo(configFilePath, config);
        File factsFile = config.flFactsPath.toFile();
        Path classDir = config.getOutputFilePath("classes");
        StaticAnalyzer analyzer = new StaticAnalyzer(configFilePath);
        analyzer.run();
        try {
            List<String> lines = Files.readAllLines(factsFile.toPath());
            assertTrue(lines.size() > 10);
            assertTrue(lines.stream().anyMatch(s -> s.startsWith("ref")));
            assertTrue(lines.stream().anyMatch(s -> s.startsWith("throw")));
            assertTrue(lines.stream().anyMatch(s -> s.startsWith("cond_expr")));
            assertTrue(lines.stream().anyMatch(s -> s.startsWith("trace") && s.contains("run")));
            assertTrue(classDir.toFile().exists());
            verifyCodeFacts(config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testSample2() {
        String configFilePath = "src/test/resources/config.sample2.properties";
        Configuration config = new Configuration(configFilePath);
        checkCoverageInfo(configFilePath, config);
        File factsFile = config.flFactsPath.toFile();
        Path classDir = config.getOutputFilePath("classes");
        StaticAnalyzer analyzer = new StaticAnalyzer(configFilePath);
        analyzer.run();
        try {
            List<String> lines = Files.readAllLines(factsFile.toPath());
            assertTrue(lines.size() > 10);
            assertTrue(lines.stream().anyMatch(s -> s.startsWith("ref")));
            assertTrue(lines.stream().anyMatch(s -> s.startsWith("assign")));
            assertTrue(classDir.toFile().exists());
            verifyCodeFacts(config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testSample4() {
        String configFilePath = "src/test/resources/config.sample4.properties";
        Configuration config = new Configuration(configFilePath);
        checkCoverageInfo(configFilePath, config);
        File factsFile = config.flFactsPath.toFile();
        Path classDir = config.getOutputFilePath("classes");
        StaticAnalyzer analyzer = new StaticAnalyzer(configFilePath);
        analyzer.run();
        try {
            List<String> lines = Files.readAllLines(factsFile.toPath());
            assertTrue(lines.size() > 10);
            assertTrue(lines.stream().anyMatch(s -> s.startsWith("ref")));
            assertTrue(lines.stream().anyMatch(s -> s.startsWith("assign")));
            assertTrue(lines.stream().anyMatch(s -> s.contains("q_field_1")));
            assertTrue(classDir.toFile().exists());
            verifyCodeFacts(config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testSample5() {
        String configFilePath = "src/test/resources/config.sample5.properties";
        Configuration config = new Configuration(configFilePath);
        checkCoverageInfo(configFilePath, config);
        File factsFile = config.flFactsPath.toFile();
        Path classDir = config.getOutputFilePath("classes");
        StaticAnalyzer analyzer = new StaticAnalyzer(configFilePath);
        analyzer.run();
        try {
            List<String> lines = Files.readAllLines(factsFile.toPath());
            assertTrue(lines.size() > 10);
            assertTrue(lines.stream().anyMatch(s -> s.startsWith("assign") && s.contains("field_1")));
            assertTrue(classDir.toFile().exists());
            verifyCodeFacts(config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void verifyCodeFacts(Configuration config) throws IOException {
        File codeFactsFile = config.codeFactsPath.toFile();
        List<String> lines = Files.readAllLines(codeFactsFile.toPath());
        assertTrue(lines.stream().anyMatch(s -> s.startsWith("method")));
        assertTrue(lines.stream().anyMatch(s -> s.startsWith("block")));
        assertTrue(lines.stream().anyMatch(s -> s.startsWith("stmt")));
        assertTrue(lines.stream().anyMatch(s -> s.startsWith("expr")));
        assertTrue(lines.stream().anyMatch(s -> s.startsWith("name")));
    }

    private boolean coverageIsEmpty(Configuration config) {
        return !config.coverageInfoPath.toFile().exists() || config.coverage.getClasses().size() == 0;
    }

    private void checkCoverageInfo(String configFilePath, Configuration config) {
        if(coverageIsEmpty(config)) {
            CoverageAnalyzer analyzer = new CoverageAnalyzer(configFilePath, config);
            analyzer.run();
            config.loadCoverage();
        }
    }
}
