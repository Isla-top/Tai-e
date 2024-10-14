package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.type.NullType;

public class ObjectAddHandler extends OnFlyHandler{

    private static final Descriptor AVOID_NULL_DESC = () -> "AvoidNullObject";

    protected ObjectAddHandler(HandlerContext context) {
        super(context);
    }

    public void onNewCallEdge(pascal.taie.analysis.graph.callgraph.Edge<CSCallSite, CSMethod> edge) {
        if (edge.getKind() == CallKind.OTHER) {
            return;
        }
        CSCallSite csCallSite = edge.getCallSite();
        Invoke invoke = csCallSite.getCallSite();

        // tomcat need
        if(!invoke.getMethodRef().resolve().isAbstract() && invoke.getMethodRef().getName().equals("getContent")){
            invoke.getMethodRef().resolve().getIR().getReturnVars()
                    .stream()
                    .filter(var -> !(var.getType() instanceof NullType))
                    .forEach(var -> solver.addVarPointsTo(solver.getContextSelector().selectContext(csCallSite, invoke.getMethodRef().resolve()), var,
                            solver.getHeapModel().getMockObj(AVOID_NULL_DESC, "Avoid Null", solver.getTypeSystem().getType("byte[]"))));
        }
    }

}
