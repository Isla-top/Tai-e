package pascal.taie.analysis.pta.plugin.taint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.analysis.pta.pts.PointsToSetFactory;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.graph.Reachability;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EntryCollector implements Plugin {

    private Solver solver;

    private Set<JMethod> sourceMethods;

    private Set<JMethod> sinkMethods;

    private Set<JMethod> entryMethods;

    private CallGraph<Invoke, JMethod> callGraph;

    private Set<JMethod> entryResult;

    private static final Logger logger = LogManager.getLogger(EntryCollector.class);

    public EntryCollector(Solver solver){
        this.solver = solver;
        TaintConfig config = TaintConfig.loadConfig(
                solver.getOptions().getString("taint-config"),
                solver.getHierarchy(),
                solver.getTypeSystem());
        sourceMethods = config.sources().stream()
                .filter(s -> s instanceof CallSource)
                .map(s -> ((CallSource) s).method())
                .collect(Collectors.toSet());
        sourceMethods.addAll(
                config.sources().stream()
                        .filter(s -> s instanceof ParamSource)
                        .map(s -> ((ParamSource) s).method())
                        .collect(Collectors.toSet()));
        sinkMethods = config.sinks().stream()
                .map(Sink::method)
                .collect(Collectors.toSet());

        entryResult = Sets.newHybridSet();
    }

    @Override
    public void onFinish(){
        entryMethods = solver.getResult().getCallGraph().entryMethods().collect(Collectors.toSet());
        callGraph = solver.getResult().getCallGraph();

        Reachability<JMethod> reachability = new Reachability<>(callGraph);
        entryMethods.forEach(m -> {
            Set<JMethod> canReachMethods = reachability.reachableNodesFrom(m);
            sourceMethods.forEach(src -> {
                sinkMethods.forEach(sk -> {
                    if(canReachMethods.contains(src) && canReachMethods.contains(sk)){
                        entryResult.add(m);
                    }
                });
            });
        });

        logger.info("Collected Entry Methods:");
        entryResult.forEach(logger::info);
    }
}
