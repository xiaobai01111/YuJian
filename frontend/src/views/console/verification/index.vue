<template>
  <div class="h-full flex flex-col gap-6 p-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-800">身份审核</h1>
        <p class="text-slate-500 mt-1">来自身份认证的申请将展示在此</p>
      </div>
      <button class="btn btn-sm btn-ghost" @click="loadData">
        刷新
      </button>
    </div>

    <div class="card bg-base-100 shadow-sm">
      <div class="card-body p-4">
        <div class="flex flex-wrap gap-4 items-center">
          <div class="form-control">
            <select v-model="query.status" class="select select-bordered select-sm w-36">
              <option value="">全部状态</option>
              <option value="0">待审核</option>
              <option value="1">已通过</option>
              <option value="2">已拒绝</option>
            </select>
          </div>
          <button class="btn btn-sm btn-ghost" @click="loadData">搜索</button>
        </div>
      </div>
    </div>

    <div class="card bg-base-100 shadow-sm flex-1 min-h-0">
      <div class="card-body p-0 flex flex-col min-h-0">
        <div class="flex-1 overflow-auto">
          <table class="table">
            <thead>
              <tr>
                <th class="w-20">ID</th>
                <th>用户</th>
                <th class="w-32">状态</th>
                <th class="w-44">提交时间</th>
                <th class="w-44">审核时间</th>
                <th class="w-40">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="6" class="text-center py-10">
                  <span class="loading loading-spinner loading-md"></span>
                </td>
              </tr>
              <tr v-else-if="records.length === 0">
                <td colspan="6" class="text-center py-10 text-slate-500">暂无数据</td>
              </tr>
              <tr v-for="item in records" :key="item.id" class="hover">
                <td>{{ item.id }}</td>
                <td>
                  <div class="font-medium text-slate-800">{{ item.nickname || item.username || '-' }}</div>
                  <div class="text-xs text-slate-500">UID {{ item.userId }}</div>
                </td>
                <td>
                  <span :class="statusBadge(item.status)">{{ statusText(item.status) }}</span>
                </td>
                <td>{{ formatDateTime(item.createdAt) }}</td>
                <td>{{ formatDateTime(item.reviewedAt) }}</td>
                <td>
                  <div class="flex flex-wrap gap-2">
                    <button class="btn btn-xs btn-ghost" @click="openDetail(item)">详情</button>
                    <button v-if="item.status === 0" class="btn btn-xs btn-ghost text-success" @click="openHandle(item, 'approve')">通过</button>
                    <button v-if="item.status === 0" class="btn btn-xs btn-ghost text-error" @click="openHandle(item, 'reject')">拒绝</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="flex items-center justify-between p-4 border-t border-base-200">
          <div class="text-sm text-slate-500">共 {{ total }} 条记录</div>
          <div class="join">
            <button class="join-item btn btn-sm" :disabled="query.page <= 1" @click="changePage(query.page - 1)">«</button>
            <button class="join-item btn btn-sm">第 {{ query.page }} 页</button>
            <button class="join-item btn btn-sm" :disabled="query.page * query.size >= total" @click="changePage(query.page + 1)">»</button>
          </div>
        </div>
      </div>
    </div>

    <dialog ref="detailModal" class="modal">
      <div class="modal-box max-w-2xl">
        <h3 class="font-bold text-lg mb-4">审核详情</h3>
        <div v-if="currentDetail" class="space-y-4">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
            <div><span class="font-semibold">用户：</span>{{ currentDetail.nickname || currentDetail.username || '-' }}</div>
            <div><span class="font-semibold">状态：</span>{{ statusText(currentDetail.status) }}</div>
            <div><span class="font-semibold">提交时间：</span>{{ formatDateTime(currentDetail.createdAt) }}</div>
            <div><span class="font-semibold">审核时间：</span>{{ formatDateTime(currentDetail.reviewedAt) }}</div>
            <div v-if="currentDetail.reviewerName"><span class="font-semibold">审核人：</span>{{ currentDetail.reviewerName }}</div>
            <div v-if="currentDetail.rejectReason"><span class="font-semibold">拒绝原因：</span>{{ currentDetail.rejectReason }}</div>
          </div>
          <div v-if="currentDetail.imageUrl" class="border border-base-200 rounded-lg p-3">
            <img :src="resolveFileUrl(currentDetail.imageUrl)" class="max-h-96 w-full object-contain rounded-md" alt="学生证" />
          </div>
        </div>
        <div class="modal-action">
          <button class="btn" @click="closeDetail">关闭</button>
        </div>
      </div>
    </dialog>

    <dialog ref="handleModal" class="modal">
      <div class="modal-box max-w-lg">
        <h3 class="font-bold text-lg mb-4">{{ handleMode === 'approve' ? '通过审核' : '拒绝审核' }}</h3>
        <div class="space-y-4">
          <div v-if="handleMode === 'approve'" class="form-control">
            <label class="label"><span class="label-text">学号（可选）</span></label>
            <input v-model="handleForm.studentId" type="text" class="input input-bordered" />
          </div>
          <div v-else class="form-control">
            <label class="label"><span class="label-text">拒绝原因</span></label>
            <textarea v-model="handleForm.rejectReason" class="textarea textarea-bordered" rows="3"></textarea>
          </div>
        </div>
        <div class="modal-action">
          <button class="btn" @click="closeHandle">取消</button>
          <button class="btn btn-primary" :disabled="submitting" @click="submitHandle">
            <span v-if="submitting" class="loading loading-spinner loading-sm"></span>
            确认
          </button>
        </div>
      </div>
    </dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getVerificationList, getVerificationDetail, handleVerification, type VerificationVO } from '@/api/system'
import { resolveFileUrl } from '@/utils/file'
import { useDialog } from '@/composables/useDialog'

const dialog = useDialog()
const loading = ref(false)
const submitting = ref(false)
const records = ref<VerificationVO[]>([])
const total = ref(0)
const currentDetail = ref<VerificationVO | null>(null)
const detailModal = ref<HTMLDialogElement | null>(null)
const handleModal = ref<HTMLDialogElement | null>(null)
const handleMode = ref<'approve' | 'reject'>('approve')

const query = reactive({
  status: '',
  page: 1,
  size: 20
})

const handleForm = reactive({
  studentId: '',
  rejectReason: ''
})

const loadData = async () => {
  loading.value = true
  try {
    const params: any = {
      page: query.page,
      size: query.size
    }
    if (query.status !== '') {
      params.status = Number(query.status)
    }
    const res: any = await getVerificationList(params)
    records.value = res?.records || []
    total.value = res?.total || 0
  } catch (e: any) {
    await dialog.alert(e.message || e.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const changePage = (page: number) => {
  query.page = page
  loadData()
}

const statusText = (status: number) => {
  if (status === 1) return '已通过'
  if (status === 2) return '已拒绝'
  return '待审核'
}

const statusBadge = (status: number) => {
  if (status === 1) return 'badge badge-success badge-sm'
  if (status === 2) return 'badge badge-error badge-sm'
  return 'badge badge-warning badge-sm'
}

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const openDetail = async (item: VerificationVO) => {
  try {
    currentDetail.value = await getVerificationDetail(item.id)
  } catch {
    currentDetail.value = item
  }
  detailModal.value?.showModal()
}

const closeDetail = () => {
  detailModal.value?.close()
}

const openHandle = (item: VerificationVO, mode: 'approve' | 'reject') => {
  currentDetail.value = item
  handleMode.value = mode
  handleForm.studentId = ''
  handleForm.rejectReason = ''
  handleModal.value?.showModal()
}

const closeHandle = () => {
  handleModal.value?.close()
}

const submitHandle = async () => {
  if (!currentDetail.value) return
  if (handleMode.value === 'reject' && !handleForm.rejectReason.trim()) {
    await dialog.alert('请输入拒绝原因')
    return
  }
  submitting.value = true
  try {
    await handleVerification(currentDetail.value.id, {
      status: handleMode.value === 'approve' ? 1 : 2,
      studentId: handleMode.value === 'approve' ? handleForm.studentId || undefined : undefined,
      rejectReason: handleMode.value === 'reject' ? handleForm.rejectReason.trim() : undefined
    })
    await dialog.alert('处理成功')
    closeHandle()
    loadData()
  } catch (e: any) {
    await dialog.alert(e.message || e.response?.data?.message || '处理失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>
