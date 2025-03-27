package logicfl.logic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.dom.ASTNode;

import logicfl.coverage.NPETrace;
import logicfl.coverage.StackTrace;
import logicfl.logic.codefacts.CodeEntity;
import logicfl.logic.codefacts.Line;
import logicfl.logic.codefacts.NameRef;
import logicfl.utils.CodeUtils;
import logicfl.utils.Configuration;

public class FactManager {

    public static final String DISABLE_DISCONTIGUOUS = ":- style_check(-discontiguous).\n";
    public static final String MARKER_STATIC_START = "%%% Logic-FL Facts\n";
    public static final String MARKER_STATIC_END = "%%% End of Static Facts\n\n";
    public static final String MARKER_CODE_START = "%%% Code Facts\n";
    public static final String MARKER_CODE_END = "%%% End of Code Facts";
    public static final String MARKER_CLASSES = "%%% Classes";
    public static final String MARKER_METHODS = "%%% Methods";
    public static final String MARKER_BLOCKS = "%%% Blocks";
    public static final String MARKER_STATEMENTS = "%%% Statements";
    public static final String MARKER_EXPRESSIONS = "%%% Expressions";
    public static final String MARKER_NAMES = "%%% Names";
    public static final String MARKER_LITERALS = "%%% Literals";
    public static final String MARKER_CODE_ENTITIES = "%%% Other Code Entities";
    public static final String MARKER_NAME_REFS = "%%% Name References";
    public static final String MARKER_DYNAMIC_START = "%%% Values\n";
    public static final String MARKER_DYNAMIC_END = "%%% End of Facts";
    public static Pattern pClassPredicate = Pattern.compile("class\\(([_a-z0-9]+), "+getQuotedString("(.*)")+"\\).*");

    public static String getFactStrings(List<? extends Predicate> predicates) {
        return getFactStrings(predicates, false);
    }

    public static String getFactStrings(List<? extends Predicate> predicates, boolean sortByPredicate) {
        if(sortByPredicate) {
            predicates = new ArrayList<>(predicates);
            Collections.sort(predicates, (p1, p2)
                -> (p1.getPredicateName().compareTo(p2.getPredicateName())));
        }
        StringBuffer sb = new StringBuffer();
        for(Predicate p : predicates) {
            sb.append(p.createTerm());
            sb.append(".\n");
        }
        return sb.toString();
    }

    public static String getNumberString(String s) {
        String numStr = null;
        char lastChar = s.charAt(s.length()-1);
        switch(lastChar) {
            case 'd':
            case 'D':                
            case 'f':
            case 'F':
                //Skip if it's an octal or a hexadecimal number.
                if(s.startsWith("0x") || (s.startsWith("0") && !s.startsWith("0.")))
                    break;
                numStr = s.substring(0, s.length()-1);
                numStr = String.valueOf(Double.parseDouble(numStr));
                break;
            case 'l':
            case 'L':
                numStr = s.substring(0, s.length()-1);
                break;
        }
        if(numStr == null) {
            //For hexadecimal floating point, use the token itself for the moment.
            if(s.contains(".") && s.contains("x"))
                numStr = getQuotedString(s);
            else
                numStr = s;
        }
        if(numStr.endsWith("."))
            numStr += "0";
        return numStr;
    }

    public static String getEscapedString(String s) {
        String escaped = s.trim()
                            .replaceAll("\\\\", "\\\\\\\\")
                            .replaceAll("\n", "\\\\n")
                            .replaceAll("\r", "\\\\r")
                            .replaceAll("\"", "\\\\\"");
        return escaped;
    }

    public static String getDoubleQuotedString(String str) {
        return String.join("", "\"", str, "\"");
    }

    public static String getQuotedString(String str) {
        return String.join("", "'", str, "'");
    }

    public static String getQuotedString(ASTNode node) {
        return getQuotedString(getEscapedString(node.toString()));
    }

    public static String getFactString(CodeEntity ce) {
        return ce.toString() + ".";
    }

    public static String getFactString(String predStr) {
        return predStr + ".";
    }

    public static String getClassFactString(String classId, String className) {
        return String.join("", "class(", classId, ", ", getQuotedString(className), ").");
    }

    public static void exportClassFacts(Facts facts, Configuration config) throws IOException {
        StringBuffer sb = new StringBuffer();
        sb.append(DISABLE_DISCONTIGUOUS);
        facts.getClassIds().forEach(classId -> {
            sb.append("\n");
            sb.append("%");
            sb.append(classId);
            sb.append(" - ");
            sb.append(facts.getClassName(classId));
            sb.append("\n");
            sb.append(getFactStrings(facts.getPredicates(classId)));
        });
        sb.append("\n\n");

        sb.append("% Stack Trace Info.");
        int fIndex = 1;
        int tIndex = 1;
        for(NPETrace trace : facts.getTraces()) {
            String failureId = TestFailure.PREFIX+fIndex++;
            TestFailure failure = new TestFailure(failureId, trace.testClass, trace.testMethod);
            if(trace.traces.size() > 0) {
                //Set the first trace's parent ID as failureId.
                StackTrace st = trace.traces.get(0);
                String traceId = Trace.PREFIX + tIndex++;
                failure.addTrace(getTrace(facts, traceId, failureId, failureId, st));
                String parentId = traceId;

                for(int i=1; i<trace.traces.size(); i++) {
                    st = trace.traces.get(i);
                    traceId = Trace.PREFIX + tIndex++;
                    failure.addTrace(getTrace(facts, traceId, parentId, failureId, st));
                    parentId = traceId;
                }
            }
            exportNPETrace(failure, sb);
        }
        sb.append("\n\n");

        exportFactsToFile(config.flFactsPath, sb.toString(), false, MARKER_STATIC_START, MARKER_STATIC_END);
        System.out.println("FL Facts are exported to " + config.flFactsPath);

        StringBuffer sbCode = new StringBuffer();
        exportCodeFacts(facts, sbCode);

        exportFactsToFile(config.codeFactsPath, sbCode.toString(), false, MARKER_CODE_START, MARKER_CODE_END);
        System.out.println("Code Facts are exported to " + config.codeFactsPath);
    }

    private static Trace getTrace(Facts facts, String traceId, String parentId, String failureId, StackTrace st) {
        String classId = facts.getClassId(CodeUtils.getIncludingClass(st.className));
        classId = classId == null ? CodeUtils.camelToLower(st.className) : classId;
        String methodId = facts.getMethodId(classId, st.lineNum);
        if(methodId == null) {
            if("<init>".equals(st.methodName)) {
                //For constructor, use the className instead.
                String simpleClassName = CodeUtils.qualifiedToSimple(CodeUtils.getIncludingClass(st.className));
                methodId = getQuotedString(simpleClassName);
            } else {
                methodId = getQuotedString(st.methodName);
            }
        }
        return new Trace(traceId, parentId, methodId, new Line(classId, st.lineNum), failureId, st.isTarget);
    }

    private static void exportNPETrace(TestFailure failure, StringBuffer sb) {
        sb.append("\n");
        sb.append(failure.toFactString());
        failure.getTraces().stream().forEach(t -> {
            sb.append("\n");
            sb.append(t.toFactString());
        });
    }

    private static void exportCodeFacts(Facts facts, StringBuffer sb) {
        sb.append(DISABLE_DISCONTIGUOUS);
        sb.append(MARKER_CLASSES);
        facts.getClassIdNameMap().forEach((classId, className) -> {
            sb.append("\n");
            sb.append(getClassFactString(classId, className));
        });

        sb.append("\n\n");
        sb.append(MARKER_METHODS);
        facts.getClassIds().forEach(classId -> {
            sb.append("\n");
            sb.append("%");
            sb.append(classId);
            sb.append(" - ");
            sb.append(facts.getClassName(classId));
            facts.getMethods(classId).values().stream()
                .sorted()
                .forEach(m -> {
                    sb.append("\n");
                    sb.append(FactManager.getFactString(m));
                });
        });

        sb.append("\n\n");
        sb.append(MARKER_BLOCKS);
        facts.getClassIds().forEach(classId -> {
            sb.append("\n");
            sb.append("%");
            sb.append(classId);
            sb.append(" - ");
            sb.append(facts.getClassName(classId));
            facts.getBlockMap(classId).values().stream()
                .sorted()
                .forEach(block -> {
                    sb.append("\n");
                    sb.append(FactManager.getFactString(block));
                });
        });

        sb.append("\n\n");
        sb.append(MARKER_STATEMENTS);
        facts.getClassIds().forEach(classId -> {
            sb.append("\n");
            sb.append("%");
            sb.append(classId);
            sb.append(" - ");
            sb.append(facts.getClassName(classId));
            facts.getStmtMap(classId).values().stream()
                .sorted()
                .forEach(stmt -> {
                    sb.append("\n");
                    sb.append(FactManager.getFactString(stmt));
                });
        });

        sb.append("\n\n");
        sb.append(MARKER_EXPRESSIONS);
        facts.getClassIds().forEach(classId -> {
            sb.append("\n");
            sb.append("%");
            sb.append(classId);
            sb.append(" - ");
            sb.append(facts.getClassName(classId));
            facts.getExprFacts(classId).entrySet().stream()
                .sorted(Comparator.comparing(Entry::getKey))
                .forEach(e -> {
                    sb.append("\n");
                    sb.append(FactManager.getFactString(e.getKey().toString(e.getValue())));
                });
        });

        sb.append("\n\n");
        sb.append(MARKER_NAMES);
        facts.getClassIds().forEach(classId -> {
            sb.append("\n");
            sb.append("%");
            sb.append(classId);
            sb.append(" - ");
            sb.append(facts.getClassName(classId));
            facts.getNameMap(classId).values().stream()
                .sorted()
                .forEach(name -> {
                    sb.append("\n");
                    sb.append(FactManager.getFactString(name));
                });
        });

        sb.append("\n\n");
        sb.append(MARKER_LITERALS);
        facts.getClassIds().forEach(classId -> {
            sb.append("\n");
            sb.append("%");
            sb.append(classId);
            sb.append(" - ");
            sb.append(facts.getClassName(classId));
            facts.getLiteralMap(classId).values().stream()
                .sorted()
                .forEach(literal -> {
                    sb.append("\n");
                    sb.append(FactManager.getFactString(literal));
                });
        });

        sb.append("\n\n");
        sb.append(MARKER_CODE_ENTITIES);
        facts.getClassIds().forEach(classId -> {
            sb.append("\n");
            sb.append("%");
            sb.append(classId);
            sb.append(" - ");
            sb.append(facts.getClassName(classId));
            facts.getCodeMap(classId).values().stream()
                .sorted()
                .forEach(ce -> {
                    sb.append("\n");
                    sb.append(FactManager.getFactString(ce));
                });
        });

        sb.append("\n\n");
        sb.append(MARKER_NAME_REFS);
        sb.append("\n");
        facts.getNameRefMap().forEach((k, map) -> {
            map.values().stream()
                .sorted(Comparator.comparing(NameRef::getKind).thenComparing(NameRef::getId))
                .forEach(nameRef -> {
                    sb.append("\n");
                    sb.append(FactManager.getFactString(nameRef.toString()));
                });
        });
    }

    public static String exportDynamicFacts(Path path, List<? extends Predicate> predicates) throws IOException {
        String facts = "\n" + FactManager.getFactStrings(predicates) + "\n";
        exportFactsToFile(path, facts, true, MARKER_DYNAMIC_START, MARKER_DYNAMIC_END);
        return facts;
    }

    private static void exportFactsToFile(Path path, String facts, boolean append, String startMarker, String endMarker) throws IOException {
        if(path.toFile().exists()) {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            int startIndex = content.indexOf(startMarker);
            int endIndex = content.indexOf(endMarker, startIndex);
            if(startIndex >= 0 && endIndex > startIndex) {
                content = content.substring(0, startIndex + startMarker.length()) + facts + "\n\n" + content.substring(endIndex);
                Files.write(path, content.getBytes(StandardCharsets.UTF_8));
                return;
            }
        }
        //If file doesn't exist, or failed to locate markers, overwrite or re-write all contents based on append.
        StandardOpenOption openOption = append ? StandardOpenOption.APPEND : StandardOpenOption.CREATE;
        String content = startMarker + facts + "\n\n" + endMarker;
        Files.write(path, content.getBytes(StandardCharsets.UTF_8), openOption);
    }

    public static void parseClassPredicate(String s, Map<String, String> map) {
        Matcher m = pClassPredicate.matcher(s);
        if(m.matches()) {
            map.put(m.group(2), m.group(1));
        }
    }
}
