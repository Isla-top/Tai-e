<template>
  <div class="parser-container">
    <!-- 解析状态显示 -->
    <el-card v-if="isLoading">
      <div class="loading-container">
        <el-progress type="circle" :percentage="parseProgress" />
        <div class="parse-status">{{ parseStatus }}</div>
      </div>
    </el-card>

    <!-- 解析成功后的概要信息 -->
    <el-card v-else-if="parsedData" class="result-card">
      <template #header>
        <div class="card-header">
          <span>解析结果</span>
          <el-button type="primary" size="small" @click="visualizeData">可视化</el-button>
        </div>
      </template>

      <!-- 解析数据的概览 -->
      <el-descriptions border direction="vertical" :column="2">
        <el-descriptions-item label="包的数量">{{ getPackagesCount() }}</el-descriptions-item>
        <el-descriptions-item label="类的数量">{{ getClassesCount() }}</el-descriptions-item>
        <el-descriptions-item label="方法的数量">{{ getMethodsCount() }}</el-descriptions-item>
        <el-descriptions-item label="关系数量">{{ getRelationsCount() }}</el-descriptions-item>
        <el-descriptions-item label="节点数量">{{ getNodesCount() }}</el-descriptions-item>
        <el-descriptions-item label="边数量">{{ getEdgesCount() }}</el-descriptions-item>
      </el-descriptions>

      <!-- 解析数据的JSON预览 -->
      <div class="data-preview">
        <el-collapse>
          <el-collapse-item title="数据预览" name="preview">
            <div class="json-preview">
              <pre>{{ getFormattedPreview() }}</pre>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>
    </el-card>

    <!-- 解析失败的错误信息 -->
    <el-card v-else-if="parseError" class="error-card">
      <el-result
        icon="error"
        title="解析失败"
        :sub-title="parseError">
        <template #extra>
          <el-button type="primary" @click="$emit('back-to-upload')">返回上传</el-button>
        </template>
      </el-result>
    </el-card>
  </div>
</template>

<script>
import { ref } from 'vue'
import yaml from 'js-yaml'

export default {
  name: 'YmlParser',
  props: {
    fileData: {
      type: Object,
      required: true
    }
  },
  emits: ['parsed-data', 'back-to-upload'],
  setup(props, { emit }) {
    // 解析后的数据
    const parsedData = ref(null);
    // 加载状态
    const isLoading = ref(true);
    // 解析进度
    const parseProgress = ref(0);
    // 解析状态描述
    const parseStatus = ref('解析中...');
    // 解析错误
    const parseError = ref(null);

    /**
     * 解析YML文件
     */
    const parseYmlFile = async () => {
      try {
        isLoading.value = true;
        parseProgress.value = 10;
        parseStatus.value = '读取YML内容...';
        
        // 延迟执行，让UI能够更新
        await new Promise(resolve => setTimeout(resolve, 200));
        // 使用js-yaml解析YML内容
        parseProgress.value = 30;
        parseStatus.value = '解析YML结构...';
        const ymlContent = props.fileData.content;
        const ymlData = yaml.load(ymlContent);
        await new Promise(resolve => setTimeout(resolve, 200));
        
        // 转换为图结构
        parseProgress.value = 60;
        parseStatus.value = '构建图结构...';
        
        // 处理数据，构建图结构
        const graphData = processYmlData(ymlData);
        
        parseProgress.value = 90;
        parseStatus.value = '完成解析...';
        
        await new Promise(resolve => setTimeout(resolve, 200));
        
        // 设置解析完成的数据
        parsedData.value = graphData;
        parseProgress.value = 100;
        parseStatus.value = '解析完成';
        
        await new Promise(resolve => setTimeout(resolve, 300));
        isLoading.value = false;
      } catch (error) {
        console.error('解析YML文件时出错:', error);
        parseError.value = error.message || '解析文件时发生未知错误';
        isLoading.value = false;
      }
    };

    /**
     * 处理YML数据，构建图结构
     * @param {Object} ymlData - 解析的YML数据对象
     * @returns {Object} 图结构
     */
    const processYmlData = (ymlData) => {
      // 确保数据结构完整
      if (!ymlData || !ymlData.metadata || !ymlData.relation || !ymlData.graph) {
        throw new Error('YML文件结构不完整，缺少必要的字段');
      }

      // 构建图结构
      return {
        metadata: ymlData.metadata || {},
        packages: ymlData.metadata?.packages || [],
        classes: ymlData.metadata?.classes || [],
        methods: ymlData.metadata?.methods || [],
        varsAndFields: ymlData.metadata?.varsAndFields || [],
        relation: {
          packageToClasses: ymlData.relation?.packageToClasses || {},
          classToMethods: ymlData.relation?.classToMethods || {},
          classToFields: ymlData.relation?.classToFields || {},
          methodToVars: ymlData.relation?.methodToVars || {},
        },
        graph: ymlData.graph || {},
        sourceNodes: ymlData.sourceNodes || [],
        sinkNodes: ymlData.sinkNodes || [],
        nodeAttributes: ymlData.nodeAttributes || [],
        edgeAttributeMap: ymlData.edgeAttributeMap || {}
      };
    };

    /**
     * 获取包的数量
     */
    const getPackagesCount = () => {
      return parsedData.value?.packages?.length || 0;
    };

    /**
     * 获取类的数量
     */
    const getClassesCount = () => {
      return parsedData.value?.classes?.length || 0;
    };

    /**
     * 获取方法的数量
     */
    const getMethodsCount = () => {
      return parsedData.value?.methods?.length || 0;
    };

    /**
     * 获取关系数量
     */
    const getRelationsCount = () => {
      const packageRelations = Object.keys(parsedData.value?.relation?.packageToClasses || {}).length;
      const classRelations = Object.keys(parsedData.value?.relation?.classToMethods || {}).length + Object.keys(parsedData.value?.relation?.classToFields || {}).length;
      const methodRelations = Object.keys(parsedData.value?.relation?.methodToVars || {}).length;
      return packageRelations + classRelations + methodRelations;
    };

    /**
     * 获取节点数量
     */
    const getNodesCount = () => {
      return Object.keys(parsedData.value?.graph || {}).length;
    };

    /**
     * 获取边的数量
     */
    const getEdgesCount = () => {
      let count = 0;
      const graph = parsedData.value?.graph || {};
      
      for (const nodeId in graph) {
        count += graph[nodeId].length;
      }
      
      return count;
    };

    /**
     * 获取格式化的数据预览
     */
    const getFormattedPreview = () => {
      if (!parsedData.value) return '';
      
      // 创建一个简化版本以便预览
      const preview = {
        packages: parsedData.value.packages.length,
        classes: parsedData.value.classes.length,
        methods: parsedData.value.methods.length,
        nodeCount: getNodesCount(),
        edgeCount: getEdgesCount(),
        // 添加一些示例数据
        samplePackages: parsedData.value.packages.slice(0, 3),
        sampleClasses: parsedData.value.classes.slice(0, 3),
        sampleMethods: parsedData.value.methods.slice(0, 3)
      };
      
      return JSON.stringify(preview, null, 2);
    };

    /**
     * 将解析的数据发送给可视化组件
     */
    const visualizeData = () => {
      if (parsedData.value) {
        emit('parsed-data', parsedData.value);
      }
    };

    // 当组件加载时，立即开始解析
    parseYmlFile();

    return {
      parsedData,
      isLoading,
      parseProgress,
      parseStatus,
      parseError,
      getPackagesCount,
      getClassesCount,
      getMethodsCount,
      getRelationsCount,
      getNodesCount,
      getEdgesCount,
      getFormattedPreview,
      visualizeData
    };
  }
}
</script>

<style scoped>
.parser-container {
  padding: 20px 0;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 0;
}

.parse-status {
  margin-top: 20px;
  font-size: 16px;
  color: #606266;
}

.result-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.data-preview {
  margin-top: 20px;
}

.json-preview {
  background-color: #f8f8f8;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
}

.json-preview pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.error-card {
  margin-top: 20px;
}
</style>