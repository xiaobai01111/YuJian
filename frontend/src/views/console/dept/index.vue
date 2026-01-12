<template>
  <div class="card bg-base-100 shadow-xl">
    <div class="card-body">
      <div class="flex justify-between items-center mb-4">
        <h2 class="card-title">部门管理</h2>
        <button class="btn btn-primary btn-sm" @click="() => openFormModal()">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          新增部门
        </button>
      </div>

      <div class="overflow-x-auto min-h-[400px]">
        <table class="table table-zebra">
          <thead>
            <tr>
              <th>部门名称</th>
              <th>负责人</th>
              <th>联系电话</th>
              <th>邮箱</th>
              <th>排序</th>
              <th>状态</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="8" class="text-center py-4">加载中...</td>
            </tr>
            <tr v-else-if="flatDeptList.length === 0">
              <td colspan="8" class="text-center py-4">暂无数据</td>
            </tr>
            <template v-else v-for="dept in flatDeptList" :key="dept.id">
              <tr class="hover">
                <td>
                  <div class="flex items-center gap-1" :style="{ paddingLeft: (dept.level ?? 0) * 16 + 'px' }">
                    <span v-if="dept.hasChildren" class="cursor-pointer text-xs opacity-60" @click="toggleExpand(dept.id)">
                      {{ expandedIds.includes(dept.id) ? '▼' : '▶' }}
                    </span>
                    <span v-else class="w-3"></span>
                    <span>{{ dept.deptName }}</span>
                  </div>
                </td>
                <td>{{ dept.leader || '-' }}</td>
                <td>{{ dept.phone || '-' }}</td>
                <td>{{ dept.email || '-' }}</td>
                <td>{{ dept.sortOrder }}</td>
                <td>
                  <span :class="['badge badge-sm', dept.status === 0 ? 'badge-success' : 'badge-error']">
                    {{ dept.status === 0 ? '正常' : '停用' }}
                  </span>
                </td>
                <td class="text-sm text-slate-500">{{ formatDate(dept.createdAt) }}</td>
                <td>
                  <div class="flex gap-1">
                    <button class="btn btn-ghost btn-xs text-primary" @click="openFormModal(dept)" title="编辑">编辑</button>
                    <button class="btn btn-ghost btn-xs text-success" @click="openFormModal(undefined, dept.id)" title="新增子部门">新增</button>
                    <button class="btn btn-ghost btn-xs text-error" @click="handleDelete(dept)" title="删除">删除</button>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Add/Edit Modal -->
    <dialog id="dept_modal" class="modal">
      <div class="modal-box">
        <h3 class="font-bold text-lg">{{ isEdit ? '编辑部门' : '新增部门' }}</h3>
        <div class="form-control mt-4">
          <label class="label"><span class="label-text">上级部门</span></label>
          <select v-model="form.parentId" class="select select-bordered">
            <option :value="0">无（顶级部门）</option>
            <option v-for="d in allDepts" :key="d.id" :value="d.id" :disabled="isEdit && d.id === currentId">
              {{ '─'.repeat(d.level || 0) }} {{ d.deptName }}
            </option>
          </select>
        </div>
        <div class="form-control mt-2">
          <label class="label"><span class="label-text">部门名称 <span class="text-error">*</span></span></label>
          <input v-model="form.deptName" type="text" class="input input-bordered" placeholder="请输入部门名称" />
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div class="form-control mt-2">
            <label class="label"><span class="label-text">负责人</span></label>
            <input v-model="form.leader" type="text" class="input input-bordered" placeholder="请输入负责人" />
          </div>
          <div class="form-control mt-2">
            <label class="label"><span class="label-text">联系电话</span></label>
            <input v-model="form.phone" type="text" class="input input-bordered" placeholder="请输入联系电话" />
          </div>
        </div>
        <div class="form-control mt-2">
          <label class="label"><span class="label-text">邮箱</span></label>
          <input v-model="form.email" type="email" class="input input-bordered" placeholder="请输入邮箱" />
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div class="form-control mt-2">
            <label class="label"><span class="label-text">排序</span></label>
            <input v-model.number="form.sortOrder" type="number" class="input input-bordered" placeholder="请输入排序" />
          </div>
          <div class="form-control mt-2">
            <label class="label"><span class="label-text">状态</span></label>
            <select v-model="form.status" class="select select-bordered">
              <option :value="0">正常</option>
              <option :value="1">停用</option>
            </select>
          </div>
        </div>
        <div class="modal-action">
          <form method="dialog">
            <button class="btn btn-ghost">取消</button>
            <button class="btn btn-primary ml-2" @click.prevent="submitForm" :disabled="submitting">确定</button>
          </form>
        </div>
      </div>
    </dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { getDeptList, createDept, updateDept, deleteDept, type DeptVO, type DeptDTO } from '@/api/system'

interface FlatDept extends DeptVO {
  level: number
  hasChildren: boolean
}

const loading = ref(false)
const submitting = ref(false)
const deptTree = ref<DeptVO[]>([])
const allDepts = ref<FlatDept[]>([])
const expandedIds = ref<number[]>([])
const isEdit = ref(false)
const currentId = ref<number>(0)

const form = reactive<DeptDTO & { id?: number }>({
  parentId: 0,
  deptName: '',
  sortOrder: 0,
  leader: '',
  phone: '',
  email: '',
  status: 0
})

onMounted(() => {
  fetchData()
})

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await getDeptList()
    const depts = res || []
    // 构建树和扁平列表
    allDepts.value = buildFlatList(depts)
    deptTree.value = buildTree(depts)
    // 默认展开所有
    expandedIds.value = depts.map((d: DeptVO) => d.id)
  } catch (error) {
    console.error('Failed to fetch depts:', error)
  } finally {
    loading.value = false
  }
}

const buildTree = (depts: DeptVO[]): DeptVO[] => {
  const map = new Map<number, DeptVO>()
  const roots: DeptVO[] = []
  
  depts.forEach(d => map.set(d.id, { ...d, children: [] }))
  
  depts.forEach(d => {
    const node = map.get(d.id)!
    if (d.parentId === 0 || !map.has(d.parentId)) {
      roots.push(node)
    } else {
      const parent = map.get(d.parentId)!
      parent.children = parent.children || []
      parent.children.push(node)
    }
  })
  
  return roots
}

const buildFlatList = (depts: DeptVO[], parentId = 0, level = 0): FlatDept[] => {
  const result: FlatDept[] = []
  const children = depts.filter(d => d.parentId === parentId)
  
  children.forEach(dept => {
    const hasChildren = depts.some(d => d.parentId === dept.id)
    result.push({ ...dept, level, hasChildren })
    if (hasChildren) {
      result.push(...buildFlatList(depts, dept.id, level + 1))
    }
  })
  
  return result
}

const flatDeptList = computed(() => {
  const result: FlatDept[] = []
  const addVisible = (depts: FlatDept[], parentId = 0) => {
    depts.filter(d => d.parentId === parentId).forEach(dept => {
      result.push(dept)
      if (dept.hasChildren && expandedIds.value.includes(dept.id)) {
        addVisible(depts, dept.id)
      }
    })
  }
  addVisible(allDepts.value)
  return result
})

const toggleExpand = (id: number) => {
  const idx = expandedIds.value.indexOf(id)
  if (idx >= 0) {
    expandedIds.value.splice(idx, 1)
  } else {
    expandedIds.value.push(id)
  }
}

const openFormModal = (dept?: DeptVO, parentId?: number) => {
  if (dept) {
    isEdit.value = true
    currentId.value = dept.id
    Object.assign(form, {
      parentId: dept.parentId,
      deptName: dept.deptName,
      sortOrder: dept.sortOrder,
      leader: dept.leader || '',
      phone: dept.phone || '',
      email: dept.email || '',
      status: dept.status
    })
  } else {
    isEdit.value = false
    currentId.value = 0
    Object.assign(form, {
      parentId: parentId || 0,
      deptName: '',
      sortOrder: 0,
      leader: '',
      phone: '',
      email: '',
      status: 0
    })
  }
  const modal = document.getElementById('dept_modal') as HTMLDialogElement
  modal.showModal()
}

const submitForm = async () => {
  if (!form.deptName?.trim()) {
    alert('请输入部门名称')
    return
  }
  
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateDept(currentId.value, form)
    } else {
      await createDept(form)
    }
    const modal = document.getElementById('dept_modal') as HTMLDialogElement
    modal.close()
    fetchData()
  } catch (error: any) {
    alert(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (dept: DeptVO) => {
  if (!confirm(`确定要删除部门「${dept.deptName}」吗？`)) return
  try {
    await deleteDept(dept.id)
    fetchData()
  } catch (error: any) {
    alert(error.message || '删除失败')
  }
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}
</script>
