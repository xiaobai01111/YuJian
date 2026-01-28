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
              <select v-model="filterTrigger" class="select select-bordered select-sm w-32" @change="loadRules({ reset: true })">
                <option :value="''">全部触发</option>
                <option value="REGISTER">注册</option>
                <option value="VERIFY">认证通过</option>
              </select>
            </div>
            <div class="form-control">
              <select v-model="filterMethod" class="select select-bordered select-sm w-40" @change="loadRules({ reset: true })">
                <option :value="''">全部方式</option>
                <option value="EDU_EMAIL">EDU邮箱</option>
                <option value="MANUAL">人工审核</option>
                <option value="SSO">SSO</option>
                <option value="ID_LIST">白名单</option>
                <option value="OCR">证件OCR</option>
              </select>
            </div>
            <div class="form-control">
              <select v-model="filterEnabled" class="select select-bordered select-sm w-32" @change="loadRules({ reset: true })">
                <option :value="''">全部状态</option>
                <option :value="true">启用</option>
                <option :value="false">停用</option>
              </select>
            </div>
            <button class="btn btn-sm btn-ghost" @click="loadRules({ reset: true })">
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
          <div ref="scrollContainer" class="flex-1 overflow-auto">
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
                  <th class="w-20">状态</th>
                  <th class="w-16">优先级</th>
                  <th class="w-32">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="loading">
                  <td colspan="9" class="text-center py-8">
                    <span class="loading loading-spinner loading-md"></span>
                  </td>
                </tr>
                <tr v-else-if="rules.length === 0">
                  <td colspan="9" class="text-center py-8 text-slate-500">暂无数据</td>
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

          <div ref="loadMoreTrigger" class="h-6" aria-hidden="true"></div>
          <div class="flex items-center justify-between p-4 border-t border-base-200 text-sm text-slate-500">
            <div>已加载 {{ rules.length }} / {{ total || '-' }} 条</div>
            <div v-if="loadingMore">正在加载更多...</div>
            <div v-else-if="!hasMore && rules.length > 0">没有更多了</div>
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
                <option value="ID_CARD">学生证</option>
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
                <option value="STUDENT_ID_RANGE">学号范围</option>
                <option value="STUDENT_ID_DICT">学号白名单</option>
              </select>
            </div>
            <div class="form-control">
              <label class="label"><span class="label-text font-medium">匹配值</span></label>
              <input
                v-if="form.matchType === 'ANY'"
                v-model="form.matchValue"
                type="text"
                class="input input-bordered"
                disabled
                placeholder="任意匹配"
              />
              <div v-else-if="form.matchType === 'EMAIL_DOMAIN'" class="border border-base-200 rounded-lg p-3 max-h-40 overflow-auto">
                <div class="text-xs text-slate-500 mb-2">选择域名（不选表示使用全部白名单）</div>
                <label v-for="domain in emailDomains" :key="domain" class="flex items-center gap-2 py-1">
                  <input type="checkbox" class="checkbox checkbox-sm" :value="domain" v-model="selectedDomains" />
                  <span>{{ domain }}</span>
                </label>
                <div v-if="emailDomains.length === 0" class="text-xs text-slate-400">暂无域名白名单</div>
              </div>
              <div v-else-if="form.matchType === 'STUDENT_ID_RANGE'" class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <input v-model="studentRangeStart" type="text" class="input input-bordered" placeholder="起始学号" />
                <input v-model="studentRangeEnd" type="text" class="input input-bordered" placeholder="结束学号" />
              </div>
              <div v-else-if="form.matchType === 'STUDENT_ID_DICT'" class="border border-base-200 rounded-lg p-3 max-h-40 overflow-auto">
                <div class="text-xs text-slate-500 mb-2">选择学号（不选表示使用全部白名单）</div>
                <label v-for="studentId in studentIdWhitelist" :key="studentId" class="flex items-center gap-2 py-1">
                  <input type="checkbox" class="checkbox checkbox-sm" :value="studentId" v-model="selectedStudentIds" />
                  <span>{{ studentId }}</span>
                </label>
                <div v-if="studentIdWhitelist.length === 0" class="text-xs text-slate-400">暂无学号白名单</div>
              </div>
              <input
                v-else
                v-model="form.matchValue"
                type="text"
                class="input input-bordered"
                placeholder="多个用英文逗号分隔"
              />
            </div>
          </div>

          <div class="form-control">
            <label class="label"><span class="label-text font-medium">优先级</span></label>
            <input v-model.number="form.priority" type="number" class="input input-bordered" />
          </div>

          <div class="form-control">
            <label class="label"><span class="label-text font-medium">分配角色</span></label>
            <div class="border border-base-200 rounded-lg p-3 max-h-40 overflow-auto bg-base-50">
              <label class="flex items-center gap-2 py-1">
                <input
                  type="radio"
                  class="radio radio-sm"
                  :value="null"
                  v-model="selectedRoleId"
                />
                <span>不分配</span>
              </label>
              <label v-for="role in roleOptions" :key="role.id" class="flex items-center gap-2 py-1">
                <input
                  type="radio"
                  class="radio radio-sm"
                  :value="role.id"
                  v-model="selectedRoleId"
                />
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
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useUserStore } from '@/stores/user'
import { useDialog } from '@/composables/useDialog'
import {
  queryAuthRules,
  createAuthRule,
  updateAuthRule,
  deleteAuthRule,
  getRoleList,
  getEmailDomains,
  getStudentIdWhitelist,
  type AuthRuleVO,
  type AuthRuleDTO,
  type RoleVO
} from '@/api/system'

const userStore = useUserStore()
const dialog = useDialog()

const canAdd = computed(() => userStore.hasPermission('system:auth-rule:add'))
const canEdit = computed(() => userStore.hasPermission('system:auth-rule:edit'))
const canDelete = computed(() => userStore.hasPermission('system:auth-rule:delete'))

const rules = ref<AuthRuleVO[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const saving = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const hasMore = ref(true)
const scrollContainer = ref<HTMLElement | null>(null)
const loadMoreTrigger = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

const filterTrigger = ref<string>('')
const filterMethod = ref<string>('')
const filterEnabled = ref<any>('')

const roleOptions = ref<RoleVO[]>([])
const emailDomains = ref<string[]>([])
const studentIdWhitelist = ref<string[]>([])
const selectedDomains = ref<string[]>([])
const selectedStudentIds = ref<string[]>([])
const studentRangeStart = ref('')
const studentRangeEnd = ref('')

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
  priority: 100,
  remark: ''
})

const selectedRoleId = computed<number | null>({
  get() {
    return form.roleIds && form.roleIds.length > 0 ? form.roleIds[0] : null
  },
  set(value) {
    form.roleIds = value ? [value] : []
  }
})

onMounted(() => {
  loadRules({ reset: true })
  loadOptions()
  nextTick(() => setupObserver())
})

onUnmounted(() => {
  observer?.disconnect()
  observer = null
})

const loadRules = async ({ append = false, reset = false } = {}) => {
  if (append && (loadingMore.value || loading.value)) return
  if (!append && loading.value) return
  if (reset) {
    currentPage.value = 1
    rules.value = []
    hasMore.value = true
  }
  append ? (loadingMore.value = true) : (loading.value = true)
  try {
    const res: any = await queryAuthRules({
      page: currentPage.value,
      size: pageSize.value,
      triggerType: filterTrigger.value || undefined,
      verifyMethod: filterMethod.value || undefined,
      enabled: filterEnabled.value === '' ? undefined : filterEnabled.value
    })
    const data = res?.data || res
    const records = data.records || []
    total.value = data.total || 0
    rules.value = append ? [...rules.value, ...records] : records
    if (total.value) {
      hasMore.value = rules.value.length < total.value
    } else {
      hasMore.value = records.length >= pageSize.value
    }
  } catch (e: any) {
    console.error(e)
    if (!append) {
      rules.value = []
      total.value = 0
    }
  } finally {
    append ? (loadingMore.value = false) : (loading.value = false)
  }
}

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

const loadMore = async () => {
  if (!hasMore.value || loading.value || loadingMore.value) return
  currentPage.value += 1
  await loadRules({ append: true })
}

const loadOptions = async () => {
  try {
    const roles: any = await getRoleList()
    roleOptions.value = roles || []
  } catch {}
  try {
    const domains: any = await getEmailDomains()
    emailDomains.value = Array.isArray(domains) ? domains : []
  } catch {
    emailDomains.value = []
  }
  try {
    const ids: any = await getStudentIdWhitelist()
    studentIdWhitelist.value = Array.isArray(ids) ? ids : []
  } catch {
    studentIdWhitelist.value = []
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
  form.priority = 100
  form.remark = ''
  selectedDomains.value = []
  selectedStudentIds.value = []
  studentRangeStart.value = ''
  studentRangeEnd.value = ''
  formModal.value?.showModal()
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
  form.roleIds = rule.roleIds && rule.roleIds.length > 0 ? [rule.roleIds[0]] : []
  form.priority = rule.priority ?? 100
  form.remark = rule.remark || ''
  selectedDomains.value = rule.matchType === 'EMAIL_DOMAIN' && rule.matchValue
    ? rule.matchValue.split(',').map(item => item.trim()).filter(item => item)
    : []
  selectedStudentIds.value = rule.matchType === 'STUDENT_ID_DICT' && rule.matchValue
    ? rule.matchValue.split(',').map(item => item.trim()).filter(item => item)
    : []
  if (rule.matchType === 'STUDENT_ID_RANGE' && rule.matchValue) {
    const [start, end] = rule.matchValue.split(',').map(item => item.trim())
    studentRangeStart.value = start || ''
    studentRangeEnd.value = end || ''
  } else {
    studentRangeStart.value = ''
    studentRangeEnd.value = ''
  }
  formModal.value?.showModal()
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
    const matchValue = (() => {
      if (form.matchType === 'EMAIL_DOMAIN') {
        return selectedDomains.value.length > 0 ? selectedDomains.value.join(',') : undefined
      }
      if (form.matchType === 'STUDENT_ID_DICT') {
        return selectedStudentIds.value.length > 0 ? selectedStudentIds.value.join(',') : undefined
      }
      if (form.matchType === 'STUDENT_ID_RANGE') {
        const start = studentRangeStart.value.trim()
        const end = studentRangeEnd.value.trim()
        if (!start || !end) return undefined
        return `${start},${end}`
      }
      return form.matchType === 'ANY' ? undefined : form.matchValue?.trim() || undefined
    })()
    const payload: AuthRuleDTO = {
      name: form.name.trim(),
      enabled: !!form.enabled,
      triggerType: form.triggerType,
      verifyMethod: form.triggerType === 'REGISTER' ? undefined : form.verifyMethod || undefined,
      matchType: form.matchType,
      matchValue,
      roleIds: form.roleIds && form.roleIds.length > 0 ? form.roleIds : [],
      priority: form.priority ?? 100,
      remark: form.remark?.trim() || undefined
    }
    if (isEdit.value && editId.value) {
      await updateAuthRule(editId.value, payload)
    } else {
      await createAuthRule(payload)
    }
    formModal.value?.close()
    await loadRules({ reset: true })
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
    await loadRules({ reset: true })
  } catch (e: any) {
    await dialog.alert(e.message || e.response?.data?.message || '删除失败')
  } finally {
    saving.value = false
  }
}


const triggerText = (val?: string) => {
  if (val === 'REGISTER') return '注册'
  if (val === 'VERIFY') return '认证'
  return '-'
}

const methodText = (val?: string) => {
  if (!val) return '不限'
  if (val === 'EDU_EMAIL') return 'EDU邮箱'
  if (val === 'ID_CARD') return '学生证'
  if (val === 'MANUAL') return '人工'
  if (val === 'SSO') return 'SSO'
  if (val === 'ID_LIST') return '白名单'
  if (val === 'OCR') return 'OCR'
  return val
}

const matchText = (type?: string, value?: string) => {
  if (!type || type === 'ANY') return '任意'
  if (type === 'EMAIL_DOMAIN') return value ? `域名: ${value}` : '域名白名单'
  if (type === 'STUDENT_ID_RANGE') return value ? `学号范围: ${value}` : '学号范围'
  if (type === 'STUDENT_ID_DICT') return value ? `学号白名单: ${value}` : '学号白名单'
  return value ? `${type}: ${value}` : type
}
</script>
