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

      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title mb-4">最新公告</h2>
          <div v-if="!canNoticeLatest" class="py-8 text-center text-slate-400">无权限查看</div>
          <div v-else-if="recentNoticesLoading" class="py-8 text-center text-slate-400">加载中...</div>
          <div v-else-if="recentNotices.length === 0" class="py-8 text-center text-slate-400">暂无公告</div>
          <ul v-else class="space-y-2 text-sm">
            <li
              v-for="notice in recentNotices"
              :key="notice.id"
              class="flex items-center justify-between gap-3 px-2 py-2 rounded-lg hover:bg-base-200/40 cursor-pointer transition-colors"
              @click="openNoticeDetail(notice.id)"
            >
              <div class="flex items-center gap-2 min-w-0">
                <span v-if="notice.isPinned" class="badge badge-xs badge-warning">置顶</span>
                <span class="truncate">{{ notice.title }}</span>
              </div>
              <span class="text-slate-400 whitespace-nowrap">{{ formatRelativeTime(notice.publishedAt) }}</span>
            </li>
          </ul>
        </div>
      </div>
    </div>

    <!-- Ops Overview -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div v-if="canNoticeOverview" class="card bg-base-100 shadow-xl">
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

      <div v-if="canLoginLog" class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title mb-4">登录概览</h2>
          <div class="grid grid-cols-2 gap-3 text-sm">
            <div class="flex justify-between">
              <span class="text-slate-500">今日成功</span>
              <span class="font-medium">{{ stats.loginSuccessToday ?? 0 }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-500">今日失败</span>
              <span class="font-medium">{{ stats.loginFailToday ?? 0 }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-500">今日总计</span>
              <span class="font-medium">{{ stats.loginTotalToday ?? 0 }}</span>
            </div>
          </div>
          <div class="mt-4">
            <div class="text-xs text-slate-400 mb-2">近7天趋势</div>
            <div v-if="loginTrendLoading" class="text-xs text-slate-400">加载中...</div>
            <div v-else-if="loginTrend.length === 0" class="text-xs text-slate-400">暂无数据</div>
            <div v-else class="space-y-1 text-xs">
              <div v-for="item in loginTrend" :key="item.date" class="flex items-center justify-between">
                <span class="text-slate-500">{{ item.date }}</span>
                <span class="text-success">✔ {{ item.success }}</span>
                <span class="text-error">✖ {{ item.fail }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Ops Lists -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div v-if="canReport" class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title mb-4">最新举报</h2>
          <div v-if="recentReportsLoading" class="py-4 text-center text-slate-400">加载中...</div>
          <div v-else-if="recentReports.length === 0" class="py-4 text-center text-slate-400">暂无举报</div>
          <ul v-else class="space-y-3 text-sm">
            <li v-for="report in recentReports" :key="report.id" class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <div class="font-medium truncate">#{{ report.id }} {{ report.reporterName || '未知用户' }}</div>
                <div class="text-slate-400 truncate">原因：{{ report.reason }}</div>
              </div>
              <span class="text-slate-400 whitespace-nowrap">{{ formatRelativeTime(report.createdAt) }}</span>
            </li>
          </ul>
        </div>
      </div>

      <div v-if="canVerify" class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title mb-4">最新审核</h2>
          <div v-if="recentVerificationsLoading" class="py-4 text-center text-slate-400">加载中...</div>
          <div v-else-if="recentVerifications.length === 0" class="py-4 text-center text-slate-400">暂无审核</div>
          <ul v-else class="space-y-3 text-sm">
            <li v-for="item in recentVerifications" :key="item.id" class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <div class="font-medium truncate">#{{ item.id }} {{ item.nickname || '未知用户' }}</div>
                <div class="text-slate-400 truncate">待审核</div>
              </div>
              <span class="text-slate-400 whitespace-nowrap">{{ formatRelativeTime(item.createdAt) }}</span>
            </li>
          </ul>
        </div>
      </div>

      <div v-if="canOperLog" class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title mb-4">最近操作日志</h2>
          <div v-if="recentOperLogsLoading" class="py-4 text-center text-slate-400">加载中...</div>
          <div v-else-if="recentOperLogs.length === 0" class="py-4 text-center text-slate-400">暂无日志</div>
          <ul v-else class="space-y-3 text-sm">
            <li v-for="log in recentOperLogs" :key="log.id" class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <div class="font-medium truncate">#{{ log.id }} {{ log.operatorName || '系统' }}</div>
                <div class="text-slate-400 truncate">{{ log.targetType }} / {{ log.action }}</div>
              </div>
              <span class="text-slate-400 whitespace-nowrap">{{ formatRelativeTime(log.createdAt) }}</span>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </div>

  <!-- Notice Detail Modal -->
  <dialog class="modal" :class="{ 'modal-open': showNoticeDetail }">
    <div class="modal-box max-w-2xl border-0 shadow-xl">
      <div class="flex items-center justify-between mb-4">
        <div class="flex items-center gap-2">
          <span v-if="noticeDetail?.isPinned" class="badge badge-error">置顶</span>
          <h3 class="font-bold text-lg">{{ noticeDetail?.title }}</h3>
        </div>
        <button class="btn btn-sm btn-circle btn-ghost" @click="showNoticeDetail = false">✕</button>
      </div>
      <div class="text-xs text-slate-400 mb-4">
        发布时间：{{ noticeDetail?.publishedAt ? formatRelativeTime(noticeDetail.publishedAt) : '-' }}
        <span v-if="noticeDetail?.createdByName" class="ml-4">发布者：{{ noticeDetail?.createdByName }}</span>
      </div>
      <div class="prose prose-sm max-w-none whitespace-pre-wrap text-slate-700">
        {{ noticeDetail?.content }}
      </div>
      <div class="modal-action">
        <button class="btn btn-sm" @click="showNoticeDetail = false">关闭</button>
      </div>
    </div>
    <form method="dialog" class="modal-backdrop" @click="showNoticeDetail = false"><button>close</button></form>
  </dialog>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { getDashboardStats, getRecentActivities, getRecentNotices, getRecentReports, getRecentVerifications, getRecentOperLogs, getNoticeDetail, getVisibleNoticeDetail, getLoginLogTrend, type DashboardStats, type RecentActivity, type RecentNotice, type RecentReport, type RecentVerification, type RecentOperLog, type NoticeVO, type LoginLogTrendItem } from '@/api/system'
import { formatRelativeTime } from '@/utils/time'
import { usePermissionStore } from '@/stores/permission'
import { useDialog } from '@/composables/useDialog'

const loading = ref(true)
const activitiesLoading = ref(true)
const activities = ref<RecentActivity[]>([])
const recentNoticesLoading = ref(false)
const recentNotices = ref<RecentNotice[]>([])
const recentReportsLoading = ref(false)
const recentReports = ref<RecentReport[]>([])
const recentVerificationsLoading = ref(false)
const recentVerifications = ref<RecentVerification[]>([])
const recentOperLogsLoading = ref(false)
const recentOperLogs = ref<RecentOperLog[]>([])
const loginTrendLoading = ref(false)
const loginTrend = ref<LoginLogTrendItem[]>([])
const showNoticeDetail = ref(false)
const noticeDetail = ref<NoticeVO | null>(null)
const stats = reactive<DashboardStats>({
  totalUsers: 0,
  todayNewUsers: 0,
  totalPosts: 0,
  todayPosts: 0,
  yesterdayPosts: 0,
  postGrowth: 0,
  userGrowth: 0,
  loginSuccessToday: 0,
  loginFailToday: 0,
  loginTotalToday: 0
})

const permissionStore = usePermissionStore()
const dialog = useDialog()
const canUserStats = computed(() => permissionStore.hasPermission('system:dashboard:user'))
const canPostStats = computed(() => permissionStore.hasPermission('system:dashboard:post'))
const canNoticeLatest = computed(() => permissionStore.hasPermission('system:dashboard:notice:list'))
const canNoticeOverview = computed(() => permissionStore.hasPermission('system:dashboard:notice:overview'))
const canNoticeManage = computed(() => permissionStore.hasPermission('system:notice:list'))
const canVerify = computed(() => permissionStore.hasPermission('system:dashboard:verify'))
const canReport = computed(() => permissionStore.hasPermission('system:dashboard:report'))
const canSensitive = computed(() => permissionStore.hasPermission('system:dashboard:ops'))
const canOperLog = computed(() => permissionStore.hasPermission('system:dashboard:operlog'))
const canLoginLog = computed(() => permissionStore.hasPermission('system:dashboard:login'))

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
  if (!canNoticeLatest.value) {
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

const fetchRecentReports = async () => {
  if (!canReport.value) {
    recentReports.value = []
    return
  }
  recentReportsLoading.value = true
  try {
    const res: any = await getRecentReports()
    recentReports.value = res || []
  } catch (error) {
    console.error('Failed to fetch recent reports', error)
  } finally {
    recentReportsLoading.value = false
  }
}

const fetchRecentVerifications = async () => {
  if (!canVerify.value) {
    recentVerifications.value = []
    return
  }
  recentVerificationsLoading.value = true
  try {
    const res: any = await getRecentVerifications()
    recentVerifications.value = res || []
  } catch (error) {
    console.error('Failed to fetch recent verifications', error)
  } finally {
    recentVerificationsLoading.value = false
  }
}

const fetchRecentOperLogs = async () => {
  if (!canOperLog.value) {
    recentOperLogs.value = []
    return
  }
  recentOperLogsLoading.value = true
  try {
    const res: any = await getRecentOperLogs()
    recentOperLogs.value = res || []
  } catch (error) {
    console.error('Failed to fetch recent oper logs', error)
  } finally {
    recentOperLogsLoading.value = false
  }
}

const fetchLoginTrend = async () => {
  if (!canLoginLog.value) {
    loginTrend.value = []
    return
  }
  loginTrendLoading.value = true
  try {
    const res: any = await getLoginLogTrend()
    loginTrend.value = res || []
  } catch (error) {
    console.error('Failed to fetch login trend', error)
  } finally {
    loginTrendLoading.value = false
  }
}

const openNoticeDetail = async (noticeId: number) => {
  if (!canNoticeLatest.value) return
  try {
    const res: any = canNoticeManage.value
      ? await getNoticeDetail(noticeId)
      : await getVisibleNoticeDetail(noticeId)
    noticeDetail.value = res || null
    showNoticeDetail.value = true
  } catch (error) {
    console.error('Failed to fetch notice detail', error)
    await dialog.alert('暂无权限查看该公告详情或公告已失效')
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
  fetchRecentReports()
  fetchRecentVerifications()
  fetchRecentOperLogs()
  fetchLoginTrend()
})
</script>
