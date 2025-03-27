package logicfl.coverage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class CoverageInfo {

    private Set<String> executedClasses;
    private Map<String, Map<Integer, Integer>> coverage;
    private Map<String, Integer> lineCount;

    public CoverageInfo() {
        executedClasses = new HashSet<>();
        coverage = new HashMap<>();
        lineCount = new HashMap<>();
    }
    public CoverageInfo(Set<String> executedClasses) {
        this.executedClasses = executedClasses;
        coverage = new HashMap<>();
        lineCount = new HashMap<>();
    }

    public Set<String> getClasses() {
        return executedClasses;
    }

    public Map<Integer, Integer> getCoverage(String className) {
        return coverage.get(className);
    }

    public boolean isCovered(String className, int lineNum) {
        return isCovered(className, lineNum, lineNum);
    }

    public boolean isCovered(String className, int startLine, int endLine) {
        if(coverage.containsKey(className)) {
            Map<Integer, Integer> map = coverage.get(className);
            for(int lineNum = startLine; lineNum <= endLine; lineNum++) {
                if(map.containsKey(lineNum) && map.get(lineNum) > 0)
                    return true;
            }

        }
        return false;
    }

    public void addCoverage(String className, Map<Integer, Integer> covered) {
        if(coverage.containsKey(className)) {
            Map<Integer, Integer> map = coverage.get(className);
            for(Entry<Integer, Integer> e : covered.entrySet()) {
                map.compute(e.getKey(), (k, v) -> v==null ? e.getValue() : v+e.getValue());
            }
        } else {
            executedClasses.add(className);
            coverage.put(className, covered);
        }
    }

    public void addCoverage(String className, int lineNum) {
        if(!coverage.containsKey(className)) {
            addClass(className);
        }
        Map<Integer, Integer> map = coverage.get(className);
        map.compute(lineNum, (k, v) -> v == null ? 1 : v+1);
    }

    public void addClass(String className) {
        executedClasses.add(className);
        coverage.putIfAbsent(className, new HashMap<>());
    }

    public void setLineCount(String className, int count) {
        lineCount.put(className, count);
    }

    public Map<String, List<Integer>> getCoveredLines() {
        Map<String, List<Integer>> lines = new HashMap<>();
        for(String className : coverage.keySet()) {
            lines.putIfAbsent(className, new ArrayList<>());
            lines.get(className).addAll(coverage.get(className).keySet());
            Collections.sort(lines.get(className));
        }
        return lines;
    }
}
