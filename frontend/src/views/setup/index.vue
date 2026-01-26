<template>
  <div class="min-h-screen bg-base-200 text-base-content">
    <div class="min-h-screen px-4 py-10 sm:py-14">
      <div class="mx-auto grid w-full max-w-6xl items-stretch gap-6 lg:grid-cols-[1.05fr,1.35fr]">
        <section class="relative overflow-hidden rounded-3xl bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 p-8 text-white shadow-2xl">
          <div class="absolute inset-0 bg-[radial-gradient(circle_at_top,rgba(255,255,255,0.12),transparent_60%)]"></div>
          <div class="relative space-y-6">
            <div class="inline-flex items-center gap-2 rounded-full bg-white/10 px-4 py-1 text-xs uppercase tracking-[0.2em]">
              初始化向导
            </div>
            <div>
              <h1 class="text-3xl font-semibold">CampusWall 部署初始化</h1>
              <p class="mt-3 text-sm text-white/70">
                完成管理员账户与站点基础配置后即可开始使用。
              </p>
            </div>
            <ul class="steps steps-vertical text-white">
              <li class="step step-primary">创建管理员</li>
              <li class="step step-primary">配置站点信息</li>
              <li class="step step-primary">确认存储策略</li>
            </ul>
            <div class="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">
              角色与菜单权限通过 SQL 初始化，无需在此页面配置。
            </div>
          </div>
        </section>

        <section class="card border border-base-200 bg-base-100 shadow-xl">
          <div class="card-body space-y-6">
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h2 class="text-2xl font-semibold">系统初始化</h2>
                <p class="text-sm text-base-content/60">建议仅在首次部署时运行。</p>
              </div>
              <span class="badge badge-outline">LOCAL</span>
            </div>

            <div v-if="loadingStatus" class="space-y-3">
              <div class="skeleton h-10 w-full"></div>
              <div class="skeleton h-32 w-full"></div>
              <div class="skeleton h-10 w-2/3"></div>
            </div>

            <div v-else-if="status?.setupCompleted" class="space-y-4">
              <div class="alert alert-success">
                <span>系统已完成初始化。</span>
              </div>
              <div class="rounded-2xl border border-base-200 bg-base-200/60 p-4 text-sm">
                <div class="font-medium">当前站点信息</div>
                <div class="mt-2 space-y-1 text-base-content/70">
                  <div>站点名称：{{ status.siteName || '未设置' }}</div>
                  <div>存储方式：{{ status.storageProvider || 'LOCAL' }}</div>
                  <div>本地路径：{{ status.localPath || '-' }}</div>
                </div>
              </div>
              <div class="flex gap-3">
                <button class="btn btn-primary" @click="goHome">进入首页</button>
                <button class="btn btn-outline" @click="goConsole">进入控制台</button>
              </div>
            </div>

            <form v-else class="space-y-6" @submit.prevent="handleSubmit">
              <div class="space-y-4">
                <div class="flex items-center justify-between">
                  <h3 class="text-lg font-semibold">管理员账号</h3>
                  <span class="badge badge-ghost">步骤 1</span>
                </div>
                <div class="grid gap-4 md:grid-cols-2">
                  <label class="form-control w-full">
                    <div class="label">
                      <span class="label-text">管理员账号</span>
                    </div>
                    <input v-model.trim="form.adminUsername" class="input input-bordered w-full" placeholder="例如 admin" />
                  </label>
                  <label class="form-control w-full">
                    <div class="label">
                      <span class="label-text">管理员昵称</span>
                    </div>
                    <input v-model.trim="form.adminNickname" class="input input-bordered w-full" placeholder="系统管理员" />
                  </label>
                  <label class="form-control w-full">
                    <div class="label">
                      <span class="label-text">管理员邮箱</span>
                    </div>
                    <input v-model.trim="form.adminEmail" class="input input-bordered w-full" placeholder="admin@example.com" />
                  </label>
                  <label class="form-control w-full">
                    <div class="label">
                      <span class="label-text">显示密码</span>
                    </div>
                    <div class="flex items-center gap-2">
                      <input type="checkbox" class="toggle toggle-primary" v-model="showPassword" />
                      <span class="text-sm text-base-content/70">开启后可查看密码</span>
                    </div>
                  </label>
                  <label class="form-control w-full">
                    <div class="label">
                      <span class="label-text">管理员密码</span>
                    </div>
                    <input
                      v-model="form.adminPassword"
                      :type="showPassword ? 'text' : 'password'"
                      class="input input-bordered w-full"
                      placeholder="设置登录密码"
                    />
                  </label>
                  <label class="form-control w-full">
                    <div class="label">
                      <span class="label-text">确认密码</span>
                    </div>
                    <input
                      v-model="form.adminConfirmPassword"
                      :type="showPassword ? 'text' : 'password'"
                      class="input input-bordered w-full"
                      placeholder="再次输入密码"
                    />
                  </label>
                </div>
              </div>

              <div class="divider">站点信息</div>

              <div class="space-y-4">
                <div class="flex items-center justify-between">
                  <h3 class="text-lg font-semibold">站点配置</h3>
                  <span class="badge badge-ghost">步骤 2</span>
                </div>
                <div class="grid gap-4 md:grid-cols-2">
                  <label class="form-control w-full md:col-span-2">
                    <div class="label">
                      <span class="label-text">站点名称</span>
                    </div>
                    <input v-model.trim="form.siteName" class="input input-bordered w-full" placeholder="CampusWall" />
                  </label>
                  <label class="form-control w-full">
                    <div class="label">
                      <span class="label-text">站点 Logo</span>
                    </div>
                    <input v-model.trim="form.logoUrl" class="input input-bordered w-full" placeholder="https://..." />
                  </label>
                  <label class="form-control w-full">
                    <div class="label">
                      <span class="label-text">站点图标</span>
                    </div>
                    <input v-model.trim="form.faviconUrl" class="input input-bordered w-full" placeholder="https://..." />
                  </label>
                  <label class="form-control w-full">
                    <div class="label">
                      <span class="label-text">主题色</span>
                    </div>
                    <select v-model="form.theme" class="select select-bordered w-full">
                      <option v-for="option in themeOptions" :key="option.value" :value="option.value">
                        {{ option.label }}
                      </option>
                    </select>
                  </label>
                </div>
              </div>

              <div class="divider">存储策略</div>

              <div class="space-y-4">
                <div class="flex items-center justify-between">
                  <h3 class="text-lg font-semibold">本地存储</h3>
                  <span class="badge badge-ghost">步骤 3</span>
                </div>
                <div class="grid gap-4 md:grid-cols-2">
                  <label class="form-control w-full">
                    <div class="label">
                      <span class="label-text">存储路径</span>
                    </div>
                    <input v-model.trim="form.localPath" class="input input-bordered w-full" placeholder="/data/uploads" />
                    <div class="label">
                      <span class="label-text-alt text-base-content/60">建议使用绝对路径并挂载持久卷。</span>
                    </div>
                  </label>
                  <label class="form-control w-full">
                    <div class="label">
                      <span class="label-text">公开访问</span>
                    </div>
                    <div class="flex items-center gap-3">
                      <input type="checkbox" class="toggle toggle-primary" v-model="form.localPublicEnabled" />
                      <span class="text-sm text-base-content/70">不推荐开启同源静态公开</span>
                    </div>
                  </label>
                </div>
                <div class="rounded-2xl border border-base-200 bg-base-200/70 p-4 text-sm text-base-content/70">
                  当前仅支持本地存储，后续可通过插件接入 S3 等对象存储。
                </div>
              </div>

              <div class="pt-2">
                <button class="btn btn-primary w-full" type="submit" :disabled="submitting">
                  <span v-if="submitting" class="loading loading-spinner"></span>
                  <span>{{ submitting ? '正在初始化...' : '完成初始化' }}</span>
                </button>
              </div>
            </form>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useDialog } from '@/composables/useDialog'
import { getSetupStatus, initSetup, type SetupInitPayload, type SetupStatus } from '@/api/setup'

const router = useRouter()
const dialog = useDialog()

const status = ref<SetupStatus | null>(null)
const loadingStatus = ref(true)
const submitting = ref(false)
const showPassword = ref(false)

const themeOptions = [
  { value: 'blue', label: '海蓝' },
  { value: 'emerald', label: '青绿' },
  { value: 'cyan', label: '水青' },
  { value: 'indigo', label: '靛蓝' },
  { value: 'violet', label: '暮紫' },
  { value: 'rose', label: '玫瑰' },
  { value: 'amber', label: '琥珀' },
  { value: 'slate', label: '石墨' }
]

const form = reactive<SetupInitPayload>({
  adminUsername: '',
  adminPassword: '',
  adminConfirmPassword: '',
  adminNickname: '',
  adminEmail: '',
  siteName: 'CampusWall',
  logoUrl: '',
  faviconUrl: '',
  theme: 'blue',
  storageProvider: 'LOCAL',
  localPath: '',
  localPublicEnabled: false
})

const loadStatus = async () => {
  loadingStatus.value = true
  try {
    const res = await getSetupStatus()
    status.value = res
    if (!res.setupCompleted) {
      form.storageProvider = res.storageProvider || 'LOCAL'
      form.localPath = res.localPath || ''
      form.localPublicEnabled = Boolean(res.localPublicEnabled)
    }
  } catch (error: any) {
    status.value = null
    await dialog.alert(error?.message || '无法获取初始化状态')
  } finally {
    loadingStatus.value = false
  }
}

const handleSubmit = async () => {
  if (!form.adminUsername || !form.adminPassword || !form.adminConfirmPassword || !form.siteName) {
    await dialog.alert('请填写管理员账号、密码和站点名称')
    return
  }
  if (form.adminPassword !== form.adminConfirmPassword) {
    await dialog.alert('两次密码输入不一致')
    return
  }
  submitting.value = true
  try {
    await initSetup(form)
    await dialog.alert('初始化完成，即将进入系统。', { title: '成功' })
    window.location.href = '/'
  } catch (error: any) {
    await dialog.alert(error?.message || '初始化失败，请检查配置')
  } finally {
    submitting.value = false
  }
}

const goHome = () => router.push('/')
const goConsole = () => router.push('/console')

onMounted(loadStatus)
</script>
