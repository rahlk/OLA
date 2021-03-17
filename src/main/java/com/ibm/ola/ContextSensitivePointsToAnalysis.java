package com.ibm.ola;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import org.junit.jupiter.api.Test;
import soot.*;
import soot.jimple.JimpleBody;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.Edge;
import soot.tagkit.LineNumberTag;
import soot.tagkit.Tag;
import vasco.CallSite;
import vasco.Context;
import vasco.ContextTransitionTable;
import vasco.callgraph.CallGraphTransformer;
import vasco.callgraph.PointsToAnalysis;
import vasco.callgraph.PointsToGraph;

public class ContextSensitivePointsToAnalysis {

    private static String outputDirectory;

    public static void main(String[] args) {
        outputDirectory = ".";
        String classPath = System.getProperty("java.class.path");
        String mainClass = null;
        int callChainDepth = 10;

        /* ------------------- OPTIONS ---------------------- */
        try {
            int i=0;
            label:
            while(true){
                switch (args[i]) {
                    case "-cp":
                        classPath = args[i + 1];
                        i += 2;
                        break;
                    case "-out":
                        outputDirectory = args[i + 1];
                        File outDirFile = new File(outputDirectory);
                        if (!outDirFile.exists() && !outDirFile.mkdirs()) {
                            throw new IOException("Could not make output directory: " + outputDirectory);
                        }
                        i += 2;
                        break;
                    case "-k":
                        callChainDepth = Integer.parseInt(args[i + 1]);
                        i += 2;
                        break;
                    default:
                        mainClass = args[i];
                        i++;
                        break label;
                }
            }
            if (i != args.length || mainClass == null)
                throw new Exception();
        } catch (Exception e) {
            System.out.println("Usage: java vasco.callgraph.CallGraphTest [-cp CLASSPATH] [-out DIR] [-k DEPTH] MAIN_CLASS");
            System.exit(1);
        }

        /* ------------------- SOOT OPTIONS ---------------------- */
        String[] sootArgs = {
                "-cp", classPath, "-pp",
                "-w", "-app",
                "-keep-line-number",
                "-keep-bytecode-offset",
                "-p", "cg", "implicit-entry:false",
                "-p", "cg.spark", "enabled",
                "-p", "cg.spark", "simulate-natives",
                "-p", "cg", "safe-forname",
                "-p", "cg", "safe-newinstance",
                "-main-class", mainClass,
                "-f", "none", mainClass
        };


        /* ------------------- ANALYSIS ---------------------- */
        CallGraphTransformer cgt = new CallGraphTransformer();
//        PackManager.v().getPack("wjtp").add(new Transform("wjtp.fcpa", cgt));
        G.reset();
        soot.Main.main(sootArgs);
//        PointsToAnalysis pointsToAnalysis = cgt.getPointsToAnalysis();
//
//

        SootClass c = Scene.v().loadClassAndSupport("com.ibm.example.irs.BusinessProcess");
        c.setApplicationClass();
        Scene.v().setMainClass(c);

        Scene.v().addBasicClass("com.ibm.example.irs.BusinessProcess", SootClass.BODIES);
        soot.Scene.v().loadNecessaryClasses();
        soot.Scene.v().setEntryPoints(EntryPoints.v().all());
        Map<Integer, Value> l = getLocals(c, "test_2", "IRS");

//        /* ------------------- LOGGING ---------------------- */
//        try {
//            printCallSiteStats(pointsToAnalysis);
//            printMethodStats(pointsToAnalysis);
//            dumpCallChainStats(pointsToAnalysis, callChainDepth);
//        } catch (FileNotFoundException e1) {
//            System.err.println("Oops! Could not create log file: " + e1.getMessage());
//            System.exit(1);
//        }

    }
    private static SootField getField(String classname, String fieldname) {
        Collection app = Scene.v().getApplicationClasses();
        for (Object o : app) {
            SootClass sc = (SootClass) o;
            if (sc.getName().equals(classname))
                return sc.getFieldByName(fieldname);
        }
        throw new RuntimeException("Field "+fieldname+" was not found in class "+classname);
    }

    private static Map<Integer,Value> getLocals(SootClass sc, String methodname, String typename) {
        HashMap<Integer,Value> res = new HashMap<>();
        for (SootMethod sm : sc.getMethods()) {
            System.err.println(sm.getName() + " is_phantom");
            try{
                Body bd = sm.retrieveActiveBody();
                JimpleBody jb = (JimpleBody) sm.getActiveBody();
                System.out.println(bd.toString());
            } catch (Exception e) {
                System.err.println(e.toString());
            }
//                for (Unit unit : jb.getUnits()) {
//                    Stmt s = (Stmt) unit;
//                    int line = getLineNumber(s);
//                    // find definitions
//                    for (Object o : s.getDefBoxes()) {
//                        if (o instanceof ValueBox) {
//                            Value v = ((ValueBox) o).getValue();
//                            if (v.getType().toString().equals(typename) && v instanceof Local)
//                                res.put(line, v);
//                        }
//                    }
//                }
            }
//        }

        return res;
    }

    private static int getLineNumber(Stmt s) {
        for (Object o : s.getTags()) {
            if (o instanceof LineNumberTag)
                return Integer.parseInt(o.toString());
        }
        return -1;
    }

    public static List<SootMethod> getSparkExplicitEdges(Unit callStmt) {
        Iterator<Edge> edges = Scene.v().getCallGraph().edgesOutOf(callStmt);
        List<SootMethod> targets = new LinkedList<SootMethod>();
        while (edges.hasNext()) {
            Edge edge = edges.next();
            if (edge.isExplicit()) {
                targets.add(edge.tgt());
            }
        }
        return targets;
    }

    public static List<SootMethod> getSparkExplicitEdges(SootMethod sootMethod) {
        Iterator<Edge> edges = Scene.v().getCallGraph().edgesOutOf(sootMethod);
        List<SootMethod> targets = new LinkedList<SootMethod>();
        while (edges.hasNext()) {
            Edge edge = edges.next();
            if (edge.isExplicit()) {
                targets.add(edge.tgt());
            }
        }
        return targets;
    }


    private static final Set<SootMethod> dirtyMethods = new HashSet<SootMethod>();

    private static void markDirty(Unit defaultSite) {
        List<SootMethod> methods = getSparkExplicitEdges(defaultSite);
        while (!methods.isEmpty()) {
            SootMethod method = methods.remove(0);
            if (!dirtyMethods.contains(method)) {
                dirtyMethods.add(method);
                methods.addAll(getSparkExplicitEdges(method));
            }
        }
    }

    public static void printCallSiteStats(PointsToAnalysis pta) throws FileNotFoundException {
        // Get context-transition table
        ContextTransitionTable<SootMethod,Unit, PointsToGraph> ctt = pta.getContextTransitionTable();
        Map<Context<SootMethod,Unit,PointsToGraph>,Set<CallSite<SootMethod,Unit,PointsToGraph>>> callSitesWithinContexts = ctt.getCallSitesOfContexts();
        Map<CallSite<SootMethod,Unit,PointsToGraph>,Map<SootMethod,Context<SootMethod,Unit,PointsToGraph>>> transitions = ctt.getTransitions();
        Set<CallSite<SootMethod,Unit,PointsToGraph>> defaultCallSites = ctt.getDefaultCallSites();

        // Initialise output stream
        PrintWriter csv = new PrintWriter(outputDirectory + "/sites.csv");
        csv.println("FcpaEdges, SparkEdges, Context, CallSite");

        // The visited set
        Set<Context<SootMethod,Unit,PointsToGraph>> visited = new HashSet<Context<SootMethod,Unit,PointsToGraph>>();

        // Maintain a stack of contexts to process
        Stack<Context<SootMethod,Unit,PointsToGraph>> stack = new Stack<Context<SootMethod,Unit,PointsToGraph>>();

        // Initialise it with the main context
        Context<SootMethod,Unit,PointsToGraph> source = pta.getContexts(Scene.v().getMainMethod()).get(0);
        stack.push(source);

        // Now recursively (using stacks) mark reachable contexts
        while (!stack.isEmpty()) {
            // Get the next item to process
            source = stack.pop();
            // Add successors
            if (callSitesWithinContexts.containsKey(source)) {
                // The above check is there because methods with no calls have no entry
                for (CallSite<SootMethod,Unit,PointsToGraph> callSite : callSitesWithinContexts.get(source)) {

                    // My edges are -1 for "default" sites, and whatever the CTT has otherwise
//                    int myEdges = defaultCallSites.contains(callSite) ? -1 : transitions.get(callSite).size();
                    // Get SPARK's edges from the Soot call graph
//                    int sparkEdges = getSparkExplicitEdges(callSite.getCallNode()).size();

//                    // Log this
//                    csv.println(myEdges + ", " + sparkEdges + ", " + source + ", " +
//                            "\"" + callSite.getCallNode() + "\"");

//                    if (myEdges > 0) {
//                        for (SootMethod method : transitions.get(callSite).keySet()) {
//                            Context<SootMethod,Unit,PointsToGraph> target = transitions.get(callSite).get(method);
//                            // Don't process the same element twice
//                            if (!visited.contains(target)) {
//                                // Mark reachable
//                                visited.add(target);
//                                // Add it's successors also later
//                                stack.push(target);
//                            }
//                        }
//                    } else if (myEdges == -1) {
//                        // Default call-site, so mark reachable closure as "dirty"
//                        markDirty(callSite.getCallNode());
//                    }
                }
            }

        }
        // Close the CSV file
        csv.close();
    }

    public static void printMethodStats(PointsToAnalysis pta) throws FileNotFoundException {
        // Initialise output stream
        PrintWriter csv = new PrintWriter(outputDirectory + "/methods.csv");
        csv.println("Method, Contexts, Application?, Dirty?");
        for (SootMethod method : pta.getMethods()) {
            csv.println("\"" + method + "\"" + ", " + pta.getContexts(method).size() +
                    ", " + (method.getDeclaringClass().isApplicationClass() ? 1 : 0) +
                    ", " + (dirtyMethods.contains(method) ? 1 : 0));
        }

        // Close the CSV file
        csv.close();
    }

    public static void dumpCallChainStats(PointsToAnalysis pta, int maxDepth) throws FileNotFoundException {
        // Initialise output stream
        PrintWriter txt = new PrintWriter(new FileOutputStream(outputDirectory + "/chains.txt"), true);
        Context<SootMethod,Unit,?> mainContext = pta.getContexts(Scene.v().getMainMethod()).get(0);
        SootMethod mainMethod = Scene.v().getMainMethod();

        txt.println("FCPA Chains");
        txt.println("------------");
        for(int k=1; k<=maxDepth; k++) {
            txt.println("k=" + k + ": " + countCallChains(pta, mainContext, k));
        }
        txt.println("Spark Chains");
        txt.println("------------");
        for(int k=1; k<=maxDepth; k++) {
            txt.println("k=" + k + ": " + countCallChains(mainMethod, k));
        }

        txt.close();

    }

    private static long countCallChains(SootMethod method, int k) {
        if (k == 0)
            return 1;

        long count = 1;
        Iterator<Edge> edges = Scene.v().getCallGraph().edgesOutOf(method);
        while(edges.hasNext()) {
            Edge edge = edges.next();
            if (edge.isExplicit()) {
                SootMethod target = edge.tgt();
                count = count + countCallChains(target, k-1);
            }
        }
        return count;
    }

    private static long countCallChains(PointsToAnalysis pta, Context<SootMethod,Unit,?> context, int k) {
        if (k == 0)
            return 1;

        long count = 1;
        ContextTransitionTable<SootMethod,Unit,?> ctt = pta.getContextTransitionTable();
        if (ctt.getCallSitesOfContexts().containsKey(context)) {
            for (CallSite<SootMethod,Unit,?> callSite : ctt.getCallSitesOfContexts().get(context)) {
                if (ctt.getDefaultCallSites().contains(callSite)) {
                    Iterator<Edge> edges = Scene.v().getCallGraph().edgesOutOf(callSite.getCallNode());
                    while(edges.hasNext()) {
                        SootMethod target = edges.next().tgt();
                        count = count + countCallChains(target, k-1);
                    }
                } else if (ctt.getTransitions().containsKey(callSite) && ctt.getTransitions().get(callSite) != null) {
                    for (Context<SootMethod,Unit,?> target : ctt.getTransitions().get(callSite).values()) {
                        if (!target.getMethod().getName().equals("<clinit>")) {
                            count = count + countCallChains(pta, target, k-1);
                        }
                    }
                }
            }
        }

        return count;

    }

    @Test
    public void testContextSensitivePointsToAnalysis() {
        // TODO: Compare output with an ideal (expected) output
        ContextSensitivePointsToAnalysis.main(new String[]{"-k", "-3", "-out", "target/test-results/CallGraphTestResults", "vasco.tests.CallGraphTestCase"});
    }

}
