<template>
  <div class="sidebar-container" :class="{ 'collapsed': isCollapsed }">
    <!-- 侧边栏头部 -->
    <div class="sidebar-header">
      <img src="../assets/2-3.png" class="logo">
      <span v-if="!isCollapsed" class="app-name">TFG 可视化</span>
      <el-icon 
        class="collapse-btn" 
        @click="toggleCollapse">
        <component :is="isCollapsed ? 'Expand' : 'Fold'" />
      </el-icon>
    </div>
    
    <!-- 侧边栏菜单 -->
    <el-menu
      :default-active="activeItem"
      class="sidebar-menu"
      :collapse="isCollapsed"
      @select="handleMenuSelect">
      
      <el-menu-item index="upload">
        <el-icon><UploadFilled /></el-icon>
        <template #title>上传文件</template>
      </el-menu-item>
      
      <el-menu-item index="parse" :disabled="!hasUploadedFile">
        <el-icon><Document /></el-icon>
        <template #title>解析数据</template>
      </el-menu-item>
      
      <!-- <el-menu-item index="visualize" :disabled="!hasParsedData">
        <el-icon><DataAnalysis /></el-icon>
        <template #title>可视化展示</template>
      </el-menu-item> -->

      <el-sub-menu v-if="functionButtons" index="visualize"  :disabled="!hasParsedData">
        <template #title>
          <el-icon><DataAnalysis /></el-icon>
          <span>可视化展示</span>
        </template>

        <!-- 视图粒度分类 -->
        <el-sub-menu index="view-granularity">
          <template #title>视图粒度</template>
          <el-menu-item index="remove-add-package">{{ functionButtons.removeAddPackages.items[functionButtons.removeAddPackages.index] }}</el-menu-item>
          <el-menu-item index="remove-add-class">{{ functionButtons.removeAddClasses.items[functionButtons.removeAddClasses.index] }}</el-menu-item>
          <el-menu-item index="remove-add-method">{{ functionButtons.removeAddMethods.items[functionButtons.removeAddMethods.index] }}</el-menu-item>
        </el-sub-menu>

        <!-- 路径推荐分类 -->
        <el-sub-menu index="path-recommend">
          <template #title>路径推荐</template>
          <el-menu-item index="shortest-path">{{ functionButtons.shortestPath }}</el-menu-item>
        </el-sub-menu>
      </el-sub-menu>
      
      <el-menu-item index="help">
        <el-icon><InfoFilled /></el-icon>
        <template #title>使用帮助</template>
      </el-menu-item>
    </el-menu>
    
    <!-- 底部状态信息 -->
    <div v-if="!isCollapsed" class="sidebar-footer">
      <div v-if="hasUploadedFile" class="file-info">
        <el-icon><Document /></el-icon>
        <span class="file-name">{{ uploadedFileName }}</span>
      </div>
      <div v-else class="no-file">
        尚未上传文件
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed } from 'vue'
import {
  Fold,
  Expand,
  UploadFilled,
  Document,
  DataAnalysis,
  Setting,
  Brush,
  SetUp,
  InfoFilled
} from '@element-plus/icons-vue'

export default {
  name: 'AppSidebar',
  components: {
    Fold,
    Expand,
    UploadFilled,
    Document,
    DataAnalysis,
    Setting,
    Brush,
    SetUp,
    InfoFilled
  },
  props: {
    /**
     * 上传的文件名
     */
    uploadedFileName: {
      type: String,
      default: ''
    },
    /**
     * 是否已上传文件
     */
    hasUploadedFile: {
      type: Boolean,
      default: false
    },
    /**
     * 是否已解析数据
     */
    hasParsedData: {
      type: Boolean,
      default: false
    },
    /**
     * 当前活动菜单项
     */
    activeMenuItem: {
      type: String,
      default: 'upload'
    }
  },
  emits: ['menu-select', 'visual-function-select'],
  setup(props, { emit }) {
    // 侧边栏折叠状态
    const isCollapsed = ref(false);
    
    // 当前活动菜单项
    const activeItem = computed(() => props.activeMenuItem);

    // 可视化功能按钮
    const functionButtons = ref({
      removeAddPackages: {items: ["移除包粒度", "添加包粒度"], index: 0},
      removeAddClasses: {items: ["移除类粒度", "添加类粒度"], index: 0},
      removeAddMethods: {items: ["移除方法粒度", "添加方法粒度"], index: 0},
      shortestPath: "最短路",
    });
    
    /**
     * 切换侧边栏折叠状态
     */
    const toggleCollapse = () => {
      isCollapsed.value = !isCollapsed.value;
    };
    
    /**
     * 处理菜单项选择
     * @param {string} index - 菜单项索引
     */
    const handleMenuSelect = (index, indexPath) => {
      if(indexPath.length === 1) emit('menu-select', index);
      else{
        let state = "";
        if(index === "remove-add-package") {
          functionButtons.value.removeAddPackages.index = 1 - functionButtons.value.removeAddPackages.index;
          state = functionButtons.value.removeAddPackages.index;
        }
        else if(index === "remove-add-class") {
          functionButtons.value.removeAddClasses.index = 1 - functionButtons.value.removeAddClasses.index;
          state = functionButtons.value.removeAddClasses.index;
        }
        else if(index === "remove-add-method") {
          functionButtons.value.removeAddMethods.index = 1 - functionButtons.value.removeAddMethods.index;
          state = functionButtons.value.removeAddMethods.index;
        }
        emit('visual-function-select', index + state);
      }
    };
    
    return {
      isCollapsed,
      activeItem,
      functionButtons,
      toggleCollapse,
      handleMenuSelect
    };
  }
}
</script>

<style scoped>
.sidebar-container {
  height: 100vh;
  background-color: #304156;
  transition: width 0.3s;
  width: 250px;
  display: flex;
  flex-direction: column;
}

.sidebar-container.collapsed {
  width: 64px;
}

.sidebar-header {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 15px;
  color: white;
  border-bottom: 1px solid #1f2d3d;
}

.logo {
  width: 24px;
  height: 24px;
  margin-right: 10px;
}

.app-name {
  font-size: 18px;
  font-weight: 600;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.collapse-btn {
  cursor: pointer;
  font-size: 20px;
}

.sidebar-menu {
  flex: 1;
  border-right: none;
  background-color: #304156;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 100%;
}

:deep(.el-menu) {
  border-right: none;
}

:deep(.el-menu-item), :deep(.el-sub-menu__title) {
  color: #888ba7;
}

:deep(.el-menu-item:hover), :deep(.el-sub-menu__title:hover) {
  background-color: #263445;
}

:deep(.el-menu-item.is-active) {
  color: #409EFF;
  background-color: #263445;
}

.sidebar-footer {
  height: 40px;
  border-top: 1px solid #1f2d3d;
  display: flex;
  align-items: center;
  padding: 0 15px;
  color: #bfcbd9;
  font-size: 12px;
}

.file-info {
  display: flex;
  align-items: center;
  width: 100%;
}

.file-name {
  margin-left: 5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.no-file {
  text-align: center;
  width: 100%;
  color: #909399;
}
</style>