<template>
  <div class="h-full flex flex-col overflow-hidden">
    <div class="flex-1 min-h-0 flex flex-col gap-6 overflow-hidden p-6">
      <!-- Header -->
      <div class="flex justify-between items-center">
        <div>
          <h1 class="text-2xl font-bold text-slate-800">认证规则</h1>
          <p class="text-slate-500 mt-1">注册/认证通过后自动分配角色与部门</p>
        </div>
        <button v-if="canAdd" class="btn btn-primary" @click="openCreateModal">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          新增规则
        </button>
      </div>

      <!-- Filters -->
      <div class="card bg-base-100 shadow-sm">
        <div class="card-body p-4">
          <div class="flex flex-wrap gap-4 items-center">
            <div class="form-control">
              <select v-model="filterTrigger" class="select select-bordered select-sm w-32" @change="loadRules">
                <option :value="''">全部触发</option>
                <option value="REGISTER">注册</option>
                <option value="VERIFY">认证通过</option>
              </select>
            </div>
            <div class="form-control">
              <select v-model="filterMethod" class="select select-bordered select-sm w-40" @change="loadRules">
                <option :value="''">全部方式</option>
                <option value="EDU_EMAIL">EDU邮箱</option>
                <option value="MANUAL">人工审核</option>
                <option value="SSO">SSO</option>
                <option value="ID_LIST">白名单</option>
                <option value="OCR">证件OCR</option>
              </select>
            </div>
            <div class="form-control">
              <select v-model="filterEnabled" class="select select-bordered select-sm w-32" @change="loadRules">
                <option :value="''">全部状态</option>
                <option :value="true">启用</option>
                <option :value="false">停用</option>
              </select>
            </div>
            <button class="btn btn-sm btn-ghost" @click="loadRules">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              搜索
            </button>
          </div>
        </div>
      </div>

      <!-- Table -->
      <div class="card bg-base-100 shadow-sm flex-1 min-h-0">
        <div class="card-body p-0 flex flex-col min-h-0">
          <div class="flex-1 overflow-auto">
            <div class="overflow-x-auto">
              <table class="table">
              <thead>
                <tr>
                  <th class="w-16">ID</th>
                  <th>规则</th>
                  <th class="w-24">触发</th>
                  <th class="w-28">方式</th>
                  <th>匹配</th>
                  <th class="w-40">角色</th>
                  <th class="w-28">部门</th>
                  <th class="w-20">状态</th>
                  <th class="w-16">优先级</th>
                  <th class="w-32">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="loading">
                  <td colspan="10" class="text-center py-8">
                    <span class="loading loading-spinner loading-md"></span>
                  </td>
                </tr>
                <tr v-else-if="rules.length === 0">
                  <td colspan="10" class="text-center py-8 text-slate-500">暂无数据</td>
                </tr>
                <tr v-for="rule in rules" :key="rule.id" class="hover">
                  <td>{{ rule.id }}</td>
                  <td class="font-medium">{{ rule.name }}</td>
                  <td>{{ triggerText(rule.triggerType) }}</td>
                  <td>{{ methodText(rule.verifyMethod) }}</td>
                  <td>
                    <div class="text-sm">
                      {{ matchText(rule.matchType, rule.matchValue) }}
                    </div>
                  </td>
                  <td>
                    <div class="flex flex-wrap gap-1">
                      <span v-for="role in rule.roleNames" :key="role" class="badge badge-ghost badge-sm">{{ role }}</span>
                      <span v-if="!rule.roleNames || rule.roleNames.length === 0" class="text-slate-400 text-xs">-</span>
                    </div>
                  </td>
                  <td>{{ rule.deptName || '-' }}</td>
                  <td>
                    <span :class="rule.enabled ? 'badge badge-success badge-sm' : 'badge badge-ghost badge-sm'">
                      {{ rule.enabled ? '启用' : '停用' }}
                    </span>
                  </td>
                  <td>{{ rule.priority ?? 100 }}</td>
                  <td>
                    <div class="flex gap-2">
                      <button v-if="canEdit" class="btn btn-xs btn-ghost" @click="openEditModal(rule)">编辑</button>
                      <button v-if="canDelete" class="btn btn-xs btn-ghost text-error" @click="handleDelete(rule)">删除</button>
                    </div>
                  </td>
                </tr>
              </tbody>
              </table>
            </div>
          </div>

          <!-- Pagination -->
          <div class="flex items-center justify-between p-4 border-t border-base-200">
            <div class="text-sm text-slate-500">共 {{ total }} 条记录</div>
            <div class="join">
              <button class="join-item btn btn-sm" :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">«</button>
              <button class="join-item btn btn-sm">第 {{ currentPage }} 页</button>
              <button class="join-item btn btn-sm" :disabled="currentPage * pageSize >= total" @click="changePage(currentPage + 1)">»</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Form Modal -->
    <dialog ref="formModal" class="modal">
      <div class="modal-box max-w-3xl">
        <h3 class="font-bold text-lg mb-4">{{ isEdit ? '编辑规则' : '新增规则' }}</h3>
        <div class="space-y-4">
          <div class="form-control">
            <label class="label"><span class="label-text font-medium">规则名称 *</span></label>
            <input v-model="form.name" type="text" class="input input-bordered" placeholder="例如：注册默认角色" />
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="form-control">
              <label class="label"><span class="label-text font-medium">触发类型 *</span></label>
              <select v-model="form.triggerType" class="select select-bordered">
                <option value="REGISTER">注册</option>
                <option value="VERIFY">认证通过</option>
              </select>
            </div>
            <div class="form-control">
              <label class="label"><span class="label-text font-medium">认证方式</span></label>
              <select v-model="form.verifyMethod" class="select select-bordered" :disabled="form.triggerType === 'REGISTER'">
                <option value="">不限</option>
                <option value="EDU_EMAIL">EDU邮箱</option>
                <option value="MANUAL">人工审核</option>
                <option value="SSO">SSO</option>
                <option value="ID_LIST">白名单</option>
                <option value="OCR">证件OCR</option>
              </select>
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="form-control">
              <label class="label"><span class="label-text font-medium">匹配方式</span></label>
              <select v-model="form.matchType" class="select select-bordered">
                <option value="ANY">任意</option>
                <option value="EMAIL_DOMAIN">邮箱域名</option>
                <option value="STUDENT_ID_PREFIX">学号前缀</option>
              </select>
            </div>
            <div class="form-control">
              <label class="label"><span class="label-text font-medium">匹配值</span></label>
              <input v-model="form.matchValue" type="text" class="input input-bordered" :disabled="form.matchType === 'ANY'"
                placeholder="多个用英文逗号分隔" />
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="form-control">
              <label class="label"><span class="label-text font-medium">分配部门</span></label>
              <select
                ref="deptSelectRef"
                v-model="form.deptId"
                class="select select-bordered max-h-60 overflow-auto"
                @scroll="handleDeptScroll"
              >
                <option :value="null">不分配</option>
                <option v-for="dept in deptOptionsVisible" :key="dept.id" :value="dept.id">{{ dept.name }}</option>
              </select>
            </div>
            <div class="form-control">
              <label class="label"><span class="label-text font-medium">优先级</span></label>
              <input v-model.number="form.priority" type="number" class="input input-bordered" />
            </div>
          </div>

          <div class="form-control">
            <label class="label"><span class="label-text font-medium">分配角色</span></label>
            <div class="border border-base-200 rounded-lg p-3 max-h-40 overflow-auto bg-base-50">
              <label v-for="role in roleOptions" :key="role.id" class="flex items-center gap-2 py-1">
                <input type="checkbox" class="checkbox checkbox-sm" :value="role.id" v-model="form.roleIds" />
                <span>{{ role.roleName }}</span>
              </label>
              <div v-if="roleOptions.length === 0" class="text-xs text-slate-400">暂无角色</div>
            </div>
          </div>

          <div class="form-control">
            <label class="label"><span class="label-text font-medium">备注</span></label>
            <input v-model="form.remark" type="text" class="input input-bordered" />
          </div>

          <div class="form-control">
            <label class="label cursor-pointer justify-start gap-3">
              <input v-model="form.enabled" type="checkbox" class="checkbox checkbox-primary" />
              <span class="label-text font-medium">启用规则</span>
            </label>
          </div>
        </div>

        <div class="modal-action">
          <button class="btn btn-ghost" @click="closeModal">取消</button>
          <button class="btn btn-primary" :disabled="saving" @click="handleSave">
            <span v-if="saving" class="loading loading-spinner loading-sm"></span>
            保存
          </button>
        </div>
      </div>
      <form method="dialog" class="modal-backdrop"><button>close</button></form>
    </dialog>

    <!-- Delete Modal -->
    <dialog ref="deleteModal" class="modal">
      <div class="modal-box">
        <h3 class="font-bold text-lg mb-3">确认删除</h3>
        <p>确定要删除规则「{{ deleteTarget?.name }}」吗？</p>
        <div class="modal-action">
          <button class="btn btn-ghost" @click="closeDelete">取消</button>
          <button class="btn btn-error" :disabled="saving" @click="confirmDelete">删除</button>
        </div>
      </div>
      <form method="dialog" class="modal-backdrop"><button>close</button></form>
    </dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useUserStore } from '@/stores/user'
import { useDialog } from '@/composables/useDialog'
import {
  queryAuthRules,
  createAuthRule,
  updateAuthRule,
  deleteAuthRule,
  getRoleList,
  getDeptTree,
  type AuthRuleVO,
  type AuthRuleDTO,
  type RoleVO,
  type DeptVO
} from '@/api/system'

const userStore = useUserStore()
const dialog = useDialog()

const canAdd = computed(() => userStore.hasPermission('system:auth-rule:add'))
const canEdit = computed(() => userStore.hasPermission('system:auth-rule:edit'))
const canDelete = computed(() => userStore.hasPermission('system:auth-rule:delete'))

const rules = ref<AuthRuleVO[]>([])
const loading = ref(false)
const saving = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const filterTrigger = ref<string>('')
const filterMethod = ref<string>('')
const filterEnabled = ref<any>('')

const roleOptions = ref<RoleVO[]>([])
const deptOptions = ref<{ id: number; name: string }[]>([])
const deptSelectRef = ref<HTMLSelectElement | null>(null)
const deptViewportRef = ref<HTMLDivElement | null>(null)
const deptOptionsVisible = ref<{ id: number; name: string }[]>([])
const deptPage = ref(1)
const deptPageSize = 200

const formModal = ref<HTMLDialogElement | null>(null)
const deleteModal = ref<HTMLDialogElement | null>(null)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const deleteTarget = ref<AuthRuleVO | null>(null)

const form = reactive<AuthRuleDTO>({
  name: '',
  enabled: true,
  triggerType: 'REGISTER',
  verifyMethod: '',
  matchType: 'ANY',
  matchValue: '',
  roleIds: [],
  deptId: null,
  priority: 100,
  remark: ''
})

onMounted(() => {
  loadRules()
  loadOptions()
})

const loadRules = async () => {
  loading.value = true
  try {
    const res: any = await queryAuthRules({
      page: currentPage.value,
      size: pageSize.value,
      triggerType: filterTrigger.value || undefined,
      verifyMethod: filterMethod.value || undefined,
      enabled: filterEnabled.value === '' ? undefined : filterEnabled.value
    })
    const data = res?.data || res
    rules.value = data.records || []
    total.value = data.total || 0
  } catch (e: any) {
    console.error(e)
    rules.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const loadOptions = async () => {
  try {
    const roles: any = await getRoleList()
    roleOptions.value = roles || []
  } catch {}
  try {
    const depts: any = await getDeptTree()
    deptOptions.value = flattenDepts(depts || [])
    resetDeptOptions()
  } catch {}
}

const flattenDepts = (nodes: DeptVO[], prefix = ''): { id: number; name: string }[] => {
  const result: { id: number; name: string }[] = []
  for (const node of nodes) {
    result.push({ id: node.id, name: `${prefix}${node.deptName}` })
    if (node.children && node.children.length > 0) {
      result.push(...flattenDepts(node.children, `${prefix}— `))
    }
  }
  return result
}

const resetDeptOptions = () => {
  deptPage.value = 1
  deptOptionsVisible.value = deptOptions.value.slice(0, deptPageSize)
  nextTick(() => {
    if (deptSelectRef.value) {
      deptSelectRef.value.scrollTop = 0
    }
  })
}

const loadMoreDeptOptions = () => {
  if (deptOptionsVisible.value.length >= deptOptions.value.length) return
  deptPage.value += 1
  deptOptionsVisible.value = deptOptions.value.slice(0, deptPage.value * deptPageSize)
}

const handleDeptScroll = (event: Event) => {
  const target = event.target as HTMLElement
  if (!target) return
  const nearBottom = target.scrollTop + target.clientHeight >= target.scrollHeight - 16
  if (nearBottom) {
    loadMoreDeptOptions()
  }
}

const openCreateModal = () => {
  isEdit.value = false
  editId.value = null
  form.name = ''
  form.enabled = true
  form.triggerType = 'REGISTER'
  form.verifyMethod = ''
  form.matchType = 'ANY'
  form.matchValue = ''
  form.roleIds = []
  form.deptId = null
  form.priority = 100
  form.remark = ''
  formModal.value?.showModal()
  resetDeptOptions()
}

const openEditModal = (rule: AuthRuleVO) => {
  isEdit.value = true
  editId.value = rule.id
  form.name = rule.name
  form.enabled = rule.enabled
  form.triggerType = rule.triggerType
  form.verifyMethod = rule.verifyMethod || ''
  form.matchType = rule.matchType || 'ANY'
  form.matchValue = rule.matchValue || ''
  form.roleIds = rule.roleIds ? [...rule.roleIds] : []
  form.deptId = rule.deptId ?? null
  form.priority = rule.priority ?? 100
  form.remark = rule.remark || ''
  formModal.value?.showModal()
  resetDeptOptions()
}

const closeModal = () => {
  formModal.value?.close()
}

const handleSave = async () => {
  if (!form.name.trim()) {
    await dialog.alert('请输入规则名称')
    return
  }
  saving.value = true
  try {
    const payload: AuthRuleDTO = {
      name: form.name.trim(),
      enabled: !!form.enabled,
      triggerType: form.triggerType,
      verifyMethod: form.triggerType === 'REGISTER' ? undefined : form.verifyMethod || undefined,
      matchType: form.matchType,
      matchValue: form.matchType === 'ANY' ? undefined : form.matchValue?.trim() || undefined,
      roleIds: form.roleIds && form.roleIds.length > 0 ? form.roleIds : [],
      deptId: form.deptId ?? undefined,
      priority: form.priority ?? 100,
      remark: form.remark?.trim() || undefined
    }
    if (isEdit.value && editId.value) {
      await updateAuthRule(editId.value, payload)
    } else {
      await createAuthRule(payload)
    }
    formModal.value?.close()
    await loadRules()
  } catch (e: any) {
    await dialog.alert(e.message || e.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const handleDelete = (rule: AuthRuleVO) => {
  deleteTarget.value = rule
  deleteModal.value?.showModal()
}

const closeDelete = () => {
  deleteTarget.value = null
  deleteModal.value?.close()
}

const confirmDelete = async () => {
  if (!deleteTarget.value) return
  saving.value = true
  try {
    await deleteAuthRule(deleteTarget.value.id)
    closeDelete()
    await loadRules()
  } catch (e: any) {
    await dialog.alert(e.message || e.response?.data?.message || '删除失败')
  } finally {
    saving.value = false
  }
}

const changePage = (page: number) => {
  currentPage.value = page
  loadRules()
}

const triggerText = (val?: string) => {
  if (val === 'REGISTER') return '注册'
  if (val === 'VERIFY') return '认证'
  return '-'
}

const methodText = (val?: string) => {
  if (!val) return '不限'
  if (val === 'EDU_EMAIL') return 'EDU邮箱'
  if (val === 'MANUAL') return '人工'
  if (val === 'SSO') return 'SSO'
  if (val === 'ID_LIST') return '白名单'
  if (val === 'OCR') return 'OCR'
  return val
}

const matchText = (type?: string, value?: string) => {
  if (!type || type === 'ANY') return '任意'
  if (!value) return type
  if (type === 'EMAIL_DOMAIN') return `域名: ${value}`
  if (type === 'STUDENT_ID_PREFIX') return `学号前缀: ${value}`
  return `${type}: ${value}`
}
</script>
