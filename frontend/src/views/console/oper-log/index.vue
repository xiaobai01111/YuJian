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

      <div class="overflow-x-auto min-h-[400px]">
        <table class="table table-zebra">
          <thead>
            <tr>
              <th>日志编号</th>
              <th>操作模块</th>
              <th>操作类型</th>
              <th>操作人员</th>
              <th>操作IP</th>
              <th>状态</th>
              <th>操作时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="8" class="text-center py-4">加载中...</td>
            </tr>
            <tr v-else-if="logList.length === 0">
              <td colspan="8" class="text-center py-4">暂无数据</td>
            </tr>
            <tr v-else v-for="log in logList" :key="log.id">
              <td>{{ log.id }}</td>
              <td>{{ log.title }}</td>
              <td><span class="badge badge-info badge-sm">{{ log.businessType }}</span></td>
              <td>{{ log.operName }}</td>
              <td><span class="badge badge-ghost">{{ log.operIp }}</span></td>
              <td>
                <span :class="['badge', log.status === 0 ? 'badge-success' : 'badge-error']">
                  {{ log.status === 0 ? '成功' : '失败' }}
                </span>
              </td>
              <td class="text-sm text-slate-500">{{ formatDate(log.operTime) }}</td>
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
          <button class="join-item btn btn-sm">Page {{ page }}</button>
          <button class="join-item btn btn-sm" :disabled="logList.length < pageSize" @click="changePage(page + 1)">»</button>
        </div>
      </div>
    </div>

    <!-- Detail Modal -->
    <dialog id="detail_modal" class="modal">
      <div class="modal-box max-w-2xl">
        <h3 class="font-bold text-lg">操作日志详情</h3>
        <div class="mt-4 space-y-2 text-sm">
          <p><span class="font-semibold">操作模块：</span>{{ currentLog?.title }}</p>
          <p><span class="font-semibold">操作类型：</span>{{ currentLog?.businessType }}</p>
          <p><span class="font-semibold">请求方法：</span>{{ currentLog?.method }}</p>
          <p><span class="font-semibold">请求URL：</span>{{ currentLog?.operUrl }}</p>
          <p><span class="font-semibold">操作人员：</span>{{ currentLog?.operName }}</p>
          <p><span class="font-semibold">操作IP：</span>{{ currentLog?.operIp }}</p>
          <p><span class="font-semibold">请求参数：</span></p>
          <pre class="bg-base-200 p-2 rounded text-xs overflow-auto max-h-32">{{ currentLog?.operParam }}</pre>
          <p><span class="font-semibold">返回结果：</span></p>
          <pre class="bg-base-200 p-2 rounded text-xs overflow-auto max-h-32">{{ currentLog?.jsonResult }}</pre>
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
import { ref, onMounted } from 'vue'

interface OperLog {
  id: number
  title: string
  businessType: string
  method: string
  operUrl: string
  operName: string
  operIp: string
  operParam?: string
  jsonResult?: string
  status: number
  operTime: string
}

const loading = ref(false)
const logList = ref<OperLog[]>([])
const page = ref(1)
const pageSize = ref(10)
const currentLog = ref<OperLog | null>(null)

onMounted(() => {
  fetchData()
})

const fetchData = async () => {
  loading.value = true
  setTimeout(() => {
    logList.value = [
      { id: 1, title: '用户管理', businessType: '新增', method: 'POST', operUrl: '/v1/users', operName: 'admin', operIp: '127.0.0.1', operParam: '{"username":"test"}', jsonResult: '{"code":200}', status: 0, operTime: '2026-01-12 19:00:00' },
      { id: 2, title: '角色管理', businessType: '修改', method: 'PUT', operUrl: '/v1/roles/1', operName: 'admin', operIp: '127.0.0.1', operParam: '{"roleName":"管理员"}', jsonResult: '{"code":200}', status: 0, operTime: '2026-01-12 18:30:00' },
      { id: 3, title: '菜单管理', businessType: '删除', method: 'DELETE', operUrl: '/v1/menus/5', operName: 'admin', operIp: '127.0.0.1', operParam: '{}', jsonResult: '{"code":500}', status: 1, operTime: '2026-01-12 18:00:00' },
    ]
    loading.value = false
  }, 500)
}

const changePage = (p: number) => {
  page.value = p
  fetchData()
}

const viewDetail = (log: OperLog) => {
  currentLog.value = log
  const modal = document.getElementById('detail_modal') as HTMLDialogElement
  modal.showModal()
}

const handleDelete = async (_log: OperLog) => {
  if (!confirm(`确定要删除该操作日志吗？`)) return
  fetchData()
}

const handleClear = async () => {
  if (!confirm('确定要清空所有操作日志吗？此操作不可恢复！')) return
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
