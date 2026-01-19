<template>
  <div class="h-full flex flex-col overflow-hidden">
    <div class="flex-1 min-h-0 flex flex-col gap-6 overflow-hidden p-6">
      <div class="flex flex-wrap justify-between items-center gap-3">
        <div>
          <h1 class="text-2xl font-bold">举报回收站</h1>
          <p class="text-gray-500 mt-1">查看已删除举报并执行恢复或彻底删除</p>
        </div>
        <div class="flex items-center gap-2">
          <button class="btn btn-ghost btn-sm" @click="loadReports">刷新</button>
        </div>
      </div>

      <div class="card bg-base-100 shadow-sm">
        <div class="card-body p-4">
          <div class="flex flex-wrap gap-4 items-center">
            <div class="form-control">
              <select v-model="filters.status" class="select select-bordered select-sm w-32" @change="loadReports">
                <option :value="undefined">全部状态</option>
                <option :value="0">待处理</option>
                <option :value="1">已处理</option>
              </select>
            </div>
            <button class="btn btn-sm btn-ghost" @click="loadReports">搜索</button>
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
                    <th>原因</th>
                    <th class="w-40">帖子</th>
                    <th class="w-28">举报者</th>
                    <th class="w-24">状态</th>
                    <th class="w-40">删除时间</th>
                    <th class="w-44">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="loading">
                    <td colspan="7" class="text-center py-8">
                      <span class="loading loading-spinner loading-md"></span>
                    </td>
                  </tr>
                  <tr v-else-if="reports.length === 0">
                    <td colspan="7" class="text-center py-8 text-gray-500">暂无数据</td>
                  </tr>
                  <tr v-for="report in reports" :key="report.id" class="hover">
                    <td>{{ report.id }}</td>
                    <td>
                      <div class="line-clamp-2 max-w-lg">{{ report.reason }}</div>
                    </td>
                    <td>
                      <div class="line-clamp-1 max-w-xs">#{{ report.post?.id }} {{ report.post?.title || '' }}</div>
                    </td>
                    <td>{{ report.reporter?.nickname || report.reporter?.username || '-' }}</td>
                    <td>
                      <span :class="getStatusClass(report.status)">
                        {{ report.status === 1 ? '已处理' : '待处理' }}
                      </span>
                    </td>
                    <td>{{ formatDateTime(report.createdAt) }}</td>
                    <td>
                      <div class="flex gap-1">
                        <button
                          v-if="canRestore"
                          class="btn btn-success btn-xs"
                          @click="handleRestore(report)"
                        >恢复</button>
                        <button
                          v-if="canPurge"
                          class="btn btn-error btn-xs"
                          @click="handlePurge(report)"
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
import { getRecycleReports, purgeRecycleReport, restoreRecycleReport, type ReportVO } from '@/api/recycle'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const reports = ref<ReportVO[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const filters = reactive({
  status: undefined as number | undefined
})

const canRestore = computed(() => userStore.hasPermission('content:recycle:report:restore'))
const canPurge = computed(() => userStore.hasPermission('content:recycle:report:purge'))
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const loadReports = async () => {
  loading.value = true
  try {
    const res: any = await getRecycleReports({
      page: currentPage.value,
      size: pageSize.value,
      status: filters.status
    })
    reports.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error('Failed to load recycle reports', error)
  } finally {
    loading.value = false
  }
}

const changePage = (page: number) => {
  currentPage.value = page
  loadReports()
}

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  const date = new Date(value)
  return `${date.toLocaleDateString()} ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
}

const getStatusClass = (status?: number) => {
  if (status === 0) return 'badge badge-warning badge-sm'
  if (status === 1) return 'badge badge-success badge-sm'
  return 'badge badge-ghost badge-sm'
}

const needReason = (report: ReportVO) => {
  const currentUserId = userStore.userInfo?.id
  return !currentUserId || report.reporter?.id !== currentUserId
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

const handleRestore = async (report: ReportVO) => {
  if (!confirm(`确认恢复举报 #${report.id}？`)) return
  const reason = promptReason(needReason(report))
  if (reason === null) return
  try {
    await restoreRecycleReport(report.id, reason || undefined)
    await loadReports()
  } catch (error: any) {
    alert(error?.message || '恢复失败')
  }
}

const handlePurge = async (report: ReportVO) => {
  if (!confirm(`确认彻底删除举报 #${report.id}？此操作不可恢复。`)) return
  const reason = promptReason(needReason(report))
  if (reason === null) return
  try {
    await purgeRecycleReport(report.id, reason || undefined)
    await loadReports()
  } catch (error: any) {
    alert(error?.message || '删除失败')
  }
}

onMounted(() => {
  loadReports()
})
</script>
