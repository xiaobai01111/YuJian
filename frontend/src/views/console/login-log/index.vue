<template>
  <div class="card bg-base-100 shadow-xl">
    <div class="card-body">
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

      <div class="overflow-x-auto min-h-[400px]">
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

      <!-- Pagination -->
      <div class="flex justify-end mt-4">
        <div class="join">
          <button class="join-item btn btn-sm" :disabled="page <= 1" @click="changePage(page - 1)">«</button>
          <button class="join-item btn btn-sm">Page {{ page }}</button>
          <button class="join-item btn btn-sm" :disabled="logList.length < pageSize" @click="changePage(page + 1)">»</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

interface LoginLog {
  id: number
  username: string
  ipaddr: string
  loginLocation: string
  browser: string
  os: string
  status: number
  msg?: string
  loginTime: string
}

const loading = ref(false)
const logList = ref<LoginLog[]>([])
const page = ref(1)
const pageSize = ref(10)

onMounted(() => {
  fetchData()
})

const fetchData = async () => {
  loading.value = true
  setTimeout(() => {
    logList.value = [
      { id: 1, username: 'admin', ipaddr: '127.0.0.1', loginLocation: '内网IP', browser: 'Chrome', os: 'Windows 10', status: 0, loginTime: '2026-01-12 19:00:00' },
      { id: 2, username: 'test', ipaddr: '192.168.1.100', loginLocation: '内网IP', browser: 'Firefox', os: 'macOS', status: 0, loginTime: '2026-01-12 18:30:00' },
      { id: 3, username: 'user1', ipaddr: '10.0.0.50', loginLocation: '内网IP', browser: 'Safari', os: 'iOS', status: 1, loginTime: '2026-01-12 18:00:00' },
    ]
    loading.value = false
  }, 500)
}

const changePage = (p: number) => {
  page.value = p
  fetchData()
}

const handleDelete = async (_log: LoginLog) => {
  if (!confirm(`确定要删除该登录日志吗？`)) return
  fetchData()
}

const handleClear = async () => {
  if (!confirm('确定要清空所有登录日志吗？此操作不可恢复！')) return
  logList.value = []
}

const handleExport = () => {
  alert('导出功能开发中...')
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return dateStr
}
</script>
