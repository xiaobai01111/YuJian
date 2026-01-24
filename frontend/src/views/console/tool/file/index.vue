<template>
  <div class="h-full flex flex-col overflow-hidden">
    <div class="flex-1 min-h-0 flex flex-col gap-6 overflow-hidden p-6">
      <div class="flex justify-between items-center">
        <div>
          <h1 class="text-2xl font-bold">文件管理</h1>
          <p class="text-gray-500 mt-1">管理系统文件与分类</p>
        </div>
        <div class="flex items-center gap-2">
          <input ref="fileInput" type="file" class="hidden" multiple @change="handleUpload" />
          <select v-if="canUpload" v-model="uploadVisibility" class="select select-sm">
            <option value="PUBLIC">公有</option>
            <option value="PRIVATE">私有</option>
          </select>
          <button v-if="canUpload" class="btn btn-sm btn-primary" :disabled="uploading" @click="openUpload">
            {{ uploading ? '上传中...' : '上传文件' }}
          </button>
          <button v-if="canCleanup" class="btn btn-sm btn-secondary btn-outline" @click="openCleanupModal">
            孤儿清理
          </button>
          <button
            v-if="canDelete"
            class="btn btn-sm btn-error btn-outline"
            :disabled="selectedIds.length === 0 || deleting"
            @click="handleBatchDelete"
          >
            批量删除
          </button>
          <button class="btn btn-sm btn-ghost" @click="reloadAll">
            刷新
          </button>
        </div>
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
                        <th class="w-12">
                          <input type="checkbox" class="checkbox checkbox-xs" :checked="isAllSelected" @change="toggleSelectAll" />
                        </th>
                        <th class="w-16">ID</th>
                        <th>文件名</th>
                        <th class="w-32">分类</th>
                        <th class="w-32">类型</th>
                        <th class="w-28">权限</th>
                        <th class="w-32">上传者</th>
                        <th class="w-32">大小</th>
                        <th class="w-40">上传时间</th>
                        <th class="w-24">操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-if="loading">
                        <td colspan="10" class="text-center py-6">加载中...</td>
                      </tr>
                      <tr v-else-if="files.length === 0">
                        <td colspan="10" class="text-center py-6">暂无数据</td>
                      </tr>
                      <tr v-else v-for="file in files" :key="file.id">
                        <td>
                          <input
                            type="checkbox"
                            class="checkbox checkbox-xs"
                            :checked="selectedIds.includes(file.id)"
                            @change="toggleSelect(file.id)"
                          />
                        </td>
                        <td>{{ file.id }}</td>
                        <td>
                          <div class="font-medium">{{ file.filename }}</div>
                          <div class="text-xs text-slate-500 truncate max-w-xs">{{ file.path }}</div>
                        </td>
                        <td>
                          <span class="badge badge-ghost badge-sm">{{ getCategoryLabel(file.mimeType) }}</span>
                        </td>
                        <td class="text-sm text-slate-500">{{ file.mimeType || '-' }}</td>
                        <td>
                          <select
                            v-if="canEditVisibility"
                            class="select select-xs"
                            :value="file.visibility || 'PRIVATE'"
                            @change="event => handleVisibilityChange(file, (event.target as HTMLSelectElement).value)"
                          >
                            <option value="PUBLIC">公有</option>
                            <option value="PRIVATE">私有</option>
                          </select>
                          <span v-else class="badge badge-ghost badge-sm">{{ getVisibilityLabel(file.visibility) }}</span>
                        </td>
                        <td class="text-sm text-slate-500">{{ file.uploaderName || '-' }}</td>
                        <td>{{ formatSize(file.size) }}</td>
                        <td class="text-sm text-slate-500">{{ formatDate(file.createdAt) }}</td>
                        <td>
                          <button
                            v-if="canDelete"
                            class="btn btn-xs btn-error btn-outline"
                            @click="handleDelete(file.id)"
                          >
                            删除
                          </button>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>

              <div class="flex items-center justify-between pt-4">
                <div class="text-xs text-slate-500">已选 {{ selectedIds.length }} 项</div>
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

    <div class="modal" :class="{ 'modal-open': showCleanupModal }">
      <div class="modal-box max-w-xl">
        <h3 class="font-bold text-lg">孤儿文件清理</h3>
        <p class="text-sm text-slate-500 mt-2">未绑定文件会先标记，再按保留期删除。本次执行将使用下面的策略。</p>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
          <label class="form-control w-full">
            <div class="label"><span class="label-text">未绑定小时</span></div>
            <input
              v-model.number="cleanupForm.markOrphanHours"
              type="number"
              min="0"
              class="input input-bordered input-sm"
            />
          </label>
          <label class="form-control w-full">
            <div class="label"><span class="label-text">保留天数</span></div>
            <input
              v-model.number="cleanupForm.retainDays"
              type="number"
              min="0"
              class="input input-bordered input-sm"
            />
          </label>
          <label class="form-control w-full">
            <div class="label"><span class="label-text">单次删除上限</span></div>
            <input
              v-model.number="cleanupForm.deleteLimit"
              type="number"
              min="1"
              class="input input-bordered input-sm"
            />
          </label>
        </div>

        <div v-if="cleanupResult" class="mt-4 text-sm text-slate-600">
          上次执行：标记 {{ cleanupResult.marked }}，删除 {{ cleanupResult.deleted }}，失败 {{ cleanupResult.failed }}
        </div>

        <div class="modal-action">
          <button class="btn btn-ghost" @click="closeCleanupModal">取消</button>
          <button class="btn btn-outline" :disabled="cleanupSaving" @click="saveCleanupConfig">
            {{ cleanupSaving ? '保存中...' : '保存策略' }}
          </button>
          <button class="btn btn-primary" :disabled="cleanupRunning" @click="runCleanup">
            {{ cleanupRunning ? '执行中...' : '立即清理' }}
          </button>
        </div>
      </div>
      <div class="modal-backdrop"><button @click="closeCleanupModal">close</button></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { usePermissionStore } from '@/stores/permission'
import { batchDeleteConsoleFiles, deleteConsoleFile, getFileCategories, getFileCleanupConfig, getFileList, runFileCleanup, updateConsoleFileVisibility, updateFileCleanupConfig, uploadConsoleFile, type FileCategoryVO, type FileCleanupConfig, type FileCleanupResult, type FileManageVO } from '@/api/system'
import { useDialog } from '@/composables/useDialog'

const categories = ref<FileCategoryVO[]>([])
const activeCategory = ref('all')
const files = ref<FileManageVO[]>([])
const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
const selectedIds = ref<number[]>([])
const uploading = ref(false)
const deleting = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const uploadVisibility = ref<'PUBLIC' | 'PRIVATE'>('PUBLIC')
const showCleanupModal = ref(false)
const cleanupForm = ref<FileCleanupConfig>({ markOrphanHours: 24, retainDays: 7, deleteLimit: 200 })
const cleanupSaving = ref(false)
const cleanupRunning = ref(false)
const cleanupResult = ref<FileCleanupResult | null>(null)

const permissionStore = usePermissionStore()
const dialog = useDialog()
const canUpload = computed(() => permissionStore.hasPermission(['system:file:upload']))
const canDelete = computed(() => permissionStore.hasPermission(['system:file:delete']))
const canEditVisibility = computed(() => permissionStore.hasPermission(['system:file:permission']))
const canCleanup = computed(() => permissionStore.hasPermission(['system:file:cleanup']))
const isAllSelected = computed(() => files.value.length > 0 && files.value.every(file => selectedIds.value.includes(file.id)))

const reloadAll = () => {
  fetchCategories()
  fetchFiles()
}

onMounted(() => {
  reloadAll()
  if (canCleanup.value) {
    fetchCleanupConfig()
  }
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
    selectedIds.value = selectedIds.value.filter(id => files.value.some(file => file.id === id))
  } catch (error) {
    files.value = []
    total.value = 0
    selectedIds.value = []
  } finally {
    loading.value = false
  }
}

const fetchCleanupConfig = async () => {
  try {
    const res = await getFileCleanupConfig()
    cleanupForm.value = {
      markOrphanHours: res?.markOrphanHours ?? cleanupForm.value.markOrphanHours,
      retainDays: res?.retainDays ?? cleanupForm.value.retainDays,
      deleteLimit: res?.deleteLimit ?? cleanupForm.value.deleteLimit
    }
  } catch (error) {
    // ignore
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

const openUpload = () => {
  fileInput.value?.click()
}

const openCleanupModal = async () => {
  await fetchCleanupConfig()
  showCleanupModal.value = true
}

const closeCleanupModal = () => {
  showCleanupModal.value = false
}

const saveCleanupConfig = async () => {
  cleanupSaving.value = true
  try {
    await updateFileCleanupConfig(cleanupForm.value)
    await dialog.alert('清理策略已保存')
  } finally {
    cleanupSaving.value = false
  }
}

const runCleanup = async () => {
  if (!await dialog.confirm('确认立即执行孤儿文件清理吗？')) return
  cleanupRunning.value = true
  try {
    const res = await runFileCleanup(cleanupForm.value)
    cleanupResult.value = res || null
    await dialog.alert(`清理完成：标记 ${res?.marked ?? 0}，删除 ${res?.deleted ?? 0}，失败 ${res?.failed ?? 0}`)
  } finally {
    cleanupRunning.value = false
  }
}

const handleUpload = async (event: Event) => {
  if (!canUpload.value) return
  const input = event.target as HTMLInputElement
  const fileList = input.files ? Array.from(input.files) : []
  if (fileList.length === 0) return
  uploading.value = true
  try {
    await Promise.allSettled(fileList.map(file => uploadConsoleFile(file, undefined, uploadVisibility.value)))
    reloadAll()
  } finally {
    uploading.value = false
    if (input) input.value = ''
  }
}

const toggleSelect = (id: number) => {
  if (selectedIds.value.includes(id)) {
    selectedIds.value = selectedIds.value.filter(item => item !== id)
  } else {
    selectedIds.value.push(id)
  }
}

const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedIds.value = []
  } else {
    selectedIds.value = files.value.map(file => file.id)
  }
}

const handleDelete = async (id: number) => {
  if (!canDelete.value) return
  if (!await dialog.confirm('确认删除该文件吗？')) return
  deleting.value = true
  try {
    await deleteConsoleFile(id)
    reloadAll()
  } finally {
    deleting.value = false
  }
}

const handleBatchDelete = async () => {
  if (!canDelete.value || selectedIds.value.length === 0) return
  if (!await dialog.confirm(`确认删除选中的 ${selectedIds.value.length} 个文件吗？`)) return
  deleting.value = true
  try {
    await batchDeleteConsoleFiles(selectedIds.value)
    selectedIds.value = []
    reloadAll()
  } finally {
    deleting.value = false
  }
}

const handleVisibilityChange = async (file: FileManageVO, visibility: string) => {
  if (!canEditVisibility.value) return
  const next = visibility.toUpperCase()
  if (file.visibility === next) return
  try {
    await updateConsoleFileVisibility(file.id, next)
    file.visibility = next
    fetchFiles()
  } catch (error) {
    fetchFiles()
  }
}

const getVisibilityLabel = (visibility?: string) => {
  return visibility?.toUpperCase() === 'PUBLIC' ? '公有' : '私有'
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
