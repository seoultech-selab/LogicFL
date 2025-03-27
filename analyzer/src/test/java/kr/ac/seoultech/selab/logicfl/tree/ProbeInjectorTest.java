package kr.ac.seoultech.selab.logicfl.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.jupiter.api.Test;

import logicfl.coverage.CoverageInfo;
import logicfl.probe.NodeVisitor;
import logicfl.probe.Probe;
import logicfl.probe.ProbeInjector;
import logicfl.utils.CodeUtils;
import logicfl.utils.JSONUtils;

public class ProbeInjectorTest {

    private static CompilationUnit cu;
    private static NodeVisitor visitor;

    @Test
    void testInjectExample() {
        String baseDir = "src/test/java";
        String resourceDir = "src/test/resources";
        String className = "Example";
        String javaFile = className + ".java";
        String classId = className.toLowerCase();
        Path expectedFile = Paths.get(resourceDir, classId + "_probe_expected.txt");
        Path outFile = Paths.get(resourceDir, classId + "_probe_injected.txt");
        try {
            injectProbes(baseDir, javaFile, className, classId, resourceDir, outFile);
            assertEquals(Files.readString(expectedFile), Files.readString(outFile));
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testInjectSample() {
        String baseDir = "src/test/java";
        String resourceDir = "src/test/resources";
        String className = "Sample";
        String javaFile = className + ".java";
        String classId = className.toLowerCase();
        Path expectedFile = Paths.get(resourceDir, classId + "_probe_expected.txt");
        Path outFile = Paths.get(resourceDir, classId + "_probe_injected.txt");
        try {
            injectProbes(baseDir, javaFile, className, classId, resourceDir, outFile);
            assertEquals(Files.readString(expectedFile), Files.readString(outFile));
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testInjectSample3() {
        String baseDir = "src/test/java";
        String resourceDir = "src/test/resources";
        String className = "Sample3";
        String javaFile = className + ".java";
        String classId = className.toLowerCase();
        Path expectedFile = Paths.get(resourceDir, classId + "_probe_expected.txt");
        Path outFile = Paths.get(resourceDir, classId + "_probe_injected.txt");
        try {
            injectProbes(baseDir, javaFile, className, classId, resourceDir, outFile);
            assertEquals(Files.readString(expectedFile), Files.readString(outFile));
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testInjectSample4() {
        String baseDir = "src/test/java";
        String resourceDir = "src/test/resources";
        String className = "Sample4";
        String javaFile = className + ".java";
        String classId = className.toLowerCase();
        Path expectedFile = Paths.get(resourceDir, classId + "_probe_expected.txt");
        Path outFile = Paths.get(resourceDir, classId + "_probe_injected.txt");
        try {
            injectProbes(baseDir, javaFile, className, classId, resourceDir, outFile);
            assertEquals(Files.readString(expectedFile), Files.readString(outFile));
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testInjectSample5() {
        String baseDir = "src/test/java";
        String resourceDir = "src/test/resources";
        String className = "Sample5";
        String javaFile = className + ".java";
        String classId = className.toLowerCase();
        Path expectedFile = Paths.get(resourceDir, classId + "_probe_expected.txt");
        Path outFile = Paths.get(resourceDir, classId + "_probe_injected.txt");
        try {
            injectProbes(baseDir, javaFile, className, classId, resourceDir, outFile);
            assertEquals(Files.readString(expectedFile), Files.readString(outFile));
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    private void injectProbes(String baseDir, String javaFile, String className, String classId,
            String resourceDir, Path outFile) throws IOException {
        String source = Files.readString(Paths.get(baseDir, "sample", javaFile));
        cu = CodeUtils.getCompilationUnit(javaFile, new String[]{}, new String[]{ baseDir }, source);
        CoverageInfo coverage = JSONUtils.loadCoverage(Paths.get(resourceDir, "probe." + classId + ".coverage.json"));
        visitor = new NodeVisitor("sample."+className, classId, cu, coverage);
        cu.accept(visitor);

        ProbeInjector probeInjector = new ProbeInjector(cu, source);
        List<Probe> probes = visitor.getProbes();
        probeInjector.inject(probes, outFile, visitor.getNonInitMap());
    }
}
