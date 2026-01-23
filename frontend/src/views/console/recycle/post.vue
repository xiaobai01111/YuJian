<template>
  <div class="h-full flex flex-col overflow-hidden">
    <div class="flex-1 min-h-0 flex flex-col gap-6 overflow-hidden p-6">
      <div class="flex flex-wrap justify-between items-center gap-3">
        <div>
          <h1 class="text-2xl font-bold">帖子回收站</h1>
          <p class="text-gray-500 mt-1">查看已删除帖子并执行恢复或彻底删除</p>
        </div>
        <div class="flex items-center gap-2">
          <button class="btn btn-ghost btn-sm" @click="loadPosts">刷新</button>
        </div>
      </div>

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
              <input
                v-model="filters.keyword"
                type="text"
                placeholder="搜索关键词..."
                class="input input-bordered input-sm w-52"
                @keyup.enter="loadPosts"
              />
            </div>
            <button class="btn btn-sm btn-ghost" @click="loadPosts">搜索</button>
          </div>
        </div>
      </div>

      <div class="card bg-base-100 shadow-sm flex-1 min-h-0">
        <div class="card-body p-0 flex flex-col min-h-0">
          <div class="flex-1 overflow-auto">
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

          <div v-if="total > pageSize" class="flex justify-center p-4 border-t">
            <div class="join">
              <button
                class="join-item btn btn-sm"
                :disabled="currentPage === 1"
                @click="changePage(currentPage - 1)"
              >«</button>
              <button class="join-item btn btn-sm">第 {{ currentPage }} / {{ totalPages }} 页</button>
              <button
                class="join-item btn btn-sm"
                :disabled="currentPage >= totalPages"
                @click="changePage(currentPage + 1)"
              >»</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { getRecyclePosts, purgeRecyclePost, restoreRecyclePost, type PostVO } from '@/api/recycle'
import { BOARD_OPTIONS, getBoardLabel, getPostBoards } from '@/utils/boards'
import { useUserStore } from '@/stores/user'
import { useDialog } from '@/composables/useDialog'

const userStore = useUserStore()
const dialog = useDialog()

const posts = ref<PostVO[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const filters = reactive({
  board: '',
  keyword: ''
})

const boardOptions = BOARD_OPTIONS

const canRestore = computed(() => userStore.hasPermission('content:recycle:post:restore'))
const canPurge = computed(() => userStore.hasPermission('content:recycle:post:purge'))
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const loadPosts = async () => {
  loading.value = true
  try {
    const res: any = await getRecyclePosts({
      page: currentPage.value,
      size: pageSize.value,
      board: filters.board || undefined,
      keyword: filters.keyword.trim() || undefined
    })
    posts.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error('Failed to load recycle posts', error)
  } finally {
    loading.value = false
  }
}

const changePage = (page: number) => {
  currentPage.value = page
  loadPosts()
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
    await loadPosts()
  } catch (error: any) {
    await dialog.alert(error?.message || '恢复失败')
  }
}

const handlePurge = async (post: PostVO) => {
  if (!await dialog.confirm(`确认彻底删除帖子「${post.title}」？此操作不可恢复。`)) return
  const reason = await promptReason(needReason(post))
  if (reason === null) return
  try {
    await purgeRecyclePost(post.id, reason || undefined)
    await loadPosts()
  } catch (error: any) {
    await dialog.alert(error?.message || '删除失败')
  }
}

onMounted(() => {
  loadPosts()
})
</script>
