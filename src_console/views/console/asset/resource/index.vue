<template>
  <div class="h-full flex flex-col overflow-hidden">
    <div class="flex-1 min-h-0 flex flex-col gap-6 overflow-hidden p-6">
      <div class="flex justify-between items-center">
        <div>
          <h1 class="text-2xl font-bold">系统资源库</h1>
          <p class="text-gray-500 mt-1">管理系统级可复用资源（Logo、默认图、运营素材等）</p>
        </div>
        <div class="flex items-center gap-2">
          <input ref="fileInput" type="file" class="hidden" multiple @change="handleUpload" />
          <button v-if="canUpload" class="btn btn-sm btn-primary" :disabled="uploading" @click="openUpload">
            {{ uploading ? '上传中...' : '上传资源' }}
          </button>
          <router-link
            v-if="canUpload"
            to="/console/asset/upload-policy"
            class="text-xs text-slate-500 hover:text-primary"
          >
            权限由上传策略控制
          </router-link>
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
              placeholder="搜索资源文件名"
              @keyup.enter="handleSearch"
            />
            <button class="btn btn-sm btn-primary" @click="handleSearch">搜索</button>
            <button class="btn btn-sm btn-ghost" @click="handleReset">重置</button>
          </div>

          <div class="flex-1 min-h-0 flex">
            <aside class="w-56 border-r border-base-200 p-4 overflow-auto">
              <div class="text-sm text-slate-500 mb-3">资源分类</div>
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
              <div ref="scrollContainer" class="flex-1 overflow-auto">
                <div class="overflow-x-auto">
                  <table class="table table-zebra">
                    <thead>
                      <tr>
                        <th class="w-12">
                          <input type="checkbox" class="checkbox checkbox-xs" :checked="isAllSelected" @change="toggleSelectAll" />
                        </th>
                        <th class="w-16">ID</th>
                        <th>文件名</th>
                        <th class="w-20">预览</th>
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
                        <td colspan="11" class="text-center py-6">加载中...</td>
                      </tr>
                      <tr v-else-if="files.length === 0">
                        <td colspan="11" class="text-center py-6">暂无数据</td>
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
                          <div class="h-10 w-10 rounded bg-base-200 overflow-hidden flex items-center justify-center">
                            <a
                              v-if="isImageFile(file.mimeType) && file.url"
                              :href="resolveFileUrl(file.url)"
                              target="_blank"
                              rel="noopener"
                            >
                              <img
                                :src="resolveFileUrl(file.url)"
                                :alt="file.filename"
                                class="h-10 w-10 object-cover"
                                loading="lazy"
                              />
                            </a>
                            <span v-else class="text-xs text-slate-400">-</span>
                          </div>
                        </td>
                        <td>
                          <span class="badge badge-ghost badge-sm">{{ getCategoryLabel(file.mimeType) }}</span>
                        </td>
                        <td class="text-sm text-slate-500">{{ file.mimeType || '-' }}</td>
                        <td>
                          <span class="badge badge-ghost badge-sm">{{ getVisibilityLabel(file.visibility) }}</span>
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
                <div ref="loadMoreTrigger" class="h-6" aria-hidden="true"></div>
              </div>

              <div class="flex items-center justify-between pt-4 text-sm text-slate-500">
                <div class="text-xs text-slate-500">已选 {{ selectedIds.length }} 项</div>
                <div v-if="loadingMore">正在加载更多...</div>
                <div v-else-if="!hasMore && files.length > 0">没有更多了</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, nextTick, ref } from 'vue'
import { usePermissionStore } from '@/stores/permission'
import { batchDeleteConsoleResources, deleteConsoleResource, getResourceCategories, getResourceList, uploadConsoleResource, type FileCategoryVO, type FileManageVO } from '@/api/system'
import { useDialog } from '@/composables/useDialog'
import { resolveFileUrl } from '@/utils/file'

const categories = ref<FileCategoryVO[]>([])
const activeCategory = ref('all')
const files = ref<FileManageVO[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const hasMore = ref(true)
const scrollContainer = ref<HTMLElement | null>(null)
const loadMoreTrigger = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null
const selectedIds = ref<number[]>([])
const uploading = ref(false)
const deleting = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

const permissionStore = usePermissionStore()
const dialog = useDialog()
const canUpload = computed(() => permissionStore.hasPermission(['system:resource:upload']))
const canDelete = computed(() => permissionStore.hasPermission(['system:resource:delete']))
const isAllSelected = computed(() => files.value.length > 0 && files.value.every(file => selectedIds.value.includes(file.id)))

const reloadAll = () => {
  fetchCategories()
  fetchFiles({ reset: true })
}

onMounted(() => {
  reloadAll()
  nextTick(() => setupObserver())
})

onUnmounted(() => {
  observer?.disconnect()
  observer = null
})

const fetchCategories = async () => {
  try {
    const res = await getResourceCategories()
    const list = res || []
    categories.value = list.length ? list : [{ key: 'all', label: '全部', count: 0 }]
  } catch (error) {
    categories.value = [{ key: 'all', label: '全部', count: 0 }]
  }
}

const fetchFiles = async ({ append = false, reset = false } = {}) => {
  if (append && (loadingMore.value || loading.value)) return
  if (!append && loading.value) return
  if (reset) {
    page.value = 1
    files.value = []
    hasMore.value = true
  }
  append ? (loadingMore.value = true) : (loading.value = true)
  try {
    const res = await getResourceList({
      page: page.value,
      size: pageSize.value,
      category: activeCategory.value,
      keyword: keyword.value.trim() || undefined
    })
    const records = res?.records || []
    total.value = res?.total || 0
    files.value = append ? [...files.value, ...records] : records
    if (total.value) {
      hasMore.value = files.value.length < total.value
    } else {
      hasMore.value = records.length >= pageSize.value
    }
    if (!append) {
      selectedIds.value = selectedIds.value.filter(id => files.value.some(file => file.id === id))
    }
  } catch (error) {
    if (!append) {
      files.value = []
      total.value = 0
      selectedIds.value = []
    }
  } finally {
    append ? (loadingMore.value = false) : (loading.value = false)
  }
}

const selectCategory = (key: string) => {
  activeCategory.value = key
  fetchFiles({ reset: true })
}

const handleSearch = () => {
  fetchFiles({ reset: true })
}

const handleReset = () => {
  keyword.value = ''
  fetchFiles({ reset: true })
}

const setupObserver = () => {
  if (!scrollContainer.value || !loadMoreTrigger.value) return
  observer?.disconnect()
  observer = new IntersectionObserver(
    entries => {
      if (entries[0]?.isIntersecting) {
        void loadMore()
      }
    },
    {
      root: scrollContainer.value,
      rootMargin: '200px 0px',
      threshold: 0
    }
  )
  observer.observe(loadMoreTrigger.value)
}

const loadMore = async () => {
  if (!hasMore.value || loading.value || loadingMore.value) return
  page.value += 1
  await fetchFiles({ append: true })
}

const openUpload = () => {
  fileInput.value?.click()
}

const handleUpload = async (event: Event) => {
  if (!canUpload.value) return
  const input = event.target as HTMLInputElement
  const fileList = input.files ? Array.from(input.files) : []
  if (fileList.length === 0) return
  uploading.value = true
  try {
    await Promise.allSettled(fileList.map(file => uploadConsoleResource(file)))
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
  if (!await dialog.confirm('确认删除该资源吗？')) return
  deleting.value = true
  try {
    await deleteConsoleResource(id)
    reloadAll()
  } finally {
    deleting.value = false
  }
}

const handleBatchDelete = async () => {
  if (!canDelete.value || selectedIds.value.length === 0) return
  if (!await dialog.confirm(`确认删除选中的 ${selectedIds.value.length} 个资源吗？`)) return
  deleting.value = true
  try {
    await batchDeleteConsoleResources(selectedIds.value)
    selectedIds.value = []
    reloadAll()
  } finally {
    deleting.value = false
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

const isImageFile = (mimeType?: string) => {
  return Boolean(mimeType && mimeType.toLowerCase().startsWith('image/'))
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
