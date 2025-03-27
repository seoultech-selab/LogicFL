package logicfl.analyzer;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import logicfl.coverage.NPETrace;
import logicfl.coverage.StackTrace;
import logicfl.logic.FactManager;
import logicfl.logic.Facts;
import logicfl.logic.Throw;
import logicfl.logic.codefacts.CodeEntity;
import logicfl.logic.codefacts.Line;
import logicfl.probe.LineMatcher;
import logicfl.probe.MethodMatcher;
import logicfl.probe.NodeVisitor;
import logicfl.probe.ProbeInjector;
import logicfl.utils.CodeUtils;
import logicfl.utils.Configuration;
import logicfl.utils.JSONUtils;
import logicfl.utils.Timer;

public class StaticAnalyzer {

    private Configuration config;
    private Facts facts;
    private Map<String, MethodMatcher> methodMatchers;
    private Map<String, Map<Integer, Set<String>>> monitorTargets;

    public StaticAnalyzer(String configFilePath) {
        this(new Configuration(configFilePath));
    }

    public StaticAnalyzer(Configuration config) {
        this.config = config;
        facts = new Facts();
        methodMatchers = new HashMap<>();
        monitorTargets = new HashMap<>();
    }

    public void run() {
        Timer timer = new Timer("static_analyzer");
        timer.setStart();
        NodeVisitor visitor = null;
        List<String> classes = new ArrayList<>(config.coverage.getClasses());
        Collections.sort(classes);
        System.out.println("Total " + classes.size() + " Target classes.");

        try {
            Path srcDir = config.getOutputFilePath("src");
            Path classDir = config.getOutputFilePath("classes");
            deleteDirectory(srcDir);
            deleteDirectory(classDir);

            Map<String, LineMatcher> lineMatchers = new HashMap<>();
            loadTracesFromJSON();

            for(String className : classes) {
                System.out.println("Analyzing class - "+className);
                String source = CodeUtils.getSource(className, config.srcPath);
                if(source == null) {
                    System.out.println("Cannot read the class. Skip analysis on the class.");
                    continue;
                }
                String classId = getClassId(className);
                CompilationUnit cu = CodeUtils.getCompilationUnit(classId, config.classPath, config.srcPath, source);
                MethodMatcher methodMatcher = methodMatchers.get(className);
                visitor = new NodeVisitor(className, classId, cu, config.coverage, facts, config.coveredOnly);
                visitor.setMethodMatcher(methodMatcher);
                cu.accept(visitor);

                //Add remaining throw/3 predicates if there is no other candidate.
                if(methodMatcher != null) {
                    Map<Integer, List<CodeEntity>> candidates = methodMatcher.getCandidates();
                    for(Entry<Integer, List<CodeEntity>> e : candidates.entrySet()) {
                        int lineNum = e.getKey();
                        List<CodeEntity> entities = e.getValue();
                        if(entities.size() == 1) {
                            CodeEntity ce = entities.get(0);
                            Throw t = new Throw(ce.getId(), Facts.NPE_ATOM, new Line(ce.getClassId(), lineNum));
                            visitor.addPredicate(t);
                        }
                    }
                }

                //Store probed classes.
                String newSource = null;
                Path newJavaFile = Paths.get(srcDir.toString(), CodeUtils.qualifiedToPath(className, ".java"));
                if(newJavaFile.getParent().toFile().exists()
                    || newJavaFile.getParent().toFile().mkdirs()) {
                    ProbeInjector probeInjector = new ProbeInjector(cu, source);
                    newSource = probeInjector.inject(visitor.getProbes(), newJavaFile, visitor.getNonInitMap());
                    lineMatchers.put(classId, probeInjector.getLineMatcher());
                }

                //Identify monitor targets, only if target_only is set.
                if(config.monitorTargetOnly())
                    identifyMonitorTargets(className, classId, newSource, visitor.getProbeNames());

            }
            //Compile probed classes.
            boolean success = CodeUtils.compileJavaFiles(srcDir, classDir, config.getTotalClassPathStr());
            if(!success) {
                return;
            }
            FactManager.exportClassFacts(facts, config);
            JSONUtils.exportLineInfo(lineMatchers, config.lineInfoPath);
            if(config.monitorTargetOnly())
                JSONUtils.exportMonitorTargets(monitorTargets, config.monitorTargetPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        timer.setEnd();
        JSONUtils.exportExecutionTime(timer, config.execTimePath);
        System.out.println("Exec. Time - " + timer.getExecTimeStr());
    }

    private void identifyMonitorTargets(String className, String classId, String newSource, Set<String> probeNames) {
        if(newSource == null) {
            System.out.println("Probe injection wasn't successful. Skip identifying monitor targets.");
            return;
        }
        Map<Integer, Set<String>> targetMap = new HashMap<>();
        monitorTargets.put(className, targetMap);
        final CompilationUnit newCu = CodeUtils.getCompilationUnit(classId, config.classPath, config.srcPath, newSource);
        newCu.accept(new ASTVisitor() {
            @Override
            public boolean visit(Assignment node) {
                Expression lhs = node.getLeftHandSide();
                if(lhs instanceof SimpleName n) {
                    addToTargetMap(n);
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(VariableDeclarationStatement node) {
                //Probes only have one fragment.
                if(node.fragments().size() == 1) {
                    VariableDeclarationFragment vdf = (VariableDeclarationFragment)node.fragments().get(0);
                    SimpleName n = vdf.getName();
                    addToTargetMap(n);
                }
                return super.visit(node);
            }

            private void addToTargetMap(SimpleName n) {
                if(probeNames.contains(n.getIdentifier())) {
                    int lineNum = CodeUtils.getStartLine(n, newCu);
                    targetMap.putIfAbsent(lineNum, new HashSet<>());
                    targetMap.get(lineNum).add(n.getIdentifier());
                }
            }
        });
    }

    private void deleteDirectory(Path directory) {
        if(directory.toFile().exists()) {
            try {
                Files.walk(directory)
                    .sorted(Comparator.reverseOrder())
                    .forEach(file -> {
                        try {
                            Files.delete(file);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                });
            } catch (IOException e) {
                e.printStackTrace();
                fail();
            }
        }
    }

    public static void main(String[] args) {
        String configFilePath = "config.properties";
        if(args.length > 0) {
            configFilePath = args[0];
        }
        System.out.println("Getting configurations from "+configFilePath);
        StaticAnalyzer analyzer = new StaticAnalyzer(configFilePath);
        analyzer.run();
    }

    public String getClassId(String className) {
        String simpleLower = CodeUtils.camelToLower(CodeUtils.qualifiedToSimple(className));
        int index = 1;
        String classId = CodeUtils.createClassId(simpleLower, index++);
        while(facts.hasClassId(classId)) {
            classId = CodeUtils.createClassId(simpleLower, index++);
        }
        facts.addClassId(classId, className);
        return classId;
    }

    private void loadTracesFromJSON() {
        List<NPETrace> traces = JSONUtils.loadTracesFromJSON(config.npeInfoPath);
        facts.addTraces(traces);
        updateMethodCallInfo(traces);
    }

    private void updateMethodCallInfo(List<NPETrace> traces) {
        for(NPETrace trace : traces) {
            List<StackTrace> list = trace.traces;
            for(int i=0; i<list.size(); i++) {
                StackTrace curr = list.get(i);
                if(!curr.isTarget || i == 0) {
                    StackTrace next = i+1 < list.size() ? list.get(i+1) : null;
                    //Add a throw predicate for methods called by a target method appeared in stack traces.
                    if(next != null && next.isTarget) {
                        methodMatchers.putIfAbsent(next.className, new MethodMatcher());
                        MethodMatcher matcher = methodMatchers.get(next.className);
                        //Use className as methodName for constructor <init>.
                        if(curr.methodName.equals("<init>")) {
                            String simpleClassName = curr.className.substring(curr.className.lastIndexOf('.')+1);
                            matcher.addMethodCallInfo(curr.className, simpleClassName, next.lineNum);
                        } else {
                            matcher.addMethodCallInfo(curr.className, curr.methodName, next.lineNum);
                        }
                    }
                }
            }
        }
    }
}
