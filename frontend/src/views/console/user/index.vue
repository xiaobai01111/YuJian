<template>
  <div class="card bg-base-100 shadow-xl">
    <div class="card-body">
      <div class="flex justify-between items-center mb-4">
        <h2 class="card-title">用户管理</h2>
        <div class="join">
          <input 
            v-model="queryParams.username" 
            class="input input-bordered join-item" 
            placeholder="搜索用户名..." 
            @keyup.enter="handleSearch"
          />
          <button class="btn btn-primary join-item" @click="handleSearch" :disabled="loading">搜索</button>
        </div>
      </div>

      <div class="overflow-x-auto min-h-[400px]">
        <table class="table table-zebra">
          <thead>
            <tr>
              <th>ID</th>
              <th>用户名</th>
              <th>昵称</th>
              <th>角色</th>
              <th>状态</th>
              <th>注册时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="7" class="text-center py-4">加载中...</td>
            </tr>
            <tr v-else-if="userList.length === 0">
              <td colspan="7" class="text-center py-4">暂无数据</td>
            </tr>
            <tr v-else v-for="user in userList" :key="user.id">
              <td>{{ user.id }}</td>
              <td>{{ user.username }}</td>
              <td>{{ user.nickname }}</td>
              <td>
                <div class="flex gap-1 flex-wrap">
                   <span v-for="role in user.roles" :key="role" class="badge badge-ghost badge-sm">{{ role }}</span>
                </div>
              </td>
              <td>
                <span :class="['badge', user.status === 0 ? 'badge-success' : 'badge-error']">
                  {{ user.status === 0 ? '正常' : '封禁' }}
                </span>
              </td>
              <td class="text-sm text-slate-500">{{ formatDate(user.createTime) }}</td>
              <td>
                <div class="flex gap-2">
                  <button class="btn btn-ghost btn-xs text-primary" @click="openRoleModal(user)" v-permission="['system:user:role']">分配角色</button>
                  <button 
                    v-if="user.status === 0"
                    class="btn btn-ghost btn-xs text-error" 
                    @click="handleBan(user)" 
                    v-permission="['system:user:ban']"
                  >
                    封禁
                  </button>
                  <button 
                    v-else
                    class="btn btn-ghost btn-xs text-success" 
                    @click="handleUnban(user)" 
                    v-permission="['system:user:ban']"
                  >
                    解封
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <!-- Pagination -->
      <div class="flex justify-end mt-4">
        <div class="join">
          <button class="join-item btn" :disabled="queryParams.page <= 1" @click="changePage(queryParams.page - 1)">«</button>
          <button class="join-item btn">Page {{ queryParams.page }}</button>
          <button class="join-item btn" :disabled="userList.length < queryParams.size" @click="changePage(queryParams.page + 1)">»</button>
        </div>
      </div>
    </div>

    <!-- Role Assignment Modal -->
    <dialog id="role_modal" class="modal">
      <div class="modal-box">
        <h3 class="font-bold text-lg">分配角色</h3>
        <p class="py-4">为用户 <span class="font-bold">{{ currentUser?.username }}</span> 分配角色</p>
        
        <div class="form-control">
          <label class="label cursor-pointer justify-start gap-4" v-for="role in allRoles" :key="role.id">
            <input type="checkbox" class="checkbox" :value="role.id" v-model="selectedRoleIds" />
            <span class="label-text">{{ role.roleName }} ({{ role.roleKey }})</span>
          </label>
        </div>

        <div class="modal-action">
          <form method="dialog">
            <button class="btn btn-ghost">取消</button>
            <button class="btn btn-primary ml-2" @click.prevent="submitRoleAssign" :disabled="submitting">确定</button>
          </form>
        </div>
      </div>
    </dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getUserList, banUser, updateUserRole, getRoleList } from '@/api/system'
import type { UserVO, RoleVO } from '@/api/system'

const loading = ref(false)
const submitting = ref(false)
const userList = ref<UserVO[]>([])
const allRoles = ref<RoleVO[]>([])

const queryParams = reactive({
  page: 1,
  size: 10,
  username: ''
})

const currentUser = ref<UserVO | null>(null)
const selectedRoleIds = ref<number[]>([])

onMounted(() => {
  fetchData()
  fetchRoles()
})

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await getUserList(queryParams)
    userList.value = res.rows || []
    // If backend uses standard PageResult, adjust accordingly. 
    // Assuming res is PageResult<UserVO> { rows: [], total: 0 } based on typical Mybatis-Plus page
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

const handleBan = async (user: UserVO) => {
  if (!confirm(`确定要封禁用户 ${user.username} 吗？`)) return
  try {
    await banUser(user.id, 1) // 1 = ban
    user.status = 1
  } catch (error) {
    console.error(error)
  }
}

const handleUnban = async (user: UserVO) => {
  if (!confirm(`确定要解封用户 ${user.username} 吗？`)) return
  try {
    await banUser(user.id, 0) // 0 = normal
    user.status = 0
  } catch (error) {
    console.error(error)
  }
}

const openRoleModal = (user: UserVO) => {
  currentUser.value = user
  // Map current user roles (string keys) to IDs is tricky if we don't have the mapping.
  // Ideally UserVO should return roleIds or we map them back from allRoles.
  // For simplicity, let's assume we map by roleKey.
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
