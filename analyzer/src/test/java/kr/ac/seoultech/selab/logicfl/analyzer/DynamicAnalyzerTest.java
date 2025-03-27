package kr.ac.seoultech.selab.logicfl.analyzer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Test;

import logicfl.analyzer.DynamicAnalyzer;
import logicfl.analyzer.StaticAnalyzer;
import logicfl.utils.Configuration;

public class DynamicAnalyzerTest {
    @Test
    void testExample() {
        try{
            String configFilePath = "src/test/resources/config.properties";
            StaticAnalyzer staticAnalyzer = new StaticAnalyzer(configFilePath);
            staticAnalyzer.run();
            DynamicAnalyzer analyzer = new DynamicAnalyzer(configFilePath);
            analyzer.run();
            List<String> lines = getFactStrings(analyzer.getConfig());
            assertTrue(lines.stream().anyMatch(s -> s.startsWith("val") && s.contains("null")));
            assertTrue(!lines.get(0).contains("."));
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testSample() {
        try{
            String configFilePath = "src/test/resources/config.sample.properties";
            StaticAnalyzer staticAnalyzer = new StaticAnalyzer(configFilePath);
            staticAnalyzer.run();
            DynamicAnalyzer analyzer = new DynamicAnalyzer(configFilePath);
            analyzer.run();
            List<String> lines = getFactStrings(analyzer.getConfig());
            assertTrue(lines.stream().anyMatch(s -> s.startsWith("val") && s.contains("null")));
            assertTrue(lines.stream().anyMatch(s -> s.startsWith("val") && (s.contains("true") || s.contains("false"))));
            assertTrue(!lines.get(0).contains("."));
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testSample2() {
        try{
            String configFilePath = "src/test/resources/config.sample2.properties";
            StaticAnalyzer staticAnalyzer = new StaticAnalyzer(configFilePath);
            staticAnalyzer.run();
            DynamicAnalyzer analyzer = new DynamicAnalyzer(configFilePath);
            analyzer.run();
            List<String> lines = getFactStrings(analyzer.getConfig());
            assertTrue(lines.stream().anyMatch(s -> s.startsWith("val") && s.contains("null")));
            assertTrue(!lines.get(0).contains("."));
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testSample3() {
        try{
            String configFilePath = "src/test/resources/config.sample3.properties";
            StaticAnalyzer staticAnalyzer = new StaticAnalyzer(configFilePath);
            staticAnalyzer.run();
            DynamicAnalyzer analyzer = new DynamicAnalyzer(configFilePath);
            analyzer.run();
            List<String> lines = getFactStrings(analyzer.getConfig());
            assertTrue(lines.stream().anyMatch(s -> s.startsWith("val") && s.contains("null")));
            assertTrue(!lines.get(0).contains("."));
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testSample4() {
        try{
            String configFilePath = "src/test/resources/config.sample4.properties";
            StaticAnalyzer staticAnalyzer = new StaticAnalyzer(configFilePath);
            staticAnalyzer.run();
            DynamicAnalyzer analyzer = new DynamicAnalyzer(configFilePath);
            analyzer.run();
            List<String> lines = getFactStrings(analyzer.getConfig());
            assertTrue(lines.stream().anyMatch(s -> s.startsWith("val") && s.contains("null")));
            assertTrue(lines.stream().anyMatch(s -> s.startsWith("val")
                && s.contains("null") && s.contains("f_field")));
            assertTrue(lines.stream().anyMatch(s -> s.startsWith("val") &&
                (s.contains("true") || s.contains("false"))));
            assertTrue(!lines.get(0).contains("."));
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testSample5() {
        try{
            String configFilePath = "src/test/resources/config.sample5.properties";
            StaticAnalyzer staticAnalyzer = new StaticAnalyzer(configFilePath);
            staticAnalyzer.run();
            DynamicAnalyzer analyzer = new DynamicAnalyzer(configFilePath);
            analyzer.run();
            List<String> lines = getFactStrings(analyzer.getConfig());
            assertTrue(lines.stream().anyMatch(s -> s.startsWith("val")
                && s.contains("null") && s.contains("f_field_1")));
            assertTrue(!lines.get(0).contains("."));
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    private List<String> getFactStrings(Configuration config) throws IOException {
        File factsFile = config.flFactsPath.toFile();
        List<String> lines = Files.readAllLines(factsFile.toPath());
        return lines;
    }
}
