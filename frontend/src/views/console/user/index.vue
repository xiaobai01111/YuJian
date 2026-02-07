<template>
  <div class="h-full flex flex-col min-h-0">
    <div class="card bg-base-100 shadow-sm border border-base-200 flex-1 min-h-0 flex flex-col">
      <div class="card-body p-4 flex-1 min-h-0 flex flex-col overflow-hidden">
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
        <div class="mb-4 flex-none">
          <div class="rounded-xl border border-base-200 bg-base-100/90 p-3 shadow-sm">
            <div class="flex flex-wrap items-center gap-3">
              <div class="tabs tabs-boxed bg-base-200/70 p-1">
                <button class="tab tab-sm px-4" :class="{ 'tab-active': viewMode === 'active' }" @click="switchView('active')">正常用户</button>
                <button class="tab tab-sm px-4" :class="{ 'tab-active': viewMode === 'deleted' }" @click="switchView('deleted')">已删除用户</button>
              </div>
              <span class="text-xs text-base-content/50">当前视图：{{ viewMode === 'active' ? '正常用户' : '已删除用户' }}</span>

              <div class="ml-auto flex flex-wrap items-center gap-2">
                <button v-if="viewMode === 'active' && userStore.hasPermission('system:user:add')" class="btn btn-primary btn-sm gap-2 font-normal" @click="handleAdd">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
              </svg>
              添加
            </button>
                <button v-if="viewMode === 'active' && userStore.hasPermission('system:user:edit')" class="btn btn-success btn-sm text-white gap-2 font-normal" :disabled="selectedIds.length !== 1" @click="handleEdit">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
              修改
            </button>
                <button v-if="viewMode === 'active' && userStore.hasPermission('system:user:delete')" class="btn btn-error btn-sm text-white gap-2 font-normal" :disabled="!hasSelection" @click="handleDelete">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
              删除
            </button>
                <button v-if="viewMode === 'deleted' && userStore.hasPermission('system:user:restore')" class="btn btn-success btn-sm text-white gap-2 font-normal" :disabled="!hasSelection" @click="handleRestoreSelected">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              恢复
            </button>
                <button v-if="viewMode === 'deleted' && userStore.hasPermission('system:user:purge')" class="btn btn-error btn-sm text-white gap-2 font-normal" :disabled="!hasSelection" @click="handlePurgeSelected">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
              彻底删除
            </button>
                <button v-if="viewMode === 'active' && userStore.hasPermission('system:user:role')" class="btn btn-info btn-sm text-white gap-2 font-normal" :disabled="selectedIds.length === 0" @click="handleBatchAssignRole">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
              批量分配角色
            </button>
                <button v-if="viewMode === 'active' && userStore.hasPermission('system:user:export')" class="btn btn-warning btn-sm text-white gap-2 font-normal" @click="handleExport">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
              </svg>
              导出
            </button>
                <button v-if="viewMode === 'active' && userStore.hasPermission('system:user:import')" class="btn btn-ghost btn-sm border-base-300 gap-2 bg-base-200 font-normal" @click="handleImport">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m4-8l-4-4m0 0L8 8m4-4v12" />
              </svg>
              导入
            </button>
                <span class="mx-1 h-5 w-px bg-base-300"></span>
                <button class="btn btn-circle btn-ghost btn-sm" @click="refreshList" title="刷新列表">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-base-content/60" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
            </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Table -->
        <div ref="scrollContainer" class="overflow-auto flex-1 min-h-0">
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
                <td colspan="12" class="text-center py-10 text-base-content/60">{{ viewMode === 'deleted' ? '暂无已删除用户' : '暂无数据' }}</td>
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
                  <div class="flex items-center gap-2">
                    <template v-if="viewMode === 'active'">
                      <input type="checkbox" class="toggle toggle-primary toggle-sm" :checked="user.status === 0" :disabled="isStatusActionDisabled(user)" @change="handleStatusChange(user)" />
                      <span v-if="user.status === 1" class="badge badge-error badge-sm">已封禁</span>
                      <span v-else class="badge badge-success badge-sm">正常</span>
                    </template>
                    <span v-else class="badge badge-neutral badge-sm">已删除</span>
                  </div>
                </td>
                <td class="text-sm text-base-content/60">{{ user.loginDate ? formatDate(user.loginDate) : '-' }}</td>
                <td>
                  <div class="flex justify-center gap-2">
                    <button v-if="viewMode === 'active' && userStore.hasPermission('system:user:edit')" class="btn btn-square btn-xs bg-blue-50 text-blue-600 border-none hover:bg-blue-100" @click="handleEditSingle(user)" title="编辑">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                    </button>
                    <button v-if="viewMode === 'active' && userStore.hasPermission('system:user:ban') && user.userType !== 1 && user.status === 0" class="btn btn-square btn-xs bg-red-50 text-red-600 border-none hover:bg-red-100" @click="handleBan(user)" title="封禁">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
                      </svg>
                    </button>
                    <button v-if="viewMode === 'active' && userStore.hasPermission('system:user:unban') && user.userType !== 1 && user.status === 1" class="btn btn-square btn-xs bg-green-50 text-green-600 border-none hover:bg-green-100" @click="handleUnban(user)" title="解封">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                      </svg>
                    </button>
                    <button v-if="viewMode === 'active' && userStore.hasPermission('system:user:role') && user.userType !== 1" class="btn btn-square btn-xs bg-amber-50 text-amber-600 border-none hover:bg-amber-100" @click="openRoleModal(user)" title="分配角色">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                         <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
                      </svg>
                    </button>
                    <button v-if="viewMode === 'deleted' && userStore.hasPermission('system:user:restore')" class="btn btn-square btn-xs bg-green-50 text-green-600 border-none hover:bg-green-100" @click="handleRestoreSingle(user)" title="恢复">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                      </svg>
                    </button>
                    <button v-if="viewMode === 'deleted' && userStore.hasPermission('system:user:purge')" class="btn btn-square btn-xs bg-red-50 text-red-600 border-none hover:bg-red-100" @click="handlePurgeSingle(user)" title="彻底删除">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <div ref="loadMoreTrigger" class="py-3 text-center text-xs text-base-content/50">
            <span v-if="loadingMore">加载中...</span>
            <span v-else-if="!hasMore && userList.length > 0">已加载完毕</span>
          </div>
        </div>
        
        <div class="flex justify-between items-center mt-4 border-t border-base-200 pt-3 flex-none text-sm text-base-content/60">
          <div>已加载 {{ userList.length }} / {{ total || '-' }} 条</div>
          <div v-if="loadingMore">正在加载更多...</div>
        </div>
      </div>
    </div>

    <!-- Modals -->
    <UserFormModal ref="userFormModalRef" :role-list="allRoles" @success="refreshList" />
    <RoleAssignModal ref="roleAssignModalRef" :role-list="allRoles" @success="refreshList" />
    <DeleteConfirmModal ref="deleteConfirmModalRef" @confirm="confirmDelete" />
    <ImportModal ref="importModalRef" @success="refreshList" />


    <!-- Ban Reason Modal -->
    <dialog class="modal" :class="{ 'modal-open': showBanModal }">
      <div class="modal-box">
        <h3 class="font-bold text-lg text-error">封禁用户</h3>
        <p class="py-2 text-base-content/70">确定要封禁用户 <strong>{{ banTargetUser?.username }}</strong> 吗？</p>
        <div class="form-control">
          <label class="label">
            <span class="label-text">封禁理由 <span class="text-error">*</span></span>
            <span class="label-text-alt">{{ banReason.length }}/200</span>
          </label>
          <textarea v-model="banReason" class="textarea textarea-bordered h-24" :class="{ 'textarea-error': banReason.length > 200 || (banReasonTouched && banReason.trim().length < 2) }" maxlength="200" placeholder="请填写封禁理由（2-200字）" @blur="banReasonTouched = true"></textarea>
          <label class="label" v-if="banReasonTouched && banReason.trim().length < 2">
            <span class="label-text-alt text-error">封禁理由至少2个字符</span>
          </label>
        </div>
        <div class="modal-action">
          <button class="btn btn-ghost" @click="closeBanModal">取消</button>
          <button class="btn btn-error" :disabled="banReason.trim().length < 2 || banReason.length > 200" @click="confirmBan">确认封禁</button>
        </div>
      </div>
      <form method="dialog" class="modal-backdrop">
        <button @click="closeBanModal">close</button>
      </form>
    </dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, onUnmounted, nextTick } from 'vue'
import { getUserList, getDeletedUserList, banUser, unbanUser, restoreUser, purgeUser, getRoleList, deleteUsers, exportUsers } from '@/api/system'
import type { UserVO, RoleVO, DeletedUserQuery } from '@/api/system'
import { useUserStore } from '@/stores/user'
import { useDialog } from '@/composables/useDialog'
import UserFormModal from './components/UserFormModal.vue'
import RoleAssignModal from './components/RoleAssignModal.vue'
import DeleteConfirmModal from './components/DeleteConfirmModal.vue'
import ImportModal from './components/ImportModal.vue'

const userStore = useUserStore()
const dialog = useDialog()
const currentUserId = computed(() => userStore.userInfo?.id)

const loading = ref(false)
const showBanModal = ref(false)
const banTargetUser = ref<UserVO | null>(null)
const banReason = ref('')
const banReasonTouched = ref(false)
const showSearch = ref(false)
const userList = ref<UserVO[]>([])
const allRoles = ref<RoleVO[]>([])
const total = ref(0)
const viewMode = ref<'active' | 'deleted'>('active')
const loadingMore = ref(false)
const hasMore = ref(true)
const scrollContainer = ref<HTMLElement | null>(null)
const loadMoreTrigger = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

const queryParams = reactive({
  page: 1,
  size: 20,
  username: '',
  nickname: '',
  phone: '',
  loginDateStart: '',
  loginDateEnd: ''
})

const deletedCursor = reactive({
  lastDeletedAt: undefined as string | undefined,
  lastId: undefined as number | undefined
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
const canBan = computed(() => userStore.hasPermission('system:user:ban'))
const canUnban = computed(() => userStore.hasPermission('system:user:unban'))

onMounted(() => {
  fetchData({ reset: true })
  fetchRoles()
  nextTick(() => setupObserver())
})

onUnmounted(() => {
  observer?.disconnect()
  observer = null
})

const setupObserver = () => {
  if (!scrollContainer.value || !loadMoreTrigger.value) return
  observer?.disconnect()
  observer = new IntersectionObserver(
    entries => {
      if (entries[0]?.isIntersecting) {
        void loadMore()
      }
    },
    {
      root: scrollContainer.value,
      rootMargin: '200px 0px',
      threshold: 0
    }
  )
  observer.observe(loadMoreTrigger.value)
}

const resetDeletedCursor = () => {
  deletedCursor.lastDeletedAt = undefined
  deletedCursor.lastId = undefined
}

const buildDeletedQuery = (): DeletedUserQuery => {
  const params: DeletedUserQuery = {
    size: queryParams.size
  }
  if (queryParams.username) params.username = queryParams.username
  if (queryParams.nickname) params.nickname = queryParams.nickname
  if (queryParams.phone) params.phone = queryParams.phone
  if (queryParams.loginDateStart) params.loginDateStart = queryParams.loginDateStart
  if (queryParams.loginDateEnd) params.loginDateEnd = queryParams.loginDateEnd
  if (deletedCursor.lastDeletedAt) params.lastDeletedAt = deletedCursor.lastDeletedAt
  if (deletedCursor.lastId) params.lastId = deletedCursor.lastId
  return params
}

const fetchData = async ({ append = false, reset = false } = {}) => {
  if (append && (loadingMore.value || loading.value)) return
  if (!append && loading.value) return
  if (reset) {
    queryParams.page = 1
    userList.value = []
    selectedIds.value = []
    hasMore.value = true
    if (viewMode.value === 'deleted') {
      resetDeletedCursor()
    }
  }
  append ? (loadingMore.value = true) : (loading.value = true)
  try {
    if (viewMode.value === 'active') {
      const res = await getUserList(queryParams)
      const records = res.records || []
      total.value = res.total || 0
      if (append) {
        userList.value = [...userList.value, ...records]
      } else {
        userList.value = records
      }
      if (total.value) {
        hasMore.value = userList.value.length < total.value
      } else {
        hasMore.value = records.length >= queryParams.size
      }
    } else {
      const res = await getDeletedUserList(buildDeletedQuery())
      const records = res.records || []
      total.value = res.total || 0
      if (append) {
        userList.value = [...userList.value, ...records]
      } else {
        userList.value = records
      }
      const lastRecord = records[records.length - 1]
      if (lastRecord) {
        deletedCursor.lastDeletedAt = lastRecord.deletedAt
        deletedCursor.lastId = lastRecord.id
      }
      hasMore.value = records.length >= queryParams.size
    }
  } catch (error) {
    console.error(error)
  } finally {
    append ? (loadingMore.value = false) : (loading.value = false)
  }
}

const refreshList = () => {
  fetchData({ reset: true })
}

const switchView = (mode: 'active' | 'deleted') => {
  if (viewMode.value === mode) return
  viewMode.value = mode
  fetchData({ reset: true })
}

const loadMore = async () => {
  if (!hasMore.value || loading.value || loadingMore.value) return
  if (viewMode.value === 'active') {
    queryParams.page += 1
    await fetchData({ append: true })
    return
  }
  if (!deletedCursor.lastDeletedAt || !deletedCursor.lastId) {
    hasMore.value = false
    return
  }
  await fetchData({ append: true })
}

const fetchRoles = async () => {
  try {
    const res = await getRoleList()
    allRoles.value = Array.isArray(res) ? res : (res.records || [])
  } catch (error) {
    console.error(error)
  }
}

const handleSearch = () => {
  fetchData({ reset: true })
}

const resetSearch = () => {
  queryParams.username = ''
  queryParams.nickname = ''
  queryParams.phone = ''
  queryParams.loginDateStart = ''
  queryParams.loginDateEnd = ''
  fetchData({ reset: true })
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
  if (viewMode.value !== 'active') return

  // 禁止操作系统管理员账号
  if (user.userType === 1) {
    await dialog.alert('系统管理员账号不允许关停或封禁')
    return
  }
  // 禁止操作自己
  if (user.id === currentUserId.value) {
    await dialog.alert('不能操作自己的账号')
    return
  }

  const newStatus = user.status === 0 ? 1 : 0
  
  if (newStatus === 1) {
    if (!canBan.value) {
      await dialog.alert('缺少封禁权限')
      return
    }
    // 封禁需要填写理由
    banTargetUser.value = user
    banReason.value = ''
    banReasonTouched.value = false
    showBanModal.value = true
  } else {
    if (!canUnban.value) {
      await dialog.alert('缺少解封权限')
      return
    }
    // 解封直接确认
    if (!await dialog.confirm(`确定要解封用户 ${user.username} 吗？`)) {
      return
    }
    try {
      await unbanUser(user.id)
      user.status = 0
    } catch (error) {
      console.error(error)
      refreshList()
    }
  }
}

const closeBanModal = () => {
  showBanModal.value = false
  banReason.value = ''
  banReasonTouched.value = false
  banTargetUser.value = null
}

const confirmBan = async () => {
  if (banReason.value.trim().length < 2) {
    banReasonTouched.value = true
    return
  }
  if (!banTargetUser.value) return
  
  try {
    const res = await banUser(banTargetUser.value.id, banReason.value)
    // 使用返回的最新用户状态更新列表
    if (res) {
      const idx = userList.value.findIndex(u => u.id === banTargetUser.value?.id)
      if (idx !== -1) {
        userList.value[idx] = res
      }
    }
    closeBanModal()
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
  if (viewMode.value !== 'active') return
  const names = userList.value
    .filter(u => selectedIds.value.includes(u.id))
    .map(u => u.nickname || u.username)
  deleteConfirmModalRef.value?.open(selectedIds.value, names)
}

const confirmDelete = async (ids: number[], reason: string) => {
  try {
    await deleteUsers(ids, reason)
    fetchData({ reset: true })
  } catch (error: unknown) {
    console.error(error)
    await dialog.alert((error as ApiErrorLike)?.response?.data?.msg || '删除失败')
  }
}

const handleBatchAssignRole = () => {
  if (viewMode.value !== 'active') return
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
    await dialog.alert('导出失败')
  }
}

const handleImport = () => {
  importModalRef.value?.open()
}

const openRoleModal = (user: UserVO) => {
  roleAssignModalRef.value?.open(user)
}

const handleBan = (user: UserVO) => {
  // 禁止操作系统管理员账号
  if (user.userType === 1) {
    void dialog.alert('系统管理员账号不允许关停或封禁')
    return
  }
  // 禁止操作自己
  if (user.id === currentUserId.value) {
    void dialog.alert('不能操作自己的账号')
    return
  }
  handleStatusChange(user)
}

const handleUnban = async (user: UserVO) => {
  if (viewMode.value !== 'active') return
  await handleStatusChange(user)
}

const isStatusActionDisabled = (user: UserVO) => {
  if (user.userType === 1 || user.id === currentUserId.value) return true
  if (viewMode.value !== 'active') return true
  return user.status === 0 ? !canBan.value : !canUnban.value
}

const handleRestoreUsers = async (ids: number[]) => {
  const reason = await dialog.prompt('请输入恢复理由（可选）', {
    title: '恢复用户',
    placeholder: '可留空',
    required: false
  })
  if (reason === null) return
  try {
    await Promise.all(ids.map(id => restoreUser(id, reason || undefined)))
    fetchData({ reset: true })
  } catch (error) {
    console.error(error)
    await dialog.alert((error as ApiErrorLike)?.response?.data?.msg || '恢复失败')
  }
}

const handlePurgeUsers = async (ids: number[]) => {
  const confirmed = await dialog.confirm('彻底删除后不可恢复，确认继续？', {
    title: '危险操作',
    confirmText: '继续删除'
  })
  if (!confirmed) return
  const reason = await dialog.prompt('请输入彻底删除理由（可选）', {
    title: '彻底删除用户',
    placeholder: '可留空',
    required: false
  })
  if (reason === null) return
  try {
    await Promise.all(ids.map(id => purgeUser(id, reason || undefined)))
    fetchData({ reset: true })
  } catch (error) {
    console.error(error)
    await dialog.alert((error as ApiErrorLike)?.response?.data?.msg || '彻底删除失败')
  }
}

const handleRestoreSelected = async () => {
  if (!selectedIds.value.length) return
  await handleRestoreUsers([...selectedIds.value])
}

const handlePurgeSelected = async () => {
  if (!selectedIds.value.length) return
  await handlePurgeUsers([...selectedIds.value])
}

const handleRestoreSingle = async (user: UserVO) => {
  await handleRestoreUsers([user.id])
}

const handlePurgeSingle = async (user: UserVO) => {
  await handlePurgeUsers([user.id])
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
