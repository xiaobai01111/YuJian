<template>
  <div class="h-full flex flex-col overflow-hidden">
    <div class="flex-1 min-h-0 flex flex-col gap-6 overflow-hidden p-6">
      <div class="flex flex-wrap justify-between items-center gap-3">
        <div>
          <h1 class="text-2xl font-bold">评论管理</h1>
          <p class="text-gray-500 mt-1">管理帖子评论内容</p>
        </div>
        <div class="flex items-center gap-2">
          <button
            v-if="canBatchDelete"
            class="btn btn-outline btn-sm"
            :disabled="selectedIds.length === 0"
            @click="handleBatchDelete"
          >
            批量删除
          </button>
          <button class="btn btn-ghost btn-sm" @click="loadComments({ reset: true })">
            刷新
          </button>
        </div>
      </div>

      <div class="card bg-base-100 shadow-sm">
        <div class="card-body p-4">
          <div class="flex flex-wrap gap-4 items-center">
            <div class="form-control">
              <input
                v-model="filters.postId"
                type="text"
                placeholder="帖子ID"
                class="input input-bordered input-sm w-32"
                @keyup.enter="loadComments({ reset: true })"
              />
            </div>
            <div class="form-control">
              <select v-model="filters.status" class="select select-bordered select-sm w-32" @change="loadComments({ reset: true })">
                <option :value="undefined">全部状态</option>
                <option :value="0">正常</option>
                <option :value="1">已删除</option>
              </select>
            </div>
            <div class="form-control">
              <input
                v-model="filters.keyword"
                type="text"
                placeholder="搜索内容..."
                class="input input-bordered input-sm w-52"
                @keyup.enter="loadComments({ reset: true })"
              />
            </div>
            <button class="btn btn-sm btn-ghost" @click="loadComments({ reset: true })">
              搜索
            </button>
          </div>
        </div>
      </div>

      <div class="card bg-base-100 shadow-sm flex-1 min-h-0">
        <div class="card-body p-0 flex flex-col min-h-0">
          <div ref="scrollContainer" class="flex-1 overflow-auto">
            <div class="overflow-x-auto">
              <table class="table">
                <thead>
                  <tr>
                    <th class="w-12">
                      <input
                        type="checkbox"
                        class="checkbox checkbox-sm"
                        :checked="allSelected"
                        :disabled="!canBatchDelete || selectableComments.length === 0"
                        @change="toggleAll"
                      />
                    </th>
                    <th class="w-16">ID</th>
                    <th class="w-24">帖子ID</th>
                    <th>内容</th>
                    <th class="w-28">作者</th>
                    <th class="w-24">状态</th>
                    <th class="w-40">发布时间</th>
                    <th class="w-44">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="loading">
                    <td colspan="8" class="text-center py-8">
                      <span class="loading loading-spinner loading-md"></span>
                    </td>
                  </tr>
                  <tr v-else-if="comments.length === 0">
                    <td colspan="8" class="text-center py-8 text-gray-500">暂无数据</td>
                  </tr>
                  <tr v-for="comment in comments" :key="comment.id" class="hover">
                    <td>
                      <input
                        v-model="selectedIds"
                        type="checkbox"
                        class="checkbox checkbox-sm"
                        :value="comment.id"
                        :disabled="!canBatchDelete || comment.status !== 0"
                      />
                    </td>
                    <td>{{ comment.id }}</td>
                    <td>
                      <button class="btn btn-ghost btn-xs" @click="goToPost(comment.postId)">
                        {{ comment.postId }}
                      </button>
                    </td>
                    <td>
                      <div class="line-clamp-2 max-w-xl">{{ comment.content }}</div>
                    </td>
                    <td>{{ getAuthorName(comment) }}</td>
                    <td>
                      <span :class="getStatusClass(comment.status)">
                        {{ comment.status === 1 ? '已删除' : '正常' }}
                      </span>
                    </td>
                    <td>{{ formatDateTime(comment.createdAt) }}</td>
                    <td>
                      <div class="flex gap-1">
                        <button
                          v-if="canEdit && comment.status === 0"
                          class="btn btn-ghost btn-xs"
                          @click="handleEdit(comment)"
                        >修改</button>
                        <button
                          v-if="canDelete"
                          class="btn btn-error btn-xs"
                          @click="handleDelete(comment)"
                        >删除</button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <div ref="loadMoreTrigger" class="h-6" aria-hidden="true"></div>
        </div>
      </div>

      <div class="flex justify-between items-center text-sm text-base-content/60">
        <div>已加载 {{ comments.length }} / {{ total || '-' }} 条</div>
        <div v-if="loadingMore">正在加载更多...</div>
        <div v-else-if="!hasMore && comments.length > 0">没有更多了</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, nextTick, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { batchDeleteConsoleComments, deleteConsoleComment, getConsoleComments, updateConsoleComment, type CommentConsoleVO } from '@/api/comment'
import { useUserStore } from '@/stores/user'
import { useDialog } from '@/composables/useDialog'

const router = useRouter()
const userStore = useUserStore()
const dialog = useDialog()

const comments = ref<CommentConsoleVO[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const hasMore = ref(true)
const scrollContainer = ref<HTMLElement | null>(null)
const loadMoreTrigger = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null
const selectedIds = ref<number[]>([])

const filters = reactive({
  postId: '',
  status: undefined as number | undefined,
  keyword: ''
})

const canDelete = computed(() => userStore.hasPermission('content:comment:delete'))
const canEdit = computed(() => userStore.hasPermission('content:comment:edit'))
const canBatchDelete = computed(() => userStore.hasPermission('content:comment:batch-delete'))

const selectableComments = computed(() => comments.value.filter(comment => comment.status === 0))

const allSelected = computed(() => {
  return selectableComments.value.length > 0
    && selectableComments.value.every(comment => selectedIds.value.includes(comment.id))
})

const getAuthorName = (comment: CommentConsoleVO) => {
  return comment.author?.nickname || comment.author?.username || '-'
}

const parsePostId = (value: string) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

const loadComments = async ({ append = false, reset = false } = {}) => {
  if (append && (loadingMore.value || loading.value)) return
  if (!append && loading.value) return
  if (reset) {
    currentPage.value = 1
    comments.value = []
    hasMore.value = true
  }
  append ? (loadingMore.value = true) : (loading.value = true)
  try {
    const res: any = await getConsoleComments({
      page: currentPage.value,
      size: pageSize.value,
      postId: filters.postId.trim() ? parsePostId(filters.postId.trim()) : undefined,
      status: filters.status,
      keyword: filters.keyword.trim() || undefined
    })
    const records = res.records || []
    total.value = res.total || 0
    comments.value = append ? [...comments.value, ...records] : records
    if (total.value) {
      hasMore.value = comments.value.length < total.value
    } else {
      hasMore.value = records.length >= pageSize.value
    }
    if (!append) {
      selectedIds.value = []
    }
  } catch (error) {
    console.error('Failed to load comments', error)
  } finally {
    append ? (loadingMore.value = false) : (loading.value = false)
  }
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
  currentPage.value += 1
  await loadComments({ append: true })
}

const toggleAll = () => {
  if (allSelected.value) {
    selectedIds.value = []
  } else {
    selectedIds.value = selectableComments.value.map(comment => comment.id)
  }
}

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  const date = new Date(value)
  return `${date.toLocaleDateString()} ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
}

const getStatusClass = (status: number) => {
  if (status === 0) return 'badge badge-success badge-sm'
  if (status === 1) return 'badge badge-error badge-sm'
  return 'badge badge-ghost badge-sm'
}

const needReason = (comment: CommentConsoleVO) => {
  const currentUserId = userStore.userInfo?.id
  return !currentUserId || comment.author?.id !== currentUserId
}

const promptReason = async (required: boolean) => {
  const tip = required ? '请输入操作原因（必填）' : '请输入操作原因（可选）'
  const reason = await dialog.prompt(tip, { required, multiline: true })
  if (reason == null) return required ? null : ''
  return reason.trim()
}

const handleEdit = async (comment: CommentConsoleVO) => {
  const content = await dialog.prompt('请输入新的评论内容', {
    defaultValue: comment.content || ''
  })
  if (!content || !content.trim()) return
  const reason = await promptReason(needReason(comment))
  if (reason === null) return
  try {
    await updateConsoleComment(comment.id, { content: content.trim() }, reason || undefined)
    await loadComments({ reset: true })
  } catch (error: any) {
    await dialog.alert(error?.message || '修改失败')
  }
}

const handleDelete = async (comment: CommentConsoleVO) => {
  if (!await dialog.confirm(`确认删除评论 #${comment.id}？`)) return
  const reason = await promptReason(needReason(comment))
  if (reason === null) return
  try {
    await deleteConsoleComment(comment.id, reason || undefined)
    await loadComments({ reset: true })
  } catch (error: any) {
    await dialog.alert(error?.message || '删除失败')
  }
}

const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) return
  if (!await dialog.confirm(`确认删除选中的 ${selectedIds.value.length} 条评论？`)) return
  const currentUserId = userStore.userInfo?.id
  const selectedComments = comments.value.filter(comment => selectedIds.value.includes(comment.id))
  const need = selectedComments.some(comment => !currentUserId || comment.author?.id !== currentUserId)
  const reason = await promptReason(need)
  if (reason === null) return
  try {
    await batchDeleteConsoleComments(selectedIds.value, reason || undefined)
    await loadComments({ reset: true })
  } catch (error: any) {
    await dialog.alert(error?.message || '批量删除失败')
  }
}

const goToPost = (postId: number) => {
  router.push(`/posts/${postId}`)
}

onMounted(() => {
  loadComments({ reset: true })
  nextTick(() => setupObserver())
})

onUnmounted(() => {
  observer?.disconnect()
  observer = null
})
</script>
