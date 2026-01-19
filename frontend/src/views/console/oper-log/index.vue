<template>
  <div class="card bg-base-100 shadow-xl">
    <div class="card-body">
      <div class="flex justify-between items-center mb-4">
        <h2 class="card-title">操作日志</h2>
        <div class="flex gap-2">
          <button class="btn btn-error btn-sm" @click="handleClear" v-permission="['system:operlog:clear']">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
            清空日志
          </button>
          <button class="btn btn-primary btn-sm" @click="handleExport" v-permission="['system:operlog:export']">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
            </svg>
            导出
          </button>
        </div>
      </div>

      <div class="flex flex-wrap gap-3 mb-4">
        <input v-model="queryParams.operatorName" class="input input-bordered input-sm" placeholder="操作人" />
        <input v-model="queryParams.targetType" class="input input-bordered input-sm" placeholder="目标类型" />
        <input v-model="queryParams.action" class="input input-bordered input-sm" placeholder="动作" />
        <input type="date" v-model="queryParams.startTime" class="input input-bordered input-sm" />
        <input type="date" v-model="queryParams.endTime" class="input input-bordered input-sm" />
        <button class="btn btn-sm btn-primary" @click="handleSearch">搜索</button>
        <button class="btn btn-sm btn-ghost" @click="handleReset">重置</button>
      </div>

      <div class="overflow-x-auto min-h-[400px]">
        <table class="table table-zebra">
          <thead>
            <tr>
              <th>日志编号</th>
              <th>操作人</th>
              <th>目标类型</th>
              <th>动作</th>
              <th>操作IP</th>
              <th>操作时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="7" class="text-center py-4">加载中...</td>
            </tr>
            <tr v-else-if="logList.length === 0">
              <td colspan="7" class="text-center py-4">暂无数据</td>
            </tr>
            <tr v-else v-for="log in logList" :key="log.id">
              <td>{{ log.id }}</td>
              <td>{{ log.operatorName || '系统' }}</td>
              <td>{{ getTargetTypeLabel(log.targetType) }}</td>
              <td>
                <span class="badge badge-info badge-sm" :title="log.action || '-'">
                  {{ getActionLabel(log.action) }}
                </span>
              </td>
              <td><span class="badge badge-ghost">{{ log.ipAddress || '-' }}</span></td>
              <td class="text-sm text-slate-500">{{ formatDate(log.createdAt) }}</td>
              <td>
                <div class="flex gap-2">
                  <button class="btn btn-ghost btn-xs text-info" @click="viewDetail(log)">详情</button>
                  <button class="btn btn-ghost btn-xs text-error" @click="handleDelete(log)" v-permission="['system:operlog:delete']">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div class="flex justify-end mt-4">
        <div class="join">
          <button class="join-item btn btn-sm" :disabled="page <= 1" @click="changePage(page - 1)">«</button>
          <button class="join-item btn btn-sm">Page {{ page }} / {{ totalPages }}</button>
          <button class="join-item btn btn-sm" :disabled="page >= totalPages" @click="changePage(page + 1)">»</button>
        </div>
      </div>
    </div>

    <!-- Detail Modal -->
    <dialog id="detail_modal" class="modal">
      <div class="modal-box max-w-2xl">
        <h3 class="font-bold text-lg">操作日志详情</h3>
        <div class="mt-4 space-y-2 text-sm">
          <p><span class="font-semibold">操作人：</span>{{ currentLog?.operatorName || '系统' }}</p>
          <p><span class="font-semibold">目标类型：</span>{{ getTargetTypeLabel(currentLog?.targetType) }}</p>
          <p><span class="font-semibold">目标ID：</span>{{ currentLog?.targetId || '-' }}</p>
          <p><span class="font-semibold">动作：</span>{{ getActionLabel(currentLog?.action) }}</p>
          <p><span class="font-semibold">原因：</span>{{ currentLog?.reason || '-' }}</p>
          <p><span class="font-semibold">IP：</span>{{ currentLog?.ipAddress || '-' }}</p>
          <p><span class="font-semibold">操作时间：</span>{{ formatDate(currentLog?.createdAt) }}</p>
          <p><span class="font-semibold">变更前：</span></p>
          <pre class="bg-base-200 p-2 rounded text-xs overflow-auto max-h-32">{{ formatJson(currentLog?.beforeValue) }}</pre>
          <p><span class="font-semibold">变更后：</span></p>
          <pre class="bg-base-200 p-2 rounded text-xs overflow-auto max-h-32">{{ formatJson(currentLog?.afterValue) }}</pre>
        </div>
        <div class="modal-action">
          <form method="dialog">
            <button class="btn">关闭</button>
          </form>
        </div>
      </div>
    </dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive } from 'vue'
import { clearOperLogs, deleteOperLog, exportOperLogs, getOperLogList, type OperLogVO } from '@/api/system'

const loading = ref(false)
const logList = ref<OperLogVO[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
const currentLog = ref<OperLogVO | null>(null)
const queryParams = reactive({
  operatorName: '',
  targetType: '',
  action: '',
  startTime: '',
  endTime: ''
})

onMounted(() => {
  fetchData()
})

const fetchData = async () => {
  loading.value = true
  try {
    const params: any = {
      page: page.value,
      size: pageSize.value
    }
    if (queryParams.operatorName) params.operatorName = queryParams.operatorName
    if (queryParams.targetType) params.targetType = queryParams.targetType
    if (queryParams.action) params.action = queryParams.action
    if (queryParams.startTime) params.startTime = queryParams.startTime
    if (queryParams.endTime) params.endTime = queryParams.endTime

    const res = await getOperLogList(params)
    logList.value = res?.records || []
    total.value = res?.total || 0
  } catch (error: any) {
    logList.value = []
    total.value = 0
    alert(error?.message || error?.response?.data?.message || '获取操作日志失败')
  } finally {
    loading.value = false
  }
}

const changePage = (p: number) => {
  page.value = p
  fetchData()
}

const handleSearch = () => {
  page.value = 1
  fetchData()
}

const handleReset = () => {
  queryParams.operatorName = ''
  queryParams.targetType = ''
  queryParams.action = ''
  queryParams.startTime = ''
  queryParams.endTime = ''
  page.value = 1
  fetchData()
}

const viewDetail = (log: OperLogVO) => {
  currentLog.value = log
  const modal = document.getElementById('detail_modal') as HTMLDialogElement
  modal.showModal()
}

const handleDelete = async (log: OperLogVO) => {
  if (!confirm(`确定要删除该操作日志吗？`)) return
  try {
    await deleteOperLog(log.id)
    fetchData()
  } catch (error: any) {
    alert(error?.message || error?.response?.data?.message || '删除失败')
  }
}

const handleClear = async () => {
  if (!confirm('确定要清空所有操作日志吗？此操作不可恢复！')) return
  try {
    await clearOperLogs()
    page.value = 1
    fetchData()
  } catch (error: any) {
    alert(error?.message || error?.response?.data?.message || '清空失败')
  }
}

const handleExport = async () => {
  try {
    const res = await exportOperLogs()
    const blob = new Blob([res as unknown as BlobPart], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '操作日志.xlsx'
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (error: any) {
    alert(error?.message || error?.response?.data?.message || '导出失败')
  }
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

const formatJson = (value: any) => {
  if (value === null || value === undefined) return '-'
  if (typeof value === 'string') return value
  try {
    return JSON.stringify(value, null, 2)
  } catch (error) {
    return String(value)
  }
}

const actionLabels: Record<string, string> = {
  create: '新增',
  update: '修改',
  delete: '删除',
  restore: '恢复',
  ban: '封禁',
  unban: '解封',
  role_assign: '分配角色',
  menu_assign: '分配菜单',
  dept_assign: '分配部门',
  status_change: '状态变更',
  publish: '发布',
  offline: '下线',
  import: '导入',
  export: '导出',
  resolve: '标记已解决'
}

const targetTypeLabels: Record<string, string> = {
  user: '用户',
  role: '角色',
  dept: '部门',
  notice: '公告',
  sensitive_word: '敏感词',
  post: '帖子',
  comment: '评论',
  report: '举报',
  auth_rule: '认证规则',
  menu: '菜单',
  login_log: '登录日志',
  oper_log: '操作日志'
}

const getActionLabel = (action?: string | null) => {
  if (!action) return '-'
  return actionLabels[action] || action
}

const getTargetTypeLabel = (targetType?: string | null) => {
  if (!targetType) return '-'
  return targetTypeLabels[targetType] || targetType
}
</script>
