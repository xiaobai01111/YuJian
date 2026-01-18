<template>
  <div class="space-y-6">
    <!-- Stats Cards -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      <div v-if="canUserStats" class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title text-sm text-slate-500">总用户数</h2>
          <div class="flex items-end justify-between">
            <span class="text-3xl font-bold">{{ loading ? '-' : stats.totalUsers.toLocaleString() }}</span>
            <span v-if="stats.userGrowth !== 0" class="text-xs flex items-center gap-1" :class="stats.userGrowth >= 0 ? 'text-success' : 'text-error'">
              <svg v-if="stats.userGrowth >= 0" xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" /></svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 17h8m0 0v-8m0 8l-8-8-4 4-6-6" /></svg>
              {{ stats.userGrowth >= 0 ? '+' : '' }}{{ stats.userGrowth }}%
            </span>
            <span v-else class="text-xs text-slate-400">今日+{{ stats.todayNewUsers }}</span>
          </div>
        </div>
      </div>
      
      <div v-if="canPostStats" class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title text-sm text-slate-500">今日发帖</h2>
          <div class="flex items-end justify-between">
            <span class="text-3xl font-bold">{{ loading ? '-' : stats.todayPosts }}</span>
            <span v-if="stats.postGrowth !== 0" class="text-xs flex items-center gap-1" :class="stats.postGrowth >= 0 ? 'text-success' : 'text-error'">
              <svg v-if="stats.postGrowth >= 0" xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" /></svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 17h8m0 0v-8m0 8l-8-8-4 4-6-6" /></svg>
              {{ stats.postGrowth >= 0 ? '+' : '' }}{{ stats.postGrowth }}%
            </span>
            <span v-else class="text-xs text-slate-400">昨日 {{ stats.yesterdayPosts }}</span>
          </div>
        </div>
      </div>

      <div v-if="canVerify" class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title text-sm text-slate-500">待审核</h2>
          <div class="flex items-end justify-between">
            <span class="text-3xl font-bold">{{ loading ? '-' : (stats.pendingVerifications ?? 0) }}</span>
            <span v-if="(stats.pendingVerifications ?? 0) > 0" class="text-xs text-warning">需处理</span>
            <span v-else class="text-xs text-success">已清空</span>
          </div>
        </div>
      </div>

      <div v-if="canPostStats" class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title text-sm text-slate-500">总帖子数</h2>
          <div class="flex items-end justify-between">
            <span class="text-3xl font-bold">{{ loading ? '-' : stats.totalPosts.toLocaleString() }}</span>
            <span class="text-xs text-slate-400">正常状态</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Recent Activity & Quick Actions -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Recent Activity -->
      <div class="card bg-base-100 shadow-xl lg:col-span-2">
        <div class="card-body">
          <h2 class="card-title mb-4">最近活动</h2>
          <div class="overflow-x-auto">
            <table class="table table-zebra">
              <thead>
                <tr>
                  <th>用户</th>
                  <th>操作</th>
                  <th>时间</th>
                  <th>状态</th>
                </tr>
              </thead>
            <tbody>
              <tr v-if="activitiesLoading">
                <td colspan="4" class="text-center py-8">
                  <span class="loading loading-spinner loading-md"></span>
                </td>
              </tr>
              <tr v-else-if="!canPostStats">
                <td colspan="4" class="text-center py-8 text-slate-400">无权限查看</td>
              </tr>
              <tr v-else-if="activities.length === 0">
                <td colspan="4" class="text-center py-8 text-slate-400">暂无活动记录</td>
              </tr>
              <tr v-for="activity in activities" :key="activity.postId">
                  <td class="flex items-center gap-2">
                    <div class="avatar placeholder">
                      <div v-if="activity.avatar" class="w-8 h-8 rounded-full">
                        <img :src="activity.avatar" :alt="activity.nickname" />
                      </div>
                      <div v-else class="bg-neutral-focus text-neutral-content rounded-full w-8 h-8">
                        <span class="text-xs">{{ activity.nickname?.charAt(0) || 'U' }}</span>
                      </div>
                    </div>
                    <span>{{ activity.nickname }}</span>
                  </td>
                  <td>{{ activity.action }}</td>
                  <td class="text-slate-500 text-sm">{{ formatRelativeTime(activity.time) }}</td>
                  <td><span class="badge badge-sm" :class="getStatusBadge(activity.status).class">{{ getStatusBadge(activity.status).text }}</span></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Quick Actions -->
      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title mb-4">快捷操作</h2>
          <div class="grid grid-cols-2 gap-4">
            <button class="btn btn-outline btn-primary h-24 flex flex-col gap-2" v-permission="['system:notice:add']">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" /></svg>
              发布公告
            </button>
            <button class="btn btn-outline h-24 flex flex-col gap-2" v-permission="['content:verification:list']">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
              审核日志
            </button>
            <button class="btn btn-outline h-24 flex flex-col gap-2" v-permission="['system:config:edit']">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" /><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
              系统设置
            </button>
            <button class="btn btn-outline text-error h-24 flex flex-col gap-2" v-permission="['system:cache:clear']">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
              清理缓存
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Ops Overview -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div v-if="canNotice" class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <div class="flex items-center justify-between mb-4">
            <h2 class="card-title">公告概览</h2>
            <span class="text-xs text-slate-400">近7天到期：{{ stats.noticeExpiringSoon ?? 0 }}</span>
          </div>
          <div class="grid grid-cols-2 gap-3 text-sm">
            <div class="flex justify-between">
              <span class="text-slate-500">总数</span>
              <span class="font-medium">{{ stats.noticeTotal ?? 0 }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-500">已发布</span>
              <span class="font-medium">{{ stats.noticePublished ?? 0 }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-500">草稿</span>
              <span class="font-medium">{{ stats.noticeDraft ?? 0 }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-500">已下线</span>
              <span class="font-medium">{{ stats.noticeOffline ?? 0 }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-500">置顶</span>
              <span class="font-medium">{{ stats.noticePinned ?? 0 }}</span>
            </div>
          </div>

          <div class="mt-6">
            <div class="text-sm font-medium mb-2">最新公告</div>
            <div v-if="recentNoticesLoading" class="py-4 text-center text-slate-400">加载中...</div>
            <div v-else-if="recentNotices.length === 0" class="py-4 text-center text-slate-400">暂无公告</div>
            <ul v-else class="space-y-2">
              <li v-for="notice in recentNotices" :key="notice.id" class="flex items-center justify-between text-sm">
                <div class="flex items-center gap-2">
                  <span v-if="notice.isPinned" class="badge badge-xs badge-warning">置顶</span>
                  <span class="truncate max-w-[240px]">{{ notice.title }}</span>
                </div>
                <span class="text-slate-400">{{ formatRelativeTime(notice.publishedAt) }}</span>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <div v-if="canVerify || canReport || canSensitive" class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title mb-4">运维待处理</h2>
          <div class="grid grid-cols-2 gap-3 text-sm">
            <div v-if="canVerify" class="flex justify-between">
              <span class="text-slate-500">身份待审核</span>
              <span class="font-medium">{{ stats.pendingVerifications ?? 0 }}</span>
            </div>
            <div v-if="canReport" class="flex justify-between">
              <span class="text-slate-500">举报待处理</span>
              <span class="font-medium">{{ stats.pendingReports ?? 0 }}</span>
            </div>
            <div v-if="canSensitive" class="flex justify-between">
              <span class="text-slate-500">敏感词总数</span>
              <span class="font-medium">{{ stats.sensitiveWords ?? 0 }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { getDashboardStats, getRecentActivities, getRecentNotices, type DashboardStats, type RecentActivity, type RecentNotice } from '@/api/system'
import { formatRelativeTime } from '@/utils/time'
import { usePermissionStore } from '@/stores/permission'

const loading = ref(true)
const activitiesLoading = ref(true)
const activities = ref<RecentActivity[]>([])
const recentNoticesLoading = ref(false)
const recentNotices = ref<RecentNotice[]>([])
const stats = reactive<DashboardStats>({
  totalUsers: 0,
  todayNewUsers: 0,
  totalPosts: 0,
  todayPosts: 0,
  yesterdayPosts: 0,
  postGrowth: 0,
  userGrowth: 0
})

const permissionStore = usePermissionStore()
const canUserStats = computed(() => permissionStore.hasPermission('system:user:list'))
const canPostStats = computed(() => permissionStore.hasPermission('content:post:list'))
const canNotice = computed(() => permissionStore.hasPermission('system:notice:list'))
const canVerify = computed(() => permissionStore.hasPermission('content:verification:list'))
const canReport = computed(() => permissionStore.hasPermission('content:report:list'))
const canSensitive = computed(() => permissionStore.hasPermission('system:sensitive-word:list'))

const fetchStats = async () => {
  loading.value = true
  try {
    const res: any = await getDashboardStats()
    Object.assign(stats, res)
  } catch (error) {
    console.error('Failed to fetch dashboard stats', error)
  } finally {
    loading.value = false
  }
}

const fetchActivities = async () => {
  activitiesLoading.value = true
  try {
    if (!canPostStats.value) {
      activities.value = []
      return
    }
    const res: any = await getRecentActivities()
    activities.value = res || []
  } catch (error) {
    console.error('Failed to fetch recent activities', error)
  } finally {
    activitiesLoading.value = false
  }
}

const fetchRecentNotices = async () => {
  if (!canNotice.value) {
    recentNotices.value = []
    return
  }
  recentNoticesLoading.value = true
  try {
    const res: any = await getRecentNotices()
    recentNotices.value = res || []
  } catch (error) {
    console.error('Failed to fetch recent notices', error)
  } finally {
    recentNoticesLoading.value = false
  }
}

const getStatusBadge = (status: number) => {
  switch (status) {
    case 0: return { class: 'badge-success', text: '正常' }
    case 1: return { class: 'badge-error', text: '已删除' }
    case 2: return { class: 'badge-warning', text: '待审核' }
    default: return { class: 'badge-ghost', text: '未知' }
  }
}

onMounted(() => {
  fetchStats()
  fetchActivities()
  fetchRecentNotices()
})
</script>
