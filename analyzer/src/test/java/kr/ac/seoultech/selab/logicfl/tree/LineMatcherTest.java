package kr.ac.seoultech.selab.logicfl.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.jupiter.api.Test;

import logicfl.analyzer.StaticAnalyzer;
import logicfl.probe.LineMatcher;
import logicfl.utils.CodeUtils;
import logicfl.utils.Configuration;

public class LineMatcherTest {
    @Test
    void testGetOriginalLine() {
        String baseDir = "src/test/resources";
        try {
            String source = Files.readString(Paths.get(baseDir, "example_probe_expected.txt"));
            LineMatcher matcher = new LineMatcher();
            CompilationUnit cu = CodeUtils.getCompilationUnit(source);
            matcher.computeLineMapping(cu);
            assertEquals(12, matcher.getOriginalLine(12));
            assertEquals(17, matcher.getOriginalLine(17));
            assertEquals(17, matcher.getOriginalLine(22));
            assertEquals(17, matcher.getOriginalLine(23));
            assertEquals(18, matcher.getOriginalLine(24));
            assertEquals(29, matcher.getOriginalLine(35));
            assertEquals(31, matcher.getOriginalLine(37));
            assertEquals(32, matcher.getOriginalLine(40));
            assertEquals(32, matcher.getOriginalLine(55));
            assertEquals(32, matcher.getOriginalLine(55));
            assertEquals(33, matcher.getOriginalLine(62));
            assertEquals(33, matcher.getOriginalLine(64));
            assertEquals(33, matcher.getOriginalLine(66));
            assertEquals(36, matcher.getOriginalLine(70));
            assertEquals(36, matcher.getOriginalLine(78));
            assertEquals(36, matcher.getOriginalLine(80));
            assertEquals(39, matcher.getOriginalLine(85));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testGetOriginalLineLang0() {
        String configFilePath = "src/test/resources/config.properties.lang0";
        Configuration config = new Configuration(configFilePath);
        String javaClass = "org.apache.commons.lang3.builder.ReflectionToStringBuilder";
        Path javaFilePath = config.getOutputFilePath("src/"+CodeUtils.qualifiedToPath(javaClass, ".java"));
        try {
            String source = Files.readString(javaFilePath);
            LineMatcher matcher = new LineMatcher();
            CompilationUnit cu = CodeUtils.getCompilationUnit(source);
            matcher.computeLineMapping(cu);
            assertEquals(129, matcher.getOriginalLine(129));
            assertEquals(156, matcher.getOriginalLine(158));
            assertEquals(156, matcher.getOriginalLine(160));
            assertEquals(385, matcher.getOriginalLine(389));
            assertEquals(386, matcher.getOriginalLine(397));
            assertEquals(386, matcher.getOriginalLine(400));
            assertEquals(388, matcher.getOriginalLine(401));
            assertEquals(560, matcher.getOriginalLine(573));
            assertEquals(563, matcher.getOriginalLine(576));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testGetOriginalLineSample() {
        String configFilePath = "src/test/resources/config.properties";
        Configuration config = new Configuration(configFilePath);
        config.coveredOnly = false;
        StaticAnalyzer analyzer = new StaticAnalyzer(config);
        analyzer.run();
        String javaClass = "sample.Example";
        Path javaFilePath = config.getOutputFilePath("src/"+CodeUtils.qualifiedToPath(javaClass, ".java"));
        try {
            String source = Files.readString(javaFilePath);
            LineMatcher matcher = new LineMatcher();
            CompilationUnit cu = CodeUtils.getCompilationUnit(source);
            matcher.computeLineMapping(cu);
            assertEquals(12, matcher.getOriginalLine(13));
            assertEquals(13, matcher.getOriginalLine(20));
            assertEquals(17, matcher.getOriginalLine(28));
            assertEquals(18, matcher.getOriginalLine(31));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
