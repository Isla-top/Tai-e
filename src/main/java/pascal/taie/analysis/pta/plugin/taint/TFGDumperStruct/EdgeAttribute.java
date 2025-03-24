package pascal.taie.analysis.pta.plugin.taint.TFGDumperStruct;

import pascal.taie.analysis.graph.flowgraph.FlowEdge;

public class EdgeAttribute {

    public final Long source;

    public final Long target;

    public final String type;

    // record callsite information for parameter-passing, return edges
    public String callSiteInfo;

    public EdgeAttribute(FlowEdge edge, MetaData meta){
        this.source = meta.indexOfVarAndField(edge.source().toString());
        this.target = meta.indexOfVarAndField(edge.target().toString());
        this.type = edge.kind().toString();
        this.callSiteInfo = "";
    }

    public void setCallSiteInfo(String callSiteInfo){
        this.callSiteInfo = callSiteInfo;
    }


}
