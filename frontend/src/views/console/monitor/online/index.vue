<template>
  <div class="h-full flex flex-col overflow-hidden">
    <div class="card bg-base-100 shadow-xl flex-1 min-h-0">
      <div class="card-body flex flex-col min-h-0">
        <div class="flex justify-between items-center mb-4">
        <h2 class="card-title">在线用户</h2>
        <button class="btn btn-sm btn-ghost" @click="fetchData({ reset: true })">刷新</button>
        </div>

        <div class="flex flex-wrap gap-3 mb-4">
          <input v-model="queryParams.keyword" class="input input-bordered input-sm" placeholder="用户名/昵称/用户ID" />
          <input v-model="queryParams.ipaddr" class="input input-bordered input-sm" placeholder="登录IP" />
          <button class="btn btn-sm btn-primary" @click="handleSearch">搜索</button>
          <button class="btn btn-sm btn-ghost" @click="handleReset">重置</button>
        </div>

        <div ref="scrollContainer" class="flex-1 overflow-auto">
          <div class="overflow-x-auto">
            <table class="table table-zebra">
              <thead>
                <tr>
                  <th>用户ID</th>
                  <th>用户名</th>
                  <th>昵称</th>
                  <th>登录IP</th>
                  <th>用户代理</th>
                  <th>登录时间</th>
                  <th>最近活跃</th>
                  <th>会话剩余</th>
                  <th>Token</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="loading">
                  <td colspan="10" class="text-center py-4">加载中...</td>
                </tr>
                <tr v-else-if="userList.length === 0">
                  <td colspan="10" class="text-center py-4">暂无数据</td>
                </tr>
                <tr v-else v-for="user in userList" :key="user.token">
                  <td>{{ user.userId }}</td>
                  <td>{{ user.username || '-' }}</td>
                  <td>{{ user.nickname || '-' }}</td>
                  <td><span class="badge badge-ghost">{{ user.ipaddr || '-' }}</span></td>
                  <td class="max-w-[260px] truncate" :title="user.userAgent || '-'">{{ user.userAgent || '-' }}</td>
                  <td class="text-sm text-slate-500">{{ formatDate(user.loginTime) }}</td>
                  <td class="text-sm text-slate-500">{{ formatDate(user.lastActiveTime) }}</td>
                  <td>{{ formatTimeout(user.tokenTimeout) }}</td>
                  <td class="text-xs text-slate-500" :title="user.token">{{ formatToken(user.token) }}</td>
                  <td>
                    <button
                      class="btn btn-ghost btn-xs text-error"
                      @click="handleKickout(user)"
                      v-permission="['system:online:kickout']"
                    >
                      强制下线
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div ref="loadMoreTrigger" class="h-6" aria-hidden="true"></div>
        </div>

        <div class="flex justify-between pt-4 text-sm text-base-content/60">
          <div>已加载 {{ userList.length }} / {{ total || '-' }} 条</div>
          <div v-if="loadingMore">正在加载更多...</div>
          <div v-else-if="!hasMore && userList.length > 0">没有更多了</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, nextTick, reactive, ref } from 'vue'
import { getOnlineUserList, kickoutOnlineUser, type OnlineUserQuery, type OnlineUserVO } from '@/api/system'
import { useDialog } from '@/composables/useDialog'

const loading = ref(false)
const loadingMore = ref(false)
const userList = ref<OnlineUserVO[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const hasMore = ref(true)
const scrollContainer = ref<HTMLElement | null>(null)
const loadMoreTrigger = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null
const queryParams = reactive({
  keyword: '',
  ipaddr: ''
})
const dialog = useDialog()

onMounted(() => {
  fetchData({ reset: true })
  nextTick(() => setupObserver())
})

onUnmounted(() => {
  observer?.disconnect()
  observer = null
})

const fetchData = async ({ append = false, reset = false } = {}) => {
  if (append && (loadingMore.value || loading.value)) return
  if (!append && loading.value) return
  if (reset) {
    page.value = 1
    userList.value = []
    hasMore.value = true
  }
  append ? (loadingMore.value = true) : (loading.value = true)
  try {
    const params: OnlineUserQuery = {
      page: page.value,
      size: pageSize.value
    }
    if (queryParams.keyword) params.keyword = queryParams.keyword
    if (queryParams.ipaddr) params.ipaddr = queryParams.ipaddr

    const res = await getOnlineUserList(params)
    const records = res?.records || []
    total.value = res?.total || 0
    userList.value = append ? [...userList.value, ...records] : records
    if (total.value) {
      hasMore.value = userList.value.length < total.value
    } else {
      hasMore.value = records.length >= pageSize.value
    }
  } catch (error: unknown) {
    if (!append) {
      userList.value = []
      total.value = 0
    }
    await dialog.alert((error as ApiErrorLike)?.message || (error as ApiErrorLike)?.response?.data?.message || '获取在线用户失败')
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
  page.value += 1
  await fetchData({ append: true })
}

const handleSearch = () => {
  fetchData({ reset: true })
}

const handleReset = () => {
  queryParams.keyword = ''
  queryParams.ipaddr = ''
  fetchData({ reset: true })
}

const handleKickout = async (user: OnlineUserVO) => {
  if (!await dialog.confirm(`确定要强制下线用户 ${user.username || user.userId} 吗？`)) return
  try {
    await kickoutOnlineUser(user.token)
    fetchData({ reset: true })
  } catch (error: unknown) {
    await dialog.alert((error as ApiErrorLike)?.message || (error as ApiErrorLike)?.response?.data?.message || '强制下线失败')
  }
}

const formatDate = (value?: string | number) => {
  if (value === undefined || value === null || value === '') return '-'
  const date = typeof value === 'number' ? new Date(value) : new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString()
}

const formatTimeout = (seconds?: number) => {
  if (seconds === undefined || seconds === null) return '-'
  if (seconds < 0) return '永不过期'
  if (seconds < 60) return `${seconds}s`
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m`
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}h`
  return `${Math.floor(seconds / 86400)}d`
}

const formatToken = (token: string) => {
  if (!token) return '-'
  if (token.length <= 16) return token
  return `${token.slice(0, 6)}...${token.slice(-6)}`
}
</script>
