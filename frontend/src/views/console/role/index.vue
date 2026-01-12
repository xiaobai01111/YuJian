<template>
  <div class="h-full flex flex-col">
    <div class="card bg-base-100 shadow-sm border border-base-200 flex-1 flex flex-col">
      <div class="card-body p-4 flex-1 flex flex-col overflow-hidden">
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
          </div>

          <div class="flex gap-2">
            <button class="btn btn-circle btn-ghost btn-sm" @click="fetchRoles">
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
                <th>角色名称</th>
                <th>角色编号</th>
                <th>角色状态</th>
                <th>角色排序</th>
                <th>角色备注</th>
                <th>创建时间</th>
                <th class="text-center w-40">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="9" class="text-center py-10">
                   <span class="loading loading-spinner loading-lg text-primary"></span>
                </td>
              </tr>
              <tr v-else v-for="(role, index) in roleList" :key="role.id" class="hover border-b border-base-100 last:border-0">
                <th>
                  <label>
                    <input type="checkbox" class="checkbox checkbox-sm rounded-sm" :checked="selectedIds.includes(role.id)" @change="toggleSelection(role.id)" />
                  </label>
                </th>
                <td class="text-base-content/60">{{ index + 1 }}</td>
                <td class="font-medium">{{ role.roleName }}</td>
                <td>{{ role.roleKey }}</td>
                <td>
                  <input type="checkbox" class="toggle toggle-primary toggle-sm" :checked="role.status === 0" :disabled="isAdminRole(role)" @change="handleStatusChange(role)" />
                </td>
                <td>{{ role.sortOrder }}</td>
                <td class="text-base-content/60 max-w-xs truncate" :title="role.remark">{{ role.remark || '-' }}</td>
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
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        
        <!-- Pagination -->
        <div class="flex justify-between items-center mt-6 border-t border-base-200 pt-4 flex-none">
          <div class="text-sm text-base-content/60">
            共 {{ roleList.length }} 条
          </div>
           <!-- Mock Pagination for now since API might not return pagination wrapper yet -->
          <div class="join">
            <button class="join-item btn btn-sm btn-ghost" disabled>«</button>
            <button class="join-item btn btn-sm btn-primary">1</button>
            <button class="join-item btn btn-sm btn-ghost" disabled>»</button>
          </div>
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

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { getRoleList, createRole, updateRole, deleteRole, getMenuTree, assignRoleMenus } from '@/api/system'
import type { RoleVO, RoleDTO, MenuVO } from '@/api/system'
import { defineComponent, h, type PropType } from 'vue'

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

// --- State ---
const loading = ref(false)
const submitting = ref(false)
const roleList = ref<RoleVO[]>([])
const menuTree = ref<MenuVO[]>([])
const loadingMenus = ref(false)

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

const isAllSelected = computed(() => {
  return roleList.value.length > 0 && selectedIds.value.length === roleList.value.length
})

onMounted(() => {
  fetchRoles()
})

const fetchRoles = async () => {
  loading.value = true
  selectedIds.value = []
  try {
    const res: any = await getRoleList()
    roleList.value = (res || []).map((role: RoleVO) => ({
        ...role,
        remark: role.remark || '暂无备注'
    }))
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
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
    if (isEdit.value) {
      await updateRole(currentId.value, form)
    } else {
      await createRole(form)
    }
    const modal = document.getElementById('role_form_modal') as HTMLDialogElement
    modal.close()
    fetchRoles()
  } catch (error) {
    console.error(error)
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (role: RoleVO) => {
  if (!confirm(`确定要删除角色 ${role.roleName} 吗？`)) return
  try {
    await deleteRole(role.id)
    fetchRoles()
  } catch (error) {
    console.error(error)
  }
}

const handleBatchDelete = async () => {
  if (!confirm(`确定要删除选中的 ${selectedIds.value.length} 个角色吗？`)) return
  try {
    for (const id of selectedIds.value) {
      await deleteRole(id)
    }
    fetchRoles()
  } catch (error) {
    console.error(error)
  }
}

const openMenuModal = async (role: RoleVO) => {
  currentRole.value = role
  selectedMenuIds.value = role.menuIds || []
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
}

const toggleMenuSelection = (id: number) => {
  const index = selectedMenuIds.value.indexOf(id)
  if (index === -1) {
    selectedMenuIds.value.push(id)
  } else {
    selectedMenuIds.value.splice(index, 1)
  }
}

const submitMenuPerms = async () => {
  if (!currentRole.value) return
  submitting.value = true
  try {
    await assignRoleMenus(currentRole.value.id, selectedMenuIds.value)
    currentRole.value.menuIds = [...selectedMenuIds.value]
    
    const modal = document.getElementById('menu_perm_modal') as HTMLDialogElement
    modal.close()
  } catch (error) {
    console.error(error)
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
  
  if (!confirm(`确定要${actionName}角色 ${role.roleName} 吗？`)) {
    fetchRoles()
    return
  }
  
  try {
    await updateRole(role.id, {
      roleName: role.roleName,
      roleKey: role.roleKey,
      sortOrder: role.sortOrder,
      status: newStatus,
      remark: role.remark
    })
    role.status = newStatus
  } catch (error) {
    console.error(error)
    fetchRoles()
  }
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

const isAdminRole = (role: RoleVO) => {
  return role.id === 1 || role.roleKey === 'admin'
}
</script>
