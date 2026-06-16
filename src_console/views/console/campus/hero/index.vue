<template>
  <div class="h-full flex flex-col overflow-hidden">
    <div class="flex-1 min-h-0 flex flex-col gap-6 overflow-hidden p-6">
      <div class="flex justify-between items-center">
        <div>
          <h1 class="text-2xl font-bold">校园展示位</h1>
          <p class="text-slate-500 mt-1">配置前台各页面的首屏展示内容与视觉</p>
        </div>
        <button class="btn btn-primary btn-sm" @click="openCreateModal" v-permission="['campus:hero:add']">
          新增配置
        </button>
      </div>

      <div class="card bg-base-100 shadow-sm">
        <div class="card-body p-4">
          <div class="flex flex-wrap gap-3 items-center">
            <input v-model="filters.keyword" class="input input-bordered input-sm w-56" placeholder="搜索页面或标题..." />
            <select v-model="filters.enabled" class="select select-bordered select-sm w-32">
              <option :value="''">全部状态</option>
              <option :value="true">启用</option>
              <option :value="false">停用</option>
            </select>
            <button class="btn btn-sm btn-primary" @click="handleSearch">搜索</button>
            <button class="btn btn-sm btn-ghost" @click="handleReset">重置</button>
          </div>
        </div>
      </div>

      <div class="card bg-base-100 shadow-sm flex-1 min-h-0">
        <div class="card-body p-0 flex flex-col min-h-0">
          <div class="flex-1 overflow-auto" ref="scrollContainer">
            <div class="overflow-x-auto">
              <table class="table">
                <thead>
                  <tr>
                    <th class="w-40">页面</th>
                    <th class="w-40">标识</th>
                    <th>标题</th>
                    <th class="w-24">主题</th>
                    <th class="w-24">状态</th>
                    <th class="w-40">更新时间</th>
                    <th class="w-32">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="loading">
                    <td colspan="7" class="text-center py-8">
                      <span class="loading loading-spinner loading-md"></span>
                    </td>
                  </tr>
                  <tr v-else-if="heroList.length === 0">
                    <td colspan="7" class="text-center py-8 text-slate-500">暂无数据</td>
                  </tr>
                  <tr v-else v-for="hero in heroList" :key="hero.id" class="hover">
                    <td>{{ hero.pageName || '-' }}</td>
                    <td class="text-slate-500">{{ hero.pageKey }}</td>
                    <td class="max-w-xs">
                      <div class="font-medium line-clamp-1">{{ hero.titleStart }} {{ hero.titleHighlight }}</div>
                    </td>
                    <td class="text-slate-500">{{ getThemeLabel(hero.theme) }}</td>
                    <td>
                      <span class="badge badge-sm" :class="hero.enabled ? 'badge-success' : 'badge-ghost'">
                        {{ hero.enabled ? '启用' : '停用' }}
                      </span>
                    </td>
                    <td class="text-slate-500 text-sm">{{ formatDateTime(hero.updatedAt) }}</td>
                    <td>
                      <div class="flex gap-2">
                        <button class="btn btn-ghost btn-xs" @click="openEditModal(hero)" v-permission="['campus:hero:edit']">编辑</button>
                        <button class="btn btn-ghost btn-xs text-error" @click="handleDelete(hero)" v-permission="['campus:hero:delete']">删除</button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
              <div ref="loadMoreTrigger" class="h-6" aria-hidden="true"></div>
            </div>
          </div>
          <div class="flex justify-between items-center p-4 border-t text-sm text-slate-500">
            <div>已加载 {{ heroList.length }} / {{ total || '-' }} 条</div>
            <div v-if="loadingMore">正在加载更多...</div>
            <div v-else-if="!hasMore && heroList.length > 0">没有更多了</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Create/Edit Modal -->
    <dialog ref="formModal" class="modal">
      <div class="modal-box max-w-4xl">
        <h3 class="font-bold text-lg mb-4">{{ isEdit ? '编辑Hero配置' : '新增Hero配置' }}</h3>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div class="form-control">
            <label class="label"><span class="label-text">页面标识 *</span></label>
            <input v-model="form.pageKey" list="hero-page-keys" class="input input-bordered" placeholder="例如 HOME" />
            <datalist id="hero-page-keys">
              <option v-for="item in pageOptions" :key="item.key" :value="item.key">{{ item.name }}</option>
            </datalist>
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">页面名称</span></label>
            <input v-model="form.pageName" class="input input-bordered" placeholder="例如 首页" />
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">主题</span></label>
            <select v-model="form.theme" class="select select-bordered">
              <option v-for="item in themeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">排序</span></label>
            <input v-model.number="form.sortOrder" type="number" class="input input-bordered" />
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">主标题前缀 *</span></label>
            <input v-model="form.titleStart" class="input input-bordered" placeholder="例如 连接每一份" />
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">主标题高亮 *</span></label>
            <input v-model="form.titleHighlight" class="input input-bordered" placeholder="例如 校园心声" />
          </div>
          <div class="form-control md:col-span-2">
            <label class="label"><span class="label-text">描述</span></label>
            <textarea v-model="form.description" class="textarea textarea-bordered h-24" placeholder="页面描述"></textarea>
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">徽章</span></label>
            <input v-model="form.badge" class="input input-bordered" placeholder="例如 New v2.0 Released" />
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">主按钮文案</span></label>
            <input v-model="form.primaryBtnText" class="input input-bordered" placeholder="例如 开始探索" />
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">主按钮链接</span></label>
            <input v-model="form.primaryBtnLink" class="input input-bordered" placeholder="例如 /publish" />
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">次按钮文案</span></label>
            <input v-model="form.secondaryBtnText" class="input input-bordered" placeholder="例如 热门话题" />
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">次按钮链接</span></label>
            <input v-model="form.secondaryBtnLink" class="input input-bordered" placeholder="例如 /search" />
          </div>
          <div class="form-control">
            <label class="label cursor-pointer justify-start gap-3">
              <input v-model="form.enabled" type="checkbox" class="checkbox checkbox-primary" />
              <span class="label-text">启用配置</span>
            </label>
          </div>
          <div class="form-control">
            <label class="label cursor-pointer justify-start gap-3">
              <input v-model="form.showStats" type="checkbox" class="checkbox checkbox-primary" />
              <span class="label-text">显示统计区域</span>
            </label>
          </div>
          <div class="form-control md:col-span-2">
            <div class="alert bg-base-200/60 text-sm text-base-content/70">
              统计数字与头像由系统实时生成（首页=用户数，板块页=帖子数）。无需手动填写。
            </div>
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">浮层标签</span></label>
            <input v-model="form.floatCardLabel" class="input input-bordered" placeholder="例如 热门动态" />
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">浮层数值</span></label>
            <input v-model="form.floatCardValue" class="input input-bordered" placeholder="例如 +128" />
          </div>
        </div>
        <div class="modal-action">
          <button class="btn btn-ghost" @click="closeModal">取消</button>
          <button class="btn btn-primary" :disabled="saving" @click="handleSave">
            <span v-if="saving" class="loading loading-spinner loading-sm"></span>
            保存
          </button>
        </div>
      </div>
      <form method="dialog" class="modal-backdrop"><button>close</button></form>
    </dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, nextTick, reactive, ref } from 'vue'
import { createCampusHero, deleteCampusHero, getCampusHeroList, updateCampusHero, type CampusHeroDTO, type CampusHeroQuery, type CampusHeroVO } from '@/api/system'
import { useDialog } from '@/composables/useDialog'

const dialog = useDialog()
const heroList = ref<CampusHeroVO[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const scrollContainer = ref<HTMLElement | null>(null)
const loadMoreTrigger = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

const filters = reactive({
  keyword: '',
  enabled: '' as '' | boolean
})

const formModal = ref<HTMLDialogElement | null>(null)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const saving = ref(false)
const pageOptions = [
  { key: 'HOME', name: '首页' },
  { key: 'CONFESSIONS', name: '表白墙' },
  { key: 'TREEHOLE', name: '树洞' },
  { key: 'HELP', name: '求助' },
  { key: 'MARKET', name: '市集' },
  { key: 'LOST_FOUND', name: '失物' }
]

const themeOptions = [
  { value: 'blue', label: '蓝色' },
  { value: 'pink', label: '粉色' },
  { value: 'emerald', label: '祖母绿' },
  { value: 'orange', label: '橙色' },
  { value: 'purple', label: '紫色' },
  { value: 'sky', label: '天空蓝' },
  { value: 'rose', label: '玫瑰红' },
  { value: 'teal', label: '青绿' },
  { value: 'amber', label: '琥珀金' }
]

const form = ref<CampusHeroDTO>({
  pageKey: 'HOME',
  pageName: '首页',
  enabled: true,
  theme: 'blue',
  titleStart: '',
  titleHighlight: '',
  description: '',
  badge: '',
  primaryBtnText: '',
  primaryBtnLink: '',
  secondaryBtnText: '',
  secondaryBtnLink: '',
  showStats: true,
  statsNumber: '',
  statsLabel: '',
  avatarUrls: [],
  floatCardLabel: '',
  floatCardValue: '',
  sortOrder: 0
})

onMounted(() => {
  fetchData({ reset: true })
  nextTick(() => setupObserver())
})

onUnmounted(() => {
  observer?.disconnect()
  observer = null
})

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

const fetchData = async ({ append = false, reset = false } = {}) => {
  if (append && (loadingMore.value || loading.value)) return
  if (!append && loading.value) return
  if (reset) {
    page.value = 1
    heroList.value = []
    hasMore.value = true
  }
  append ? (loadingMore.value = true) : (loading.value = true)
  try {
    const params: CampusHeroQuery = { page: page.value, size: pageSize.value }
    if (filters.keyword) params.keyword = filters.keyword
    if (filters.enabled !== '') params.enabled = filters.enabled
    const res = await getCampusHeroList(params)
    const records = res.records || []
    total.value = res.total || 0
    heroList.value = append ? [...heroList.value, ...records] : records
    if (total.value) {
      hasMore.value = heroList.value.length < total.value
    } else {
      hasMore.value = records.length >= pageSize.value
    }
  } catch (error) {
    console.error('Failed to load hero configs', error)
  } finally {
    append ? (loadingMore.value = false) : (loading.value = false)
  }
}

const loadMore = async () => {
  if (!hasMore.value || loading.value || loadingMore.value) return
  page.value += 1
  await fetchData({ append: true })
}

const handleSearch = () => {
  fetchData({ reset: true })
}

const handleReset = () => {
  filters.keyword = ''
  filters.enabled = ''
  fetchData({ reset: true })
}

const openCreateModal = () => {
  isEdit.value = false
  editId.value = null
  form.value = {
    pageKey: 'HOME',
    pageName: '首页',
    enabled: true,
    theme: 'blue',
    titleStart: '',
    titleHighlight: '',
    description: '',
    badge: '',
    primaryBtnText: '',
    primaryBtnLink: '',
    secondaryBtnText: '',
    secondaryBtnLink: '',
    showStats: true,
    statsNumber: '',
    statsLabel: '',
    avatarUrls: [],
    floatCardLabel: '',
    floatCardValue: '',
    sortOrder: 0
  }
  formModal.value?.showModal()
}

const openEditModal = (hero: CampusHeroVO) => {
  isEdit.value = true
  editId.value = hero.id
  form.value = {
    pageKey: hero.pageKey,
    pageName: hero.pageName || '',
    enabled: hero.enabled,
    theme: hero.theme || 'blue',
    titleStart: hero.titleStart || '',
    titleHighlight: hero.titleHighlight || '',
    description: hero.description || '',
    badge: hero.badge || '',
    primaryBtnText: hero.primaryBtnText || '',
    primaryBtnLink: hero.primaryBtnLink || '',
    secondaryBtnText: hero.secondaryBtnText || '',
    secondaryBtnLink: hero.secondaryBtnLink || '',
    showStats: hero.showStats ?? true,
    statsNumber: hero.statsNumber || '',
    statsLabel: hero.statsLabel || '',
    avatarUrls: hero.avatarUrls || [],
    floatCardLabel: hero.floatCardLabel || '',
    floatCardValue: hero.floatCardValue || '',
    sortOrder: hero.sortOrder || 0
  }
  formModal.value?.showModal()
}

const closeModal = () => {
  formModal.value?.close()
}

const handleSave = async () => {
  if (!form.value.pageKey?.trim()) {
    await dialog.alert('请输入页面标识')
    return
  }
  if (!form.value.titleStart?.trim() || !form.value.titleHighlight?.trim()) {
    await dialog.alert('请输入完整标题')
    return
  }
  const normalizedKey = form.value.pageKey.trim().toUpperCase()
  form.value.pageKey = normalizedKey
  if (!form.value.pageName?.trim()) {
    const match = pageOptions.find(item => item.key === normalizedKey)
    if (match) {
      form.value.pageName = match.name
    }
  }
  form.value.statsNumber = ''
  form.value.statsLabel = ''
  form.value.avatarUrls = []

  saving.value = true
  try {
    if (isEdit.value && editId.value) {
      await updateCampusHero(editId.value, form.value)
    } else {
      await createCampusHero(form.value)
    }
    closeModal()
    fetchData({ reset: true })
  } catch (error: unknown) {
    await dialog.alert((error as ApiErrorLike)?.message || (error as ApiErrorLike)?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const handleDelete = async (hero: CampusHeroVO) => {
  if (!await dialog.confirm(`确定删除 ${hero.pageName || hero.pageKey} 的Hero配置吗？`)) return
  try {
    await deleteCampusHero(hero.id)
    fetchData({ reset: true })
  } catch (error: unknown) {
    await dialog.alert((error as ApiErrorLike)?.message || (error as ApiErrorLike)?.response?.data?.message || '删除失败')
  }
}

const getThemeLabel = (value?: string) => {
  if (!value) return '-'
  const match = themeOptions.find(item => item.value === value)
  return match ? match.label : value
}

const formatDateTime = (dateStr?: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}
</script>
