package kr.ac.seoultech.selab.logicfl.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.WildcardType;
import org.junit.jupiter.api.Test;

import logicfl.probe.TreePath;
import logicfl.utils.CodeUtils;

public class CodeUtilsTest {
    @Test
    void testCamelToLower() {
        assertEquals("code_utils_test", CodeUtils.camelToLower("CodeUtilsTest"));
        assertEquals("node_visitor", CodeUtils.camelToLower("NodeVisitor"));
        assertEquals("kr_ac_seoultech_selab_static_analyzer", CodeUtils.camelToLower("kr.ac.seoultech.selab.StaticAnalyzer"));
        assertEquals("example", CodeUtils.camelToLower("Example"));
        assertEquals("person", CodeUtils.camelToLower("person"));
        assertEquals("base64encoder", CodeUtils.camelToLower("BASE64Encoder"));
        assertEquals("index_of", CodeUtils.camelToLower("indexOf"));
        assertEquals("sample1_1", CodeUtils.camelToLower("Sample1_1"));
    }

    @Test
    void testGetTypeName() {
        assertEquals("Assignment", CodeUtils.getTypeName(ASTNode.ASSIGNMENT));
        assertEquals("MethodInvocation", CodeUtils.getTypeName(ASTNode.METHOD_INVOCATION));
    }

    @Test
    void testQualifiedToPath() {
        String[] arr = {"kr", "ac", "seoultech", "selab", "logicfl", "analyzer", "StaticAnalyzer"};
        String qualifiedName = String.join(".", arr);
        String filePath = String.join(File.separator, arr);
        String extension = ".java";
        String filePathWithExtension = String.join(File.separator, arr) + extension;
        assertEquals(filePath, CodeUtils.qualifiedToPath(qualifiedName));
        assertEquals(filePathWithExtension, CodeUtils.qualifiedToPath(qualifiedName, extension));
    }

    @Test
    void testPathToQualified() {
        String[] arr = {"kr", "ac", "seoultech", "selab", "logicfl", "analyzer", "StaticAnalyzer"};
        String filePath = String.join(File.separator, arr);
        String qualifiedName = String.join(".", arr);

        assertEquals(qualifiedName, CodeUtils.pathToQualified(filePath, false));
        assertEquals(qualifiedName, CodeUtils.pathToQualified(filePath + ".class", true));
        assertEquals("StaticAnalyzer", CodeUtils.pathToQualified("StaticAnalyzer", true));
    }

    @Test
    void testGetTypeNode() {
        String source = "import java.util.List;\n"
        +" public class Sample<T> {\n"
        +"  public void method(List<? extends T> list, Class<?> clazz) {\n"
        +"      List<?> newList = null;\n"
        +"      clazz = clazz.getSuperclass();\n"
        +"  };\n"
        +" }";
        CompilationUnit cu = CodeUtils.getCompilationUnit("Sample.java", new String[]{}, new String[]{}, source);
        Map<String, List<ITypeBinding>> tbMap = new HashMap<>();
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(ParameterizedType node) {
                ITypeBinding binding = node.resolveBinding();
                tbMap.putIfAbsent("ParameterizedType", new ArrayList<>());
                tbMap.get("ParameterizedType").add(binding);
                return super.visit(node);
            }

            @Override
            public boolean visit(WildcardType node) {
                ITypeBinding binding = node.resolveBinding();
                tbMap.putIfAbsent("WildcardType", new ArrayList<>());
                tbMap.get("WildcardType").add(binding);
                return super.visit(node);
            }

            @Override
            public boolean visit(SimpleName node) {
                if(node.resolveBinding() instanceof IVariableBinding) {
                    tbMap.putIfAbsent("Var", new ArrayList<>());
                    tbMap.get("Var").add(node.resolveTypeBinding());
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(MethodInvocation node) {
                tbMap.putIfAbsent("Method", new ArrayList<>());
                tbMap.get("Method").add(node.resolveTypeBinding());
                return super.visit(node);
            }
        });
        Type t = CodeUtils.getTypeNode(cu.getAST(), tbMap.get("ParameterizedType").get(0));
        assertEquals("List<? extends T>", t.toString());

        t = CodeUtils.getTypeNode(cu.getAST(), tbMap.get("WildcardType").get(0));
        assertEquals("? extends T", t.toString());

        t = CodeUtils.getTypeNode(cu.getAST(), tbMap.get("Var").get(0));
        assertEquals("List<? extends T>", t.toString());

        t = CodeUtils.getTypeNode(cu.getAST(), tbMap.get("ParameterizedType").get(1));
        assertEquals("Class<?>", t.toString());

        t = CodeUtils.getTypeNode(cu.getAST(), tbMap.get("WildcardType").get(1));
        assertEquals("?", t.toString());

        t = CodeUtils.getTypeNode(cu.getAST(), tbMap.get("Var").get(1));
        assertEquals("Class<?>", t.toString());

        t = CodeUtils.getTypeNode(cu.getAST(), tbMap.get("Method").get(0));
        assertEquals("Class<?>", t.toString());
    }

    @Test
    void testStartEndLines() {
        String source = "public class Sample { \n"
            + "  public void method(List<String> list) {\n"
            + "     List<String> newList = null;\n"
            + "     newList = list != null ? \n"
            + "         list : new ArrayList<>();\n"
            + " };\n"
            + "}";
        CompilationUnit cu = CodeUtils.getCompilationUnit("Sample.java", new String[]{}, new String[]{}, source);
        List<ASTNode> nodes = new ArrayList<>();
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                nodes.add(node);
                return super.visit(node);
            }

            @Override
            public boolean visit(Assignment node) {
                nodes.add(node);
                return super.visit(node);
            }
        });
        assertEquals(2, nodes.size());
        assertEquals(2, CodeUtils.getStartLine(nodes.get(0)));
        assertEquals(4, CodeUtils.getStartLine(nodes.get(1)));
        assertEquals(6, CodeUtils.getEndLine(nodes.get(0)));
        assertEquals(5, CodeUtils.getEndLine(nodes.get(1)));
    }

    @Test
    void testGetNumberOfLines() {
        String source = "public class Sample { \n"
            + "  public void method(List<String> list) {\n"
            + "     List<String> newList = null;\n"
            + "     newList = list != null ? \n"
            + "         list : new ArrayList<>();\n"
            + " };\n"
            + "}";
        CompilationUnit cu = CodeUtils.getCompilationUnit("Sample.java", new String[]{}, new String[]{}, source);
        List<ASTNode> nodes = new ArrayList<>();
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(ConditionalExpression node) {
                nodes.add(node);
                return super.visit(node);
            }

            @Override
            public boolean visit(MethodDeclaration node) {
                nodes.add(node);
                return super.visit(node);
            }
        });
        assertEquals(2, nodes.size());
        assertEquals(5, CodeUtils.getNumberOfLines(nodes.get(0)));
        assertEquals(2, CodeUtils.getNumberOfLines(nodes.get(1)));
    }

    @Test
    void testFindTreePath() {
        String source = "public class Sample { \n"
            + "  public void method(int num) {\n"
            + "     int sum = 0;\n"
            + "     for(int i=0; i<num; i++){\n"
            + "         if(i % 2 == 0) {\n"
            + "             sum = sum + i;\n"
            + "         }\n"
            + "     }\n"
            + " };\n"
            + "}";
        CompilationUnit cu = CodeUtils.getCompilationUnit("Sample.java", new String[]{}, new String[]{}, source);
        Map<String, ASTNode> map = new HashMap<>();
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(Assignment node) {
                map.put("assign", node);
                return super.visit(node);
            }

            @Override
            public boolean visit(IfStatement node) {
                map.put("if", node);
                return super.visit(node);
            }

            @Override
            public boolean visit(Block node) {
                if(node.getParent() instanceof ForStatement)
                    map.put("for_block", node);
                return super.visit(node);
            }

            @Override
            public boolean visit(ForStatement node) {
                map.put("for", node);
                return super.visit(node);
            }
        });
        TreePath path = CodeUtils.findTreePath(map.get("assign"), map.get("for"));
        assertEquals(map.get("for_block"), path.getNode(map.get("for")));
        path = path.getChild();
        assertEquals(map.get("if"), path.getNode(map.get("for_block")));
        path = path.getBottom();
        assertEquals(ExpressionStatement.EXPRESSION_PROPERTY, path.getLocation());
    }

    @Test
    void testIsJavaLangPackage() {
        assertTrue(!CodeUtils.isJavaLangPackage("java.lang.annotation.Annotation"));
        assertTrue(!CodeUtils.isJavaLangPackage("java.lang.reflect.Field"));
        assertTrue(CodeUtils.isJavaLangPackage("java.lang.Object"));
    }

    @Test
    void testRemoveTypeParameters() {
        assertEquals("AAClass", CodeUtils.removeTypeParameters("AAClass"));
        assertEquals("AAClass", CodeUtils.removeTypeParameters("AAClass<String>"));
        assertEquals("AAClass.BBClass", CodeUtils.removeTypeParameters("AAClass<String>.BBClass<String>"));
        assertEquals("AInterface", CodeUtils.removeTypeParameters("AInterface<? super T>"));
        assertEquals("AInterface", CodeUtils.removeTypeParameters("AInterface<AInterface<? super T>>"));
        assertEquals("org.apache.commons.lang3.reflect.testbed.GenericParent", CodeUtils.removeTypeParameters("org.apache.commons.lang3.reflect.testbed.GenericParent<java.lang.String>"));
        assertEquals("Transformer", CodeUtils.removeTypeParameters("Transformer<Entry<E>, E>"));
    }
}
