package logicfl.probe;

import java.util.TreeMap;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class LineMatcher {
    private static final int MARKER_TYPE_START = 0;
    private static final int MARKER_TYPE_END = 1;
    private TreeMap<Integer, ProbeRange> probeRanges;

    public LineMatcher() {
        probeRanges = new TreeMap<>();
    }

    public void computeLineMapping(CompilationUnit cu) {
        cu.accept(new ASTVisitor() {
            boolean checkNextNode = false;
            ProbeRange probeRange = null;

            @Override
            public void preVisit(ASTNode node) {
                super.preVisit(node);
                if(checkNextNode) {
                    int endLine = cu.getLineNumber(node.getStartPosition()+node.getLength());
                    if(probeRange != null) {
                        probeRange.setProbedLineEnd(endLine);
                    }
                    checkNextNode = false;
                }
            }

            @Override
            public boolean visit(VariableDeclarationStatement node) {
                if(!node.fragments().isEmpty()) {
                    VariableDeclarationFragment vdf = (VariableDeclarationFragment) node.fragments().get(0);
                    String marker = vdf.getName().getIdentifier();
                    if(marker.startsWith(ProbeInjector.MARKER_START)) {
                        updateLineMapWithMarker(vdf, marker, MARKER_TYPE_START);
                        return false;
                    } else if(marker.startsWith(ProbeInjector.MARKER_END)) {
                        updateLineMapWithMarker(vdf, marker, MARKER_TYPE_END);
                        checkNextNode = true;
                        return false;
                    }
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(VariableDeclarationExpression node) {
                if(!node.fragments().isEmpty()) {
                    VariableDeclarationFragment vdf = (VariableDeclarationFragment) node.fragments().get(0);
                    String marker = vdf.getName().getIdentifier();
                    if(marker.startsWith(ProbeInjector.MARKER_START)) {
                        updateLineMapWithMarker(vdf, marker, MARKER_TYPE_START);
                        return false;
                    } else if(marker.startsWith(ProbeInjector.MARKER_END)) {
                        updateLineMapWithMarker(vdf, marker, MARKER_TYPE_END);
                        checkNextNode = true;
                        return false;
                    }
                }
                return super.visit(node);
            }

            private ProbeRange updateLineMapWithMarker(VariableDeclarationFragment vdf, String marker, int markerType) {
                if(markerType == MARKER_TYPE_START) {
                    int newLineNum = cu.getLineNumber(vdf.getStartPosition());
                    int orgLineNum = Integer.parseInt(marker.substring(marker.lastIndexOf('_')+1));
                    probeRange = new ProbeRange(newLineNum, orgLineNum);
                    probeRanges.put(newLineNum, probeRange);
                } else if(markerType == MARKER_TYPE_END) {
                    int newLineNum = cu.getLineNumber(vdf.getStartPosition());
                    int orgLineStart = Integer.parseInt(marker.substring(marker.lastIndexOf('_')+1));
                    int orgLineEnd = Integer.parseInt(vdf.getInitializer().toString());
                    if(probeRange != null && probeRange.getOrgLineStart() == orgLineStart) {
                        probeRange.setEndLine(newLineNum + 1);
                        probeRange.setOrgLineEnd(orgLineEnd);
                    } else {
                        throw new RuntimeException("Cannot find matching probe start! - "+marker);
                    }
                }
                return null;
            }
        });
    }

    public int getOriginalLine(int lineNum) {
        Integer floorKey = probeRanges.floorKey(lineNum);
        //No floor key means there is no adjustment.
        if(floorKey == null)
            return lineNum;

        ProbeRange probeRange = probeRanges.get(floorKey);
        //lineNum in range means it's a probe - returns orgLine.
        if(probeRange.inRange(lineNum)) {
            return probeRange.getOrgLineStart();
        } else if(lineNum <= probeRange.getProbedLineEnd()
            && probeRange.hasDifferentLines()) {
            //If lineNum belongs to the lines replaced with probe names,
            //Get the offset from the orgStartLine.
            int offset = lineNum - (probeRange.getEndLine()+1);
            return probeRange.getOrgLineStart() + offset;
        }
        //Otherwise, it's after the probeRange, but before another probe range.
        //In this case, apply the current offset.
        return lineNum - probeRange.getOffset();
    }

    public TreeMap<Integer, ProbeRange> getProbeRanges() {
        return probeRanges;
    }

    public void addProbeRange(ProbeRange range) {
        probeRanges.put(range.getStartLine(), range);
    }
}
