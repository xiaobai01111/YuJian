<template>
  <div class="card bg-base-100 shadow-xl">
    <div class="card-body">
      <div class="flex justify-between items-center mb-4">
        <h2 class="card-title">岗位管理</h2>
        <button class="btn btn-primary btn-sm" @click="openAddModal" v-permission="['system:post:add']">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          新增岗位
        </button>
      </div>

      <div class="overflow-x-auto min-h-[400px]">
        <table class="table table-zebra">
          <thead>
            <tr>
              <th>岗位编码</th>
              <th>岗位名称</th>
              <th>排序</th>
              <th>状态</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="6" class="text-center py-4">加载中...</td>
            </tr>
            <tr v-else-if="postList.length === 0">
              <td colspan="6" class="text-center py-4">暂无数据</td>
            </tr>
            <tr v-else v-for="post in postList" :key="post.id">
              <td><span class="badge badge-ghost">{{ post.postCode }}</span></td>
              <td>{{ post.postName }}</td>
              <td>{{ post.sortOrder }}</td>
              <td>
                <span :class="['badge', post.status === 0 ? 'badge-success' : 'badge-error']">
                  {{ post.status === 0 ? '正常' : '停用' }}
                </span>
              </td>
              <td class="text-sm text-slate-500">{{ formatDate(post.createdAt) }}</td>
              <td>
                <div class="flex gap-2">
                  <button class="btn btn-ghost btn-xs text-primary" @click="openEditModal(post)" v-permission="['system:post:edit']">编辑</button>
                  <button class="btn btn-ghost btn-xs text-error" @click="handleDelete(post)" v-permission="['system:post:delete']">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Add/Edit Modal -->
    <dialog id="post_modal" class="modal">
      <div class="modal-box">
        <h3 class="font-bold text-lg">{{ isEdit ? '编辑岗位' : '新增岗位' }}</h3>
        <div class="form-control mt-4">
          <label class="label"><span class="label-text">岗位名称</span></label>
          <input v-model="form.postName" type="text" class="input input-bordered" placeholder="请输入岗位名称" />
        </div>
        <div class="form-control mt-2">
          <label class="label"><span class="label-text">岗位编码</span></label>
          <input v-model="form.postCode" type="text" class="input input-bordered" placeholder="请输入岗位编码" />
        </div>
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

interface Post {
  id: number
  postCode: string
  postName: string
  sortOrder: number
  status: number
  remark?: string
  createdAt: string
}

const loading = ref(false)
const submitting = ref(false)
const postList = ref<Post[]>([])
const isEdit = ref(false)

const form = reactive({
  id: 0,
  postCode: '',
  postName: '',
  sortOrder: 0,
  status: 0,
  remark: ''
})

onMounted(() => {
  fetchData()
})

const fetchData = async () => {
  loading.value = true
  setTimeout(() => {
    postList.value = [
      { id: 1, postCode: 'ceo', postName: '董事长', sortOrder: 1, status: 0, createdAt: '2026-01-12' },
      { id: 2, postCode: 'manager', postName: '项目经理', sortOrder: 2, status: 0, createdAt: '2026-01-12' },
      { id: 3, postCode: 'developer', postName: '开发工程师', sortOrder: 3, status: 0, createdAt: '2026-01-12' },
    ]
    loading.value = false
  }, 500)
}

const openAddModal = () => {
  isEdit.value = false
  Object.assign(form, { id: 0, postCode: '', postName: '', sortOrder: 0, status: 0, remark: '' })
  const modal = document.getElementById('post_modal') as HTMLDialogElement
  modal.showModal()
}

const openEditModal = (post: Post) => {
  isEdit.value = true
  Object.assign(form, post)
  const modal = document.getElementById('post_modal') as HTMLDialogElement
  modal.showModal()
}

const submitForm = async () => {
  submitting.value = true
  setTimeout(() => {
    const modal = document.getElementById('post_modal') as HTMLDialogElement
    modal.close()
    fetchData()
    submitting.value = false
  }, 500)
}

const handleDelete = async (post: Post) => {
  if (!confirm(`确定要删除岗位 ${post.postName} 吗？`)) return
  fetchData()
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString()
}
</script>
