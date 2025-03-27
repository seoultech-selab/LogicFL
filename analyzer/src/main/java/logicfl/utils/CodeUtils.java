package logicfl.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IntersectionType;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.WildcardType;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import logicfl.logic.codefacts.Range;
import logicfl.probe.TreePath;

public class CodeUtils {

    public static final int JAVA_LEVEL = AST.getJLSLatest();
    public static final String JAVA_VERSION = JavaCore.latestSupportedJavaVersion();
    public static final String NAME_KEY_DELIM = "#";
    private static Pattern pCamelCase = Pattern.compile("([a-z_]*)([_A-Z0-9]+[_a-z0-9]*)");
    private static Pattern pJavaLangSubPkgClasses = Pattern.compile("java\\.lang\\.(.+)\\.(.*)");

    public static CompilationUnit getCompilationUnit(String unitName, String[] classPath, String[] sourcePath, String source) {
        ASTParser parser = ASTParser.newParser(JAVA_LEVEL);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JAVA_VERSION, options);
        parser.setCompilerOptions(options);
        parser.setEnvironment(classPath, sourcePath, null, true);
        parser.setUnitName(unitName);
        parser.setResolveBindings(true);
        parser.setSource(source.toCharArray());
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        return cu;
    }

    public static CompilationUnit getCompilationUnit(String source) {
        ASTParser parser = ASTParser.newParser(JAVA_LEVEL);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JAVA_VERSION, options);
        parser.setCompilerOptions(options);
        parser.setSource(source.toCharArray());
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        return cu;
    }

    public static String getSource(String className, String srcPath) throws IOException {
        return getSource(className, new String[]{ srcPath });
    }

    public static String getSource(String className, String[] srcPaths) throws IOException {
        String filePath = CodeUtils.qualifiedToPath(className, ".java");
        String source = null;
        //Use a file found from a directory in the order of the source path configuration.
        for(String sourcePath : srcPaths) {
            Path srcFile = Paths.get(sourcePath, filePath);
            if(srcFile.toFile().exists())
                source = Files.readString(srcFile);
        }
        return source;
    }

    public static String getMethodCode(String className, String methodName, String[] srcPaths) {
        return getMethodCode(className, methodName, -1, srcPaths);
    }

    public static String getMethodCode(String className, String methodName, int lineNum, String[] srcPaths) {
        Map<String, String> methodCode = new HashMap<>();
        try {
            String source = CodeUtils.getSource(getIncludingClass(className), srcPaths);
            if(source == null) {
                throw new IOException("Cannot read source code of " + className + " from " + Arrays.toString(srcPaths));
            }
            CompilationUnit cu = CodeUtils.getCompilationUnit(source);            
            cu.accept(new ASTVisitor() {
                @Override
                public boolean visit(MethodDeclaration node) {
                    if(node.getName().getIdentifier().equals(methodName)) {
                        if(lineNum < 0 ||
                            lineNum >= getStartLine(node, cu) && lineNum <= getEndLine(node, cu)) {
                            methodCode.put(methodName, node.toString());
                        }                        
                    }
                    return false;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return methodCode.get(methodName);
    }

    public static String getMethodSignature(IMethodBinding mb) {
        mb = mb.getMethodDeclaration();
        if(mb != null) {
            StringBuffer sb = new StringBuffer();
            sb.append(mb.getName());
            sb.append("(");
            for(ITypeBinding param : mb.getParameterTypes()) {
                sb.append(getStrippedClass(param.getQualifiedName()));
                sb.append(",");
            }
            sb.append(")");
            return sb.toString();
        }
        return null;
    }

    public static String getMethodKey(IMethodBinding mb) {
        mb = mb.getMethodDeclaration();
        if(mb != null) {
            StringBuffer sb = new StringBuffer();
            ITypeBinding declType = mb.getDeclaringClass();
            sb.append(declType != null ? getStrippedClass(declType.getQualifiedName()) : "unknown");
            sb.append(";");
            sb.append(getMethodSignature(mb));
            return sb.toString();
        }
        return null;
    }

    public static String getVariableKey(IVariableBinding vb) {
        //Consider fields only at the momment.
        if(vb != null && vb.isField()) {
            StringBuffer sb = new StringBuffer();
            ITypeBinding declType = vb.getDeclaringClass();
            sb.append(declType != null ? getStrippedClass(declType.getQualifiedName()) : "unknown");
            sb.append(";");
            sb.append(vb.getName());
            return sb.toString();
        }
        return null;
    }

    public static String getTypeName(int type) {
        return type == -1 ? "root" : ASTNode.nodeClassForType(type).getSimpleName();
    }

    public static String camelToLower(String className) {
        List<String> words = new ArrayList<>();
        int index = className.lastIndexOf('.');
        if(index >= 0) {
            String[] packages = className.substring(0, index).split("\\.");
            words.addAll(Arrays.asList(packages));
            className = className.substring(index+1);
        }
        Matcher m = pCamelCase.matcher(className);
        while(m.find()){
            if(m.group(1).length() > 0) {
                words.add(m.group(1));
            }
            words.add(m.group(2).toLowerCase());
        }
        return words.size() > 0 ? String.join("_", words) : className;
    }

    public static String qualifiedToSimple(String qualifiedName) {
        int index = qualifiedName.lastIndexOf('.');
        return index > 0 && index < qualifiedName.length()-1 ?
            qualifiedName.substring(index+1) : qualifiedName;
    }

    public static String getPackageFromQualified(String qualifiedName) {
        int index = qualifiedName.lastIndexOf('.');
        return index > 0 && index < qualifiedName.length()-1 ?
            qualifiedName.substring(0, index) : null;
    }

    public static String qualifiedToPath(String qulifiedName) {
        return qulifiedName.replaceAll("\\.", File.separator);
    }

    public static String qualifiedToPath(String qulifiedName, String extension) {
        return qulifiedName.replaceAll("\\.", File.separator) + extension;
    }

    public static String pathToQualified(String filePath) {
        return pathToQualified(filePath, true);
    }

    public static String pathToQualified(String filePath, boolean hasExtension) {
        int index = filePath.lastIndexOf('.');
        String qualified = hasExtension && index > 0 ? filePath.substring(0, index) : filePath;
        return qualified.replaceAll("\\/", "\\.");
    }

    public static String createClassId(String simpleLower, int index) {
        return simpleLower + "_" + index;
    }

    public static String getIncludingClass(String className) {
        int index = className.indexOf('$');
        return index >= 0 ? className.substring(0, index) : className;
    }

    public static Statement getEnclosingStatement(ASTNode node) {
        ASTNode parent = node.getParent();
        while(parent != null && !(parent instanceof Statement)) {
            parent = parent.getParent();
        }
        return (Statement)parent;
    }

    public static ListRewrite getListRewrite(ASTRewrite rewrite, ASTNode loc) {
        ASTNode parent = loc.getParent();
        if(loc.getLocationInParent() instanceof ChildListPropertyDescriptor desc)
            return rewrite.getListRewrite(parent, desc);
        return null;
    }

    public static Type getTypeNode(AST ast, ITypeBinding binding) {
        return getTypeNode(ast, binding, null, null, null);
    }

    @SuppressWarnings("unchecked")
    public static Type getTypeNode(AST ast, ITypeBinding binding, String packageName, Set<String> importedClasses, Set<String> missingClasses) {
        if (binding == null) {
            return ast.newSimpleType(ast.newName("java.lang.Object"));
        }

        if (binding.isPrimitive()) {
            return ast.newPrimitiveType(PrimitiveType.toCode(binding.getName()));
        }

        if (binding.isArray()) {
            Type elementType = getTypeNode(ast, binding.getElementType(), packageName, importedClasses, missingClasses);
            return ast.newArrayType(elementType, binding.getDimensions());
        }

        if (binding.isWildcardType()) {
            WildcardType wildcardType = ast.newWildcardType();
            ITypeBinding bound = binding.getBound();
            if (bound != null) {
                Type boundType = getTypeNode(ast, bound, packageName, importedClasses, missingClasses);
                if(!(boundType instanceof WildcardType wt) || wt.getBound() != null) {
                    wildcardType.setBound(boundType, binding.isUpperbound());
                }
            }
            return wildcardType;
        }

        if (binding.isCapture()) {
            return getTypeNode(ast, binding.getWildcard(), packageName, importedClasses, missingClasses);
        }

        if (binding.isIntersectionType()) {
            IntersectionType intersectionType = ast.newIntersectionType();
            for (ITypeBinding type : binding.getTypeBounds()) {
                intersectionType.types().add(getTypeNode(ast, type, packageName, importedClasses, missingClasses));
            }
            return intersectionType;
        }

        if (binding.isParameterizedType() || binding.isGenericType()) {
            ITypeBinding erasureBinding = binding.getErasure();
            if(!erasureBinding.isTypeVariable())
                checkMissingImport(erasureBinding.getQualifiedName(), packageName, importedClasses, missingClasses);
            String erasureTypeName = erasureBinding.isNested() ? erasureBinding.getQualifiedName() : erasureBinding.getName();
            Type erasure = ast.newSimpleType(ast.newName(erasureTypeName));
            ParameterizedType parameterizedType = ast.newParameterizedType(erasure);
            ITypeBinding[] arguments = binding.isGenericType() ? binding.getTypeParameters() : binding.getTypeArguments();
            for (ITypeBinding typeArg : arguments) {
                Type arg = getTypeNode(ast, typeArg, packageName, importedClasses, missingClasses);
                if(hasCapturedParameter(typeArg)) {
                    WildcardType wildcardType = ast.newWildcardType();
                    wildcardType.setBound(arg);
                    arg = wildcardType;
                }
                parameterizedType.typeArguments().add(arg);
            }
            return parameterizedType;
        }

        // Qualified Types and Simple Types
        String qualifiedName = binding.getQualifiedName();
        boolean isSkipped = false;
        if(!binding.isTypeVariable())
            isSkipped = checkMissingImport(qualifiedName, packageName, importedClasses, missingClasses);
        if (isSkipped || qualifiedName.contains(".") && !isJavaLangPackage(qualifiedName)) {
            return ast.newSimpleType(ast.newName(qualifiedName));
        } else {
            return ast.newSimpleType(ast.newSimpleName(binding.getName()));
        }
    }

    private static boolean hasCapturedParameter(ITypeBinding tb) {
        if(tb.isParameterizedType()) {
            for(ITypeBinding typeArg : tb.getTypeArguments()) {
                if(typeArg.isCapture() || hasCapturedParameter(typeArg))
                    return true;
            }
        }
        return false;
    }

    private static boolean checkMissingImport(String qualifiedName, String packageName, Set<String> importedClasses,
            Set<String> missingClasses) {
        if(importedClasses != null && missingClasses != null && qualifiedName != null
            && packageName != null && !qualifiedName.startsWith(packageName)
            && !isJavaLangPackage(qualifiedName)
            && !importedClasses.contains(qualifiedName)) {
            String className = qualifiedToSimple(qualifiedName);
            for(String importedClass : importedClasses) {
                //Ignore identical class name.
                if(importedClass.endsWith("."+className))
                    return true;
            }
            missingClasses.add(qualifiedName);
            importedClasses.add(qualifiedName);
        }
        return false;
    }

    public static boolean isJavaLangPackage(String qualifiedName) {
        if(qualifiedName.startsWith("java.lang")) {
            Matcher m = pJavaLangSubPkgClasses.matcher(qualifiedName);
            return !m.matches();
        }
        return false;
    }

    public static boolean compileJavaFiles(Path sourceDirectory, Path outputDirectory, String classPath) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        boolean success = false;

        try {
            // Create a list of all .java files in the source directory and its sub-directories
            List<File> javaFiles = getJavaFiles(sourceDirectory.toFile());

            // Prepare the compilation task
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(javaFiles);
            Iterable<String> options = Arrays.asList("-cp", classPath, "-g", "-d", outputDirectory.toString());
            CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits);

            // Compile the source code
            success = task.call();

            if (success) {
                System.out.println("Compilation successful!");
            } else {
                System.out.println("Compilation failed!");
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    if(diagnostic.getSource() != null) {
                        System.err.println(diagnostic.getSource().getName());
                        System.err.println(diagnostic.getMessage(null));
                        System.err.println("Line:"+diagnostic.getLineNumber());
                    }
                }
            }

            fileManager.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return success;
    }

    public static List<File> getJavaFiles(File directory) {
        List<File> javaFiles = new ArrayList<>();
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    javaFiles.addAll(getJavaFiles(file));
                } else if (file.getName().endsWith(".java")) {
                    javaFiles.add(file);
                }
            }
        }

        return javaFiles;
    }

    public static int getStartLine(ASTNode node) {
        if(node != null && node.getRoot() instanceof CompilationUnit cu) {
            return getStartLine(node, cu);
        } else {
            throw new IllegalArgumentException("The given node doesn't have CompilationUnit root.");
        }
    }

    public static int getEndLine(ASTNode node) {
        if(node != null && node.getRoot() instanceof CompilationUnit cu) {
            return getEndLine(node, cu);
        } else {
            throw new IllegalArgumentException("The given node doesn't have CompilationUnit root.");
        }
    }

    public static int getStartLine(ASTNode node, CompilationUnit cu) {
        return cu.getLineNumber(node.getStartPosition());
    }

    public static int getEndLine(ASTNode node, CompilationUnit cu) {
        return cu.getLineNumber(node.getStartPosition() + node.getLength());
    }

    public static Range getRange(String classId, ASTNode node, CompilationUnit cu) {
        return new Range(classId, node.getStartPosition(), node.getLength(),
            getStartLine(node, cu), getEndLine(node, cu));
    }

    public static int getNumberOfLines(ASTNode node) {
        if(node != null) {
            if(node.getRoot() instanceof CompilationUnit cu) {
                int startLine = getStartLine(node, cu);
                int endLine = getEndLine(node, cu);
                return endLine - startLine + 1;
            } else {
                return node.toString().split("\\r?\\n|\\r").length;
            }
        }
        return 0;
    }

    public static String getNodeType(int nodeType) {
        try {
            return ASTNode.nodeClassForType(nodeType).getSimpleName();
        } catch (Exception e) {
            return "invalid";
        }
    }

    /**
     * Find a {@code TreePath} from {@code fromNode} to{@code toNode}.
     * The path is found by climbing up to the {@code toNode},
     * and we can trace back to {@code fromNode} using the returned {@code TreePath}.
     *
     * @param fromNode the node where the path tracking starts.
     * @param toNode the node where the path tracking ends.
     * @return a {@code TreePath} instance corresponds to the path in {@code toNode}.
     * If it is the same as {@code fromNode}, an empty {@code TreePath} instance will be returned.
     */
    public static TreePath findTreePath(ASTNode fromNode, ASTNode toNode) {
        if(fromNode != null && fromNode.equals(toNode))
            return new TreePath();
        TreePath curr = new TreePath(fromNode);
        ASTNode parent = fromNode.getParent();
        while(parent != null && fromNode != null) {
            if(parent.equals(toNode)) {
                break;
            }
            curr.setParent(new TreePath(parent));
            curr = curr.getParent();
            fromNode = parent;
            parent = fromNode.getParent();
        }
        return curr;
    }

    public static boolean isIdentical(ListRewrite listRewrite1, ListRewrite listRewrite2) {
        if(listRewrite1 == null && listRewrite2 == null) {
            return true;
        }
        if(listRewrite1 != null && listRewrite2 != null) {
            return listRewrite1.getParent().equals(listRewrite2.getParent())
                && listRewrite2.getLocationInParent().equals(listRewrite2.getLocationInParent());
        }
        return false;
    }

    public static String getStrippedClass(String className) {
        className = removeTypeParameters(className);
        return getIncludingClass(className);
    }

    public static String removeTypeParameters(String className) {
        StringBuffer sb = new StringBuffer();
        int openCount = 0;
        char c = ' ';
        for(int i=0; i < className.length(); i++) {
            c = className.charAt(i);
            if(c == '<') {
                openCount++;
            } else if(c == '>') {
                openCount--;
                continue;
            }
            if(openCount == 0)
                sb.append(className.charAt(i));
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static void replaceNodeInAST(ASTNode node, ASTNode replacement) {
        ASTNode parent = node.getParent();
        StructuralPropertyDescriptor desc = node.getLocationInParent();
        if(parent != null && desc != null) {
            if (desc.isChildProperty()) {
                parent.setStructuralProperty(desc, replacement);
            } else if (desc.isChildListProperty()) {
                List<ASTNode> list = (List<ASTNode>) parent.getStructuralProperty(desc);
                int index = list.indexOf(node);
                if (index != -1) {
                    list.set(index, replacement);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void replaceNodeWithRewrite(ASTNode node, ASTNode replacement, ASTRewrite rewrite) {
        ASTNode parent = node.getParent();
        StructuralPropertyDescriptor desc = node.getLocationInParent();
        if(parent != null && desc != null) {
            if (desc.isChildProperty()) {
                rewrite.set(parent, desc, replacement, null);
            } else if (desc.isChildListProperty()) {
                List<ASTNode> list = (List<ASTNode>) parent.getStructuralProperty(desc);
                int index = list.indexOf(node);
                if (index != -1) {
                    rewrite.replace(list.get(index), replacement, null);
                }
            }
        }
    }

    public static void replaceNode(ASTNode node, ASTNode replacement, ASTRewrite rewrite) {
        replaceNodeInAST(node, replacement);
        replaceNodeWithRewrite(node, replacement, rewrite);
    }

    public static boolean isLiteral(ASTNode node) {
        return node instanceof NullLiteral ||
            node instanceof NumberLiteral ||
            node instanceof StringLiteral ||
            node instanceof BooleanLiteral ||
            node instanceof CharacterLiteral;
    }
}
