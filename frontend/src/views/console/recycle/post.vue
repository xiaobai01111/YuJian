<template>
  <div class="h-full flex flex-col overflow-hidden">
    <div class="flex-1 min-h-0 flex flex-col gap-6 overflow-hidden p-6">
      <div class="flex flex-wrap justify-between items-center gap-3">
        <div>
          <h1 class="text-2xl font-bold">帖子回收站</h1>
          <p class="text-gray-500 mt-1">查看已删除帖子并执行恢复或彻底删除</p>
        </div>
        <div class="flex items-center gap-2">
          <button class="btn btn-ghost btn-sm" @click="loadPosts({ reset: true })">刷新</button>
        </div>
      </div>

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
              <input
                v-model="filters.keyword"
                type="text"
                placeholder="搜索关键词..."
                class="input input-bordered input-sm w-52"
                @keyup.enter="loadPosts({ reset: true })"
              />
            </div>
            <button class="btn btn-sm btn-ghost" @click="loadPosts({ reset: true })">搜索</button>
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
                    <th class="w-16">ID</th>
                    <th>标题</th>
                    <th class="w-32">板块</th>
                    <th class="w-28">作者</th>
                    <th class="w-40">删除时间</th>
                    <th class="w-44">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="loading">
                    <td colspan="6" class="text-center py-8">
                      <span class="loading loading-spinner loading-md"></span>
                    </td>
                  </tr>
                  <tr v-else-if="posts.length === 0">
                    <td colspan="6" class="text-center py-8 text-gray-500">暂无数据</td>
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
                    <td>{{ post.author?.nickname || post.author?.username || '-' }}</td>
                    <td>{{ formatDateTime(post.updatedAt || post.createdAt) }}</td>
                    <td>
                      <div class="flex gap-1">
                        <button
                          v-if="canRestore"
                          class="btn btn-success btn-xs"
                          @click="handleRestore(post)"
                        >恢复</button>
                        <button
                          v-if="canPurge"
                          class="btn btn-error btn-xs"
                          @click="handlePurge(post)"
                        >彻底删除</button>
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

      <div class="flex items-center justify-between text-sm text-base-content/60">
        <div>已加载 {{ posts.length }} / {{ total || '-' }} 条</div>
        <div v-if="loadingMore">正在加载更多...</div>
        <div v-else-if="!hasMore && posts.length > 0">没有更多了</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, nextTick, reactive, ref } from 'vue'
import { getRecyclePosts, purgeRecyclePost, restoreRecyclePost, type PostVO } from '@/api/recycle'
import { BOARD_OPTIONS, getBoardLabel, getPostBoards } from '@/utils/boards'
import { useUserStore } from '@/stores/user'
import { useDialog } from '@/composables/useDialog'

const userStore = useUserStore()
const dialog = useDialog()

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

const filters = reactive({
  board: '',
  keyword: ''
})

const boardOptions = BOARD_OPTIONS

const canRestore = computed(() => userStore.hasPermission('content:recycle:post:restore'))
const canPurge = computed(() => userStore.hasPermission('content:recycle:post:purge'))
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
    const res = await getRecyclePosts({
      page: currentPage.value,
      size: pageSize.value,
      board: filters.board || undefined,
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
    console.error('Failed to load recycle posts', error)
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

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  const date = new Date(value)
  return `${date.toLocaleDateString()} ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
}

const needReason = (post: PostVO) => {
  const currentUserId = userStore.userInfo?.id
  return !currentUserId || post.author?.id !== currentUserId
}

const promptReason = async (required: boolean) => {
  const tip = required ? '请输入操作原因（必填）' : '请输入操作原因（可选）'
  const reason = await dialog.prompt(tip, { required, multiline: true })
  if (reason == null) return required ? null : ''
  return reason.trim()
}

const handleRestore = async (post: PostVO) => {
  if (!await dialog.confirm(`确认恢复帖子「${post.title}」？`)) return
  const reason = await promptReason(needReason(post))
  if (reason === null) return
  try {
    await restoreRecyclePost(post.id, reason || undefined)
    await loadPosts({ reset: true })
  } catch (error: unknown) {
    await dialog.alert((error as ApiErrorLike)?.message || '恢复失败')
  }
}

const handlePurge = async (post: PostVO) => {
  if (!await dialog.confirm(`确认彻底删除帖子「${post.title}」？此操作不可恢复。`)) return
  const reason = await promptReason(needReason(post))
  if (reason === null) return
  try {
    await purgeRecyclePost(post.id, reason || undefined)
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
