<template>
  <div class="h-full flex flex-col overflow-hidden">
    <div class="flex-1 min-h-0 flex flex-col gap-6 overflow-hidden p-6">
      <div class="flex flex-wrap justify-between items-center gap-3">
        <div>
          <h1 class="text-2xl font-bold">举报管理</h1>
          <p class="text-gray-500 mt-1">处理用户举报内容</p>
        </div>
        <div class="flex items-center gap-2">
          <button
            v-if="canBatchHandle"
            class="btn btn-outline btn-sm"
            :disabled="selectedIds.length === 0"
            @click="handleBatchDecision('同意')"
          >
            批量同意
          </button>
          <button
            v-if="canBatchHandle"
            class="btn btn-outline btn-sm"
            :disabled="selectedIds.length === 0"
            @click="handleBatchDecision('驳回')"
          >
            批量驳回
          </button>
          <button class="btn btn-ghost btn-sm" @click="loadReports">
            刷新
          </button>
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
            <button class="btn btn-sm btn-ghost" @click="loadReports">
              搜索
            </button>
          </div>
        </div>
      </div>

      <div class="card bg-base-100 shadow-sm">
        <div class="card-body p-0 flex flex-col">
          <div class="overflow-auto max-h-[calc(100vh-360px)]">
            <div class="overflow-x-auto">
              <table class="table">
                <thead>
                  <tr>
                    <th class="w-12">
                      <input
                        type="checkbox"
                        class="checkbox checkbox-sm"
                        :checked="allSelected"
                        :disabled="!canBatchHandle || selectableReports.length === 0"
                        @change="toggleAll"
                      />
                    </th>
                    <th class="w-16">ID</th>
                    <th>原因</th>
                    <th class="w-40">帖子</th>
                    <th class="w-28">举报者</th>
                    <th class="w-24">状态</th>
                    <th class="w-40">举报时间</th>
                    <th class="w-40">处理时间</th>
                    <th class="w-48">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="loading">
                    <td colspan="9" class="text-center py-8">
                      <span class="loading loading-spinner loading-md"></span>
                    </td>
                  </tr>
                  <tr v-else-if="reports.length === 0">
                    <td colspan="9" class="text-center py-8 text-gray-500">暂无数据</td>
                  </tr>
                  <tr v-for="report in reports" :key="report.id" class="hover">
                    <td>
                      <input
                        v-model="selectedIds"
                        type="checkbox"
                        class="checkbox checkbox-sm"
                        :value="report.id"
                        :disabled="!canBatchHandle || report.status !== 0"
                      />
                    </td>
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
                    <td>{{ formatDateTime(report.handledAt) }}</td>
                    <td>
                      <div class="flex flex-wrap gap-1">
                        <button
                          v-if="canHandle && report.status === 0"
                          class="btn btn-ghost btn-xs"
                          @click="handleSubmit(report)"
                        >提交</button>
                        <button
                          v-if="canHandle && report.status === 0"
                          class="btn btn-success btn-xs"
                          @click="handleDecision(report, '同意')"
                        >同意</button>
                        <button
                          v-if="canHandle && report.status === 0"
                          class="btn btn-error btn-xs"
                          @click="handleDecision(report, '驳回')"
                        >驳回</button>
                        <button
                          v-if="canDelete"
                          class="btn btn-outline btn-xs"
                          @click="handleDelete(report)"
                        >删除</button>
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { batchHandleConsoleReports, deleteConsoleReport, getConsoleReports, handleConsoleReport, type ReportVO } from '@/api/report'
import { useUserStore } from '@/stores/user'
import { useDialog } from '@/composables/useDialog'

const userStore = useUserStore()
const dialog = useDialog()

const reports = ref<ReportVO[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const selectedIds = ref<number[]>([])

const filters = reactive({
  status: undefined as number | undefined
})

const canHandle = computed(() => userStore.hasPermission('content:report:handle'))
const canBatchHandle = computed(() => userStore.hasPermission('content:report:batch-handle'))
const canDelete = computed(() => userStore.hasPermission('content:report:delete'))

const selectableReports = computed(() => reports.value.filter(report => report.status === 0))

const allSelected = computed(() => {
  return selectableReports.value.length > 0
    && selectableReports.value.every(report => selectedIds.value.includes(report.id))
})

const loadReports = async () => {
  loading.value = true
  try {
    const res = await getConsoleReports({
      page: currentPage.value,
      size: pageSize.value,
      status: filters.status
    })
    reports.value = res.records || []
    total.value = res.total || 0
    selectedIds.value = []
  } catch (error) {
    console.error('Failed to load reports', error)
  } finally {
    loading.value = false
  }
}

const changePage = (page: number) => {
  currentPage.value = page
  loadReports()
}

const toggleAll = () => {
  if (allSelected.value) {
    selectedIds.value = []
  } else {
    selectedIds.value = selectableReports.value.map(report => report.id)
  }
}

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  const date = new Date(value)
  return `${date.toLocaleDateString()} ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
}

const getStatusClass = (status: number) => {
  if (status === 0) return 'badge badge-warning badge-sm'
  if (status === 1) return 'badge badge-success badge-sm'
  return 'badge badge-ghost badge-sm'
}

const promptRemark = async () => {
  const remark = await dialog.prompt('请输入处理说明（必要时必填）', { multiline: true })
  if (remark == null) return ''
  return remark.trim()
}

const needReason = (report: ReportVO) => {
  const currentUserId = userStore.userInfo?.id
  return !currentUserId || report.reporter?.id !== currentUserId
}

const promptReason = async (required: boolean) => {
  const tip = required ? '请输入操作原因（必填）' : '请输入操作原因（可选）'
  const reason = await dialog.prompt(tip, { required, multiline: true })
  if (reason == null) return required ? null : ''
  return reason.trim()
}

const handleSubmit = async (report: ReportVO) => {
  const result = await dialog.prompt('请输入处理结果', {
    defaultValue: report.result || '',
    required: true,
    multiline: true
  })
  if (!result || !result.trim()) return
  const remark = await promptRemark()
  try {
    await handleConsoleReport(report.id, { result: result.trim(), remark: remark || undefined })
    await loadReports()
  } catch (error: unknown) {
    await dialog.alert((error as ApiErrorLike)?.message || '处理失败')
  }
}

const handleDecision = async (report: ReportVO, decision: string) => {
  if (!await dialog.confirm(`确认${decision}该举报？`)) return
  const remark = await promptRemark()
  try {
    await handleConsoleReport(report.id, { result: decision, remark: remark || undefined })
    await loadReports()
  } catch (error: unknown) {
    await dialog.alert((error as ApiErrorLike)?.message || '处理失败')
  }
}

const handleBatchDecision = async (decision: string) => {
  if (selectedIds.value.length === 0) return
  if (!await dialog.confirm(`确认批量${decision}选中的 ${selectedIds.value.length} 条举报？`)) return
  const remark = await promptRemark()
  try {
    await batchHandleConsoleReports(selectedIds.value, decision, remark || undefined)
    await loadReports()
  } catch (error: unknown) {
    await dialog.alert((error as ApiErrorLike)?.message || '批量处理失败')
  }
}

const handleDelete = async (report: ReportVO) => {
  if (!await dialog.confirm(`确认删除举报 #${report.id}？`)) return
  const reason = await promptReason(needReason(report))
  if (reason === null) return
  try {
    await deleteConsoleReport(report.id, reason || undefined)
    await loadReports()
  } catch (error: unknown) {
    await dialog.alert((error as ApiErrorLike)?.message || '删除失败')
  }
}

onMounted(() => {
  loadReports()
})
</script>
