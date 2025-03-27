package kr.ac.seoultech.selab.logicfl.coverage;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import logicfl.coverage.CoverageAnalyzer;
import logicfl.coverage.CoverageInfo;
import logicfl.utils.Configuration;
import logicfl.utils.JSONUtils;

public class CoverageAnalyzerTest {
    @Test
    void testExample() {
        String configFilePath = "src/test/resources/config.properties";
        Configuration config = new Configuration(configFilePath);
        CoverageAnalyzer analyzer = new CoverageAnalyzer(configFilePath, config);
        analyzer.run();
        try {
            CoverageInfo coverage = JSONUtils.loadCoverage(config.coverageInfoPath);
            assertTrue(coverage.isCovered("sample.Example", 8));
            assertTrue(coverage.isCovered("sample.Example", 32));
            assertTrue(!coverage.isCovered("sample.ExampleTest", 14));
            assertTrue(coverage.isCovered("sample.Person", 13));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testExampleFilter() {
        String configFilePath = "src/test/resources/config.properties";
        Configuration config = new Configuration(configFilePath);
        config.testsInfoPath = Paths.get("src/test/resources/tests.filter.json");
        config.coverageInfoPath = Paths.get("src/test/resources/coverage.filter.json");
        CoverageAnalyzer analyzer = new CoverageAnalyzer(configFilePath, config);
        analyzer.run();
        try {
            CoverageInfo coverage = JSONUtils.loadCoverage(config.coverageInfoPath);
            assertTrue(coverage.isCovered("sample.Example", 8));
            assertTrue(coverage.isCovered("sample.Example", 32));
            assertTrue(!coverage.isCovered("sample.ExampleTest", 14));
            assertTrue(!coverage.isCovered("sample.Person", 13));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testSample() {
        String configFilePath = "src/test/resources/config.sample.properties";
        Configuration config = new Configuration(configFilePath);
        CoverageAnalyzer analyzer = new CoverageAnalyzer(configFilePath, config);
        try {
            File covInfoFile = config.coverageInfoPath.toFile();
            if(covInfoFile.exists())
                Files.delete(config.coverageInfoPath);
            analyzer.run();
            assertTrue(covInfoFile.exists());
            CoverageInfo coverage = JSONUtils.loadCoverage(config.coverageInfoPath);
            assertTrue(coverage.isCovered("sample.SampleTest", 10));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testSample2() {
        String configFilePath = "src/test/resources/config.sample2.properties";
        Configuration config = new Configuration(configFilePath);
        CoverageAnalyzer analyzer = new CoverageAnalyzer(configFilePath, config);
        try {
            File covInfoFile = config.coverageInfoPath.toFile();
            if(covInfoFile.exists())
                Files.delete(config.coverageInfoPath);
            analyzer.run();
            assertTrue(covInfoFile.exists());
            CoverageInfo coverage = JSONUtils.loadCoverage(config.coverageInfoPath);
            assertTrue(coverage.isCovered("sample.Sample2", 11));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testSample3() {
        String configFilePath = "src/test/resources/config.sample3.properties";
        Configuration config = new Configuration(configFilePath);
        CoverageAnalyzer analyzer = new CoverageAnalyzer(configFilePath, config);
        try {
            File covInfoFile = config.coverageInfoPath.toFile();
            if(covInfoFile.exists())
                Files.delete(config.coverageInfoPath);
            analyzer.run();
            assertTrue(covInfoFile.exists());
            CoverageInfo coverage = JSONUtils.loadCoverage(config.coverageInfoPath);
            assertTrue(coverage.isCovered("sample.Sample3", 20));
            assertTrue(coverage.isCovered("sample.Sample3", 26));
            assertTrue(coverage.isCovered("sample.Sample3", 41));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testSample4() {
        String configFilePath = "src/test/resources/config.sample4.properties";
        Configuration config = new Configuration(configFilePath);
        CoverageAnalyzer analyzer = new CoverageAnalyzer(configFilePath, config);
        try {
            File covInfoFile = config.coverageInfoPath.toFile();
            if(covInfoFile.exists())
                Files.delete(config.coverageInfoPath);
            analyzer.run();
            assertTrue(covInfoFile.exists());
            CoverageInfo coverage = JSONUtils.loadCoverage(config.coverageInfoPath);
            assertTrue(coverage.isCovered("sample.Sample4", 38));
            assertTrue(coverage.isCovered("sample.Sample4", 48));
            assertTrue(coverage.isCovered("sample.Sample4", 56));
            assertTrue(coverage.isCovered("sample.Sample4", 73));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testSample5() {
        String configFilePath = "src/test/resources/config.sample5.properties";
        Configuration config = new Configuration(configFilePath);
        CoverageAnalyzer analyzer = new CoverageAnalyzer(configFilePath, config);
        try {
            File covInfoFile = config.coverageInfoPath.toFile();
            if(covInfoFile.exists())
                Files.delete(config.coverageInfoPath);
            analyzer.run();
            assertTrue(covInfoFile.exists());
            CoverageInfo coverage = JSONUtils.loadCoverage(config.coverageInfoPath);
            assertTrue(coverage.isCovered("sample.Sample5", 26));
            assertTrue(coverage.isCovered("sample.Sample5", 28));
            assertTrue(coverage.isCovered("sample.Sample5", 30));
            assertTrue(coverage.isCovered("sample.Sample5", 36));
            assertTrue(coverage.isCovered("sample.Sample5", 47));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}
