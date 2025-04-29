package pascal.taie.analysis.pta.plugin.taint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.flowgraph.*;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.plugin.taint.TFGDumperStruct.EdgeAttribute;
import pascal.taie.analysis.pta.plugin.taint.TFGDumperStruct.MetaData;
import pascal.taie.analysis.pta.plugin.taint.TFGDumperStruct.NodeAttribute;
import pascal.taie.analysis.pta.plugin.taint.TFGDumperStruct.Relation;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.ClassMember;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.TwoKeyMap;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class DumperStruct {

    private static final Logger logger = LogManager.getLogger(DumperStruct.class);

    public final MetaData metadata;

    /**
     * represent the relations among packages, classes, methods, variables and fields
     */
    public final Relation relation;

    /**
     * represent the taint flow path
     */
    public final Map<Long, List<Long>> graph;

    public final List<Long> sourceNodes;

    public final List<Long> sinkNodes;

    public final List<NodeAttribute> nodeAttributes;

    public final Map<Long, Map<Long, EdgeAttribute>> edgeAttributeMap;

    public DumperStruct(TaintFlowGraph tfg, PointerAnalysisResult pta, TaintManager manager) {
        Set<Node> nodes = tfg.getNodes();
//        Collection<FlowEdge> edges = tfg.getEdges();
        Collection<FlowEdge> edges = tfg.getNodes().stream()
                                        .map(tfg::getOutEdgesOf)
                                        .flatMap(Collection::stream)
                                        .collect(Collectors.toSet());

        List<JMethod> methodList = allMethodsFromNodes(nodes);
        List<JClass> classList = allClassFromNodes(nodes, methodList);
        List<Node> nodeList = nodes.stream().toList();
        this.metadata = new MetaData(nodeList, methodList, classList);

        this.relation = new Relation(nodes, metadata);

        this.nodeAttributes = new ArrayList<>();
        nodeList.forEach(n -> this.nodeAttributes.add(new NodeAttribute(n, pta, manager, metadata)));
//        logger.info("total:  " + nodes.stream().filter(n -> n instanceof InstanceFieldNode).collect(Collectors.toSet()).size());
//        nodes.stream().filter(n -> n instanceof InstanceFieldNode).forEach(n -> {
//            logger.info(tfg.getPredsOf(n).size() + "   " + tfg.getSuccsOf(n).size());
//        });

        this.edgeAttributeMap = Maps.newHybridMap();
        this.graph = Maps.newHybridMap();
        edges.forEach(edge -> {
            Long key = this.metadata.indexOfVarAndField(edge.source().toString());
            Long value = this.metadata.indexOfVarAndField(edge.target().toString());
            if (!this.graph.containsKey(this.metadata.indexOfVarAndField(edge.source().toString()))) {
                this.graph.put(key, new ArrayList<>());
            }
            this.graph.get(key).add(value);

            if(!this.edgeAttributeMap.containsKey(key)){
                edgeAttributeMap.put(key, Maps.newHybridMap());
            }
            this.edgeAttributeMap.get(key).put(value, new EdgeAttribute(edge, metadata));
        });
        this.graph.replaceAll((k, v) -> this.graph.get(k).stream().sorted().toList()); // delete distinct()
        initEdgeAttributeMapDetail(nodes, pta);

        this.sourceNodes = tfg.getSourceNodes().stream().map(n -> this.metadata.indexOfVarAndField(n.toString())).toList();

        this.sinkNodes = tfg.getSinkNodes().stream().map(n -> this.metadata.indexOfVarAndField(n.toString())).toList();
    }

    /**
     * get all methods that contain at least one variable in nodes
     */
    private List<JMethod> allMethodsFromNodes(Set<Node> nodes) {
        List<JMethod> methodList = new ArrayList<>();
        methodList.addAll(nodes.stream().filter(n -> (n instanceof VarNode))
                               .map(n -> ((VarNode) n).getVar().getMethod())
                               .toList());
        methodList.addAll(nodes.stream().filter(n -> (n instanceof ArrayIndexNode))
                               .map(n -> {
                                   ArrayIndexNode ain = ((ArrayIndexNode) n);
                                   if (ain.getBase().getContainerMethod().isPresent()) {
                                       return ain.getBase().getContainerMethod().get();
                                   }
                                   throw new RuntimeException("Error occurs while finding container of an ArrayIndexNode ");
                               })
                               .toList());
        return methodList;
    }

    /**
     * get all classes which:
     * 1.contain at least one field in nodes,
     * 2.contain methods that contain at least one variable in nodes
     */
    private List<JClass> allClassFromNodes(Set<Node> nodes, List<JMethod> methodList) {
        /* add class containing taint field */
        List<JClass> classList = new ArrayList<>();
        classList.addAll(nodes.stream().filter(n -> (n instanceof InstanceFieldNode))
                              .map(n -> ((InstanceFieldNode) n).getField().getDeclaringClass())
                              .toList());
        classList.addAll(nodes.stream().filter(n -> (n instanceof StaticFieldNode))
                              .map(n -> ((StaticFieldNode) n).getField().getDeclaringClass())
                              .toList());

        /* add class containing methods that contain taint */
        classList.addAll(methodList.stream()
                                   .map(ClassMember::getDeclaringClass)
                                   .toList());
        return classList;
    }

    private void initEdgeAttributeMapDetail(Set<Node> nodes, PointerAnalysisResult pta){
        Map<Var, VarNode> var2Node = Maps.newHybridMap();
        nodes.forEach(n -> {
            if(n instanceof VarNode vn){
                var2Node.put(vn.getVar(), vn);
            }
        });
        pta.getCallGraph().edges()
                .forEach(e -> {
                    if(e.getKind() != CallKind.OTHER){
                        InvokeExp invokeExp = e.getCallSite().getRValue();
                        for (int i = 0; i < invokeExp.getArgCount(); ++i) {
                            Var arg = invokeExp.getArg(i);
                            Var param = e.getCallee().getIR().getParam(i);
                            if(var2Node.containsKey(arg) && var2Node.containsKey(param)){
                                Long key1 = metadata.indexOfVarAndField(var2Node.get(arg).toString());
                                Long key2 = metadata.indexOfVarAndField(var2Node.get(param).toString());

                                EdgeAttribute attribute = edgeAttributeMap.get(key1).get(key2);
                                if(attribute != null){
                                    attribute.setCallSiteInfo(e.getCallSite().toString());
                                }
                            }
                        }

                        if(invokeExp instanceof InvokeInstanceExp iie){
                            Var base = iie.getBase();
                            Var thisVar = e.getCallee().getIR().getThis();
                            if(var2Node.containsKey(base) && var2Node.containsKey(thisVar)){
                                Long key1 = metadata.indexOfVarAndField(var2Node.get(base).toString());
                                Long key2 = metadata.indexOfVarAndField(var2Node.get(thisVar).toString());

                                EdgeAttribute attribute = edgeAttributeMap.get(key1).get(key2);
                                if(attribute != null){
                                    attribute.setCallSiteInfo(e.getCallSite().toString());
                                }
                            }
                        }

                        if(e.getCallSite().getResult() != null){
                            Var result = e.getCallSite().getResult();
                            if(var2Node.containsKey(result)){
                                Long key1 = metadata.indexOfVarAndField(var2Node.get(result).toString());
                                e.getCallee().getIR().getReturnVars().forEach(rv -> {
                                    if(var2Node.containsKey(rv)){
                                        Long key2 = metadata.indexOfVarAndField(var2Node.get(rv).toString());

                                        EdgeAttribute attribute = edgeAttributeMap.get(key2).get(key1);
                                        if(attribute != null){
                                            attribute.setCallSiteInfo(e.getCallSite().toString());
                                        }
                                    }
                                });
                            }
                        }
                    }
                });
    }

    public void dump(File file) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .disable(YAMLGenerator.Feature.SPLIT_LINES)
                .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR));
        try {
            mapper.writeValue(file, this);
        } catch (IOException e) {
            logger.warn("Failed to dump {}", file.getAbsolutePath(), e);
        }
    }
}
