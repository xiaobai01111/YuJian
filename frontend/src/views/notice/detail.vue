<template>
  <div class="max-w-4xl mx-auto">
    <div class="mb-4">
      <router-link to="/notices" class="btn btn-ghost btn-sm gap-2">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
        返回公告列表
      </router-link>
    </div>

    <div v-if="loading" class="flex justify-center py-12">
      <span class="loading loading-spinner loading-lg"></span>
    </div>

    <div v-else-if="error" class="card bg-base-100 shadow-sm border border-base-200">
      <div class="card-body items-center text-center py-12">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 text-error mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
        <h3 class="text-lg font-medium text-slate-700">{{ error }}</h3>
        <router-link to="/notices" class="btn btn-primary btn-sm mt-4">返回列表</router-link>
      </div>
    </div>

    <div v-else-if="notice" class="card bg-base-100 shadow-sm border border-base-200">
      <div class="card-body p-6 md:p-8">
        <div class="mb-6">
          <div class="flex items-center gap-2 mb-2">
            <span v-if="notice.isPinned" class="badge badge-error">置顶</span>
            <h1 class="text-2xl font-bold text-slate-800">{{ notice.title }}</h1>
          </div>
          <div class="flex items-center gap-4 text-sm text-slate-500">
            <span>发布者：{{ notice.createdByName || '管理员' }}</span>
            <span>发布时间：{{ formatDateTime(notice.publishedAt) }}</span>
            <span v-if="notice.endAt" class="text-warning">有效期至：{{ formatDateTime(notice.endAt) }}</span>
          </div>
        </div>
        <div class="divider my-2"></div>
        <div class="prose max-w-none" v-html="notice.content"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getPublicNoticeDetail, getVisibleNoticeDetail, type NoticeVO } from '@/api/system'

const route = useRoute()
const userStore = useUserStore()
const notice = ref<NoticeVO | null>(null)
const loading = ref(true)
const error = ref('')

onMounted(() => {
  loadNotice()
})

const loadNotice = async () => {
  const id = Number(route.params.id)
  if (!id) {
    error.value = '公告ID无效'
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    if (userStore.token) {
      notice.value = await getVisibleNoticeDetail(id)
    } else {
      notice.value = await getPublicNoticeDetail(id)
    }
  } catch (e: any) {
    error.value = e.response?.data?.msg || '公告不存在或无权查看'
  } finally {
    loading.value = false
  }
}

const formatDateTime = (dateStr?: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}
</script>
