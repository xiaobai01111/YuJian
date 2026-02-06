<template>
  <div class="h-full flex flex-col overflow-hidden">
    <div class="flex-1 min-h-0 flex flex-col gap-6 overflow-hidden pr-1">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-800">敏感词管理</h1>
        <p class="text-slate-500 text-sm mt-1">管理系统敏感词库，支持单条添加和批量导入</p>
      </div>
    </div>

    <!-- Toolbar -->
    <div class="card bg-base-100 shadow-sm border border-base-200">
      <div class="card-body p-4">
        <div class="flex flex-wrap items-center gap-4">
          <!-- Search -->
          <div class="form-control">
            <input 
              v-model="searchKeyword" 
              type="text" 
              placeholder="搜索敏感词..." 
              class="input input-bordered input-sm w-48"
              @keyup.enter="handleSearch"
            />
          </div>
          
          <!-- Level Filter (已移除警告级别，仅拦截) -->

          <button class="btn btn-sm btn-ghost" @click="handleSearch">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            搜索
          </button>

          <div class="flex-1"></div>

          <!-- Actions -->
          <button v-if="canAdd" class="btn btn-sm btn-primary" @click="showAddModal = true">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            添加
          </button>
          
          <button v-if="canAdd" class="btn btn-sm btn-secondary" @click="showBatchModal = true">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
            </svg>
            批量导入
          </button>

          <button 
            v-if="canDelete"
            class="btn btn-sm btn-error" 
            :disabled="selectedIds.length === 0"
            @click="handleBatchDelete"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
            删除选中 ({{ selectedIds.length }})
          </button>

          <button class="btn btn-sm btn-ghost" @click="loadData">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            刷新
          </button>
        </div>
      </div>
    </div>

    <!-- Table -->
    <div class="card bg-base-100 shadow-sm border border-base-200 flex-1 min-h-0">
      <div class="card-body p-0 flex flex-col min-h-0">
        <div class="flex-1 overflow-auto" ref="scrollContainer">
          <div class="overflow-x-auto">
            <table class="table">
            <thead>
              <tr>
                <th class="w-12">
                  <input 
                    type="checkbox" 
                    class="checkbox checkbox-sm"
                    :checked="isAllSelected"
                    @change="toggleSelectAll"
                  />
                </th>
                <th class="w-20">ID</th>
                <th>敏感词</th>
                <th class="w-44">创建时间</th>
                <th class="w-32">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="5" class="text-center py-8">
                  <span class="loading loading-spinner loading-md"></span>
                </td>
              </tr>
              <tr v-else-if="words.length === 0">
                <td colspan="5" class="text-center py-8 text-slate-500">暂无数据</td>
              </tr>
              <tr v-for="word in words" :key="word.id" class="hover">
                <td>
                  <input 
                    type="checkbox" 
                    class="checkbox checkbox-sm"
                    :checked="selectedIds.includes(word.id)"
                    @change="toggleSelect(word.id)"
                  />
                </td>
                <td class="text-slate-500">{{ word.id }}</td>
                <td class="font-medium">{{ word.word }}</td>
                <td class="text-slate-500 text-sm">{{ formatDateTime(word.createdAt) }}</td>
                <td>
                  <div class="flex gap-2">
                    <button v-if="canDelete" class="btn btn-xs btn-ghost text-error" @click="handleDelete(word)">
                      删除
                    </button>
                    <span v-else class="text-slate-400 text-xs">-</span>
                  </div>
                </td>
              </tr>
            </tbody>
            </table>
            <div ref="loadMoreTrigger" class="h-6" aria-hidden="true"></div>
          </div>
        </div>

        <!-- Load Status -->
        <div class="flex items-center justify-between p-4 border-t border-base-200 text-sm text-slate-500">
          <div>已加载 {{ words.length }} / {{ total || '-' }} 条</div>
          <div v-if="loadingMore">正在加载更多...</div>
          <div v-else-if="!hasMore && words.length > 0">没有更多了</div>
        </div>
      </div>
    </div>

    </div>

    <!-- Add Modal -->
    <dialog class="modal" :class="{ 'modal-open': showAddModal }">
      <div class="modal-box">
        <h3 class="font-bold text-lg mb-4">添加敏感词</h3>
        <div class="form-control mb-4">
          <label class="label"><span class="label-text">敏感词</span></label>
          <input 
            v-model="addForm.word" 
            type="text" 
            placeholder="输入敏感词（至少2个字符）" 
            class="input input-bordered"
          />
        </div>
        <div class="modal-action">
          <button class="btn btn-ghost" @click="showAddModal = false">取消</button>
          <button class="btn btn-primary" :disabled="addLoading" @click="handleAdd">
            <span v-if="addLoading" class="loading loading-spinner loading-sm"></span>
            确定
          </button>
        </div>
      </div>
      <form method="dialog" class="modal-backdrop" @click="showAddModal = false"><button>close</button></form>
    </dialog>

    <!-- Batch Import Modal -->
    <dialog class="modal" :class="{ 'modal-open': showBatchModal }">
      <div class="modal-box max-w-2xl">
        <h3 class="font-bold text-lg mb-4">批量导入敏感词</h3>
        <div class="form-control mb-4">
          <label class="label">
            <span class="label-text">敏感词列表</span>
            <span class="label-text-alt text-slate-400">每行一个，后台分片处理</span>
          </label>
          <textarea 
            v-model="batchForm.text" 
            class="textarea textarea-bordered h-48 font-mono"
            placeholder="每行输入一个敏感词&#10;例如：&#10;敏感词1&#10;敏感词2&#10;敏感词3"
          ></textarea>
        </div>
        <!-- Import Result -->
        <div v-if="batchResult" class="alert mb-4" :class="batchResult.addedCount > 0 ? 'alert-success' : 'alert-warning'">
          <div>
            <p>导入完成：成功 {{ batchResult.addedCount }} 条，跳过 {{ batchResult.skippedCount }} 条，无效 {{ batchResult.invalidCount }} 条</p>
            <p v-if="batchResult.skippedCount > 0" class="text-xs mt-1">
              跳过（已存在）: {{ batchResult.skipped?.slice(0, 5).join(', ') }}{{ (batchResult.skipped?.length ?? 0) > 5 ? '...' : '' }}
            </p>
            <p v-if="batchResult.invalidCount > 0" class="text-xs mt-1">
              无效（过短）: {{ batchResult.invalid?.slice(0, 5).join(', ') }}{{ (batchResult.invalid?.length ?? 0) > 5 ? '...' : '' }}
            </p>
          </div>
        </div>

        <div class="modal-action">
          <button class="btn btn-ghost" @click="closeBatchModal">关闭</button>
          <button class="btn btn-primary" :disabled="batchLoading" @click="handleBatchImport">
            <span v-if="batchLoading" class="loading loading-spinner loading-sm"></span>
            导入
          </button>
        </div>
      </div>
      <form method="dialog" class="modal-backdrop" @click="closeBatchModal"><button>close</button></form>
    </dialog>

    <!-- Delete Confirm Modal -->
    <dialog class="modal" :class="{ 'modal-open': showDeleteModal }">
      <div class="modal-box">
        <h3 class="font-bold text-lg mb-4">确认删除</h3>
        <p>确定要删除敏感词 "<span class="font-bold text-error">{{ deleteTarget?.word }}</span>" 吗？</p>
        <p class="text-sm text-slate-500 mt-2">删除后将立即生效，相关内容将不再被过滤。</p>
        <div class="modal-action">
          <button class="btn btn-ghost" @click="showDeleteModal = false">取消</button>
          <button class="btn btn-error" :disabled="deleteLoading" @click="confirmDelete">
            <span v-if="deleteLoading" class="loading loading-spinner loading-sm"></span>
            删除
          </button>
        </div>
      </div>
      <form method="dialog" class="modal-backdrop" @click="showDeleteModal = false"><button>close</button></form>
    </dialog>

    <!-- Batch Delete Confirm Modal -->
    <dialog class="modal" :class="{ 'modal-open': showBatchDeleteModal }">
      <div class="modal-box">
        <h3 class="font-bold text-lg mb-4">确认批量删除</h3>
        <p>确定要删除选中的 <span class="font-bold text-error">{{ selectedIds.length }}</span> 条敏感词吗？</p>
        <p class="text-sm text-slate-500 mt-2">删除后将立即生效，相关内容将不再被过滤。</p>
        <div class="modal-action">
          <button class="btn btn-ghost" @click="showBatchDeleteModal = false">取消</button>
          <button class="btn btn-error" :disabled="deleteLoading" @click="confirmBatchDelete">
            <span v-if="deleteLoading" class="loading loading-spinner loading-sm"></span>
            删除
          </button>
        </div>
      </div>
      <form method="dialog" class="modal-backdrop" @click="showBatchDeleteModal = false"><button>close</button></form>
    </dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { 
  querySensitiveWords, 
  createSensitiveWord, 
  createSensitiveWordsBatch,
  deleteSensitiveWord,
  deleteSensitiveWords
} from '@/api/system'
import { useUserStore } from '@/stores/user'
import { useDialog } from '@/composables/useDialog'

const userStore = useUserStore()
const dialog = useDialog()
const canAdd = computed(() => userStore.hasPermission('system:sensitive-word:add'))
const canDelete = computed(() => userStore.hasPermission('system:sensitive-word:delete'))

interface SensitiveWordVO {
  id: number
  word: string
  level: number
  levelText: string
  createdAt: string
}

interface BatchResult {
  addedCount: number
  skippedCount: number
  invalidCount: number
  added?: string[]
  skipped?: string[]
  invalid?: string[]
}

// State
const loading = ref(false)
const words = ref<SensitiveWordVO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const searchKeyword = ref('')
const selectedIds = ref<number[]>([])
const loadingMore = ref(false)
const hasMore = ref(true)
const scrollContainer = ref<HTMLElement | null>(null)
const loadMoreTrigger = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

// Modals
const showAddModal = ref(false)
const showBatchModal = ref(false)
const showDeleteModal = ref(false)
const showBatchDeleteModal = ref(false)
const addLoading = ref(false)
const batchLoading = ref(false)
const deleteLoading = ref(false)

// Forms
const addForm = ref({ word: '' })
const batchForm = ref({ text: '' })
const batchResult = ref<BatchResult | null>(null)
const deleteTarget = ref<SensitiveWordVO | null>(null)

// Computed
const isAllSelected = computed(() => {
  return words.value.length > 0 && selectedIds.value.length === words.value.length
})

// Methods
onMounted(() => {
  loadData()
  nextTick(() => setupObserver())
})

onUnmounted(() => {
  observer?.disconnect()
  observer = null
})

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

async function fetchData({ append = false, reset = false } = {}) {
  if (append && (loadingMore.value || loading.value)) return
  if (!append && loading.value) return
  if (reset) {
    currentPage.value = 1
    words.value = []
    selectedIds.value = []
    hasMore.value = true
  }
  append ? (loadingMore.value = true) : (loading.value = true)
  try {
    const res = await querySensitiveWords({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined
    })
    const records = res.records || []
    total.value = res.total || 0
    if (append) {
      words.value = [...words.value, ...records]
    } else {
      words.value = records
    }
    if (total.value) {
      hasMore.value = words.value.length < total.value
    } else {
      hasMore.value = records.length >= pageSize.value
    }
  } catch (e: unknown) {
    console.error('Failed to load sensitive words', e)
    if (!append) {
      words.value = []
      total.value = 0
    }
  } finally {
    append ? (loadingMore.value = false) : (loading.value = false)
  }
}

async function loadData() {
  await fetchData({ reset: true })
}

function handleSearch() {
  loadData()
}

async function loadMore() {
  if (!hasMore.value || loading.value || loadingMore.value) return
  currentPage.value += 1
  await fetchData({ append: true })
}

function toggleSelectAll() {
  if (isAllSelected.value) {
    selectedIds.value = []
  } else {
    selectedIds.value = words.value.map(w => w.id)
  }
}

function toggleSelect(id: number) {
  const idx = selectedIds.value.indexOf(id)
  if (idx >= 0) {
    selectedIds.value.splice(idx, 1)
  } else {
    selectedIds.value.push(id)
  }
}

async function handleAdd() {
  if (!addForm.value.word.trim()) {
    await dialog.alert('请输入敏感词')
    return
  }
  addLoading.value = true
  try {
    await createSensitiveWord({
      word: addForm.value.word.trim(),
      level: 2
    })
    showAddModal.value = false
    addForm.value = { word: '' }
    loadData()
  } catch (e: unknown) {
    await dialog.alert((e as ApiErrorLike)?.message || (e as ApiErrorLike)?.response?.data?.message || '添加失败')
  } finally {
    addLoading.value = false
  }
}

async function handleBatchImport() {
  const lines = batchForm.value.text.split('\n').map(l => l.trim()).filter(l => l)
  if (lines.length === 0) {
    await dialog.alert('请输入敏感词')
    return
  }
  batchLoading.value = true
  try {
    const res = await createSensitiveWordsBatch({
      words: lines,
      level: 2
    })
    batchResult.value = res
    loadData()
  } catch (e: unknown) {
    await dialog.alert((e as ApiErrorLike)?.message || (e as ApiErrorLike)?.response?.data?.message || '导入失败')
  } finally {
    batchLoading.value = false
  }
}

function closeBatchModal() {
  showBatchModal.value = false
  batchForm.value = { text: '' }
  batchResult.value = null
}

function handleDelete(word: SensitiveWordVO) {
  deleteTarget.value = word
  showDeleteModal.value = true
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  deleteLoading.value = true
  try {
    await deleteSensitiveWord(deleteTarget.value.id)
    showDeleteModal.value = false
    deleteTarget.value = null
    loadData()
  } catch (e: unknown) {
    await dialog.alert((e as ApiErrorLike)?.message || (e as ApiErrorLike)?.response?.data?.message || '删除失败')
  } finally {
    deleteLoading.value = false
  }
}

function handleBatchDelete() {
  if (selectedIds.value.length === 0) return
  showBatchDeleteModal.value = true
}

async function confirmBatchDelete() {
  deleteLoading.value = true
  try {
    await deleteSensitiveWords(selectedIds.value)
    showBatchDeleteModal.value = false
    selectedIds.value = []
    loadData()
  } catch (e: unknown) {
    await dialog.alert((e as ApiErrorLike)?.message || (e as ApiErrorLike)?.response?.data?.message || '删除失败')
  } finally {
    deleteLoading.value = false
  }
}

function formatDateTime(dateStr?: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}
</script>
