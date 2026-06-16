<template>
  <div class="max-w-4xl mx-auto">
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-slate-800">公告列表</h1>
      <p class="text-slate-500 mt-1">查看校园最新公告通知</p>
    </div>

    <div v-if="loading" class="flex justify-center py-12">
      <span class="loading loading-spinner loading-lg"></span>
    </div>

    <div v-else-if="notices.length === 0" class="card bg-base-100 shadow-sm border border-base-200">
      <div class="card-body items-center text-center py-12">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 text-slate-300 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M11 5.882V19.24a1.76 1.76 0 01-3.417.592l-2.147-6.15M18 13a3 3 0 100-6M5.436 13.683A4.001 4.001 0 017 6h1.832c4.1 0 7.625-1.234 9.168-3v14c-1.543-1.766-5.067-3-9.168-3H7a3.988 3.988 0 01-1.564-.317z" />
        </svg>
        <h3 class="text-lg font-medium text-slate-700">暂无公告</h3>
        <p class="text-slate-500 text-sm">稍后再来看看吧</p>
      </div>
    </div>

    <div v-else class="space-y-4">
      <router-link
        v-for="notice in notices"
        :key="notice.id"
        :to="`/notices/${notice.id}`"
        class="card bg-base-100 shadow-sm border border-base-200 hover:shadow-md transition-shadow block"
      >
        <div class="card-body p-5">
          <div class="flex items-start gap-3">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-2">
                <span v-if="notice.isPinned" class="badge badge-error badge-sm">置顶</span>
                <h3 class="font-bold text-slate-800 line-clamp-1">{{ notice.title }}</h3>
              </div>
              <p class="text-slate-500 text-sm line-clamp-2">{{ getPreviewContent(notice.content) }}</p>
              <div class="flex items-center gap-4 mt-3 text-xs text-slate-400">
                <span>{{ notice.createdByName || '管理员' }}</span>
                <span>{{ formatDateTime(notice.publishedAt) }}</span>
              </div>
            </div>
          </div>
        </div>
      </router-link>

      <div v-if="userStore.token" class="flex justify-center mt-6">
        <button class="btn btn-sm" :disabled="!hasMore || loadingMore" @click="loadMore">
          <span v-if="loadingMore">加载中...</span>
          <span v-else-if="hasMore">加载更多</span>
          <span v-else>没有更多了</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useUserStore } from '@/stores/user'
import { getPublicNotices, getVisibleNotices, type NoticeVO, type VisibleNoticeQuery } from '@/api/system'

const userStore = useUserStore()
const notices = ref<NoticeVO[]>([])
const loading = ref(true)
const loadingMore = ref(false)
const pageSize = ref(10)
const hasMore = ref(true)

const cursor = reactive({
  lastPinned: undefined as number | undefined,
  lastPublishedAt: undefined as string | undefined,
  lastId: undefined as number | undefined
})

onMounted(() => {
  loadNotices()
})

const resetCursor = () => {
  cursor.lastPinned = undefined
  cursor.lastPublishedAt = undefined
  cursor.lastId = undefined
}

const loadNotices = async ({ append = false } = {}) => {
  if (append && (loadingMore.value || loading.value)) return
  if (!append) {
    resetCursor()
    hasMore.value = true
    loading.value = true
  } else {
    loadingMore.value = true
  }
  try {
    if (userStore.token) {
      const params: VisibleNoticeQuery = { size: pageSize.value }
      if (cursor.lastId !== undefined && cursor.lastPinned !== undefined) {
        params.lastId = cursor.lastId
        params.lastPinned = cursor.lastPinned
        if (cursor.lastPublishedAt) {
          params.lastPublishedAt = cursor.lastPublishedAt
        }
      }
      const res = await getVisibleNotices(params)
      const records = res.records || []
      notices.value = append ? [...notices.value, ...records] : records
      const lastRecord = records[records.length - 1]
      if (lastRecord) {
        cursor.lastPinned = lastRecord.isPinned ? 1 : 0
        cursor.lastPublishedAt = lastRecord.publishedAt || undefined
        cursor.lastId = lastRecord.id
      }
      hasMore.value = records.length >= pageSize.value
    } else {
      const res = await getPublicNotices(50)
      notices.value = res || []
      hasMore.value = false
    }
  } catch (e) {
    console.error('Failed to load notices', e)
  } finally {
    if (!append) {
      loading.value = false
    } else {
      loadingMore.value = false
    }
  }
}

const loadMore = () => {
  if (!hasMore.value || loadingMore.value || loading.value) return
  void loadNotices({ append: true })
}

const getPreviewContent = (content: string) => {
  const text = content.replace(/<[^>]+>/g, '')
  return text.length > 100 ? text.slice(0, 100) + '...' : text
}

const formatDateTime = (dateStr?: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}
</script>
