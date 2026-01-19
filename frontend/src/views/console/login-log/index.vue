<template>
  <div class="h-full flex flex-col overflow-hidden">
    <div class="card bg-base-100 shadow-xl flex-1 min-h-0">
      <div class="card-body flex flex-col min-h-0">
      <div class="flex justify-between items-center mb-4">
        <h2 class="card-title">登录日志</h2>
        <div class="flex gap-2">
          <button class="btn btn-error btn-sm" @click="handleClear" v-permission="['system:loginlog:clear']">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
            清空日志
          </button>
          <button class="btn btn-primary btn-sm" @click="handleExport" v-permission="['system:loginlog:export']">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
            </svg>
            导出
          </button>
        </div>
      </div>

      <div class="flex flex-wrap gap-3 mb-4">
        <input v-model="queryParams.username" class="input input-bordered input-sm" placeholder="用户名" />
        <input v-model="queryParams.ipaddr" class="input input-bordered input-sm" placeholder="登录IP" />
        <select v-model="queryParams.status" class="select select-bordered select-sm">
          <option value="">全部状态</option>
          <option value="0">成功</option>
          <option value="1">失败</option>
        </select>
        <input type="date" v-model="queryParams.loginTimeStart" class="input input-bordered input-sm" />
        <input type="date" v-model="queryParams.loginTimeEnd" class="input input-bordered input-sm" />
        <button class="btn btn-sm btn-primary" @click="handleSearch">搜索</button>
        <button class="btn btn-sm btn-ghost" @click="handleReset">重置</button>
      </div>

      <div class="flex-1 overflow-auto">
        <div class="overflow-x-auto">
          <table class="table table-zebra">
            <thead>
              <tr>
                <th>日志编号</th>
                <th>用户名</th>
                <th>登录IP</th>
                <th>登录地点</th>
                <th>浏览器</th>
                <th>操作系统</th>
                <th>状态</th>
                <th>登录时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="9" class="text-center py-4">加载中...</td>
              </tr>
              <tr v-else-if="logList.length === 0">
                <td colspan="9" class="text-center py-4">暂无数据</td>
              </tr>
              <tr v-else v-for="log in logList" :key="log.id">
                <td>{{ log.id }}</td>
                <td>{{ log.username }}</td>
                <td><span class="badge badge-ghost">{{ log.ipaddr }}</span></td>
                <td>{{ log.loginLocation }}</td>
                <td>{{ log.browser }}</td>
                <td>{{ log.os }}</td>
                <td>
                  <span :class="['badge', log.status === 0 ? 'badge-success' : 'badge-error']">
                    {{ log.status === 0 ? '成功' : '失败' }}
                  </span>
                </td>
                <td class="text-sm text-slate-500">{{ formatDate(log.loginTime) }}</td>
                <td>
                  <button class="btn btn-ghost btn-xs text-error" @click="handleDelete(log)" v-permission="['system:loginlog:delete']">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Pagination -->
      <div class="flex justify-end pt-4">
        <div class="join">
          <button class="join-item btn btn-sm" :disabled="page <= 1" @click="changePage(page - 1)">«</button>
          <button class="join-item btn btn-sm">Page {{ page }} / {{ totalPages }}</button>
          <button class="join-item btn btn-sm" :disabled="page >= totalPages" @click="changePage(page + 1)">»</button>
        </div>
      </div>
    </div>
  </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { clearLoginLogs, deleteLoginLog, exportLoginLogs, getLoginLogList, type LoginLogVO } from '@/api/system'

const loading = ref(false)
const logList = ref<LoginLogVO[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
const queryParams = reactive({
  username: '',
  ipaddr: '',
  status: '',
  loginTimeStart: '',
  loginTimeEnd: ''
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
    if (queryParams.username) params.username = queryParams.username
    if (queryParams.ipaddr) params.ipaddr = queryParams.ipaddr
    if (queryParams.status !== '') params.status = Number(queryParams.status)
    if (queryParams.loginTimeStart) params.loginTimeStart = queryParams.loginTimeStart
    if (queryParams.loginTimeEnd) params.loginTimeEnd = queryParams.loginTimeEnd

    const res = await getLoginLogList(params)
    logList.value = res?.records || []
    total.value = res?.total || 0
  } catch (error: any) {
    logList.value = []
    total.value = 0
    alert(error?.message || error?.response?.data?.message || '获取登录日志失败')
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
  queryParams.username = ''
  queryParams.ipaddr = ''
  queryParams.status = ''
  queryParams.loginTimeStart = ''
  queryParams.loginTimeEnd = ''
  page.value = 1
  fetchData()
}

const handleDelete = async (log: LoginLogVO) => {
  if (!confirm(`确定要删除该登录日志吗？`)) return
  try {
    await deleteLoginLog(log.id)
    fetchData()
  } catch (error: any) {
    alert(error?.message || error?.response?.data?.message || '删除失败')
  }
}

const handleClear = async () => {
  if (!confirm('确定要清空所有登录日志吗？此操作不可恢复！')) return
  try {
    await clearLoginLogs()
    page.value = 1
    fetchData()
  } catch (error: any) {
    alert(error?.message || error?.response?.data?.message || '清空失败')
  }
}

const handleExport = async () => {
  try {
    const res = await exportLoginLogs()
    const blob = new Blob([res as unknown as BlobPart], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '登录日志.xlsx'
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (error: any) {
    alert(error?.message || error?.response?.data?.message || '导出失败')
  }
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}
</script>
