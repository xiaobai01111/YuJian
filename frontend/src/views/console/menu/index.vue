<template>
  <div class="card bg-base-100 shadow-xl">
    <div class="card-body">
      <div class="flex justify-between items-center mb-4">
        <h2 class="card-title">菜单管理</h2>
        <button class="btn btn-primary" @click="openFormModal()" v-permission="['system:menu:add']">新增菜单</button>
      </div>

      <div class="overflow-x-auto min-h-[400px]">
        <table class="table table-zebra">
          <thead>
            <tr>
              <th>名称</th>
              <th>排序</th>
              <th>组件路径</th>
              <th>权限字符</th>
              <th>类型</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="6" class="text-center py-4">加载中...</td>
            </tr>
            <template v-else>
              <!-- Flattened tree rendering logic needed or use recursive component -->
              <!-- For table, flattening the tree with level is easiest -->
              <tr v-for="menu in flattenedMenuList" :key="menu.id">
                <td>
                  <div :style="{ paddingLeft: (menu.level * 20) + 'px' }" class="flex items-center gap-2">
                    <span v-if="menu.children?.length" @click="toggleExpand(menu.id)" class="cursor-pointer font-bold w-4 text-center select-none">
                      {{ expandedIds.includes(menu.id) ? 'v' : '>' }}
                    </span>
                    <span v-else class="w-4"></span>
                    <svg v-if="menu.icon" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                       <!-- Simple icon logic -->
                       <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
                    </svg>
                    {{ menu.name }}
                  </div>
                </td>
                <td>{{ menu.order }}</td>
                <td>{{ menu.component || '-' }}</td>
                <td><kbd v-if="menu.perms" class="kbd kbd-sm">{{ menu.perms }}</kbd></td>
                <td>
                   <span class="badge badge-sm" :class="getTypeClass(menu.menuType)">{{ getTypeLabel(menu.menuType) }}</span>
                </td>
                <td>
                  <button class="btn btn-ghost btn-xs text-primary" @click="openFormModal(undefined, menu.id)" v-permission="['system:menu:add']">新增子项</button>
                  <button class="btn btn-ghost btn-xs" @click="openFormModal(menu)" v-permission="['system:menu:edit']">修改</button>
                  <button class="btn btn-ghost btn-xs text-error" @click="handleDelete(menu)" v-permission="['system:menu:remove']">删除</button>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Menu Form Modal -->
    <dialog id="menu_form_modal" class="modal">
      <div class="modal-box w-11/12 max-w-2xl">
        <h3 class="font-bold text-lg">{{ isEdit ? '编辑菜单' : '新增菜单' }}</h3>
        
        <div class="grid grid-cols-2 gap-4 mt-4">
          <div class="form-control">
            <label class="label"><span class="label-text">上级菜单</span></label>
            <!-- Should be a tree select, simple select for now -->
            <select class="select select-bordered w-full" v-model="form.parentId">
              <option :value="0">主目录</option>
              <option v-for="menu in flattenedAllMenus" :key="menu.id" :value="menu.id">
                {{ ' '.repeat(menu.level * 2) }}{{ menu.name }}
              </option>
            </select>
          </div>
          
          <div class="form-control">
            <label class="label"><span class="label-text">菜单类型</span></label>
            <div class="join">
              <input class="join-item btn" type="radio" name="options" aria-label="目录" value="M" v-model="form.menuType" />
              <input class="join-item btn" type="radio" name="options" aria-label="菜单" value="C" v-model="form.menuType" />
              <input class="join-item btn" type="radio" name="options" aria-label="按钮" value="F" v-model="form.menuType" />
            </div>
          </div>

          <div class="form-control">
            <label class="label"><span class="label-text">菜单名称</span></label>
            <input type="text" v-model="form.menuName" class="input input-bordered w-full" />
          </div>

          <div class="form-control">
            <label class="label"><span class="label-text">显示排序</span></label>
            <input type="number" v-model="form.orderNum" class="input input-bordered w-full" />
          </div>

          <template v-if="form.menuType !== 'F'">
            <div class="form-control">
              <label class="label"><span class="label-text">路由地址</span></label>
              <input type="text" v-model="form.path" placeholder="例如：user" class="input input-bordered w-full" />
            </div>
            
            <div class="form-control" v-if="form.menuType === 'C'">
              <label class="label"><span class="label-text">组件路径</span></label>
              <input type="text" v-model="form.component" placeholder="例如：console/user/index" class="input input-bordered w-full" />
            </div>

            <div class="form-control">
              <label class="label"><span class="label-text">图标</span></label>
              <input type="text" v-model="form.icon" class="input input-bordered w-full" />
            </div>
          </template>

          <div class="form-control">
            <label class="label"><span class="label-text">权限字符</span></label>
            <input type="text" v-model="form.perms" placeholder="例如：system:user:list" class="input input-bordered w-full" />
          </div>
          
          <div class="form-control">
            <label class="label cursor-pointer justify-start gap-4">
              <span class="label-text">状态</span>
              <input type="checkbox" class="toggle" :checked="form.status" @change="form.status = ($event.target as HTMLInputElement).checked" />
              <span class="label-text">{{ form.status ? '正常' : '停用' }}</span>
            </label>
          </div>
          
          <div class="form-control" v-if="form.menuType !== 'F'">
            <label class="label cursor-pointer justify-start gap-4">
              <span class="label-text">显示</span>
              <input type="checkbox" class="toggle" :checked="form.visible" @change="form.visible = ($event.target as HTMLInputElement).checked" />
              <span class="label-text">{{ form.visible ? '显示' : '隐藏' }}</span>
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
import { ref, reactive, onMounted, computed } from 'vue'
import { getMenuTree, createMenu, updateMenu, deleteMenu } from '@/api/system'
import type { MenuVO, MenuDTO } from '@/api/system'

const loading = ref(false)
const submitting = ref(false)
const menuTree = ref<MenuVO[]>([])
const expandedIds = ref<number[]>([])

const form = reactive<MenuDTO>({
  menuName: '',
  parentId: 0,
  orderNum: 0,
  path: '',
  component: '',
  query: '',
  isFrame: false,
  isCache: false,
  menuType: 'M',
  visible: true,
  status: true,
  perms: '',
  icon: ''
})
const isEdit = ref(false)
const currentId = ref<number>(0)

onMounted(() => {
  fetchMenus()
})

const fetchMenus = async () => {
  loading.value = true
  try {
    const res: any = await getMenuTree()
    menuTree.value = res || []
    // Expand top level by default
    expandedIds.value = menuTree.value.map(m => m.id)
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// Flatten tree for table, considering expanded state
const flattenedMenuList = computed(() => {
  const result: (MenuVO & { level: number })[] = []
  
  const traverse = (nodes: MenuVO[], level: number) => {
    for (const node of nodes) {
      result.push({ ...node, level })
      if (node.children && node.children.length > 0 && expandedIds.value.includes(node.id)) {
        traverse(node.children, level + 1)
      }
    }
  }
  
  traverse(menuTree.value, 0)
  return result
})

// Flatten all menus for select (ignoring expanded)
const flattenedAllMenus = computed(() => {
  const result: (MenuVO & { level: number })[] = []
  const traverse = (nodes: MenuVO[], level: number) => {
    for (const node of nodes) {
      result.push({ ...node, level })
      if (node.children) {
        traverse(node.children, level + 1)
      }
    }
  }
  traverse(menuTree.value, 0)
  return result
})

const toggleExpand = (id: number) => {
  const index = expandedIds.value.indexOf(id)
  if (index === -1) {
    expandedIds.value.push(id)
  } else {
    expandedIds.value.splice(index, 1)
  }
}

const getTypeLabel = (type: string) => {
  const map: Record<string, string> = { M: '目录', C: '菜单', F: '按钮' }
  return map[type] || type
}

const getTypeClass = (type: string) => {
  const map: Record<string, string> = { M: 'badge-ghost', C: 'badge-primary', F: 'badge-outline' }
  return map[type] || 'badge-ghost'
}

const openFormModal = (menu?: MenuVO, parentId?: number) => {
  if (menu) {
    isEdit.value = true
    currentId.value = menu.id
    form.menuName = menu.name
    form.parentId = menu.parentId
    form.orderNum = menu.order
    form.path = menu.path
    form.component = menu.component
    form.query = menu.query
    form.isFrame = menu.isFrame
    form.isCache = menu.isCache
    form.menuType = menu.menuType
    form.visible = menu.visible
    form.status = menu.status
    form.perms = menu.perms
    form.icon = menu.icon
  } else {
    isEdit.value = false
    form.menuName = ''
    form.parentId = parentId || 0
    form.orderNum = 0
    form.path = ''
    form.component = ''
    form.query = ''
    form.isFrame = false
    form.isCache = false
    form.menuType = 'C'
    form.visible = true
    form.status = true
    form.perms = ''
    form.icon = ''
  }
  const modal = document.getElementById('menu_form_modal') as HTMLDialogElement
  modal.showModal()
}

const submitForm = async () => {
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateMenu(currentId.value, form)
    } else {
      await createMenu(form)
    }
    const modal = document.getElementById('menu_form_modal') as HTMLDialogElement
    modal.close()
    fetchMenus()
  } catch (error) {
    console.error(error)
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (menu: MenuVO) => {
  if (!confirm(`确定要删除菜单 ${menu.name} 吗？`)) return
  try {
    await deleteMenu(menu.id)
    fetchMenus()
  } catch (error) {
    console.error(error)
  }
}
</script>
