<template>
  <div class="h-full flex flex-col">
    <div class="card bg-base-100 shadow-sm border border-base-200 flex-1 flex flex-col">
      <div class="card-body p-4 flex-1 flex flex-col overflow-hidden">
        <!-- Search Panel -->
        <div class="collapse collapse-arrow bg-base-200/50 rounded-lg mb-4 flex-none" :class="{ 'collapse-open': showSearch }">
          <input type="checkbox" v-model="showSearch" />
          <div class="collapse-title text-sm font-medium py-2 min-h-0">
            <span class="flex items-center gap-2">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              搜索
            </span>
          </div>
          <div class="collapse-content">
            <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3 pt-2">
              <div class="form-control">
                <label class="label py-1"><span class="label-text text-xs">登录账号</span></label>
                <input type="text" v-model="queryParams.username" placeholder="请输入登录账号" class="input input-bordered input-sm" />
              </div>
              <div class="form-control">
                <label class="label py-1"><span class="label-text text-xs">用户名称</span></label>
                <input type="text" v-model="queryParams.nickname" placeholder="请输入用户名称" class="input input-bordered input-sm" />
              </div>
              <div class="form-control">
                <label class="label py-1"><span class="label-text text-xs">手机号</span></label>
                <input type="text" v-model="queryParams.phone" placeholder="请输入手机号" class="input input-bordered input-sm" />
              </div>
              <div class="form-control">
                <label class="label py-1"><span class="label-text text-xs">登录时间</span></label>
                <input type="date" v-model="queryParams.loginDateStart" class="input input-bordered input-sm" />
              </div>
              <div class="form-control">
                <label class="label py-1"><span class="label-text text-xs">至</span></label>
                <input type="date" v-model="queryParams.loginDateEnd" class="input input-bordered input-sm" />
              </div>
            </div>
            <div class="flex gap-2 mt-3">
              <button class="btn btn-primary btn-sm" @click="handleSearch">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                搜索
              </button>
              <button class="btn btn-ghost btn-sm" @click="resetSearch">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                重置
              </button>
            </div>
          </div>
        </div>

        <!-- Toolbar -->
        <div class="flex flex-wrap justify-between items-center mb-4 gap-4 flex-none">
          <div class="flex flex-wrap gap-2">
            <button v-if="userStore.hasPermission('system:user:add')" class="btn btn-primary btn-sm gap-2 font-normal" @click="handleAdd">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
              </svg>
              添加
            </button>
            <button v-if="userStore.hasPermission('system:user:edit')" class="btn btn-success btn-sm text-white gap-2 font-normal" :disabled="selectedIds.length !== 1" @click="handleEdit">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
              修改
            </button>
            <button v-if="userStore.hasPermission('system:user:delete')" class="btn btn-error btn-sm text-white gap-2 font-normal" :disabled="!hasSelection" @click="handleDelete">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
              删除
            </button>
            <button v-if="userStore.hasPermission('system:user:role')" class="btn btn-info btn-sm text-white gap-2 font-normal" :disabled="selectedIds.length === 0" @click="handleBatchAssignRole">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
              批量分配角色
            </button>
            <button v-if="userStore.hasPermission('system:user:export')" class="btn btn-warning btn-sm text-white gap-2 font-normal" @click="handleExport">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
              </svg>
              导出
            </button>
            <button v-if="userStore.hasPermission('system:user:import')" class="btn btn-ghost btn-sm border-base-300 gap-2 bg-base-200 font-normal" @click="handleImport">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m4-8l-4-4m0 0L8 8m4-4v12" />
              </svg>
              导入
            </button>
          </div>
          
          <div class="flex gap-2">
            <button class="btn btn-circle btn-ghost btn-sm" @click="fetchData">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-base-content/60" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
            </button>
          </div>
        </div>

        <!-- Table -->
        <div class="overflow-auto flex-1 min-h-0">
          <table class="table table-md table-pin-rows">
            <thead class="bg-base-200/30 text-base-content/70">
              <tr>
                <th class="w-10">
                  <label>
                    <input type="checkbox" class="checkbox checkbox-sm rounded-sm" :checked="isAllSelected" @change="toggleSelectAll" />
                  </label>
                </th>
                <th class="w-16">序号</th>
                <th>登录账号</th>
                <th>头像</th>
                <th>用户名称</th>
                <th>邮箱</th>
                <th>手机号</th>
                <th>角色</th>
                <th>用户性别</th>
                <th>用户状态</th>
                <th>登录时间</th>
                <th class="text-center w-32">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="12" class="text-center py-10">
                  <span class="loading loading-spinner loading-lg text-primary"></span>
                </td>
              </tr>
              <tr v-else-if="userList.length === 0">
                <td colspan="12" class="text-center py-10 text-base-content/60">暂无数据</td>
              </tr>
              <tr v-else v-for="(user, index) in userList" :key="user.id" class="hover border-b border-base-100 last:border-0">
                <th>
                  <label>
                    <input type="checkbox" class="checkbox checkbox-sm rounded-sm" :checked="selectedIds.includes(user.id)" @change="toggleSelection(user.id)" />
                  </label>
                </th>
                <td class="text-base-content/60">{{ index + 1 }}</td>
                <td class="font-medium">{{ user.username }}</td>
                <td>
                  <div class="avatar placeholder">
                    <div v-if="user.avatar" class="w-8 h-8 rounded-lg ring-1 ring-base-300">
                      <img :src="user.avatar" alt="Avatar" />
                    </div>
                    <div v-else class="bg-primary text-primary-content rounded-lg w-8 h-8">
                      <span class="text-sm">{{ getAvatarText(user.nickname || user.username) }}</span>
                    </div>
                  </div>
                </td>
                <td>{{ user.nickname }}</td>
                <td class="text-base-content/60">{{ user.email || '-' }}</td>
                <td class="text-base-content/60">{{ user.phone || '-' }}</td>
                <td>
                  <span class="badge badge-sm badge-primary">
                    {{ user.roles?.[0] || '未分配' }}
                  </span>
                </td>
                <td>
                  <span class="badge badge-sm badge-ghost">{{ getSexLabel(user.sex) }}</span>
                </td>
                <td>
                  <input type="checkbox" class="toggle toggle-primary toggle-sm" :checked="user.status === 0" :disabled="user.username === 'admin'" @change="handleStatusChange(user)" />
                </td>
                <td class="text-sm text-base-content/60">{{ user.loginDate ? formatDate(user.loginDate) : '-' }}</td>
                <td>
                  <div class="flex justify-center gap-2">
                    <button v-if="userStore.hasPermission('system:user:edit') && user.username !== 'admin'" class="btn btn-square btn-xs bg-blue-50 text-blue-600 border-none hover:bg-blue-100" @click="handleEditSingle(user)" title="编辑">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                    </button>
                    <button v-if="userStore.hasPermission('system:user:ban') && user.username !== 'admin'" class="btn btn-square btn-xs bg-red-50 text-red-600 border-none hover:bg-red-100" @click="handleBan(user)" title="封禁">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
                      </svg>
                    </button>
                    <button v-if="userStore.hasPermission('system:user:role') && user.username !== 'admin'" class="btn btn-square btn-xs bg-amber-50 text-amber-600 border-none hover:bg-amber-100" @click="openRoleModal(user)" title="分配角色">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                         <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        
        <!-- Pagination -->
        <div class="flex justify-between items-center mt-6 border-t border-base-200 pt-4 flex-none">
          <div class="text-sm text-base-content/60">
            共 {{ total }} 条
          </div>
          <div class="join">
            <button class="join-item btn btn-sm btn-ghost" :disabled="queryParams.page <= 1" @click="changePage(queryParams.page - 1)">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <button v-for="p in pageNumbers" :key="p" 
              class="join-item btn btn-sm" 
              :class="p === queryParams.page ? 'btn-primary' : 'btn-ghost'"
              @click="changePage(p as number)"
            >
              {{ p }}
            </button>
            <button class="join-item btn btn-sm btn-ghost" :disabled="userList.length < queryParams.size" @click="changePage(queryParams.page + 1)">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Modals -->
    <UserFormModal ref="userFormModalRef" :role-list="allRoles" @success="fetchData" />
    <RoleAssignModal ref="roleAssignModalRef" :role-list="allRoles" @success="fetchData" />
    <DeleteConfirmModal ref="deleteConfirmModalRef" @confirm="confirmDelete" />
    <ImportModal ref="importModalRef" @success="fetchData" />

    <!-- Ban Reason Modal -->
    <dialog class="modal" :class="{ 'modal-open': showBanModal }">
      <div class="modal-box">
        <h3 class="font-bold text-lg text-error">封禁用户</h3>
        <p class="py-2 text-base-content/70">确定要封禁用户 <strong>{{ banTargetUser?.username }}</strong> 吗？</p>
        <div class="form-control">
          <label class="label"><span class="label-text">封禁理由 <span class="text-error">*</span></span></label>
          <textarea v-model="banReason" class="textarea textarea-bordered h-24" placeholder="请填写封禁理由（必填）"></textarea>
        </div>
        <div class="modal-action">
          <button class="btn btn-ghost" @click="showBanModal = false">取消</button>
          <button class="btn btn-error" @click="confirmBan">确认封禁</button>
        </div>
      </div>
      <form method="dialog" class="modal-backdrop">
        <button @click="showBanModal = false">close</button>
      </form>
    </dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { getUserList, banUser, getRoleList, deleteUsers, exportUsers } from '@/api/system'
import type { UserVO, RoleVO } from '@/api/system'
import { useUserStore } from '@/stores/user'
import UserFormModal from './components/UserFormModal.vue'
import RoleAssignModal from './components/RoleAssignModal.vue'
import DeleteConfirmModal from './components/DeleteConfirmModal.vue'
import ImportModal from './components/ImportModal.vue'

const userStore = useUserStore()
const currentUserId = computed(() => userStore.userInfo?.id)

const loading = ref(false)
const showBanModal = ref(false)
const banTargetUser = ref<UserVO | null>(null)
const banReason = ref('')
const showSearch = ref(false)
const userList = ref<UserVO[]>([])
const allRoles = ref<RoleVO[]>([])
const total = ref(0)

const queryParams = reactive({
  page: 1,
  size: 10,
  username: '',
  nickname: '',
  phone: '',
  loginDateStart: '',
  loginDateEnd: ''
})

const selectedIds = ref<number[]>([])

const userFormModalRef = ref<InstanceType<typeof UserFormModal>>()
const roleAssignModalRef = ref<InstanceType<typeof RoleAssignModal>>()
const deleteConfirmModalRef = ref<InstanceType<typeof DeleteConfirmModal>>()
const importModalRef = ref<InstanceType<typeof ImportModal>>()

const isAllSelected = computed(() => {
  return userList.value.length > 0 && selectedIds.value.length === userList.value.length
})

const hasSelection = computed(() => selectedIds.value.length > 0)

const pageNumbers = computed(() => {
  const pages = []
  const current = queryParams.page
  if (current > 1) pages.push(current - 1)
  pages.push(current)
  if (userList.value.length >= queryParams.size) pages.push(current + 1)
  return pages
})

onMounted(() => {
  fetchData()
  fetchRoles()
})

const fetchData = async () => {
  loading.value = true
  selectedIds.value = []
  try {
    const res: any = await getUserList(queryParams)
    userList.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const fetchRoles = async () => {
  try {
    const res: any = await getRoleList()
    allRoles.value = res || []
  } catch (error) {
    console.error(error)
  }
}

const handleSearch = () => {
  queryParams.page = 1
  fetchData()
}

const resetSearch = () => {
  queryParams.username = ''
  queryParams.nickname = ''
  queryParams.phone = ''
  queryParams.loginDateStart = ''
  queryParams.loginDateEnd = ''
  queryParams.page = 1
  fetchData()
}

const changePage = (page: number) => {
  queryParams.page = page
  fetchData()
}

const toggleSelection = (id: number) => {
  if (selectedIds.value.includes(id)) {
    selectedIds.value = selectedIds.value.filter(i => i !== id)
  } else {
    selectedIds.value.push(id)
  }
}

const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedIds.value = []
  } else {
    selectedIds.value = userList.value.map(u => u.id)
  }
}

const handleStatusChange = async (user: UserVO) => {
  // 禁止操作自己
  if (user.id === currentUserId.value) {
    alert('不能操作自己的账号')
    return
  }
  // 禁止操作 admin
  if (user.username === 'admin') {
    alert('不能操作管理员账号')
    return
  }

  const newStatus = user.status === 0 ? 1 : 0
  
  if (newStatus === 1) {
    // 封禁需要填写理由
    banTargetUser.value = user
    banReason.value = ''
    showBanModal.value = true
  } else {
    // 解封直接确认
    if (!confirm(`确定要解封用户 ${user.username} 吗？`)) {
      return
    }
    try {
      await banUser(user.id, 0)
      user.status = 0
    } catch (error) {
      console.error(error)
      fetchData()
    }
  }
}

const confirmBan = async () => {
  if (!banReason.value.trim()) {
    alert('请填写封禁理由')
    return
  }
  if (!banTargetUser.value) return
  
  try {
    await banUser(banTargetUser.value.id, 1, banReason.value)
    banTargetUser.value.status = 1
    showBanModal.value = false
    fetchData()
  } catch (error) {
    console.error(error)
  }
}

const handleAdd = () => {
  userFormModalRef.value?.open()
}

const handleEdit = () => {
  const user = userList.value.find(u => u.id === selectedIds.value[0])
  if (user) {
    userFormModalRef.value?.open(user)
  }
}

const handleEditSingle = (user: UserVO) => {
  userFormModalRef.value?.open(user)
}

const handleDelete = () => {
  const names = userList.value
    .filter(u => selectedIds.value.includes(u.id))
    .map(u => u.nickname || u.username)
  deleteConfirmModalRef.value?.open(selectedIds.value, names)
}

const confirmDelete = async (ids: number[], reason: string) => {
  try {
    await deleteUsers(ids, reason)
    fetchData()
  } catch (error: any) {
    console.error(error)
    alert(error?.response?.data?.msg || '删除失败')
  }
}

const handleBatchAssignRole = () => {
  const users = userList.value.filter(u => selectedIds.value.includes(u.id))
  if (users.length > 0) {
    roleAssignModalRef.value?.openBatch(selectedIds.value, users.map(u => u.nickname || u.username))
  }
}

const handleExport = async () => {
  try {
    const res = await exportUsers(queryParams)
    const blob = new Blob([res as unknown as BlobPart], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '用户列表.xlsx'
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    console.error(error)
    alert('导出失败')
  }
}

const handleImport = () => {
  importModalRef.value?.open()
}

const openRoleModal = (user: UserVO) => {
  roleAssignModalRef.value?.open(user)
}

const handleBan = (user: UserVO) => {
  // 禁止操作自己
  if (user.id === currentUserId.value) {
    alert('不能操作自己的账号')
    return
  }
  handleStatusChange(user)
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

const getSexLabel = (sex?: number) => {
  if (sex === 1) return '男'
  if (sex === 2) return '女'
  return '未知'
}

const getAvatarText = (name: string) => {
  if (!name) return '?'
  return name.charAt(0).toUpperCase()
}
</script>
