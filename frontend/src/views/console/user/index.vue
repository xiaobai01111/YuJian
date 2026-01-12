<template>
  <div class="p-4">
    <div class="card bg-base-100 shadow-sm border border-base-200">
      <div class="card-body p-4">
        <!-- Toolbar -->
        <div class="flex flex-wrap justify-between items-center mb-4 gap-4">
          <div class="flex flex-wrap gap-2">
            <button class="btn btn-primary btn-sm gap-2" @click="() => {}">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
              </svg>
              添加
            </button>
            <button class="btn btn-success btn-sm text-white gap-2" :disabled="!hasSelection" @click="() => {}">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
              修改
            </button>
            <button class="btn btn-error btn-sm text-white gap-2" :disabled="!hasSelection" @click="() => {}">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
              删除
            </button>
            <button class="btn btn-info btn-sm text-white gap-2" :disabled="!hasSelection" @click="openRoleModal(selectedUsers[0])">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
              分配角色
            </button>
            <button class="btn btn-warning btn-sm text-white gap-2">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
              </svg>
              导出
            </button>
          </div>
          
          <div class="flex gap-2">
            <div class="dropdown dropdown-end">
              <div tabindex="0" role="button" class="btn btn-circle btn-ghost btn-sm">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </div>
              <div tabindex="0" class="dropdown-content z-[1] menu p-4 shadow bg-base-100 rounded-box w-64">
                <div class="form-control">
                  <input type="text" v-model="queryParams.username" placeholder="搜索用户名..." class="input input-bordered input-sm w-full" @keyup.enter="handleSearch" />
                </div>
              </div>
            </div>
            <button class="btn btn-circle btn-ghost btn-sm" @click="fetchData">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
            </button>
          </div>
        </div>

        <!-- Table -->
        <div class="overflow-x-auto min-h-[500px]">
          <table class="table table-md">
            <thead class="bg-base-200/50">
              <tr>
                <th>
                  <label>
                    <input type="checkbox" class="checkbox checkbox-sm" :checked="isAllSelected" @change="toggleSelectAll" />
                  </label>
                </th>
                <th>序号</th>
                <th>登录账号</th>
                <th>头像</th>
                <th>用户名称</th>
                <th>邮箱</th>
                <th>角色</th>
                <th>用户状态</th>
                <th>创建时间</th>
                <th class="text-center">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="10" class="text-center py-10">
                  <span class="loading loading-spinner loading-lg text-primary"></span>
                </td>
              </tr>
              <tr v-else-if="userList.length === 0">
                <td colspan="10" class="text-center py-10 text-base-content/60">暂无数据</td>
              </tr>
              <tr v-else v-for="(user, index) in userList" :key="user.id" class="hover">
                <th>
                  <label>
                    <input type="checkbox" class="checkbox checkbox-sm" :checked="selectedIds.includes(user.id)" @change="toggleSelection(user.id)" />
                  </label>
                </th>
                <td>{{ index + 1 }}</td>
                <td>{{ user.username }}</td>
                <td>
                  <div class="avatar">
                    <div class="w-8 rounded-full">
                      <img :src="user.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + user.username" alt="Avatar" />
                    </div>
                  </div>
                </td>
                <td>{{ user.nickname }}</td>
                <td>{{ user.email || '-' }}</td>
                <td>
                  <div class="flex flex-wrap gap-1">
                     <span v-for="role in user.roles" :key="role" class="badge badge-primary badge-outline badge-sm">{{ role }}</span>
                  </div>
                </td>
                <td>
                  <input type="checkbox" class="toggle toggle-primary toggle-sm" :checked="user.status === 0" @change="handleStatusChange(user)" />
                </td>
                <td class="text-sm text-base-content/70">{{ formatDate(user.createTime) }}</td>
                <td>
                  <div class="flex justify-center gap-2">
                    <button class="btn btn-circle btn-xs btn-ghost text-primary bg-primary/10 hover:bg-primary/20" title="编辑">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                    </button>
                    <button class="btn btn-circle btn-xs btn-ghost text-error bg-error/10 hover:bg-error/20" @click="handleBan(user)" title="删除/封禁">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                    <button class="btn btn-circle btn-xs btn-ghost text-warning bg-warning/10 hover:bg-warning/20" @click="openRoleModal(user)" title="分配角色">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
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
        <div class="flex justify-between items-center mt-6 border-t border-base-200 pt-4">
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

    <!-- Role Assignment Modal -->
    <dialog id="role_modal" class="modal">
      <div class="modal-box">
        <h3 class="font-bold text-lg">分配角色</h3>
        <p class="py-4">为用户 <span class="font-bold text-primary">{{ currentUser?.username }}</span> 分配角色</p>
        
        <div class="form-control max-h-60 overflow-y-auto border border-base-200 rounded-lg p-2">
          <label class="label cursor-pointer justify-start gap-4 hover:bg-base-200 rounded px-2" v-for="role in allRoles" :key="role.id">
            <input type="checkbox" class="checkbox checkbox-primary checkbox-sm" :value="role.id" v-model="selectedRoleIds" />
            <span class="label-text">{{ role.roleName }} ({{ role.roleKey }})</span>
          </label>
        </div>

        <div class="modal-action">
          <form method="dialog">
            <button class="btn btn-ghost" @click="currentUser = null">取消</button>
            <button class="btn btn-primary ml-2" @click.prevent="submitRoleAssign" :disabled="submitting">确定</button>
          </form>
        </div>
      </div>
    </dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { getUserList, banUser, updateUserRole, getRoleList } from '@/api/system'
import type { UserVO, RoleVO } from '@/api/system'

const loading = ref(false)
const submitting = ref(false)
const userList = ref<UserVO[]>([])
const allRoles = ref<RoleVO[]>([])
const total = ref(0) // Mock total for now as getUserList might not return it in current simplified mock logic

const queryParams = reactive({
  page: 1,
  size: 10,
  username: ''
})

const currentUser = ref<UserVO | null>(null)
const selectedRoleIds = ref<number[]>([])
const selectedIds = ref<number[]>([])

const isAllSelected = computed(() => {
  return userList.value.length > 0 && selectedIds.value.length === userList.value.length
})

const hasSelection = computed(() => selectedIds.value.length > 0)
const selectedUsers = computed(() => userList.value.filter(u => selectedIds.value.includes(u.id)))

const pageNumbers = computed(() => {
  // Simple pagination logic, showing mostly 1 page for now or +/- 2 around current
  const pages = []
  const current = queryParams.page
  // Mock max page 5 for visual if we don't have total. If we have total, calculate.
  // Assuming infinite scroll style API or unknown total, just show current and next/prev logic
  // But let's show at least current
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
  selectedIds.value = [] // Reset selection on reload
  try {
    const res: any = await getUserList(queryParams)
    userList.value = res.rows || []
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
  const newStatus = user.status === 0 ? 1 : 0
  const actionName = newStatus === 1 ? '封禁' : '解封'
  
  if (!confirm(`确定要${actionName}用户 ${user.username} 吗？`)) {
    // Revert visual toggle if cancelled (needs force update or key change, simplest is fetch)
    // For now simple alert and we might not see immediate revert without refresh unless we use v-model. 
    // Using :checked prop so it should persist state unless we change it manually.
    // Ideally we should revert UI state.
    // Better to use v-model with a local copy or just refresh.
    fetchData() 
    return
  }

  try {
    await banUser(user.id, newStatus)
    user.status = newStatus
  } catch (error) {
    console.error(error)
    fetchData() // Revert on error
  }
}

const handleBan = (user: UserVO) => {
  handleStatusChange(user)
}

const openRoleModal = (user: UserVO) => {
  if (!user) return
  currentUser.value = user
  selectedRoleIds.value = allRoles.value
    .filter(r => user.roles.includes(r.roleKey))
    .map(r => r.id)
    
  const modal = document.getElementById('role_modal') as HTMLDialogElement
  modal.showModal()
}

const submitRoleAssign = async () => {
  if (!currentUser.value) return
  submitting.value = true
  try {
    await updateUserRole(currentUser.value.id, selectedRoleIds.value)
    // Update local state
    const roleKeys = allRoles.value
        .filter(r => selectedRoleIds.value.includes(r.id))
        .map(r => r.roleKey)
    currentUser.value.roles = roleKeys
    
    const modal = document.getElementById('role_modal') as HTMLDialogElement
    modal.close()
    currentUser.value = null
  } catch (error) {
    console.error(error)
  } finally {
    submitting.value = false
  }
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}
</script>
