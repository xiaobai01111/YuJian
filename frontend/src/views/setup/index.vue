<template>
  <div class="min-h-screen w-full bg-base-200 flex items-center justify-center p-4 lg:p-8">
    <!-- Main Card Container -->
    <div class="w-full max-w-5xl bg-base-100 rounded-3xl shadow-2xl overflow-hidden min-h-[600px] flex flex-col lg:flex-row animate-fade-in">
      
      <!-- Sidebar / Steps -->
      <div class="lg:w-[320px] bg-slate-900 text-white p-8 flex flex-col relative shrink-0">
        <!-- Decorative Background -->
        <div class="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(255,255,255,0.1),transparent_50%)]"></div>
        <div class="absolute bottom-0 right-0 p-10 opacity-5">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="w-48 h-48">
             <path fill-rule="evenodd" d="M12.97 3.97a.75.75 0 011.06 0l7.5 7.5a.75.75 0 010 1.06l-7.5 7.5a.75.75 0 11-1.06-1.06l6.22-6.22H3a.75.75 0 010-1.5h16.19l-6.22-6.22a.75.75 0 010-1.06z" clip-rule="evenodd" />
          </svg>
        </div>

        <!-- Content -->
        <div class="relative z-10 flex flex-col h-full">
            <div class="flex items-center gap-3 mb-12">
                <div class="w-10 h-10 rounded-xl bg-gradient-to-tr from-primary to-primary-focus flex items-center justify-center font-bold text-xl shadow-lg">C</div>
                <span class="font-bold text-xl tracking-wide">CampusWall</span>
            </div>

            <!-- Steps List -->
            <div class="space-y-2 flex-1">
                <div v-for="(step, index) in steps" :key="index" 
                    class="group flex items-center gap-4 p-3 rounded-xl transition-all duration-300"
                    :class="[
                        currentStep === index + 1 ? 'bg-white/10 text-white translate-x-2' : 
                        currentStep > index + 1 ? 'text-white/60' : 'text-white/30'
                    ]"
                >
                    <div class="relative flex-shrink-0 w-8 h-8 rounded-full border-2 flex items-center justify-center text-sm font-bold transition-all duration-300"
                        :class="[
                             currentStep === index + 1 ? 'border-primary bg-primary text-white scale-110 shadow-lg shadow-primary/30' : 
                             currentStep > index + 1 ? 'border-emerald-500 bg-emerald-500 text-white border-transparent' : 'border-current'
                        ]"
                    >
                        <span v-if="currentStep > index + 1">
                            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="3" stroke="currentColor" class="w-4 h-4">
                                <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                            </svg>
                        </span>
                        <span v-else>{{ index + 1 }}</span>
                    </div>
                    <div class="flex flex-col">
                        <span class="font-medium text-sm transition-colors group-hover:text-white">{{ step.title }}</span>
                        <span class="text-xs opacity-70">{{ step.desc }}</span>
                    </div>
                </div>
            </div>
            
            <!-- Info Box -->
            <div class="mt-8 rounded-xl border border-white/10 bg-white/5 p-4 text-xs text-white/60 backdrop-blur-sm">
                <div class="font-medium text-white/80 mb-1">提示</div>
                角色与菜单权限将通过 SQL 自动初始化，无需在此配置。
            </div>

            <div class="mt-6 text-xs text-white/30 text-center">
                &copy; {{ new Date().getFullYear() }} CampusWall Setup
            </div>
        </div>
      </div>

      <!-- Main Content Area -->
      <div class="flex-1 flex flex-col relative bg-base-100">
         <!-- Loading State -->
         <div v-if="loadingStatus" class="flex-1 flex flex-col items-center justify-center space-y-6 animate-pulse">
             <div class="loading loading-spinner loading-lg text-primary"></div>
             <div class="text-base-content/50 font-medium">正在检查系统状态...</div>
         </div>

         <!-- Already Setup State -->
         <div v-else-if="status?.setupCompleted" class="flex-1 flex flex-col items-center justify-center text-center p-8 space-y-8 animate-fade-in">
             <div class="w-24 h-24 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center shadow-sm">
                 <svg class="w-12 h-12" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" /></svg>
             </div>
             <div>
                <h2 class="text-3xl font-bold text-base-content">系统已就绪</h2>
                <p class="mt-3 text-base-content/60 text-lg">您的 CampusWall 站点已完成初始化配置。</p>
             </div>
             
             <div class="w-full max-w-md bg-base-200/50 rounded-2xl p-6 text-left space-y-4 border border-base-200">
                 <div class="flex justify-between items-center text-sm border-b border-base-content/5 pb-3">
                     <span class="text-base-content/60">站点名称</span>
                     <span class="font-semibold text-lg">{{ status.siteName }}</span>
                 </div>
                 <div class="flex justify-between items-center text-sm">
                     <span class="text-base-content/60">存储模式</span>
                     <span class="badge badge-primary badge-outline">{{ status.storageProvider || 'LOCAL' }}</span>
                 </div>
                 <div class="flex justify-between items-center text-sm">
                     <span class="text-base-content/60">本地路径</span>
                     <span class="font-mono text-xs bg-base-300 px-2 py-1 rounded">{{ status.localPath || '-' }}</span>
                 </div>
             </div>

             <div class="flex flex-wrap gap-4 justify-center pt-4">
                 <button class="btn btn-primary btn-lg px-8 shadow-lg shadow-primary/20" @click="goHome">进入首页</button>
                 <button class="btn btn-outline btn-lg" @click="goConsole">进入控制台</button>
             </div>
         </div>

         <!-- Form Wizard -->
         <div v-else class="flex-1 flex flex-col h-full overflow-hidden">
            <!-- Header -->
            <div class="p-8 pb-4 border-b border-base-200/50">
                <div class="flex justify-between items-end">
                    <div>
                        <h2 class="text-2xl font-bold text-base-content">{{ steps[currentStep - 1]?.title }}</h2>
                        <p class="text-base-content/60 mt-1">{{ steps[currentStep - 1]?.desc }}</p>
                    </div>
                    <div class="text-sm font-medium text-primary">
                        步骤 {{ currentStep }} / {{ steps.length }}
                    </div>
                </div>
            </div>

            <!-- Scrollable Form Area -->
            <form class="flex-1 overflow-y-auto p-8 custom-scrollbar" @submit.prevent>
                
                <!-- Step 1: Admin -->
                <div v-show="currentStep === 1" class="space-y-6 animate-slide-up">
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div class="form-control w-full">
                            <label class="label">
                                <span class="label-text font-medium">管理员账号</span>
                                <span class="label-text-alt text-error">*</span>
                            </label>
                            <input v-model.trim="form.adminUsername" class="input input-bordered w-full focus:input-primary" placeholder="例如 admin" />
                        </div>
                        <div class="form-control w-full">
                            <label class="label">
                                <span class="label-text font-medium">管理员昵称</span>
                            </label>
                            <input v-model.trim="form.adminNickname" class="input input-bordered w-full focus:input-primary" placeholder="系统管理员" />
                        </div>
                        <div class="form-control w-full md:col-span-2">
                            <label class="label">
                                <span class="label-text font-medium">管理员邮箱</span>
                            </label>
                            <input v-model.trim="form.adminEmail" class="input input-bordered w-full focus:input-primary" placeholder="admin@example.com" />
                        </div>
                        
                        <div class="form-control w-full">
                            <label class="label">
                                <span class="label-text font-medium">登录密码</span>
                                <span class="label-text-alt text-error">*</span>
                            </label>
                            <div class="relative">
                                <input 
                                    v-model="form.adminPassword" 
                                    :type="showPassword ? 'text' : 'password'" 
                                    class="input input-bordered w-full focus:input-primary pr-10" 
                                    placeholder="设置强密码" 
                                />
                                <button type="button" class="absolute right-3 top-1/2 -translate-y-1/2 text-base-content/40 hover:text-base-content" @click="showPassword = !showPassword">
                                    <svg v-if="showPassword" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" /></svg>
                                    <svg v-else class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></svg>
                                </button>
                            </div>
                        </div>
                        <div class="form-control w-full">
                            <label class="label">
                                <span class="label-text font-medium">确认密码</span>
                                <span class="label-text-alt text-error">*</span>
                            </label>
                            <input 
                                v-model="form.adminConfirmPassword" 
                                :type="showPassword ? 'text' : 'password'" 
                                class="input input-bordered w-full focus:input-primary" 
                                placeholder="再次输入密码" 
                            />
                        </div>
                    </div>
                </div>

                <!-- Step 2: Site -->
                 <div v-show="currentStep === 2" class="space-y-6 animate-slide-up">
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div class="form-control w-full md:col-span-2">
                            <label class="label">
                                <span class="label-text font-medium">站点名称</span>
                                <span class="label-text-alt text-error">*</span>
                            </label>
                            <input v-model.trim="form.siteName" class="input input-bordered w-full focus:input-primary" placeholder="CampusWall" />
                        </div>
                        
                        <div class="form-control w-full">
                            <label class="label">
                                <span class="label-text font-medium">站点 Logo URL</span>
                            </label>
                            <input v-model.trim="form.logoUrl" class="input input-bordered w-full focus:input-primary" placeholder="https://..." />
                        </div>
                        <div class="form-control w-full">
                            <label class="label">
                                <span class="label-text font-medium">站点 Favicon URL</span>
                            </label>
                            <input v-model.trim="form.faviconUrl" class="input input-bordered w-full focus:input-primary" placeholder="https://..." />
                        </div>
                        
                        <div class="form-control w-full md:col-span-2">
                            <label class="label">
                                <span class="label-text font-medium">主题色</span>
                            </label>
                            <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
                                <div v-for="option in themeOptions" :key="option.value" 
                                    class="cursor-pointer rounded-lg border p-3 flex items-center gap-3 transition-all"
                                    :class="form.theme === option.value ? 'border-primary bg-primary/5 ring-1 ring-primary' : 'border-base-300 hover:border-base-content/30'"
                                    @click="form.theme = option.value"
                                >
                                    <div class="w-4 h-4 rounded-full" :class="option.colorClass"></div>
                                    <span class="text-sm font-medium">{{ option.label }}</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Step 3: Storage -->
                 <div v-show="currentStep === 3" class="space-y-6 animate-slide-up">
                    <div class="alert shadow-sm border border-info/20 bg-info/5 text-sm">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="stroke-info shrink-0 w-6 h-6"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                        <span>当前仅支持本地存储，后续可通过插件接入 S3 等对象存储。</span>
                    </div>

                    <div class="form-control w-full">
                        <label class="label">
                            <span class="label-text font-medium">存储路径</span>
                        </label>
                        <input v-model.trim="form.localPath" class="input input-bordered w-full focus:input-primary font-mono text-sm" placeholder="/data/uploads" />
                        <div class="label">
                            <span class="label-text-alt text-base-content/60">建议使用绝对路径并挂载持久卷。</span>
                        </div>
                    </div>

                    <div class="form-control w-full">
                        <label class="label cursor-pointer justify-start gap-4">
                            <span class="label-text font-medium">公开访问 (Insecure)</span>
                            <input type="checkbox" class="toggle toggle-primary" v-model="form.localPublicEnabled" />
                        </label>
                        <div class="pl-1 text-xs text-base-content/60 max-w-lg">
                            开启后允许直接通过 Web 服务器访问静态文件。为了安全性，建议保持关闭，走 API 鉴权访问。
                        </div>
                    </div>
                </div>

            </form>

            <!-- Footer Actions -->
            <div class="p-6 border-t border-base-200/50 flex justify-between items-center bg-base-100/50 backdrop-blur-sm">
                <button 
                    v-if="currentStep > 1" 
                    class="btn btn-ghost hover:bg-base-200 gap-2" 
                    @click="prevStep"
                >
                    <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" /></svg>
                    上一步
                </button>
                <div v-else></div> <!-- Spacer -->

                <button 
                    v-if="currentStep < 3" 
                    class="btn btn-primary gap-2 px-8 shadow-lg shadow-primary/20" 
                    @click="nextStep"
                >
                    下一步
                    <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" /></svg>
                </button>
                <button 
                    v-else 
                    class="btn btn-primary gap-2 px-8 shadow-lg shadow-primary/20" 
                    :class="{ 'loading': submitting }"
                    @click="handleSubmit"
                    :disabled="submitting"
                >
                    完成初始化
                </button>
            </div>
         </div>

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
const currentStep = ref(1)

const steps = [
  { title: '管理员账号', desc: '配置最高权限管理员' },
  { title: '站点信息', desc: '自定义站点外观与名称' },
  { title: '存储策略', desc: '配置文件的存储位置' }
]

const themeOptions = [
  { value: 'blue', label: '海蓝', colorClass: 'bg-blue-500' },
  { value: 'emerald', label: '青绿', colorClass: 'bg-emerald-500' },
  { value: 'cyan', label: '水青', colorClass: 'bg-cyan-500' },
  { value: 'indigo', label: '靛蓝', colorClass: 'bg-indigo-500' },
  { value: 'violet', label: '暮紫', colorClass: 'bg-violet-500' },
  { value: 'rose', label: '玫瑰', colorClass: 'bg-rose-500' },
  { value: 'amber', label: '琥珀', colorClass: 'bg-amber-500' },
  { value: 'slate', label: '石墨', colorClass: 'bg-slate-500' }
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
  } catch (error: unknown) {
    status.value = null
    await dialog.alert((error as ApiErrorLike)?.message || '无法获取初始化状态')
  } finally {
    loadingStatus.value = false
  }
}

const validateStep = async (step: number): Promise<boolean> => {
    if (step === 1) {
        if (!form.adminUsername) {
            await dialog.alert('请输入管理员账号', { title: '提示' })
            return false
        }
        if (!form.adminPassword) {
            await dialog.alert('请输入管理员密码', { title: '提示' })
            return false
        }
        if (form.adminPassword !== form.adminConfirmPassword) {
            await dialog.alert('两次输入的密码不一致', { title: '提示' })
            return false
        }
    } else if (step === 2) {
        if (!form.siteName) {
            await dialog.alert('请输入站点名称', { title: '提示' })
            return false
        }
    }
    return true
}

const nextStep = async () => {
    if (await validateStep(currentStep.value)) {
        if (currentStep.value < steps.length) {
            currentStep.value++
        }
    }
}

const prevStep = () => {
    if (currentStep.value > 1) {
        currentStep.value--
    }
}

const handleSubmit = async () => {
  // Final validation
  if (!await validateStep(1) || !await validateStep(2)) return
  
  submitting.value = true
  try {
    await initSetup(form)
    await dialog.alert('初始化完成，即将进入系统。', { title: '成功' })
    window.location.href = '/'
  } catch (error: unknown) {
    await dialog.alert((error as ApiErrorLike)?.message || '初始化失败，请检查配置')
  } finally {
    submitting.value = false
  }
}

const goHome = () => router.push('/')
const goConsole = () => router.push('/console')

onMounted(loadStatus)
</script>

<style scoped>
/* Custom Scrollbar for the form area */
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: 20px;
}
.dark .custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.1);
}

/* Animations */
@keyframes fadeIn {
    from { opacity: 0; }
    to { opacity: 1; }
}
.animate-fade-in {
    animation: fadeIn 0.5s ease-out;
}

@keyframes slideUp {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
}
.animate-slide-up {
    animation: slideUp 0.3s ease-out;
}
</style>
