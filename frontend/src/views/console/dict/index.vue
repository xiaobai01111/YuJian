<template>
  <div class="card bg-base-100 shadow-xl">
    <div class="card-body">
      <div class="flex justify-between items-center mb-4">
        <h2 class="card-title">字典管理</h2>
        <div class="flex gap-2">
          <button class="btn btn-primary btn-sm" @click="openAddModal" v-permission="['system:dict:add']">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            新增字典
          </button>
        </div>
      </div>

      <div class="overflow-x-auto min-h-[400px]">
        <table class="table table-zebra">
          <thead>
            <tr>
              <th>字典编码</th>
              <th>字典名称</th>
              <th>状态</th>
              <th>备注</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="6" class="text-center py-4">加载中...</td>
            </tr>
            <tr v-else-if="dictList.length === 0">
              <td colspan="6" class="text-center py-4">暂无数据</td>
            </tr>
            <tr v-else v-for="dict in dictList" :key="dict.id">
              <td><span class="badge badge-ghost">{{ dict.dictCode }}</span></td>
              <td>{{ dict.dictName }}</td>
              <td>
                <span :class="['badge', dict.status === 0 ? 'badge-success' : 'badge-error']">
                  {{ dict.status === 0 ? '正常' : '停用' }}
                </span>
              </td>
              <td class="max-w-xs truncate">{{ dict.remark || '-' }}</td>
              <td class="text-sm text-slate-500">{{ formatDate(dict.createdAt) }}</td>
              <td>
                <div class="flex gap-2">
                  <button class="btn btn-ghost btn-xs text-primary" @click="openEditModal(dict)" v-permission="['system:dict:edit']">编辑</button>
                  <button class="btn btn-ghost btn-xs text-info" @click="viewDictData(dict)">字典数据</button>
                  <button class="btn btn-ghost btn-xs text-error" @click="handleDelete(dict)" v-permission="['system:dict:delete']">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Add/Edit Modal -->
    <dialog id="dict_modal" class="modal">
      <div class="modal-box">
        <h3 class="font-bold text-lg">{{ isEdit ? '编辑字典' : '新增字典' }}</h3>
        <div class="form-control mt-4">
          <label class="label"><span class="label-text">字典名称</span></label>
          <input v-model="form.dictName" type="text" class="input input-bordered" placeholder="请输入字典名称" />
        </div>
        <div class="form-control mt-2">
          <label class="label"><span class="label-text">字典编码</span></label>
          <input v-model="form.dictCode" type="text" class="input input-bordered" placeholder="请输入字典编码" :disabled="isEdit" />
        </div>
        <div class="form-control mt-2">
          <label class="label"><span class="label-text">状态</span></label>
          <select v-model="form.status" class="select select-bordered">
            <option :value="0">正常</option>
            <option :value="1">停用</option>
          </select>
        </div>
        <div class="form-control mt-2">
          <label class="label"><span class="label-text">备注</span></label>
          <textarea v-model="form.remark" class="textarea textarea-bordered" placeholder="请输入备注"></textarea>
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

interface DictType {
  id: number
  dictName: string
  dictCode: string
  status: number
  remark?: string
  createdAt: string
}

const loading = ref(false)
const submitting = ref(false)
const dictList = ref<DictType[]>([])
const isEdit = ref(false)

const form = reactive({
  id: 0,
  dictName: '',
  dictCode: '',
  status: 0,
  remark: ''
})

onMounted(() => {
  fetchData()
})

const fetchData = async () => {
  loading.value = true
  // TODO: 实现 API 调用
  setTimeout(() => {
    dictList.value = [
      { id: 1, dictName: '用户性别', dictCode: 'sys_user_sex', status: 0, remark: '用户性别列表', createdAt: '2026-01-12' },
      { id: 2, dictName: '系统状态', dictCode: 'sys_normal_disable', status: 0, remark: '系统状态列表', createdAt: '2026-01-12' },
    ]
    loading.value = false
  }, 500)
}

const openAddModal = () => {
  isEdit.value = false
  Object.assign(form, { id: 0, dictName: '', dictCode: '', status: 0, remark: '' })
  const modal = document.getElementById('dict_modal') as HTMLDialogElement
  modal.showModal()
}

const openEditModal = (dict: DictType) => {
  isEdit.value = true
  Object.assign(form, dict)
  const modal = document.getElementById('dict_modal') as HTMLDialogElement
  modal.showModal()
}

const submitForm = async () => {
  submitting.value = true
  // TODO: 实现 API 调用
  setTimeout(() => {
    const modal = document.getElementById('dict_modal') as HTMLDialogElement
    modal.close()
    fetchData()
    submitting.value = false
  }, 500)
}

const handleDelete = async (dict: DictType) => {
  if (!confirm(`确定要删除字典 ${dict.dictName} 吗？`)) return
  // TODO: 实现 API 调用
  fetchData()
}

const viewDictData = (dict: DictType) => {
  alert(`查看字典数据: ${dict.dictCode}`)
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString()
}
</script>
