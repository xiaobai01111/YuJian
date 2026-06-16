<template>
  <div class="h-full flex flex-col gap-4 p-6">
    <div>
      <h1 class="text-2xl font-bold text-slate-800">上传策略</h1>
      <p class="text-slate-500 mt-1">为各业务场景设置上传归档类型与默认可见性</p>
      <p class="text-xs text-slate-400 mt-1">公共媒体库/业务附件库/系统资源库用于后台库上传，公共上传用于前台通用上传。</p>
    </div>

    <div class="card bg-base-100 shadow-sm flex-1 overflow-auto">
      <div class="card-body">
        <div class="flex items-center justify-between mb-4">
          <div>
            <h2 class="card-title text-lg">策略配置</h2>
            <p class="text-xs text-slate-400">库管理用于设置库级默认可见性；策略管理用于细分业务场景的归档与可见性。</p>
          </div>
          <button class="btn btn-sm btn-ghost" :disabled="loading" @click="fetchPolicies">
            刷新
          </button>
        </div>

        <div v-if="!canView" class="text-slate-500 text-center py-10">无权限查看上传策略</div>

        <div v-else class="space-y-10">
          <section class="space-y-3">
            <div>
              <h3 class="text-base font-semibold">库管理</h3>
              <p class="text-xs text-slate-500">仅管理三个库的默认可见性，更新后会同步已归档文件。</p>
            </div>
            <div class="overflow-x-auto">
              <table class="table table-sm">
                <thead>
                  <tr>
                    <th>资源库</th>
                    <th class="w-40">默认可见性</th>
                    <th class="w-40">更新时间</th>
                    <th class="w-20">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="loading">
                    <td colspan="4" class="text-center py-6">加载中...</td>
                  </tr>
                  <tr v-else-if="libraryPolicies.length === 0">
                    <td colspan="4" class="text-center py-6 text-slate-400">暂无库管理配置</td>
                  </tr>
                  <tr v-else v-for="policy in libraryPolicies" :key="policy.sceneCode">
                    <td>
                      <div class="font-medium">{{ formatSceneName(policy) }}</div>
                      <div class="text-xs text-slate-500">{{ policy.sceneCode }}</div>
                    </td>
                    <td>
                      <select v-model="policy.visibility" class="select select-sm w-full" :disabled="!canEdit">
                        <option v-for="option in visibilityOptions" :key="option.value" :value="option.value">
                          {{ option.label }}
                        </option>
                      </select>
                    </td>
                    <td class="text-sm text-slate-500">{{ formatDate(policy.updatedAt) }}</td>
                    <td>
                      <button
                        class="btn btn-xs btn-primary"
                        :disabled="!canEdit || savingMap[policy.sceneCode]"
                        @click="savePolicy(policy)"
                      >
                        <span v-if="savingMap[policy.sceneCode]" class="loading loading-spinner loading-xs"></span>
                        保存
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <section class="space-y-3">
            <div>
              <h3 class="text-base font-semibold">策略管理</h3>
              <p class="text-xs text-slate-500">用于细分业务场景（如头像、身份材料），仅影响后续上传。</p>
            </div>

            <div class="flex flex-wrap items-center gap-3">
              <input
                v-model.trim="filters.keyword"
                class="input input-bordered input-sm w-64"
                placeholder="搜索场景名称/编码"
              />
              <select v-model="filters.assetType" class="select select-sm">
                <option value="">全部归档类型</option>
                <option v-for="option in assetTypeOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
              <select v-model="filters.visibility" class="select select-sm">
                <option value="">全部可见性</option>
                <option v-for="option in visibilityOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
              <button class="btn btn-sm btn-ghost" @click="resetFilters">清除筛选</button>
              <div class="text-xs text-slate-500">共 {{ filteredStrategies.length }} 条</div>
            </div>

            <div class="overflow-x-auto">
              <table class="table table-sm">
                <thead>
                  <tr>
                    <th>场景</th>
                    <th class="w-48">归档类型</th>
                    <th class="w-40">默认可见性</th>
                    <th class="w-40">更新时间</th>
                    <th class="w-20">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="loading">
                    <td colspan="5" class="text-center py-6">加载中...</td>
                  </tr>
                  <tr v-else-if="filteredStrategies.length === 0">
                    <td colspan="5" class="text-center py-6 text-slate-400">暂无策略配置</td>
                  </tr>
                  <tr v-else v-for="policy in filteredStrategies" :key="policy.sceneCode">
                    <td>
                      <div class="font-medium">{{ formatSceneName(policy) }}</div>
                      <div class="text-xs text-slate-500">{{ policy.sceneCode }}</div>
                    </td>
                    <td>
                      <select v-model="policy.assetType" class="select select-sm w-full" :disabled="!canEdit">
                        <option v-for="option in assetTypeOptions" :key="option.value" :value="option.value">
                          {{ option.label }}
                        </option>
                      </select>
                    </td>
                    <td>
                      <select v-model="policy.visibility" class="select select-sm w-full" :disabled="!canEdit">
                        <option v-for="option in visibilityOptions" :key="option.value" :value="option.value">
                          {{ option.label }}
                        </option>
                      </select>
                    </td>
                    <td class="text-sm text-slate-500">{{ formatDate(policy.updatedAt) }}</td>
                    <td>
                      <button
                        class="btn btn-xs btn-primary"
                        :disabled="!canEdit || savingMap[policy.sceneCode]"
                        @click="savePolicy(policy)"
                      >
                        <span v-if="savingMap[policy.sceneCode]" class="loading loading-spinner loading-xs"></span>
                        保存
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { getUploadPolicies, updateUploadPolicy, type UploadPolicyVO } from '@/api/system'
import { useDialog } from '@/composables/useDialog'
import { useUserStore } from '@/stores/user'

type UploadPolicyRow = UploadPolicyVO & {
  visibility: string
}

const dialog = useDialog()
const userStore = useUserStore()
const loading = ref(false)
const policies = ref<UploadPolicyRow[]>([])
const savingMap = reactive<Record<string, boolean>>({})
const libraryCodeOrder = ['gallery', 'file', 'resource']
const libraryCodeSet = new Set(libraryCodeOrder)
const filters = reactive({
  keyword: '',
  assetType: '',
  visibility: ''
})

const canView = computed(() => userStore.hasPermission('system:upload-policy:list'))
const canEdit = computed(() => userStore.hasPermission('system:upload-policy:edit'))

const assetTypeOptions = [
  { value: 'gallery', label: '公共媒体库' },
  { value: 'file', label: '业务附件库' },
  { value: 'resource', label: '系统资源库' }
]

const visibilityOptions = [
  { value: '', label: '默认' },
  { value: 'PUBLIC', label: '公有' },
  { value: 'PRIVATE', label: '私有' }
]

const libraryPolicies = computed(() => {
  const map = new Map(policies.value.map(policy => [policy.sceneCode.toLowerCase(), policy]))
  return libraryCodeOrder
    .map(code => map.get(code))
    .filter((item): item is UploadPolicyRow => Boolean(item))
})

const strategyPolicies = computed(() => {
  return policies.value.filter(policy => !libraryCodeSet.has(policy.sceneCode.toLowerCase()))
})

const filteredStrategies = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return strategyPolicies.value.filter(policy => {
    const matchesKeyword = !keyword
      || (policy.sceneName || '').toLowerCase().includes(keyword)
      || policy.sceneCode.toLowerCase().includes(keyword)
    const matchesAssetType = !filters.assetType || policy.assetType === filters.assetType
    const matchesVisibility = !filters.visibility || policy.visibility === filters.visibility
    return matchesKeyword && matchesAssetType && matchesVisibility
  })
})

const resetFilters = () => {
  filters.keyword = ''
  filters.assetType = ''
  filters.visibility = ''
}

const fetchPolicies = async () => {
  if (!canView.value) return
  loading.value = true
  try {
    const res = await getUploadPolicies()
    policies.value = (res || []).map(item => ({
      ...item,
      visibility: item.visibility || ''
    }))
  } catch (error) {
    dialog.alert('加载上传策略失败')
  } finally {
    loading.value = false
  }
}

const savePolicy = async (policy: UploadPolicyRow) => {
  if (!canEdit.value) return
  savingMap[policy.sceneCode] = true
  try {
    const res = await updateUploadPolicy(policy.sceneCode, {
      assetType: policy.assetType,
      visibility: policy.visibility || ''
    })
    if (res) {
      policy.assetType = res.assetType
      policy.visibility = res.visibility || ''
      policy.updatedAt = res.updatedAt
    }
  } catch (error) {
    dialog.alert('保存上传策略失败')
  } finally {
    savingMap[policy.sceneCode] = false
  }
}

const formatDate = (value?: string) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}

const formatSceneName = (policy: UploadPolicyRow) => {
  if (policy.sceneCode?.toLowerCase() === 'public') {
    return '公共上传（前台）'
  }
  return policy.sceneName || policy.sceneCode
}

onMounted(() => {
  fetchPolicies()
})
</script>
