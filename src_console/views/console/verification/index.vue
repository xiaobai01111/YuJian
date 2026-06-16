<template>
  <div class="h-full flex flex-col gap-6 p-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-800">身份审核</h1>
        <p class="text-slate-500 mt-1">来自身份认证的申请将展示在此</p>
      </div>
      <button class="btn btn-sm btn-ghost" @click="handleRefresh" :disabled="activeTab === 'review' ? !canList : !canWhitelistRead">
        刷新
      </button>
    </div>

    <div class="tabs tabs-boxed w-fit">
      <a v-if="showReviewTab" class="tab" :class="{ 'tab-active': activeTab === 'review' }" @click="activeTab = 'review'">审核列表</a>
      <a v-if="showWhitelistTab" class="tab" :class="{ 'tab-active': activeTab === 'studentIds' }" @click="activeTab = 'studentIds'">学号白名单</a>
    </div>

    <div v-show="activeTab === 'review'" class="flex flex-col gap-6 flex-1 min-h-0">
      <div v-if="!canList" class="card bg-base-100 shadow-sm">
        <div class="card-body p-6 text-slate-500 text-center">无权限查看审核列表</div>
      </div>
      <template v-else>
        <div class="card bg-base-100 shadow-sm">
          <div class="card-body p-4">
            <div class="flex flex-wrap gap-4 items-center">
              <div class="form-control">
                <select v-model="query.status" class="select select-bordered select-sm w-36">
                  <option value="">全部状态</option>
                  <option value="0">待审核</option>
                  <option value="1">已通过</option>
                  <option value="2">已拒绝</option>
                  <option value="3">已取消</option>
                </select>
              </div>
              <button class="btn btn-sm btn-ghost" @click="loadData({ reset: true })">搜索</button>
            </div>
          </div>
        </div>

        <div class="card bg-base-100 shadow-sm flex-1 min-h-0">
          <div class="card-body p-0 flex flex-col min-h-0">
            <div ref="scrollContainer" class="flex-1 overflow-auto">
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
                        <button v-if="canView" class="btn btn-xs btn-ghost" @click="openDetail(item)">详情</button>
                        <button v-if="canHandle && item.status === 0" class="btn btn-xs btn-ghost text-success" @click="openHandle(item, 'approve')">通过</button>
                        <button v-if="canHandle && item.status === 0" class="btn btn-xs btn-ghost text-error" @click="openHandle(item, 'reject')">拒绝</button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
              <div ref="loadMoreTrigger" class="h-6" aria-hidden="true"></div>
            </div>

            <div class="flex items-center justify-between p-4 border-t border-base-200 text-sm text-slate-500">
              <div>已加载 {{ records.length }} / {{ total || '-' }} 条</div>
              <div v-if="loadingMore">正在加载更多...</div>
              <div v-else-if="!hasMore && records.length > 0">没有更多了</div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <div v-show="activeTab === 'studentIds'" class="flex-1 min-h-0 flex flex-col">
      <div v-if="!canWhitelistRead" class="card bg-base-100 shadow-sm">
        <div class="card-body p-6 text-slate-500 text-center">无权限查看学号白名单</div>
      </div>
      <div v-else class="card bg-base-100 shadow-sm flex-1 min-h-0">
        <div class="card-body p-4 flex flex-col min-h-0">
          <div class="flex items-center justify-between mb-4">
            <h2 class="card-title text-lg">学号白名单</h2>
            <div class="flex gap-2">
              <button class="btn btn-sm btn-outline" @click="showStudentBatchModal = true" :disabled="!canWhitelistEdit">批量添加</button>
              <button class="btn btn-sm btn-error btn-outline" @click="batchDeleteStudentIds" :disabled="!canWhitelistEdit || selectedStudentIds.length === 0">
                批量删除 ({{ selectedStudentIds.length }})
              </button>
              <button class="btn btn-sm btn-primary" @click="saveStudentIds" :disabled="!canWhitelistEdit || savingStudentIds">
                <span v-if="savingStudentIds" class="loading loading-spinner loading-xs"></span>
                保存
              </button>
            </div>
          </div>
          <p class="text-sm text-slate-500 mb-4 grow-0">用于学号认证的白名单配置，未配置时仅支持其他审核方式。</p>

          <div class="border border-base-200 rounded-lg flex-1 min-h-0 overflow-hidden flex flex-col">
            <div v-if="loadingStudentIds" class="h-56 flex items-center justify-center">
              <span class="loading loading-spinner loading-md"></span>
            </div>
            <div v-else ref="studentScrollContainer" class="flex-1 min-h-0 overflow-auto">
              <template v-if="studentIds.length > 0">
                <table class="table table-sm">
                  <thead>
                    <tr>
                      <th><input type="checkbox" class="checkbox checkbox-sm" v-model="selectAllStudentIds" :disabled="!canWhitelistEdit" /></th>
                      <th>学号</th>
                      <th class="w-20">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(_, index) in visibleStudentIds" :key="`student-${index}`">
                      <td><input type="checkbox" class="checkbox checkbox-sm" :value="index" v-model="selectedStudentIds" :disabled="!canWhitelistEdit" /></td>
                      <td>
                        <input v-model="studentIds[index]" type="text" class="input input-bordered input-sm w-full max-w-xs" placeholder="2024000001" :disabled="!canWhitelistEdit" />
                      </td>
                      <td>
                        <button class="btn btn-ghost btn-xs text-error" @click="removeStudentId(index)" :disabled="!canWhitelistEdit">删除</button>
                      </td>
                    </tr>
                  </tbody>
                </table>
                <div v-if="hasMoreStudentRows" ref="studentLoadMoreTrigger" class="h-8" aria-hidden="true"></div>
              </template>
              <template v-else>
                <div class="grid grid-cols-[4rem_minmax(0,1fr)_6rem] items-center px-4 h-11 text-sm font-semibold text-slate-600">
                  <div><input type="checkbox" class="checkbox checkbox-sm" disabled /></div>
                  <div>学号</div>
                  <div class="text-right">操作</div>
                </div>
                <div class="border-t border-base-200 px-4 py-6 text-center text-slate-400">暂无学号配置</div>
              </template>
            </div>
          </div>

          <div class="mt-3 text-xs text-slate-500 flex items-center justify-between">
            <span>已展示 {{ visibleStudentIds.length }} / {{ studentIds.length }} 条</span>
            <span v-if="hasMoreStudentRows">下滑继续加载</span>
          </div>

          <button class="btn btn-outline btn-sm w-fit mt-3" @click="addStudentId" :disabled="!canWhitelistEdit">+ 添加学号</button>
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
            <div v-if="currentDetail.verifyMethod"><span class="font-semibold">认证方式：</span>{{ formatVerifyMethod(currentDetail.verifyMethod) }}</div>
            <div v-if="currentDetail.studentId"><span class="font-semibold">学号：</span>{{ currentDetail.studentId }}</div>
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
          <div v-if="handleMode === 'approve'" class="space-y-4">
            <div class="form-control">
              <label class="label"><span class="label-text">认证方式</span></label>
              <select v-model="handleForm.verifyMethod" class="select select-bordered">
                <option value="MANUAL">人工审核</option>
                <option value="EDU_EMAIL">EDU邮箱</option>
                <option value="ID_LIST">学号白名单</option>
                <option value="ID_CARD">学生证</option>
                <option value="OCR">证件OCR</option>
                <option value="SSO">SSO</option>
              </select>
            </div>
            <div class="form-control">
              <label class="label"><span class="label-text">学号（可选）</span></label>
              <input v-model="handleForm.studentId" type="text" class="input input-bordered" />
            </div>
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

    <dialog :class="{ 'modal modal-open': showStudentBatchModal, 'modal': !showStudentBatchModal }">
      <div class="modal-box">
        <h3 class="font-bold text-lg mb-4">批量添加学号</h3>
        <p class="text-sm text-slate-500 mb-2">每行一个学号</p>
        <textarea v-model="studentBatchInput" class="textarea textarea-bordered w-full h-40" placeholder="2024000001&#10;2024000002"></textarea>
        <div class="modal-action">
          <button class="btn btn-ghost" @click="showStudentBatchModal = false">取消</button>
          <button class="btn btn-primary" @click="batchAddStudentIds" :disabled="!canWhitelistEdit">添加</button>
        </div>
      </div>
      <form method="dialog" class="modal-backdrop" @click="showStudentBatchModal = false"></form>
    </dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick, computed, watch } from 'vue'
import { getVerificationList, getVerificationDetail, handleVerification, getStudentIdWhitelist, updateStudentIdWhitelist, type VerificationQuery, type VerificationVO } from '@/api/system'
import { resolveFileUrl } from '@/utils/file'
import { useDialog } from '@/composables/useDialog'
import { useUserStore } from '@/stores/user'

const dialog = useDialog()
const userStore = useUserStore()
const canList = computed(() => userStore.hasPermission('content:verification:list'))
const canView = computed(() => userStore.hasPermission('content:verification:view'))
const canHandle = computed(() => userStore.hasPermission('content:verification:handle'))
const canWhitelistList = computed(() => userStore.hasPermission('content:verification:whitelist:list'))
const canWhitelistEdit = computed(() => userStore.hasPermission('content:verification:whitelist:edit'))
const canWhitelistRead = computed(() => canWhitelistList.value || canWhitelistEdit.value)
const showReviewTab = computed(() => canList.value)
const showWhitelistTab = computed(() => canWhitelistRead.value)

const activeTab = ref<'review' | 'studentIds'>('review')
const loading = ref(false)
const loadingMore = ref(false)
const submitting = ref(false)
const records = ref<VerificationVO[]>([])
const total = ref(0)
const hasMore = ref(true)
const currentDetail = ref<VerificationVO | null>(null)
const detailModal = ref<HTMLDialogElement | null>(null)
const handleModal = ref<HTMLDialogElement | null>(null)
const handleMode = ref<'approve' | 'reject'>('approve')
const scrollContainer = ref<HTMLElement | null>(null)
const loadMoreTrigger = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

const query = reactive({
  status: '',
  page: 1,
  size: 20
})

const handleForm = reactive({
  studentId: '',
  rejectReason: '',
  verifyMethod: 'MANUAL'
})

const studentIds = ref<string[]>([])
const selectedStudentIds = ref<number[]>([])
const showStudentBatchModal = ref(false)
const studentBatchInput = ref('')
const savingStudentIds = ref(false)
const loadingStudentIds = ref(false)
const studentIdsLoaded = ref(false)
const visibleStudentCount = ref(0)
const studentScrollContainer = ref<HTMLElement | null>(null)
const studentLoadMoreTrigger = ref<HTMLElement | null>(null)
let studentObserver: IntersectionObserver | null = null

const STUDENT_PAGE_SIZE = 100

const visibleStudentIds = computed(() => studentIds.value.slice(0, visibleStudentCount.value))
const hasMoreStudentRows = computed(() => visibleStudentCount.value < studentIds.value.length)

const selectAllStudentIds = computed({
  get: () => selectedStudentIds.value.length === studentIds.value.length && studentIds.value.length > 0,
  set: (val: boolean) => {
    selectedStudentIds.value = val ? studentIds.value.map((_, i) => i) : []
  }
})

const ensureActiveTab = () => {
  if (showReviewTab.value) {
    if (!showWhitelistTab.value && activeTab.value !== 'review') {
      activeTab.value = 'review'
    }
    return
  }
  if (showWhitelistTab.value) {
    activeTab.value = 'studentIds'
    return
  }
}

watch([showReviewTab, showWhitelistTab], () => ensureActiveTab(), { immediate: true })

const loadData = async ({ append = false, reset = false } = {}) => {
  if (!canList.value) {
    records.value = []
    total.value = 0
    hasMore.value = false
    return
  }
  if (append && (loadingMore.value || loading.value)) return
  if (!append && loading.value) return
  if (reset) {
    query.page = 1
    records.value = []
    hasMore.value = true
  }
  append ? (loadingMore.value = true) : (loading.value = true)
  try {
    const params: VerificationQuery = {
      page: query.page,
      size: query.size
    }
    if (query.status !== '') {
      params.status = Number(query.status)
    }
    const res = await getVerificationList(params)
    const newRecords = res?.records || []
    total.value = res?.total || 0
    records.value = append ? [...records.value, ...newRecords] : newRecords
    if (total.value) {
      hasMore.value = records.value.length < total.value
    } else {
      hasMore.value = newRecords.length >= query.size
    }
  } catch (e: unknown) {
    await dialog.alert((e as ApiErrorLike)?.message || (e as ApiErrorLike)?.response?.data?.message || '加载失败')
  } finally {
    append ? (loadingMore.value = false) : (loading.value = false)
  }
}

const loadStudentIds = async () => {
  if (!canWhitelistRead.value) {
    studentIds.value = []
    selectedStudentIds.value = []
    visibleStudentCount.value = 0
    studentIdsLoaded.value = false
    return
  }
  if (loadingStudentIds.value) return
  if (studentIdsLoaded.value) return
  loadingStudentIds.value = true
  try {
    const res = await getStudentIdWhitelist()
    studentIds.value = Array.isArray(res) ? res : []
    selectedStudentIds.value = []
    visibleStudentCount.value = Math.min(STUDENT_PAGE_SIZE, studentIds.value.length)
    studentIdsLoaded.value = true
  } catch (e: unknown) {
    console.error('Failed to load student ids', e)
    studentIds.value = []
    selectedStudentIds.value = []
    visibleStudentCount.value = 0
    studentIdsLoaded.value = false
  } finally {
    loadingStudentIds.value = false
    nextTick(() => setupStudentObserver())
  }
}

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

const loadMore = async () => {
  if (!hasMore.value || loading.value || loadingMore.value) return
  query.page += 1
  await loadData({ append: true })
}

const setupStudentObserver = () => {
  studentObserver?.disconnect()
  if (!studentScrollContainer.value || !studentLoadMoreTrigger.value || !hasMoreStudentRows.value) return
  studentObserver = new IntersectionObserver(
    entries => {
      if (entries[0]?.isIntersecting) {
        const nextCount = Math.min(visibleStudentCount.value + STUDENT_PAGE_SIZE, studentIds.value.length)
        if (nextCount !== visibleStudentCount.value) {
          visibleStudentCount.value = nextCount
        }
      }
    },
    {
      root: studentScrollContainer.value,
      rootMargin: '160px 0px',
      threshold: 0
    }
  )
  studentObserver.observe(studentLoadMoreTrigger.value)
}

const statusText = (status: number) => {
  if (status === 1) return '已通过'
  if (status === 2) return '已拒绝'
  if (status === 3) return '已取消'
  return '待审核'
}

const statusBadge = (status: number) => {
  if (status === 1) return 'badge badge-success badge-sm'
  if (status === 2) return 'badge badge-error badge-sm'
  if (status === 3) return 'badge badge-ghost badge-sm'
  return 'badge badge-warning badge-sm'
}

const formatVerifyMethod = (method?: string) => {
  if (!method) return '-'
  const normalized = method.toLowerCase()
  if (normalized === 'edu_email' || normalized === 'email') return '邮箱认证'
  if (normalized === 'id_list' || normalized === 'student_id') return '学号认证'
  if (normalized === 'id_card') return '学生证'
  if (normalized === 'ocr') return '证件OCR'
  if (normalized === 'sso') return 'SSO'
  if (normalized === 'manual') return '人工审核'
  return method
}

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const openDetail = async (item: VerificationVO) => {
  if (!canView.value) {
    await dialog.alert('无权限查看审核详情')
    return
  }
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
  if (!canHandle.value) {
    void dialog.alert('无权限处理审核')
    return
  }
  currentDetail.value = item
  handleMode.value = mode
  handleForm.studentId = ''
  handleForm.rejectReason = ''
  handleForm.verifyMethod = item.verifyMethod || 'MANUAL'
  handleModal.value?.showModal()
}

const closeHandle = () => {
  handleModal.value?.close()
}

const submitHandle = async () => {
  if (!currentDetail.value) return
  if (!canHandle.value) {
    await dialog.alert('无权限处理审核')
    return
  }
  if (handleMode.value === 'reject' && !handleForm.rejectReason.trim()) {
    await dialog.alert('请输入拒绝原因')
    return
  }
  submitting.value = true
  try {
    await handleVerification(currentDetail.value.id, {
      status: handleMode.value === 'approve' ? 1 : 2,
      studentId: handleMode.value === 'approve' ? handleForm.studentId || undefined : undefined,
      rejectReason: handleMode.value === 'reject' ? handleForm.rejectReason.trim() : undefined,
      verifyMethod: handleMode.value === 'approve' ? handleForm.verifyMethod : undefined
    })
    await dialog.alert('处理成功')
    closeHandle()
    loadData({ reset: true })
  } catch (e: unknown) {
    await dialog.alert((e as ApiErrorLike)?.message || (e as ApiErrorLike)?.response?.data?.message || '处理失败')
  } finally {
    submitting.value = false
  }
}

const addStudentId = () => {
  if (!canWhitelistEdit.value) {
    void dialog.alert('无权限编辑学号白名单')
    return
  }
  studentIds.value.push('')
  visibleStudentCount.value = studentIds.value.length
  studentIdsLoaded.value = true
  nextTick(() => setupStudentObserver())
}

const removeStudentId = (index: number) => {
  if (!canWhitelistEdit.value) {
    void dialog.alert('无权限编辑学号白名单')
    return
  }
  studentIds.value.splice(index, 1)
  selectedStudentIds.value = selectedStudentIds.value.filter(i => i !== index).map(i => i > index ? i - 1 : i)
  visibleStudentCount.value = Math.min(visibleStudentCount.value, studentIds.value.length)
  nextTick(() => setupStudentObserver())
}

const batchAddStudentIds = () => {
  if (!canWhitelistEdit.value) {
    void dialog.alert('无权限编辑学号白名单')
    return
  }
  const list = studentBatchInput.value.split('\n').map(item => item.trim()).filter(item => item)
  studentIds.value.push(...list)
  studentBatchInput.value = ''
  showStudentBatchModal.value = false
  visibleStudentCount.value = studentIds.value.length
  studentIdsLoaded.value = true
  nextTick(() => setupStudentObserver())
}

const batchDeleteStudentIds = () => {
  if (!canWhitelistEdit.value) {
    void dialog.alert('无权限编辑学号白名单')
    return
  }
  const toDelete = new Set(selectedStudentIds.value)
  studentIds.value = studentIds.value.filter((_, i) => !toDelete.has(i))
  selectedStudentIds.value = []
  visibleStudentCount.value = Math.min(visibleStudentCount.value, studentIds.value.length)
  nextTick(() => setupStudentObserver())
}

const saveStudentIds = async () => {
  if (!canWhitelistEdit.value) {
    await dialog.alert('无权限编辑学号白名单')
    return
  }
  const valid = studentIds.value.map(item => item.trim()).filter(item => item)
  savingStudentIds.value = true
  try {
    await updateStudentIdWhitelist(valid)
    studentIds.value = valid
    selectedStudentIds.value = []
    visibleStudentCount.value = Math.min(visibleStudentCount.value || STUDENT_PAGE_SIZE, studentIds.value.length)
    await dialog.alert('保存成功')
  } catch (e: unknown) {
    await dialog.alert((e as ApiErrorLike)?.message || (e as ApiErrorLike)?.response?.data?.message || '保存失败')
  } finally {
    savingStudentIds.value = false
    nextTick(() => setupStudentObserver())
  }
}

const handleRefresh = () => {
  if (activeTab.value === 'studentIds') {
    if (!canWhitelistRead.value) {
      void dialog.alert('无权限查看学号白名单')
      return
    }
    studentIdsLoaded.value = false
    void loadStudentIds()
  } else {
    if (!canList.value) {
      void dialog.alert('无权限查看审核列表')
      return
    }
    void loadData({ reset: true })
  }
}

watch(activeTab, tab => {
  if (tab === 'studentIds') {
    if (canWhitelistRead.value) {
      void loadStudentIds()
    }
  } else {
    studentObserver?.disconnect()
    studentObserver = null
    if (canList.value && records.value.length === 0) {
      void loadData({ reset: true })
    }
  }
})

watch(hasMoreStudentRows, () => {
  nextTick(() => setupStudentObserver())
})

watch(canList, val => {
  if (val && records.value.length === 0) {
    void loadData({ reset: true })
  }
})

watch(canWhitelistRead, val => {
  if (val && activeTab.value === 'studentIds') {
    studentIdsLoaded.value = false
    void loadStudentIds()
  }
})

onMounted(() => {
  if (canList.value) {
    loadData({ reset: true })
  }
  if (activeTab.value === 'studentIds' && canWhitelistRead.value) {
    void loadStudentIds()
  }
  nextTick(() => setupObserver())
})

onUnmounted(() => {
  observer?.disconnect()
  observer = null
  studentObserver?.disconnect()
  studentObserver = null
})
</script>
