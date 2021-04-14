package com.ibm.ola;

import boomerang.*;
import boomerang.results.BackwardBoomerangResults;
import boomerang.scene.*;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.jimple.BoomerangPretransformer;
import boomerang.scene.jimple.SootCallGraph;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.options.Options;
import sync.pds.solver.OneWeightFunctions;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.Node;
import wpds.impl.Weight;
import wpds.impl.Weight.NoWeight;

public class BoomerangPointsTo {
    private static CallGraph callGraph;
    private static DataFlowScope dataFlowScope;
    private static final Logger LOGGER = LoggerFactory.getLogger(BoomerangPointsTo.class);
    protected static int analysisTimeout = 3000 * 1000;

    public static void main(String... args) {
        String sootClassPath = "/Users/rkrsn/workspace/ibm-ola-guest/resources/IRS/build/classes/java/main";
        String mainClass = "com.ibm.example.irs.BusinessProcess";

        /* ------------------- OPTIONS ---------------------- */

        setupSoot(sootClassPath, mainClass);
        analyze();
    }

    private static void setupSoot(String sootClassPath, String mainClass) {
        G.v().reset();
        Options.v().set_prepend_classpath(true);
        Options.v().set_whole_program(true);
        Options.v().set_keep_line_number(true);
        Options.v().setPhaseOption("cg.spark", "on");
        Options.v().setPhaseOption("cg", "verbose:false");
        Options.v().set_output_format(Options.output_format_none);
        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_include(getIncludeList());
        Options.v().setPhaseOption("jb", "use-original-names:true");
        Options.v().set_exclude(excludedPackages());
        Options.v().set_soot_classpath(sootClassPath);
        Options.v().set_process_dir(Collections.singletonList(sootClassPath));
        Options.v().set_main_class(mainClass);

        Scene.v().loadNecessaryClasses();
        SootClass c = Scene.v().forceResolve(mainClass, SootClass.BODIES);
        if (c != null) {
            c.setApplicationClass();
        }
        for (SootClass sc : Scene.v().getApplicationClasses()) {
            c = Scene.v().forceResolve(sc.toString(), SootClass.BODIES);
            c.setApplicationClass();
        }

        // Set entry points
        List<SootMethod> ePoints = new LinkedList<>();
        for (SootClass sc : Scene.v().getApplicationClasses()) {
            for (SootMethod sm : sc.getMethods()) {
                System.out.printf("Soot Method:%s\n", sm.toString());
                ePoints.add(sm);
            }
        }

        for (SootMethod ep : ePoints) {
            System.out.printf("Entrypoint:%s\n", ep.toString());
        }

        Scene.v().setEntryPoints(ePoints);
    }

    // private String getTargetClass() {
    // SootClass sootClass = new SootClass("dummyClass");
    // Type paramType = ArrayType.v(RefType.v("java.lang.String"), 1);
    // SootMethod mainMethod =
    // new SootMethod(
    // "main",
    // Collections.singletonList(paramType),
    // VoidType.v(),
    // Modifier.PUBLIC | Modifier.STATIC);
    // sootClass.addMethod(mainMethod);
    // JimpleBody body = Jimple.v().newBody(mainMethod);
    // mainMethod.setActiveBody(body);
    // RefType testCaseType = RefType.v(getTestCaseClassName());
    // Local loc = Jimple.v().newLocal("l0", paramType);
    // body.getLocals().add(loc);
    // body.getUnits().add(Jimple.v().newIdentityStmt(loc,
    // Jimple.v().newParameterRef(paramType,
    // 0)));
    // Local allocatedTestObj = Jimple.v().newLocal("dummyObj", testCaseType);
    // body.getLocals().add(allocatedTestObj);
    // body.getUnits()
    // .add(Jimple.v().newAssignStmt(allocatedTestObj,
    // Jimple.v().newNewExpr(testCaseType)));
    // body.getUnits()
    // .add(
    // Jimple.v()
    // .newInvokeStmt(
    // Jimple.v().newVirtualInvokeExpr(allocatedTestObj,
    // sootTestMethod.makeRef())));
    // body.getUnits().add(Jimple.v().newReturnVoidStmt());
    //
    // Scene.v().addClass(sootClass);
    // body.validate();
    // return sootClass.toString();
    // }

    protected static List<String> getIncludeList() {
        List<String> includeList = new LinkedList<>();
        includeList.add("com.ibm.*");
        return includeList;
    }

    public static List<String> excludedPackages() {
        List<String> excludedPackages = new LinkedList<>();
        excludedPackages.add("java.lang.*");
        excludedPackages.add("java.util.*");
        excludedPackages.add("java.io.*");
        excludedPackages.add("sun.misc.*");
        excludedPackages.add("java.net.*");
        excludedPackages.add("sun.nio.*");
        excludedPackages.add("javax.servlet.*");
        excludedPackages.add("sun.*");
        excludedPackages.add("javax.*");
        excludedPackages.add("com.sun.*");
        excludedPackages.add("org.xml.*");
        excludedPackages.add("org.w3c.*");
        excludedPackages.add("apple.awt.*");
        excludedPackages.add("com.apple.*");
        return excludedPackages;
    }

    private static void analyze() {
        Transform transform = new Transform("wjtp.ifds", createAnalysisTransformer());
        PackManager.v().getPack("wjtp").add(transform);
        PackManager.v().getPack("cg").apply();
        PackManager.v().getPack("wjtp").apply();
    }

    protected static SceneTransformer createAnalysisTransformer() {
        return new SceneTransformer() {
            protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
                BoomerangPretransformer.v().reset();
                BoomerangPretransformer.v().apply();
                callGraph = new SootCallGraph();
                dataFlowScope = SootDataFlowScope.make(Scene.v());
//                runWholeProgram();
                 runWholeProgramMode();
            }
        };
    }

    private static void runWholeProgramMode() {
        AnalysisScope scope = new AnalysisScope(callGraph) {
            @Override
            protected Collection<? extends Query> generate(Edge cfgEdge) {
                Statement statement = cfgEdge.getTarget();
                if (statement.isAssign()) {
                    System.out.println(statement + " " + statement.getLeftOp() + " " + statement.getRightOp());
                    return Collections.singleton(BackwardQuery.make(cfgEdge, statement.getLeftOp()));
                }
                return Collections.emptySet();
            }
        };
        Collection<Query> seeds = scope.computeSeeds();

        // 1. Create a Boomerang solver.
        Boomerang solver = new Boomerang(callGraph, SootDataFlowScope.make(Scene.v()), new DefaultBoomerangOptions() {
            @Override
            public boolean allowMultipleQueries() {
                return true;
            }
        });

        for (Query query : seeds) {
            if (query.getType() != null) {
                BackwardBoomerangResults<Weight.NoWeight> backwardQueryResults = solver.solve((BackwardQuery) query);
                if (backwardQueryResults.isEmpty())
                    continue;
                System.out.println("Solving query: " + query);
                System.out.println("All allocation sites of the query variable are:");
                System.out.println(backwardQueryResults.getAllocationSites());

                System.out.println("All aliasing access path of the query variable are:");
                System.out.println(backwardQueryResults.getAllAliases());
                System.out.println("============");
            }
        }
        solver.unregisterAllListeners();
    }

  private static void runWholeProgram() {
    final Set<Node<Edge, Val>> results = Sets.newHashSet();
    WholeProgramBoomerang<NoWeight> solver =
        new WholeProgramBoomerang<NoWeight>(
            callGraph,
            dataFlowScope,
            new DefaultBoomerangOptions() {
              @Override
              public int analysisTimeoutMS() {
                return analysisTimeout;
              }

              @Override
              public boolean onTheFlyCallGraph() {
                return true;
              }

              @Override
              public boolean allowMultipleQueries() {
                return true;
              }
            }) {

    @Override
    protected WeightFunctions<Edge, Val, Field, NoWeight> getForwardFieldWeights() {
        return new OneWeightFunctions<>(NoWeight.NO_WEIGHT_ONE);
    }

    @Override
    protected WeightFunctions<Edge, Val, Field, NoWeight> getBackwardFieldWeights() {
        return new OneWeightFunctions<>(NoWeight.NO_WEIGHT_ONE);
    }

    @Override
    protected WeightFunctions<Edge, Val, Edge, NoWeight> getBackwardCallWeights() {
        return new OneWeightFunctions<>(NoWeight.NO_WEIGHT_ONE);
    }

    @Override
    protected WeightFunctions<Edge, Val, Edge, NoWeight> getForwardCallWeights(ForwardQuery sourceQuery) {
        return new OneWeightFunctions<>(NoWeight.NO_WEIGHT_ONE);
    }

    @Override
    public void wholeProgramAnalysis() {
        AnalysisScope scope = new AnalysisScope(callGraph) {
            @Override
            protected Collection<? extends Query> generate(Edge cfgEdge) {
                Statement statement = cfgEdge.getTarget();
                if (statement.containsInvokeExpr()) {
                    Val arg = statement.getInvokeExpr().getArg(0);
                    return Collections.singleton(BackwardQuery.make(cfgEdge, arg));
                }
                return Collections.emptySet();
            }
        };
        for (Query s : scope.computeSeeds()) {
            if (s.getType() != null) {
                System.out.println(s);
                BackwardBoomerangResults<Weight.NoWeight> backwardQueryResults = solve((BackwardQuery) s);
                System.out.println("All allocation sites of the query variable are:");
                System.out.println(backwardQueryResults.getAllocationSites());
            }
        }

        System.out.println("Total solvers:" + this.getSolvers().size());
        System.out.println(options.statsFactory());
    }
};

solver.wholeProgramAnalysis();solver.unregisterAllListeners();}}
