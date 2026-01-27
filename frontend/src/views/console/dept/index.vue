<template>
  <div class="h-full flex flex-col min-h-0">
    <div class="card bg-base-100 shadow-sm border border-base-200 flex-1 min-h-0 flex flex-col">
      <div class="card-body p-4 flex-1 min-h-0 flex flex-col overflow-hidden">
        <!-- Toolbar -->
        <div class="flex flex-wrap justify-between items-center mb-4 gap-4 flex-none">
          <div class="flex flex-wrap gap-2">
            <button class="btn btn-primary btn-sm gap-2 font-normal" @click="openFormModal()">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
              </svg>
              新增部门
            </button>
            <button class="btn btn-ghost btn-sm gap-2 font-normal" @click="toggleExpandAll">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4" />
              </svg>
              {{ isExpanded ? '收起全部' : '展开全部' }}
            </button>
          </div>

          <div class="flex gap-2">
            <button class="btn btn-circle btn-ghost btn-sm" @click="() => fetchDeptTree()">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-base-content/60" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
            </button>
          </div>
        </div>

        <div class="flex flex-wrap items-center gap-2 mb-4 flex-none">
          <div class="form-control">
            <input
              v-model="searchKeyword"
              type="text"
              placeholder="搜索部门名称/负责人/电话/邮箱"
              class="input input-bordered input-sm w-72"
              @keyup.enter="handleSearch"
            />
          </div>
          <button class="btn btn-sm btn-ghost" @click="handleSearch">搜索</button>
          <button class="btn btn-sm btn-ghost" @click="resetSearch">重置</button>
        </div>

        <!-- Table -->
        <div class="card bg-base-100 shadow-sm border border-base-200 flex-1 min-h-0">
          <div class="card-body p-0 flex flex-col min-h-0">
            <div class="overflow-auto flex-1 min-h-0">
              <table class="table dept-table">
            <thead class="bg-base-200/30 text-base-content/70">
              <tr>
                <th class="w-64" style="border-bottom: 1px solid hsl(var(--b2) / 0.8); border-right: 1px solid hsl(var(--b2) / 0.8)">部门名称</th>
                <th style="border-bottom: 1px solid hsl(var(--b2) / 0.8); border-right: 1px solid hsl(var(--b2) / 0.8)">负责人</th>
                <th style="border-bottom: 1px solid hsl(var(--b2) / 0.8); border-right: 1px solid hsl(var(--b2) / 0.8)">联系电话</th>
                <th style="border-bottom: 1px solid hsl(var(--b2) / 0.8); border-right: 1px solid hsl(var(--b2) / 0.8)">邮箱</th>
                <th style="border-bottom: 1px solid hsl(var(--b2) / 0.8); border-right: 1px solid hsl(var(--b2) / 0.8)">排序</th>
                <th style="border-bottom: 1px solid hsl(var(--b2) / 0.8); border-right: 1px solid hsl(var(--b2) / 0.8)">数据范围</th>
                <th style="border-bottom: 1px solid hsl(var(--b2) / 0.8); border-right: 1px solid hsl(var(--b2) / 0.8)">状态</th>
                <th style="border-bottom: 1px solid hsl(var(--b2) / 0.8); border-right: 1px solid hsl(var(--b2) / 0.8)">创建时间</th>
                <th class="text-center w-40" style="border-bottom: 1px solid hsl(var(--b2) / 0.8)">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="9" class="text-center py-10">
                  <span class="loading loading-spinner loading-lg text-primary"></span>
                </td>
              </tr>
              <template v-else>
                <DeptTreeRow
                  v-for="dept in visibleTree"
                  :key="dept.id"
                  :dept="dept"
                  :level="0"
                  :expanded-ids="expandedIds"
                  @toggle-expand="toggleExpand"
                  @edit="openFormModal"
                  @delete="handleDelete"
                  @add-child="openFormModal"
                  @status-change="handleStatusChange"
                />
              </template>
            </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Delete Dept Modal -->
    <dialog :class="['modal', showDeleteModal && 'modal-open']">
      <div class="modal-box max-w-lg">
        <h3 class="font-bold text-lg mb-4">删除部门</h3>
        <p class="text-base-content/70 mb-4">
          确定要删除部门 <span class="font-semibold text-error">{{ deleteTarget?.deptName }}</span> 吗？
        </p>
        
        <!-- 用户列表 -->
        <div v-if="deleteLoading" class="flex justify-center py-4">
          <span class="loading loading-spinner loading-md"></span>
        </div>
        <div v-else-if="deleteUsers.length > 0" class="mb-4">
          <div class="alert alert-warning mb-3">
            <svg xmlns="http://www.w3.org/2000/svg" class="stroke-current shrink-0 h-5 w-5" fill="none" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            <span>该部门下有 <strong>{{ deleteUsers.length }}</strong> 个用户</span>
          </div>
          <div class="max-h-32 overflow-y-auto bg-base-200 rounded-lg p-2">
            <div v-for="user in deleteUsers" :key="user.id" class="text-sm py-1 px-2">
              {{ user.username }} <span class="text-base-content/50">({{ user.nickname }})</span>
            </div>
          </div>
          
          <!-- 用户处理策略 -->
          <div class="form-control mt-4">
            <label class="label"><span class="label-text font-medium">用户处理方式</span></label>
            <select v-model="deleteStrategy" class="select select-bordered w-full">
              <option value="TRANSFER_PARENT">转移到上级部门</option>
              <option value="UNASSIGN">转移到未分配</option>
              <option value="DELETE">删除用户</option>
            </select>
          </div>
          
          <!-- 策略后果提示 -->
          <div class="mt-3 text-sm">
            <div v-if="deleteStrategy === 'TRANSFER_PARENT'" class="alert alert-info py-2">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="stroke-current shrink-0 w-5 h-5"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
              <span>用户将被转移到上级部门，并强制重新登录</span>
            </div>
            <div v-else-if="deleteStrategy === 'UNASSIGN'" class="alert alert-info py-2">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="stroke-current shrink-0 w-5 h-5"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
              <span>用户将变为未分配部门状态，并强制重新登录</span>
            </div>
            <div v-else-if="deleteStrategy === 'DELETE'" class="alert alert-error py-2">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="stroke-current shrink-0 w-5 h-5"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path></svg>
              <span><strong>危险操作！</strong>用户将被软删除并强制下线，无法恢复登录</span>
            </div>
          </div>
          
          <!-- 删除原因（删除用户时显示） -->
          <div v-if="deleteStrategy === 'DELETE'" class="form-control mt-3">
            <label class="label"><span class="label-text font-medium">删除原因</span></label>
            <textarea v-model="deleteReason" class="textarea textarea-bordered" placeholder="请输入删除原因"></textarea>
          </div>
        </div>
        <div v-else class="text-base-content/60 mb-4">该部门下没有用户</div>
        
        <div class="modal-action">
          <button class="btn btn-ghost" @click="closeDeleteModal" :disabled="deleteLoading">取消</button>
          <button class="btn btn-error" @click="confirmDelete" :disabled="deleteLoading">
            <span v-if="deleteLoading" class="loading loading-spinner loading-sm"></span>
            确认删除
          </button>
        </div>
      </div>
      <form method="dialog" class="modal-backdrop">
        <button @click="closeDeleteModal">close</button>
      </form>
    </dialog>

    <!-- Dept Form Modal -->
    <dialog id="dept_form_modal" class="modal">
      <div class="modal-box">
        <h3 class="font-bold text-lg mb-4">{{ isEdit ? '编辑部门' : '新增部门' }}</h3>
        <div class="space-y-4">
          <div class="form-control w-full">
            <label class="label"><span class="label-text font-medium">上级部门</span></label>
            <input
              v-if="isEdit && currentId === SYSTEM_DEPT_ID"
              class="input input-bordered w-full"
              value="系统部门（顶级）"
              disabled
            />
            <select v-else v-model="form.parentId" class="select select-bordered w-full">
              <option v-for="dept in flatDeptList" :key="dept.id" :value="dept.id">
                {{ '　'.repeat(dept.level) + dept.deptName }}
              </option>
            </select>
          </div>
          <div class="form-control w-full">
            <label class="label"><span class="label-text font-medium">部门名称</span></label>
            <input type="text" v-model="form.deptName" placeholder="请输入部门名称" class="input input-bordered w-full" />
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div class="form-control w-full">
              <label class="label"><span class="label-text font-medium">负责人</span></label>
              <input type="text" v-model="form.leader" placeholder="请输入负责人" class="input input-bordered w-full" />
            </div>
            <div class="form-control w-full">
              <label class="label"><span class="label-text font-medium">联系电话</span></label>
              <input type="text" v-model="form.phone" placeholder="请输入联系电话" class="input input-bordered w-full" />
            </div>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div class="form-control w-full">
              <label class="label"><span class="label-text font-medium">邮箱</span></label>
              <input type="email" v-model="form.email" placeholder="请输入邮箱" class="input input-bordered w-full" />
            </div>
            <div class="form-control w-full">
              <label class="label"><span class="label-text font-medium">排序</span></label>
              <input type="number" v-model="form.sortOrder" class="input input-bordered w-full" />
            </div>
          </div>
          <div class="form-control w-full">
            <label class="label"><span class="label-text font-medium">数据范围</span></label>
            <select v-model="form.dataScope" class="select select-bordered w-full" :disabled="isEdit && currentId === SYSTEM_DEPT_ID">
              <option :value="1">全部数据权限</option>
              <option :value="2">自定义数据权限</option>
              <option :value="4">本部门及以下数据权限</option>
              <option :value="3">本部门数据权限</option>
              <option :value="5">仅本人数据权限</option>
            </select>
            <label v-if="isEdit && currentId === SYSTEM_DEPT_ID" class="label">
              <span class="label-text-alt text-base-content/60">系统部门数据范围不可修改</span>
            </label>
          </div>
          <div v-if="!isEdit" class="form-control">
            <label class="label cursor-pointer justify-start gap-4">
              <span class="label-text font-medium">状态</span>
              <input type="checkbox" class="toggle toggle-primary" :checked="form.status === 0" @change="form.status = ($event.target as HTMLInputElement).checked ? 0 : 1" />
              <span class="label-text text-base-content/60">{{ form.status === 0 ? '正常' : '停用' }}</span>
            </label>
          </div>
        </div>
        <div class="modal-action">
          <form method="dialog">
            <button class="btn btn-ghost">取消</button>
            <button class="btn btn-primary ml-2" @click.prevent="submitForm" :disabled="submitting">保存</button>
          </form>
        </div>
      </div>
    </dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, defineComponent, h, type PropType } from 'vue'
import { getDeptTree, createDept, updateDept, deleteDeptWithStrategy, updateDeptStatus, getDeptUsers, type DeptVO, type DeptDTO, type DeptUserStrategy } from '@/api/system'
import type { UserVO } from '@/api/system'
import { useDialog } from '@/composables/useDialog'

interface DeptNode extends DeptVO {
  level?: number
}

// --- Components ---
const DeptTreeRow = defineComponent({
  name: 'DeptTreeRow',
  props: {
    dept: { type: Object as PropType<DeptVO>, required: true },
    level: { type: Number, default: 0 },
    expandedIds: { type: Set as PropType<Set<number>>, required: true }
  },
  emits: ['toggle-expand', 'edit', 'delete', 'add-child', 'status-change'],
  setup(props, { emit }) {
    return () => {
      const hasChildren = props.dept.children && props.dept.children.length > 0
      const isExpanded = props.expandedIds.has(props.dept.id)
      const paddingLeft = `${props.level * 24 + 16}px`

      const rows = []
      const cellBorder = 'border-bottom: 1px solid hsl(var(--b2) / 0.8); border-right: 1px solid hsl(var(--b2) / 0.8)'
      const cellBorderLast = 'border-bottom: 1px solid hsl(var(--b2) / 0.8)'
      
      // Current row
      rows.push(h('tr', { class: 'hover' }, [
        h('td', { style: `padding-left: ${paddingLeft}; ${cellBorder}` }, [
          h('div', { class: 'flex items-center gap-2' }, [
            hasChildren
              ? h('button', {
                  class: 'w-5 h-5 flex items-center justify-center text-base-content/60 hover:text-primary',
                  onClick: () => emit('toggle-expand', props.dept.id)
                }, [
                  h('svg', {
                    class: `w-4 h-4 transition-transform ${isExpanded ? 'rotate-90' : ''}`,
                    fill: 'none',
                    viewBox: '0 0 24 24',
                    stroke: 'currentColor'
                  }, [
                    h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M9 5l7 7-7 7' })
                  ])
                ])
              : h('span', { class: 'w-5' }),
            h('span', { class: 'font-medium' }, props.dept.deptName)
          ])
        ]),
        h('td', { style: cellBorder }, props.dept.leader || '-'),
        h('td', { style: cellBorder }, props.dept.phone || '-'),
        h('td', { class: 'text-base-content/60', style: cellBorder }, props.dept.email || '-'),
        h('td', { style: cellBorder }, props.dept.sortOrder),
        h('td', { style: cellBorder }, [
          h('span', { class: `badge badge-sm ${getDataScopeClass(props.dept.dataScope)}` }, getDataScopeLabel(props.dept.dataScope))
        ]),
        h('td', { style: cellBorder }, [
          h('input', {
            type: 'checkbox',
            class: 'toggle toggle-primary toggle-sm',
            checked: props.dept.status === 0,
            onChange: () => emit('status-change', props.dept)
          })
        ]),
        h('td', { class: 'text-base-content/60 text-sm', style: cellBorder }, formatDate(props.dept.createdAt)),
        h('td', { style: cellBorderLast }, [
          h('div', { class: 'flex justify-center gap-2' }, [
            h('button', {
              class: 'btn btn-square btn-xs bg-blue-50 text-blue-600 border-none hover:bg-blue-100',
              title: '编辑',
              onClick: () => emit('edit', props.dept)
            }, [
              h('svg', { class: 'h-3.5 w-3.5', fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor' }, [
                h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z' })
              ])
            ]),
            h('button', {
              class: 'btn btn-square btn-xs bg-green-50 text-green-600 border-none hover:bg-green-100',
              title: '新增子部门',
              onClick: () => emit('add-child', null, props.dept.id)
            }, [
              h('svg', { class: 'h-3.5 w-3.5', fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor' }, [
                h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M12 4v16m8-8H4' })
              ])
            ]),
            props.dept.id === SYSTEM_DEPT_ID ? null : h('button', {
              class: 'btn btn-square btn-xs bg-red-50 text-red-600 border-none hover:bg-red-100',
              title: '删除',
              onClick: () => emit('delete', props.dept)
            }, [
              h('svg', { class: 'h-3.5 w-3.5', fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor' }, [
                h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16' })
              ])
            ])
          ])
        ])
      ]))

      // Children rows
      if (hasChildren && isExpanded) {
        for (const child of props.dept.children!) {
          rows.push(h(DeptTreeRow, {
            dept: child,
            level: props.level + 1,
            expandedIds: props.expandedIds,
            onToggleExpand: (id: number) => emit('toggle-expand', id),
            onEdit: (d: DeptVO) => emit('edit', d),
            onDelete: (d: DeptVO) => emit('delete', d),
            onAddChild: (_: any, parentId: number) => emit('add-child', null, parentId),
            onStatusChange: (d: DeptVO) => emit('status-change', d)
          }))
        }
      }

      return rows
    }
  }
})

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

const getDataScopeLabel = (scope?: number) => {
  switch (scope) {
    case 1:
      return '全部数据权限'
    case 2:
      return '自定义数据权限'
    case 3:
      return '本部门数据权限'
    case 4:
      return '本部门及以下数据权限'
    case 5:
      return '仅本人数据权限'
    default:
      return '-'
  }
}

const getDataScopeClass = (scope?: number) => {
  switch (scope) {
    case 1:
      return 'badge-success'
    case 2:
      return 'badge-warning'
    case 3:
      return 'badge-info'
    case 4:
      return 'badge-primary'
    case 5:
      return 'badge-ghost'
    default:
      return 'badge-ghost'
  }
}

// --- State ---
const loading = ref(false)
const submitting = ref(false)
const deptTree = ref<DeptVO[]>([])
const visibleTree = ref<DeptVO[]>([])
const expandedIds = ref<Set<number>>(new Set())
const isExpanded = ref(false)
const SYSTEM_DEPT_ID = 1
const dialog = useDialog()
const searchKeyword = ref('')

const form = reactive<DeptDTO>({
  parentId: SYSTEM_DEPT_ID,
  deptName: '',
  sortOrder: 0,
  leader: '',
  phone: '',
  email: '',
  status: 0,
  dataScope: 3
})
const isEdit = ref(false)
const currentId = ref<number>(0)

// Flatten dept tree for parent select
const flatDeptList = computed(() => {
  const result: DeptNode[] = []
  const flatten = (depts: DeptVO[], level: number) => {
    for (const dept of depts) {
      result.push({ ...dept, level })
      if (dept.children) {
        flatten(dept.children, level + 1)
      }
    }
  }
  flatten(deptTree.value, 0)
  return result
})

onMounted(() => {
  fetchDeptTree()
})

const fetchDeptTree = async (preserveExpanded = false) => {
  loading.value = true
  try {
    const res: any = await getDeptTree()
    deptTree.value = res || []
    applySearch(preserveExpanded)
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const toggleExpand = (id: number) => {
  if (expandedIds.value.has(id)) {
    expandedIds.value.delete(id)
  } else {
    expandedIds.value.add(id)
  }
  expandedIds.value = new Set(expandedIds.value)
}

const toggleExpandAll = () => {
  if (isExpanded.value) {
    expandedIds.value = new Set()
    isExpanded.value = false
  } else {
    const allIds = new Set<number>()
    const collectIds = (depts: DeptVO[]) => {
      for (const dept of depts) {
        allIds.add(dept.id)
        if (dept.children) collectIds(dept.children)
      }
    }
    collectIds(visibleTree.value)
    expandedIds.value = allIds
    isExpanded.value = true
  }
}

const handleSearch = () => {
  applySearch()
}

const resetSearch = () => {
  searchKeyword.value = ''
  applySearch()
}

const applySearch = (preserveExpanded = false) => {
  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    visibleTree.value = deptTree.value
    // 仅在非保留模式下重置展开状态
    if (!preserveExpanded) {
      expandedIds.value = new Set()
      isExpanded.value = false
    }
    return
  }
  const { tree, expanded } = filterDeptTree(deptTree.value, keyword)
  visibleTree.value = tree
  expandedIds.value = expanded
  isExpanded.value = expanded.size > 0
}

const filterDeptTree = (nodes: DeptVO[], keyword: string) => {
  const expanded = new Set<number>()
  const match = (dept: DeptVO) => {
    const target = `${dept.deptName || ''} ${dept.leader || ''} ${dept.phone || ''} ${dept.email || ''}`.toLowerCase()
    return target.includes(keyword.toLowerCase())
  }
  const walk = (list: DeptVO[]): DeptVO[] => {
    const result: DeptVO[] = []
    for (const dept of list) {
      const children = dept.children ? walk(dept.children) : []
      if (match(dept) || children.length > 0) {
        if (children.length > 0) {
          expanded.add(dept.id)
        }
        result.push({ ...dept, children })
      }
    }
    return result
  }
  return { tree: walk(nodes), expanded }
}

const openFormModal = (dept?: DeptVO | null, parentId?: number) => {
  if (dept) {
    isEdit.value = true
    currentId.value = dept.id
    form.parentId = dept.parentId === 0 ? SYSTEM_DEPT_ID : dept.parentId
    form.deptName = dept.deptName
    form.sortOrder = dept.sortOrder
    form.leader = dept.leader || ''
    form.phone = dept.phone || ''
    form.email = dept.email || ''
    form.status = dept.status
    form.dataScope = dept.dataScope ?? 3
  } else {
    isEdit.value = false
    form.parentId = parentId || SYSTEM_DEPT_ID
    form.deptName = ''
    form.sortOrder = 0
    form.leader = ''
    form.phone = ''
    form.email = ''
    form.status = 0
    form.dataScope = 3
  }
  const modal = document.getElementById('dept_form_modal') as HTMLDialogElement
  modal.showModal()
}

const submitForm = async () => {
  if (!form.deptName) {
    await dialog.alert('请输入部门名称')
    return
  }
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateDept(currentId.value, form)
    } else {
      await createDept(form)
    }
    const modal = document.getElementById('dept_form_modal') as HTMLDialogElement
    modal.close()
    fetchDeptTree(true)
  } catch (error) {
    console.error(error)
  } finally {
    submitting.value = false
  }
}

// --- Delete Modal State ---
const showDeleteModal = ref(false)
const deleteTarget = ref<DeptVO | null>(null)
const deleteUsers = ref<UserVO[]>([])
const deleteLoading = ref(false)
const deleteStrategy = ref<DeptUserStrategy>('UNASSIGN')
const deleteReason = ref('')

const handleDelete = async (dept: DeptVO) => {
  if (dept.children && dept.children.length > 0) {
    await dialog.alert('存在子部门，无法删除，请先删除或转移子部门')
    return
  }
  deleteTarget.value = dept
  deleteStrategy.value = 'UNASSIGN'
  deleteReason.value = ''
  deleteLoading.value = true
  showDeleteModal.value = true
  
  try {
    const res: any = await getDeptUsers(dept.id)
    deleteUsers.value = res || []
  } catch (error) {
    console.error(error)
    deleteUsers.value = []
  } finally {
    deleteLoading.value = false
  }
}

const closeDeleteModal = () => {
  showDeleteModal.value = false
  deleteTarget.value = null
  deleteUsers.value = []
}

const confirmDelete = async () => {
  if (!deleteTarget.value) return
  if (deleteStrategy.value === 'DELETE' && !deleteReason.value.trim()) {
    await dialog.alert('删除用户必须提供原因')
    return
  }
  deleteLoading.value = true
  try {
    await deleteDeptWithStrategy(deleteTarget.value.id, {
      userStrategy: deleteStrategy.value,
      reason: deleteReason.value || undefined
    })
    closeDeleteModal()
    fetchDeptTree(true)
  } catch (error: any) {
    await dialog.alert(error?.response?.data?.message || '删除失败')
  } finally {
    deleteLoading.value = false
  }
}

const handleStatusChange = async (dept: DeptVO) => {
  const newStatus = dept.status === 0 ? 1 : 0
  const actionName = newStatus === 1 ? '停用' : '启用'
  
  if (!await dialog.confirm(`确定要${actionName}部门 ${dept.deptName} 吗？${newStatus === 1 ? '\n停用后该部门下的用户将被踢出登录。' : ''}`)) {
    fetchDeptTree(true)
    return
  }
  
  try {
    await updateDeptStatus(dept.id, newStatus)
    fetchDeptTree(true)
  } catch (error: any) {
    await dialog.alert(error?.response?.data?.message || '操作失败')
    fetchDeptTree(true)
  }
}
</script>

<style scoped>
.dept-table {
  border-collapse: collapse;
  width: 100%;
}
.dept-table :deep(th),
.dept-table :deep(td) {
  border-bottom: 1px solid hsl(var(--b2) / 0.8);
  border-right: 1px solid hsl(var(--b2) / 0.8);
}
.dept-table :deep(th:last-child),
.dept-table :deep(td:last-child) {
  border-right: 0;
}
.dept-table :deep(tbody tr:last-child th),
.dept-table :deep(tbody tr:last-child td) {
  border-bottom: 0;
}
</style>
