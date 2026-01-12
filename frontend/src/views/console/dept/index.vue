<template>
  <div class="card bg-base-100 shadow-xl">
    <div class="card-body">
      <div class="flex justify-between items-center mb-4">
        <h2 class="card-title">部门管理</h2>
        <button class="btn btn-primary btn-sm" @click="() => openAddModal()" v-permission="['system:dept:add']">
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
              <th>排序</th>
              <th>状态</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="5" class="text-center py-4">加载中...</td>
            </tr>
            <tr v-else-if="deptList.length === 0">
              <td colspan="5" class="text-center py-4">暂无数据</td>
            </tr>
            <template v-else v-for="dept in deptList" :key="dept.id">
              <tr>
                <td>
                  <span :style="{ paddingLeft: ((dept.level ?? 0)) * 20 + 'px' }">
                    {{ (dept.level ?? 0) > 0 ? '└─ ' : '' }}{{ dept.deptName }}
                  </span>
                </td>
                <td>{{ dept.sortOrder }}</td>
                <td>
                  <span :class="['badge', dept.status === 0 ? 'badge-success' : 'badge-error']">
                    {{ dept.status === 0 ? '正常' : '停用' }}
                  </span>
                </td>
                <td class="text-sm text-slate-500">{{ formatDate(dept.createdAt) }}</td>
                <td>
                  <div class="flex gap-2">
                    <button class="btn btn-ghost btn-xs text-primary" @click="openEditModal(dept)" v-permission="['system:dept:edit']">编辑</button>
                    <button class="btn btn-ghost btn-xs text-success" @click="openAddModal(dept.id)" v-permission="['system:dept:add']">新增</button>
                    <button class="btn btn-ghost btn-xs text-error" @click="handleDelete(dept)" v-permission="['system:dept:delete']">删除</button>
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
            <option v-for="d in deptList" :key="d.id" :value="d.id">{{ d.deptName }}</option>
          </select>
        </div>
        <div class="form-control mt-2">
          <label class="label"><span class="label-text">部门名称</span></label>
          <input v-model="form.deptName" type="text" class="input input-bordered" placeholder="请输入部门名称" />
        </div>
        <div class="form-control mt-2">
          <label class="label"><span class="label-text">排序</span></label>
          <input v-model.number="form.sortOrder" type="number" class="input input-bordered" placeholder="请输入排序" />
        </div>
        <div class="form-control mt-2">
          <label class="label"><span class="label-text">负责人</span></label>
          <input v-model="form.leader" type="text" class="input input-bordered" placeholder="请输入负责人" />
        </div>
        <div class="form-control mt-2">
          <label class="label"><span class="label-text">状态</span></label>
          <select v-model="form.status" class="select select-bordered">
            <option :value="0">正常</option>
            <option :value="1">停用</option>
          </select>
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
import { ref, reactive, onMounted } from 'vue'

interface Dept {
  id: number
  parentId: number
  deptName: string
  sortOrder: number
  leader?: string
  status: number
  level?: number
  createdAt: string
}

const loading = ref(false)
const submitting = ref(false)
const deptList = ref<Dept[]>([])
const isEdit = ref(false)

const form = reactive({
  id: 0,
  parentId: 0,
  deptName: '',
  sortOrder: 0,
  leader: '',
  status: 0
})

onMounted(() => {
  fetchData()
})

const fetchData = async () => {
  loading.value = true
  setTimeout(() => {
    deptList.value = [
      { id: 1, parentId: 0, deptName: '总公司', sortOrder: 1, leader: '张三', status: 0, level: 0, createdAt: '2026-01-12' },
      { id: 2, parentId: 1, deptName: '研发部', sortOrder: 1, leader: '李四', status: 0, level: 1, createdAt: '2026-01-12' },
      { id: 3, parentId: 1, deptName: '市场部', sortOrder: 2, leader: '王五', status: 0, level: 1, createdAt: '2026-01-12' },
    ]
    loading.value = false
  }, 500)
}

const openAddModal = (parentId: number = 0) => {
  isEdit.value = false
  Object.assign(form, { id: 0, parentId, deptName: '', sortOrder: 0, leader: '', status: 0 })
  const modal = document.getElementById('dept_modal') as HTMLDialogElement
  modal.showModal()
}

const openEditModal = (dept: Dept) => {
  isEdit.value = true
  Object.assign(form, dept)
  const modal = document.getElementById('dept_modal') as HTMLDialogElement
  modal.showModal()
}

const submitForm = async () => {
  submitting.value = true
  setTimeout(() => {
    const modal = document.getElementById('dept_modal') as HTMLDialogElement
    modal.close()
    fetchData()
    submitting.value = false
  }, 500)
}

const handleDelete = async (dept: Dept) => {
  if (!confirm(`确定要删除部门 ${dept.deptName} 吗？`)) return
  fetchData()
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString()
}
</script>
