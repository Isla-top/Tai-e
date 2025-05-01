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
    
    <!-- 节点详情侧边栏 -->
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
            {{ selectedNode.type}}
          </el-descriptions-item>
          <el-descriptions-item label="节点ID">
            {{ selectedNode.id }}
          </el-descriptions-item>
          <template v-if="selectedNode.className">
            <el-descriptions-item label="所在类名">
              {{ selectedNode.className }}
            </el-descriptions-item>
          </template>
          <template v-if="selectedNode.methodName">
            <el-descriptions-item label="所在方法名">
              {{ selectedNode.methodName }}
            </el-descriptions-item>
          </template>
          <template v-if="selectedNode.name">
            <el-descriptions-item label="变量名">
              {{ selectedNode.name }}
            </el-descriptions-item>
          </template>
        </el-descriptions>

        <div v-if="selectedNode.taintInfos && selectedNode.taintInfos.length" class="taint-section">
          <h4>存储污点对象信息 ({{ selectedNode.taintInfos.length }})</h4>
          <el-table :data="selectedNode.taintInfos" stripe style="width: 100%">
            <el-table-column prop="taintObj" label="污点对象" width="auto" />
            <el-table-column prop="taintSource" label="污点产生源" width="auto" />
          </el-table>
        </div>
        
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
                  @click="focusNode(scope.row.id, selectedNode.id)">
                  查看
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
      <div v-else class="no-selection">
        请在图中选择变量或者字段节点来查看详情
      </div>
    </el-drawer>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ZoomIn, ZoomOut, Refresh } from '@element-plus/icons-vue'
import * as go from 'gojs'
import Graph from 'node-dijkstra';

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
        const now = performance.now();
        if(graphFunc === "shortest-path") getShortestPath();
        else if(graphFunc === "frequency-node-path") getFrequencyNodePath();
        else{
          const index = graphFunc[graphFunc.length - 1];
          graphFunc = graphFunc.slice(0, graphFunc.length - 1);
          if(graphFunc === "remove-add-package"){
            if(index === '1') granularity.packages = false;
            else granularity.packages = true;
            diagram.nodes.filter(node => (node.data.type === 'variable' || node.data.type === 'field') && node.visible)
                         .each(node => removeAddPackage(node));
          } 
          else if(graphFunc === "remove-add-class"){
            if(index === '1') granularity.classes = false;
            else granularity.classes = true;
            diagram.nodes.filter(node => (node.data.type === 'variable' || node.data.type === 'field') && node.visible)
                         .each(node => removeAddClass(node));
          }
          else if(graphFunc === "remove-add-method"){
            if(index === '1') granularity.methods = false;
            else granularity.methods = true;
            diagram.nodes.filter(node => (node.data.type === 'variable' || node.data.type === 'field') && node.visible)
                         .each(node => removeAddMethod(node));
          } 
        }
        console.log(graphFunc + "功能耗时:" + (performance.now() - now));
      }
    )

    const granularity = {
      packages: true,
      classes: true,
      methods: true,
    }

    /** 
     *  移除/添加某个变量/字段节点的包粒度
     *  @param {Object} node - 节点
    */
    const removeAddPackage = (node) => {
      if(!granularity.packages){
        // remove
        diagram.startTransaction("removePackages");
        const parentPkg = diagram.findNodeForKey(node.data.realGroup[0]);
        if(parentPkg.data.visible){
          parentPkg.memberParts.each(member => diagram.model.setDataProperty(member.data, "group", undefined));
          diagram.model.setDataProperty(parentPkg.data, "visible", false);
        } 
        diagram.commitTransaction("removePackages");
      } else {
        // add
        diagram.startTransaction("addPackages");
        if(granularity.classes){
          const parentCls = diagram.findNodeForKey(node.data.realGroup[1]).containingGroup;
          diagram.model.setDataProperty(parentCls.data, "group", parentCls.data.realGroup[0]);
          if(!parentCls.containingGroup.visible){
            diagram.model.setDataProperty(parentCls.containingGroup.data, "visible", true);
            diagram.model.setDataProperty(parentCls.containingGroup.data, "isSubGraphExpanded", true);
          }
        }else if(granularity.methods){
          if(node.data.type === 'variable'){
            const parentMth = diagram.findNodeForKey(node.data.realGroup[2]);
            diagram.model.setDataProperty(parentMth.data, "group", parentMth.data.realGroup[0]);
            if(!parentMth.containingGroup.visible){
              diagram.model.setDataProperty(parentMth.containingGroup.data, "visible", true);
              diagram.model.setDataProperty(parentMth.containingGroup.data, "isSubGraphExpanded", true);
            }
          } else {
            diagram.model.setDataProperty(node.data, "group", node.data.realGroup[0]);
            if(!node.containingGroup.visible){
              diagram.model.setDataProperty(node.containingGroup.data, "visible", true);
              diagram.model.setDataProperty(node.containingGroup.data, "isSubGraphExpanded", true);
            }
          }
        }else{
          diagram.model.setDataProperty(node.data, "group", node.data.realGroup[0]);
          if(!node.containingGroup.visible){
            diagram.model.setDataProperty(node.containingGroup.data, "visible", true);
            diagram.model.setDataProperty(node.containingGroup.data, "isSubGraphExpanded", true);
          }
        }
        diagram.commitTransaction("addPackages");
      }
    }

    /** 
     *  移除/添加某个变量/字段节点的类粒度
     *  @param {Object} node - 节点
    */
    const removeAddClass = (node) => {
      if(!granularity.classes){
        // remove
        diagram.startTransaction("removeClasses");
        const parentCls = diagram.findNodeForKey(node.data.realGroup[1]).containingGroup;
        if(parentCls.visible){
          parentCls.memberParts.each(member => member.memberParts.each(m => diagram.model.setDataProperty(m.data, "group", parentCls.data.group)));
          diagram.model.setDataProperty(parentCls.data, "visible", false);
        }
        diagram.commitTransaction("removeClasses");
      } else {
        // add
        diagram.startTransaction("addClasses");
        if(granularity.packages){
          const parentCls = diagram.findNodeForKey(node.data.realGroup[1]).containingGroup;
          if(!parentCls.visible) diagram.model.setDataProperty(parentCls.data, "group", parentCls.data.realGroup[0]);
        }
        if(granularity.methods){
          let parentNode = node; // node.data.type === 'field'的情况
          if(node.data.type === 'variable') parentNode = node.containingGroup;
          diagram.model.setDataProperty(parentNode.data, "group", parentNode.data.realGroup[1]);
          if(!parentNode.containingGroup.containingGroup.visible){
            diagram.model.setDataProperty(parentNode.containingGroup.containingGroup.data, "visible", true);
            diagram.model.setDataProperty(parentNode.containingGroup.containingGroup.data, "isSubGraphExpanded", true);
          }
        }else{
          diagram.model.setDataProperty(node.data, "group", node.data.realGroup[1]);
          if(!node.containingGroup.containingGroup.visible) {
            diagram.model.setDataProperty(node.containingGroup.containingGroup.data, "visible", true);
            diagram.model.setDataProperty(node.containingGroup.containingGroup.data, "isSubGraphExpanded", true);
          }
        }
        diagram.commitTransaction("addClasses");
      }
    }

    /** 
     *  移除/添加某个变量/字段节点的方法粒度
     *  @param {Object} node - 节点
    */
    const removeAddMethod = (node) => {
      if(node.data.type === 'field') return;
      if(!granularity.methods){
        // remove
        diagram.startTransaction("removeMethods");
        const parentMth = diagram.findNodeForKey(node.data.realGroup[2]);
        parentMth.memberParts.each(member => diagram.model.setDataProperty(member.data, "group", parentMth.data.group));
        diagram.model.setDataProperty(parentMth.data, "visible", false);
        diagram.commitTransaction("removeMethods");
      } else {
        // add
        diagram.startTransaction("addMethods");
        diagram.model.setDataProperty(node.data, "group", node.data.realGroup[2]);
        if(!node.containingGroup.visible) {
          diagram.model.setDataProperty(node.containingGroup.data, "visible", true);
          diagram.model.setDataProperty(node.containingGroup.data, "isSubGraphExpanded", true);
          if(granularity.classes) diagram.model.setDataProperty(node.containingGroup.data, "group", node.containingGroup.data.realGroup[1]);
          else if(granularity.packages) diagram.model.setDataProperty(node.containingGroup.data, "group", node.containingGroup.data.realGroup[0]);
        }
        diagram.commitTransaction("addMethods");
      }
    }

    const getShortestPath = () => {
      if(selectedSourceSink.value[0] === null || selectedSourceSink.value[1] === null) return;
      const source = selectedSourceSink.value[0];
      const sink = selectedSourceSink.value[1];
      // paths.entry[0]: 当前节点，paths.entry[1]: 记录当前节点在路径的上一个节点
      const paths = new Map([[source, null]]);
      
      const workList = [ source ];
      while(workList.length != 0){
        const node = workList.shift();
        if(node.data.key === sink.data.key) break;
        node.findNodesOutOf().each(next => {
          if(paths.has(next)) return;
          paths.set(next, node);
          workList.push(next);
        })
      }

      diagram.startTransaction("emphasisShortestPath");
      diagram.nodes.filter(node => node.visible).each(node => {
        diagram.model.setDataProperty(node.data, "visible", false);
        diagram.model.setDataProperty(node.data, "isCollapsed", true);
      });
      let now = sink;
      while(now != null){
        diagram.model.setDataProperty(now.data, "visible", true);
        diagram.model.setDataProperty(now.data, "isCollapsed", false);
        const last = paths.get(now);
        now.findLinksBetween(last).each(link => diagram.model.setDataProperty(link.data, "visible", true));
        now = last;
      }
      diagram.commitTransaction("emphasisShortestPath");
      console.log("finish shortest path");
    }

    const getFrequencyNodePath = () => {
      if(selectedSourceSink.value[0] === null || selectedSourceSink.value[1] === null) return;
      const source = selectedSourceSink.value[0];
      const sink = selectedSourceSink.value[1];
      // paths.entry[0]: 当前节点，paths.entry[1]: 记录当前节点在路径的下一个节点
      
      const route = new Graph();
      diagram.nodes.each(node => {
        const tos = {};
        node.findNodesOutOf().each(n => tos[n.data.key] = 1 / (n.findLinksInto().count + 1));
        route.addNode(node.data.key, tos);
      })
      diagram.startTransaction("emphasisFrequencyPath");
      diagram.nodes.filter(node => node.visible).each(node => {
        diagram.model.setDataProperty(node.data, "visible", false);
        diagram.model.setDataProperty(node.data, "isCollapsed", true);
      });
      const paths = route.path(source.data.key, sink.data.key);
      for(let i = 0; i < paths.length - 1; i = i + 1){
        const from = diagram.findNodeForKey(paths[i]);
        const to = diagram.findNodeForKey(paths[i + 1]);
        if(!from.data.visible){
          diagram.model.setDataProperty(from.data, "visible", true);
          diagram.model.setDataProperty(from.data, "isCollapsed", false);
        }
        diagram.model.setDataProperty(to.data, "visible", true);
        diagram.model.setDataProperty(to.data, "isCollapsed", false);
        from.findLinksBetween(to).each(link => diagram.model.setDataProperty(link.data, "visible", true));
      }
      diagram.commitTransaction("emphasisFrequencyPath");
    }

    // 图表引用和GoJS实例
    const diagramRef = ref(null);
    let diagram = null;
    
    // 选中的节点详情
    const selectedNode = ref(null);
    const detailsVisible = ref(false);

    // GoJS全局构建符
    const $ = go.GraphObject.make;

    // 选中的要追踪的source([0])与sink([1])节点
    const selectedSourceSink = ref([null, null]);

    // 节点信息存储
    const nodeAttrs = ref(null);

    /**
     * 初始化GoJS图表
     */
    const initDiagram = () => {
      // 创建GoJS图表
      const start = performance.now();
      diagram = new go.Diagram(diagramRef.value, {
        "undoManager.isEnabled": true,  // 启用撤销/重做
        "toolManager.hoverDelay": 100,  // 鼠标悬停延迟
        "toolManager.toolTipDuration": 10000, // 工具提示显示时间
        "animationManager.isEnabled": false,
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
        },
        // 改变选择的节点后触发
        "ChangedSelection": (e) => {
          if(e.diagram.selection.size === 2){
            const node1 = e.diagram.selection.toArray()[0];
            const node2 = e.diagram.selection.toArray()[1];
            if(node1.data.isSource && node2.data.isSink){
              selectedSourceSink.value[0] = node1;
              selectedSourceSink.value[1] = node2;
            }else if(node2.data.isSource && node1.data.isSink){
              selectedSourceSink.value[0] = node2;
              selectedSourceSink.value[1] = node1;
            }
          }
        }
      });

      diagram.addModelChangedListener(function(e) {
        // 监听某个node的visible变化并lazy evaluation粒度展示
        if (e.propertyName === "visible" && (e.object.type === "variable" || e.object.type === "field")) {
          if(e.newValue === true){
            console.log("节点变为可见:", e.object);
            diagram.startTransaction("nodeVisibleTrue");
            const node = diagram.findNodeForKey(e.object.key);
            parentVisible(node, true);
            removeAddPackage(node);
            removeAddClass(node);
            removeAddMethod(node);
            diagram.commitTransaction("nodeVisibleTrue");
          }else{
            diagram.startTransaction("nodeVisibleFalse");
            const node = diagram.findNodeForKey(e.object.key);
            parentVisible(node, false);
            diagram.commitTransaction("nodeVisibleFalse");
          }
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
                // node.findNodesOutOf().each(next => parentVisible(next, true));
                node.findNodesOutOf().each(next => diagram.model.setDataProperty(next.data, "visible", true));
                node.findLinksOutOf().each(link => diagram.model.setDataProperty(link.data, "visible", true));
              }
              else {
                const collapseFrom = (start) => {
                  start.findLinksOutOf().each(link => diagram.model.setDataProperty(link.data, "visible", false));
                  start.findNodesOutOf().each(next => {
                    if(next.findLinksInto().any(link => link.visible) || !next.visible) return;
                    // parentVisible(next, false);
                    diagram.model.setDataProperty(next.data, "visible", false);
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
        },
        new go.Binding("stroke", "type", linkTypeToColor)),
        $(go.Shape, { 
          toArrow: "Standard",
          stroke: "#555",
          fill: "#555"
        },
        new go.Binding("fill", "type", linkTypeToColor)),
        $(go.TextBlock, {
          margin: new go.Margin(5, 5, 12, 5),
          font: "Bold 10px Sans-Serif",
          stroke: "#333",
          wrap: go.TextBlock.WrapFit,
          editable: false,
          alignment: go.Spot.Bottom
        },
        new go.Binding("text", "type")),
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

      const step1 = performance.now();
      console.log(`完成图模板创建，耗时${step1 - start}毫秒`);
      
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
      
      const step2 = performance.now();
      console.log(`完成图模型创建，耗时${step2 - start}毫秒`);
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
     * 根据节点类型确定颜色
     * @param {Object} type - 边类型
     * @returns {string} 颜色值
     */
    const linkTypeToColor = (type) => {
      switch(type){
        case 'RETURN': 
        case 'THIS_PASSING': 
        case 'PARAMETER_PASSING': return "#77DD77"; // 森林绿
        case 'INSTANCE_LOAD': 
        case 'INSTANCE_STORE': return '#B399D4'; // 浅紫
        case 'ARRAY_LOAD':
        case 'ARRAY_STORE': return '#3A4F8C'; // 深海蓝
        case 'LOCAL_ASSIGN': return '#FFB347'; // 浅橙
        case 'OTHER': return '#696969'; // 中灰
        default: return "black";
      }
    }
    
    /**
     * 加载图数据
     */
    const loadGraphData = () => {
      if (!props.graphData || !diagram) return;
      
      const nodeDataArray = [];
      const linkDataArray = [];
      
      const start = performance.now();
      // 处理图数据
      processGraphData(nodeDataArray, linkDataArray);
      const step1 = performance.now();
      console.log(`In loadGraphData: 完成图数据处理，耗时${step1 - start}毫秒`);
      // 设置图表模型
      diagram.model = new go.GraphLinksModel({
        nodeDataArray: nodeDataArray,
        linkDataArray: linkDataArray
      });
      console.log(`In loadGraphData: 完成图渲染，耗时${performance.now() - step1}毫秒`);

      // 执行布局
      diagram.layoutDiagram(true);
    };
    
    /**
     * 处理图数据，生成节点和链接
     * @param {Array} nodeDataArray - 节点数组引用
     * @param {Array} linkDataArray - 链接数组引用
     */
    const processGraphData = (nodeDataArray, linkDataArray) => {
      const { metadata, relation, sourceNodes, sinkNodes, nodeAttributes, edgeAttributeMap } = props.graphData;
      nodeAttrs.value = nodeAttributes;
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
      if (edgeAttributeMap) {
        for (const [from, toMap] of Object.entries(edgeAttributeMap)) {
          for(const [to, attribute] of Object.entries(toMap)) {
            // 添加节点间的链接
            linkDataArray.push({
              from: `n_${from}`,
              to: `n_${to}`,
              relationship: "connects",
              visible: false,
              type: attribute.type,
              callSite: attribute.callSiteInfo
            });
          }
        }
      }

      // // 处理图边关系
      // if (graph) {
      //   for (const [from, toArray] of Object.entries(graph)) {
      //     toArray.forEach(to => {
      //       // 添加节点间的链接
      //       linkDataArray.push({
      //         from: `n_${from}`,
      //         to: `n_${to}`,
      //         relationship: "connects",
      //         visible: false,
      //       });
      //     });
      //   }
      // }
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
     * 获取节点详细数据
     * @param {go.Node} goNode - GoJS节点对象
     * @returns {Object} 扩展的节点数据
     */
    const getNodeData = (goNode) => {
      if (!goNode || !(goNode.data.type === 'variable' || goNode.data.type === 'field')) return null;
      
      const attr = nodeAttrs.value[parseInt(goNode.data.key.split("_")[1])];
      const neighbors = [];
      const taintInfos = [];
      
      // 获取相关的节点
      goNode.findLinksConnected().each(link => {
        const otherNode = link.getOtherNode(goNode);
        if (otherNode) {
          neighbors.push({
            id: otherNode.data.key,
            type: nodeAttrs.value[parseInt(otherNode.data.key.split("_")[1])].type,
            name: nodeInfoToLabel(otherNode.data)
          });
        }
      });

      // 污点对象信息
      const length = attr.taintObjs.length;
      for(let i = 0; i < length; i++){
        taintInfos.push({
          taintObj: attr.taintObjs[i],
          taintSource: attr.taintSources[i]
        })
      }
      
      return {
        id: goNode.data.key,
        name: attr.varOrFieldName,
        type: attr.type,
        className: attr.className,
        methodName: attr.methodName,
        isSource: goNode.data.isSource,
        isSink: goNode.data.isSink,
        neighbors: neighbors,
        taintInfos: taintInfos,
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
     * @param {string} nodeId - 邻接节点ID
     * @param {string} selectedNodeId - 当前节点ID
     */
    const focusNode = (nodeId, selectedNodeId) => {
      if (diagram) {
        const node = diagram.findNodeForKey(nodeId);
        const selectedNode = diagram.findNodeForKey(selectedNodeId);
        if (node) {
          if(!node.visible) {
            diagram.startTransaction("focusNode");
            diagram.model.setDataProperty(node.data, "visible", true);
            diagram.model.setDataProperty(node.data, "isCollapsed", false);
            node.findLinksBetween(selectedNode).each(link => diagram.model.setDataProperty(link.data, "visible", true));
            diagram.commitTransaction("focusNode");
          }
          diagram.select(node);
          diagram.commandHandler.scrollToPart(node);
        }
      }
    };
    
    onMounted(() => {
      nextTick(() => {
        initDiagram();
      });
    });
    
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