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
              <select v-model="filters.board" class="select select-bordered select-sm w-40" @change="loadPosts">
                <option value="">全部板块</option>
                <option v-for="option in boardOptions" :key="option.key" :value="option.key">
                  {{ option.label }}
                </option>
              </select>
            </div>
            <div class="form-control">
              <select v-model="filters.status" class="select select-bordered select-sm w-32" @change="loadPosts">
                <option :value="undefined">全部状态</option>
                <option :value="0">正常</option>
                <option :value="1">已解决</option>
                <option :value="2">已删除</option>
              </select>
            </div>
            <div class="form-control">
              <input
                v-model="filters.keyword"
                type="text"
                placeholder="搜索关键词..."
                class="input input-bordered input-sm w-52"
                @keyup.enter="loadPosts"
              />
            </div>
            <button class="btn btn-sm btn-ghost" @click="loadPosts">
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
          <div class="flex-1 overflow-auto">
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
                    <div class="flex gap-1">
                      <button class="btn btn-ghost btn-xs" @click="goToDetail(post.id)">查看</button>
                      <button
                        v-if="canResolve && post.status === 0"
                        class="btn btn-success btn-xs"
                        @click="handleResolve(post)"
                      >已解决</button>
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

          <!-- Pagination -->
          <div v-if="total > pageSize" class="flex justify-center p-4 border-t">
            <div class="join">
              <button
                class="join-item btn btn-sm"
                :disabled="currentPage === 1"
                @click="changePage(currentPage - 1)"
              >«</button>
              <button class="join-item btn btn-sm">第 {{ currentPage }} / {{ Math.ceil(total / pageSize) }} 页</button>
              <button
                class="join-item btn btn-sm"
                :disabled="currentPage * pageSize >= total"
                @click="changePage(currentPage + 1)"
              >»</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Publish Modal -->
    <PostPublishModal v-model="showPublish" :use-console-api="true" @success="handlePublishSuccess" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getConsolePostList, resolveConsolePost, deleteConsolePost, type PostVO } from '@/api/post'
import { BOARD_OPTIONS, getBoardLabel, getPostBoards } from '@/utils/boards'
import { useUserStore } from '@/stores/user'
import PostPublishModal from '@/components/post/PostPublishModal.vue'

const router = useRouter()
const userStore = useUserStore()

const posts = ref<PostVO[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const showPublish = ref(false)

const filters = reactive({
  board: '',
  status: undefined as number | undefined,
  keyword: ''
})

const boardOptions = BOARD_OPTIONS

const canAdd = computed(() => userStore.hasPermission('content:post:add'))
const canDelete = computed(() => userStore.hasPermission('content:post:delete'))
const canResolve = computed(() => userStore.hasPermission('content:post:resolve'))

const loadPosts = async () => {
  loading.value = true
  try {
    const res: any = await getConsolePostList({
      page: currentPage.value,
      size: pageSize.value,
      board: filters.board || undefined,
      status: filters.status,
      keyword: filters.keyword.trim() || undefined
    })
    posts.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error('Failed to load posts', error)
  } finally {
    loading.value = false
  }
}

const changePage = (page: number) => {
  currentPage.value = page
  loadPosts()
}

const getStatusText = (status: number) => {
  if (status === 0) return '正常'
  if (status === 1) return '已解决'
  if (status === 2) return '已删除'
  return '未知'
}

const getStatusClass = (status: number) => {
  if (status === 0) return 'badge badge-success badge-sm'
  if (status === 1) return 'badge badge-warning badge-sm'
  if (status === 2) return 'badge badge-error badge-sm'
  return 'badge badge-ghost badge-sm'
}

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  const date = new Date(value)
  return `${date.toLocaleDateString()} ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
}

const goToDetail = (id: number) => {
  router.push(`/posts/${id}`)
}

const goToPublish = () => {
  showPublish.value = true
}

const handlePublishSuccess = () => {
  loadPosts()
}

const handleResolve = async (post: PostVO) => {
  if (!confirm(`确认将帖子「${post.title}」标记为已解决？`)) return
  try {
    await resolveConsolePost(post.id)
    await loadPosts()
  } catch (error: any) {
    alert(error?.message || '操作失败')
  }
}

const handleDelete = async (post: PostVO) => {
  if (!confirm(`确认删除帖子「${post.title}」？`)) return
  try {
    await deleteConsolePost(post.id)
    await loadPosts()
  } catch (error: any) {
    alert(error?.message || '删除失败')
  }
}

onMounted(() => {
  loadPosts()
})
</script>
