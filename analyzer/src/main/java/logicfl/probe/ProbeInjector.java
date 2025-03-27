package logicfl.probe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Dimension;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import logicfl.utils.CodeUtils;

@SuppressWarnings("unchecked")
public class ProbeInjector {
    public static final String MARKER_START = "PROBE_START_LINE_";
    public static final String MARKER_END = "PROBE_END_LINE_";
    public static final String DO_COND_TOGGLE = "DO_COND_TOGGLE_LINE_";
    public static final String FOR_STMT_TOGGLE = "FOR_STMT_TOGGLE_LINE_";
    public static final int MODE_STATEMENT = 0;
    public static final int MODE_FIELD = 1;
    public static final int MODE_SHORT_CIRCUIT = 2;
    public static final int MODE_ASSIGN_ONLY = 3;

    private Set<String> importedClasses;
    private Set<String> missingClasses;
    private String packageName;
    private CompilationUnit cu;
    private LineMatcher matcher;
    private AST ast;
    private Document doc;
    private ASTRewrite rewrite;
    private ListRewrite listRewrite;
    private Map<String, SimpleEntry<VariableDeclarationStatement, ListRewrite>> markerStartMap;
    private Map<String, SimpleEntry<VariableDeclarationStatement, ListRewrite>> markerEndMap;
    private Map<ASTNode, ASTNode> relocatedNodes;
    private Map<Probe, VariableDeclarationFragment> nonInitMap;

    public ProbeInjector(CompilationUnit cu, String source) {
        this.cu = cu;
        this.matcher = new LineMatcher();
        importedClasses = new HashSet<>();
        missingClasses = new HashSet<>();
        packageName = getImportedClasses();
        ast = cu.getAST();
        doc = new Document(source);
        rewrite = ASTRewrite.create(ast);
        listRewrite = null;
        markerStartMap = null;
        markerEndMap = null;
        nonInitMap = null;
    }

    public String inject(List<Probe> probes, Path outFilePath, Map<Probe, VariableDeclarationFragment> nonInitMap) {
        markerStartMap = new HashMap<>();
        markerEndMap = new HashMap<>();
        relocatedNodes = new HashMap<>();
        this.nonInitMap = nonInitMap;
        //Store relocated initializers.
        Map<ForStatement, LocationInfo> forInitMap = new HashMap<>();
        //Store the new if statement and the path to the original condition.
        Map<ForStatement, LocationInfo> forExprMap = new HashMap<>();
        //Store the toggle if statement and added updaters.
        Map<ForStatement, LocationInfo> forUpdMap = new HashMap<>();

        for(Probe probe : probes) {
            ASTNode loc = probe.getLocation().getLocNode();
            listRewrite = null;

            Statement parentStmt = CodeUtils.getEnclosingStatement(probe.getTarget());

            //If a body of an if and loop statements are not Block, add a block for future probing.
            loc = checkSingleStmtBody(probe, loc, parentStmt);
            boolean probeNotInBody = !isTargetInLoopBody(probe.getTarget(), parentStmt);

            //Maybe checking whether probe is in the condition?
            if(loc instanceof WhileStatement whileStmt && probeNotInBody) {
                injectProbeInWhileStatement(probe, loc, whileStmt);
            } else if(loc instanceof DoStatement doStmt && probeNotInBody) {
                injectProbeInDoStatement(probe, loc, doStmt);
            } else if(loc instanceof ForStatement forStmt && probeNotInBody) {
                injectProbeInForStatement(probe, loc, forStmt, forInitMap, forExprMap, forUpdMap, nonInitMap);
            } else {
                ListRewrite markerRewrite = listRewrite == null ? CodeUtils.getListRewrite(rewrite, loc) : listRewrite;
                addMarker(markerRewrite, loc, MARKER_START, markerStartMap);
                injectProbe(probe);
                addMarker(markerRewrite, loc, MARKER_END, markerEndMap);
            }
        }
        listRewrite = null;
        return rewriteSourceCode(outFilePath);
    }

    private boolean isTargetInLoopBody(Expression target, Statement parentStmt) {
        TreePath path = CodeUtils.findTreePath(target, parentStmt);
        StructuralPropertyDescriptor desc = path.getLocation();
        return WhileStatement.BODY_PROPERTY.equals(desc) ||
            DoStatement.BODY_PROPERTY.equals(desc) ||
            ForStatement.BODY_PROPERTY.equals(desc);
    }

    private ASTNode checkSingleStmtBody(Probe probe, ASTNode loc, Statement parentStmt) {
        if(parentStmt != null && !(parentStmt instanceof Block)
            && parentStmt != loc && !relocatedNodes.containsKey(parentStmt)) {
            relocateStmtToBlock(probe, parentStmt, loc);
            if(probe.getLocation().getLocNode() != loc)
                loc = probe.getLocation().getLocNode();
        } else if(relocatedNodes.containsKey(parentStmt)) {
            //For a relocated node, need to adjust probe's location for the new block.
            listRewrite = rewrite.getListRewrite(parentStmt.getParent(), Block.STATEMENTS_PROPERTY);
            probe.getLocation().setLocNode(parentStmt);
            loc = parentStmt;
        }
        return loc;
    }

    private void injectProbeInWhileStatement(Probe probe, ASTNode loc, WhileStatement whileStmt) {
        ListRewrite markerRewrite = getMarkerRewrite(probe, loc, whileStmt.getBody());

        //Add markers before the first while body statement.
        //Markers should have lines of the while condition.
        List<Statement> bodyStmts = markerRewrite.getRewrittenList();
        Statement firstStmt = bodyStmts.size() > 0 ? bodyStmts.get(0) : null;
        Expression cond = whileStmt.getExpression();
        int markerLoc = firstStmt == null ? ProbeLocation.INSERT_LAST : ProbeLocation.INSERT_BEFORE;
        ProbeLocation pLoc = new ProbeLocation(firstStmt, markerLoc);
        int startLine = CodeUtils.getStartLine(cond, cu);
        int endLine = CodeUtils.getEndLine(cond, cu);
        addMarker(markerRewrite, pLoc, MARKER_START+startLine, startLine, endLine, markerStartMap);
        pLoc = new ProbeLocation(firstStmt, markerLoc);
        Statement endMarker = addMarker(markerRewrite, pLoc, MARKER_END+startLine, startLine, endLine, markerEndMap);

        //Add a break route between the markers.
        Expression copied = (Expression)ASTNode.copySubtree(ast, cond);
        TreePath path = probe.getPath(cond);
        IfStatement ifStmt = createIfStmtWithNegatedCondition(copied, path);
        replaceWithProbeName(probe.getName(), path.getTop(), ifStmt);
        markerRewrite.insertBefore(ifStmt, endMarker, null);

        //Inject probes before the new if condition.
        probe.setLocation(new ProbeLocation(ifStmt, ProbeLocation.INSERT_BEFORE));
        listRewrite = markerRewrite;
        injectProbe(probe);

        //Replace the while condition to true.
        rewrite.replace(cond, ast.newBooleanLiteral(true), null);
    }

    private void injectProbeInDoStatement(Probe probe, ASTNode loc, DoStatement doStmt) {
        ListRewrite markerRewrite = getMarkerRewrite(probe, loc, doStmt.getBody());

        //Add markers at the beginning of the body.
        List<Statement> bodyStmts = markerRewrite.getRewrittenList();
        Statement firstStmt = bodyStmts.size() > 0 ? bodyStmts.get(0) : null;
        int startLine = CodeUtils.getStartLine(doStmt, cu);
        int endLine = CodeUtils.getEndLine(doStmt, cu);
        int markerLoc = firstStmt == null ? ProbeLocation.INSERT_LAST : ProbeLocation.INSERT_BEFORE;
        ProbeLocation pLoc = new ProbeLocation(firstStmt, markerLoc);
        Statement startMarker = addMarker(markerRewrite, pLoc, MARKER_START+startLine, startLine, endLine, markerStartMap);
        pLoc = new ProbeLocation(firstStmt, markerLoc);
        //Do statments may already have probes, so don't break the markers.
        if(!markerEndMap.containsKey(MARKER_END+startLine))
            addMarker(markerRewrite, pLoc, MARKER_END+startLine, startLine, endLine, markerEndMap);

        /*
         * do { body; } while (cond);
         * ->
         * do {
         *  boolean toggle = false;
         *  if(toggle) {
         *      if(!(cond)) break;
         *  }
         *  toggle = true;
         *  body;
         * } while (true);
         */

        //Create a toggle for the first condition check - boolean toggleName = false;
        String toggleName = DO_COND_TOGGLE + CodeUtils.getStartLine(doStmt, cu);
        createToggleDeclaration(toggleName, doStmt);
        IfStatement toggleStmt = createToggleStmt(toggleName, startMarker, markerRewrite);

        Expression cond = doStmt.getExpression();
        TreePath path = probe.getPath(cond);
        Expression copied = (Expression)ASTNode.copySubtree(ast, cond);
        IfStatement ifStmt = createIfStmtWithNegatedCondition(copied, path);
        replaceWithProbeName(probe.getName(), path.getTop(), ifStmt);
        Block body = (Block)toggleStmt.getThenStatement();
        body.statements().add(ifStmt);

        //Inject probes for the new if condition.
        probe.setLocation(new ProbeLocation(ifStmt, ProbeLocation.INSERT_BEFORE));
        listRewrite = rewrite.getListRewrite(body, Block.STATEMENTS_PROPERTY);
        injectProbe(probe);

        //Replace the while condition to true.
        rewrite.replace(cond, ast.newBooleanLiteral(true), null);
    }

    private Statement createToggleDeclaration(String toggleName, Statement stmt) {
        ListRewrite markerRewrite = CodeUtils.getListRewrite(rewrite, stmt);
        if(markerRewrite == null) {
            ASTNode copied = ASTNode.copySubtree(ast, stmt);
            Block block = ast.newBlock();
            rewrite.replace(stmt, block, null);
            block.statements().add(copied);
            markerRewrite = CodeUtils.getListRewrite(rewrite, stmt);
        }

        VariableDeclarationFragment vdf = ast.newVariableDeclarationFragment();
        vdf.setName(ast.newSimpleName(toggleName));
        vdf.setInitializer(ast.newBooleanLiteral(false));
        VariableDeclarationStatement vds = ast.newVariableDeclarationStatement(vdf);
        vds.setType(ast.newPrimitiveType(PrimitiveType.BOOLEAN));

        //Add markers for the previous line of the given statement.
        int startLine = CodeUtils.getStartLine(stmt, cu)-1;
        int endLine = startLine;
        ProbeLocation pLoc = new ProbeLocation(stmt, ProbeLocation.INSERT_BEFORE);
        addMarker(markerRewrite, pLoc, MARKER_START+startLine, startLine, endLine, markerStartMap);
        pLoc = new ProbeLocation(stmt, ProbeLocation.INSERT_BEFORE);
        Statement endMarker = addMarker(markerRewrite, pLoc, MARKER_END+startLine, startLine, endLine, markerEndMap);
        markerRewrite.insertBefore(vds, endMarker, null);

        return endMarker;
    }

    private IfStatement createToggleStmt(String toggleName, Statement loc, ListRewrite markerRewrite) {
        return createToggleStmt(toggleName, loc, markerRewrite, false);
    }

    private IfStatement createToggleStmt(String toggleName, Statement loc,
        ListRewrite markerRewrite, boolean toggleOn) {
        IfStatement toggleStmt = ast.newIfStatement();
        if(toggleOn) {
            //toggleName set to false first, so this will make the condition passed.
            PrefixExpression negated = ast.newPrefixExpression();
            negated.setOperator(PrefixExpression.Operator.NOT);
            negated.setOperand(ast.newSimpleName(toggleName));
            toggleStmt.setExpression(negated);
        } else {
            toggleStmt.setExpression(ast.newSimpleName(toggleName));
        }
        Block body = ast.newBlock();
        toggleStmt.setThenStatement(body);
        if(loc == null)
            markerRewrite.insertFirst(toggleStmt, null);
        else
            markerRewrite.insertAfter(toggleStmt, loc, null);

        Assignment toggle = ast.newAssignment();
        toggle.setLeftHandSide(ast.newSimpleName(toggleName));
        toggle.setRightHandSide(ast.newBooleanLiteral(true));
        ExpressionStatement exprStmt = ast.newExpressionStatement(toggle);

        //if toggle is off, the else part is the one executed only for the first time.
        if(!toggleOn) {
            body = ast.newBlock();
            toggleStmt.setElseStatement(body);
        }
        body.statements().add(exprStmt);

        return toggleStmt;
    }

    private void injectProbeInForStatement(Probe probe, ASTNode loc, ForStatement forStmt,
        Map<ForStatement, LocationInfo> forInitMap, Map<ForStatement, LocationInfo> forExprMap,
        Map<ForStatement, LocationInfo> forUpdMap, Map<Probe, VariableDeclarationFragment> nonInitMap) {
        Statement body = (Statement)rewrite.get(forStmt, ForStatement.BODY_PROPERTY);
        ListRewrite markerRewrite = getMarkerRewrite(probe, loc, body);
        TreePath path = probe.getPath(forStmt);
        if(ForStatement.INITIALIZERS_PROPERTY.equals(path.getLocation())) {
            injectProbeInForInitializer(probe, forStmt, forInitMap, forExprMap,forUpdMap, markerRewrite);
        } else if(ForStatement.EXPRESSION_PROPERTY.equals(path.getLocation())) {
            injectProbeInForExpression(probe, forStmt, forExprMap, markerRewrite);
        } else if(ForStatement.UPDATERS_PROPERTY.equals(path.getLocation())) {
            injectProbeInForUpdaters(probe, forStmt, forInitMap, forUpdMap, nonInitMap, path, markerRewrite);
        } else {
            //If it's not one of the aboves, it shouldn't do anything.
            return;
        }
    }

    private void relocateForInit(ForStatement forStmt, Statement startMarker,
        Map<ForStatement, LocationInfo> forInitMap, Map<ForStatement, LocationInfo> forExprMap,
        ListRewrite markerRewrite) {

        //Add an if statement with a toggle.
        String toggleName = FOR_STMT_TOGGLE + CodeUtils.getStartLine(forStmt, cu);
        Statement toggleEndMarker = createToggleDeclaration(toggleName, forStmt);
        IfStatement toggleStmt = createToggleStmt(toggleName, startMarker, markerRewrite, true);

        //Initializers should go to the then part.
        ListRewrite newListRewrite = rewrite.getListRewrite(toggleStmt.getThenStatement(), Block.STATEMENTS_PROPERTY);
        forInitMap.put(forStmt, new LocationInfo(toggleStmt, newListRewrite));

        //Initializers are a list of expressions or one single VDE.
        List<ExpressionStatement> copiedInitializers = forInitMap.get(forStmt).statements();
        if(forStmt.initializers().size() > 0 &&
            forStmt.initializers().get(0) instanceof VariableDeclarationExpression vde) {
            ListRewrite initRewrite = rewrite.getListRewrite(toggleStmt.getThenStatement(), Block.STATEMENTS_PROPERTY);
            //For the single VDE, separate each initialization part as an assignment.
            List<VariableDeclarationFragment> orgInitializers = (List<VariableDeclarationFragment>)vde.fragments();
            for(VariableDeclarationFragment vdf : orgInitializers) {
                Expression init = vdf.getInitializer();
                Expression copied = (Expression)ASTNode.copySubtree(ast, init);
                Assignment assign = ast.newAssignment();
                assign.setLeftHandSide(ast.newSimpleName(vdf.getName().getIdentifier()));
                assign.setRightHandSide(copied);
                ExpressionStatement exprStmt = ast.newExpressionStatement(assign);
                copiedInitializers.add(exprStmt);
                initRewrite.insertLast(exprStmt, null);
            }
        } else {
            //For a list of expresions, move them outside of the for statement.
            ListRewrite initRewrite = CodeUtils.getListRewrite(rewrite, forStmt);
            for(Expression init : (List<Expression>)forStmt.initializers()) {
                Expression copiedInit = (Expression)ASTNode.copySubtree(ast, init);
                ExpressionStatement exprStmt = ast.newExpressionStatement(copiedInit);
                initRewrite.insertBefore(exprStmt, toggleEndMarker, null);
                copiedInitializers.add(exprStmt);
                rewrite.remove(init, null);
            }
        }
    }

    private void injectProbeInForInitializer(Probe probe, ForStatement forStmt,
        Map<ForStatement, LocationInfo> forInitMap, Map<ForStatement, LocationInfo> forExprMap,
        Map<ForStatement, LocationInfo> forUpdMap, ListRewrite markerRewrite) {

        SimpleEntry<Statement, Statement> markers = addMarkersToForBody(forStmt, markerRewrite);
        Statement startMarker = markers.getKey();
        Statement endMarker = markers.getValue();

        if(!forInitMap.containsKey(forStmt)) {
            relocateForInit(forStmt, startMarker, forInitMap, forExprMap, markerRewrite);
        }

        List<ExpressionStatement> copiedInitializers = forInitMap.get(forStmt).statements();
        if(copiedInitializers.size() > 0) {
            ListRewrite initRewrite = null;
            TreePath path = getInitPath(probe, forStmt);
            ExpressionStatement exprStmt = null;
            int index = -1;
            if(path.getLocation().equals(VariableDeclarationExpression.FRAGMENTS_PROPERTY)) {
                //The initialization part was separated as assignments.
                initRewrite = forInitMap.get(forStmt).getListRewrite();
                index = path.getIndex(); //get index from VDE's fragments.
                path = path.getChild(); //the child path is actual fragment.
                exprStmt = copiedInitializers.get(index);
                //Adjust tree path.
                TreePath newPath = new TreePath(ExpressionStatement.EXPRESSION_PROPERTY);
                TreePath assign = new TreePath(Assignment.RIGHT_HAND_SIDE_PROPERTY);
                newPath.setChild(assign);
                TreePath vdfInit = path;
                if(vdfInit.getChild() != null) {
                    assign.setChild(vdfInit.getChild());
                }
                replaceWithProbeName(probe.getName(), newPath, exprStmt);
                probe.setPath(path.getTop());
            } else {
                //Initializers are a list of expressions in this case.
                initRewrite = CodeUtils.getListRewrite(rewrite, forStmt);
                index = path.getParent().getIndex();
                exprStmt = copiedInitializers.get(index);
                replaceWithProbeName(probe.getName(), path, exprStmt.getExpression());
            }
            probe.setLocation(new ProbeLocation(exprStmt, ProbeLocation.INSERT_BEFORE));
            listRewrite = initRewrite;
            injectProbe(probe);

            if(index >= 0 && forStmt.initializers().get(0) instanceof VariableDeclarationExpression vde) {
                //Probe injection replaces for init. Reverse it back to a default literal.
                List<VariableDeclarationFragment> orgInitializers = (List<VariableDeclarationFragment>)vde.fragments();
                VariableDeclarationFragment vdf = orgInitializers.get(index);
                Expression defaultLiteral = createDefaultLiteral(vde.getType());
                rewrite.replace(vdf.getInitializer(), defaultLiteral, null);
            }
        }

        TreePath path = new TreePath();
        if(!forExprMap.containsKey(forStmt))
            relocateForExpr(forStmt, path, forExprMap, markerRewrite, endMarker);
        if(!forUpdMap.containsKey(forStmt))
            relocateUpdaters(forStmt, forInitMap, forUpdMap, markerRewrite);
    }

    private TreePath getInitPath(Probe probe, ForStatement forStmt) {
        TreePath path = probe.getPath(forStmt);
        while(path != null && !path.getLocation().equals(ForStatement.INITIALIZERS_PROPERTY))
            path = path.getChild();
        //Initializer's child path indicates the actual expression where the probe belongs.
        return path.getChild();
    }

    private IfStatement relocateForExpr(ForStatement forStmt, TreePath path,
            Map<ForStatement, LocationInfo> forExprMap, ListRewrite markerRewrite, Statement endMarker) {
        Expression copied = (Expression) ASTNode.copySubtree(ast, forStmt.getExpression());
        IfStatement ifStmt = createIfStmtWithNegatedCondition(copied, path);

        //If init. was relocated, ifStmt should go after the stored toggleStmt.
        if (forExprMap.containsKey(forStmt)) {
            markerRewrite = forExprMap.get(forStmt).getListRewrite();
            markerRewrite.insertAfter(ifStmt, forExprMap.get(forStmt).getLoc(), null);
        } else {
            markerRewrite.insertBefore(ifStmt, endMarker, null);
        }
        forExprMap.put(forStmt, new LocationInfo(ifStmt, markerRewrite, path));

        return ifStmt;
    }

    private void injectProbeInForExpression(Probe probe, ForStatement forStmt,
            Map<ForStatement, LocationInfo> forExprMap, ListRewrite markerRewrite) {
        Statement endMarker = addMarkersToForBody(forStmt, markerRewrite).getValue();

        IfStatement ifStmt = null;
        TreePath path = probe.getPath(forStmt.getExpression());
        if(!forExprMap.containsKey(forStmt)) {
            ifStmt = relocateForExpr(forStmt, path, forExprMap, markerRewrite, endMarker);
        }
        ifStmt = (IfStatement)forExprMap.get(forStmt).getLoc();
        TreePath storedPath = forExprMap.get(forStmt).getPath();
        path.setParent(storedPath.getParent());

        //Inject probes before the new if condition.
        replaceWithProbeName(probe.getName(), path.getTop(), ifStmt);
        probe.setLocation(new ProbeLocation(ifStmt, ProbeLocation.INSERT_BEFORE));
        listRewrite = forExprMap.get(forStmt).getListRewrite();
        injectProbe(probe);

        //Replace the for expression to true.
        rewrite.replace(forStmt.getExpression(), ast.newBooleanLiteral(true), null);
    }

    private void relocateUpdaters(ForStatement forStmt, Map<ForStatement, LocationInfo> forInitMap,
            Map<ForStatement, LocationInfo> forUpdMap, ListRewrite markerRewrite) {
        String toggleName = FOR_STMT_TOGGLE + CodeUtils.getStartLine(forStmt, cu);
        //If forInitMap doesn't have forStmt, a toggle declaration needs to be created.
        if(!forInitMap.containsKey(forStmt))
            createToggleDeclaration(toggleName, forStmt);

        //If initializers have been relocated, updaters should go to the else part of them.
        if(forInitMap.containsKey(forStmt)) {
            IfStatement initIfStmt = (IfStatement)forInitMap.get(forStmt).getLoc();
            Block elseBody = ast.newBlock();
            initIfStmt.setElseStatement(elseBody);
            ListRewrite elseListRewrite = rewrite.getListRewrite(elseBody, Block.STATEMENTS_PROPERTY);

            IfStatement toggleStmt = createToggleStmt(toggleName, null, elseListRewrite);
            ListRewrite newListRewrite = rewrite.getListRewrite(toggleStmt.getThenStatement(), Block.STATEMENTS_PROPERTY);
            forUpdMap.put(forStmt, new LocationInfo(toggleStmt, newListRewrite));
        } else {
            //Othrewise, updaters should go to the for body.
            Statement startMarker = addMarkersToForBody(forStmt, markerRewrite).getKey();
            IfStatement toggleStmt = createToggleStmt(toggleName, startMarker, markerRewrite);
            ListRewrite newListRewrite = rewrite.getListRewrite(toggleStmt.getThenStatement(), Block.STATEMENTS_PROPERTY);
            forUpdMap.put(forStmt, new LocationInfo(toggleStmt, newListRewrite));
        }
        markerRewrite = forUpdMap.get(forStmt).getListRewrite();

        List<Expression> updaters = (List<Expression>) forStmt.updaters();
        for(Expression updater : updaters) {
            Expression copied = (Expression)ASTNode.copySubtree(ast, updater);
            ExpressionStatement exprStmt = ast.newExpressionStatement(copied);
            markerRewrite.insertLast(exprStmt, null);
            forUpdMap.get(forStmt).statements().add(exprStmt);
            rewrite.remove(updater, null);
        }
    }

    private void injectProbeInForUpdaters(Probe probe, ForStatement forStmt,
            Map<ForStatement, LocationInfo> forInitMap, Map<ForStatement, LocationInfo> forUpdMap,
            Map<Probe, VariableDeclarationFragment> nonInitMap, TreePath path, ListRewrite markerRewrite) {
        List<ExpressionStatement> updaterStmts = null;
        //If updaters are not relocated to the for body, relocate them.
        ListRewrite updaterListRewrite = rewrite.getListRewrite(forStmt, ForStatement.UPDATERS_PROPERTY);
        if(updaterListRewrite.getRewrittenList().size() > 0) {
            relocateUpdaters(forStmt, forInitMap, forUpdMap, markerRewrite);
        }

        markerRewrite = forUpdMap.get(forStmt).getListRewrite();
        updaterStmts = forUpdMap.get(forStmt).statements();

        int index = path.getIndex();
        if(updaterStmts != null && index >= 0 && index < updaterStmts.size()) {
            ExpressionStatement updaterStmt = (ExpressionStatement)updaterStmts.get(index);
            path.setLocation(ExpressionStatement.EXPRESSION_PROPERTY);
            replaceWithProbeName(probe.getName(), path, updaterStmt);
            probe.setLocation(new ProbeLocation(updaterStmt, ProbeLocation.INSERT_BEFORE));
            listRewrite = markerRewrite;
            injectProbe(probe);
        }
    }

    private SimpleEntry<Statement, Statement> addMarkersToForBody(ForStatement forStmt, ListRewrite markerRewrite) {
        List<Statement> bodyStmts = markerRewrite.getRewrittenList();
        Statement firstStmt = bodyStmts.size() > 0 ? bodyStmts.get(0) : null;
        if(firstStmt != null && isMarker(firstStmt, MARKER_START)) {
            for(Statement stmt : bodyStmts.subList(1, bodyStmts.size())) {
                if(isMarker(stmt, MARKER_END))
                    return new SimpleEntry<>(firstStmt, stmt);
            }
        }
        Statement startMarker = null;
        Statement endMarker = null;
        if(firstStmt == null) {
            int startLine = CodeUtils.getStartLine(forStmt.getBody(), cu);
            int endLine = CodeUtils.getEndLine(forStmt.getBody(), cu);
            ProbeLocation pLoc = new ProbeLocation(firstStmt, ProbeLocation.INSERT_LAST);
            startMarker = addMarker(markerRewrite, pLoc, MARKER_START+startLine, startLine, endLine, markerStartMap);
            pLoc = new ProbeLocation(firstStmt, ProbeLocation.INSERT_LAST);
            endMarker = addMarker(markerRewrite, pLoc, MARKER_END+startLine, startLine, endLine, markerEndMap);
        } else {
            ProbeLocation pLoc = new ProbeLocation(firstStmt, ProbeLocation.INSERT_BEFORE);
            startMarker = addMarker(markerRewrite, pLoc, MARKER_START, markerStartMap);
            pLoc = new ProbeLocation(firstStmt, ProbeLocation.INSERT_BEFORE);
            endMarker = addMarker(markerRewrite, pLoc, MARKER_END, markerEndMap);
        }
        return new SimpleEntry<>(startMarker, endMarker);
    }

    private boolean isMarker(Statement stmt, String marker) {
        if(stmt instanceof VariableDeclarationStatement vds && vds.fragments().size() == 1) {
            VariableDeclarationFragment vdf = (VariableDeclarationFragment)vds.fragments().get(0);
            return vdf.getName().toString().startsWith(marker);
        }
        return false;
    }

    private ListRewrite getMarkerRewrite(Probe probe, ASTNode loc, Statement body) {
        //If listRewrite is not null, the body is relocated, so use the new body's listRewrite.
        ListRewrite markerRewrite = listRewrite;
        if(markerRewrite == null) {
            if(!(body instanceof Block)) {
                relocateStmtToBlock(probe, body, loc);
                markerRewrite = listRewrite;
            } else {
                markerRewrite = rewrite.getListRewrite(body, Block.STATEMENTS_PROPERTY);
            }
        }
        return markerRewrite;
    }

    private IfStatement createIfStmtWithNegatedCondition(Expression cond, TreePath path) {
        ParenthesizedExpression parenthesizedCond = ast.newParenthesizedExpression();
        parenthesizedCond.setExpression(cond);
        PrefixExpression negated = ast.newPrefixExpression();
        negated.setOperator(PrefixExpression.Operator.NOT);
        negated.setOperand(parenthesizedCond);
        IfStatement ifStmt = ast.newIfStatement();
        Block body = ast.newBlock();
        ifStmt.setThenStatement(body);
        ifStmt.setExpression(negated);
        body.statements().add(ast.newBreakStatement());

        path = updateTreePath(path, ParenthesizedExpression.EXPRESSION_PROPERTY);
        path = updateTreePath(path, PrefixExpression.OPERAND_PROPERTY);
        path = updateTreePath(path, IfStatement.EXPRESSION_PROPERTY);

        return ifStmt;
    }

    private TreePath updateTreePath(TreePath curr, StructuralPropertyDescriptor desc) {
        return updateTreePath(curr, desc, -1);
    }

    private TreePath updateTreePath(TreePath curr, StructuralPropertyDescriptor desc, int index) {
        TreePath parent = new TreePath(desc, index);
        curr.setParent(parent);
        return parent;
    }

    private ASTNode relocateStmtToBlock(Probe p, Statement parentStmt, ASTNode loc) {
        ASTNode newStmt = null;
        if(loc != null &&
            ((loc instanceof IfStatement ifStmt && !p.getTarget().equals(ifStmt.getExpression())) ||
            loc.getNodeType() == ASTNode.WHILE_STATEMENT ||
            loc.getNodeType() == ASTNode.DO_STATEMENT ||
            loc.getNodeType() == ASTNode.FOR_STATEMENT ||
            loc.getNodeType() == ASTNode.ENHANCED_FOR_STATEMENT)) {
            newStmt = addBlock(parentStmt, p);
        }
        if(newStmt != null) {
            listRewrite = rewrite.getListRewrite(newStmt.getParent(), Block.STATEMENTS_PROPERTY);
            p.getLocation().setLocNode(newStmt);
            relocatedNodes.put(parentStmt, newStmt);
        }
        return newStmt;
    }

    private ASTNode addBlock(Statement stmt, Probe p) {
        Block block = ast.newBlock();
        ASTNode newStmt = ASTNode.copySubtree(ast, stmt);
        replaceWithProbeName(p.getName(), p.getPath(stmt), newStmt);
        rewrite.replace(stmt, block, null);
        CodeUtils.replaceNodeInAST(stmt, block);
        block.statements().add(stmt);

        return stmt;
    }

    private void injectProbe(Probe p) {
        if(p.getLocation().isField()) {
            injectProbe(p, MODE_FIELD);
        } else {
            injectProbe(p, MODE_STATEMENT);
        }
    }

    private void injectProbe(Probe p, int mode) {
        if(listRewrite == null)
            listRewrite = CodeUtils.getListRewrite(rewrite, p.getLocation().getLocNode());

        //If probe contains a child probe about non-initialized name, add a default init.
        if(nonInitMap.containsKey(p)) {
            VariableDeclarationFragment vdf = nonInitMap.get(p);
            Type type = null;
            if(vdf.getParent() instanceof VariableDeclarationStatement vds) {
                type = vds.getType();
            } else if(vdf.getParent() instanceof VariableDeclarationExpression vde) {
                type = vde.getType();
            }
            if(type != null && vdf.getInitializer() == null) {
                Expression defaultLiteral = createDefaultLiteral(type);
                rewrite.set(vdf, VariableDeclarationFragment.INITIALIZER_PROPERTY, defaultLiteral, null);
            }
        }

        //Update child probes included in the expression.
        Expression newNode = (Expression)ASTNode.copySubtree(ast, p.getTarget());

        //Replace the target with the new variable.
        if(p.getParent() == null)
            replaceTargetWithName(p);
        else
            replaceWithProbeName(p.getName(), p.getPath(), p.getParent().getTargetInProbe());

        //newChildLoc == null indicates the short-circuit probe w/ conditional evaluation.
        Type type = createTypeNode(p.getTarget());
        if(mode == MODE_ASSIGN_ONLY || mode == MODE_SHORT_CIRCUIT) {
            ExpressionStatement es = createAssignment(p, type, newNode);
            p.getLocation().insert(es, listRewrite);
            p.setProbeNode(es, newNode);
            mode = mode == MODE_SHORT_CIRCUIT ? MODE_STATEMENT : mode;
        } else {
            //Add new VDS before the target location.
            if(mode == MODE_FIELD) {
                FieldDeclaration fd = createFieldDeclaration(p, type, newNode);
                p.getLocation().insert(fd, listRewrite);
                p.setProbeNode(fd, newNode);
            } else {
                VariableDeclarationStatement vds = createVariableDeclaration(p, type, newNode);
                p.getLocation().insert(vds, listRewrite);
                p.setProbeNode(vds, newNode);
            }
        }

        //TODO: need to handle a case that loc is a field declaration.
        if(mode == MODE_FIELD) {
            injectChildProbes(p, mode);
        } else if(isBooleanExpression(p.getTarget())) {
            injectProbeForBooleanExpr(p, (InfixExpression)p.getTarget(), mode);
        } else if(p.getTarget() instanceof ConditionalExpression) {
            injectProbeForConditionalExpr(p, mode);
        } else if(p.getTarget() instanceof LambdaExpression) {
            injectProbeForLambdaExpr(p, mode);
        } else {
            injectChildProbes(p, mode);
        }
    }

    private void injectChildProbes(Probe p, int mode) {
        ASTNode currBlock = getBlockNode(p.getTarget());
        for(int i = 0; i < p.getChildren().size(); i++) {
            Probe child = p.getChildren().get(i);
            ASTNode childBlock = getBlockNode(child.getTarget());
            if(childBlock != currBlock) {
                ASTNode orgNode = p.getTarget();
                ASTNode newNode = p.getTargetInProbe();
                updateChildProbe(child, orgNode, newNode);
            } else {
                ProbeLocation pLoc = i == 0 ?
                new ProbeLocation(p.getProbeNode(), ProbeLocation.INSERT_BEFORE) :
                new ProbeLocation(p.getChildren().get(i-1).getProbeNode(), ProbeLocation.INSERT_AFTER);
                child.setLocation(pLoc);
            }
            injectProbe(child, mode);
        }
    }

    private void updateChildProbe(Probe child, ASTNode orgNode, ASTNode newNode) {
        TreePath locPath = CodeUtils.findTreePath(child.getLocation().getLocNode(), orgNode);
        ASTNode newLoc = locPath.getBottomNode(newNode);
        ProbeLocation pLoc = new ProbeLocation(newLoc, ProbeLocation.INSERT_BEFORE);
        child.setLocation(pLoc);
        listRewrite = null;
    }

    private ASTNode getBlockNode(ASTNode node) {
        ASTNode parent = node.getParent();
        while(parent != null) {
            if(parent instanceof Block)
                return parent;
            parent = parent.getParent();
        }
        return null;
    }

    private void injectProbeForBooleanExpr(Probe p, InfixExpression target, int mode) {
        Probe leftOperand = null;
        Probe rightOperand = null;
        List<Probe> extOperands = new ArrayList<>();
        boolean evalIfTrue = target.getOperator().equals(InfixExpression.Operator.CONDITIONAL_AND);
        for(Probe child : p.getChildren()) {
            StructuralPropertyDescriptor desc = child.getPath().getLocation();
            if(desc.equals(InfixExpression.LEFT_OPERAND_PROPERTY)) {
                leftOperand = child;
            } else if(desc.equals(InfixExpression.RIGHT_OPERAND_PROPERTY)) {
                rightOperand = child;
            } else if(desc.equals(InfixExpression.EXTENDED_OPERANDS_PROPERTY)) {
                extOperands.add(child);
            }
        }
        if(leftOperand != null && rightOperand != null) {
            injectProbeForBooleanExpr(leftOperand, rightOperand, extOperands, evalIfTrue, mode);
        } else {
            injectChildProbes(p, mode);
        }
    }

    private void injectProbeForBooleanExpr(Probe leftOperand, Probe rightOperand, List<Probe> extOperands, boolean evalIfTrue, int mode) {
        ListRewrite savedListRewrite = listRewrite;

        //Inject leftOperand probe before the parent.
        ASTNode parentProbeNode = leftOperand.getParent().getProbeNode();
        ProbeLocation pLoc = new ProbeLocation(parentProbeNode, ProbeLocation.INSERT_BEFORE);
        leftOperand.setLocation(pLoc);
        injectProbe(leftOperand, mode);

        //Add a VDS or Assignment and inject a probe for rightOperand before the parent.
        String condVarName = leftOperand.getName();
        PrimitiveType bool = ast.newPrimitiveType(PrimitiveType.BOOLEAN);
        Statement rhs = null;
        if(mode == MODE_ASSIGN_ONLY) {
            rhs = createAssignment(rightOperand, bool, ast.newBooleanLiteral(evalIfTrue));
        } else {
            rhs = createVariableDeclaration(rightOperand, bool, ast.newBooleanLiteral(evalIfTrue));
        }
        listRewrite.insertBefore(rhs, parentProbeNode, null);

        IfStatement ifStmt = createIfStatement(condVarName, evalIfTrue);
        listRewrite.insertBefore(ifStmt, parentProbeNode, null);
        Block block = (Block)ifStmt.getThenStatement();

        listRewrite = rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
        pLoc = new ProbeLocation(block, ProbeLocation.INSERT_LAST);
        rightOperand.setLocation(pLoc);
        injectProbe(rightOperand, MODE_SHORT_CIRCUIT);

        //Add probes for extended operands.
        condVarName = rightOperand.getName();
        for(Probe operand : extOperands) {
            //Add a VDS before the outermost if statement.
            Statement ext = null;
            if(mode == MODE_ASSIGN_ONLY) {
                ext = createAssignment(operand, bool, ast.newBooleanLiteral(evalIfTrue));
            } else {
                bool = ast.newPrimitiveType(PrimitiveType.BOOLEAN);
                ext = createVariableDeclaration(operand, bool, ast.newBooleanLiteral(evalIfTrue));
            }
            savedListRewrite.insertBefore(ext, ifStmt, null);

            //Then add an if statement to the current block.
            IfStatement newIfStmt = createIfStatement(condVarName, evalIfTrue);
            listRewrite.insertLast(newIfStmt, null);
            block = (Block)newIfStmt.getThenStatement();

            listRewrite = rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
            pLoc = new ProbeLocation(block, ProbeLocation.INSERT_LAST);
            operand.setLocation(pLoc);
            injectProbe(operand, MODE_SHORT_CIRCUIT);
            condVarName = operand.getName();
        }

        //Restore listRewrite.
        listRewrite = savedListRewrite;
    }

    private void injectProbeForConditionalExpr(Probe p, int mode) {
        Probe condExpr = null;
        Probe thenExpr = null;
        Probe elseExpr = null;
        for(Probe child : p.getChildren()) {
            StructuralPropertyDescriptor desc = child.getPath().getLocation();
            if(desc.equals(ConditionalExpression.THEN_EXPRESSION_PROPERTY)) {
                thenExpr = child;
            } else if(desc.equals(ConditionalExpression.ELSE_EXPRESSION_PROPERTY)) {
                elseExpr = child;
            } else if(desc.equals(ConditionalExpression.EXPRESSION_PROPERTY)) {
                condExpr = child;
            }
        }
        if(condExpr != null && (thenExpr != null || elseExpr != null))
            injectProbeForConditionalExpr(condExpr, thenExpr, elseExpr, mode);
        else
            injectChildProbes(p, mode);
    }

    private void injectProbeForConditionalExpr(Probe condExpr, Probe thenExpr, Probe elseExpr, int mode) {
        ListRewrite savedListRewrite = listRewrite;

        //Insert the condition before the parent.
        ASTNode parentProbeNode = condExpr.getParent().getProbeNode();
        ProbeLocation pLoc = new ProbeLocation(parentProbeNode, ProbeLocation.INSERT_BEFORE);
        condExpr.setLocation(pLoc);
        injectProbe(condExpr, mode);

        //Add VDSs for thenExpr and elseExpr.
        if(thenExpr != null) {
            Type type = createTypeNode(thenExpr.getTarget());
            Expression initValue = createDefaultLiteral(type);
            VariableDeclarationStatement vds = createVariableDeclaration(thenExpr, type, initValue);
            listRewrite.insertBefore(vds, parentProbeNode, null);
        }

        if(elseExpr != null) {
            Type type = createTypeNode(elseExpr.getTarget());
            Expression initValue = createDefaultLiteral(type);
            VariableDeclarationStatement vds = createVariableDeclaration(elseExpr, type, initValue);
            listRewrite.insertBefore(vds, parentProbeNode, null);
        }

        //Add an if statement to inject child probes.
        IfStatement ifStmt = null;
        if(thenExpr == null && elseExpr != null) {
            ifStmt = createIfStatement(condExpr.getName(), false);
        } else {
            ifStmt = createIfStatement(condExpr.getName(), true);
        }
        Block thenBlock = (Block)ifStmt.getThenStatement();
        Block elseBlock = null;

        if(thenExpr != null && elseExpr != null) {
            elseBlock = ast.newBlock();
            ifStmt.setElseStatement(elseBlock);
        }
        listRewrite.insertBefore(ifStmt, parentProbeNode, null);

        //Add child probes of thenExpr / elseExpr to an appropriate block.
        if(thenExpr != null) {
            listRewrite = rewrite.getListRewrite(thenBlock, Block.STATEMENTS_PROPERTY);
            pLoc = new ProbeLocation(thenBlock, ProbeLocation.INSERT_LAST);
            thenExpr.setLocation(pLoc);
            injectProbe(thenExpr, MODE_SHORT_CIRCUIT);
        }

        if(elseExpr != null) {
            Block block = thenExpr == null ? thenBlock : elseBlock;
            listRewrite = rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
            pLoc = new ProbeLocation(block, ProbeLocation.INSERT_LAST);
            elseExpr.setLocation(pLoc);
            injectProbe(elseExpr, MODE_SHORT_CIRCUIT);
        }

        //Restore listRewrite and newChildLoc;
        listRewrite = savedListRewrite;
    }

    private void injectProbeForLambdaExpr(Probe p, int mode) {
        LambdaExpression lambda = (LambdaExpression)p.getTargetInProbe();
        ProbeLocation pLoc = null;
        if(lambda.getBody() instanceof Expression body) {
            //For an expression body, create a block for it.
            ReturnStatement returnStmt = ast.newReturnStatement();
            Expression newExpr = (Expression)ASTNode.copySubtree(ast, body);
            returnStmt.setExpression(newExpr);
            Block block = ast.newBlock();
            block.statements().add(returnStmt);
            rewrite.replace(lambda.getBody(), block, null);
            listRewrite = rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);

            p.setProbeNode(returnStmt, returnStmt); //update to new copied body.
            for(Probe child : p.getChildren()) {
                TreePath targetPath = CodeUtils.findTreePath(child.getTarget(), (LambdaExpression)p.getTarget());
                //If the target is the body itself, it should be matched to return's expression.
                if(targetPath.getLocation().equals(LambdaExpression.BODY_PROPERTY)) {
                    targetPath.setLocation(ReturnStatement.EXPRESSION_PROPERTY);
                }
                child.setPath(targetPath);
                pLoc = new ProbeLocation(returnStmt, ProbeLocation.INSERT_BEFORE);
                child.setLocation(pLoc);
                injectProbe(child, mode);
            }
        } else {
            //Need to find new locations for the new lambda body.
            for(Probe child : p.getChildren()) {
                LambdaExpression orgLambda = (LambdaExpression)p.getTarget();
                updateChildProbe(child, orgLambda.getBody(), lambda.getBody());
                injectProbe(child, mode);
            }
        }
    }

    private IfStatement createIfStatement(String condVarName, boolean evalIfTrue) {
        IfStatement ifStmt = ast.newIfStatement();
        if(evalIfTrue) {
            ifStmt.setExpression(ast.newSimpleName(condVarName));
        } else {
            PrefixExpression prefix = ast.newPrefixExpression();
            prefix.setOperand(ast.newSimpleName(condVarName));
            prefix.setOperator(PrefixExpression.Operator.NOT);
            ifStmt.setExpression(prefix);
        }
        Block block = ast.newBlock();
        ifStmt.setThenStatement(block);
        return ifStmt;
    }

    private Expression createCastExpression(Type type, Expression initValue) {
        CastExpression castExpr = ast.newCastExpression();
        Type t = (Type)ASTNode.copySubtree(ast, type);
        castExpr.setType(t);
        ParenthesizedExpression expr = ast.newParenthesizedExpression();
        expr.setExpression(initValue);
        castExpr.setExpression(expr);
        return castExpr;
    }

    private <T> VariableDeclarationFragment createVariableDeclarationFragment(Probe p, Type type, Expression initValue) {
        ITypeBinding tb = p.getTarget().resolveTypeBinding();
        if(isCastRequired(tb, p.getTarget())) {
            initValue = createCastExpression(type, initValue);
        }
        VariableDeclarationFragment vdf = ast.newVariableDeclarationFragment();
        SimpleName name = ast.newSimpleName(p.getName());
        vdf.setName(name);
        vdf.setInitializer(initValue);
        return vdf;
    }

    private FieldDeclaration createFieldDeclaration(Probe p, Type type, Expression initValue) {
        VariableDeclarationFragment vdf = createVariableDeclarationFragment(p, type, initValue);
        FieldDeclaration fd = ast.newFieldDeclaration(vdf);
        fd.setType(type);
        FieldDeclaration orgFD = (FieldDeclaration)p.getLocation().getLocNode();
        fd.modifiers().addAll(ast.newModifiers(orgFD.getModifiers()));
        return fd;
    }

    private VariableDeclarationStatement createVariableDeclaration(Probe p, Type type, Expression initValue) {
        VariableDeclarationFragment vdf = createVariableDeclarationFragment(p, type, initValue);
        VariableDeclarationStatement vds = ast.newVariableDeclarationStatement(vdf);
        vds.setType(type);
        return vds;
    }

    private ExpressionStatement createAssignment(Probe p, Type type, Expression initValue) {
        ITypeBinding tb = p.getTarget().resolveTypeBinding();
        if(isCastRequired(tb, p.getTarget())) {
            initValue = createCastExpression(type, initValue);
        }
        Assignment assign = ast.newAssignment();
        SimpleName name = ast.newSimpleName(p.getName());
        assign.setLeftHandSide(name);
        assign.setRightHandSide(initValue);
        ExpressionStatement es = ast.newExpressionStatement(assign);
        return es;
    }

    private boolean isCastRequired(ITypeBinding tb, Expression expr) {
        if(expr instanceof MethodInvocation invoc) {
            IMethodBinding mb = invoc.resolveMethodBinding();
            ITypeBinding ret = mb != null ? mb.getMethodDeclaration().getReturnType() : null;
            return isCastRequired(ret, tb);
        }
        return false;
    }

    private boolean isCastRequired(ITypeBinding src, ITypeBinding dst) {
        if(src != null && dst != null
            && src.isParameterizedType() && dst.isParameterizedType()
            && src.getTypeArguments().length == dst.getTypeArguments().length) {
            for(int i=0; i < src.getTypeArguments().length; i++) {
                ITypeBinding srcArg = src.getTypeArguments()[i];
                ITypeBinding dstArg = dst.getTypeArguments()[i];
                if(srcArg.isParameterizedType() && dstArg.isParameterizedType()) {
                    return isCastRequired(srcArg, dstArg);
                } else if(srcArg.isTypeVariable() && dstArg.isTypeVariable()) {
                    ITypeBinding[] srcBounds = srcArg.getTypeBounds();
                    ITypeBinding[] dstBounds = dstArg.getTypeBounds();
                    if(srcBounds.length > 0 && dstBounds.length == 0) {
                        return true;
                    } else if(srcBounds.length > 0 && dstBounds.length > 0) {
                        //TODO: For more safe assignment, compatibility of type bounds should be checked.
                        return false;
                    }
                }
            }
        }
        return false;
    }

    private boolean isBooleanExpression(Expression target) {
        if(target instanceof InfixExpression infix &&
            (infix.getOperator().equals(InfixExpression.Operator.CONDITIONAL_AND)
                || infix.getOperator().equals(InfixExpression.Operator.CONDITIONAL_OR))) {
            return true;
        }
        return false;
    }

    private void replaceWithProbeName(String probeName, TreePath path, ASTNode newNode) {
        TreePath curr = path;
        ASTNode newParent = newNode;
        while(curr.getChild() != null
            && curr.getChild().getLocation() != null) {
            newParent = curr.getNode(newParent);
            curr = curr.getChild();
        }
        Name name = ast.newName(probeName);
        curr.setNode(name, newParent);
    }

    private void replaceTargetWithName(Probe p) {
        SimpleName name = ast.newSimpleName(p.getName());
        CodeUtils.replaceNodeWithRewrite(p.getTarget(), name, rewrite);
    }

    private String rewriteSourceCode(Path outFilePath) {
        String newSource = null;
        try {
            if(missingClasses.size() > 0) {
                addImportDeclarations();
            }
            TextEdit edits = rewrite.rewriteAST(doc, JavaCore.getDefaultOptions());
            edits.apply(doc);
            newSource = doc.get();
            Files.writeString(outFilePath, newSource);
            matcher.computeLineMapping(CodeUtils.getCompilationUnit(newSource));
        } catch (MalformedTreeException | BadLocationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to rewrite code to " + outFilePath.toString());
        }
        return newSource;
    }

    private void addImportDeclarations() {
        AST ast = cu.getAST();
        ListRewrite listRewrite = rewrite.getListRewrite(cu, CompilationUnit.IMPORTS_PROPERTY);

        for(String missing : missingClasses) {
            ImportDeclaration importDeclaration = ast.newImportDeclaration();
            importDeclaration.setName(ast.newName(missing));
            listRewrite.insertLast(importDeclaration, null);
        }
    }

    private String getImportedClasses() {
        List<String> packageNames = new ArrayList<>();
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(ImportDeclaration node) {
                importedClasses.add(node.getName().getFullyQualifiedName());
                return false;
            }

            @Override
            public boolean visit(PackageDeclaration node) {
                packageNames.add(node.getName().toString());
                return false;
            }
        });

        return packageNames.size() > 0 ? packageNames.get(0) : null;
    }

    private VariableDeclarationStatement addMarker(ListRewrite markerRewrite, ASTNode loc, String marker,
        Map<String, SimpleEntry<VariableDeclarationStatement, ListRewrite>> map) {
        ProbeLocation pLoc = new ProbeLocation(loc, ProbeLocation.INSERT_BEFORE);
        return addMarker(markerRewrite, pLoc, marker, map);
    }

    private VariableDeclarationStatement addMarker(ListRewrite markerRewrite, ProbeLocation pLoc, String marker,
        Map<String, SimpleEntry<VariableDeclarationStatement, ListRewrite>> map) {
        int startLine = pLoc.getStartLine(cu);
        int endLine = pLoc.getEndLine(cu);
        String markerName;
        if(pLoc.isInsertLast()) {
            //For INSERT_LAST case, use the line after the end line is the actual location.
            endLine = endLine + 1;
            markerName = marker + endLine;
        } else {
            markerName = marker + startLine;
        }
        return addMarker(markerRewrite, pLoc, markerName, startLine, endLine, map);
    }

    private VariableDeclarationStatement addMarker(ListRewrite markerRewrite, ProbeLocation pLoc, String markerName,
        int startLine, int endLine, Map<String, SimpleEntry<VariableDeclarationStatement, ListRewrite>> map) {
        if(map.containsKey(markerName)) {
            if(markerName.startsWith(MARKER_START)) {
                //Ignore duplicate start markers and return the existing one.
                return map.get(markerName).getKey();
            } else {
                //Remove duplicate end markers.
                ASTNode oldMarker = map.get(markerName).getKey();
                ListRewrite oldMarkerRewrite = map.get(markerName).getValue();
                oldMarkerRewrite.remove(oldMarker, null);
            }
        }
        SimpleName name = ast.newSimpleName(markerName);
        VariableDeclarationFragment vdf = ast.newVariableDeclarationFragment();
        vdf.setName(name);
        vdf.setInitializer(ast.newNumberLiteral(String.valueOf(endLine)));
        VariableDeclarationStatement vds = ast.newVariableDeclarationStatement(vdf);
        PrimitiveType intType = ast.newPrimitiveType(PrimitiveType.INT);
        vds.setType(intType);

        pLoc.insert(vds, markerRewrite);
        map.put(markerName, new SimpleEntry<>(vds, markerRewrite));
        return vds;
    }

    private Type createTypeNode(Expression expr) {
        ITypeBinding tb = null;
        //For variables, need to use the variable's type binding.
        if(expr instanceof Name name) {
            IBinding binding = name.resolveBinding();
            if(binding instanceof IVariableBinding vb)
                tb = vb.getType();
        } else {
            tb = expr.resolveTypeBinding();
        }
        //For wildcard type, need to use it's bound, or Object if there is none.
        if(tb != null) {
            if(expr.getParent() instanceof VariableDeclarationFragment vdf){
                //For an initializer of VDF, use VDF's type.
                if(expr.equals(vdf.getInitializer())) {
                    ASTNode parent = vdf.getParent();
                    Type t = null;
                    if(parent instanceof VariableDeclarationStatement vds) {
                        t = vds.getType();
                    } else if(parent instanceof VariableDeclarationExpression vde) {
                        t = vde.getType();
                    }
                    if(vdf.extraDimensions().size() > 0)
                        t = createArrayType(t, vdf.extraDimensions());
                    if(t != null)
                        return (Type)ASTNode.copySubtree(ast, t);
                }
            } else if(tb.isCapture() || tb.isWildcardType()) {
                ITypeBinding[] bounds = tb.getTypeBounds();
                tb = bounds.length > 0 ? bounds[0] : null;
            }
            return CodeUtils.getTypeNode(ast, tb, packageName, importedClasses, missingClasses);
        } else {
            return ast.newSimpleType(ast.newName("Object"));
        }
    }

    private ArrayType createArrayType(Type t, List<Dimension> dimensions) {
        ArrayType arrayType = ast.newArrayType((Type)ASTNode.copySubtree(ast, t));
        arrayType.dimensions().clear();
        for(Dimension dimension : dimensions)
            arrayType.dimensions().add(ASTNode.copySubtree(ast, dimension));
        return arrayType;
    }

    private Expression createDefaultLiteral(Type type) {
        return createDefaultLiteral(type, false);
    }

    private Expression createDefaultLiteral(Type type, boolean defaultBoolean) {
        if(type != null && type instanceof PrimitiveType primitive) {
            PrimitiveType.Code code = primitive.getPrimitiveTypeCode();
            if(code.equals(PrimitiveType.BOOLEAN)) {
                return ast.newBooleanLiteral(defaultBoolean);
            } else if(code.equals(PrimitiveType.CHAR)) {
                CharacterLiteral literal = ast.newCharacterLiteral();
                literal.setCharValue(' ');
                return literal;
            } else if(code.equals(PrimitiveType.DOUBLE) || code.equals(PrimitiveType.FLOAT)) {
                return ast.newNumberLiteral("0.0");
            } else {
                return ast.newNumberLiteral("0");
            }
        }
        return ast.newNullLiteral();
    }

    public LineMatcher getLineMatcher() {
        return matcher;
    }

    private class LocationInfo {
        private Statement loc;
        private ListRewrite listRewrite;
        private TreePath path;
        private List<ExpressionStatement> statements;

        public LocationInfo(Statement loc, ListRewrite listRewrite, TreePath path,
                List<ExpressionStatement> statements) {
            this.loc = loc;
            this.listRewrite = listRewrite;
            this.path = path;
            this.statements = statements;
        }

        public LocationInfo(Statement loc, ListRewrite listRewrite, TreePath path) {
            this(loc, listRewrite, path, null);
        }

        public LocationInfo(Statement loc, ListRewrite listRewrite) {
            this(loc, listRewrite, null, new ArrayList<>());
        }

        public Statement getLoc() {
            return loc;
        }

        public ListRewrite getListRewrite() {
            return listRewrite;
        }

        public TreePath getPath() {
            return path;
        }

        public List<ExpressionStatement> statements() {
            if(statements == null)
                statements = new ArrayList<>();
            return statements;
        }
    }
}