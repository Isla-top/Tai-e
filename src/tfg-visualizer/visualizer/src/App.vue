<template>
  <div class="app-container">
    <!-- 侧边栏 -->
    <AppSidebar 
      :uploaded-file-name="uploadedFileName"
      :has-uploaded-file="!!uploadedFile"
      :has-parsed-data="!!parsedData"
      :active-menu-item="currentStep"
      @menu-select="handleMenuSelect"
      @visual-function-select="handleFunctionSelect"
    />
    
    <!-- 主内容区域 -->
    <div class="main-content">
      <!-- 顶部导航 -->
      <el-page-header 
        :title="getHeaderTitle()" 
        :content="getHeaderContent()" 
        @back="handleBack"
      />
      
      <!-- 内容区域 -->
      <div class="content-area">
        <!-- 上传组件 -->
        <div v-if="currentStep === 'upload'" class="component-container">
          <FileUploader @file-parsed="handleFileParsed" />
        </div>
        
        <!-- 解析组件 -->
        <div v-else-if="currentStep === 'parse'" class="component-container">
          <YmlParser 
            v-if="uploadedFile" 
            :file-data="uploadedFile" 
            @parsed-data="handleDataParsed"
            @back-to-upload="currentStep = 'upload'" 
          />
        </div>
        
        <!-- 可视化组件 -->
        <div v-else-if="currentStep === 'visualize'" class="component-container">
          <GraphVisualizer 
            v-if="parsedData" 
            :graph-function="currentFunction"
            :graph-data="parsedData" 
          />
        </div>
        
        <!-- 帮助页面 -->
        <div v-else-if="currentStep === 'help'" class="component-container">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>使用帮助</span>
              </div>
            </template>
            <div class="help-content">
              <h3>YML 可视化工具使用指南</h3>
              
              <el-divider />
              
              <h4>1. 上传文件</h4>
              <p>在"上传文件"页面，您可以通过拖拽或点击上传按钮来上传 YML 格式的图结构文件。</p>
              <p>支持的文件格式：.yml 和 .yaml</p>
              
              <h4>2. 解析数据</h4>
              <p>上传文件后，系统会自动解析 YML 内容，并展示解析结果的摘要信息。</p>
              <p>您可以查看解析出的包、类、方法等数量信息。</p>
              
              <h4>3. 可视化展示</h4>
              <p>解析完成后，您可以进入可视化界面，查看完整的图结构。</p>
              <p>可视化界面支持以下功能：</p>
              <ul>
                <li>切换不同视图（完整图、包关系、类关系、方法关系）</li>
                <li>放大/缩小/重置缩放</li>
                <li>点击节点查看详情</li>
                <li>在详情面板中浏览相关节点</li>
              </ul>
              
              <h4>4. 图例说明</h4>
              <div class="legend-help">
                <div class="legend-item">
                  <div class="legend-color" style="background-color: #CCE5FF;"></div>
                  <span>包 (Package) - 蓝色</span>
                </div>
                <div class="legend-item">
                  <div class="legend-color" style="background-color: #D5F5E3;"></div>
                  <span>类 (Class) - 绿色</span>
                </div>
                <div class="legend-item">
                  <div class="legend-color" style="background-color: #FCF3CF;"></div>
                  <span>方法 (Method) - 黄色</span>
                </div>
                <div class="legend-item">
                  <div class="legend-color" style="background-color: #F5CBA7;"></div>
                  <span>变量/字段 (Variable/Field) - 橙色</span>
                </div>
                <div class="legend-item">
                  <div class="legend-color" style="background-color: #FFCCE5;"></div>
                  <span>源节点 (Source) - 粉色</span>
                </div>
                <div class="legend-item">
                  <div class="legend-color" style="background-color: #FFD700;"></div>
                  <span>汇节点 (Sink) - 金色</span>
                </div>
              </div>
            </div>
          </el-card>
        </div>
        
        <!-- 设置页面 -->
        <div v-else-if="currentStep === 'theme' || currentStep === 'layout'" class="component-container">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>{{ currentStep === 'theme' ? '主题设置' : '布局设置' }}</span>
              </div>
            </template>
            <div class="settings-content">
              <p>该功能正在开发中，敬请期待！</p>
            </div>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import AppSidebar from './components/AppSidebar.vue'
import FileUploader from './components/FileUploader.vue'
import YmlParser from './components/YmlParser.vue'
import GraphVisualizer from './components/GraphVisualizer.vue'

export default {
  name: 'App',
  components: {
    AppSidebar,
    FileUploader,
    YmlParser,
    GraphVisualizer
  },
  setup() {
    // 当前步骤/页面
    const currentStep = ref('upload');
    
    // 上传的文件数据
    const uploadedFile = ref(null);
    const uploadedFileName = ref('');
    
    // 解析后的数据
    const parsedData = ref(null);

    // 可视化图功能
    const currentFunction = ref('');
    
    /**
     * 处理文件解析
     * @param {Object} fileData - 文件数据对象
     */
    const handleFileParsed = (fileData) => {
      uploadedFile.value = fileData;
      uploadedFileName.value = fileData.filename;
      currentStep.value = 'parse';
    };
    
    /**
     * 处理数据解析完成
     * @param {Object} data - 解析后的数据
     */
    const handleDataParsed = (data) => {
      parsedData.value = data;
      currentStep.value = 'visualize';
    };
    
    /**
     * 处理菜单选择
     * @param {string} menuItem - 菜单项名称
     */
    const handleMenuSelect = (menuItem) => {
      currentStep.value = menuItem;
    };

    /**
     * 可视化功能选择
     * @param {string} functionItem - 功能项名称
     */
    const handleFunctionSelect = (functionItem) => {
      currentFunction.value = functionItem;
    }
    
    /**
     * 获取页面标题
     */
    const getHeaderTitle = () => {
      switch (currentStep.value) {
        case 'upload': return 'TFG 可视化';
        case 'parse': return '数据解析';
        case 'visualize': return '图结构可视化';
        case 'help': return '使用帮助';
        default: return 'TFG 可视化';
      }
    };
    
    /**
     * 获取页面子标题
     */
    const getHeaderContent = () => {
      switch (currentStep.value) {
        case 'upload': return '上传 YML 文件';
        case 'parse': return '解析 YML 结构';
        case 'visualize': return '查看图结构';
        case 'help': return '使用指南';
        default: return '';
      }
    };
    
    /**
     * 处理返回按钮点击
     */
    const handleBack = () => {
      switch (currentStep.value) {
        case 'parse':
          currentStep.value = 'upload';
          break;
        case 'visualize':
          currentStep.value = 'parse';
          break;
        case 'help':
          currentStep.value = 'upload';
          break;
        default:
          break;
      }
    };
    
    return {
      currentStep,
      uploadedFile,
      uploadedFileName,
      parsedData,
      currentFunction,
      handleFileParsed,
      handleDataParsed,
      handleMenuSelect,
      handleFunctionSelect,
      getHeaderTitle,
      getHeaderContent,
      handleBack
    };
  }
}
</script>

<style>
/* 全局样式 */
body {
  margin: 0;
  padding: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}

#app {
  width: 100%;
  height: 100%;
}
</style>

<style scoped>
.app-container {
  display: flex;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

.main-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #f5f7fa;
}

.content-area {
  margin-top: 20px;
}

.component-container {
  margin: 10px 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.help-content {
  padding: 10px;
}

.help-content h3 {
  text-align: center;
  margin-bottom: 20px;
}

.help-content h4 {
  margin-top: 20px;
  margin-bottom: 10px;
  color: #409EFF;
}

.help-content p, .help-content ul {
  margin: 10px 0;
  line-height: 1.6;
}

.legend-help {
  margin: 20px 0;
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
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

.settings-content {
  padding: 40px;
  text-align: center;
  color: #909399;
}
</style>