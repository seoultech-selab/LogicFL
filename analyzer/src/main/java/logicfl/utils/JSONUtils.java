package logicfl.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import logicfl.coverage.CoverageInfo;
import logicfl.coverage.NPETrace;
import logicfl.coverage.TestInfo;
import logicfl.probe.LineMatcher;
import logicfl.probe.ProbeRange;

public class JSONUtils {

    public static List<NPETrace> loadTracesFromJSON(Path npeInfoPath) {
        List<NPETrace> traces = new ArrayList<>();
        File npeTraceFile = npeInfoPath.toFile();
        if (npeTraceFile.exists()) {
            try (FileInputStream fis = new FileInputStream(npeTraceFile)) {
                JSONTokener tokener = new JSONTokener(fis);
                JSONObject root = new JSONObject(tokener);
                JSONArray traceArray = root.getJSONArray("npe.traces");
                for (int i = 0; i < traceArray.length(); i++) {
                    JSONObject traceObject = traceArray.getJSONObject(i);
                    NPETrace npeTrace = new NPETrace(traceObject);
                    traces.add(npeTrace);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return traces;
    }

    public static Map<String, List<Integer>> loadTraceInfoFromJSON(Path npeInfoPath, boolean targetOnly, boolean separateInnerClass) {
		Map<String, List<Integer>> traceMap = new HashMap<>();
		if (npeInfoPath.toFile().exists()) {
			try {
                String jsonStr = Files.readString(npeInfoPath);
				JSONObject root = new JSONObject(jsonStr);
				JSONArray traceArray = root.getJSONArray("npe.traces");

				for (int i = 0; i < traceArray.length(); i++) {
					JSONObject traceNode = traceArray.getJSONObject(i);
					JSONArray tracesArray = traceNode.getJSONArray("traces");
					for (int j = 0; j < tracesArray.length(); j++) {
						JSONObject trace = tracesArray.getJSONObject(j);
						boolean isTarget = trace.optBoolean("is_target", true);
						if (targetOnly && !isTarget) {
							continue;
						}
						String className = separateInnerClass ? trace.getString("class")
								: CodeUtils.getIncludingClass(trace.getString("class"));
                        traceMap.putIfAbsent(className, new ArrayList<>());
                        traceMap.get(className).add(trace.getInt("line"));
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return traceMap;
	}

    public static TestInfo loadTestsInfo(Path testsInfoPath) throws IOException {
        String jsonStr = Files.readString(testsInfoPath);
        JSONObject jsonObject = new JSONObject(jsonStr);

        List<String> passedClasses = toStringList(jsonObject.optJSONArray("passed.classes"));
        List<String> failedClasses = toStringList(jsonObject.optJSONArray("failed.classes"));
        List<String> filteredClasses = toStringList(jsonObject.optJSONArray("filtered.classes"));

        TestInfo testsInfo = new TestInfo(passedClasses, failedClasses, filteredClasses, new HashMap<>());

        JSONArray failedTestsArray = jsonObject.optJSONArray("failed.tests");
        if (failedTestsArray != null) {
            for (int i = 0; i < failedTestsArray.length(); i++) {
                JSONObject testNode = failedTestsArray.getJSONObject(i);
                String failedClass = testNode.getString("class");
                String failedTest = testNode.getString("name");
                testsInfo.addFailedTest(failedClass, failedTest);
            }
        }
        return testsInfo;
    }

    public static void exportTestInfo(TestInfo testsInfo, Path testsInfoPath) throws IOException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("passed.classes", new JSONArray(testsInfo.passedClasses));
        jsonObject.put("failed.classes", new JSONArray(testsInfo.failedClasses));
        jsonObject.put("filtered.classes", new JSONArray(testsInfo.filteredClasses));

        JSONArray failedTestsArray = new JSONArray();
        for (Map.Entry<String, List<String>> entry : testsInfo.failedTests.entrySet()) {
            String failedClass = entry.getKey();
            for (String failedTest : entry.getValue()) {
                JSONObject testNode = new JSONObject();
                testNode.put("class", failedClass);
                testNode.put("name", failedTest);
                failedTestsArray.put(testNode);
            }
        }

        jsonObject.put("failed.tests", failedTestsArray);

        String jsonStr = jsonObject.toString(4);
        Files.writeString(testsInfoPath, jsonStr);
    }

    public static CoverageInfo loadCoverage(String path) {
        return loadCoverage(Paths.get(path));
    }

    public static CoverageInfo loadCoverage(Path path) {
        CoverageInfo info = new CoverageInfo();
        if(path.toFile().exists()) {
            try {
                String jsonStr = Files.readString(path);
                JSONObject obj = new JSONObject(jsonStr);

                //Get executed classes.
                JSONArray classes = obj.getJSONArray("classes");
                for(int i=0; i<classes.length(); i++) {
                    info.addClass(classes.getString(i));
                }

                //Get coverage information.
                JSONArray coverage = obj.getJSONArray("coverage");
                for(int i=0; i<coverage.length(); i++) {
                    JSONObject cov = coverage.getJSONObject(i);
                    Map<Integer, Integer> map = new HashMap<>();
                    toList(cov.getJSONArray("covered")).stream().forEach(line -> map.put((Integer)line, 1));
                    info.addCoverage(cov.getString("className"), map);
                }
            } catch (IOException e){
                e.printStackTrace();
            }
        }
        return info;
    }

    public static List<Object> toList(JSONArray jsonArray) {
        List<Object> list = new ArrayList<>();
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(jsonArray.get(i));
            }
        }
        return list;
    }

    public static List<String> toStringList(JSONArray jsonArray) {
        List<String> list = new ArrayList<>();
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(jsonArray.getString(i));
            }
        }
        return list;
    }

    public static void loadLineMatcher(Map<String, LineMatcher> matchers, Path lineInfoPath) {
        File lineInfoFile = lineInfoPath.toFile();
        if (lineInfoFile.exists()) {
            try {
                String jsonStr = Files.readString(lineInfoPath);
                JSONObject root = new JSONObject(jsonStr);
                JSONArray classesArray = root.getJSONArray("classes");

                for (int i = 0; i < classesArray.length(); i++) {
                    JSONObject classInfo = classesArray.getJSONObject(i);
                    String classId = classInfo.getString("classId");
                    LineMatcher matcher = new LineMatcher();
                    JSONArray ranges = classInfo.getJSONArray("probe_ranges");

                    for (int j = 0; j < ranges.length(); j++) {
                        JSONObject range = ranges.getJSONObject(j);
                        ProbeRange probeRange = new ProbeRange(
                                range.getInt(ProbeRange.K_START_LINE),
                                range.getInt(ProbeRange.K_END_LINE),
                                range.getInt(ProbeRange.K_ORG_LINE_START),
                                range.getInt(ProbeRange.K_ORG_LINE_END),
                                range.getInt(ProbeRange.K_PROBED_LINE_END),
                                range.getInt(ProbeRange.K_OFFSET));
                        matcher.addProbeRange(probeRange);
                    }
                    matchers.put(classId, matcher);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void exportLineInfo(Map<String, LineMatcher> matchers, Path lineInfoPath) {
        JSONObject root = new JSONObject();
        JSONArray infoArray = new JSONArray();
        root.put("classes", infoArray);

        for (String classId : matchers.keySet()) {
            JSONObject classInfo = new JSONObject();
            classInfo.put("classId", classId);
            JSONArray ranges = new JSONArray();
            classInfo.put("probe_ranges", ranges);
            LineMatcher matcher = matchers.get(classId);
            matcher.getProbeRanges().forEach((startLine, probeRange) -> {
                JSONObject node = new JSONObject();
                node.put(ProbeRange.K_START_LINE, probeRange.getStartLine());
                node.put(ProbeRange.K_END_LINE, probeRange.getEndLine());
                node.put(ProbeRange.K_ORG_LINE_START, probeRange.getOrgLineStart());
                node.put(ProbeRange.K_ORG_LINE_END, probeRange.getOrgLineEnd());
                node.put(ProbeRange.K_PROBED_LINE_END, probeRange.getProbedLineEnd());
                node.put(ProbeRange.K_OFFSET, probeRange.getOffset());
                ranges.put(node);
            });
            infoArray.put(classInfo);
        }

        try {
            File lineInfoFile = lineInfoPath.toFile();
            try (FileWriter fileWriter = new FileWriter(lineInfoFile)) {
                fileWriter.write(root.toString(4)); // Pretty print with an indent of 4
            }
            System.out.println("Line matching information exported to " + lineInfoPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void exportCoverageInfo(CoverageInfo coverage, Path coverageInfoPath) {
        JSONObject root = new JSONObject();

        // Serialize executed classes
        JSONArray classesArray = new JSONArray();
        coverage.getClasses().stream().sorted().forEach(classesArray::put);
        root.put("classes", classesArray);

        // Serialize coverage information
        JSONArray coverageArray = new JSONArray();
        for (String className : coverage.getClasses()) {
            JSONObject classCoverage = new JSONObject();
            classCoverage.put("className", className);
            Set<Integer> coveredLines = new HashSet<>(coverage.getCoverage(className).keySet());
            JSONArray coveredArray = new JSONArray();
            coveredLines.stream().sorted().forEach(coveredArray::put);
            classCoverage.put("covered", coveredArray);
            coverageArray.put(classCoverage);
        }
        root.put("coverage", coverageArray);

        try (FileWriter fileWriter = new FileWriter(coverageInfoPath.toFile())) {
            fileWriter.write(formatArrayToSingleLine(root));
            System.out.println("Coverage information exported to " + coverageInfoPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String formatArrayToSingleLine(JSONObject jsonObject) {
        String jsonString = jsonObject.toString(4); // Indent with 4 spaces
        // Replace multi-line arrays with single-line arrays
        Pattern pattern = Pattern.compile("\\[\\s*([\\d\\s,\\s]+)\\s*\\]");
        Matcher matcher = pattern.matcher(jsonString);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String replacement = matcher.group(1).replaceAll("\\s+", " ").trim();
            matcher.appendReplacement(sb, "[ " + replacement + " ]");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static void exportMonitorTargets(Map<String, Map<Integer, Set<String>>> monitorTargets, Path monitorTargetPath) {
        JSONObject root = new JSONObject();

        for (Map.Entry<String, Map<Integer, Set<String>>> classEntry : monitorTargets.entrySet()) {
            String className = classEntry.getKey();
            JSONObject classObject = new JSONObject();
            Map<Integer, Set<String>> lineTargets = classEntry.getValue();
            for (Map.Entry<Integer, Set<String>> lineEntry : lineTargets.entrySet()) {
                Integer lineNumber = lineEntry.getKey();
                Set<String> targets = lineEntry.getValue();
                JSONArray targetsArray = new JSONArray(targets);
                classObject.put(String.valueOf(lineNumber), targetsArray);
            }
            root.put(className, classObject);
        }

        try {
            Files.write(monitorTargetPath, root.toString(4).getBytes());
            System.out.println("Monitor targets are exported to " + monitorTargetPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadMonitorTargets(Map<String, Map<Integer, Set<String>>> monitorTargets, Path monitorTargetPath) {
        try {
            String jsonStr = Files.readString(monitorTargetPath);
            JSONObject root = new JSONObject(jsonStr);

            for (String className : root.keySet()) {
                Map<Integer, Set<String>> lineTargets = new HashMap<>();
                JSONObject classObject = root.getJSONObject(className);
                for (String lineNumberStr : classObject.keySet()) {
                    Integer lineNumber = Integer.valueOf(lineNumberStr);
                    JSONArray targetsArray = classObject.getJSONArray(lineNumberStr);
                    Set<String> targets = new HashSet<>();
                    for (int i = 0; i < targetsArray.length(); i++) {
                        targets.add(targetsArray.getString(i));
                    }
                    lineTargets.put(lineNumber, targets);
                }

                monitorTargets.put(className, lineTargets);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void exportExecutionTime(Timer timer, Path execTimePath) {
        JSONObject timerJsonObject = timer.getJsonObject();
        try {
            JSONObject jsonObject;
            if (Files.exists(execTimePath)) {
                String content = Files.readString(execTimePath);
                jsonObject = new JSONObject(content);
            } else {
                jsonObject = new JSONObject();
            }
            jsonObject.put(timer.getName(), timerJsonObject);

            Files.writeString(execTimePath, jsonObject.toString(4));
        } catch (IOException e) {
            e.printStackTrace();
        }        
    }
}
