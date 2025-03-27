package logicfl.probe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import logicfl.logic.codefacts.CodeEntity;
import logicfl.utils.CodeUtils;

public class MethodMatcher {

    private class MethodCallInfo {
        public String className;
        public String methodName;
        public int lineNum;

        public MethodCallInfo(String className, String methodName, int lineNum) {
            this.className = className;
            this.methodName = methodName;
            this.lineNum = lineNum;
        }

        public boolean isMatched(IMethodBinding binding, int lineNum) {
            ITypeBinding tb = binding.getDeclaringClass();
            String bindingClassName = tb != null ? CodeUtils.getStrippedClass(tb.getQualifiedName()) : "";
            String bindingMethodName = CodeUtils.removeTypeParameters(binding.getName());

            return this.lineNum == lineNum
                && CodeUtils.getStrippedClass(this.className).equals(bindingClassName)
                && this.methodName.equals(bindingMethodName);
        }
    }

    private Map<Integer, List<MethodCallInfo>> methodCalls;
    private Map<Integer, List<CodeEntity>> candidates;

    public MethodMatcher() {
        methodCalls = new HashMap<>();
        candidates = new HashMap<>();
    }

    public void addMethodCallInfo(String className, String methodName, int lineNum) {
        methodCalls.putIfAbsent(lineNum, new ArrayList<>());
        methodCalls.get(lineNum).add(new MethodCallInfo(className, methodName, lineNum));
    }

    public boolean hasMatch(IMethodBinding binding, int lineNum) {
        List<MethodCallInfo> list = methodCalls.get(lineNum);
        if(list != null) {
            for(MethodCallInfo info : list) {
                if(info.isMatched(binding, lineNum))
                    return true;
            }
        }
        return false;
    }

    public boolean isLineAppeared(int lineNum) {
        return methodCalls.containsKey(lineNum)
            && methodCalls.get(lineNum).size() > 0;
    }

    public boolean addCandidate(int lineNum, CodeEntity ce) {
        if(isLineAppeared(lineNum)) {
            candidates.putIfAbsent(lineNum, new ArrayList<>());
            return candidates.get(lineNum).add(ce);
        }
        return false;
    }

    public Map<Integer, List<CodeEntity>> getCandidates() {
        return candidates;
    }
}
