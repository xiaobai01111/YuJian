<template>
  <div class="h-full flex flex-col overflow-hidden">
    <div class="flex-1 min-h-0 flex flex-col gap-6 overflow-hidden p-6">
      <div class="flex justify-between items-center">
        <div>
          <h1 class="text-2xl font-bold">图库管理</h1>
          <p class="text-gray-500 mt-1">管理系统图片与分类</p>
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
                  <div v-else v-for="image in images" :key="image.id" class="card bg-base-200/40">
                    <figure class="h-36 bg-base-200">
                      <img
                        v-if="image.url"
                        :src="image.url"
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
                    </div>
                  </div>
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
import { getGalleryCategories, getGalleryList, type FileCategoryVO, type FileManageVO } from '@/api/system'

const categories = ref<FileCategoryVO[]>([])
const activeCategory = ref('all')
const images = ref<FileManageVO[]>([])
const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

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
  } catch (error) {
    images.value = []
    total.value = 0
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
