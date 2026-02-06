<template>
  <div class="h-full flex flex-col overflow-hidden">
    <div class="flex-1 min-h-0 flex flex-col gap-6 overflow-hidden p-6">
    <!-- Header -->
    <div class="flex justify-between items-center mb-6">
      <div>
        <h1 class="text-2xl font-bold">公告管理</h1>
        <p class="text-gray-500 mt-1">管理系统公告通知</p>
      </div>
      <button class="btn btn-primary" @click="openCreateModal">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
        新增公告
      </button>
    </div>

    <!-- Filters -->
    <div class="card bg-base-100 shadow-sm mb-6">
      <div class="card-body p-4">
        <div class="flex flex-wrap gap-4 items-center">
          <div class="form-control">
            <select v-model="filterStatus" class="select select-bordered select-sm w-32" @change="handleSearch">
              <option :value="undefined">全部状态</option>
              <option :value="0">草稿</option>
              <option :value="1">已发布</option>
              <option :value="2">已下线</option>
            </select>
          </div>
          <div class="form-control">
            <input 
              v-model="filterKeyword" 
              type="text" 
              placeholder="搜索标题..." 
              class="input input-bordered input-sm w-48"
              @keyup.enter="handleSearch"
            />
          </div>
          <button class="btn btn-sm btn-ghost" @click="handleSearch">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            搜索
          </button>
        </div>
      </div>
    </div>

    <!-- Table -->
    <div class="card bg-base-100 shadow-sm flex-1 min-h-0">
      <div class="card-body p-0 flex flex-col min-h-0">
        <div class="flex-1 overflow-auto" ref="scrollContainer">
          <div class="overflow-x-auto">
            <table class="table">
            <thead>
              <tr>
                <th class="w-12">ID</th>
                <th>标题</th>
                <th class="w-24">状态</th>
                <th class="w-24">可见范围</th>
                <th class="w-20">置顶</th>
                <th class="w-32">发布者</th>
                <th class="w-40">发布时间</th>
                <th class="w-48">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="8" class="text-center py-8">
                  <span class="loading loading-spinner loading-md"></span>
                </td>
              </tr>
              <tr v-else-if="notices.length === 0">
                <td colspan="8" class="text-center py-8 text-gray-500">暂无数据</td>
              </tr>
              <tr v-for="notice in notices" :key="notice.id" class="hover">
                <td>{{ notice.id }}</td>
                <td>
                  <div class="font-medium line-clamp-1 max-w-xs">{{ notice.title }}</div>
                </td>
                <td>
                  <span :class="getStatusClass(notice.status)">{{ notice.statusText }}</span>
                </td>
                <td>{{ notice.scopeTypeText }}</td>
                <td>
                  <span v-if="notice.isPinned" class="badge badge-error badge-sm">置顶</span>
                  <span v-else class="text-gray-400">-</span>
                </td>
                <td>{{ notice.createdByName || '-' }}</td>
                <td>{{ formatDateTime(notice.publishedAt) || '-' }}</td>
                <td>
                  <div class="flex gap-1">
                    <button class="btn btn-ghost btn-xs" @click="openEditModal(notice)">编辑</button>
                    <button 
                      v-if="notice.status === 0" 
                      class="btn btn-success btn-xs" 
                      @click="handlePublish(notice)"
                    >发布</button>
                    <button 
                      v-if="notice.status === 1" 
                      class="btn btn-warning btn-xs" 
                      @click="handleOffline(notice)"
                    >下线</button>
                    <button class="btn btn-error btn-xs" @click="handleDelete(notice)">删除</button>
                  </div>
                </td>
              </tr>
            </tbody>
            </table>
            <div ref="loadMoreTrigger" class="h-6" aria-hidden="true"></div>
          </div>
        </div>

        <!-- Load Status -->
        <div class="flex justify-between items-center p-4 border-t text-sm text-slate-500">
          <div>已加载 {{ notices.length }} / {{ total || '-' }} 条</div>
          <div v-if="loadingMore">正在加载更多...</div>
          <div v-else-if="!hasMore && notices.length > 0">没有更多了</div>
        </div>
      </div>
    </div>

    </div>

    <!-- Create/Edit Modal -->
    <dialog ref="formModal" class="modal">
      <div class="modal-box max-w-3xl">
        <h3 class="font-bold text-lg mb-4">{{ isEdit ? '编辑公告' : '新增公告' }}</h3>
        
        <div class="space-y-4">
          <!-- Title -->
          <div class="form-control">
            <label class="label"><span class="label-text font-medium">标题 *</span></label>
            <input v-model="form.title" type="text" class="input input-bordered" placeholder="请输入公告标题" />
          </div>

          <!-- Content -->
          <div class="form-control">
            <label class="label"><span class="label-text font-medium">内容 *</span></label>
            <textarea v-model="form.content" class="textarea textarea-bordered h-40" placeholder="请输入公告内容"></textarea>
          </div>

          <!-- Scope Type -->
          <div class="form-control">
            <label class="label"><span class="label-text font-medium">可见范围</span></label>
            <select v-model="form.scopeType" class="select select-bordered">
              <option value="ALL">全校可见</option>
              <option value="DEPT">指定部门</option>
              <option value="USERS">指定用户</option>
            </select>
          </div>

          <!-- Scope IDs -->
          <div v-if="form.scopeType !== 'ALL'" class="form-control">
            <label class="label">
              <span class="label-text font-medium">{{ form.scopeType === 'DEPT' ? '部门ID' : '用户ID' }} (逗号分隔)</span>
            </label>
            <input v-model="scopeIdsInput" type="text" class="input input-bordered" placeholder="例如: 1,2,3" />
          </div>

          <!-- Pinned & Period -->
          <div class="grid grid-cols-2 gap-4">
            <div class="form-control">
              <label class="label cursor-pointer justify-start gap-3">
                <input v-model="form.isPinned" type="checkbox" class="checkbox checkbox-primary" />
                <span class="label-text font-medium">置顶</span>
              </label>
            </div>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div class="form-control">
              <label class="label"><span class="label-text font-medium">生效时间</span></label>
              <input v-model="form.startAt" type="datetime-local" class="input input-bordered" />
            </div>
            <div class="form-control">
              <label class="label"><span class="label-text font-medium">过期时间</span></label>
              <input v-model="form.endAt" type="datetime-local" class="input input-bordered" />
            </div>
          </div>
        </div>

        <div class="modal-action">
          <button class="btn btn-ghost" @click="closeModal">取消</button>
          <button class="btn btn-primary" :disabled="saving" @click="handleSave">
            <span v-if="saving" class="loading loading-spinner loading-sm"></span>
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
      <form method="dialog" class="modal-backdrop"><button>close</button></form>
    </dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { 
  queryNotices, createNotice, updateNotice, publishNotice, offlineNotice, deleteNotice,
  type NoticeVO, type NoticeDTO
} from '@/api/system'
import { useDialog } from '@/composables/useDialog'

const notices = ref<NoticeVO[]>([])
const loading = ref(false)
const saving = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loadingMore = ref(false)
const hasMore = ref(true)
const scrollContainer = ref<HTMLElement | null>(null)
const loadMoreTrigger = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null
const filterStatus = ref<number | undefined>(undefined)
const filterKeyword = ref('')
const dialog = useDialog()

const formModal = ref<HTMLDialogElement | null>(null)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const form = ref<NoticeDTO>({
  title: '',
  content: '',
  scopeType: 'ALL',
  scopeIds: [],
  isPinned: false,
  startAt: undefined,
  endAt: undefined
})
const scopeIdsInput = ref('')

onMounted(() => {
  loadNotices({ reset: true })
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

const loadNotices = async ({ append = false, reset = false } = {}) => {
  if (append && (loadingMore.value || loading.value)) return
  if (!append && loading.value) return
  if (reset) {
    currentPage.value = 1
    notices.value = []
    hasMore.value = true
  }
  append ? (loadingMore.value = true) : (loading.value = true)
  try {
    const res = await queryNotices({
      page: currentPage.value,
      size: pageSize.value,
      status: filterStatus.value,
      keyword: filterKeyword.value || undefined
    })
    const records = res.records || []
    total.value = res.total || 0
    notices.value = append ? [...notices.value, ...records] : records
    if (total.value) {
      hasMore.value = notices.value.length < total.value
    } else {
      hasMore.value = records.length >= pageSize.value
    }
  } catch (e) {
    console.error('Failed to load notices', e)
  } finally {
    append ? (loadingMore.value = false) : (loading.value = false)
  }
}

const loadMore = async () => {
  if (!hasMore.value || loading.value || loadingMore.value) return
  currentPage.value += 1
  await loadNotices({ append: true })
}

const handleSearch = () => {
  loadNotices({ reset: true })
}

const openCreateModal = () => {
  isEdit.value = false
  editId.value = null
  form.value = {
    title: '',
    content: '',
    scopeType: 'ALL',
    scopeIds: [],
    isPinned: false,
    startAt: undefined,
    endAt: undefined
  }
  scopeIdsInput.value = ''
  formModal.value?.showModal()
}

const openEditModal = (notice: NoticeVO) => {
  isEdit.value = true
  editId.value = notice.id
  form.value = {
    title: notice.title,
    content: notice.content,
    scopeType: notice.scopeType || 'ALL',
    scopeIds: notice.scopeIds || [],
    isPinned: notice.isPinned,
    startAt: notice.startAt ? notice.startAt.slice(0, 16) : undefined,
    endAt: notice.endAt ? notice.endAt.slice(0, 16) : undefined
  }
  scopeIdsInput.value = notice.scopeIds?.join(',') || ''
  formModal.value?.showModal()
}

const closeModal = () => {
  formModal.value?.close()
}

const handleSave = async () => {
  if (!form.value.title?.trim()) {
    await dialog.alert('请输入标题')
    return
  }
  if (!form.value.content?.trim()) {
    await dialog.alert('请输入内容')
    return
  }

  // Parse scope IDs
  if (form.value.scopeType !== 'ALL' && scopeIdsInput.value) {
    form.value.scopeIds = scopeIdsInput.value.split(',').map(s => parseInt(s.trim())).filter(n => !isNaN(n))
  } else {
    form.value.scopeIds = []
  }

  saving.value = true
  try {
    if (isEdit.value && editId.value) {
      await updateNotice(editId.value, form.value)
    } else {
    await createNotice(form.value)
  }
  closeModal()
  loadNotices({ reset: true })
  } catch (e: unknown) {
    await dialog.alert((e as ApiErrorLike)?.response?.data?.msg || (e as ApiErrorLike)?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const handlePublish = async (notice: NoticeVO) => {
  if (!await dialog.confirm(`确定发布公告「${notice.title}」？`)) return
  try {
    await publishNotice(notice.id)
    loadNotices({ reset: true })
  } catch (e: unknown) {
    await dialog.alert((e as ApiErrorLike)?.response?.data?.msg || (e as ApiErrorLike)?.message || '发布失败')
  }
}

const handleOffline = async (notice: NoticeVO) => {
  if (!await dialog.confirm(`确定下线公告「${notice.title}」？`)) return
  try {
    await offlineNotice(notice.id)
    loadNotices({ reset: true })
  } catch (e: unknown) {
    await dialog.alert((e as ApiErrorLike)?.response?.data?.msg || (e as ApiErrorLike)?.message || '下线失败')
  }
}

const handleDelete = async (notice: NoticeVO) => {
  if (!await dialog.confirm(`确定删除公告「${notice.title}」？此操作不可恢复！`)) return
  try {
    await deleteNotice(notice.id)
    loadNotices({ reset: true })
  } catch (e: unknown) {
    await dialog.alert((e as ApiErrorLike)?.response?.data?.msg || (e as ApiErrorLike)?.message || '删除失败')
  }
}

const getStatusClass = (status: number) => {
  switch (status) {
    case 0: return 'badge badge-ghost'
    case 1: return 'badge badge-success'
    case 2: return 'badge badge-warning'
    default: return 'badge'
  }
}

const formatDateTime = (dateStr?: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}
</script>
