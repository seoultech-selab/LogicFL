package sample;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import logicfl.utils.CodeUtils;

public class Sample3 {

    private String str;
    private Integer num;
    private Object obj;

    public Sample3(String s, Integer n, Object o) {
        this.str = s;
        this.num = n;
        this.obj = o;
    }

    public Supplier<String> lambdaSample() {
        return () -> this.num.toString();
    }

    @Override
    public String toString() {
        String s = this.lambdaSample().get();
        return String.join("/", str, num.toString(), obj.toString(), s);
    }

    public List<String> getMethods(String source) {
        List<String> methods = new ArrayList<>();
        CompilationUnit cu = CodeUtils.getCompilationUnit(source);
        cu.accept(getVisitor(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                String methodName = node.getName().toString();
                methods.add(methodName);
                return super.visit(node);
            }
        }));
        return methods;
    }

    public ASTVisitor getVisitor(ASTVisitor visitor) {
        return visitor;
    }
}