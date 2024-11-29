package pascal.taie.analysis.pta.plugin.infer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.plugin.dcl.ServiceLoaderModel;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.analysis.pta.pts.PointsToSetFactory;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;

import java.util.Map;

public class AvoidNullHandler implements Plugin {

    private Solver solver;

    private ClassHierarchy hierarchy;

    private CSManager csManager;

    private HeapModel heapModel;

    /**
     * one class maps to one obj
     */
    private Map<JClass, Obj> classObjMap;

    private final PointsToSetFactory ptsFactory;

    private static final Logger logger = LogManager.getLogger(AvoidNullHandler.class);

    public AvoidNullHandler(Solver solver){
        this.solver = solver;
        this.csManager = solver.getCSManager();
        this.hierarchy = solver.getHierarchy();
        this.heapModel = solver.getHeapModel();
        this.ptsFactory = new PointsToSetFactory(csManager.getObjectIndexer());
        this.classObjMap = Maps.newHybridMap();
    }

    @Override
    public void onPhaseFinish(){
        logger.info("");
        logger.warn("New Phase:");
        csManager.getCSVars()
                .stream()
                .filter(v -> v.getPointsToSet() == null || v.getPointsToSet().isEmpty())
                .filter(v -> !(v.getType() instanceof PrimitiveType || v.getType() instanceof ArrayType))
//                .filter(v -> !v.getType().getName().equals("java.lang.String"))
//                .filter(v -> !v.getType().getName().equals("java.lang.Class"))
//                .filter(v -> !v.getType().getName().equals("java.lang.Object"))
                .filter(v -> v.getType().getName().startsWith("org") || v.getType().getName().startsWith("java.lang.reflect") || v.getType().getName().startsWith("java.lang.Class"))
                .forEach(v -> {
                    logger.info(v + "   " + v.getType());
                    PointsToSet pts = getMockObjs(v);
                    if(!pts.isEmpty()) {
                        solver.addVarPointsTo(v.getContext(), v.getVar(), pts);
                    }
                });
    }

    private PointsToSet getMockObjs(CSVar csVar){
        if(csVar.getVar().getInvokes().isEmpty()){
            return ptsFactory.make();
        }
        Invoke invoke = csVar.getVar().getInvokes().get(0);

        return hierarchy.getAllSubclassesOf(hierarchy.getClass(csVar.getType().getName()))
                .stream()
                .filter(c -> !c.isInterface() && !c.isAbstract())
                .map(c -> classObjMap.computeIfAbsent(c, clz ->
                        heapModel.getMockObj(() -> (clz.getSimpleName() + "Obj"), invoke, clz.getType(), invoke.getContainer())))
                .map(o -> csManager.getCSObj(solver.getContextSelector().getEmptyContext(), o))
                .collect(ptsFactory::make, PointsToSet::addObject, PointsToSet::addAll);
    }
}
