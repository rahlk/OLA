package com.ibm.ola;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.demandpa.alg.DemandRefinementPointsTo;
import com.ibm.wala.demandpa.alg.refinepolicy.NeverRefineCGPolicy;
import com.ibm.wala.demandpa.alg.refinepolicy.NeverRefineFieldsPolicy;
import com.ibm.wala.demandpa.alg.refinepolicy.RefinementPolicyFactory;
import com.ibm.wala.demandpa.alg.refinepolicy.SinglePassRefinementPolicy;
import com.ibm.wala.demandpa.alg.statemachine.DummyStateMachine;
import com.ibm.wala.demandpa.alg.statemachine.StateMachineFactory;
import com.ibm.wala.demandpa.flowgraph.IFlowLabel;
import com.ibm.wala.demandpa.util.MemoryAccessMap;
import com.ibm.wala.demandpa.util.SimpleMemoryAccessMap;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.config.AnalysisScopeReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.util.io.CommandLine;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

public class DemandPointsToAnalysis {
   /**
   * Usage: PointsToAnalysis -scopeFile file_path 
   * 
   * @throws IOException
   * @throws CancelException
   * @throws ClassHierarchyException
   * @throws IllegalArgumentException
   */
    static final Logger logger = LoggerFactory.getLogger(DemandPointsToAnalysis.class);

    public static void main(String[] args) throws IOException, ClassHierarchyException, CancelException {
        
        // Construct the AnalysisScope from the class path.
        Properties p = CommandLine.parse(args);
        // WALA properties

        String scopeFile = p.getProperty("scopeFile");
        if (scopeFile == null) {
            throw new IllegalArgumentException("Must specify the scope file");
        }
        AnalysisScope scope = AnalysisScopeReader.readJavaScope(scopeFile, null, 
                DemandPointsToAnalysis.class.getClassLoader());

        // We need a baseline call graph. Here we use a CHACallGraph based on a ClassHierarchy.
        ClassHierarchy cha = ClassHierarchyFactory.make(scope);
        CHACallGraph chaCG = new CHACallGraph(cha);
        chaCG.init(Util.makeMainEntrypoints(scope, cha));
        AnalysisOptions walaOptions = new AnalysisOptions();
        walaOptions.setReflectionOptions(ReflectionOptions.FULL);
        IAnalysisCacheView cache = new AnalysisCacheImpl();

        // We also need a heap model to create InstanceKeys for allocation sites, etc.
        // Here we use a 0-1 CFA builder, which will give a heap abstraction similar to
        // context-insensitive Andersen's analysis
        HeapModel heapModel = Util.makeZeroOneContainerCFABuilder(walaOptions, cache, cha, scope);
        
        // The MemoryAccessMap helps the demand analysis find matching field reads and writes
        MemoryAccessMap mam = new SimpleMemoryAccessMap(chaCG, heapModel, false);
        
        // The StateMachineFactory helps in tracking additional states like calling
        // contexts. For context-insensitive analysis we use a DummyStateMachine.Factory
        StateMachineFactory<IFlowLabel> stateMachineFactory = new DummyStateMachine.Factory<>();
        DemandRefinementPointsTo drpt = DemandRefinementPointsTo.makeWithDefaultFlowGraph(chaCG, heapModel, mam, cha,
                walaOptions, stateMachineFactory);
        
        // The RefinementPolicyFactory determines how the analysis refines match edges
        // (see PLDI'06 paper). Here we use a policy that does not perform refinement and just uses a
        // fixed budget for a single pass
        RefinementPolicyFactory refinementPolicyFactory = new SinglePassRefinementPolicy.Factory(
                new NeverRefineFieldsPolicy(), new NeverRefineCGPolicy(), 1000);

        drpt.setRefinementPolicyFactory(refinementPolicyFactory);
        
        for (CGNode node : chaCG) {
            if (isApplicationNode(node)) {
                IR ir = node.getIR();

                Iterator<CallSiteReference> callSites = ir.iterateCallSites();
                while (callSites.hasNext()) {
                    CallSiteReference site = callSites.next();

                    // logger.info("{}\nCall Site: {}", node, site);
                    SSAAbstractInvokeInstruction[] calls = ir.getCalls(site);
                    try {
                        PointerKey pk = heapModel.getPointerKeyForLocal(node, calls[0].getUse(0));
                        Pair<DemandRefinementPointsTo.PointsToResult, Collection<InstanceKey>> pointsTo = drpt.getPointsTo(pk, k -> true);
                        logger.info("POINTS TO RESULT: {} -> {}", site, pointsTo.snd);
                    } catch (Exception e) {
                        logger.info("Exception: {}", e.toString());
                    }
                }
            }
        }
    }

    private static boolean isApplicationNode(CGNode t) {
        IMethod meth = t.getMethod();
        String classLoader = meth.getDeclaringClass().getClassLoader().toString();
        return classLoader.equals("Application") && t.getIR() != null;
    }

}
