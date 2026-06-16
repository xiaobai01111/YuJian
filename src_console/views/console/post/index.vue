<template>
  <div class="h-full flex flex-col overflow-hidden">
    <div class="flex-1 min-h-0 flex flex-col gap-6 overflow-hidden p-6">
      <!-- Header -->
      <div class="flex justify-between items-center">
        <div>
          <h1 class="text-2xl font-bold">帖子管理</h1>
          <p class="text-gray-500 mt-1">管理系统帖子内容</p>
        </div>
        <button v-if="canAdd" class="btn btn-primary" @click="goToPublish">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          发布帖子
        </button>
      </div>

      <!-- Filters -->
      <div class="card bg-base-100 shadow-sm">
        <div class="card-body p-4">
          <div class="flex flex-wrap gap-4 items-center">
            <div class="form-control">
              <select v-model="filters.board" class="select select-bordered select-sm w-40" @change="loadPosts({ reset: true })">
                <option value="">全部板块</option>
                <option v-for="option in boardOptions" :key="option.key" :value="option.key">
                  {{ option.label }}
                </option>
              </select>
            </div>
            <div class="form-control">
              <select v-model="filters.status" class="select select-bordered select-sm w-32" @change="loadPosts({ reset: true })">
                <option :value="undefined">全部状态</option>
                <option :value="0">正常</option>
                <option :value="1">已解决</option>
                <option :value="2">已删除</option>
                <option :value="3">待审核</option>
                <option :value="4">已下架</option>
                <option :value="5">已售出</option>
              </select>
            </div>
            <div class="form-control">
              <input
                v-model="filters.keyword"
                type="text"
                placeholder="搜索关键词..."
                class="input input-bordered input-sm w-52"
                @keyup.enter="loadPosts({ reset: true })"
              />
            </div>
            <button class="btn btn-sm btn-ghost" @click="loadPosts({ reset: true })">
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
          <div ref="scrollContainer" class="flex-1 overflow-auto">
            <div class="overflow-x-auto">
              <table class="table">
              <thead>
                <tr>
                  <th class="w-14">ID</th>
                  <th>标题</th>
                  <th class="w-40">板块</th>
                  <th class="w-24">作者</th>
                  <th class="w-24">状态</th>
                  <th class="w-40">发布时间</th>
                  <th class="w-52">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="loading">
                  <td colspan="7" class="text-center py-8">
                    <span class="loading loading-spinner loading-md"></span>
                  </td>
                </tr>
                <tr v-else-if="posts.length === 0">
                  <td colspan="7" class="text-center py-8 text-gray-500">暂无数据</td>
                </tr>
                <tr v-for="post in posts" :key="post.id" class="hover">
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
                  <td>
                    <span>{{ post.isAnonymous ? '匿名' : (post.author?.nickname || post.author?.username || '-') }}</span>
                  </td>
                  <td>
                    <span :class="getStatusClass(post.status)">{{ getStatusText(post.status) }}</span>
                  </td>
                  <td>{{ formatDateTime(post.createdAt) }}</td>
                  <td>
                    <div class="flex flex-wrap gap-1">
                      <button class="btn btn-ghost btn-xs" @click="goToDetail(post.id)">查看</button>
                      <button
                        v-if="canResolve && post.status === 0 && isResolvePost(post)"
                        class="btn btn-success btn-xs"
                        @click="handleResolve(post)"
                      >已解决</button>
                      <button
                        v-if="canSold && post.status === 0 && isMarketPost(post)"
                        class="btn btn-warning btn-xs"
                        @click="handleSold(post)"
                      >已售出</button>
                      <button
                        v-if="canReject && post.status === 0 && !post.authorIsAdmin"
                        class="btn btn-outline btn-xs"
                        @click="handleReject(post)"
                      >驳回</button>
                      <button
                        v-if="canPin && !post.showOnHome"
                        class="btn btn-info btn-xs"
                        @click="handlePin(post)"
                      >置顶</button>
                      <button
                        v-if="canPin && post.showOnHome"
                        class="btn btn-ghost btn-xs"
                        @click="handleUnpin(post)"
                      >取消置顶</button>
                      <button
                        v-if="canOffline && post.status !== 4 && post.status !== 2"
                        class="btn btn-ghost btn-xs"
                        @click="handleOffline(post)"
                      >下架</button>
                      <button
                        v-if="canOnline && post.status === 4"
                        class="btn btn-ghost btn-xs"
                        @click="handleOnline(post)"
                      >上架</button>
                      <button
                        v-if="canLock && !post.isLocked"
                        class="btn btn-ghost btn-xs"
                        @click="handleLock(post)"
                      >锁定</button>
                      <button
                        v-if="canUnlock && post.isLocked"
                        class="btn btn-ghost btn-xs"
                        @click="handleUnlock(post)"
                      >解锁</button>
                      <button
                        v-if="canCommentList"
                        class="btn btn-ghost btn-xs"
                        @click="goToComments(post.id)"
                      >评论</button>
                      <button
                        v-if="canDelete"
                        class="btn btn-error btn-xs"
                        @click="handleDelete(post)"
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
        <div>已加载 {{ posts.length }} / {{ total || '-' }} 条</div>
        <div v-if="loadingMore">正在加载更多...</div>
        <div v-else-if="!hasMore && posts.length > 0">没有更多了</div>
      </div>
    </div>

    <!-- Publish Modal -->
    <PostPublishModal v-model="showPublish" :use-console-api="true" @success="handlePublishSuccess" />
    <PostDetailModal v-model="showPostDetail" :postId="selectedPostId" @updated="loadPosts({ reset: true })" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import {
  getConsolePostList,
  resolveConsolePost,
  deleteConsolePost,
  soldConsolePost,
  rejectConsolePost,
  pinConsolePost,
  unpinConsolePost,
  offlineConsolePost,
  onlineConsolePost,
  lockConsolePost,
  unlockConsolePost,
  type PostVO
} from '@/api/post'
import { BOARD_OPTIONS, getBoardLabel, getPostBoards, type BoardKey } from '@/utils/boards'
import { useUserStore } from '@/stores/user'
import PostPublishModal from '@/components/post/PostPublishModal.vue'
import PostDetailModal from '@/components/post/PostDetailModal.vue'
import { useDialog } from '@/composables/useDialog'

const userStore = useUserStore()
const dialog = useDialog()
const router = useRouter()

const posts = ref<PostVO[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const hasMore = ref(true)
const scrollContainer = ref<HTMLElement | null>(null)
const loadMoreTrigger = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null
const showPublish = ref(false)
const showPostDetail = ref(false)
const selectedPostId = ref<number | null>(null)

const filters = reactive({
  board: '',
  status: undefined as number | undefined,
  keyword: ''
})

const canViewBoard = (key: string) => {
  return userStore.hasPermission(`content:channel:${key}:view`) || userStore.hasPermission(`content:channel:${key}:manage`)
}

const boardOptions = computed(() => {
  if (userStore.userInfo?.permissions?.includes('*')) {
    return BOARD_OPTIONS
  }
  return BOARD_OPTIONS.filter(option => canViewBoard(option.key))
})

const resolveBoards: BoardKey[] = ['help', 'treehole', 'lost-found']
const isMarketPost = (post: PostVO) => {
  return getPostBoards(post).includes('market')
}
const isResolvePost = (post: PostVO) => {
  const boards = getPostBoards(post)
  return !boards.includes('market') && resolveBoards.some(board => boards.includes(board))
}

const canAdd = computed(() => userStore.hasPermission('content:post:add'))
const canDelete = computed(() => userStore.hasPermission('content:post:delete'))
const canResolve = computed(() => userStore.hasPermission('content:post:resolve'))
const canSold = computed(() => userStore.hasPermission('content:post:sold'))
const canReject = computed(() => userStore.hasPermission('content:post:reject'))
const canPin = computed(() => userStore.hasPermission('content:post:pin'))
const canOffline = computed(() => userStore.hasPermission('content:post:offline'))
const canOnline = computed(() => userStore.hasPermission('content:post:online'))
const canLock = computed(() => userStore.hasPermission('content:post:lock'))
const canUnlock = computed(() => userStore.hasPermission('content:post:unlock'))
const canCommentList = computed(() => userStore.hasPermission('content:comment:list'))

const loadPosts = async ({ append = false, reset = false } = {}) => {
  if (append && (loadingMore.value || loading.value)) return
  if (!append && loading.value) return
  if (reset) {
    currentPage.value = 1
    posts.value = []
    hasMore.value = true
  }
  append ? (loadingMore.value = true) : (loading.value = true)
  try {
    const res = await getConsolePostList({
      page: currentPage.value,
      size: pageSize.value,
      board: filters.board || undefined,
      status: filters.status,
      keyword: filters.keyword.trim() || undefined
    })
    const records = res.records || []
    total.value = res.total || 0
    posts.value = append ? [...posts.value, ...records] : records
    if (total.value) {
      hasMore.value = posts.value.length < total.value
    } else {
      hasMore.value = records.length >= pageSize.value
    }
  } catch (error) {
    console.error('Failed to load posts', error)
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
  await loadPosts({ append: true })
}

const getStatusText = (status: number) => {
  if (status === 0) return '正常'
  if (status === 1) return '已解决'
  if (status === 2) return '已删除'
  if (status === 3) return '待审核'
  if (status === 4) return '已下架'
  if (status === 5) return '已售出'
  return '未知'
}

const getStatusClass = (status: number) => {
  if (status === 0) return 'badge badge-success badge-sm'
  if (status === 1) return 'badge badge-warning badge-sm'
  if (status === 2) return 'badge badge-error badge-sm'
  if (status === 3) return 'badge badge-info badge-sm'
  if (status === 4) return 'badge badge-neutral badge-sm'
  if (status === 5) return 'badge badge-accent badge-sm'
  return 'badge badge-ghost badge-sm'
}

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  const date = new Date(value)
  return `${date.toLocaleDateString()} ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
}

const goToDetail = (id: number) => {
  selectedPostId.value = id
  showPostDetail.value = true
}

const goToPublish = () => {
  showPublish.value = true
}

const goToComments = (postId: number) => {
  router.push({
    path: '/console/content/comment',
    query: { postId: String(postId) }
  })
}

const handlePublishSuccess = () => {
  loadPosts({ reset: true })
}

const promptReasonOptional = async () => {
  const reason = await dialog.prompt('请输入操作原因（可选）', { required: false, multiline: true })
  if (reason === null) return null
  const trimmed = reason.trim()
  return trimmed ? trimmed : undefined
}

const handleResolve = async (post: PostVO) => {
  if (!await dialog.confirm(`确认将帖子「${post.title}」标记为已解决？`)) return
  const reason = await promptReasonOptional()
  if (reason === null) return
  try {
    await resolveConsolePost(post.id, reason)
    await loadPosts({ reset: true })
  } catch (error: unknown) {
    await dialog.alert((error as ApiErrorLike)?.message || '操作失败')
  }
}

const handleSold = async (post: PostVO) => {
  if (!await dialog.confirm(`确认将帖子「${post.title}」标记为已售出？`)) return
  const reason = await promptReasonOptional()
  if (reason === null) return
  try {
    await soldConsolePost(post.id, reason)
    await loadPosts({ reset: true })
  } catch (error: unknown) {
    await dialog.alert((error as ApiErrorLike)?.message || '操作失败')
  }
}

const handleReject = async (post: PostVO) => {
  if (!await dialog.confirm(`确认驳回帖子「${post.title}」？`)) return
  const reason = await promptReasonOptional()
  if (reason === null) return
  try {
    await rejectConsolePost(post.id, reason)
    await loadPosts({ reset: true })
  } catch (error: unknown) {
    await dialog.alert((error as ApiErrorLike)?.message || '操作失败')
  }
}

const handlePin = async (post: PostVO) => {
  const reason = await promptReasonOptional()
  if (reason === null) return
  try {
    await pinConsolePost(post.id, reason)
    await loadPosts({ reset: true })
  } catch (error: unknown) {
    await dialog.alert((error as ApiErrorLike)?.message || '操作失败')
  }
}

const handleUnpin = async (post: PostVO) => {
  const reason = await promptReasonOptional()
  if (reason === null) return
  try {
    await unpinConsolePost(post.id, reason)
    await loadPosts({ reset: true })
  } catch (error: unknown) {
    await dialog.alert((error as ApiErrorLike)?.message || '操作失败')
  }
}

const handleOffline = async (post: PostVO) => {
  if (!await dialog.confirm(`确认下架帖子「${post.title}」？`)) return
  const reason = await promptReasonOptional()
  if (reason === null) return
  try {
    await offlineConsolePost(post.id, reason)
    await loadPosts({ reset: true })
  } catch (error: unknown) {
    await dialog.alert((error as ApiErrorLike)?.message || '操作失败')
  }
}

const handleOnline = async (post: PostVO) => {
  if (!await dialog.confirm(`确认上架帖子「${post.title}」？`)) return
  const reason = await promptReasonOptional()
  if (reason === null) return
  try {
    await onlineConsolePost(post.id, reason)
    await loadPosts({ reset: true })
  } catch (error: unknown) {
    await dialog.alert((error as ApiErrorLike)?.message || '操作失败')
  }
}

const handleLock = async (post: PostVO) => {
  const reason = await promptReasonOptional()
  if (reason === null) return
  try {
    await lockConsolePost(post.id, reason)
    await loadPosts({ reset: true })
  } catch (error: unknown) {
    await dialog.alert((error as ApiErrorLike)?.message || '操作失败')
  }
}

const handleUnlock = async (post: PostVO) => {
  const reason = await promptReasonOptional()
  if (reason === null) return
  try {
    await unlockConsolePost(post.id, reason)
    await loadPosts({ reset: true })
  } catch (error: unknown) {
    await dialog.alert((error as ApiErrorLike)?.message || '操作失败')
  }
}

const handleDelete = async (post: PostVO) => {
  if (!await dialog.confirm(`确认删除帖子「${post.title}」？`)) return
  const reason = await promptReasonOptional()
  if (reason === null) return
  try {
    await deleteConsolePost(post.id, reason)
    await loadPosts({ reset: true })
  } catch (error: unknown) {
    await dialog.alert((error as ApiErrorLike)?.message || '删除失败')
  }
}

onMounted(() => {
  loadPosts({ reset: true })
  nextTick(() => setupObserver())
})

onUnmounted(() => {
  observer?.disconnect()
  observer = null
})
</script>
