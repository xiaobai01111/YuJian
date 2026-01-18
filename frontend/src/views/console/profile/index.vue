<template>
  <div class="grid grid-cols-1 xl:grid-cols-3 gap-6">
    <!-- Profile Summary -->
    <div class="card bg-base-100 shadow-xl">
      <div class="card-body">
        <div class="flex items-center gap-4">
          <div class="avatar placeholder">
            <div class="bg-primary text-primary-content rounded-full w-20">
              <span class="text-2xl">{{ avatarText }}</span>
            </div>
          </div>
          <div>
            <h2 class="text-xl font-semibold">{{ userInfo?.nickname || '用户' }}</h2>
            <p class="text-slate-500">@{{ userInfo?.username || '-' }}</p>
            <div class="mt-2 flex flex-wrap gap-2">
              <span v-if="verifyStatusBadge" :class="verifyStatusBadge.class">{{ verifyStatusBadge.text }}</span>
              <span v-if="userInfo?.creditScore != null" class="badge badge-outline">信用 {{ userInfo.creditScore }}</span>
            </div>
          </div>
        </div>

        <div class="divider my-4"></div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
          <div><span class="font-semibold">邮箱：</span>{{ userInfo?.email || '未设置' }}</div>
          <div><span class="font-semibold">手机：</span>{{ userInfo?.phone || '未设置' }}</div>
          <div><span class="font-semibold">性别：</span>{{ formatSex(userInfo?.sex) }}</div>
          <div><span class="font-semibold">注册时间：</span>{{ formatDate(userInfo?.createdAt) }}</div>
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
      <!-- Basic Info -->
      <div class="card bg-base-100 shadow-xl">
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
      <div class="card bg-base-100 shadow-xl">
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
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { getUserInfo, getMyProfile, updateProfile, updatePassword, type UserProfileVO } from '@/api/auth'

const userStore = useUserStore()

interface UserProfile extends UserProfileVO {
  roles?: string[]
  permissions?: string[]
}

const userInfo = ref<UserProfile | null>(null)
const loading = ref(false)
const savingBasic = ref(false)
const savingPwd = ref(false)

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
      getUserInfo(),
      getMyProfile().catch(() => null)
    ])

    if (authInfo) {
      userStore.setUserInfo(authInfo)
    }

    const merged: UserProfile = {
      ...profileInfo,
      ...authInfo,
      phone: profileInfo?.phone ?? authInfo?.phone,
      sex: profileInfo?.sex ?? authInfo?.sex,
      createdAt: authInfo?.createdAt ?? profileInfo?.createdAt
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
    alert('请输入昵称')
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
    alert('保存成功')
  } catch (e: any) {
    alert(e.message || e.response?.data?.message || '保存失败')
  } finally {
    savingBasic.value = false
  }
}

const updatePasswordAction = async () => {
  if (!pwdForm.oldPassword || !pwdForm.newPassword || !pwdForm.confirmPassword) {
    alert('请填写完整的密码信息')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    alert('两次输入的密码不一致')
    return
  }
  if (pwdForm.newPassword.length < 6) {
    alert('密码长度不能少于6位')
    return
  }
  savingPwd.value = true
  try {
    await updatePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
      confirmPassword: pwdForm.confirmPassword
    })
    alert('密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch (e: any) {
    alert(e.message || e.response?.data?.message || '密码修改失败')
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
</script>
