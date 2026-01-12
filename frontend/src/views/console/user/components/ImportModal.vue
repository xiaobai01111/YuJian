<template>
  <dialog ref="modalRef" class="modal">
    <div class="modal-box">
      <form method="dialog">
        <button class="btn btn-sm btn-circle btn-ghost absolute right-2 top-2">✕</button>
      </form>
      <h3 class="font-bold text-lg mb-4">导入用户</h3>
      
      <div class="space-y-4">
        <!-- 下载模板 -->
        <div class="alert bg-base-200">
          <svg xmlns="http://www.w3.org/2000/svg" class="stroke-info shrink-0 w-6 h-6" fill="none" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
          </svg>
          <div>
            <div class="text-sm">请先下载导入模板，按格式填写后上传</div>
            <button class="btn btn-link btn-xs text-primary p-0" @click="downloadTemplate">下载模板</button>
          </div>
        </div>
        
        <!-- 上传区域 -->
        <div 
          class="border-2 border-dashed border-base-300 rounded-lg p-8 text-center hover:border-primary transition-colors cursor-pointer"
          :class="{ 'border-primary bg-primary/5': isDragging }"
          @click="triggerFileInput"
          @dragover.prevent="isDragging = true"
          @dragleave.prevent="isDragging = false"
          @drop.prevent="handleDrop"
        >
          <input ref="fileInput" type="file" accept=".xlsx,.xls" class="hidden" @change="handleFileChange" />
          <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 mx-auto text-base-content/30 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
          </svg>
          <p class="text-base-content/60">点击或拖拽文件到此处上传</p>
          <p class="text-xs text-base-content/40 mt-1">支持 .xlsx, .xls 格式</p>
        </div>
        
        <!-- 已选文件 -->
        <div v-if="selectedFile" class="flex items-center gap-2 p-3 bg-base-200 rounded-lg">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-success" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <span class="flex-1 text-sm truncate">{{ selectedFile.name }}</span>
          <button class="btn btn-ghost btn-xs btn-circle" @click.stop="clearFile">✕</button>
        </div>
        
        <!-- 选项 -->
        <label class="flex items-center gap-2 cursor-pointer">
          <input type="checkbox" v-model="updateExisting" class="checkbox checkbox-sm checkbox-primary" />
          <span class="text-sm">更新已存在的用户数据</span>
        </label>
      </div>

      <div class="modal-action">
        <button class="btn btn-ghost" @click="close">取消</button>
        <button class="btn btn-primary" @click="handleImport" :disabled="!selectedFile || loading">
          <span v-if="loading" class="loading loading-spinner loading-sm"></span>
          开始导入
        </button>
      </div>
    </div>
    <form method="dialog" class="modal-backdrop">
      <button>close</button>
    </form>
  </dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { importUsers, downloadUserTemplate } from '@/api/system'

const emit = defineEmits<{
  (e: 'success'): void
}>()

const modalRef = ref<HTMLDialogElement>()
const fileInput = ref<HTMLInputElement>()
const loading = ref(false)
const isDragging = ref(false)
const selectedFile = ref<File | null>(null)
const updateExisting = ref(false)

const open = () => {
  selectedFile.value = null
  updateExisting.value = false
  modalRef.value?.showModal()
}

const close = () => {
  modalRef.value?.close()
}

const triggerFileInput = () => {
  fileInput.value?.click()
}

const handleFileChange = (e: Event) => {
  const target = e.target as HTMLInputElement
  const files = target.files
  if (files && files.length > 0 && files[0]) {
    selectedFile.value = files[0]
  }
}

const handleDrop = (e: DragEvent) => {
  isDragging.value = false
  const files = e.dataTransfer?.files
  if (files && files.length > 0) {
    const file = files[0]
    if (file && file.name.match(/\.xlsx?$/)) {
      selectedFile.value = file
    } else {
      alert('请上传 Excel 文件')
    }
  }
}

const clearFile = () => {
  selectedFile.value = null
  if (fileInput.value) fileInput.value.value = ''
}

const downloadTemplate = async () => {
  try {
    const res = await downloadUserTemplate()
    const blob = new Blob([res as unknown as BlobPart], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '用户导入模板.xlsx'
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    console.error(error)
    alert('下载模板失败')
  }
}

const handleImport = async () => {
  if (!selectedFile.value) return
  
  loading.value = true
  try {
    const res: any = await importUsers(selectedFile.value, updateExisting.value)
    alert(res || '导入成功')
    emit('success')
    close()
  } catch (error: any) {
    console.error(error)
    alert(error?.response?.data?.msg || '导入失败')
  } finally {
    loading.value = false
  }
}

defineExpose({ open, close })
</script>
