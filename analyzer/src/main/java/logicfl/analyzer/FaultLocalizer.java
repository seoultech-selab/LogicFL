package logicfl.analyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jpl7.Atom;
import org.jpl7.Query;
import org.jpl7.Term;

import logicfl.coverage.NPETrace;
import logicfl.coverage.StackTrace;
import logicfl.logic.codefacts.Line;
import logicfl.utils.Configuration;
import logicfl.utils.JSONUtils;
import logicfl.utils.Timer;

public class FaultLocalizer {
    private Configuration config;
    private List<StackTrace> candidates;

    public FaultLocalizer(String configFilePath) {
        config = new Configuration(configFilePath);
        candidates = new ArrayList<>();
    }

    public List<StackTrace> getCandidates() {
        return this.candidates;
    }

    public static void main(String[] args) {
        String configFilePath = "config.properties";
        if(args.length > 0) {
            configFilePath = args[0];
        }
        System.out.println("Getting configurations from "+configFilePath);

        FaultLocalizer localizer = new FaultLocalizer(configFilePath);
        localizer.run();
    }

    public void loadTracesFromJSON() {
        List<NPETrace> traces = JSONUtils.loadTracesFromJSON(config.npeInfoPath);
        for(NPETrace trace : traces) {
            // Consider the first target stack trace entry as candidates.
            for (StackTrace st : trace.traces) {
                if (st.isTarget) {
                    candidates.add(st);
                    break;
                }
            }
        }
    }

    public void run() {
        Timer timer = new Timer("fault_localizer");
        timer.setStart();
        System.out.println("Running Fault Localizer with");
        System.out.println("Rules: " + config.rulesPath);
        System.out.println("Facts: " + config.flFactsPath);
        System.out.println("Code Facts: " + config.codeFactsPath);
        Query main = new Query("consult",
            Term.termArrayToList(
                new Term[]{
                    new Atom(config.flFactsPath.toString()),
                    new Atom(config.codeFactsPath.toString()),
                    new Atom(config.rulesPath.toString())
                }));
        System.out.println("consult "+(main.hasSolution() ? "Succeeded" : "Failed"));

        StringBuffer sb = new StringBuffer("Fault Localization Results");
        StringBuffer sb2 = new StringBuffer();
        //find_npe_cause(Expr, Line, Cause, Loc)
        String textQuery = "find_npe_cause(Expr, Line, Cause, Loc)";
        Query q = new Query(Term.textToTerm(textQuery));
        Map<String, Term>[] solutions = q.allSolutions();
        for(Map<String, Term> sol : solutions) {
            String expr = sol.get("Expr").toString();
            String line = sol.get("Line").toString();
            String cause = sol.get("Cause").toString();
            String loc = sol.get("Loc").toString();
            String exprCode = getCode(expr);
                String causeCode = getCode(cause);
                System.out.printf("NPE at %s / Null Expression %s[%s]\n\tcan be caused by \n%s[%s] at %s.\n\n",
                    line, expr, exprCode, cause, causeCode, loc);
                sb.append("\n");
                sb.append(String.join("", "NPE at ", line, " / Null Expression - ", expr, "[", exprCode, "]"));
                sb.append("\n\t can be caused by \n");
                sb.append(String.join("", cause, "[", causeCode, "] - ", loc, ".\n"));
                Line faultyLine = new Line(loc);
                Query classQuery = new Query(Term.textToTerm("class(" + faultyLine.getClassId() +", ClassName)"));
                if(classQuery.hasSolution()) {
                    String className = classQuery.oneSolution().get("ClassName").toString();
                    className = className.substring(1, className.length()-1);
                    sb2.append(className);
                    sb2.append(" ");
                    sb2.append(faultyLine.getLineNum());
                    sb2.append("\n");
                }
        }
        System.out.println("Total " + solutions.length + " Identified Fault Locations for NPE.");

        try {
            Files.writeString(config.rootCausePath, sb.toString());
            String result = sb2.toString();
            if(result.length() > 0)
                Files.writeString(config.faultyLinesPath, result.substring(0, result.length()-1));
        } catch (IOException e) {
            e.printStackTrace();
        }
        timer.setEnd();
        JSONUtils.exportExecutionTime(timer, config.execTimePath);
        System.out.println("Exec. Time - " + timer.getExecTimeStr());
    }

    private String getCode(String expr) {
        Query q = new Query("expr(" + expr +", Code)");
        if(q.hasSolution()) {
            return q.oneSolution().get("Code").toString();
        }
        return expr;
    }
}
