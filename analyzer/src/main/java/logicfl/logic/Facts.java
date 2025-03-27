package logicfl.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IBinding;

import logicfl.coverage.NPETrace;
import logicfl.logic.codefacts.CodeBlock;
import logicfl.logic.codefacts.CodeEntity;
import logicfl.logic.codefacts.CodeName;
import logicfl.logic.codefacts.Expr;
import logicfl.logic.codefacts.Literal;
import logicfl.logic.codefacts.MethodDecl;
import logicfl.logic.codefacts.NameRef;
import logicfl.logic.codefacts.Stmt;
import logicfl.utils.CodeUtils;

public class Facts {

    public static final String NPE_ATOM = "null_pointer_exception";
    public static final int NAME_REF_TYPE = -99;
    public static final int[] NAME_TYPES = {
        IBinding.PACKAGE,
        IBinding.TYPE,
        IBinding.VARIABLE,
        IBinding.METHOD,
        IBinding.ANNOTATION,
        IBinding.MEMBER_VALUE_PAIR,
        IBinding.MODULE,
        NameRef.K_QUALIFIED,
        NAME_REF_TYPE
    };

    private Map<String, String> classIdNameMap;
    private Map<String, TreeMap<Integer, MethodDecl>> methodMap;
    private Map<String, Map<ASTNode, CodeEntity>> codeMap;
    private Map<String, Map<ASTNode, CodeBlock>> blockMap;
    private Map<String, Map<ASTNode, Stmt>> stmtMap;
    private Map<String, Map<ASTNode, Expr>> exprMap;
    private Map<String, Map<ASTNode, CodeName>> nameMap;
    private Map<String, Map<ASTNode, Literal>> literalMap;
    private Map<Integer, Map<String, NameRef>> nameRefMap;
    private Map<String, List<Predicate>> predicates;
    private List<NPETrace> traces;

    public Facts() {
        classIdNameMap = new HashMap<>();
        methodMap = new HashMap<>();
        codeMap = new HashMap<>();
        blockMap = new HashMap<>();
        stmtMap = new HashMap<>();
        exprMap = new HashMap<>();
        nameMap = new HashMap<>();
        literalMap = new HashMap<>();
        nameRefMap = new HashMap<>();
        for(int type : NAME_TYPES)
            nameRefMap.put(type, new HashMap<>());
        predicates = new HashMap<>();
        traces = new ArrayList<>();
    }

    public Facts(String classId, String className) {
        this();
        addClassId(classId, className);
    }

    public Map<ASTNode, CodeEntity> getCodeMap(String classId) {
        return codeMap.get(classId);
    }

    public Map<ASTNode, CodeBlock> getBlockMap(String classId) {
        return blockMap.get(classId);
    }

    public Map<ASTNode, Stmt> getStmtMap(String classId) {
        return stmtMap.get(classId);
    }

    public Map<ASTNode, Expr> getExprMap(String classId) {
        return exprMap.get(classId);
    }

    public Map<ASTNode, CodeName> getNameMap(String classId) {
        return nameMap.get(classId);
    }

    public Map<ASTNode, Literal> getLiteralMap(String classId) {
        return literalMap.get(classId);
    }

    public Map<Expr, String> getExprFacts(String classId) {
        Map<Expr, String> map = new HashMap<>();
        if(exprMap.containsKey(classId))
            exprMap.get(classId)
                .entrySet()
                .forEach(e -> map.put(e.getValue(), e.getKey().toString()));
        return map;
    }

    public Map<Integer, Map<String, NameRef>> getNameRefMap() {
        return nameRefMap;
    }

    public List<Predicate> getPredicates(String classId) {
        return predicates.get(classId);
    }

    public void add(String classId, Predicate p) {
        predicates.get(classId).add(p);
    }

    public void addAll(String classId, Collection<Predicate> predicates) {
        this.predicates.get(classId).addAll(predicates);
    }

    public Set<String> getClassIds() {
        return classIdNameMap.keySet();
    }

    public Map<String, String> getClassIdNameMap() {
        return classIdNameMap;
    }

    public void addClassId(String classId, String className) {
        classIdNameMap.put(classId, className);
        predicates.putIfAbsent(classId, new ArrayList<>());
        codeMap.putIfAbsent(classId, new HashMap<>());
        blockMap.putIfAbsent(classId, new HashMap<>());
        stmtMap.putIfAbsent(classId, new HashMap<>());
        exprMap.putIfAbsent(classId, new HashMap<>());
        nameMap.putIfAbsent(classId, new HashMap<>());
        literalMap.putIfAbsent(classId, new HashMap<>());
        methodMap.putIfAbsent(classId, new TreeMap<>());
    }

    public String getClassName(String classId) {
        return classIdNameMap.getOrDefault(classId, "N/A");
    }

    public String getClassId(String className) {
        String simpleLower = CodeUtils.camelToLower(CodeUtils.qualifiedToSimple(className));
        int index = 1;
        String classId = CodeUtils.createClassId(simpleLower, index++);
        while(hasClassId(classId)) {
            if(className.equals(getClassName(classId))) {
                return classId;
            }
            classId = CodeUtils.createClassId(simpleLower, index++);
        }
        return null;
    }

    public boolean hasClassId(String classId) {
        return classIdNameMap.containsKey(classId);
    }

    public TreeMap<Integer, MethodDecl> getMethods(String classId) {
        return methodMap.get(classId);
    }

    public List<NPETrace> getTraces() {
        return this.traces;
    }

    public void addTraces(List<NPETrace> traces) {
        this.traces.addAll(traces);
    }

    public String getMethodId(String classId, int lineNum) {
        TreeMap<Integer, MethodDecl> methods = methodMap.get(classId);
        if(methods != null) {
            Integer methodStartLine = methods.floorKey(lineNum);
            if(methodStartLine == null)
                return null;
            MethodDecl method = methods.get(methodStartLine);
            return method.getId();
        }
        return null;
    }
}