package logicfl.analyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.jdi.BooleanValue;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Mirror;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.StepRequest;

import logicfl.coverage.CoverageAnalyzer;
import logicfl.coverage.CoverageInfo;
import logicfl.coverage.JUnit4TestRunner;
import logicfl.coverage.JUnit5TestRunner;
import logicfl.coverage.TestRunner;
import logicfl.logic.FactManager;
import logicfl.logic.Val;
import logicfl.logic.codefacts.Line;
import logicfl.logic.codefacts.NameRef;
import logicfl.probe.LineMatcher;
import logicfl.probe.NodeVisitor;
import logicfl.utils.CodeUtils;
import logicfl.utils.Configuration;
import logicfl.utils.JSONUtils;
import logicfl.utils.Timer;

public class DynamicAnalyzer {

    private static final String JUNIT5_TEST_RUNNER = JUnit5TestRunner.class.getName();
    private static final String JUNIT4_TEST_RUNNER = JUnit4TestRunner.class.getName();
    public static final String PROBE_NPE_TRACE_INFO = "probe.npe.traces.json";
    private static final String PROBE_COVERAGE_INFO = "probe.coverage.json";

    private Configuration config;
    private String configFilePath;
    private Map<String, String> classMap;
    private Map<String, String> variableMap;
    private Map<String, Val> predicates;
    private Path npeInfoPath;
    private Path coverageInfoPath;
    private Map<String, LineMatcher> matchers;
    private Map<String, Map<Integer, Set<String>>> monitorTargets;

    public DynamicAnalyzer(String configFilePath) {
        this.configFilePath = configFilePath;
        config = new Configuration(configFilePath);
        config.loadTestsInfo();
        classMap = new HashMap<>();
        variableMap = new HashMap<>();
        predicates = new HashMap<>();
        npeInfoPath = config.getOutputFilePath(PROBE_NPE_TRACE_INFO);
        coverageInfoPath = config.getOutputFilePath(PROBE_COVERAGE_INFO);
        matchers = new HashMap<>();
        monitorTargets = new HashMap<>();
    }

    public Configuration getConfig() {
        return this.config;
    }

    public void run() {
        Timer timer = new Timer("dynamic_analyzer");
        timer.setStart();
        //Load NPE stack trace information.
        Map<String, List<Integer>> targets = null;
        String classPath = config.getOutputFilePath("classes").toAbsolutePath() + File.pathSeparator + config.classPathStr;
        CoverageAnalyzer analyzer = new CoverageAnalyzer(configFilePath);
        config.npeInfoPath = npeInfoPath;
        config.coverageInfoPath = coverageInfoPath;
        if(config.monitorCoverage()) {
            TestRunner.runTestsWithCoverage(configFilePath, classPath, config, true);
            CoverageInfo coverage = new CoverageInfo();
            try {
                analyzer.analyzeCoverageData(coverage, coverageInfoPath, npeInfoPath, classPath, true);
                targets = coverage.getCoveredLines();
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Failed to analyze coverage. Using traces instead.");
                targets = JSONUtils.loadTraceInfoFromJSON(npeInfoPath, true, true);
            }
        } else {
            TestRunner.runTests(configFilePath, classPath, config, true);
            targets = JSONUtils.loadTraceInfoFromJSON(npeInfoPath, true, true);
        }

        //Load class & variable references from predicates.
        loadFromPredicates();

        //Load line matcher.
        JSONUtils.loadLineMatcher(matchers, config.lineInfoPath);

        //Load monitor target information.
        if(config.monitorTargetOnly())
            JSONUtils.loadMonitorTargets(monitorTargets, config.monitorTargetPath);

        //Attach debugger and collect values.
        VirtualMachine vm = null;
        EventSet eventSet = null;
        try {
            LaunchingConnector connector = Bootstrap.virtualMachineManager().defaultConnector();
            Map<String, Connector.Argument> options = connector.defaultArguments();
            String runnerClass = config.junitVersion.equals(Configuration.JUNIT4) ?
                JUNIT4_TEST_RUNNER : JUNIT5_TEST_RUNNER;
            String commandLine = String.join(" ", runnerClass, configFilePath, "true", npeInfoPath.toString());
            options.get("main").setValue(commandLine);
            options.get("options").setValue("-cp " + classPath);
            vm = connector.launch(options);

            EventRequestManager eventRequestManager = vm.eventRequestManager();

            ClassPrepareRequest classPrepareRequest = null;
            for(String className : targets.keySet()) {
                classPrepareRequest = vm.eventRequestManager().createClassPrepareRequest();
                classPrepareRequest.addClassFilter(className);
                classPrepareRequest.enable();
            }

            boolean vmDisconnected = false;
            String className = null;
            int lineNum = -1;
            while (!vmDisconnected) {
                eventSet = vm.eventQueue().remove();
                for (Event event : eventSet) {
                    if (event instanceof VMDisconnectEvent) {
                        vmDisconnected = true;
                    } else if (event instanceof BreakpointEvent) {
                        LocatableEvent e = (LocatableEvent)event;
                        Location loc = e.location();
                        ReferenceType clazz = loc.declaringType();
                        className = CodeUtils.getIncludingClass(clazz.name());
                        lineNum = loc.lineNumber();
                        if(config.monitorAllVisible()) {
                            StackFrame top = e.thread().frame(0);
                            List<LocalVariable> visibleVariables = top.visibleVariables();
                            for (LocalVariable variable : visibleVariables) {
                                addValue(top.getValue(variable), variable, className, lineNum);
                            }
                        } else if(config.monitorTargetOnly() && monitorTargets.containsKey(className)) {
                            Set<String> variables = monitorTargets.get(className).get(lineNum);
                            if(variables != null && variables.size() > 0) {
                                fireStepOverRequest(eventRequestManager, e.thread());
                            }
                        }
                    } else if (event instanceof StepEvent stepEvent) {
                        StackFrame top = stepEvent.thread().frame(0);
                        if(config.monitorTargetOnly() && monitorTargets.containsKey(className)) {
                            Set<String> variables = monitorTargets.get(className).get(lineNum);
                            if(variables != null) {
                                for (String varName : variables) {
                                    LocalVariable variable = top.visibleVariableByName(varName);
                                    if(variable == null && config.printDebugInfo) {
                                        System.out.printf("The variable %s is not visible at line %d.\n", varName, lineNum);
                                        continue;
                                    }
                                    addValue(top.getValue(variable), variable, className, lineNum);
                                }
                            }
                        }
                        // Disable the step request after the step over
                        StepRequest curr = (StepRequest)stepEvent.request();
                        curr.disable();
                        eventRequestManager.deleteEventRequest(curr);
                    } else if (event instanceof ClassPrepareEvent cpe) {
                        ReferenceType classType = cpe.referenceType();
                        className = classType.name();
                        Location loc = null;
                        BreakpointRequest breakpointRequest = null;
                        if(targets.containsKey(className)) {
                            for(Integer lineNumber : targets.get(className)) {
                                List<Location> locations = classType.locationsOfLine(lineNumber);
                                if(locations.size() == 0) {
                                    System.err.printf("No break point locations at %s (line %d)\n", className, lineNumber);
                                    continue;
                                }
                                loc = locations.get(locations.size()-1);
                                breakpointRequest = eventRequestManager.createBreakpointRequest(loc);
                                breakpointRequest.enable();
                            }
                        }
                    } else if (event instanceof VMStartEvent) {
                        vm.resume();
                    }
                }
                eventSet.resume();
            }
        } catch (Exception e) {
            e.printStackTrace();
            if(vm != null) {
                vm.exit(1);
            }
        } finally {
            if(config.printDebugInfo) {
                printOutput(vm);
            }
        }
        exportPredicates();
        timer.setEnd();
        JSONUtils.exportExecutionTime(timer, config.execTimePath);
        System.out.println("Exec. Time - " + timer.getExecTimeStr());
    }

    private void addValue(Value value, Mirror variable, String className, int lineNum) {
        //Add val/3 predicate if value is null.
        if(config.monitorNull() && value == null) {
            addValPredicate(className, lineNum, variable, "null");
        } else if(config.monitorBoolean() && value instanceof BooleanValue) {
            addValPredicate(className, lineNum, variable, value.toString());
        }
    }

    private void fireStepOverRequest(EventRequestManager eventRequestManager, ThreadReference thread) {
        //Make sure previous step requests are removed.
        for(StepRequest stepRequest : eventRequestManager.stepRequests()) {
            stepRequest.disable();
            eventRequestManager.deleteEventRequest(stepRequest);
        }
        StepRequest stepRequest = eventRequestManager.createStepRequest(
            thread,
            StepRequest.STEP_LINE,
            StepRequest.STEP_OVER
        );
        stepRequest.addCountFilter(1); // Next step only
        stepRequest.enable();
    }

    private void printOutput(VirtualMachine vm) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(vm.process().getInputStream()))) {
            reader.lines().forEach(line -> System.out.println(line));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(vm.process().getErrorStream()))) {
            String s = "";
            while((s = reader.readLine()) != null) {
                System.out.println(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportPredicates() {
        try {
            Set<Val> uniqueValues = new HashSet<>(this.predicates.values());
            List<Val> predicates = new ArrayList<>(uniqueValues);
            predicates.sort((p1, p2) -> p1.getLine().compareTo(p2.getLine()));
            String facts = FactManager.exportDynamicFacts(config.flFactsPath, predicates);
            System.out.println(facts);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadFromPredicates() {
        try {
            List<String> lines = Files.readAllLines(config.codeFactsPath);
            for(String s : lines) {
                if(s.startsWith(NameRef.PREFIX+"(")) {
                    NameRef ref = new NameRef(s);
                    if(ref != null && ref.isVariable()) {
                        addVariable(ref, variableMap);
                    }
                } else if(s.startsWith("class(")) {
                    FactManager.parseClassPredicate(s, classMap);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addVariable(NameRef ref, Map<String, String> variables) {
        variables.put(ref.getKey(), ref.getId());
    }

    private void addValPredicate(String className, int locLineNum, Mirror variable, String value) {
        String varName = null;
        if(variable instanceof LocalVariable var) {
            varName = var.name();
        } else if(variable instanceof Field field) {
            varName = field.name();
        }
        if(config.printDebugInfo)
            System.out.println(varName + " - " + value);
        String classRef = classMap.containsKey(className)
                        ? classMap.get(className)
                        : className;
        if (varName.contains(NodeVisitor.LINE_SEP)){
            int index = varName.lastIndexOf(NodeVisitor.LINE_SEP);
            int pIndex = varName.indexOf(NodeVisitor.PROBE_INDEX_SEP, index);
            int lineNum = Integer.parseInt(
                    varName.substring(index + NodeVisitor.LINE_SEP.length(),
                        pIndex < 0 ? varName.length() : pIndex));
            String varKey = varName.substring(0, index);
            Line line = new Line(classRef, lineNum);
            Val val = new Val(varKey, value, line);
            predicates.put(varName, val);
        } else {
            LineMatcher matcher = matchers.get(classRef);
            int lineNum = matcher == null ? locLineNum : matcher.getOriginalLine(locLineNum);
            String key = classRef + NodeVisitor.CLASS_SEP + varName + NodeVisitor.LINE_SEP + lineNum;
            if(variableMap.containsKey(key)) {
                Line line = new Line(classRef, lineNum);
                Val val = new Val(variableMap.get(key), value, line);
                predicates.put(key, val);
            } else if(config.printDebugInfo) {
                System.out.println("Cannot find the variable.");
                System.out.println("variable:" + varName);
                System.out.println("value:" + value);
                System.out.println("key:" + key);
            }
        }
    }

    public static void main(String[] args) {
        String configFilePath = "config.properties";
        if(args.length > 0) {
            configFilePath = args[0];
        }
        System.out.println("Getting configurations from "+configFilePath);

        DynamicAnalyzer analyzer = new DynamicAnalyzer(configFilePath);
        analyzer.run();
    }
}
