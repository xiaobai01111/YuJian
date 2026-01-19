<template>
  <div class="h-full flex flex-col overflow-hidden">
    <div class="flex-1 min-h-0 flex flex-col gap-6 overflow-hidden p-6">
      <div class="flex justify-between items-center">
        <div>
          <h1 class="text-2xl font-bold">图库管理</h1>
          <p class="text-gray-500 mt-1">管理系统图片与分类</p>
        </div>
        <div class="flex items-center gap-2">
          <input ref="fileInput" type="file" class="hidden" accept="image/*" multiple @change="handleUpload" />
          <select v-if="canUpload" v-model="uploadVisibility" class="select select-sm">
            <option value="PUBLIC">公有</option>
            <option value="PRIVATE">私有</option>
          </select>
          <button v-if="canUpload" class="btn btn-sm btn-primary" :disabled="uploading" @click="openUpload">
            {{ uploading ? '上传中...' : '上传图片' }}
          </button>
          <button
            v-if="canDelete"
            class="btn btn-sm btn-ghost"
            :disabled="images.length === 0"
            @click="toggleSelectAll"
          >
            {{ isAllSelected ? '清空选择' : '全选当前页' }}
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
              placeholder="搜索图片名称"
              @keyup.enter="handleSearch"
            />
            <button class="btn btn-sm btn-primary" @click="handleSearch">搜索</button>
            <button class="btn btn-sm btn-ghost" @click="handleReset">重置</button>
          </div>

          <div class="flex-1 min-h-0 flex">
            <aside class="w-56 border-r border-base-200 p-4 overflow-auto">
              <div class="text-sm text-slate-500 mb-3">图片分类</div>
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
                <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
                  <div v-if="loading" class="col-span-full text-center py-6">加载中...</div>
                  <div v-else-if="images.length === 0" class="col-span-full text-center py-6">暂无数据</div>
                  <div v-else v-for="image in images" :key="image.id" class="card bg-base-200/40 relative">
                    <div class="absolute left-2 top-2 z-10">
                      <input
                        type="checkbox"
                        class="checkbox checkbox-xs"
                        :checked="selectedIds.includes(image.id)"
                        @change="toggleSelect(image.id)"
                      />
                    </div>
                    <figure class="h-36 bg-base-200">
                      <img
                        v-if="image.url"
                        :src="resolveFileUrl(image.url)"
                        :alt="image.filename"
                        class="h-full w-full object-cover"
                        loading="lazy"
                      />
                      <div v-else class="text-xs text-slate-400">无预览</div>
                    </figure>
                    <div class="card-body p-3">
                      <div class="font-medium truncate" :title="image.filename">{{ image.filename }}</div>
                      <div class="text-xs text-slate-500">{{ image.mimeType || '-' }}</div>
                      <div class="text-xs text-slate-500">{{ formatSize(image.size) }} · {{ formatDate(image.createdAt) }}</div>
                      <div class="text-xs text-slate-500">上传者：{{ image.uploaderName || '-' }}</div>
                      <div class="text-xs text-slate-500 flex items-center gap-1">
                        <span>权限：</span>
                        <select
                          v-if="canEditVisibility"
                          class="select select-xs"
                          :value="image.visibility || 'PRIVATE'"
                          @change="event => handleVisibilityChange(image, (event.target as HTMLSelectElement).value)"
                        >
                          <option value="PUBLIC">公有</option>
                          <option value="PRIVATE">私有</option>
                        </select>
                        <span v-else>{{ getVisibilityLabel(image.visibility) }}</span>
                      </div>
                      <div v-if="canDelete" class="mt-2">
                        <button class="btn btn-xs btn-error btn-outline" @click="handleDelete(image.id)">删除</button>
                      </div>
                    </div>
                  </div>
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { usePermissionStore } from '@/stores/permission'
import { batchDeleteConsoleGallery, deleteConsoleGallery, getGalleryCategories, getGalleryList, updateConsoleGalleryVisibility, uploadConsoleGallery, type FileCategoryVO, type FileManageVO } from '@/api/system'
import { resolveFileUrl } from '@/utils/file'

const categories = ref<FileCategoryVO[]>([])
const activeCategory = ref('all')
const images = ref<FileManageVO[]>([])
const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
const selectedIds = ref<number[]>([])
const uploading = ref(false)
const deleting = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const uploadVisibility = ref<'PUBLIC' | 'PRIVATE'>('PUBLIC')

const permissionStore = usePermissionStore()
const canUpload = computed(() => permissionStore.hasPermission(['system:gallery:upload']))
const canDelete = computed(() => permissionStore.hasPermission(['system:gallery:delete']))
const canEditVisibility = computed(() => permissionStore.hasPermission(['system:gallery:permission']))
const isAllSelected = computed(() => images.value.length > 0 && images.value.every(image => selectedIds.value.includes(image.id)))

const reloadAll = () => {
  fetchCategories()
  fetchImages()
}

onMounted(() => {
  reloadAll()
})

const fetchCategories = async () => {
  try {
    const res = await getGalleryCategories()
    const list = res || []
    categories.value = list.length ? list : [{ key: 'all', label: '全部', count: 0 }]
  } catch (error) {
    categories.value = [{ key: 'all', label: '全部', count: 0 }]
  }
}

const fetchImages = async () => {
  loading.value = true
  try {
    const res = await getGalleryList({
      page: page.value,
      size: pageSize.value,
      category: activeCategory.value,
      keyword: keyword.value.trim() || undefined
    })
    images.value = res?.records || []
    total.value = res?.total || 0
    selectedIds.value = selectedIds.value.filter(id => images.value.some(image => image.id === id))
  } catch (error) {
    images.value = []
    total.value = 0
    selectedIds.value = []
  } finally {
    loading.value = false
  }
}

const selectCategory = (key: string) => {
  activeCategory.value = key
  page.value = 1
  fetchImages()
}

const handleSearch = () => {
  page.value = 1
  fetchImages()
}

const handleReset = () => {
  keyword.value = ''
  page.value = 1
  fetchImages()
}

const changePage = (p: number) => {
  page.value = p
  fetchImages()
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
    await Promise.allSettled(fileList.map(file => uploadConsoleGallery(file, undefined, uploadVisibility.value)))
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
    selectedIds.value = images.value.map(image => image.id)
  }
}

const handleDelete = async (id: number) => {
  if (!canDelete.value) return
  if (!confirm('确认删除该图片吗？')) return
  deleting.value = true
  try {
    await deleteConsoleGallery(id)
    reloadAll()
  } finally {
    deleting.value = false
  }
}

const handleBatchDelete = async () => {
  if (!canDelete.value || selectedIds.value.length === 0) return
  if (!confirm(`确认删除选中的 ${selectedIds.value.length} 张图片吗？`)) return
  deleting.value = true
  try {
    await batchDeleteConsoleGallery(selectedIds.value)
    selectedIds.value = []
    reloadAll()
  } finally {
    deleting.value = false
  }
}

const handleVisibilityChange = async (image: FileManageVO, visibility: string) => {
  if (!canEditVisibility.value) return
  const next = visibility.toUpperCase()
  if (image.visibility === next) return
  try {
    await updateConsoleGalleryVisibility(image.id, next)
    image.visibility = next
    fetchImages()
  } catch (error) {
    fetchImages()
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
  return date.toLocaleDateString()
}
</script>
