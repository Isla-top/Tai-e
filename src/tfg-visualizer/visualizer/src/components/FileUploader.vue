<template>
  <div class="file-uploader">
    <!-- 上传区域容器 -->
    <el-card class="uploader-card">
      <div 
        class="upload-area"
        :class="{ 'is-dragover': isDragover }"
        @dragover.prevent="handleDragover"
        @dragleave.prevent="handleDragleave"
        @drop.prevent="handleDrop">
        
        <!-- 上传图标 -->
        <el-icon class="upload-icon"><Upload /></el-icon>
        
        <!-- 上传文本提示 -->
        <div class="upload-text">
          <span>将 YML 文件拖到此处，或</span>
          <el-button type="primary" size="small" @click="triggerFileInput">点击上传</el-button>
        </div>
        
        <!-- 文件类型提示 -->
        <div class="upload-tip">
          只支持 .yml 或 .yaml 文件
        </div>
        
        <!-- 隐藏的文件输入框 -->
        <input 
          ref="fileInput" 
          type="file" 
          accept=".yml,.yaml" 
          style="display: none" 
          @change="handleFileChange"
        />
      </div>
      
      <!-- 已上传文件显示区域 -->
      <div v-if="uploadedFile" class="uploaded-file">
        <el-row align="middle" :gutter="12">
          <el-col :span="18">
            <div class="file-info">
              <el-icon><Document /></el-icon>
              <span class="file-name">{{ uploadedFile.name }}</span>
              <span class="file-size">({{ formatFileSize(uploadedFile.size) }})</span>
            </div>
          </el-col>
          <el-col :span="6" style="text-align: right">
            <el-button type="success" size="small" @click="handleParseFile">
              解析文件
            </el-button>
          </el-col>
        </el-row>
      </div>
    </el-card>
  </div>
</template>

<script>
import { ref } from 'vue'
import { Upload, Document } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'FileUploader',
  components: {
    Upload,
    Document
  },
  emits: ['file-parsed'],
  setup(props, { emit }) {
    // 文件输入引用
    const fileInput = ref(null)
    // 上传的文件
    const uploadedFile = ref(null)
    // 是否处于拖放状态
    const isDragover = ref(false)

    /**
     * 处理拖动进入事件
     */
    const handleDragover = () => {
      isDragover.value = true
    }

    /**
     * 处理拖动离开事件
     */
    const handleDragleave = () => {
      isDragover.value = false
    }

    /**
     * 处理文件拖放事件
     * @param {DragEvent} event - 拖放事件对象
     */
    const handleDrop = (event) => {
      isDragover.value = false
      const files = event.dataTransfer.files
      if (files.length > 0) {
        validateAndSetFile(files[0])
      }
    }

    /**
     * 触发文件输入点击
     */
    const triggerFileInput = () => {
      fileInput.value.click()
    }

    /**
     * 处理文件选择变更
     * @param {Event} event - 文件输入事件
     */
    const handleFileChange = (event) => {
      const files = event.target.files
      if (files.length > 0) {
        validateAndSetFile(files[0])
      }
    }

    /**
     * 验证并设置文件
     * @param {File} file - 要验证的文件
     */
    const validateAndSetFile = (file) => {
      const fileName = file.name.toLowerCase()
      if (fileName.endsWith('.yml') || fileName.endsWith('.yaml')) {
        uploadedFile.value = file
      } else {
        // 使用 Element Plus 的消息通知
        ElMessage.error('请上传 YML 或 YAML 文件')
      }
    }

    /**
     * 格式化文件大小
     * @param {number} size - 文件大小（字节）
     * @returns {string} 格式化后的大小
     */
    const formatFileSize = (size) => {
      if (size < 1024) {
        return size + ' B'
      } else if (size < 1024 * 1024) {
        return (size / 1024).toFixed(2) + ' KB'
      } else {
        return (size / (1024 * 1024)).toFixed(2) + ' MB'
      }
    }

    /**
     * 处理文件解析
     */
    const handleParseFile = () => {
      if (!uploadedFile.value) return

      const reader = new FileReader()
      reader.onload = (e) => {
        try {
          // 将文件内容作为字符串传递
          emit('file-parsed', {
            content: e.target.result,
            filename: uploadedFile.value.name
          })
        } catch (error) {
          ElMessage.error('解析文件时出错: ' + error.message)
        }
      }
      reader.onerror = () => {
        ElMessage.error('读取文件时出错')
      }
      reader.readAsText(uploadedFile.value)
    }

    return {
      fileInput,
      uploadedFile,
      isDragover,
      handleDragover,
      handleDragleave,
      handleDrop,
      triggerFileInput,
      handleFileChange,
      formatFileSize,
      handleParseFile
    }
  }
}
</script>

<style scoped>
.file-uploader {
  width: 100%;
}

.uploader-card {
  margin: 20px 0;
}

.upload-area {
  padding: 40px 20px;
  border: 2px dashed #e0e0e0;
  border-radius: 4px;
  cursor: pointer;
  text-align: center;
  transition: all 0.3s;
}

.upload-area.is-dragover {
  border-color: #409EFF;
  background-color: rgba(64, 158, 255, 0.05);
}

.upload-icon {
  font-size: 48px;
  color: #909399;
  margin-bottom: 10px;
}

.upload-text {
  margin: 10px 0;
  color: #606266;
}

.upload-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 10px;
}

.uploaded-file {
  margin-top: 20px;
  padding: 10px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
}

.file-info {
  display: flex;
  align-items: center;
}

.file-name {
  margin: 0 10px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-size {
  font-size: 12px;
  color: #909399;
}
</style>