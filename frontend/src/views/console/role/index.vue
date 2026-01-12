<template>
  <div class="card bg-base-100 shadow-xl">
    <div class="card-body">
      <div class="flex justify-between items-center mb-4">
        <h2 class="card-title">角色管理</h2>
        <button class="btn btn-primary" @click="openFormModal()" v-permission="['system:role:add']">新增角色</button>
      </div>

      <div class="overflow-x-auto min-h-[400px]">
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>角色名称</th>
              <th>权限字符</th>
              <th>排序</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="6" class="text-center py-4">加载中...</td>
            </tr>
            <tr v-else v-for="role in roleList" :key="role.id">
              <td>{{ role.id }}</td>
              <td>{{ role.roleName }}</td>
              <td><kbd class="kbd kbd-sm">{{ role.roleKey }}</kbd></td>
              <td>{{ role.sortOrder }}</td>
              <td>
                <span :class="['badge', role.status === 0 ? 'badge-success' : 'badge-ghost']">
                  {{ role.status === 0 ? '正常' : '停用' }}
                </span>
              </td>
              <td>
                <button class="btn btn-ghost btn-xs text-primary" @click="openFormModal(role)" v-permission="['system:role:edit']">编辑</button>
                <button class="btn btn-ghost btn-xs text-info" @click="openMenuModal(role)" v-permission="['system:role:edit']">菜单权限</button>
                <button class="btn btn-ghost btn-xs text-error" @click="handleDelete(role)" v-permission="['system:role:remove']">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Role Form Modal -->
    <dialog id="role_form_modal" class="modal">
      <div class="modal-box">
        <h3 class="font-bold text-lg">{{ isEdit ? '编辑角色' : '新增角色' }}</h3>
        <div class="form-control w-full mt-4">
          <label class="label"><span class="label-text">角色名称</span></label>
          <input type="text" v-model="form.roleName" placeholder="例如：普通用户" class="input input-bordered w-full" />
        </div>
        <div class="form-control w-full mt-2">
          <label class="label"><span class="label-text">权限字符</span></label>
          <input type="text" v-model="form.roleKey" placeholder="例如：user" class="input input-bordered w-full" />
        </div>
        <div class="form-control w-full mt-2">
          <label class="label"><span class="label-text">排序</span></label>
          <input type="number" v-model="form.sortOrder" class="input input-bordered w-full" />
        </div>
        <div class="form-control mt-2">
          <label class="label cursor-pointer justify-start gap-4">
            <span class="label-text">状态</span>
            <input type="checkbox" class="toggle" :checked="form.status === 0" @change="form.status = ($event.target as HTMLInputElement).checked ? 0 : 1" />
            <span class="label-text">{{ form.status === 0 ? '正常' : '停用' }}</span>
          </label>
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
        <h3 class="font-bold text-lg">分配菜单权限 - {{ currentRole?.roleName }}</h3>
        <div class="py-4 h-96 overflow-y-auto border rounded-lg mt-2 p-4">
          <!-- Simplified Tree View using nested details/summary or recursion -->
          <!-- For simplicity in this iteration, flat list with indentation if tree component complex -->
          <!-- Better: Use a simple recursive component or flat loop if levels shallow -->
          <div v-if="loadingMenus" class="text-center">加载菜单...</div>
          <div v-else>
             <!-- Recursive Tree Component can be here, but let's do a simple recursive loop helper or component -->
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
import { ref, reactive, onMounted } from 'vue'
import { getRoleList, createRole, updateRole, deleteRole, getMenuTree, assignRoleMenus } from '@/api/system'
import type { RoleVO, RoleDTO, MenuVO } from '@/api/system'
// Simple Tree Item Component (Inline)
import { defineComponent, h, PropType } from 'vue'

const MenuTreeItem = defineComponent({
  props: {
    menu: { type: Object as PropType<MenuVO>, required: true },
    selectedIds: { type: Array as PropType<number[]>, required: true }
  },
  emits: ['toggle'],
  setup(props, { emit }) {
    return () => {
      const isChecked = props.selectedIds.includes(props.menu.id)
      const hasChildren = props.menu.children && props.menu.children.length > 0
      
      return h('div', { class: 'pl-4' }, [
        h('div', { class: 'flex items-center gap-2 py-1' }, [
          h('input', { 
            type: 'checkbox', 
            class: 'checkbox checkbox-xs', 
            checked: isChecked,
            onChange: () => emit('toggle', props.menu.id)
          }),
          h('span', props.menu.name)
        ]),
        hasChildren ? props.menu.children!.map(child => h(MenuTreeItem, {
          menu: child,
          selectedIds: props.selectedIds,
          onToggle: (id: number) => emit('toggle', id)
        })) : null
      ])
    }
  }
})

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
  menuIds: []
})
const isEdit = ref(false)
const currentId = ref<number>(0)
const currentRole = ref<RoleVO | null>(null)
const selectedMenuIds = ref<number[]>([])

onMounted(() => {
  fetchRoles()
})

const fetchRoles = async () => {
  loading.value = true
  try {
    const res: any = await getRoleList()
    roleList.value = res || []
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
  } else {
    isEdit.value = false
    form.roleName = ''
    form.roleKey = ''
    form.sortOrder = 0
    form.status = 0
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
    // Update local list to reflect changes without full refresh if possible, or just refresh
    // For simplicity, update local role's menuIds
    currentRole.value.menuIds = [...selectedMenuIds.value]
    
    const modal = document.getElementById('menu_perm_modal') as HTMLDialogElement
    modal.close()
  } catch (error) {
    console.error(error)
  } finally {
    submitting.value = false
  }
}
</script>
