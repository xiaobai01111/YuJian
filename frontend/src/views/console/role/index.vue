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
              添加
            </button>
            <button class="btn btn-success btn-sm text-white gap-2 font-normal" :disabled="selectedIds.length !== 1" @click="handleEdit">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
              修改
            </button>
            <button class="btn btn-error btn-sm text-white gap-2 font-normal" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
              删除
            </button>
            <button class="btn btn-success btn-outline btn-sm gap-2 font-normal" :disabled="selectedIds.length !== 1" @click="handleAssignMenu">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16m-7 6h7" />
              </svg>
              分配菜单
            </button>
            <button class="btn btn-outline btn-sm gap-2 font-normal" :disabled="!canAssignDept" @click="handleAssignDept">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7h18M3 12h18M3 17h18" />
              </svg>
              授权部门
            </button>
          </div>

          <div class="flex gap-2">
            <button class="btn btn-circle btn-ghost btn-sm" @click="refreshRoles">
               <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-base-content/60" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
            </button>
          </div>
        </div>

        <div class="flex flex-wrap items-center gap-2 mb-4 flex-none">
          <div class="form-control">
            <input
              v-model="queryParams.keyword"
              type="text"
              placeholder="搜索角色名称/标识/备注"
              class="input input-bordered input-sm w-64"
              @keyup.enter="handleSearch"
            />
          </div>
          <button class="btn btn-sm btn-ghost" @click="handleSearch">搜索</button>
          <button class="btn btn-sm btn-ghost" @click="resetSearch">重置</button>
        </div>

        <!-- Table -->
        <div ref="scrollContainer" class="overflow-auto flex-1 min-h-0">
          <div class="rounded-xl border border-base-200 overflow-hidden">
            <table class="table table-md table-pin-rows role-table">
            <thead class="bg-base-200/30 text-base-content/70">
              <tr>
                <th class="w-10">
                   <label>
                    <input type="checkbox" class="checkbox checkbox-sm rounded-sm" :checked="isAllSelected" @change="toggleSelectAll" />
                  </label>
                </th>
                <th class="w-16">序号</th>
                <th>角色名称</th>
                <th>角色编号</th>
                <th>角色状态</th>
                <th>角色排序</th>
                <th>角色备注</th>
                <th>部门权限</th>
                <th>创建时间</th>
                <th class="text-center w-40">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="10" class="text-center py-10">
                  <span class="loading loading-spinner loading-lg text-primary"></span>
                </td>
              </tr>
              <tr v-else v-for="(role, index) in roleList" :key="role.id" class="hover">
                <th>
                  <label>
                    <input type="checkbox" class="checkbox checkbox-sm rounded-sm" :checked="selectedIds.includes(role.id)" @change="toggleSelection(role.id)" />
                  </label>
                </th>
                <td class="text-base-content/60">{{ index + 1 }}</td>
                <td class="font-medium">{{ role.roleName }}</td>
                <td>{{ role.roleKey }}</td>
                <td>
                  <div class="flex items-center gap-2">
                    <input type="checkbox" class="toggle toggle-primary toggle-sm" :checked="role.status === 0" :disabled="isAdminRole(role)" @change="handleStatusChange(role)" />
                    <span v-if="role.status === 1" class="badge badge-error badge-sm">停用</span>
                    <span v-else class="badge badge-success badge-sm">正常</span>
                  </div>
                </td>
                <td>{{ role.sortOrder }}</td>
                <td class="text-base-content/60 max-w-xs truncate" :title="role.remark || ''">{{ role.remark || '-' }}</td>
                <td>
                  <span v-if="isAdminRole(role)" class="badge badge-primary badge-sm">全部部门</span>
                  <span v-else-if="getRoleDeptCount(role.id) > 0" class="badge badge-success badge-sm">已分配 {{ getRoleDeptCount(role.id) }} 个</span>
                  <span v-else class="badge badge-ghost badge-sm">未分配</span>
                </td>
                <td class="text-base-content/60 text-sm">{{ formatDate(role.createdAt) }}</td>
                <td>
                   <div class="flex justify-center gap-2">
                      <button class="btn btn-square btn-xs bg-blue-50 text-blue-600 border-none hover:bg-blue-100" title="编辑" @click="openFormModal(role)">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                        </svg>
                      </button>
                      <button v-if="!isAdminRole(role)" class="btn btn-square btn-xs bg-red-50 text-red-600 border-none hover:bg-red-100" title="删除" @click="handleDelete(role)">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                        </svg>
                      </button>
                      <button v-if="!isAdminRole(role)" class="btn btn-square btn-xs bg-green-50 text-green-600 border-none hover:bg-green-100" title="菜单权限" @click="openMenuModal(role)">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                           <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                        </svg>
                      </button>
                      <button v-if="!isAdminRole(role)" class="btn btn-square btn-xs bg-purple-50 text-purple-600 border-none hover:bg-purple-100" title="授权部门" @click="openRoleDeptModal(role)">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7h18M3 12h18M3 17h18" />
                        </svg>
                      </button>
                  </div>
                </td>
              </tr>
            </tbody>
            </table>
          </div>
          <div ref="loadMoreTrigger" class="py-3 text-center text-xs text-base-content/50">
            <span v-if="loadingMore">加载中...</span>
            <span v-else-if="!hasMore && roleList.length > 0">已加载完毕</span>
          </div>
        </div>
        
        <div class="flex justify-between items-center mt-4 border-t border-base-200 pt-3 flex-none text-sm text-base-content/60">
          <div>已加载 {{ roleList.length }} / {{ total || '-' }} 条</div>
          <div v-if="loadingMore">正在加载更多...</div>
        </div>
      </div>
    </div>

    <!-- Role Form Modal -->
    <dialog id="role_form_modal" class="modal">
      <div class="modal-box">
        <h3 class="font-bold text-lg mb-4">{{ isEdit ? '编辑角色' : '新增角色' }}</h3>
        <div class="space-y-4">
          <div class="form-control w-full">
            <label class="label"><span class="label-text font-medium">角色名称</span></label>
            <input type="text" v-model="form.roleName" placeholder="例如：普通用户" class="input input-bordered w-full" />
          </div>
          <div class="form-control w-full">
            <label class="label"><span class="label-text font-medium">权限字符</span></label>
            <input type="text" v-model="form.roleKey" placeholder="例如：user" class="input input-bordered w-full" :disabled="isEdit" :class="{ 'input-disabled bg-base-200': isEdit }" />
            <label v-if="isEdit" class="label"><span class="label-text-alt text-warning">权限字符创建后不可修改</span></label>
          </div>
          <div class="form-control w-full">
            <label class="label"><span class="label-text font-medium">排序</span></label>
            <input type="number" v-model="form.sortOrder" class="input input-bordered w-full" />
          </div>
          <div class="form-control w-full">
            <label class="label"><span class="label-text font-medium">备注</span></label>
            <textarea v-model="form.remark" class="textarea textarea-bordered h-24" placeholder="请输入备注信息"></textarea>
          </div>
          <div class="form-control">
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

    <!-- Menu Permission Modal -->
    <dialog id="menu_perm_modal" class="modal">
      <div class="modal-box">
        <h3 class="font-bold text-lg">分配菜单权限 - <span class="text-primary">{{ currentRole?.roleName }}</span></h3>
        <div class="py-4 h-96 overflow-y-auto border border-base-200 rounded-lg mt-4 p-4 bg-base-50">
          <div v-if="loadingMenus" class="flex justify-center items-center h-full">
            <span class="loading loading-spinner text-primary"></span>
          </div>
          <div v-else class="space-y-1">
             <MenuTreeItem 
                v-for="menu in menuTree" 
                :key="menu.id" 
                :menu="menu" 
                :selected-ids="selectedMenuIds"
                @toggle="toggleMenuSelection"
             />
          </div>
        </div>
        <div class="modal-action">
          <form method="dialog">
            <button class="btn btn-ghost">取消</button>
            <button class="btn btn-primary ml-2" @click.prevent="submitMenuPerms" :disabled="submitting">保存</button>
          </form>
        </div>
      </div>
    </dialog>

    <!-- Data Scope Modal -->
    <dialog id="role_dept_modal" class="modal">
      <div class="modal-box max-w-2xl">
        <h3 class="font-bold text-lg">授权部门 - <span class="text-primary">{{ roleDeptRole?.roleName }}</span></h3>
        <div class="space-y-4 mt-4">
          <div class="form-control">
            <label class="label"><span class="label-text font-medium">授权部门</span></label>
            <div class="border border-base-200 rounded-lg p-3 max-h-60 overflow-auto bg-base-50">
              <div v-if="loadingDepts" class="flex items-center justify-center py-6">
                <span class="loading loading-spinner text-primary"></span>
              </div>
              <div v-else class="space-y-1">
                <DeptTreeItem
                  v-for="dept in deptTree"
                  :key="dept.id"
                  :dept="dept"
                  :level="0"
                  :selected-ids="roleDeptForm.deptIds"
                  @toggle="(id:number, checked:boolean) => toggleDeptSelection(roleDeptForm.deptIds, id, checked)"
                />
              </div>
            </div>
            <label class="label"><span class="label-text-alt text-base-content/60">选择多个部门时，权限范围取并集</span></label>
          </div>
        </div>
        <div class="modal-action">
          <form method="dialog">
            <button class="btn btn-ghost">取消</button>
            <button class="btn btn-primary ml-2" @click.prevent="submitRoleDept" :disabled="submitting">保存</button>
          </form>
        </div>
      </div>
    </dialog>

    <!-- Role Delete Modal -->
    <dialog id="role_delete_modal" class="modal">
      <div class="modal-box">
        <h3 class="font-bold text-lg text-error">删除角色</h3>
        <p class="py-2 text-base-content/70">
          确定要删除角色
          <strong v-if="deleteRoleTargets.length === 1">{{ deleteRoleTargets[0].roleName }}</strong>
          <strong v-else>{{ deleteRoleTargets.length }} 个角色</strong>
          吗？
        </p>
        <div class="form-control">
          <label class="label">
            <span class="label-text font-medium">该角色下用户</span>
            <span class="label-text-alt">{{ deleteRoleUserCount }} 人</span>
          </label>
          <div class="border border-base-200 rounded-lg p-3 max-h-40 overflow-auto bg-base-50">
            <div v-if="deleteRoleLoading" class="flex items-center justify-center py-4">
              <span class="loading loading-spinner text-primary"></span>
            </div>
            <div v-else-if="deleteRoleUserCount === 0" class="text-sm text-base-content/60">
              无用户
            </div>
            <div v-else class="text-sm space-y-3">
              <div v-for="role in deleteRoleTargets" :key="role.id">
                <div class="font-medium text-base-content/70 mb-1">{{ role.roleName }}</div>
                <ul class="space-y-1">
                  <li v-for="user in deleteRoleUsersMap[role.id] || []" :key="user.id">
                    {{ user.nickname || user.username }}
                    <span class="text-base-content/50">({{ user.username }})</span>
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
        <div class="form-control mt-3">
          <label class="label cursor-pointer justify-start gap-3">
            <input type="checkbox" class="checkbox checkbox-sm" v-model="deleteWithUsers" />
            <span class="label-text">同时删除该角色下的用户</span>
          </label>
        </div>
        <div class="form-control mt-2">
          <label class="label"><span class="label-text">删除原因（可选）</span></label>
          <textarea v-model="deleteReason" class="textarea textarea-bordered h-20" placeholder="可填写防误删原因"></textarea>
        </div>
        <div class="modal-action">
          <form method="dialog">
            <button class="btn btn-ghost">取消</button>
            <button class="btn btn-error ml-2" @click.prevent="confirmDeleteRole" :disabled="submitting">确认删除</button>
          </form>
        </div>
      </div>
    </dialog>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { getRoleList, createRole, updateRole, deleteRole, getMenuTree, assignRoleMenus, getRoleMenuIds, getRoleUsers, getDeptTreeForRole, assignRoleDepts, getRoleDeptIds } from '@/api/system'
import type { RoleVO, RoleDTO, MenuVO, UserVO, DeptVO } from '@/api/system'
import { defineComponent, h, type PropType } from 'vue'
import { useDialog } from '@/composables/useDialog'

// --- Components ---
// 菜单树组件 - 支持目录/菜单/按钮三级结构
const MenuTreeItem: ReturnType<typeof defineComponent> = defineComponent({
  name: 'MenuTreeItem',
  props: {
    menu: { type: Object as PropType<MenuVO>, required: true },
    selectedIds: { type: Array as PropType<number[]>, required: true },
    level: { type: Number, default: 0 }
  },
  emits: ['toggle'],
  setup(props, { emit }): () => ReturnType<typeof h> {
    const expanded = ref(props.level === 0) // 顶级默认展开
    
    return (): ReturnType<typeof h> => {
      const isChecked = props.selectedIds.includes(props.menu.id)
      const hasChildren = props.menu.children && props.menu.children.length > 0
      const isButton = props.menu.menuType === 'F' || (props.menu as any).type === 2
      const paddingLeft = `${props.level * 20 + 8}px`
      const prefix = props.level > 0 ? `${'|  '.repeat(Math.max(props.level - 1, 0))}|- ` : ''
      
      // 按钮权限（叶子节点，无展开）
      if (isButton || !hasChildren) {
        return h('div', { 
          class: 'flex items-center gap-2 py-1.5 hover:bg-base-200/50 cursor-pointer',
          style: { paddingLeft }
        }, [
          h('span', { class: 'w-4' }), // 占位
          h('input', { 
            type: 'checkbox', 
            class: 'checkbox checkbox-xs checkbox-primary rounded', 
            checked: isChecked,
            onChange: () => emit('toggle', props.menu.id)
          }),
          h('span', { class: 'text-sm select-none' }, props.menu.name)
        ])
      }
      
      // 目录/菜单（有子节点）
      return h('div', {}, [
        // 当前节点
        h('div', { 
          class: 'flex items-center gap-2 py-1.5 hover:bg-base-200/50 cursor-pointer',
          style: { paddingLeft }
        }, [
          // 展开/收起箭头
          h('button', { 
            class: 'w-4 h-4 flex items-center justify-center text-base-content/60 hover:text-primary',
            onClick: () => { expanded.value = !expanded.value }
          }, [
            h('svg', { 
              class: `w-3 h-3 transition-transform ${expanded.value ? 'rotate-90' : ''}`,
              fill: 'none', 
              viewBox: '0 0 24 24', 
              stroke: 'currentColor'
            }, [
              h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M9 5l7 7-7 7' })
            ])
          ]),
          // 复选框
          h('input', { 
            type: 'checkbox', 
            class: 'checkbox checkbox-xs checkbox-primary rounded', 
            checked: isChecked,
            onChange: () => emit('toggle', props.menu.id)
          }),
          // 菜单名称
          h('span', { 
            class: 'text-sm select-none font-medium',
            onClick: () => { expanded.value = !expanded.value }
          }, props.menu.name)
        ]),
        // 子节点
        expanded.value ? h('div', {}, 
          props.menu.children!.map((child: MenuVO) => h(MenuTreeItem, {
            menu: child,
            selectedIds: props.selectedIds,
            level: props.level + 1,
            onToggle: (id: number) => emit('toggle', id)
          }))
        ) : null
      ])
    }
  }
})

const DeptTreeItem: ReturnType<typeof defineComponent> = defineComponent({
  name: 'DeptTreeItem',
  props: {
    dept: { type: Object as PropType<DeptVO>, required: true },
    level: { type: Number, default: 0 },
    selectedIds: { type: Array as PropType<number[]>, required: true }
  },
  emits: ['toggle'],
  setup(props, { emit }) {
    return () => {
      const isChecked = props.selectedIds.includes(props.dept.id)
      const hasChildren = props.dept.children && props.dept.children.length > 0
      const paddingLeft = `${props.level * 20 + 8}px`
      return h('div', {}, [
        h('div', {
          class: 'flex items-center gap-2 py-1 cursor-pointer rounded-md hover:bg-base-200/60',
          style: { paddingLeft }
        }, [
          h('input', {
            type: 'checkbox',
            class: 'checkbox checkbox-sm',
            checked: isChecked,
            onChange: () => emit('toggle', props.dept.id, !isChecked)
          }),
          h('span', { class: props.level === 0 ? 'text-sm font-medium' : 'text-sm text-base-content/70' }, props.dept.deptName)
        ]),
        hasChildren ? h('div', { class: 'border-l border-base-200 ml-3 pl-2' },
          props.dept.children!.map(child => h(DeptTreeItem, {
            dept: child,
            level: props.level + 1,
            selectedIds: props.selectedIds,
            onToggle: (id: number, checked: boolean) => emit('toggle', id, checked)
          }))
        ) : null
      ])
    }
  }
})

// --- State ---
const loading = ref(false)
const submitting = ref(false)
const roleList = ref<RoleVO[]>([])
const total = ref(0)
const loadingMore = ref(false)
const hasMore = ref(true)
const scrollContainer = ref<HTMLElement | null>(null)
const loadMoreTrigger = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null
const menuTree = ref<MenuVO[]>([])
const loadingMenus = ref(false)
const loadingDepts = ref(false)
const deptTree = ref<DeptVO[]>([])
const roleDeptCounts = ref<Record<number, number>>({})

const roleDeptRole = ref<RoleVO | null>(null)
const roleDeptForm = reactive({
  deptIds: [] as number[]
})

const form = reactive<RoleDTO>({
  roleName: '',
  roleKey: '',
  sortOrder: 0,
  status: 0,
  remark: '',
  menuIds: []
})
const isEdit = ref(false)
const currentId = ref<number>(0)
const currentRole = ref<RoleVO | null>(null)
const selectedMenuIds = ref<number[]>([])
const selectedIds = ref<number[]>([])
const deleteRoleTargets = ref<RoleVO[]>([])
const deleteRoleUsersMap = ref<Record<number, UserVO[]>>({})
const deleteRoleLoading = ref(false)
const deleteWithUsers = ref(false)
const deleteReason = ref('')
const dialog = useDialog()
const queryParams = reactive({
  page: 1,
  size: 20,
  keyword: ''
})

const isAllSelected = computed(() => {
  return roleList.value.length > 0 && selectedIds.value.length === roleList.value.length
})
const selectedRole = computed(() => roleList.value.find(r => r.id === selectedIds.value[0]) || null)
const canAssignDept = computed(() => selectedIds.value.length === 1 && !!selectedRole.value && !isAdminRole(selectedRole.value))


onMounted(() => {
  fetchRoles({ reset: true })
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

const fetchRoles = async ({ append = false, reset = false } = {}) => {
  if (append && (loading.value || loadingMore.value)) return
  if (!append && loading.value) return
  if (reset) {
    queryParams.page = 1
    roleList.value = []
    selectedIds.value = []
    hasMore.value = true
  }
  append ? (loadingMore.value = true) : (loading.value = true)
  try {
    const res: any = await getRoleList(queryParams)
    const records = Array.isArray(res) ? res : (res?.records || [])
    total.value = Array.isArray(res) ? records.length : (res?.total || 0)
    if (append) {
      roleList.value = [...roleList.value, ...records]
    } else {
      roleList.value = records
    }
    if (records.length > 0) {
      await loadRoleDeptCounts(records, append)
    }
    if (total.value) {
      hasMore.value = roleList.value.length < total.value
    } else {
      hasMore.value = records.length >= queryParams.size
    }
  } catch (error: any) {
    console.error(error)
    await dialog.alert(error?.response?.data?.message || '获取角色列表失败')
  } finally {
    append ? (loadingMore.value = false) : (loading.value = false)
  }
}

const loadMore = async () => {
  if (!hasMore.value || loading.value || loadingMore.value) return
  queryParams.page += 1
  await fetchRoles({ append: true })
}

const handleSearch = () => {
  fetchRoles({ reset: true })
}

const resetSearch = () => {
  queryParams.keyword = ''
  fetchRoles({ reset: true })
}

const refreshRoles = () => {
  fetchRoles({ reset: true })
}

const openFormModal = (role?: RoleVO) => {
  if (role) {
    isEdit.value = true
    currentId.value = role.id
    form.roleName = role.roleName
    form.roleKey = role.roleKey
    form.sortOrder = role.sortOrder
    form.status = role.status
    form.remark = role.remark
  } else {
    isEdit.value = false
    form.roleName = ''
    form.roleKey = ''
    form.sortOrder = 0
    form.status = 0
    form.remark = ''
  }
  const modal = document.getElementById('role_form_modal') as HTMLDialogElement
  modal.showModal()
}

const submitForm = async () => {
  submitting.value = true
  try {
    const payload: RoleDTO = {
      roleName: form.roleName.trim(),
      roleKey: form.roleKey.trim(),
      sortOrder: form.sortOrder,
      status: form.status,
      remark: form.remark?.trim() || undefined
    }
    if (isEdit.value) {
      await updateRole(currentId.value, payload)
    } else {
      await createRole(payload)
    }
    const modal = document.getElementById('role_form_modal') as HTMLDialogElement
    modal.close()
    refreshRoles()
  } catch (error: any) {
    console.error(error)
    await dialog.alert(error?.response?.data?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (role: RoleVO) => {
  deleteRoleTargets.value = [role]
  deleteWithUsers.value = false
  deleteReason.value = ''
  deleteRoleUsersMap.value = {}
  deleteRoleLoading.value = true
  const modal = document.getElementById('role_delete_modal') as HTMLDialogElement
  modal.showModal()
  try {
    const res: any = await getRoleUsers(role.id)
    deleteRoleUsersMap.value = { [role.id]: res || [] }
  } catch (error: any) {
    console.error(error)
    await dialog.alert(error?.response?.data?.message || '获取角色用户失败')
  } finally {
    deleteRoleLoading.value = false
  }
}

const handleBatchDelete = async () => {
  const targets = roleList.value.filter(r => selectedIds.value.includes(r.id))
  if (targets.length === 0) return
  deleteRoleTargets.value = targets
  deleteWithUsers.value = false
  deleteReason.value = ''
  deleteRoleUsersMap.value = {}
  deleteRoleLoading.value = true
  const modal = document.getElementById('role_delete_modal') as HTMLDialogElement
  modal.showModal()
  try {
    const results = await Promise.all(
      targets.map(async role => {
        const users = await getRoleUsers(role.id)
        return { roleId: role.id, users: users || [] }
      })
    )
    const map: Record<number, UserVO[]> = {}
    results.forEach(r => { map[r.roleId] = r.users })
    deleteRoleUsersMap.value = map
  } catch (error: any) {
    console.error(error)
    await dialog.alert(error?.response?.data?.message || '获取角色用户失败')
  } finally {
    deleteRoleLoading.value = false
  }
}

const confirmDeleteRole = async () => {
  if (deleteRoleTargets.value.length === 0) return
  submitting.value = true
  try {
    const failed: number[] = []
    for (const role of deleteRoleTargets.value) {
      try {
        await deleteRole(role.id, {
          deleteUsers: deleteWithUsers.value,
          reason: deleteReason.value?.trim() || undefined
        })
      } catch {
        failed.push(role.id)
      }
    }
    roleList.value = roleList.value.filter(r => !deleteRoleTargets.value.some(t => t.id === r.id))
    selectedIds.value = []
    const modal = document.getElementById('role_delete_modal') as HTMLDialogElement
    modal.close()
    if (failed.length > 0) {
      await dialog.alert(`部分角色删除失败：${failed.join(', ')}`)
    }
    refreshRoles()
  } catch (error: any) {
    console.error(error)
    await dialog.alert(error?.response?.data?.message || '删除失败')
  } finally {
    submitting.value = false
  }
}

const deleteRoleUserCount = computed(() => {
  return Object.values(deleteRoleUsersMap.value).reduce((sum, list) => sum + list.length, 0)
})

const openMenuModal = async (role: RoleVO) => {
  currentRole.value = role
  selectedMenuIds.value = []
  const modal = document.getElementById('menu_perm_modal') as HTMLDialogElement
  modal.showModal()
  
  if (menuTree.value.length === 0) {
    loadingMenus.value = true
    try {
      const res: any = await getMenuTree()
      menuTree.value = res || []
    } catch (error) {
      console.error(error)
    } finally {
      loadingMenus.value = false
    }
  }
  try {
    const res: any = await getRoleMenuIds(role.id)
    selectedMenuIds.value = normalizeMenuSelection(menuTree.value, res || [])
  } catch (error: any) {
    console.error(error)
    await dialog.alert(error?.response?.data?.message || '获取角色菜单失败')
  }
}

const openRoleDeptModal = async (role: RoleVO) => {
  roleDeptRole.value = role
  roleDeptForm.deptIds = []
  const modal = document.getElementById('role_dept_modal') as HTMLDialogElement
  modal.showModal()
  
  await ensureDeptTree()
  await fetchRoleDeptIds(role.id, roleDeptForm)
}

const submitRoleDept = async () => {
  if (!roleDeptRole.value) return
  submitting.value = true
  try {
    const updated = await assignRoleDepts(
      roleDeptRole.value.id,
      roleDeptForm.deptIds
    )
    const idx = roleList.value.findIndex(r => r.id === roleDeptRole.value?.id)
    if (idx !== -1) {
      roleList.value[idx] = updated
    }
    roleDeptCounts.value[roleDeptRole.value.id] = roleDeptForm.deptIds.length
    const modal = document.getElementById('role_dept_modal') as HTMLDialogElement
    modal.close()
  } catch (error: any) {
    console.error(error)
    await dialog.alert(error?.response?.data?.message || '保存授权部门失败')
  } finally {
    submitting.value = false
  }
}

const ensureDeptTree = async () => {
  if (deptTree.value.length > 0) return
  loadingDepts.value = true
  try {
    const res: any = await getDeptTreeForRole()
    deptTree.value = res || []
  } catch (error) {
    console.error(error)
  } finally {
    loadingDepts.value = false
  }
}

const fetchRoleDeptIds = async (roleId: number, target?: { deptIds: number[] }) => {
  if (!roleId) return
  try {
    const res: any = await getRoleDeptIds(roleId)
    if (target) {
      target.deptIds = res || []
    } else {
      form.deptIds = res || []
    }
  } catch (error: any) {
    console.error(error)
  }
}

const findDeptById = (nodes: DeptVO[], id: number): DeptVO | null => {
  for (const node of nodes) {
    if (node.id === id) return node
    if (node.children && node.children.length > 0) {
      const found = findDeptById(node.children, id)
      if (found) return found
    }
  }
  return null
}

const collectDeptIds = (dept: DeptVO, target: number[] = []) => {
  target.push(dept.id)
  if (dept.children && dept.children.length > 0) {
    for (const child of dept.children) {
      collectDeptIds(child, target)
    }
  }
  return target
}

const toggleDeptSelection = (target: number[] | undefined, id: number, checked: boolean) => {
  if (!target) return
  const dept = findDeptById(deptTree.value, id)
  const ids = dept ? collectDeptIds(dept) : [id]
  if (checked) {
    for (const deptId of ids) {
      if (!target.includes(deptId)) {
        target.push(deptId)
      }
    }
  } else {
    for (const deptId of ids) {
      const index = target.indexOf(deptId)
      if (index !== -1) {
        target.splice(index, 1)
      }
    }
  }
}

const findMenuPath = (menus: MenuVO[], targetId: number, path: MenuVO[] = []): MenuVO[] | null => {
  for (const menu of menus) {
    const nextPath = [...path, menu]
    if (menu.id === targetId) {
      return nextPath
    }
    if (menu.children && menu.children.length > 0) {
      const found = findMenuPath(menu.children, targetId, nextPath)
      if (found) {
        return found
      }
    }
  }
  return null
}

const collectMenuIds = (menu: MenuVO): number[] => {
  const ids = [menu.id]
  if (menu.children && menu.children.length > 0) {
    for (const child of menu.children) {
      ids.push(...collectMenuIds(child))
    }
  }
  return ids
}

const hasSelectedDescendant = (menu: MenuVO, selected: Set<number>): boolean => {
  if (!menu.children || menu.children.length === 0) {
    return false
  }
  for (const child of menu.children) {
    if (selected.has(child.id) || hasSelectedDescendant(child, selected)) {
      return true
    }
  }
  return false
}

const normalizeMenuSelection = (menus: MenuVO[], selectedIds: number[]): number[] => {
  const selected = new Set(selectedIds)
  const walk = (menu: MenuVO): boolean => {
    if (!menu.children || menu.children.length === 0) {
      return selected.has(menu.id)
    }
    let childSelected = false
    for (const child of menu.children) {
      if (walk(child)) {
        childSelected = true
      }
    }
    if (childSelected) {
      selected.add(menu.id)
    }
    return selected.has(menu.id) || childSelected
  }
  for (const menu of menus) {
    walk(menu)
  }
  return Array.from(selected)
}

const toggleMenuSelection = (id: number) => {
  const path = findMenuPath(menuTree.value, id) || []
  const target = path.length > 0 ? path[path.length - 1] : null
  const ids = target ? collectMenuIds(target) : [id]
  const selected = new Set(selectedMenuIds.value)
  const isSelected = selected.has(id)

  if (isSelected) {
    ids.forEach(menuId => selected.delete(menuId))
  } else {
    ids.forEach(menuId => selected.add(menuId))
  }

  // 同步父级勾选状态：有子级勾选则父级勾选，全部取消则父级取消
  if (path.length > 1) {
    const parents = path.slice(0, -1)
    for (const parent of parents) {
      if (hasSelectedDescendant(parent, selected)) {
        selected.add(parent.id)
      } else {
        selected.delete(parent.id)
      }
    }
  }

  selectedMenuIds.value = Array.from(selected)
}

const submitMenuPerms = async () => {
  if (!currentRole.value) return
  submitting.value = true
  try {
    const updated = await assignRoleMenus(currentRole.value.id, selectedMenuIds.value)
    currentRole.value.menuIds = [...selectedMenuIds.value]
    const idx = roleList.value.findIndex(r => r.id === currentRole.value?.id)
    if (idx !== -1) {
      roleList.value[idx] = updated
    }
    
    const modal = document.getElementById('menu_perm_modal') as HTMLDialogElement
    modal.close()
  } catch (error: any) {
    console.error(error)
    await dialog.alert(error?.response?.data?.message || '分配菜单失败')
  } finally {
    submitting.value = false
  }
}

const handleEdit = () => {
    const role = roleList.value.find(r => r.id === selectedIds.value[0])
    if (role) openFormModal(role)
}

const handleAssignMenu = () => {
    const role = roleList.value.find(r => r.id === selectedIds.value[0])
    if (role) openMenuModal(role)
}

const handleAssignDept = () => {
    const role = roleList.value.find(r => r.id === selectedIds.value[0])
    if (role) openRoleDeptModal(role)
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
    selectedIds.value = roleList.value.map(u => u.id)
  }
}

const handleStatusChange = async (role: RoleVO) => {
  const newStatus = role.status === 0 ? 1 : 0
  const actionName = newStatus === 1 ? '停用' : '启用'
  
  if (!await dialog.confirm(`确定要${actionName}角色 ${role.roleName} 吗？`)) {
    refreshRoles()
    return
  }
  
  try {
    const updated = await updateRole(role.id, {
      roleName: role.roleName,
      roleKey: role.roleKey,
      sortOrder: role.sortOrder,
      status: newStatus,
      remark: role.remark
    })
    const idx = roleList.value.findIndex(r => r.id === role.id)
    if (idx !== -1) {
      roleList.value[idx] = updated
    }
  } catch (error: any) {
    console.error(error)
    await dialog.alert(error?.response?.data?.message || '更新角色状态失败')
    refreshRoles()
  }
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

const getRoleDeptCount = (roleId: number) => {
  return roleDeptCounts.value[roleId] || 0
}

const loadRoleDeptCounts = async (roles: RoleVO[], append = false) => {
  const map: Record<number, number> = append ? { ...roleDeptCounts.value } : {}
  await Promise.all(
    roles.map(async role => {
      if (isAdminRole(role)) {
        map[role.id] = 0
        return
      }
      try {
        const res: any = await getRoleDeptIds(role.id)
        map[role.id] = Array.isArray(res) ? res.length : 0
      } catch (error) {
        console.error(error)
        map[role.id] = 0
      }
    })
  )
  roleDeptCounts.value = map
}

const isAdminRole = (role: RoleVO) => {
  return role.id === 1 || role.roleKey === 'admin'
}

</script>

<style scoped>
.role-table {
  border-collapse: collapse;
  width: 100%;
}
.role-table th,
.role-table td {
  border-bottom: 1px solid hsl(var(--b2) / 0.8);
  border-right: 1px solid hsl(var(--b2) / 0.8);
}
.role-table th:last-child,
.role-table td:last-child {
  border-right: 0;
}
.role-table tbody tr:last-child th,
.role-table tbody tr:last-child td {
  border-bottom: 0;
}
</style>
