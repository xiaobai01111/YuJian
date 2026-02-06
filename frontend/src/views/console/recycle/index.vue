<template>
  <div class="h-full flex flex-col overflow-hidden">
    <div class="flex-1 min-h-0 flex flex-col gap-4 overflow-hidden p-6">
      <div class="flex flex-wrap justify-between items-center gap-3">
        <div>
          <h1 class="text-2xl font-bold">回收站</h1>
          <p class="text-gray-500 mt-1">通过页面切换查看帖子、评论、举报的已删除内容</p>
        </div>
        <button class="btn btn-ghost btn-sm" :disabled="activeState.loading || activeState.loadingMore" @click="reloadCurrent">
          刷新
        </button>
      </div>

      <div v-if="availableTabs.length > 0" class="tabs tabs-boxed w-fit">
        <button
          v-for="tab in availableTabs"
          :key="tab.key"
          class="tab"
          :class="{ 'tab-active': activeTab === tab.key }"
          @click="switchTab(tab.key)"
        >
          {{ tab.label }}
        </button>
      </div>

      <div v-if="availableTabs.length === 0" class="card bg-base-100 shadow-sm flex-1 min-h-0">
        <div class="card-body flex items-center justify-center text-base-content/60">
          没有可访问的回收站权限
        </div>
      </div>

      <template v-else>
        <div class="card bg-base-100 shadow-sm">
          <div class="card-body p-4">
            <div v-if="activeTab === 'post'" class="flex flex-wrap gap-4 items-center">
              <div class="form-control">
                <select
                  v-model="postFilters.board"
                  class="select select-bordered select-sm w-40"
                  @change="reloadCurrent"
                >
                  <option value="">全部板块</option>
                  <option v-for="option in boardOptions" :key="option.key" :value="option.key">
                    {{ option.label }}
                  </option>
                </select>
              </div>
              <div class="form-control">
                <input
                  v-model="postFilters.keyword"
                  type="text"
                  placeholder="搜索关键词..."
                  class="input input-bordered input-sm w-52"
                  @keyup.enter="reloadCurrent"
                />
              </div>
              <button class="btn btn-sm btn-ghost" @click="reloadCurrent">搜索</button>
            </div>

            <div v-else-if="activeTab === 'comment'" class="flex flex-wrap gap-4 items-center">
              <div class="form-control">
                <input
                  v-model="commentFilters.postId"
                  type="text"
                  placeholder="帖子ID"
                  class="input input-bordered input-sm w-32"
                  @keyup.enter="reloadCurrent"
                />
              </div>
              <div class="form-control">
                <input
                  v-model="commentFilters.keyword"
                  type="text"
                  placeholder="搜索内容..."
                  class="input input-bordered input-sm w-52"
                  @keyup.enter="reloadCurrent"
                />
              </div>
              <button class="btn btn-sm btn-ghost" @click="reloadCurrent">搜索</button>
            </div>

            <div v-else class="flex flex-wrap gap-4 items-center">
              <div class="form-control">
                <select
                  v-model="reportFilters.status"
                  class="select select-bordered select-sm w-32"
                  @change="reloadCurrent"
                >
                  <option :value="undefined">全部状态</option>
                  <option :value="0">待处理</option>
                  <option :value="1">已处理</option>
                </select>
              </div>
              <button class="btn btn-sm btn-ghost" @click="reloadCurrent">搜索</button>
            </div>
          </div>
        </div>

        <div class="card bg-base-100 shadow-sm flex-1 min-h-0">
          <div class="card-body p-0 flex flex-col min-h-0">
            <div ref="scrollContainer" class="flex-1 overflow-auto">
              <div class="overflow-x-auto">
                <table class="table">
                  <thead>
                    <tr v-if="activeTab === 'post'">
                      <th class="w-16">ID</th>
                      <th>标题</th>
                      <th class="w-32">板块</th>
                      <th class="w-28">作者</th>
                      <th class="w-40">删除时间</th>
                      <th class="w-44">操作</th>
                    </tr>
                    <tr v-else-if="activeTab === 'comment'">
                      <th class="w-16">ID</th>
                      <th class="w-24">帖子ID</th>
                      <th>内容</th>
                      <th class="w-28">作者</th>
                      <th class="w-40">删除时间</th>
                      <th class="w-44">操作</th>
                    </tr>
                    <tr v-else>
                      <th class="w-16">ID</th>
                      <th>原因</th>
                      <th class="w-40">帖子</th>
                      <th class="w-28">举报者</th>
                      <th class="w-24">状态</th>
                      <th class="w-40">删除时间</th>
                      <th class="w-44">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-if="activeState.loading">
                      <td :colspan="tableColspan" class="text-center py-8">
                        <span class="loading loading-spinner loading-md"></span>
                      </td>
                    </tr>

                    <tr v-else-if="activeTab === 'post' && postState.items.length === 0">
                      <td colspan="6" class="text-center py-8 text-gray-500">暂无帖子回收内容</td>
                    </tr>
                    <tr v-else-if="activeTab === 'comment' && commentState.items.length === 0">
                      <td colspan="6" class="text-center py-8 text-gray-500">暂无评论回收内容</td>
                    </tr>
                    <tr v-else-if="activeTab === 'report' && reportState.items.length === 0">
                      <td colspan="7" class="text-center py-8 text-gray-500">暂无举报回收内容</td>
                    </tr>

                    <template v-else-if="activeTab === 'post'">
                      <tr v-for="post in postState.items" :key="post.id" class="hover">
                        <td>{{ post.id }}</td>
                        <td>
                          <div class="font-medium line-clamp-1 max-w-xs">{{ post.title }}</div>
                        </td>
                        <td>
                          <div class="flex flex-wrap gap-1">
                            <span
                              v-for="board in getPostBoards(post)"
                              :key="board"
                              class="badge badge-ghost badge-sm"
                            >
                              {{ getBoardLabel(board) }}
                            </span>
                          </div>
                        </td>
                        <td>{{ post.author?.nickname || post.author?.username || '-' }}</td>
                        <td>{{ formatDateTime(post.updatedAt || post.createdAt) }}</td>
                        <td>
                          <div class="flex gap-1">
                            <button
                              v-if="canRestorePost"
                              class="btn btn-success btn-xs"
                              @click="handleRestorePost(post)"
                            >
                              恢复
                            </button>
                            <button
                              v-if="canPurgePost"
                              class="btn btn-error btn-xs"
                              @click="handlePurgePost(post)"
                            >
                              彻底删除
                            </button>
                          </div>
                        </td>
                      </tr>
                    </template>

                    <template v-else-if="activeTab === 'comment'">
                      <tr v-for="comment in commentState.items" :key="comment.id" class="hover">
                        <td>{{ comment.id }}</td>
                        <td>{{ comment.postId }}</td>
                        <td>
                          <div class="line-clamp-2 max-w-xl">{{ comment.content }}</div>
                        </td>
                        <td>{{ comment.author?.nickname || comment.author?.username || '-' }}</td>
                        <td>{{ formatDateTime(comment.createdAt) }}</td>
                        <td>
                          <div class="flex gap-1">
                            <button
                              v-if="canRestoreComment"
                              class="btn btn-success btn-xs"
                              @click="handleRestoreComment(comment)"
                            >
                              恢复
                            </button>
                            <button
                              v-if="canPurgeComment"
                              class="btn btn-error btn-xs"
                              @click="handlePurgeComment(comment)"
                            >
                              彻底删除
                            </button>
                          </div>
                        </td>
                      </tr>
                    </template>

                    <template v-else>
                      <tr v-for="report in reportState.items" :key="report.id" class="hover">
                        <td>{{ report.id }}</td>
                        <td>
                          <div class="line-clamp-2 max-w-lg">{{ report.reason }}</div>
                        </td>
                        <td>
                          <div class="line-clamp-1 max-w-xs">#{{ report.post?.id }} {{ report.post?.title || '' }}</div>
                        </td>
                        <td>{{ report.reporter?.nickname || report.reporter?.username || '-' }}</td>
                        <td>
                          <span :class="getReportStatusClass(report.status)">
                            {{ report.status === 1 ? '已处理' : '待处理' }}
                          </span>
                        </td>
                        <td>{{ formatDateTime(report.createdAt) }}</td>
                        <td>
                          <div class="flex gap-1">
                            <button
                              v-if="canRestoreReport"
                              class="btn btn-success btn-xs"
                              @click="handleRestoreReport(report)"
                            >
                              恢复
                            </button>
                            <button
                              v-if="canPurgeReport"
                              class="btn btn-error btn-xs"
                              @click="handlePurgeReport(report)"
                            >
                              彻底删除
                            </button>
                          </div>
                        </td>
                      </tr>
                    </template>
                  </tbody>
                </table>
              </div>
            </div>
            <div ref="loadMoreTrigger" class="h-6" aria-hidden="true"></div>
          </div>
        </div>

        <div class="flex items-center justify-between text-sm text-base-content/60">
          <div>已加载 {{ activeLoaded }} / {{ activeTotal || '-' }} 条</div>
          <div v-if="activeState.loadingMore">正在加载更多...</div>
          <div v-else-if="!activeState.hasMore && activeLoaded > 0">没有更多了</div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getRecycleComments,
  getRecyclePosts,
  getRecycleReports,
  purgeRecycleComment,
  purgeRecyclePost,
  purgeRecycleReport,
  restoreRecycleComment,
  restoreRecyclePost,
  restoreRecycleReport,
  type CommentConsoleVO,
  type PostVO,
  type ReportVO
} from '@/api/recycle'
import { BOARD_OPTIONS, getBoardLabel, getPostBoards } from '@/utils/boards'
import { useDialog } from '@/composables/useDialog'
import { useUserStore } from '@/stores/user'

type TabKey = 'post' | 'comment' | 'report'

interface PagedState<T> {
  items: T[]
  loading: boolean
  loadingMore: boolean
  total: number
  page: number
  size: number
  hasMore: boolean
  initialized: boolean
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const dialog = useDialog()
const boardOptions = BOARD_OPTIONS

const canViewPost = computed(() => userStore.hasPermission('content:recycle:post:list'))
const canViewComment = computed(() => userStore.hasPermission('content:recycle:comment:list'))
const canViewReport = computed(() => userStore.hasPermission('content:recycle:report:list'))
const canRestorePost = computed(() => userStore.hasPermission('content:recycle:post:restore'))
const canPurgePost = computed(() => userStore.hasPermission('content:recycle:post:purge'))
const canRestoreComment = computed(() => userStore.hasPermission('content:recycle:comment:restore'))
const canPurgeComment = computed(() => userStore.hasPermission('content:recycle:comment:purge'))
const canRestoreReport = computed(() => userStore.hasPermission('content:recycle:report:restore'))
const canPurgeReport = computed(() => userStore.hasPermission('content:recycle:report:purge'))

const availableTabs = computed(() => {
  const tabs: Array<{ key: TabKey; label: string }> = []
  if (canViewPost.value) tabs.push({ key: 'post', label: '帖子' })
  if (canViewComment.value) tabs.push({ key: 'comment', label: '评论' })
  if (canViewReport.value) tabs.push({ key: 'report', label: '举报' })
  return tabs
})

const postFilters = reactive({
  board: '',
  keyword: ''
})

const commentFilters = reactive({
  postId: '',
  keyword: ''
})

const reportFilters = reactive({
  status: undefined as number | undefined
})

const postState = reactive<PagedState<PostVO>>({
  items: [],
  loading: false,
  loadingMore: false,
  total: 0,
  page: 1,
  size: 10,
  hasMore: true,
  initialized: false
})

const commentState = reactive<PagedState<CommentConsoleVO>>({
  items: [],
  loading: false,
  loadingMore: false,
  total: 0,
  page: 1,
  size: 10,
  hasMore: true,
  initialized: false
})

const reportState = reactive<PagedState<ReportVO>>({
  items: [],
  loading: false,
  loadingMore: false,
  total: 0,
  page: 1,
  size: 10,
  hasMore: true,
  initialized: false
})

const normalizeTab = (value: unknown): TabKey | null => {
  const text = String(value || '').toLowerCase()
  if (text === 'post' || text === 'comment' || text === 'report') return text
  return null
}

const pickDefaultTab = (): TabKey | null => availableTabs.value[0]?.key ?? null
const initialTab = normalizeTab(route.query.tab) || pickDefaultTab() || 'post'
const activeTab = ref<TabKey>(initialTab)

const activeState = computed(() => {
  if (activeTab.value === 'post') return postState
  if (activeTab.value === 'comment') return commentState
  return reportState
})

const activeLoaded = computed(() => activeState.value.items.length)
const activeTotal = computed(() => activeState.value.total)
const tableColspan = computed(() => (activeTab.value === 'report' ? 7 : 6))

const scrollContainer = ref<HTMLElement | null>(null)
const loadMoreTrigger = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

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

const updatePagedState = <T>(state: PagedState<T>, records: T[], append: boolean) => {
  state.items = append ? [...state.items, ...records] : records
  if (state.total) {
    state.hasMore = state.items.length < state.total
  } else {
    state.hasMore = records.length >= state.size
  }
  state.initialized = true
}

const loadPosts = async ({ append = false, reset = false } = {}) => {
  if (append && (postState.loadingMore || postState.loading)) return
  if (!append && postState.loading) return
  if (reset) {
    postState.page = 1
    postState.items = []
    postState.hasMore = true
  }
  append ? (postState.loadingMore = true) : (postState.loading = true)
  try {
    const response = await getRecyclePosts({
      page: postState.page,
      size: postState.size,
      board: postFilters.board || undefined,
      keyword: postFilters.keyword.trim() || undefined
    })
    postState.total = response.total || 0
    updatePagedState(postState, response.records || [], append)
  } catch (error) {
    console.error('Failed to load recycle posts', error)
  } finally {
    append ? (postState.loadingMore = false) : (postState.loading = false)
  }
}

const loadComments = async ({ append = false, reset = false } = {}) => {
  if (append && (commentState.loadingMore || commentState.loading)) return
  if (!append && commentState.loading) return
  if (reset) {
    commentState.page = 1
    commentState.items = []
    commentState.hasMore = true
  }
  append ? (commentState.loadingMore = true) : (commentState.loading = true)
  try {
    const response = await getRecycleComments({
      page: commentState.page,
      size: commentState.size,
      postId: commentFilters.postId.trim() ? Number(commentFilters.postId) : undefined,
      keyword: commentFilters.keyword.trim() || undefined
    })
    commentState.total = response.total || 0
    updatePagedState(commentState, response.records || [], append)
  } catch (error) {
    console.error('Failed to load recycle comments', error)
  } finally {
    append ? (commentState.loadingMore = false) : (commentState.loading = false)
  }
}

const loadReports = async ({ append = false, reset = false } = {}) => {
  if (append && (reportState.loadingMore || reportState.loading)) return
  if (!append && reportState.loading) return
  if (reset) {
    reportState.page = 1
    reportState.items = []
    reportState.hasMore = true
  }
  append ? (reportState.loadingMore = true) : (reportState.loading = true)
  try {
    const response = await getRecycleReports({
      page: reportState.page,
      size: reportState.size,
      status: reportFilters.status
    })
    reportState.total = response.total || 0
    updatePagedState(reportState, response.records || [], append)
  } catch (error) {
    console.error('Failed to load recycle reports', error)
  } finally {
    append ? (reportState.loadingMore = false) : (reportState.loading = false)
  }
}

const loadActive = async ({ append = false, reset = false } = {}) => {
  if (activeTab.value === 'post') {
    await loadPosts({ append, reset })
    return
  }
  if (activeTab.value === 'comment') {
    await loadComments({ append, reset })
    return
  }
  await loadReports({ append, reset })
}

const ensureActiveLoaded = async () => {
  if (availableTabs.value.length === 0) return
  if (!activeState.value.initialized) {
    await loadActive({ reset: true })
  }
}

const reloadCurrent = async () => {
  await loadActive({ reset: true })
}

const loadMore = async () => {
  if (!activeState.value.hasMore || activeState.value.loading || activeState.value.loadingMore) return
  activeState.value.page += 1
  await loadActive({ append: true })
}

const switchTab = (tab: TabKey) => {
  if (activeTab.value === tab) return
  activeTab.value = tab
}

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  const date = new Date(value)
  return `${date.toLocaleDateString()} ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
}

const getReportStatusClass = (status?: number) => {
  if (status === 0) return 'badge badge-warning badge-sm'
  if (status === 1) return 'badge badge-success badge-sm'
  return 'badge badge-ghost badge-sm'
}

const needReasonByAuthorId = (authorId?: number) => {
  const currentUserId = userStore.userInfo?.id
  return !currentUserId || authorId !== currentUserId
}

const promptReason = async (required: boolean) => {
  const tip = required ? '请输入操作原因（必填）' : '请输入操作原因（可选）'
  const reason = await dialog.prompt(tip, { required, multiline: true })
  if (reason == null) return required ? null : ''
  return reason.trim()
}

const handleRestorePost = async (post: PostVO) => {
  if (!await dialog.confirm(`确认恢复帖子「${post.title}」？`)) return
  const reason = await promptReason(needReasonByAuthorId(post.author?.id))
  if (reason === null) return
  try {
    await restoreRecyclePost(post.id, reason || undefined)
    await reloadCurrent()
  } catch (error: unknown) {
    await dialog.alert((error as ApiErrorLike)?.message || '恢复失败')
  }
}

const handlePurgePost = async (post: PostVO) => {
  if (!await dialog.confirm(`确认彻底删除帖子「${post.title}」？此操作不可恢复。`)) return
  const reason = await promptReason(needReasonByAuthorId(post.author?.id))
  if (reason === null) return
  try {
    await purgeRecyclePost(post.id, reason || undefined)
    await reloadCurrent()
  } catch (error: unknown) {
    await dialog.alert((error as ApiErrorLike)?.message || '删除失败')
  }
}

const handleRestoreComment = async (comment: CommentConsoleVO) => {
  if (!await dialog.confirm(`确认恢复评论 #${comment.id}？`)) return
  const reason = await promptReason(needReasonByAuthorId(comment.author?.id))
  if (reason === null) return
  try {
    await restoreRecycleComment(comment.id, reason || undefined)
    await reloadCurrent()
  } catch (error: unknown) {
    await dialog.alert((error as ApiErrorLike)?.message || '恢复失败')
  }
}

const handlePurgeComment = async (comment: CommentConsoleVO) => {
  if (!await dialog.confirm(`确认彻底删除评论 #${comment.id}？此操作不可恢复。`)) return
  const reason = await promptReason(needReasonByAuthorId(comment.author?.id))
  if (reason === null) return
  try {
    await purgeRecycleComment(comment.id, reason || undefined)
    await reloadCurrent()
  } catch (error: unknown) {
    await dialog.alert((error as ApiErrorLike)?.message || '删除失败')
  }
}

const handleRestoreReport = async (report: ReportVO) => {
  if (!await dialog.confirm(`确认恢复举报 #${report.id}？`)) return
  const reason = await promptReason(needReasonByAuthorId(report.reporter?.id))
  if (reason === null) return
  try {
    await restoreRecycleReport(report.id, reason || undefined)
    await reloadCurrent()
  } catch (error: unknown) {
    await dialog.alert((error as ApiErrorLike)?.message || '恢复失败')
  }
}

const handlePurgeReport = async (report: ReportVO) => {
  if (!await dialog.confirm(`确认彻底删除举报 #${report.id}？此操作不可恢复。`)) return
  const reason = await promptReason(needReasonByAuthorId(report.reporter?.id))
  if (reason === null) return
  try {
    await purgeRecycleReport(report.id, reason || undefined)
    await reloadCurrent()
  } catch (error: unknown) {
    await dialog.alert((error as ApiErrorLike)?.message || '删除失败')
  }
}

watch(availableTabs, tabs => {
  if (tabs.length === 0) return
  const activeExists = tabs.some(item => item.key === activeTab.value)
  if (!activeExists) {
    activeTab.value = tabs[0]!.key
  }
}, { immediate: true })

watch(
  () => route.query.tab,
  queryTab => {
    const normalized = normalizeTab(queryTab)
    if (!normalized) return
    if (!availableTabs.value.some(item => item.key === normalized)) return
    if (normalized !== activeTab.value) {
      activeTab.value = normalized
    }
  }
)

watch(
  activeTab,
  async tab => {
    if (availableTabs.value.length === 0) return
    if (route.query.tab !== tab) {
      const query = { ...route.query, tab }
      await router.replace({ query })
    }
    await ensureActiveLoaded()
    await nextTick()
    setupObserver()
  },
  { immediate: true }
)

onMounted(() => {
  nextTick(() => setupObserver())
})

onUnmounted(() => {
  observer?.disconnect()
  observer = null
})
</script>
