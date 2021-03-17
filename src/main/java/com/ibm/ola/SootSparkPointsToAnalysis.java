package com.ibm.ola;

import soot.*;
import soot.jimple.JimpleBody;
import soot.jimple.Stmt;
import soot.jimple.internal.JIfStmt;
import soot.options.Options;
import soot.tagkit.LineNumberTag;
import soot.tagkit.Tag;
import vasco.callgraph.CallGraphTransformer;
import vasco.callgraph.PointsToAnalysis;
import vasco.callgraph.PointsToGraph;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SootSparkPointsToAnalysis {

    public static void main(String[] args) {
        String sootClassPath = "/Users/rkrsn/workspace/IRS/build/classes/java/main";
        String mainClass = "com.ibm.example.irs.BusinessProcess";

//        /* ------------------- OPTIONS ---------------------- */
//        try {
//            int i=0;
//            while (true) {
//                if ("-cp".equals(args[i])) {
//                    sootClassPath = args[i + 1];
//                    i += 2;
//                } else {
//                    mainClass = args[i];
//                    i++;
//                    break;
//                }
//            }
//            if (i != args.length || mainClass == null)
//                throw new Exception();
//        } catch (Exception e) {
//            System.out.println("Usage: java com.ibm.ola.SimpleSoot [-cp CLASSPATH] MAIN_CLASS");
//            System.exit(1);
//        }

        G.reset();
        Options.v().set_prepend_classpath(true);
        soot.options.Options.v().set_keep_line_number(true);
        soot.options.Options.v().set_whole_program(true);
        soot.options.Options.v().set_process_dir(Collections.singletonList(sootClassPath+"/com/ibm/example/irs/"));
        Options.v().setPhaseOption("cg.spark", "on");
        soot.options.Options.v().setPhaseOption("cg","verbose:true");
        Options.v().set_soot_classpath(sootClassPath);
        SootClass sc = Scene.v().loadClassAndSupport(mainClass);
        sc.setApplicationClass();
        soot.Scene.v().loadNecessaryClasses();
        soot.Scene.v().setEntryPoints(EntryPoints.v().all());

        /* Context sensitive with VASCO */

        /* Context sensitive with VASCO */
        CallGraphTransformer cgt = new CallGraphTransformer();
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.fcpa", cgt));
        PackManager.v().runPacks();

        Map<Integer, Value> ls = getLocals(sc,"test_2","com.ibm.example.irs.IRS");
        SootField f = getField("IRS","salaryList");

        printLocalIntersects(ls);
        printFieldIntersects(ls, f);

    }


    private static int getLineNumber(Stmt s) {
        int i = -1;
        for (Object o : s.getTags()) {
            if (o instanceof LineNumberTag)
                i = Integer.parseInt(o.toString());
        }
        return i;
    }

    private static SootField getField(String className, String fieldName) {
        Collection<SootClass> app = Scene.v().getApplicationClasses();
        System.out.println(app);
        for (SootClass sc : app) {
            if (sc.getName().equals(className))
                if (sc.getName().equals(className))
                    return sc.getFieldByName(fieldName);
        }
        throw new RuntimeException("Field "+fieldName+" was not found in class "+className);
    }

    private static Map<Integer,Value> getLocals(SootClass sc, String methodName, String typeName) {
        HashMap<Integer,Value> res = new HashMap<>();
        for (SootMethod sm : sc.getMethods()) {
//            System.err.println(sm.getName()+" "+sm.isPhantom());
            if (sm.getName().equals(methodName) && sm.isConcrete()) {
                JimpleBody jb = (JimpleBody) sm.retrieveActiveBody();
//                System.out.println(jb.toString());
                for (Unit unit : jb.getUnits()) {
                    Stmt s = (Stmt) unit;
                    int line = getLineNumber(s);
                    // find definitions
                    for (Object o : s.getDefBoxes()) {
                        if (o instanceof ValueBox) {
                            Value v = ((ValueBox) o).getValue();
                            SootClass parentClass = null;
                            try {
                                parentClass = Scene.v().getSootClass(v.getType().toString());
                            } catch (RuntimeException e) {
                                continue;
                            }


                            if (Scene.v().getApplicationClasses().toString().contains(parentClass.getShortJavaStyleName())) {
                                System.out.println(line + " " + s.toString() + " " + parentClass.getShortJavaStyleName());
                                res.put(line, v);
                            }
                        }
                    }
                }
            }
        }

        return res;
    }

    private static void printLocalIntersects(Map<Integer,Value> ls) {
        soot.PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
        for (Map.Entry<Integer, Value> integerValueEntry : ls.entrySet()) {
            int p1 = (Integer) integerValueEntry.getKey();
            Local l1 = (Local) integerValueEntry.getValue();
            PointsToSet r1 = pta.reachingObjects(l1);
            for (Map.Entry<Integer, Value> valueEntry : ls.entrySet()) {
                int p2 = (Integer) valueEntry.getKey();
                Local l2 = (Local) valueEntry.getValue();
                PointsToSet r2 = pta.reachingObjects(l2);
                if (p1 <= p2)
                    System.out.println("[" + p1 + "," + p2 + "]	 Container intersect? " + r1.hasNonEmptyIntersection(r2));
            }
        }
    }


    private static void printFieldIntersects(Map<Integer,Value> ls, SootField f) {
        soot.PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
        for (Map.Entry<Integer, Value> integerValueEntry : ls.entrySet()) {
            int p1 = integerValueEntry.getKey();
            Local l1 = (Local) integerValueEntry.getValue();
            PointsToSet r1 = pta.reachingObjects(l1, f);
            for (Map.Entry<Integer, Value> valueEntry : ls.entrySet()) {
                int p2 = valueEntry.getKey();
                Local l2 = (Local) valueEntry.getValue();
                PointsToSet r2 = pta.reachingObjects(l2, f);
                if (p1 <= p2)
                    System.out.println("[" + p1 + "," + p2 + "]	 Container.item intersect? " + r1.hasNonEmptyIntersection(r2));
            }
        }
    }
}