package kr.ac.seoultech.selab.logicfl.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import logicfl.logic.Predicate;
import logicfl.logic.Ref;
import logicfl.logic.Return;
import logicfl.logic.codefacts.Line;
import logicfl.logic.codefacts.NameRef;
import logicfl.probe.NodeVisitor;
import logicfl.utils.CodeUtils;

class NodeVisitorTest {

    private static CompilationUnit cuExample;
    private static CompilationUnit cuPerson;
    private static NodeVisitor vstExample;
    private static NodeVisitor vstPerson;
    private static final String C_PERSON = "person";
    private static final String C_EXAMPLE = "example";

    @BeforeAll
    static void beforeAll() {
        try {
            Path baseDir = Paths.get("src/test/java");
            String source = Files.readString(Paths.get(baseDir.toString(), "sample", "Example.java"));
            cuExample = CodeUtils.getCompilationUnit("Example.java", new String[]{}, new String[]{baseDir.toString()}, source);
            source = Files.readString(Paths.get(baseDir.toString(), "sample", "Person.java"));
            cuPerson = CodeUtils.getCompilationUnit("Person.java", new String[]{}, new String[]{baseDir.toString()}, source);
            vstExample = new NodeVisitor("sample.Example", C_EXAMPLE, cuExample);
            cuExample.accept(vstExample);
            vstPerson = new NodeVisitor("sample.Person", C_PERSON, cuPerson);
            cuPerson.accept(vstPerson);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testVisitFieldAccess() {
        List<Predicate> predicates = vstPerson.getPredicates().stream().filter(p -> p instanceof Ref).toList();
        assertEquals(0, predicates.size());
        predicates = vstExample.getPredicates().stream().filter(p -> p instanceof Ref).toList();
        assertTrue(predicates.size() > 0);
    }

    @Test
    void testVisitReturnStatement() {
        List<Predicate> predicates = vstExample.getPredicates().stream().filter(p -> p instanceof Return).toList();
        Map<String, NameRef> methodMap = vstExample.getNameRefMap().get(IBinding.METHOD);
        List<ReturnStatement> returns = new ArrayList<>();
        Map<ASTNode, String> methodIdMap = new HashMap<>();
        cuExample.accept(new ASTVisitor() {
            @Override
            public boolean visit(ReturnStatement node) {
                returns.add(node);
                ASTNode parent = node.getParent();
                while(parent != null) {
                    if(parent instanceof MethodDeclaration method) {
                        IMethodBinding binding = method.resolveBinding();
                        NameRef methodRef = methodMap.get(binding.getKey());
                        methodIdMap.put(node, methodRef.toString());
                    }
                    parent = parent.getParent();
                }

                return super.visit(node);
            }
        });
        assertEquals(returns.size(), predicates.size());
        ReturnStatement returnStmt = returns.get(0);
        Line line = new Line(C_EXAMPLE, cuExample.getLineNumber(returnStmt.getStartPosition()));
        assertEquals(line, ((Return)predicates.get(0)).getLine());
        returnStmt = returns.get(1);
        line = new Line(C_EXAMPLE, cuExample.getLineNumber(returnStmt.getStartPosition()));
        assertEquals(line, ((Return)predicates.get(1)).getLine());
    }
}