<template>
  <div class="h-full flex flex-col overflow-hidden">
    <div class="flex-1 min-h-0 flex flex-col gap-6 overflow-hidden p-6">
      <div class="flex justify-between items-center">
        <div>
          <h1 class="text-2xl font-bold">文件管理</h1>
          <p class="text-gray-500 mt-1">管理系统文件与分类</p>
        </div>
        <button class="btn btn-sm btn-ghost" @click="reloadAll">
          刷新
        </button>
      </div>

      <div class="card bg-base-100 shadow-sm flex-1 min-h-0">
        <div class="card-body p-0 flex flex-col min-h-0">
          <div class="flex items-center gap-3 p-4 border-b border-base-200">
            <input
              v-model="keyword"
              class="input input-bordered input-sm w-64"
              placeholder="搜索文件名"
              @keyup.enter="handleSearch"
            />
            <button class="btn btn-sm btn-primary" @click="handleSearch">搜索</button>
            <button class="btn btn-sm btn-ghost" @click="handleReset">重置</button>
          </div>

          <div class="flex-1 min-h-0 flex">
            <aside class="w-56 border-r border-base-200 p-4 overflow-auto">
              <div class="text-sm text-slate-500 mb-3">文件分类</div>
              <ul class="menu menu-sm bg-base-200/40 rounded-box">
                <li v-for="item in categories" :key="item.key">
                  <button
                    class="justify-between"
                    :class="{ active: activeCategory === item.key }"
                    @click="selectCategory(item.key)"
                  >
                    <span>{{ item.label }}</span>
                    <span class="badge badge-ghost badge-sm">{{ item.count }}</span>
                  </button>
                </li>
              </ul>
            </aside>

            <div class="flex-1 min-h-0 flex flex-col p-4">
              <div class="flex-1 overflow-auto">
                <div class="overflow-x-auto">
                  <table class="table table-zebra">
                    <thead>
                      <tr>
                        <th class="w-16">ID</th>
                        <th>文件名</th>
                        <th class="w-32">分类</th>
                        <th class="w-32">类型</th>
                        <th class="w-32">大小</th>
                        <th class="w-40">上传时间</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-if="loading">
                        <td colspan="6" class="text-center py-6">加载中...</td>
                      </tr>
                      <tr v-else-if="files.length === 0">
                        <td colspan="6" class="text-center py-6">暂无数据</td>
                      </tr>
                      <tr v-else v-for="file in files" :key="file.id">
                        <td>{{ file.id }}</td>
                        <td>
                          <div class="font-medium">{{ file.filename }}</div>
                          <div class="text-xs text-slate-500 truncate max-w-xs">{{ file.path }}</div>
                        </td>
                        <td>
                          <span class="badge badge-ghost badge-sm">{{ getCategoryLabel(file.mimeType) }}</span>
                        </td>
                        <td class="text-sm text-slate-500">{{ file.mimeType || '-' }}</td>
                        <td>{{ formatSize(file.size) }}</td>
                        <td class="text-sm text-slate-500">{{ formatDate(file.createdAt) }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>

              <div class="flex justify-end pt-4">
                <div class="join">
                  <button class="join-item btn btn-sm" :disabled="page <= 1" @click="changePage(page - 1)">«</button>
                  <button class="join-item btn btn-sm">Page {{ page }} / {{ totalPages }}</button>
                  <button class="join-item btn btn-sm" :disabled="page >= totalPages" @click="changePage(page + 1)">»</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getFileCategories, getFileList, type FileCategoryVO, type FileManageVO } from '@/api/system'

const categories = ref<FileCategoryVO[]>([])
const activeCategory = ref('all')
const files = ref<FileManageVO[]>([])
const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const reloadAll = () => {
  fetchCategories()
  fetchFiles()
}

onMounted(() => {
  reloadAll()
})

const fetchCategories = async () => {
  try {
    const res = await getFileCategories()
    const list = res || []
    categories.value = list.length ? list : [{ key: 'all', label: '全部', count: 0 }]
  } catch (error) {
    categories.value = [{ key: 'all', label: '全部', count: 0 }]
  }
}

const fetchFiles = async () => {
  loading.value = true
  try {
    const res = await getFileList({
      page: page.value,
      size: pageSize.value,
      category: activeCategory.value,
      keyword: keyword.value.trim() || undefined
    })
    files.value = res?.records || []
    total.value = res?.total || 0
  } catch (error) {
    files.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const selectCategory = (key: string) => {
  activeCategory.value = key
  page.value = 1
  fetchFiles()
}

const handleSearch = () => {
  page.value = 1
  fetchFiles()
}

const handleReset = () => {
  keyword.value = ''
  page.value = 1
  fetchFiles()
}

const changePage = (p: number) => {
  page.value = p
  fetchFiles()
}

const formatSize = (size?: number) => {
  if (!size && size !== 0) return '-'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  if (size < 1024 * 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`
  return `${(size / 1024 / 1024 / 1024).toFixed(1)} GB`
}

const formatDate = (value?: string) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}

const getCategoryLabel = (mimeType?: string) => {
  if (!mimeType) return '其他'
  if (mimeType.startsWith('image/')) return '图片'
  if (mimeType.startsWith('video/')) return '视频'
  if (mimeType.startsWith('audio/')) return '音频'
  if (mimeType.startsWith('text/') || mimeType.startsWith('application/pdf') || mimeType.startsWith('application/msword') || mimeType.startsWith('application/vnd')) {
    return '文档'
  }
  if (mimeType.includes('zip') || mimeType.includes('rar') || mimeType.includes('7z') || mimeType.includes('tar') || mimeType.includes('gzip')) {
    return '压缩包'
  }
  return '其他'
}
</script>
