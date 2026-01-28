<template>
  <div v-if="loading" class="flex justify-center items-center py-20">
    <span class="loading loading-spinner loading-lg"></span>
  </div>
  <div v-else class="grid grid-cols-1 xl:grid-cols-3 gap-6">
    <!-- Profile Summary -->
    <div class="card bg-base-100 shadow-xl">
      <div class="card-body">
        <div class="flex items-center gap-4">
          <div class="avatar" :class="{ 'placeholder': !userInfo?.avatar }">
            <div class="bg-primary text-primary-content rounded-full w-20" v-if="!userInfo?.avatar">
              <span class="text-2xl">{{ avatarText }}</span>
            </div>
            <div class="w-20 rounded-full" v-else>
              <img :src="userInfo.avatar" alt="头像" />
            </div>
          </div>
          <div>
            <h2 class="text-xl font-semibold">{{ userInfo?.nickname || '用户' }}</h2>
            <p class="text-slate-500">@{{ userInfo?.username || '-' }}</p>
            <div class="mt-2 flex flex-wrap gap-2">
              <span v-if="verifyStatusBadge" :class="verifyStatusBadge.class">{{ verifyStatusBadge.text }}</span>
              <span v-if="userInfo?.verifyMethod" class="badge badge-outline badge-sm">{{ formatVerifyMethod(userInfo.verifyMethod) }}</span>
              <span v-if="userInfo?.creditScore != null" class="badge badge-outline">信用 {{ userInfo.creditScore }}</span>
            </div>
          </div>
        </div>

        <div class="divider my-4"></div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
          <div><span class="font-semibold">邮箱：</span>{{ userInfo?.email || '未设置' }}</div>
          <div><span class="font-semibold">手机：</span>{{ userInfo?.phone || '未设置' }}</div>
          <div><span class="font-semibold">教育邮箱：</span>{{ userInfo?.eduEmail || '未绑定' }}</div>
          <div><span class="font-semibold">性别：</span>{{ formatSex(userInfo?.sex) }}</div>
          <div><span class="font-semibold">注册时间：</span>{{ formatDate(userInfo?.createdAt) }}</div>
          <div><span class="font-semibold">更新时间：</span>{{ formatDate(userInfo?.updatedAt) }}</div>
        </div>

        <div class="mt-4">
          <div class="text-sm font-semibold mb-2">角色</div>
          <div class="flex flex-wrap gap-2">
            <span v-for="role in displayRoles" :key="role" class="badge badge-ghost badge-sm">{{ role }}</span>
          </div>
        </div>

      </div>
    </div>

    <!-- Forms -->
    <div class="xl:col-span-2 space-y-6">
      <!-- Tabs -->
      <div class="tabs tabs-boxed bg-base-100 p-1">
        <a class="tab" :class="{ 'tab-active': activeTab === 'profile' }" @click="activeTab = 'profile'">基本资料</a>
        <a class="tab" :class="{ 'tab-active': activeTab === 'password' }" @click="activeTab = 'password'">修改密码</a>
        <a class="tab" :class="{ 'tab-active': activeTab === 'verify' }" @click="activeTab = 'verify'">身份认证</a>
      </div>

      <!-- Basic Info -->
      <div v-show="activeTab === 'profile'" class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title mb-4">基本资料</h2>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="form-control">
              <label class="label"><span class="label-text">用户昵称</span></label>
              <input v-model="basicForm.nickname" type="text" class="input input-bordered" />
            </div>
            <div class="form-control">
              <label class="label"><span class="label-text">用户名</span></label>
              <input :value="userInfo?.username || '-'" type="text" class="input input-bordered" disabled />
            </div>
            <div class="form-control">
              <label class="label"><span class="label-text">手机号码</span></label>
              <input v-model="basicForm.phone" type="text" class="input input-bordered" />
            </div>
            <div class="form-control">
              <label class="label"><span class="label-text">邮箱</span></label>
              <input v-model="basicForm.email" type="email" class="input input-bordered" />
            </div>
            <div class="form-control">
              <label class="label"><span class="label-text">性别</span></label>
              <select v-model="basicForm.sex" class="select select-bordered">
                <option :value="0">未知</option>
                <option :value="1">男</option>
                <option :value="2">女</option>
              </select>
            </div>
          </div>
          <div class="card-actions justify-end mt-4">
            <button class="btn btn-primary" @click="updateBasicInfo" :disabled="savingBasic" v-permission="['system:profile:edit']">
              <span v-if="savingBasic" class="loading loading-spinner loading-sm"></span>
              保存
            </button>
          </div>
        </div>
      </div>

      <!-- Change Password -->
      <div v-show="activeTab === 'password'" class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title mb-4">修改密码</h2>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="form-control md:col-span-2">
              <label class="label"><span class="label-text">旧密码</span></label>
              <input v-model="pwdForm.oldPassword" type="password" class="input input-bordered" />
            </div>
            <div class="form-control">
              <label class="label"><span class="label-text">新密码</span></label>
              <input v-model="pwdForm.newPassword" type="password" class="input input-bordered" />
            </div>
            <div class="form-control">
              <label class="label"><span class="label-text">确认新密码</span></label>
              <input v-model="pwdForm.confirmPassword" type="password" class="input input-bordered" />
            </div>
          </div>
          <div class="card-actions justify-end mt-4">
            <button class="btn btn-primary" @click="updatePasswordAction" :disabled="savingPwd" v-permission="['system:profile:password']">
              <span v-if="savingPwd" class="loading loading-spinner loading-sm"></span>
              修改密码
            </button>
          </div>
        </div>
      </div>

      <!-- Identity Verification -->
      <div v-show="activeTab === 'verify'" class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title mb-4">身份认证</h2>
          
          <!-- Current Status -->
          <div class="alert" :class="verifyAlertClass">
            <svg xmlns="http://www.w3.org/2000/svg" class="stroke-current shrink-0 h-6 w-6" fill="none" viewBox="0 0 24 24">
              <path v-if="userInfo?.verifyStatus === 2" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              <path v-else-if="userInfo?.verifyStatus === 1" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <div>
              <h3 class="font-bold">{{ verifyStatusText }}</h3>
              <div class="text-sm">{{ verifyStatusDesc }}</div>
            </div>
          </div>

          <!-- Verification Options -->
          <div v-if="userInfo?.verifyStatus === 0" class="mt-6">
            <h3 class="font-semibold mb-4">选择认证方式</h3>
            
            <!-- Email Verification -->
            <div class="collapse collapse-arrow bg-base-200 mb-3">
              <input type="radio" name="verify-accordion" v-model="verifyMethod" value="email" />
              <div class="collapse-title text-md font-medium">
                📧 教育邮箱认证
              </div>
              <div class="collapse-content">
                <p class="text-sm text-slate-500 mb-4">使用您的学校教育邮箱进行认证，系统将发送验证邮件到您的邮箱。</p>
                <div class="form-control">
                  <label class="label"><span class="label-text">教育邮箱</span></label>
                  <input v-model="eduEmailInput" type="email" placeholder="example@edu.cn" class="input input-bordered" />
                </div>
                <button class="btn btn-primary btn-sm mt-3" @click="submitEmailVerify" :disabled="verifying || !eduEmailInput">
                  <span v-if="verifying" class="loading loading-spinner loading-sm"></span>
                  发送验证邮件
                </button>
                <div v-if="emailCodeSent" class="mt-4 space-y-2">
                  <div class="form-control">
                    <label class="label"><span class="label-text">验证码</span></label>
                    <input v-model="emailCode" type="text" class="input input-bordered" placeholder="请输入邮箱验证码" />
                  </div>
                  <button class="btn btn-primary btn-sm" @click="submitEmailConfirm" :disabled="verifying || !emailCode">
                    <span v-if="verifying" class="loading loading-spinner loading-sm"></span>
                    确认认证
                  </button>
                </div>
              </div>
            </div>

            <!-- ID Card Verification -->
            <div class="collapse collapse-arrow bg-base-200">
              <input type="radio" name="verify-accordion" v-model="verifyMethod" value="id_card" />
              <div class="collapse-title text-md font-medium">
                🪪 学生证认证
              </div>
              <div class="collapse-content">
                <p class="text-sm text-slate-500 mb-4">上传您的学生证照片，管理员将在1-3个工作日内审核。</p>
                <div class="form-control">
                  <label class="label"><span class="label-text">学生证照片</span></label>
                  <input type="file" accept="image/*" class="file-input file-input-bordered w-full" @change="handleIdCardFile" />
                </div>
                <button class="btn btn-primary btn-sm mt-3" @click="submitIdCardVerify" :disabled="verifying || !idCardFile">
                  <span v-if="verifying" class="loading loading-spinner loading-sm"></span>
                  提交审核
                </button>
              </div>
            </div>

            <!-- Student ID Verification -->
            <div class="collapse collapse-arrow bg-base-200 mt-3">
              <input type="radio" name="verify-accordion" v-model="verifyMethod" value="student_id" />
              <div class="collapse-title text-md font-medium">
                🎓 学号认证
              </div>
              <div class="collapse-content">
                <p class="text-sm text-slate-500 mb-4">输入学号进行认证，一个学号仅允许绑定一个用户。</p>
                <div class="form-control">
                  <label class="label"><span class="label-text">学号</span></label>
                  <input v-model="studentIdInput" type="text" class="input input-bordered" placeholder="请输入学号" />
                </div>
                <button class="btn btn-primary btn-sm mt-3" @click="submitStudentIdVerify" :disabled="verifying || !studentIdInput">
                  <span v-if="verifying" class="loading loading-spinner loading-sm"></span>
                  提交审核
                </button>
              </div>
            </div>
          </div>

          <!-- Already Verified -->
          <div v-else-if="userInfo?.verifyStatus === 1" class="mt-6 text-center">
            <div class="text-5xl mb-4">⏳</div>
            <p class="text-slate-600">您的认证申请正在审核中，请耐心等待。</p>
            <button class="btn btn-outline btn-sm mt-4" @click="cancelVerifyRequest" :disabled="cancelingVerify">
              <span v-if="cancelingVerify" class="loading loading-spinner loading-sm"></span>
              取消认证申请
            </button>
          </div>

          <div v-else class="mt-6 text-center">
            <div class="text-6xl mb-4">🎉</div>
            <p class="text-slate-600">您已完成身份认证，认证方式：{{ formatVerifyMethod(userInfo?.verifyMethod) }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>

</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { getUserInfo, getMyProfile, updateProfile, updatePassword, verifyEmail, confirmEmail, submitIdCard, submitStudentId, cancelVerification, type UserProfileVO } from '@/api/auth'
import { uploadIdCardImage } from '@/api/file'
import { useDialog } from '@/composables/useDialog'

const userStore = useUserStore()
const dialog = useDialog()

interface UserProfile extends UserProfileVO {
  roles?: string[]
  permissions?: string[]
}

const userInfo = ref<UserProfile | null>(null)
const loading = ref(false)
const savingBasic = ref(false)
const savingPwd = ref(false)
const activeTab = ref<'profile' | 'password' | 'verify'>('profile')

// Verification
const verifyMethod = ref<'email' | 'id_card' | 'student_id'>('email')
const eduEmailInput = ref('')
const emailCode = ref('')
const emailCodeSent = ref(false)
const idCardFile = ref<File | null>(null)
const verifying = ref(false)
const studentIdInput = ref('')
const cancelingVerify = ref(false)

const basicForm = reactive({
  nickname: '',
  phone: '',
  email: '',
  sex: 0
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const displayRoles = computed(() => {
  return userInfo.value?.roles && userInfo.value.roles.length > 0 ? userInfo.value.roles : ['普通用户']
})

const avatarText = computed(() => {
  const name = userInfo.value?.nickname || userInfo.value?.username || 'U'
  return name.charAt(0).toUpperCase()
})

const verifyStatusBadge = computed(() => {
  const status = userInfo.value?.verifyStatus
  if (status === 2) return { text: '已认证', class: 'badge badge-success' }
  if (status === 1) return { text: '审核中', class: 'badge badge-warning' }
  if (status === 0) return { text: '未认证', class: 'badge badge-ghost' }
  return null
})

onMounted(() => {
  fetchUserInfo()
})

const fetchUserInfo = async () => {
  loading.value = true
  try {
    const [authInfo, profileInfo] = await Promise.all([
      getUserInfo().catch(e => { console.error('getUserInfo failed', e); return null }),
      getMyProfile().catch(e => { console.error('getMyProfile failed', e); return null })
    ])

    console.log('authInfo:', authInfo)
    console.log('profileInfo:', profileInfo)

    if (authInfo) {
      userStore.setUserInfo(authInfo)
    }

    // Prefer profileInfo (from /users/me) as it has more complete data
    const merged: UserProfile = {
      id: profileInfo?.id ?? authInfo?.id ?? 0,
      username: profileInfo?.username ?? authInfo?.username ?? '',
      nickname: profileInfo?.nickname ?? authInfo?.nickname ?? '',
      avatar: profileInfo?.avatar ?? authInfo?.avatar,
      email: profileInfo?.email ?? authInfo?.email,
      phone: profileInfo?.phone ?? authInfo?.phone,
      eduEmail: profileInfo?.eduEmail,
      sex: profileInfo?.sex ?? authInfo?.sex,
      verifyStatus: profileInfo?.verifyStatus ?? authInfo?.verifyStatus,
      verifyMethod: profileInfo?.verifyMethod ?? authInfo?.verifyMethod,
      verifyRejectReason: profileInfo?.verifyRejectReason,
      creditScore: profileInfo?.creditScore ?? authInfo?.creditScore,
      createdAt: profileInfo?.createdAt ?? authInfo?.createdAt,
      updatedAt: profileInfo?.updatedAt,
      roles: authInfo?.roles,
      permissions: authInfo?.permissions
    }

    userInfo.value = merged
    basicForm.nickname = merged.nickname || ''
    basicForm.email = merged.email || ''
    basicForm.phone = merged.phone || ''
    basicForm.sex = merged.sex ?? 0
  } catch (e) {
    console.error('Failed to load profile', e)
  } finally {
    loading.value = false
  }
}

const updateBasicInfo = async () => {
  if (!basicForm.nickname.trim()) {
    await dialog.alert('请输入昵称')
    return
  }
  savingBasic.value = true
  try {
    await updateProfile({
      nickname: basicForm.nickname.trim(),
      email: basicForm.email?.trim() || undefined,
      phone: basicForm.phone?.trim() || undefined,
      sex: Number(basicForm.sex)
    })
    await fetchUserInfo()
    await dialog.alert('保存成功')
  } catch (e: any) {
    await dialog.alert(e.message || e.response?.data?.message || '保存失败')
  } finally {
    savingBasic.value = false
  }
}

const updatePasswordAction = async () => {
  if (!pwdForm.oldPassword || !pwdForm.newPassword || !pwdForm.confirmPassword) {
    await dialog.alert('请填写完整的密码信息')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    await dialog.alert('两次输入的密码不一致')
    return
  }
  if (pwdForm.newPassword.length < 6) {
    await dialog.alert('密码长度不能少于6位')
    return
  }
  savingPwd.value = true
  try {
    await updatePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
      confirmPassword: pwdForm.confirmPassword
    })
    await dialog.alert('密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch (e: any) {
    await dialog.alert(e.message || e.response?.data?.message || '密码修改失败')
  } finally {
    savingPwd.value = false
  }
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString()
}

const formatSex = (sex?: number) => {
  if (sex === 1) return '男'
  if (sex === 2) return '女'
  return '未知'
}

const formatVerifyMethod = (method?: string) => {
  if (!method) return ''
  const normalized = method.toLowerCase()
  if (normalized === 'email' || normalized === 'edu_email') return '邮箱认证'
  if (normalized === 'id_card') return '学生证认证'
  if (normalized === 'id_list' || normalized === 'student_id') return '学号认证'
  if (normalized === 'ocr') return '证件OCR'
  if (normalized === 'sso') return 'SSO认证'
  if (normalized === 'manual') return '人工审核'
  return method || ''
}

const verifyAlertClass = computed(() => {
  const status = userInfo.value?.verifyStatus
  if (status === 2) return 'alert-success'
  if (status === 1) return 'alert-warning'
  return 'alert-info'
})

const verifyStatusText = computed(() => {
  const status = userInfo.value?.verifyStatus
  if (status === 2) return '已完成认证'
  if (status === 1) return '审核中'
  return '未认证'
})

const verifyStatusDesc = computed(() => {
  const status = userInfo.value?.verifyStatus
  if (status === 2) return '您的身份已通过认证，可以使用完整功能。'
  if (status === 1) return '您的认证申请正在审核中，请耐心等待。'
  if (userInfo.value?.verifyRejectReason) {
    return `认证未通过：${userInfo.value.verifyRejectReason}`
  }
  return '完成身份认证后可以使用更多功能。'
})

const handleIdCardFile = (e: Event) => {
  const target = e.target as HTMLInputElement
  if (target.files && target.files[0]) {
    idCardFile.value = target.files[0]
  }
}

const submitEmailVerify = async () => {
  if (!eduEmailInput.value) return
  verifying.value = true
  try {
    await verifyEmail(eduEmailInput.value)
    emailCodeSent.value = true
    await dialog.alert('验证邮件已发送，请查收邮箱并点击链接完成认证。')
  } catch (e: any) {
    await dialog.alert(e.message || e.response?.data?.message || '发送失败')
  } finally {
    verifying.value = false
  }
}

const submitEmailConfirm = async () => {
  if (!emailCode.value) return
  verifying.value = true
  try {
    await confirmEmail(emailCode.value)
    await dialog.alert('认证成功')
    emailCode.value = ''
    emailCodeSent.value = false
    await fetchUserInfo()
  } catch (e: any) {
    await dialog.alert(e.message || e.response?.data?.message || '认证失败')
  } finally {
    verifying.value = false
  }
}

const submitIdCardVerify = async () => {
  if (!idCardFile.value) return
  verifying.value = true
  try {
    const upload = await uploadIdCardImage(idCardFile.value)
    await submitIdCard({ imageUrl: String(upload.id) })
    await dialog.alert('提交成功，请等待审核。')
    await fetchUserInfo()
    idCardFile.value = null
  } catch (e: any) {
    await dialog.alert(e.message || e.response?.data?.message || '提交失败')
  } finally {
    verifying.value = false
  }
}

const submitStudentIdVerify = async () => {
  if (!studentIdInput.value.trim()) {
    await dialog.alert('请输入学号')
    return
  }
  verifying.value = true
  try {
    await submitStudentId({ studentId: studentIdInput.value.trim() })
    await dialog.alert('提交成功，请等待审核。')
    studentIdInput.value = ''
    await fetchUserInfo()
  } catch (e: any) {
    await dialog.alert(e.message || e.response?.data?.message || '提交失败')
  } finally {
    verifying.value = false
  }
}

const cancelVerifyRequest = async () => {
  const ok = await dialog.confirm('确定要取消当前认证申请吗？')
  if (!ok) return
  cancelingVerify.value = true
  try {
    await cancelVerification()
    await dialog.alert('已取消认证申请')
    await fetchUserInfo()
  } catch (e: any) {
    await dialog.alert(e.message || e.response?.data?.message || '取消失败')
  } finally {
    cancelingVerify.value = false
  }
}
</script>
