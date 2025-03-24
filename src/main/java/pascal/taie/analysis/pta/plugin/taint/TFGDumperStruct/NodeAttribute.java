package pascal.taie.analysis.pta.plugin.taint.TFGDumperStruct;

import pascal.taie.analysis.graph.flowgraph.*;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.plugin.taint.TaintManager;

import java.util.ArrayList;
import java.util.List;

public class NodeAttribute {

    public final Long index;

    public final String className;

    public final String methodName;

    public final String varOrFieldName;

    public final String type;

    public final List<String> taintObjs;

    public final List<String> taintSources;

    public NodeAttribute(Node node, PointerAnalysisResult pta, TaintManager manager, MetaData meta){
        this.index = meta.indexOfVarAndField(node.toString());
        this.taintObjs = new ArrayList<>();
        this.taintSources = new ArrayList<>();
        if(node instanceof VarNode vn){
            this.className = vn.getVar().getMethod().getDeclaringClass().toString();
            this.methodName = vn.getVar().getMethod().getSubsignature().toString();
            this.varOrFieldName = vn.getVar().getName();
            this.type = "VarNode";
            pta.getPointsToSet(vn.getVar()).stream()
                    .filter(manager::isTaint)
                    .forEach(taint -> {
                        taintObjs.add(taint.toString());
                        taintSources.add(manager.getSourcePoint(taint).toString());
                    });
        }else if(node instanceof InstanceFieldNode ifn){
            this.className = ifn.getField().getDeclaringClass().toString();
            this.methodName = "";
            this.varOrFieldName = ifn.getField().getName();
            this.type = "InstanceFieldNode";
            pta.getPointsToSet(ifn.getField()).stream()
                    .filter(manager::isTaint)
                    .forEach(taint -> {
                        taintObjs.add(taint.toString());
                        taintSources.add(manager.getSourcePoint(taint).toString());
                    });
        }else if(node instanceof StaticFieldNode sfn){
            this.className = sfn.getField().getDeclaringClass().toString();
            this.methodName = "";
            this.varOrFieldName = sfn.getField().getName();
            this.type = "StaticFieldNode";
            pta.getPointsToSet(sfn.getField()).stream()
                    .filter(manager::isTaint)
                    .forEach(taint -> {
                        taintObjs.add(taint.toString());
                        taintSources.add(manager.getSourcePoint(taint).toString());
                    });
        }else{
            ArrayIndexNode ain = (ArrayIndexNode) node;
            if(ain.getBase().getContainerMethod().isPresent()){
                this.className = ain.getBase().getContainerMethod().get().getDeclaringClass().toString();
                this.methodName = ain.getBase().getContainerMethod().get().toString();
            } else {
                throw new RuntimeException("Error occurs while finding container of an ArrayIndexNode ");
            }
            this.varOrFieldName = ain.getBase().toString();
            this.type = "ArrayIndexNode";
            pta.getPointsToSet(ain.getBase()).stream()
                    .filter(manager::isTaint)
                    .forEach(taint -> {
                        taintObjs.add(taint.toString());
                        taintSources.add(manager.getSourcePoint(taint).toString());
                    });
        }

    }
}
