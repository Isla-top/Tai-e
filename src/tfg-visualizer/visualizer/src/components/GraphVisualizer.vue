<template>
  <div class="visualizer-container">
    <!-- 可视化标题和工具栏 -->
    <el-card class="visualizer-card">
      <template #header>
        <div class="card-header">
          <span>图结构可视化</span>
          <div class="toolbar">
            <el-button-group class="zoom-controls">
              <el-button size="small" @click="zoomOut"><el-icon><zoom-out /></el-icon></el-button>
              <el-button size="small" @click="resetZoom"><el-icon><refresh /></el-icon></el-button>
              <el-button size="small" @click="zoomIn"><el-icon><zoom-in /></el-icon></el-button>
            </el-button-group>
          </div>
        </div>
      </template>
      
      <!-- 图表展示区域 -->
      <div ref="diagramRef" class="diagram-container"></div>
      
      <!-- 图例说明 -->
      <div class="legend-container">
        <div class="legend-title">图例:</div>
        <div class="legend-items">
          <div class="legend-item">
            <div class="legend-color package-color"></div>
            <span>包 (Package)</span>
          </div>
          <div class="legend-item">
            <div class="legend-color class-color"></div>
            <span>类 (Class)</span>
          </div>
          <div class="legend-item">
            <div class="legend-color method-color"></div>
            <span>方法 (Method)</span>
          </div>
          <div class="legend-item">
            <div class="legend-color var-color"></div>
            <span>变量/字段 (Variable/Field)</span>
          </div>
          <div class="legend-item">
            <div class="legend-color source-color"></div>
            <span>源节点 (Source)</span>
          </div>
          <div class="legend-item">
            <div class="legend-color sink-color"></div>
            <span>汇节点 (Sink)</span>
          </div>
        </div>
      </div>
    </el-card>
    
    <!-- 详情侧边栏 -->
    <el-drawer
      v-model="detailsVisible"
      title="节点详情"
      direction="rtl"
      size="30%">
      <div v-if="selectedNode" class="node-details">
        <h3>{{ getNodeTitle(selectedNode) }}</h3>
        
        <el-descriptions 
          border 
          direction="vertical" 
          :column="1" 
          class="node-description">
          <el-descriptions-item label="节点类型">
            {{ getNodeTypeLabel(selectedNode) }}
          </el-descriptions-item>
          <el-descriptions-item label="节点ID">
            {{ selectedNode.id }}
          </el-descriptions-item>
          <template v-if="selectedNode.className">
            <el-descriptions-item label="类名">
              {{ selectedNode.className }}
            </el-descriptions-item>
          </template>
          <template v-if="selectedNode.methodName">
            <el-descriptions-item label="方法名">
              {{ selectedNode.methodName }}
            </el-descriptions-item>
          </template>
          <template v-if="selectedNode.varOrFieldName">
            <el-descriptions-item label="变量名">
              {{ selectedNode.varOrFieldName }}
            </el-descriptions-item>
          </template>
        </el-descriptions>
        
        <div v-if="selectedNode.neighbors && selectedNode.neighbors.length" class="neighbors-section">
          <h4>关联节点 ({{ selectedNode.neighbors.length }})</h4>
          <el-table :data="selectedNode.neighbors" stripe style="width: 100%">
            <el-table-column prop="id" label="节点ID" width="80" />
            <el-table-column prop="type" label="类型" width="100" />
            <el-table-column prop="name" label="名称" />
            <el-table-column fixed="right" label="操作" width="80">
              <template #default="scope">
                <el-button 
                  type="text" 
                  size="small" 
                  @click="focusNode(scope.row.id)">
                  查看
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
      <div v-else class="no-selection">
        请在图中选择一个节点来查看详情
      </div>
    </el-drawer>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ZoomIn, ZoomOut, Refresh } from '@element-plus/icons-vue'
// 这里使用import语法导入GoJS，而不是全局变量$
import * as go from 'gojs'

export default {
  name: 'GraphVisualizer',
  components: {
    ZoomIn,
    ZoomOut,
    Refresh
  },
  props: {
    graphFunction: {
      type: String,
      required: true
    },
    graphData: {
      type: Object,
      required: true
    }
  },
  setup(props) {
    watch(
      () => props.graphFunction,
      (graphFunc) => {
        if(graphFunc === "shortest-path") hightShortestPath();
        else{
          const index = graphFunc[graphFunc.length - 1];
          graphFunc = graphFunc.slice(0, graphFunc.length - 1);
          if(graphFunc === "remove-add-package") removeAddPackage(index);
          else if(graphFunc === "remove-add-class") removeAddClass(index);
          else if(graphFunc === "remove-add-method") removeAddMethod(index);
        }
      }
    )

    const granularity = {
      packages: true,
      classes: true,
      methods: true,
    }

    const removeAddPackage = (index) => {
      if(index === '1'){
        // remove
        diagram.startTransaction("removePackages");
        granularity.packages = false;
        diagram.nodes.filter(node => node.data.type === 'package')
                     .each(node => {
                        node.memberParts.each(member => diagram.model.setDataProperty(member.data, "group", undefined));
                        if(node.data.visible) diagram.model.setDataProperty(node.data, "visible", false);
                     });
        diagram.commitTransaction("removePackages");
      } else {
        // add
        diagram.startTransaction("addPackages");
        granularity.packages = true;
        if(granularity.classes){
          diagram.nodes.filter(node => node.data.type === 'class') 
                       .each(node => {
                          diagram.model.setDataProperty(node.data, "group", node.data.realGroup[0]);
                          if(node.visible && !node.containingGroup.visible) {
                            diagram.model.setDataProperty(node.containingGroup.data, "visible", true);
                            diagram.model.setDataProperty(node.containingGroup.data, "isSubGraphExpanded", true);
                          }
                       });
        }else if(granularity.methods){
          diagram.nodes.filter(node => node.data.type === 'method') 
                       .each(node => {
                          diagram.model.setDataProperty(node.data, "group", node.data.realGroup[0]);
                          if(node.visible && !node.containingGroup.visible) {
                            diagram.model.setDataProperty(node.containingGroup.data, "visible", true);
                            diagram.model.setDataProperty(node.containingGroup.data, "isSubGraphExpanded", true);
                          }
                       });
        }else{
          diagram.nodes.filter(node => node.data.type === 'variable' || node.data.type === 'field') 
                       .each(node => {
                          diagram.model.setDataProperty(node.data, "group", node.data.realGroup[0]);
                          if(node.visible && !node.containingGroup.visible) {
                            diagram.model.setDataProperty(node.containingGroup.data, "visible", true);
                            diagram.model.setDataProperty(node.containingGroup.data, "isSubGraphExpanded", true);
                          }
                       });
        }
        diagram.commitTransaction("addPackages");
      }
    }

    const removeAddClass = (index) => {
      if(index === '1'){
        // remove
        diagram.startTransaction("removeClasses");
        granularity.classes = false;
        diagram.nodes.filter(node => node.data.type === 'class')
                     .each(node => {
                        node.memberParts.each(member => member.memberParts.each(m => diagram.model.setDataProperty(m.data, "group", node.data.group)));
                        if(node.data.visible) diagram.model.setDataProperty(node.data, "visible", false);
                     });
        diagram.commitTransaction("removeClasses");
      } else {
        // add
        diagram.startTransaction("addClasses");
        granularity.classes = true;
        if(granularity.packages){
          diagram.nodes.filter(node => node.data.type === 'class') 
                       .each(node => diagram.model.setDataProperty(node.data, "group", node.data.realGroup[0]));
        }
        if(granularity.methods){
          diagram.nodes.filter(node => node.data.type === 'method' || node.data.type === 'field') 
                      .each(node => {
                          diagram.model.setDataProperty(node.data, "group", node.data.realGroup[1]);
                          if(node.visible && !node.containingGroup.containingGroup.visible) {
                            diagram.model.setDataProperty(node.containingGroup.containingGroup.data, "visible", true);
                            diagram.model.setDataProperty(node.containingGroup.containingGroup.data, "isSubGraphExpanded", true);
                          }
                      });
        }else{
          diagram.nodes.filter(node => node.data.type === 'variable' || node.data.type === 'field') 
                      .each(node => {
                          diagram.model.setDataProperty(node.data, "group", node.data.realGroup[1]);
                          if(node.visible && !node.containingGroup.containingGroup.visible) {
                            diagram.model.setDataProperty(node.containingGroup.containingGroup.data, "visible", true);
                            diagram.model.setDataProperty(node.containingGroup.containingGroup.data, "isSubGraphExpanded", true);
                          }
                      });
        }
        diagram.commitTransaction("addClasses");
      }
    }

    const removeAddMethod = (index) => {
      if(index === '1'){
        // remove
        diagram.startTransaction("removeMethods");
        granularity.methods = false;
        diagram.nodes.filter(node => node.data.type === 'method')
                     .each(node => {
                        node.memberParts.each(member => diagram.model.setDataProperty(member.data, "group", node.data.group));
                        if(node.data.visible) diagram.model.setDataProperty(node.data, "visible", false);
                     });
        diagram.commitTransaction("removeMethods");
      } else {
        // add
        diagram.startTransaction("addMethods");
        granularity.methods = true;
        if(granularity.classes){
          diagram.nodes.filter(node => node.data.type === 'method') 
                      .each(node => diagram.model.setDataProperty(node.data, "group", node.data.realGroup[1]));
        }else if(granularity.packages){
          diagram.nodes.filter(node => node.data.type === 'method') 
                       .each(node => diagram.model.setDataProperty(node.data, "group", node.data.realGroup[0]));
        }
        diagram.nodes.filter(node => node.data.type === 'variable') 
                    .each(node => {
                        diagram.model.setDataProperty(node.data, "group", node.data.realGroup[2]);
                        if(node.visible && !node.containingGroup.visible) {
                          diagram.model.setDataProperty(node.containingGroup.data, "visible", true);
                          diagram.model.setDataProperty(node.containingGroup.data, "isSubGraphExpanded", true);
                        }
                    });
        diagram.commitTransaction("addMethods");
      }
    }

    const hightShortestPath = () => {

    }

    // 图表引用和GoJS实例
    const diagramRef = ref(null);
    let diagram = null;
    
    // 选中的节点详情
    const selectedNode = ref(null);
    const detailsVisible = ref(false);
    const $ = go.GraphObject.make;

    /**
     * 初始化GoJS图表
     */
    const initDiagram = () => {
      // 创建GoJS图表
      diagram = new go.Diagram(diagramRef.value, {
        "undoManager.isEnabled": true,  // 启用撤销/重做
        "toolManager.hoverDelay": 100,  // 鼠标悬停延迟
        "toolManager.toolTipDuration": 10000, // 工具提示显示时间
        padding: 20,
        layout: new go.ForceDirectedLayout({
          defaultSpringLength: 50,
          defaultElectricalCharge: 100,
          maxIterations: 200
        }),
        // 双击一个节点时触发
        "ObjectDoubleClicked": (e) => {
          const node = e.diagram.selection.first();
          if (node) {
            selectedNode.value = getNodeData(node);
            detailsVisible.value = true;
          } else {
            selectedNode.value = null;
            detailsVisible.value = false;
          }
        }
      });

      diagram.addModelChangedListener(function(e) {
        if (e.propertyName === "visible" && (e.object.type === "variable" || e.object.type === "field")) {
          console.log("节点变为可见:", e.object);
        }
      });
      
      // 节点模板
      diagram.nodeTemplate =
        $(go.Node, "Auto", {
          selectionAdorned: true,
          selectionObjectName: "SHAPE",
          locationSpot: go.Spot.Center,
          minSize: new go.Size(100, 50),
          toolTip: $(go.Adornment, "Auto",
            new go.Shape({ fill: "#FFFFCC" }),
            new go.TextBlock({ margin: 4 },
              new go.Binding("text", "", nodeInfoToTooltip))
          )
        },
        $(go.Shape, "RoundedRectangle", {
          name: "SHAPE",
          fill: "white",
          stroke: "black",
          strokeWidth: 1,
          portId: "",
          cursor: "pointer"
        },
        new go.Binding("fill", "", nodeTypeToColor)),
        new go.Binding("visible", "visible"),
        $(go.TextBlock, {
          margin: new go.Margin(5, 5, 12, 5),
          font: "Bold 15px Sans-Serif",
          stroke: "#333",
          wrap: go.TextBlock.WrapFit,
          editable: false,
          alignment: go.Spot.Center
        },
        new go.Binding("text", "", nodeInfoToLabel)),
        $("Button", 
          { 
            visible: true,
            alignment: go.Spot.Bottom,
            alignmentFocus: go.Spot.Top,
            click: (e, obj) => {
              e.diagram.startTransaction("expand or collapse next nodes");
              var node = obj.part;
              if (node.data.isCollapsed) {
                node.findNodesOutOf().each(next => parentVisible(next, true));
                node.findLinksOutOf().each(link => diagram.model.setDataProperty(link.data, "visible", true));
              }
              else {
                const collapseFrom = (start) => {
                  start.findLinksOutOf().each(link => diagram.model.setDataProperty(link.data, "visible", false));
                  start.findNodesOutOf().each(next => {
                    if(next.findLinksInto().any(link => link.visible) || !next.visible) return;
                    parentVisible(next, false);
                    diagram.model.setDataProperty(next.data, "isCollapsed", true);
                    collapseFrom(next);
                  })
                };
                collapseFrom(node);
              }
              diagram.model.setDataProperty(node.data, "isCollapsed", !node.data.isCollapsed)
              e.diagram.commitTransaction("expand or collapse next nodes");
            }
          },
          $(go.Shape,
              {
                name: "ButtonIcon",
                desiredSize: new go.Size(12, 6)
              },
          new go.Binding("figure", "isCollapsed", collapsed => collapsed ? "PlusLine" : "MinusLine")),
        )
      );
        
      // 链接模板
      diagram.linkTemplate =
        $(go.Link, {
          curve: go.Link.Bezier,
          adjusting: go.Link.Stretch,
          reshapable: true,
          relinkableFrom: true,
          relinkableTo: true,
          toShortLength: 4
        },
        $(go.Shape, { 
          strokeWidth: 3,
          stroke: "#555"
        }),
        $(go.Shape, { 
          toArrow: "Standard",
          stroke: "#555",
          fill: "#555"
        }),
        new go.Binding("visible", "visible"),
      );

      // Group模板
      diagram.groupTemplate =
        $(go.Group, "Auto",
            {
              layout: $(go.LayeredDigraphLayout,
                  {
                    direction: 90,
                    layeringOption: go.LayeredDigraphLayout.LayerLongestPathSource,
                    alignOption: go.LayeredDigraphLayout.AlignAll
                  }),
              isSubGraphExpanded: false
            },
            new go.Binding("isSubGraphExpanded", "isSubGraphExpanded"),
            new go.Binding("visible", "visible"),
            $(go.Shape, "RoundedRectangle",
                { stroke: "gray", strokeWidth: 2 },
                new go.Binding("fill", "", nodeTypeToColor)),
            $(go.Panel, "Vertical",
                { defaultAlignment: go.Spot.Left, margin: 4 },
                $(go.Panel, "Horizontal",
                    { defaultAlignment: go.Spot.Top },
                    $("SubGraphExpanderButton"),
                    $(go.TextBlock,
                        { font: "Bold 18px Sans-Serif", margin: 4 },
                        new go.Binding("text", "", nodeInfoToLabel)),
                ),
                $(go.Placeholder,
                { padding: new go.Margin(0, 10) }),
            ),
        );
      
      // 加载图数据
      loadGraphData();

      //初始可见节点设置
      const parentVisible = (child, canVisit) => {
                diagram.model.setDataProperty(child.data, "visible", canVisit);
                while(child.containingGroup){
                  if(!canVisit && child.containingGroup.memberParts.any(n => n.visible)) return;
                  diagram.model.setDataProperty(child.containingGroup.data, "visible", canVisit);
                  child = child.containingGroup; 
                }
              };
      diagram.startTransaction("initial source sink nodes visible");
        diagram.nodes.filter(node => node.visible).each(node => parentVisible(node, true));
      diagram.commitTransaction("initial source sink nodes visible");
    };
    
    /**
     * 将节点信息转换为标签文本
     * @param {Object} nodeInfo - 节点数据
     * @returns {string} 节点标签
     */
    const nodeInfoToLabel = (nodeInfo) => {
      // 根据节点类型返回不同的标签
      if (nodeInfo.type === 'package') {
        return nodeInfo.name;
      } else if (nodeInfo.type === 'class') {
        const parts = nodeInfo.name.split('.');
        return parts[parts.length - 1]; // 只显示类名，不显示包名
      } else if (nodeInfo.type === 'method') {
        // 提取方法名
        let methodSig = nodeInfo.name.slice(nodeInfo.name.indexOf(":") + 2, nodeInfo.name.length - 1);
        let [returnType, nameAndParams] = methodSig.split(" ");
        let returnClass = returnType.split(".").at(-1);
        let [name, params] = nameAndParams.split("(");
        let methodName = returnClass + " " + name + "(";
        params.split(",").forEach(str => {
          methodName += str.split(".").at(-1);
        });
        return methodName;
      } else if (nodeInfo.type === 'variable') {
        let varName = nodeInfo.name.slice(nodeInfo.name.indexOf("/") + 1, nodeInfo.name.length - 1);
        return varName;
      } else if (nodeInfo.type === 'field') {
        let fieldName = nodeInfo.name.slice(nodeInfo.name.indexOf("}.") + 1, nodeInfo.name.length - 1);
        return fieldName;
      }
      return `${nodeInfo.name}`;
    };
    
    /**
     * 将节点信息转换为工具提示
     * @param {Object} nodeInfo - 节点数据
     * @returns {string} 工具提示文本
     */
    const nodeInfoToTooltip = (nodeInfo) => {
      let tooltip = `ID: ${nodeInfo.key}\n`;
      
      if (nodeInfo.type === 'variable') {
        tooltip += `变量: ${nodeInfo.name}\n`;
        if (nodeInfo.className) tooltip += `所属类: ${nodeInfo.className}\n`;
        if (nodeInfo.methodName) tooltip += `所属方法: ${nodeInfo.methodName}`;
      } else if(nodeInfo.type === 'field') {
        tooltip += `变量: ${nodeInfo.name}\n`;
        if (nodeInfo.className) tooltip += `所属类: ${nodeInfo.className}\n`;
      }
      
      return tooltip;
    };
    
    /**
     * 根据节点类型确定颜色
     * @param {Object} nodeInfo - 节点数据
     * @returns {string} 颜色值
     */
    const nodeTypeToColor = (nodeInfo) => {
      // 如果是源节点或汇节点，优先显示特定颜色
      if (nodeInfo.isSource) return "#FFCCE5"; // 源节点颜色
      if (nodeInfo.isSink) return "#FFD700";   // 汇节点颜色
      
      // 根据节点类型返回不同的颜色
      switch (nodeInfo.type) {
        case 'package': return "#CCE5FF";  // 蓝色
        case 'class': return "#D5F5E3";    // 绿色
        case 'method': return "#FCF3CF";   // 黄色
        case 'field':
        case 'variable': return "#F5CBA7";      // 橙色
        default: return "white";
      }
    };
    
    /**
     * 加载图数据
     */
    const loadGraphData = () => {
      if (!props.graphData || !diagram) return;
      
      const nodeDataArray = [];
      const linkDataArray = [];
      
      // 处理图数据
      processGraphData(nodeDataArray, linkDataArray);
      
      // 设置图表模型
      diagram.model = new go.GraphLinksModel({
        nodeDataArray: nodeDataArray,
        linkDataArray: linkDataArray
      });

      // 执行布局
      diagram.layoutDiagram(true);
    };
    
    /**
     * 处理图数据，生成节点和链接
     * @param {Array} nodeDataArray - 节点数组引用
     * @param {Array} linkDataArray - 链接数组引用
     */
    const processGraphData = (nodeDataArray, linkDataArray) => {
      const { metadata, relation, graph, sourceNodes, sinkNodes, nodeAttributes, edgeAttributeMap } = props.graphData;
      console.log(nodeAttributes + edgeAttributeMap);
      if (metadata && metadata.packages && relation.packageToClasses) {
        metadata.packages.forEach((packageName, index) => {
          // 添加包节点
          const pkgKey = `p_${index}`;
          nodeDataArray.push({
            key: pkgKey,
            name: packageName,
            type: 'package',
            isGroup: true,
            category: 'PackageGroup',
            isSubGraphExpanded: false,
            visible: false,
          });
          
          // 添加包的类
          (relation.packageToClasses[index] || []).forEach(classIndex => {
            // 添加类节点
            const clsKey = `c_${classIndex}`;
            nodeDataArray.push({
              key: clsKey,
              name: metadata.classes[classIndex],
              type: 'class',
              isGroup: true,
              group: pkgKey,
              category: 'ClassGroup',
              isSubGraphExpanded: false,
              visible: false,
              realGroup: [ pkgKey ],
            });

            // 在类中创建两个子group：方法组和字段组
            const methodsGroupKey = `${clsKey}_methods`;
            const fieldsGroupKey = `${clsKey}_fields`;
            
            // 方法组
            nodeDataArray.push({
              key: methodsGroupKey,
              name: 'Methods',
              type: 'methods-container',
              isGroup: true,
              group: clsKey,
              category: 'MethodsGroup',
              isSubGraphExpanded: true,
              visible: false,
            });

            // 字段组
            nodeDataArray.push({
              key: fieldsGroupKey,
              name: 'Fields',
              type: 'fields-container',
              isGroup: true,
              group: clsKey,
              category: 'FieldsGroup',
              isSubGraphExpanded: true,
              visible: false,
            });
            
            // 添加类的方法
            (relation.classToMethods[classIndex] || []).forEach(methodIndex => {
              // 添加方法节点
              const mthKey = `m_${methodIndex}`;
              nodeDataArray.push({
                key: mthKey,
                name: metadata.methods[methodIndex],
                type: 'method',
                isGroup: true,
                group: methodsGroupKey,
                category: 'MethodGroup',
                isSubGraphExpanded: false,
                visible: false,
                realGroup: [pkgKey, methodsGroupKey],
              });

              // 添加方法的变量
              (relation.methodToVars[methodIndex] || []).forEach(varIndex => {
                // 添加变量节点
                const varKey = `n_${varIndex}`;
                const source = sourceNodes?.includes(varIndex);
                const sink = sinkNodes?.includes(varIndex);
                nodeDataArray.push({
                  key: varKey,
                  name: metadata.varsAndFields[varIndex],
                  type: 'variable',
                  group: mthKey,
                  isSource: source,
                  isSink: sink,
                  visible: source || sink,
                  isCollapsed: true,
                  realGroup: [pkgKey, methodsGroupKey, mthKey],
                });


              });
            });

            // 添加类的字段
            (relation.classToFields[classIndex] || []).forEach(fieldIndex => {
              // 添加字段节点
              const fldKey = `n_${fieldIndex}`;
              const source = sourceNodes?.includes(fieldIndex);
              const sink = sinkNodes?.includes(fieldIndex);
              nodeDataArray.push({
                key: fldKey,
                name: metadata.varsAndFields[fieldIndex],
                type: 'field',
                group: fieldsGroupKey,
                isSource: source,
                isSink: sink,
                visible: source || sink,
                isCollapsed: true,
                realGroup: [pkgKey, fieldsGroupKey],
              });
            });

          });
        });
      }
      
      // 处理图边关系
      if (graph) {
        for (const [from, toArray] of Object.entries(graph)) {
          toArray.forEach(to => {
            // 添加节点间的链接
            linkDataArray.push({
              from: `n_${from}`,
              to: `n_${to}`,
              relationship: "connects",
              visible: false,
            });
          });
        }
      }
    };
    
    /**
     * 获取节点的标题
     * @param {Object} node - 节点数据
     * @returns {string} 节点标题
     */
    const getNodeTitle = (node) => {
      if (node.type === 'package') {
        return `包: ${node.name}`;
      } else if (node.type === 'class') {
        return `类: ${node.name}`;
      } else if (node.type === 'method') {
        return `方法: ${node.name}`;
      } else if (node.type === 'variable') {
        return `变量: ${node.name}`;
      } else if(node.type === 'field') {
        return `字段: ${node.name}`;
      }
      return `节点 ${node.id}`;
    };
    
    /**
     * 获取节点类型的中文描述
     * @param {Object} node - 节点数据
     * @returns {string} 类型描述
     */
    const getNodeTypeLabel = (node) => {
      const typeMap = {
        'package': '包',
        'class': '类',
        'method': '方法',
        'var': '变量/字段',
        'unknown': '未知'
      };
      return typeMap[node.type] || '未知';
    };
    
    /**
     * 获取节点详细数据
     * @param {go.Node} goNode - GoJS节点对象
     * @returns {Object} 扩展的节点数据
     */
    const getNodeData = (goNode) => {
      if (!goNode) return null;
      
      const data = goNode.data;
      const neighbors = [];
      
      // 获取相关的节点
      goNode.findLinksConnected().each(link => {
        const otherNode = link.getOtherNode(goNode);
        if (otherNode) {
          neighbors.push({
            id: otherNode.data.key,
            type: getNodeTypeLabel(otherNode.data),
            name: nodeInfoToLabel(otherNode.data)
          });
        }
      });
      
      return {
        id: data.key,
        name: data.name,
        type: data.type,
        className: data.className,
        methodName: data.methodName,
        varOrFieldName: data.varOrFieldName,
        isSource: data.isSource,
        isSink: data.isSink,
        neighbors: neighbors
      };
    };
    
    /**
     * 放大图表
     */
    const zoomIn = () => {
      if (diagram) diagram.commandHandler.increaseZoom();
    };
    
    /**
     * 缩小图表
     */
    const zoomOut = () => {
      if (diagram) diagram.commandHandler.decreaseZoom();
    };
    
    /**
     * 重置缩放比例
     */
    const resetZoom = () => {
      if (diagram) {
        diagram.scale = 1.0;
        diagram.commandHandler.scrollToPart(diagram.findNodeForKey("p_0"));
      }
    };
    
    /**
     * 聚焦到特定节点
     * @param {string} nodeId - 节点ID
     */
    const focusNode = (nodeId) => {
      if (diagram) {
        const node = diagram.findNodeForKey(nodeId);
        if (node) {
          diagram.select(node);
          diagram.commandHandler.scrollToPart(node);
        }
      }
    };
    
    // 组件挂载时初始化
    onMounted(() => {
      nextTick(() => {
        // 确保DOM已更新
        initDiagram();
      });
    });
    
    // 组件卸载时清理
    onUnmounted(() => {
      if (diagram) {
        diagram.div = null;
        diagram = null;
      }
    });
    
    return {
      diagramRef,
      selectedNode,
      detailsVisible,
      getNodeTitle,
      getNodeTypeLabel,
      zoomIn,
      zoomOut,
      resetZoom,
      focusNode
    };
  }
}
</script>

<style scoped>
.visualizer-container {
  padding: 20px 0;
}

.visualizer-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar {
  display: flex;
  gap: 10px;
}

.zoom-controls {
  margin-left: 10px;
}

.diagram-container {
  width: 100%;
  height: 600px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  margin-bottom: 20px;
}

.legend-container {
  margin-top: 20px;
  padding: 10px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
}

.legend-title {
  font-weight: bold;
  margin-bottom: 10px;
}

.legend-items {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.legend-item {
  display: flex;
  align-items: center;
}

.legend-color {
  width: 20px;
  height: 20px;
  border: 1px solid #999;
  border-radius: 3px;
  margin-right: 5px;
}

.package-color {
  background-color: #CCE5FF;
}

.class-color {
  background-color: #D5F5E3;
}

.method-color {
  background-color: #FCF3CF;
}

.var-color {
  background-color: #F5CBA7;
}

.source-color {
  background-color: #FFCCE5;
}

.sink-color {
  background-color: #FFD700;
}

.node-details {
  padding: 0 20px;
}

.node-description {
  margin: 20px 0;
}

.neighbors-section {
  margin: 20px 0;
}

.no-selection {
  padding: 30px;
  text-align: center;
  color: #909399;
}
</style>