<template>
  <div class="h-full flex flex-col overflow-hidden">
    <div class="flex-1 min-h-0 flex flex-col gap-6 overflow-hidden p-6">
      <div class="flex flex-wrap justify-between items-center gap-3">
        <div>
          <h1 class="text-2xl font-bold">评论回收站</h1>
          <p class="text-gray-500 mt-1">查看已删除评论并执行恢复或彻底删除</p>
        </div>
        <div class="flex items-center gap-2">
          <button class="btn btn-ghost btn-sm" @click="loadComments">刷新</button>
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
                @keyup.enter="loadComments"
              />
            </div>
            <div class="form-control">
              <input
                v-model="filters.keyword"
                type="text"
                placeholder="搜索内容..."
                class="input input-bordered input-sm w-52"
                @keyup.enter="loadComments"
              />
            </div>
            <button class="btn btn-sm btn-ghost" @click="loadComments">搜索</button>
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
                    <th class="w-24">帖子ID</th>
                    <th>内容</th>
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
                  <tr v-else-if="comments.length === 0">
                    <td colspan="6" class="text-center py-8 text-gray-500">暂无数据</td>
                  </tr>
                  <tr v-for="comment in comments" :key="comment.id" class="hover">
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
                          v-if="canRestore"
                          class="btn btn-success btn-xs"
                          @click="handleRestore(comment)"
                        >恢复</button>
                        <button
                          v-if="canPurge"
                          class="btn btn-error btn-xs"
                          @click="handlePurge(comment)"
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
import { getRecycleComments, purgeRecycleComment, restoreRecycleComment, type CommentConsoleVO } from '@/api/recycle'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const comments = ref<CommentConsoleVO[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const filters = reactive({
  postId: '',
  keyword: ''
})

const canRestore = computed(() => userStore.hasPermission('content:recycle:comment:restore'))
const canPurge = computed(() => userStore.hasPermission('content:recycle:comment:purge'))
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const loadComments = async () => {
  loading.value = true
  try {
    const res: any = await getRecycleComments({
      page: currentPage.value,
      size: pageSize.value,
      postId: filters.postId.trim() ? Number(filters.postId) : undefined,
      keyword: filters.keyword.trim() || undefined
    })
    comments.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error('Failed to load recycle comments', error)
  } finally {
    loading.value = false
  }
}

const changePage = (page: number) => {
  currentPage.value = page
  loadComments()
}

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  const date = new Date(value)
  return `${date.toLocaleDateString()} ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
}

const needReason = (comment: CommentConsoleVO) => {
  const currentUserId = userStore.userInfo?.id
  return !currentUserId || comment.author?.id !== currentUserId
}

const promptReason = (required: boolean) => {
  const tip = required ? '请输入操作原因（必填）' : '请输入操作原因（可选）'
  const reason = prompt(tip)
  if (required && (!reason || !reason.trim())) {
    alert('请输入操作原因')
    return null
  }
  return reason ? reason.trim() : ''
}

const handleRestore = async (comment: CommentConsoleVO) => {
  if (!confirm(`确认恢复评论 #${comment.id}？`)) return
  const reason = promptReason(needReason(comment))
  if (reason === null) return
  try {
    await restoreRecycleComment(comment.id, reason || undefined)
    await loadComments()
  } catch (error: any) {
    alert(error?.message || '恢复失败')
  }
}

const handlePurge = async (comment: CommentConsoleVO) => {
  if (!confirm(`确认彻底删除评论 #${comment.id}？此操作不可恢复。`)) return
  const reason = promptReason(needReason(comment))
  if (reason === null) return
  try {
    await purgeRecycleComment(comment.id, reason || undefined)
    await loadComments()
  } catch (error: any) {
    alert(error?.message || '删除失败')
  }
}

onMounted(() => {
  loadComments()
})
</script>
