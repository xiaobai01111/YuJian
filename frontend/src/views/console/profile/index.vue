<template>
  <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
    <!-- Profile Card -->
    <div class="card bg-base-100 shadow-xl">
      <div class="card-body items-center text-center">
        <div class="avatar placeholder mb-4">
          <div class="bg-primary text-primary-content rounded-full w-24">
            <span class="text-3xl">{{ userInfo?.nickname?.charAt(0) || 'U' }}</span>
          </div>
        </div>
        <h2 class="card-title">{{ userInfo?.nickname || '用户' }}</h2>
        <p class="text-slate-500">@{{ userInfo?.username }}</p>
        <div class="divider"></div>
        <div class="w-full text-left space-y-2 text-sm">
          <p><span class="font-semibold">邮箱：</span>{{ userInfo?.email || '未设置' }}</p>
          <p><span class="font-semibold">手机：</span>{{ userInfo?.phone || '未设置' }}</p>
          <p><span class="font-semibold">部门：</span>{{ userInfo?.dept || '未分配' }}</p>
          <p><span class="font-semibold">角色：</span>
            <span v-for="role in userInfo?.roles" :key="role" class="badge badge-ghost badge-sm mr-1">{{ role }}</span>
          </p>
          <p><span class="font-semibold">创建时间：</span>{{ formatDate(userInfo?.createdAt) }}</p>
        </div>
      </div>
    </div>

    <!-- Edit Forms -->
    <div class="lg:col-span-2 space-y-6">
      <!-- Basic Info -->
      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title mb-4">基本资料</h2>
          <div class="form-control">
            <label class="label"><span class="label-text">用户昵称</span></label>
            <input v-model="basicForm.nickname" type="text" class="input input-bordered" />
          </div>
          <div class="form-control mt-2">
            <label class="label"><span class="label-text">手机号码</span></label>
            <input v-model="basicForm.phone" type="text" class="input input-bordered" />
          </div>
          <div class="form-control mt-2">
            <label class="label"><span class="label-text">邮箱</span></label>
            <input v-model="basicForm.email" type="email" class="input input-bordered" />
          </div>
          <div class="form-control mt-2">
            <label class="label"><span class="label-text">性别</span></label>
            <select v-model="basicForm.sex" class="select select-bordered">
              <option value="0">男</option>
              <option value="1">女</option>
              <option value="2">未知</option>
            </select>
          </div>
          <div class="card-actions justify-end mt-4">
            <button class="btn btn-primary" @click="updateBasicInfo" :disabled="submitting" v-permission="['system:profile:edit']">
              保存
            </button>
          </div>
        </div>
      </div>

      <!-- Change Password -->
      <div class="card bg-base-100 shadow-xl">
        <div class="card-body">
          <h2 class="card-title mb-4">修改密码</h2>
          <div class="form-control">
            <label class="label"><span class="label-text">旧密码</span></label>
            <input v-model="pwdForm.oldPassword" type="password" class="input input-bordered" />
          </div>
          <div class="form-control mt-2">
            <label class="label"><span class="label-text">新密码</span></label>
            <input v-model="pwdForm.newPassword" type="password" class="input input-bordered" />
          </div>
          <div class="form-control mt-2">
            <label class="label"><span class="label-text">确认新密码</span></label>
            <input v-model="pwdForm.confirmPassword" type="password" class="input input-bordered" />
          </div>
          <div class="card-actions justify-end mt-4">
            <button class="btn btn-primary" @click="updatePassword" :disabled="submitting" v-permission="['system:profile:password']">
              修改密码
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

interface UserInfo {
  id: number
  username: string
  nickname: string
  email?: string
  phone?: string
  sex?: string
  dept?: string
  roles?: string[]
  createdAt?: string
}

const userInfo = ref<UserInfo | null>(null)
const submitting = ref(false)

const basicForm = reactive({
  nickname: '',
  phone: '',
  email: '',
  sex: '0'
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

onMounted(() => {
  fetchUserInfo()
})

const fetchUserInfo = async () => {
  // 从 store 获取用户信息
  const info = userStore.userInfo
  if (info) {
    userInfo.value = {
      id: info.id,
      username: info.username,
      nickname: info.nickname,
      email: info.email,
      roles: info.roles,
      createdAt: info.createdAt
    }
    basicForm.nickname = info.nickname || ''
    basicForm.email = info.email || ''
  }
}

const updateBasicInfo = async () => {
  submitting.value = true
  setTimeout(() => {
    alert('保存成功')
    submitting.value = false
  }, 500)
}

const updatePassword = async () => {
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    alert('两次输入的密码不一致')
    return
  }
  if (pwdForm.newPassword.length < 6) {
    alert('密码长度不能少于6位')
    return
  }
  submitting.value = true
  setTimeout(() => {
    alert('密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    submitting.value = false
  }, 500)
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString()
}
</script>
