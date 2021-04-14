package com.ibm.ola;

import java.io.IOException;
import java.util.Properties;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.analysis.pointers.BasicHeapGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;

import java.util.function.Predicate;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.collections.CollectionFilter;
import com.ibm.wala.util.graph.GraphSlicer;

/**
 * This application is a WALA client: it invokes an SWT TreeViewer to visualize
 * a Points-To solution
 *
 * @author sfink
 */
public class VanillaPointsToAnalysis {

        static final Logger logger = LoggerFactory.getLogger(VanillaPointsToAnalysis.class);

        public static void main(String[] args)
                        throws WalaException, IllegalArgumentException, CancelException, IOException {

                // Construct the AnalysisScope from the class path.
                Properties p = CommandLine.parse(args);
                String scopeFile = p.getProperty("scopeFile");
                if (scopeFile == null) {
                        throw new IllegalArgumentException("Must specify the scope file");
                }

                // WALA properties
                Properties walaProperties = WalaExamplesProperties.loadProperties();
                walaProperties.setProperty("DOT_EXE", "/usr/local/bin/dot");
                walaProperties.setProperty("PDFVIEW_EXE", "/Applications/PDF Expert.app/Contents/MacOS/PDF Expert");
                String pdfFile = "./pointstograph.pdf";
                String dotFile = "./pointstograph.dot";

                // Build the points to graph
                Graph<Object> pointsToGraph = buildPointsTo(scopeFile);
                // Graph<Object> prunedGraph = pruneForAppLoader(pointsToGraph);

                // DotUtil.dotify(pointsToGraph, null, dotFile, pdfFile,
                // p.getProperty(WalaExamplesProperties.DOT_EXE));
        }

        public static Graph<Object> buildPointsTo(String scopeFile)
                        throws WalaException, IllegalArgumentException, CancelException, IOException {

                AnalysisScope scope = AnalysisScopeReader.readJavaScope(scopeFile, null,
                                VanillaPointsToAnalysis.class.getClassLoader());
                // We need a baseline call graph. Here we use a CHACallGraph based on a
                // ClassHierarchy.
                ClassHierarchy cha = ClassHierarchyFactory.make(scope);
                Iterable<Entrypoint> entryPoints = Util.makeMainEntrypoints(scope, cha);
                AnalysisOptions walaOptions = new AnalysisOptions(scope, entryPoints);
                walaOptions.setReflectionOptions(ReflectionOptions.NONE);
                IAnalysisCacheView cache = new AnalysisCacheImpl();

                // -------------------
                // Build a call graph
                // -------------------
                CallGraphBuilder<InstanceKey> builder = Util.makeVanillaZeroOneCFABuilder(Language.JAVA, walaOptions,
                                cache, cha, scope);
                CallGraph cg = builder.makeCallGraph(walaOptions, null);
                PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();

                // Iterable<PointerKey> pKeys = pointerAnalysis.getPointerKeys();
                // for (PointerKey pk : pKeys) {
                // OrdinalSet<InstanceKey> pointsToSet = pointerAnalysis.getPointsToSet(pk);
                // logger.info("{} -> {}", pk.toString(), pointsToSet.toString());
                // }

                return new BasicHeapGraph<>(pointerAnalysis, cg);
        }
}
