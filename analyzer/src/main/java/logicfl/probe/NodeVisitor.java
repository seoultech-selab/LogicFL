package logicfl.probe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import logicfl.coverage.CoverageInfo;
import logicfl.logic.Argument;
import logicfl.logic.Assign;
import logicfl.logic.CondExpr;
import logicfl.logic.Facts;
import logicfl.logic.MethodInvoc;
import logicfl.logic.Param;
import logicfl.logic.Predicate;
import logicfl.logic.Ref;
import logicfl.logic.Return;
import logicfl.logic.Throw;
import logicfl.logic.codefacts.CodeBlock;
import logicfl.logic.codefacts.CodeEntity;
import logicfl.logic.codefacts.CodeName;
import logicfl.logic.codefacts.Expr;
import logicfl.logic.codefacts.Line;
import logicfl.logic.codefacts.Literal;
import logicfl.logic.codefacts.Loc;
import logicfl.logic.codefacts.MethodDecl;
import logicfl.logic.codefacts.NameRef;
import logicfl.logic.codefacts.Stmt;
import logicfl.utils.CodeUtils;

public class NodeVisitor extends ASTVisitor {
    public static final String PROBE_INDEX_SEP = "_v";
    public static final String LINE_SEP = "_line_";
    public static final String CLASS_SEP = ";";

    private String className;
    private String classId;
    private CompilationUnit cu;
    private Stack<Probe> stack;
    private Map<ASTNode, CodeEntity> codeMap;
    private Map<ASTNode, CodeBlock> blockMap;
    private Map<ASTNode, Stmt> stmtMap;
    private Map<ASTNode, Expr> exprMap;
    private Map<ASTNode, CodeName> nameMap;
    private Map<ASTNode, Literal> literalMap;
    private Map<Integer, Map<String, NameRef>> nameRefMap;
    private List<Predicate> predicates;
    private TreeMap<Integer, MethodDecl> methodMap;
    private List<Probe> probes;
    private Set<String> probeNames;
    private CoverageInfo coverage;
    private MethodMatcher methodMatcher;
    private boolean coveredOnly;

    private NameRef currMethod;
    private boolean addPredicate;
    private Map<IVariableBinding, VariableDeclarationFragment> notInitialized;
    private Map<Probe, VariableDeclarationFragment> nonInitMap;

    public NodeVisitor(String className, String classId, CompilationUnit cu) {
        this(className, classId, cu, null, new Facts(className, classId));
    }

    public NodeVisitor(String className, String classId, CompilationUnit cu, CoverageInfo coverage) {
        this(className, classId, cu, coverage, new Facts(className, classId));
    }

    public NodeVisitor(String className, String classId, CompilationUnit cu, CoverageInfo coverage, Facts facts) {
        this(className, classId, cu, coverage, facts, true);
    }

    public NodeVisitor(String className, String classId, CompilationUnit cu, CoverageInfo coverage,
        Facts facts, boolean coveredOnly) {
        this.className = className;
        this.classId = classId;
        this.cu = cu;
        facts.addClassId(classId, className);
        this.codeMap = facts.getCodeMap(classId);
        this.blockMap = facts.getBlockMap(classId);
        this.stmtMap = facts.getStmtMap(classId);
        this.exprMap = facts.getExprMap(classId);
        this.nameMap = facts.getNameMap(classId);
        this.literalMap = facts.getLiteralMap(classId);
        this.nameRefMap = facts.getNameRefMap();
        this.methodMap = facts.getMethods(classId);
        predicates = facts.getPredicates(classId);
        probes = new ArrayList<>();
        probeNames = new HashSet<>();
        stack = new Stack<>();
        this.coverage = coverage;
        methodMatcher = new MethodMatcher();
        this.coveredOnly = coveredOnly;
        notInitialized = new HashMap<>();
        nonInitMap = new HashMap<>();
    }

    public Map<Probe, VariableDeclarationFragment> getNonInitMap() {
        return nonInitMap;
    }

    public String getClassId() {
        return classId;
    }

    public Map<ASTNode, Expr> getExprMap() {
        return exprMap;
    }

    public Map<Integer, Map<String, NameRef>> getNameRefMap() {
        return nameRefMap;
    }

    public List<Predicate> getPredicates() {
        return predicates;
    }

    public List<Probe> getProbes() {
        return probes;
    }

    public Set<String> getProbeNames() {
        return probeNames;
    }

    public void setMethodMatcher(MethodMatcher matcher) {
        if(matcher != null)
            methodMatcher = matcher;
    }

    public boolean addPredicate(Predicate p) {
        return this.predicates.add(p);
    }

    @Override
    public void postVisit(ASTNode node) {
        if(!stack.isEmpty() && node.equals(stack.peek().getTarget())) {
            Probe p = stack.pop();
            //Add root probes to the list.
            if(p.getParent() == null)
                probes.add(p);
        }
    }

    @Override
    public void preVisit(ASTNode node) {
        boolean isCovered = isCovered(node);
        //Set adding predicate.
        addPredicate = !coveredOnly || isCovered;

        //Checking for probing.
        if(isCovered && node instanceof Statement stmt) {
            addToCodeMap(stmt);
        } else if(isCovered && node instanceof Expression e) {
            ASTNode loc = getProbeLocation(e);
            if(loc != null &&  checkValidExpression(e)) {
                if(isProbingPossible(e, loc)) {
                    if(e instanceof Name n) {
                        addProbeForVariables(n, loc);
                    } else {
                        Expr expr = addToExprMap(e);
                        addProbe(expr, e, loc);
                    }
                } else if(e instanceof Name n){
                    //Add a not probed name for future reference.
                    addNameRefForVariables(n);
                }
            }
            addToExprMap(e);
        } else if(addPredicate) {
            addToCodeMap(node);
        }
    }

    private boolean isCovered(ASTNode node) {
        int startLine = CodeUtils.getStartLine(node, cu);
        int endLine = CodeUtils.getEndLine(node, cu);
        if(coverage != null && !coverage.isCovered(className, startLine, endLine))
            return false;
        return true;
    }

    private boolean isProbingPossible(Expression e, ASTNode loc) {
        if(loc instanceof SuperConstructorInvocation
            || loc instanceof ConstructorInvocation
            || loc instanceof TryStatement
            || loc instanceof SwitchCase)
            return false;
        return true;
    }

    private ASTNode getProbeLocation(ASTNode node) {
        ASTNode loc = node.getParent();
        while(loc != null && !checkValidLocation(loc)) {
            loc = loc.getParent();
        }
        return loc;
    }

    private boolean checkValidLocation(ASTNode loc) {
        if((loc instanceof Statement
            || loc instanceof FieldDeclaration)
            && loc.getLocationInParent() instanceof ChildListPropertyDescriptor) {
            return true;
        }
        return false;
    }

    private boolean checkValidExpression(ASTNode node) {
        switch(node.getNodeType()) {
            case ASTNode.NULL_LITERAL:
            case ASTNode.NUMBER_LITERAL:
            case ASTNode.BOOLEAN_LITERAL:
            case ASTNode.CHARACTER_LITERAL:
            case ASTNode.STRING_LITERAL:
            case ASTNode.THIS_EXPRESSION:
            case ASTNode.ASSIGNMENT:
            case ASTNode.ARRAY_CREATION:
            case ASTNode.ARRAY_INITIALIZER:
            case ASTNode.CASE_DEFAULT_EXPRESSION:
            case ASTNode.CAST_EXPRESSION:
            case ASTNode.CLASS_INSTANCE_CREATION:
            case ASTNode.CREATION_REFERENCE:
            case ASTNode.EXPRESSION_METHOD_REFERENCE:
            case ASTNode.METHOD_REF:
            case ASTNode.POSTFIX_EXPRESSION:
            case ASTNode.SUPER_METHOD_REFERENCE:
            case ASTNode.TYPE_LITERAL:
            case ASTNode.TYPE_METHOD_REFERENCE:            
            case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
                return false;
        }

        StructuralPropertyDescriptor desc = node.getLocationInParent();
        return !(
            (node instanceof Annotation) ||
            (node.getParent() instanceof Assignment
                && Assignment.LEFT_HAND_SIDE_PROPERTY.equals(desc)) ||
            (node.getParent() instanceof VariableDeclarationFragment
                && VariableDeclarationFragment.NAME_PROPERTY.equals(desc)) ||
            (node instanceof SuperFieldAccess
                && SuperFieldAccess.NAME_PROPERTY.equals(desc)) ||
            ((node instanceof MethodInvocation || node instanceof SuperMethodInvocation)
                && node.getParent() instanceof ExpressionStatement) ||
            (node instanceof PrefixExpression prefix
                && (prefix.getOperator().equals(PrefixExpression.Operator.INCREMENT)
                    || prefix.getOperator().equals(PrefixExpression.Operator.DECREMENT))) ||
            (node instanceof PostfixExpression postfix
                && (postfix.getOperator().equals(PostfixExpression.Operator.INCREMENT)
                    || postfix.getOperator().equals(PostfixExpression.Operator.DECREMENT)))
        );
    }

    @Override
    public boolean visit(FieldAccess node) {
        if(addPredicate) {
            Expression expression = node.getExpression();
            Expr fa = addToExprMap(node);
            if(expression instanceof Name name) {
                NameRef eName = getNameRef(name.resolveBinding());
                Ref ref = new Ref(eName.getId(), fa.getId(), getLine(expression));
                predicates.add(ref);
            } else if(!(expression instanceof ThisExpression)) {
                //For this.field, 'this' is not added for ref/3.
                Expr expr = addToExprMap(expression);
                Ref ref = new Ref(expr.getId(), fa.getId(), getLine(expression));
                predicates.add(ref);
            }
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(ArrayAccess node) {
        if(addPredicate) {
            Expr arrayAccess = addToExprMap(node);
            Expr array = addToExprMap(node.getArray());
            Ref ref = new Ref(array.getId(), arrayAccess.getId(), getLine(node.getArray()));
            predicates.add(ref);
        }
        return super.visit(node);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean visit(VariableDeclarationStatement node) {
        //For non-final, non-initialized declarations, save the node for future checking.
        if(!Modifier.isFinal(node.getModifiers())) {
            for(VariableDeclarationFragment vdf : (List<VariableDeclarationFragment>)node.fragments()) {
                Expression init = vdf.getInitializer();
                if(init == null) {
                    SimpleName var = vdf.getName();
                    notInitialized.put((IVariableBinding)var.resolveBinding(), vdf);
                }
            }
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(VariableDeclarationFragment node) {
        Expression initializer = node.getInitializer();
        SimpleName var = node.getName();
        if(addPredicate && initializer != null) {
            NameRef varName = getNameRef(var.resolveBinding());
            Expr initExpr = addToExprMap(initializer);
            Assign assign = new Assign(varName.getId(), initExpr.getId(), getLine(node));
            predicates.add(assign);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(CastExpression node) {
        if(addPredicate) {
            Expr expr = addToExprMap(node);
            Expr subExpr = addToExprMap(node.getExpression());
            Assign assign = new Assign(expr.getId(), subExpr.getId(), getLine(node));
            predicates.add(assign);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(ParenthesizedExpression node) {
        if(addPredicate) {
            Expr expr = addToExprMap(node);
            Expr subExpr = addToExprMap(node.getExpression());
            Assign assign = new Assign(expr.getId(), subExpr.getId(), getLine(node));
            predicates.add(assign);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(Assignment node) {
        if(addPredicate) {
            Expression lhs = node.getLeftHandSide();
            Expression rhs = node.getRightHandSide();
            Expr lhsExpr = addToExprMap(lhs);
            Expr rhsExpr = addToExprMap(rhs);

            //TODO: need to handle different assignment operators such as +=, -=.
            Assign assign = new Assign(lhsExpr.getId(), rhsExpr.getId(), getLine(node));
            predicates.add(assign);
        }
        return super.visit(node);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean visit(MethodDeclaration node) {
        currMethod = addToMethodMap(node);

        //Create param predicates.
        List<SingleVariableDeclaration> params = (List<SingleVariableDeclaration>)node.parameters();
        for(int index=0; index < params.size(); index++) {
            SingleVariableDeclaration p = params.get(index);
            NameRef n = getNameRef(p.getName().resolveBinding());
            //param's index starts from 1.
            Param param = new Param(n.getId(), index+1, currMethod.getId());
            predicates.add(param);
        }

        //Create throw predicates.
        for(Type exception : (List<Type>)node.thrownExceptionTypes()) {
            ITypeBinding exceptionBinding = exception.resolveBinding();
            if(exceptionBinding != null) {
                predicates.add(getThrowPredicate(currMethod, exceptionBinding, null));
            }
        }

        return super.visit(node);
    }

    private NameRef addToMethodMap(MethodDeclaration node) {
        IMethodBinding binding = node.resolveBinding();
        NameRef method = getNameRef(binding);
        int startLine = CodeUtils.getStartLine(node, cu);
        MethodDecl methodDecl = new MethodDecl(method.getId(), CodeUtils.getRange(classId, node, cu));
        methodMap.put(startLine, methodDecl);
        return method;
    }

    @Override
    public boolean visit(ThrowStatement node) {
        if(addPredicate) {
            ITypeBinding exceptionBinding = node.getExpression().resolveTypeBinding();
            predicates.add(getThrowPredicate(currMethod, exceptionBinding, getLine(node)));
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(EnhancedForStatement node) {
        if(addPredicate) {
            Expression expr = node.getExpression();
            Expr collExpr = addToExprMap(expr);
            Stmt forStmt = addToStmtMap(node);

            Ref ref = new Ref(collExpr.getId(), forStmt.getId(), getLine(expr));
            predicates.add(ref);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(ReturnStatement node) {
        if(addPredicate) {
            Expression expr = node.getExpression();
            Expr returnExpr = addToExprMap(expr);

            Return ret = new Return(returnExpr.getId(), currMethod.getId(), getLine(node));
            predicates.add(ret);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(ConditionalExpression node) {
        if(addPredicate) {
            Expr boolExpr = addToExprMap(node.getExpression());
            Expr thenExpr = addToExprMap(node.getThenExpression());
            Expr elseExpr = addToExprMap(node.getElseExpression());

            CondExpr condExpr = new CondExpr(boolExpr.getId(), thenExpr.getId(), elseExpr.getId(), getLine(node));
            predicates.add(condExpr);
        }
        return super.visit(node);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean visit(MethodInvocation node) {
        if(addPredicate) {
            Expr methodInvoc = addToExprMap(node);
            processInvocations(methodInvoc, getLine(node), node.resolveMethodBinding(), node.arguments());

            //For expr.method(args), add probe to expr.
            Expression e = node.getExpression();
            if(e instanceof Name name) {
                NameRef n = getNameRef(name.resolveBinding());
                Ref ref = new Ref(n.getId(), methodInvoc.getId(), getLine(name));
                predicates.add(ref);
            } else if(e != null) {
                Expr expr = addToExprMap(e);
                Ref ref = new Ref(expr.getId(), methodInvoc.getId(), getLine(e));
                predicates.add(ref);
            }
        }
        return super.visit(node);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean visit(SuperMethodInvocation node) {
        if(addPredicate) {
            Expr expr = addToExprMap(node);
            Line line = getLine(node);
            processInvocations(expr, line, node.resolveMethodBinding(), node.arguments());
        }
        return super.visit(node);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean visit(ConstructorInvocation node) {
        if(addPredicate) {
            //Consider this as an expression, not a statement for convenience of processing invocation together.
            Expr expr = addToExprMap(node);
            Line line = getLine(node);
            processInvocations(expr, line, node.resolveConstructorBinding(), node.arguments());
        }
        return super.visit(node);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean visit(SuperConstructorInvocation node) {
        if(addPredicate) {
            Expr expr = addToExprMap(node);
            Line line = getLine(node);
            processInvocations(expr, line, node.resolveConstructorBinding(), node.arguments());
        }
        return super.visit(node);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean visit(ClassInstanceCreation node) {
        if(addPredicate) {
            Expr expr = addToExprMap(node);
            Line line = getLine(node);
            processInvocations(expr, line, node.resolveConstructorBinding(), node.arguments());

            Expression e = node.getExpression();
            if(e instanceof Name name) {
                NameRef n = getNameRef(name.resolveBinding());
                Ref ref = new Ref(n.getId(), expr.getId(), getLine(name));
                predicates.add(ref);
            }
        }
        return super.visit(node);
    }

    private void processInvocations(CodeEntity ce, Line line, IMethodBinding binding, List<Expression> arguments) {
        NameRef method = getNameRef(binding);
        MethodInvoc invoc = new MethodInvoc(ce.getId(), method.getId(), line);
        predicates.add(invoc);

        if(binding != null) {
            for(ITypeBinding exceptionBinding : binding.getExceptionTypes()) {
                predicates.add(getThrowPredicate(ce, exceptionBinding, line));
            }
            if(methodMatcher.hasMatch(binding, line.getLineNum())) {
                Throw t = new Throw(ce.getId(), Facts.NPE_ATOM, line);
                predicates.add(t);
            } else {
                methodMatcher.addCandidate(line.getLineNum(), ce);
            }
        }

        //Create argument predicates.
        int index = 1;
        for(Expression arg : arguments) {
            Expr argExpr = addToExprMap(arg);
            Argument argument = new Argument(argExpr.getId(), index++, ce.getId());
            predicates.add(argument);
        }
    }

    private Throw getThrowPredicate(NameRef method, ITypeBinding binding, Line line) {
        String methodId = method == null ? line.getClassId() : method.getId();
        if(line == null)
            return new Throw(methodId, CodeUtils.camelToLower(binding.getName()));
        return new Throw(methodId, CodeUtils.camelToLower(binding.getName()));
    }

    private Throw getThrowPredicate(CodeEntity ce, ITypeBinding binding, Line line) {
        return new Throw(ce.getId(), CodeUtils.camelToLower(binding.getName()), line);
    }

    private void addProbe(Expr expr, Expression n, ASTNode loc) {
        String probeName = getProbeName(expr, n);
        int index = 1;
        while(probeNames.contains(probeName))
            probeName = getProbeName(expr, n, index++);
        Probe probe = new Probe(probeName, n, new ProbeLocation(loc));
        if(!stack.isEmpty()) {
            stack.peek().addChild(probe);
        }

        stack.push(probe);
        probeNames.add(probeName);
        //If an uninitialized name is probed, need to add an initializer later.
        if(n instanceof SimpleName name) {
            IVariableBinding binding = (IVariableBinding)name.resolveBinding();
            if(notInitialized.containsKey(binding)) {
                nonInitMap.put(probe, notInitialized.get(binding));
            }
        }
    }

    private String getProbeName(Expr expr, Expression n) {
        return expr.getId() + LINE_SEP + CodeUtils.getStartLine(n, cu);
    }

    private String getProbeName(Expr expr, Expression n, int index) {
        return expr.getId() + LINE_SEP + CodeUtils.getStartLine(n, cu) + PROBE_INDEX_SEP + index;
    }

    private void addNameRefForVariables(Name node) {
        IBinding binding = node.resolveBinding();
        if(binding != null && binding instanceof IVariableBinding) {
            NameRef var = getNameRef(binding);
            Map<String, NameRef> map = nameRefMap.get(Facts.NAME_REF_TYPE);
            String newKey = classId + CLASS_SEP + var.getName() + LINE_SEP + CodeUtils.getStartLine(node, cu);
            var = new NameRef(var.getId(), var.getKind(), var.getName(), newKey);
            map.putIfAbsent(binding.getKey(), var);
        }
    }

    private void addProbeForVariables(Name node, ASTNode loc) {
        CodeName var = addToNameMap(node);
        if(var.getNameRef().isVariable() && loc != null && isValidVariableLocation(node))
            addProbe(var, node, loc);
    }

    private boolean isValidVariableLocation(Name node) {
        ASTNode parent = node.getParent();
        return !(
            (parent == null
            || (parent instanceof SingleVariableDeclaration
                && SingleVariableDeclaration.NAME_PROPERTY.equals(node.getLocationInParent()))
            || (parent instanceof VariableDeclarationFragment
                && VariableDeclarationFragment.NAME_PROPERTY.equals(node.getLocationInParent()))) ||
            (parent instanceof FieldAccess
                && FieldAccess.NAME_PROPERTY.equals(node.getLocationInParent())) ||
            (parent instanceof PrefixExpression prefix
                && (prefix.getOperator().equals(PrefixExpression.Operator.INCREMENT)
                    || prefix.getOperator().equals(PrefixExpression.Operator.DECREMENT))) ||
            (parent instanceof PostfixExpression postfix
                && (postfix.getOperator().equals(PostfixExpression.Operator.INCREMENT)
                    || postfix.getOperator().equals(PostfixExpression.Operator.DECREMENT)))
        );
    }

    @Override
    public boolean visit(SimpleName node) {
        return super.visit(node);
    }

    @Override
    public boolean visit(QualifiedName node) {
        if(addPredicate && node.getQualifier() instanceof SimpleName qualifier) {
            CodeName qName = addToNameMap(node);
            CodeName eName = addToNameMap(qualifier);
            Ref ref = new Ref(eName.getId(), qName.getId(), getLine(qualifier));
            predicates.add(ref);
            if(isCovered(node) && checkValidExpression(node))
                addProbeForVariables(qualifier, getProbeLocation(node));
            if(!stack.isEmpty() && qualifier.equals(stack.peek().getTarget()))
                stack.pop();
        }
        return false;
    }

    @Override
    public boolean visit(SimpleType node) {
        return false;
    }

    @Override
    public boolean visit(QualifiedType node) {
        return false;
    }

    private Line getLine(ASTNode node) {
        return new Line(classId, CodeUtils.getStartLine(node, cu));
    }

    private CodeEntity addToCodeMap(ASTNode node) {
        if(node instanceof Block block) {
            return addToBlockMap(block);
        } else if(node instanceof Statement stmt) {
            return addToStmtMap(stmt);
        } else if(node instanceof Expression expr) {
            return addToExprMap(expr);
        } else if(node instanceof Name name) {
            return addToNameMap(name);
        } else if(CodeUtils.isLiteral(node)) {
            return addToLiteralMap(node);
        } else if(codeMap.containsKey(node)) {
            return codeMap.get(node);
        } else {
            CodeEntity ce = null;
            ASTNode parentNode = node.getParent();
            if(parentNode != null) {
                CodeEntity parent = addToCodeMap(parentNode);
                Loc locInParent = new Loc(node);
                ce = new CodeEntity(CodeEntity.createId(classId, codeMap.size()+1), classId, node, parent, locInParent);
                ce.setRange(CodeUtils.getRange(classId, node, cu));
                codeMap.put(node, ce);
            } else {
                ce = new CodeEntity(CodeEntity.createId(classId, codeMap.size()+1), classId, node);
                ce.setRange(CodeUtils.getRange(classId, node, cu));
                codeMap.put(node, ce);
            }
            return ce;
        }
    }

    private CodeBlock addToBlockMap(Block node) {
        if(!blockMap.containsKey(node)) {
            ASTNode parent = node.getParent();
            CodeEntity ce = addToCodeMap(parent);
            Loc loc = new Loc(node);
            CodeBlock block = new CodeBlock(CodeBlock.createId(classId, blockMap.size()+1), classId, node, ce, loc);
            block.setRange(CodeUtils.getRange(classId, node, cu));
            blockMap.put(node, block);
        }
        return blockMap.get(node);
    }

    private Stmt addToStmtMap(Statement node) {
        if(!stmtMap.containsKey(node)) {
            ASTNode parentNode = node.getParent();
            CodeEntity parent = addToCodeMap(parentNode);
            Stmt stmt = new Stmt(Stmt.createId(classId, stmtMap.size()+1), classId, node, parent);
            stmt.setRange(CodeUtils.getRange(classId, node, cu));
            stmtMap.put(node, stmt);
        }
        return stmtMap.get(node);
    }

    private Expr addToExprMap(ASTNode node) {
        if(node == null) {
            return Expr.getNone();
        }
        if(node instanceof Name name) {
            return addToNameMap(name);
        } else if(CodeUtils.isLiteral(node)) {
            return addToLiteralMap(node);
        } else {
            if(node instanceof FieldAccess fa
                && fa.getExpression() instanceof ThisExpression) {
                return addToNameMap(fa.getName());
            }
            if(exprMap.containsKey(node)) {
                return exprMap.get(node);
            }
            return getExpr(node);
        }
    }

    private Expr getExpr(ASTNode node) {
        ASTNode parentNode = node.getParent();
        CodeEntity parent = addToCodeMap(parentNode);
        Loc locInParent = new Loc(node);
        Expr expr = new Expr(node, classId, exprMap.size()+1, parent, locInParent);
        expr.setRange(CodeUtils.getRange(classId, node, cu));
        exprMap.put(node, expr);
        return expr;
    }

    private Literal addToLiteralMap(ASTNode node) {
        if(!literalMap.containsKey(node)) {
            ASTNode parentNode = node.getParent();
            CodeEntity parent = addToCodeMap(parentNode);
            Loc locInParent = new Loc(node);
            Literal literal = new Literal(node, classId, literalMap.size()+1, parent, locInParent);
            literal.setRange(CodeUtils.getRange(classId, node, cu));
            literalMap.put(node, literal);
        }
        return literalMap.get(node);
    }

    private String getQualifiedNameKey(QualifiedName name) {
        List<String> keys = new ArrayList<>();
        Name qualifier = name.getQualifier();
        SimpleName fName = name.getName();
        if(qualifier instanceof QualifiedName qName)
            keys.add(getQualifiedNameKey(qName));
        else {
            IBinding qb = qualifier.resolveBinding();
            keys.add(qb != null ? qb.getKey() : qualifier.toString());
        }
        IBinding fb = fName.resolveBinding();
        keys.add(fb != null ? fb.getKey() : fName.toString());

        return String.join(NameRef.KEY_DELIM, keys);
    }

    private CodeName addToNameMap(Name name) {
        if(!nameMap.containsKey(name)) {
            IBinding binding = name.resolveBinding();
            NameRef nameRef = null;
            if(binding instanceof IVariableBinding
                && name instanceof QualifiedName qName) {
                String key = getQualifiedNameKey(qName);
                Map<String, NameRef> map = nameRefMap.get(NameRef.K_QUALIFIED);
                nameRef = new NameRef(map.size()+1, NameRef.K_QUALIFIED, qName.toString(), key);
                map.putIfAbsent(key, nameRef);
            } else {
                nameRef = getNameRef(binding);
            }
            ASTNode parentNode = name.getParent();
            CodeEntity parent = null;
            if(parentNode instanceof FieldAccess fa
                && fa.getExpression() instanceof ThisExpression) {
                parent = getExpr(parentNode);
            } else {
                parent = addToCodeMap(parentNode);
            }
            Loc locInParent = new Loc(name);
            CodeName codeName = new CodeName(nameRef, classId, name, parent, locInParent);
            codeName.setRange(CodeUtils.getRange(classId, name, cu));
            nameMap.put(name, codeName);
        }
        return nameMap.get(name);
    }

    private NameRef getNameRef(IBinding binding) {
        if(binding != null) {
            //map shouldn't be null - initialized in the constructor.
            Map<String, NameRef> map = nameRefMap.get(binding.getKind());
            if(binding instanceof IMethodBinding mb) {
                binding = mb.getMethodDeclaration();
            }
            map.putIfAbsent(binding.getKey(), new NameRef(binding, map.size()+1));
            return map.get(binding.getKey());
        }
        return NameRef.UNKNOWN;
    }
}