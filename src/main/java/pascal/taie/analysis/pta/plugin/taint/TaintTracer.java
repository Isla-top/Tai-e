package pascal.taie.analysis.pta.plugin.taint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.graph.flowgraph.*;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.CompositePlugin;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.*;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TaintTracer extends CompositePlugin {

    private static final Logger logger = LogManager.getLogger(TaintTracer.class);

    private Solver solver;

    private MultiMap<Var, Var> vSource2Sink;

    private MultiMap<JField, Var> fSource2Sink;

//    // var -> var = a.f
//    private MultiMap<Var, LoadField> var2LoadResult = Maps.newMultiMap();
//
//    // var -> a.f = var
//    private MultiMap<Var, StoreField> var2StoreResult = Maps.newMultiMap();
//
//    // var -> var = a[i]
//    private MultiMap<Var, StoreField> var2LoadArrayResult = Maps.newMultiMap();
//
//    // var -> a[i] = var
//    private MultiMap<Var, StoreField> var2StoreArrayResult = Maps.newMultiMap();

    /**
     * var -> var1 that
     * var = var1.f
     * var = var1[i]
     * var1.f = var
     * var1[i] = var
     */
    private MultiMap<Var, Var> var2FieldResult = Maps.newMultiMap();

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        this.vSource2Sink = Maps.newMultiMap();
        this.fSource2Sink = Maps.newMultiMap();
    }

    @Override
    public void onNewStmt(Stmt stmt, JMethod container) {
        if(stmt instanceof LoadField lf){
            if(lf.getRValue() instanceof InstanceFieldAccess ifa) {
                var2FieldResult.put(lf.getLValue(), ifa.getBase());
            }
        }else if(stmt instanceof StoreField sf){
            if(sf.getLValue() instanceof InstanceFieldAccess ifa){
                var2FieldResult.put(sf.getRValue(), ifa.getBase());
            }
        }else if(stmt instanceof LoadArray la){
            var2FieldResult.put(la.getLValue(), la.getRValue().getBase());
        }else if(stmt instanceof StoreArray sa){
            var2FieldResult.put(sa.getRValue(), sa.getLValue().getBase());
        }
    }

    @Override
    public void onFinish() {
        ObjectFlowGraph ofg = solver.getResult().getObjectFlowGraph();
        ((Set)solver.getResult().getResult("pascal.taie.analysis.pta.plugin.taint.TaintAnalysis"))
                .forEach(r -> {
                    logger.info(((TaintFlow)r).sourcePoint());
                    TaintFlow tf = (TaintFlow) r;

                    Var sourceVar = null;
                    JField sourceField = null;
                    if(tf.sourcePoint() instanceof CallSourcePoint csp){
                        switch (csp.indexRef().kind()){
                            case VAR -> sourceVar = getCallSourceVar(csp);
                            case FIELD -> sourceField = getCallField(csp);
                            default -> logger.error("no this choice");
                        }
                    }

                    Var sinkVar = getSinkVar(tf.sinkPoint());
                    if(sourceVar != null){
                        vSource2Sink.put(sourceVar, sinkVar);
                        traceTaint(sourceVar, sinkVar, ofg);
                    }
                    if(sourceField != null){
                        fSource2Sink.put(sourceField, sinkVar);
                    }


                });
    }

    private void traceTaint(Var source, Var sink, ObjectFlowGraph ofg){
        MultiMap<Node, Node> edges = Maps.newMultiMap();
        // tell how many layers the taint is wrapped
        Map<Node, Integer> node2Layers = Maps.newHybridMap();
        Set<Node> newNodes = Sets.newHybridSet();

        node2Layers.put(ofg.getVarNode(source), 0);
        newNodes.add(ofg.getVarNode(source));
        while(!newNodes.isEmpty()){
            Set<Node> newNodesCopy = Sets.newHybridSet(newNodes);
            newNodes.clear();
            newNodesCopy.forEach(n -> {
                ofg.getOutEdgesOf(n).forEach(e -> {
                    edges.put(e.source(), e.target());
                    node2Layers.put(e.target(), node2Layers.get(e.source()));
                    newNodes.add(e.target());
                });

                if(n instanceof VarNode vn){
                    processVarNode(node2Layers, vn, edges, newNodes, ofg);
                    processFieldNode(node2Layers, vn, edges, newNodes, ofg);
                }
            });
            logger.info(newNodes);
        }
        logger.info(edges);
    }

    private void processVarNode(Map<Node, Integer> node2Layers, VarNode vn, MultiMap<Node, Node> edges, Set<Node> newNodes, ObjectFlowGraph ofg){
        if(node2Layers.get(vn) == 0){
            return;
        }
        vn.getVar().getLoadFields()
                .forEach(lf -> {
                    JField jf = lf.getRValue().getFieldRef().resolve();
                    solver.getCSManager().getCSVarsOf(vn.getVar())
                            .stream()
                            .map(csv -> solver.getPointsToSetOf(csv))
                            .forEach(pt -> {
                                pt.forEach(cso -> {
                                    InstanceFieldNode ifn = ofg.getInstanceFieldNode(cso.getObject(), jf);
                                    if(ifn == null || !node2Layers.containsKey(ifn) || ofg.getVarNode(lf.getLValue()) == null) return;
                                    edges.put(vn, ofg.getVarNode(lf.getLValue()));
                                    node2Layers.put(ofg.getVarNode(lf.getLValue()), node2Layers.get(vn) - 1);
                                    newNodes.add(ofg.getVarNode(lf.getLValue()));
                                });
                            });
                });
    }

    private void processFieldNode(Map<Node, Integer> node2Layers, VarNode vn, MultiMap<Node, Node> edges, Set<Node> newNodes, ObjectFlowGraph ofg){
        Set<Node> nextNodes = Sets.newHybridSet(edges.get(vn));
        nextNodes.forEach(n -> {
            if(n instanceof InstanceFieldNode ifn){
                var2FieldResult.get(vn.getVar()).forEach(v -> {
                    Node wrap = ofg.getVarNode(v);
                    if(solver.getResult().getPointsToSet(v).contains(ifn.getBase()) && wrap != null && !node2Layers.containsKey(wrap)){
                        edges.put(vn, wrap);
                        if(!node2Layers.containsKey(wrap)){
                            node2Layers.put(wrap, node2Layers.get(vn) + 1);
                            newNodes.add(wrap);
                        }
                    }
                });
            }else if(n instanceof ArrayIndexNode ain){
                var2FieldResult.get(vn.getVar()).forEach(v -> {
                    Node wrap = ofg.getVarNode(v);
                    if(solver.getResult().getPointsToSet(v).contains(ain.getBase()) && wrap != null){
                        edges.put(vn, wrap);
                        if(!node2Layers.containsKey(wrap)){
                            node2Layers.put(wrap, node2Layers.get(vn) + 1);
                            newNodes.add(wrap);
                        }
                    }
                });
            }
        });
    }


    private Var getCallSourceVar(CallSourcePoint csp){
        Var result = null;
        switch (csp.indexRef().index()){
            case -2 -> result = csp.sourceCall().getResult();
            case -1 -> result = csp.sourceCall().getRValue().getMethodRef().resolve().getIR().getThis();
            default -> result = csp.sourceCall().getRValue().getArg(csp.indexRef().index());
        }
        return result;
    }

    private JField getCallField(CallSourcePoint csp){
        return null;
    }

    private Var getSinkVar(SinkPoint sp){
        Var result = null;
        switch (sp.indexRef().index()){
            case -2 -> result = sp.sinkCall().getResult();
            case -1 -> result = sp.sinkCall().getRValue().getMethodRef().resolve().getIR().getThis();
            default -> result = sp.sinkCall().getRValue().getArg(sp.indexRef().index());
        }
        return result;
    }
}
