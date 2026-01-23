<template>
  <div class="h-full flex flex-col overflow-hidden">
    <div class="card bg-base-100 shadow-xl flex-1 min-h-0">
      <div class="card-body flex flex-col min-h-0">
        <div class="flex justify-between items-center mb-4">
        <div>
          <h2 class="card-title">阻止名单</h2>
          <p class="text-sm text-slate-500">管理被限制访问的IP、用户或设备</p>
        </div>
        <div class="flex gap-2">
          <button
            class="btn btn-sm btn-primary"
            @click="openCreateModal"
            v-permission="['system:blocklist:add']"
          >
            新增
          </button>
          <button
            class="btn btn-sm btn-secondary"
            @click="openBatchModal"
            v-permission="['system:blocklist:add']"
          >
            批量导入
          </button>
        </div>
      </div>

        <div class="flex flex-wrap gap-3 mb-4">
        <select v-model="queryParams.targetType" class="select select-bordered select-sm w-40">
          <option value="">全部类型</option>
          <option v-for="item in targetTypeOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </option>
        </select>
        <select v-model="queryParams.status" class="select select-bordered select-sm w-40">
          <option value="">全部状态</option>
          <option :value="0">启用</option>
          <option :value="1">停用</option>
        </select>
        <input v-model="queryParams.keyword" class="input input-bordered input-sm w-64" placeholder="目标值/原因" />
        <button class="btn btn-sm btn-primary" @click="handleSearch">搜索</button>
        <button class="btn btn-sm btn-ghost" @click="handleReset">重置</button>
        </div>

        <div class="flex-1 overflow-auto">
          <div class="overflow-x-auto">
            <table class="table table-zebra">
              <thead>
                <tr>
                  <th class="w-16">ID</th>
                  <th class="w-28">类型</th>
                  <th>目标值</th>
                  <th class="w-24">状态</th>
                  <th class="w-40">过期时间</th>
                  <th class="w-40">创建人</th>
                  <th class="w-40">创建时间</th>
                  <th class="w-32">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="loading">
                  <td colspan="8" class="text-center py-4">加载中...</td>
                </tr>
                <tr v-else-if="blocklist.length === 0">
                  <td colspan="8" class="text-center py-4">暂无数据</td>
                </tr>
                <tr v-else v-for="item in blocklist" :key="item.id">
                  <td>{{ item.id }}</td>
                  <td>{{ getTargetTypeLabel(item.targetType) }}</td>
                  <td>
                    <div class="font-medium">{{ item.targetValue }}</div>
                    <div v-if="item.reason" class="text-xs text-slate-500">{{ item.reason }}</div>
                  </td>
                  <td>
                    <span :class="getStatusClass(item)">{{ getStatusText(item) }}</span>
                  </td>
                  <td class="text-sm text-slate-500">{{ formatDateTime(item.expireAt) }}</td>
                  <td>{{ item.createdByName || '-' }}</td>
                  <td class="text-sm text-slate-500">{{ formatDateTime(item.createdAt) }}</td>
                  <td>
                    <div class="flex gap-2">
                      <button
                        class="btn btn-ghost btn-xs"
                        @click="openEditModal(item)"
                        v-permission="['system:blocklist:edit']"
                      >
                        编辑
                      </button>
                      <button
                        class="btn btn-ghost btn-xs text-error"
                        @click="handleDelete(item)"
                        v-permission="['system:blocklist:delete']"
                      >
                        删除
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="flex justify-end pt-4">
          <div class="join">
            <button class="join-item btn btn-sm" :disabled="page <= 1" @click="changePage(page - 1)">«</button>
            <button class="join-item btn btn-sm">Page {{ page }} / {{ totalPages }}</button>
            <button class="join-item btn btn-sm" :disabled="page >= totalPages" @click="changePage(page + 1)">»</button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <dialog ref="formModal" class="modal">
    <div class="modal-box max-w-xl">
      <h3 class="font-bold text-lg mb-4">{{ isEdit ? '编辑阻止名单' : '新增阻止名单' }}</h3>
      <div class="space-y-4">
        <div class="form-control">
          <label class="label"><span class="label-text">类型 *</span></label>
          <select v-model="form.targetType" class="select select-bordered">
            <option v-for="item in targetTypeOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </option>
          </select>
        </div>
        <div class="form-control">
          <label class="label"><span class="label-text">目标值 *</span></label>
          <input
            v-model="form.targetValue"
            class="input input-bordered"
            :placeholder="getTargetValuePlaceholder(form.targetType)"
          />
        </div>
        <div class="form-control">
          <label class="label"><span class="label-text">原因</span></label>
          <textarea v-model="form.reason" class="textarea textarea-bordered" placeholder="可选，填写阻止原因"></textarea>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div class="form-control">
            <label class="label"><span class="label-text">状态</span></label>
            <select v-model="form.status" class="select select-bordered">
              <option :value="0">启用</option>
              <option :value="1">停用</option>
            </select>
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">过期时间</span></label>
            <input v-model="form.expireAt" type="datetime-local" class="input input-bordered" />
          </div>
        </div>
      </div>
      <div class="modal-action">
        <button class="btn btn-ghost" @click="closeModal">取消</button>
        <button class="btn btn-primary" :disabled="saving" @click="handleSave">
          <span v-if="saving" class="loading loading-spinner loading-sm"></span>
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </div>
    </div>
    <form method="dialog" class="modal-backdrop"><button>close</button></form>
  </dialog>

  <dialog ref="batchModal" class="modal">
    <div class="modal-box max-w-2xl">
      <h3 class="font-bold text-lg mb-4">批量导入阻止名单</h3>
      <div class="space-y-4">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div class="form-control">
            <label class="label"><span class="label-text">类型 *</span></label>
            <select v-model="batchForm.targetType" class="select select-bordered">
              <option v-for="item in targetTypeOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </option>
            </select>
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">状态</span></label>
            <select v-model="batchForm.status" class="select select-bordered">
              <option :value="0">启用</option>
              <option :value="1">停用</option>
            </select>
          </div>
        </div>
        <div class="form-control">
          <label class="label"><span class="label-text">默认原因</span></label>
          <input v-model="batchForm.reason" class="input input-bordered" placeholder="可选，批量导入默认原因" />
        </div>
        <div class="form-control">
          <label class="label"><span class="label-text">过期时间</span></label>
          <input v-model="batchForm.expireAt" type="datetime-local" class="input input-bordered" />
        </div>
        <div class="form-control">
          <label class="label">
            <span class="label-text">目标值列表</span>
            <span class="label-text-alt text-slate-400">每行一个，支持“目标值|原因”</span>
          </label>
          <textarea
            v-model="batchForm.text"
            class="textarea textarea-bordered h-44 font-mono"
            placeholder="例如：&#10;192.168.1.1|恶意访问&#10;user_1001&#10;device-abc"
          ></textarea>
        </div>
        <div v-if="batchResult" class="alert" :class="batchResult.addedCount > 0 ? 'alert-success' : 'alert-warning'">
          <div>
            <p>导入完成：成功 {{ batchResult.addedCount }} 条，跳过 {{ batchResult.skippedCount }} 条，无效 {{ batchResult.invalidCount }} 条</p>
            <p v-if="batchResult.skippedCount > 0" class="text-xs mt-1">
              跳过: {{ batchResult.skipped?.slice(0, 5).join(', ') }}{{ (batchResult.skipped?.length ?? 0) > 5 ? '...' : '' }}
            </p>
            <p v-if="batchResult.invalidCount > 0" class="text-xs mt-1">
              无效: {{ batchResult.invalid?.slice(0, 5).join(', ') }}{{ (batchResult.invalid?.length ?? 0) > 5 ? '...' : '' }}
            </p>
          </div>
        </div>
      </div>
      <div class="modal-action">
        <button class="btn btn-ghost" @click="closeBatchModal">取消</button>
        <button class="btn btn-primary" :disabled="batchLoading" @click="handleBatchImport">
          <span v-if="batchLoading" class="loading loading-spinner loading-sm"></span>
          导入
        </button>
      </div>
    </div>
    <form method="dialog" class="modal-backdrop"><button>close</button></form>
  </dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createBlocklist,
  deleteBlocklist,
  getBlocklist,
  importBlocklist,
  updateBlocklist,
  type BlocklistBatchImportResult,
  type BlocklistDTO,
  type BlocklistVO
} from '@/api/system'
import { useDialog } from '@/composables/useDialog'

const targetTypeOptions = [
  { value: 'IP', label: 'IP地址' },
  { value: 'USER', label: '用户' },
  { value: 'DEVICE', label: '设备' }
]

const loading = ref(false)
const blocklist = ref<BlocklistVO[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
const queryParams = reactive({
  targetType: '',
  status: '',
  keyword: ''
})

const formModal = ref<HTMLDialogElement | null>(null)
const batchModal = ref<HTMLDialogElement | null>(null)
const saving = ref(false)
const form = reactive({
  id: null as number | null,
  targetType: 'IP',
  targetValue: '',
  reason: '',
  status: 0,
  expireAt: ''
})

const batchLoading = ref(false)
const batchResult = ref<BlocklistBatchImportResult | null>(null)
const batchForm = reactive({
  targetType: 'IP',
  status: 0,
  expireAt: '',
  reason: '',
  text: ''
})
const dialog = useDialog()

const isEdit = computed(() => form.id !== null)

onMounted(() => {
  fetchData()
})

const fetchData = async () => {
  loading.value = true
  try {
    const params: any = {
      page: page.value,
      size: pageSize.value
    }
    if (queryParams.targetType) params.targetType = queryParams.targetType
    if (queryParams.status !== '') params.status = queryParams.status
    if (queryParams.keyword) params.keyword = queryParams.keyword

    const res = await getBlocklist(params)
    blocklist.value = res?.records || []
    total.value = res?.total || 0
  } catch (error: any) {
    blocklist.value = []
    total.value = 0
    await dialog.alert(error?.message || error?.response?.data?.message || '获取阻止名单失败')
  } finally {
    loading.value = false
  }
}

const changePage = (p: number) => {
  page.value = p
  fetchData()
}

const handleSearch = () => {
  page.value = 1
  fetchData()
}

const handleReset = () => {
  queryParams.targetType = ''
  queryParams.status = ''
  queryParams.keyword = ''
  page.value = 1
  fetchData()
}

const openCreateModal = () => {
  form.id = null
  form.targetType = 'IP'
  form.targetValue = ''
  form.reason = ''
  form.status = 0
  form.expireAt = ''
  formModal.value?.showModal()
}

const openBatchModal = () => {
  batchForm.targetType = 'IP'
  batchForm.status = 0
  batchForm.expireAt = ''
  batchForm.reason = ''
  batchForm.text = ''
  batchResult.value = null
  batchModal.value?.showModal()
}

const openEditModal = (item: BlocklistVO) => {
  form.id = item.id
  form.targetType = item.targetType
  form.targetValue = item.targetValue
  form.reason = item.reason || ''
  form.status = item.status ?? 0
  form.expireAt = formatDateTimeLocal(item.expireAt)
  formModal.value?.showModal()
}

const closeModal = () => {
  formModal.value?.close()
}

const closeBatchModal = () => {
  batchModal.value?.close()
}

const handleSave = async () => {
  if (!form.targetValue.trim()) {
    await dialog.alert('目标值不能为空')
    return
  }
  const payload: BlocklistDTO = {
    targetType: form.targetType as BlocklistDTO['targetType'],
    targetValue: form.targetValue.trim(),
    reason: form.reason.trim() || undefined,
    status: form.status,
    expireAt: form.expireAt ? form.expireAt : undefined
  }
  saving.value = true
  try {
    if (form.id) {
      await updateBlocklist(form.id, payload)
    } else {
      await createBlocklist(payload)
    }
    closeModal()
    fetchData()
  } catch (error: any) {
    await dialog.alert(error?.message || error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const handleBatchImport = async () => {
  const values = batchForm.text
    .split('\n')
    .map(line => line.trim())
    .filter(line => line.length > 0)

  if (values.length === 0) {
    await dialog.alert('请输入目标值列表')
    return
  }
  batchLoading.value = true
  try {
    const res = await importBlocklist({
      targetType: batchForm.targetType as BlocklistDTO['targetType'],
      status: batchForm.status,
      expireAt: batchForm.expireAt || undefined,
      reason: batchForm.reason.trim() || undefined,
      values
    })
    batchResult.value = res
    fetchData()
  } catch (error: any) {
    await dialog.alert(error?.message || error?.response?.data?.message || '批量导入失败')
  } finally {
    batchLoading.value = false
  }
}

const handleDelete = async (item: BlocklistVO) => {
  if (!await dialog.confirm(`确定要删除 ${item.targetValue} 吗？`)) return
  try {
    await deleteBlocklist(item.id)
    fetchData()
  } catch (error: any) {
    await dialog.alert(error?.message || error?.response?.data?.message || '删除失败')
  }
}

const getTargetTypeLabel = (type: string) => {
  const match = targetTypeOptions.find(item => item.value === type)
  return match ? match.label : type
}

const getTargetValuePlaceholder = (type: string) => {
  if (type === 'IP') return '例如：192.168.1.1 或 2001:db8::1'
  if (type === 'USER') return '用户ID'
  if (type === 'DEVICE') return '设备标识'
  return '目标值'
}

const getStatusText = (item: BlocklistVO) => {
  if (item.expired) return '已过期'
  return item.status === 0 ? '启用' : '停用'
}

const getStatusClass = (item: BlocklistVO) => {
  if (item.expired) return 'badge badge-ghost'
  return item.status === 0 ? 'badge badge-success' : 'badge badge-ghost'
}

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}

const formatDateTimeLocal = (value?: string) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (num: number) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}
</script>
