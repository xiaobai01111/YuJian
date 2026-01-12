<template>
  <dialog id="login_modal" class="modal">
    <div class="modal-box w-full max-w-sm p-8 rounded-3xl bg-base-100 shadow-2xl overflow-visible relative">
      <form method="dialog">
        <button class="btn btn-sm btn-circle btn-ghost absolute right-4 top-4">✕</button>
      </form>
      
      <!-- Header -->
      <div class="text-center mb-8">
        <div class="w-16 h-16 rounded-2xl bg-gradient-to-br from-purple-600 to-blue-500 mx-auto flex items-center justify-center shadow-lg mb-4">
          <span class="text-3xl font-bold text-white">C</span>
        </div>
        <h3 class="font-bold text-2xl text-slate-800 dark:text-white">{{ isLogin ? '欢迎回来' : '加入我们' }}</h3>
        <p class="text-slate-500 text-sm mt-2">{{ isLogin ? '登录 CampusWall 开始你的校园之旅' : '创建一个账号，连接校园生活' }}</p>
      </div>

      <!-- Login Form -->
      <form v-if="isLogin" @submit.prevent="handleLogin" class="space-y-4">
        <div class="form-control">
          <div class="relative">
            <input 
              v-model="loginForm.username" 
              type="text" 
              placeholder="学号 / 用户名" 
              class="input input-bordered w-full pl-12 rounded-xl bg-base-200/50 focus:bg-base-100 transition-colors h-12"
              required 
            />
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 absolute left-4 top-3.5 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
          </div>
        </div>

        <div class="form-control">
          <div class="relative">
            <input 
              v-model="loginForm.password" 
              type="password" 
              placeholder="密码" 
              class="input input-bordered w-full pl-12 rounded-xl bg-base-200/50 focus:bg-base-100 transition-colors h-12"
              required 
            />
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 absolute left-4 top-3.5 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
          </div>
          <label class="label">
            <span class="label-text-alt text-error" v-if="errorMsg">{{ errorMsg }}</span>
            <a href="#" class="label-text-alt link link-hover text-primary ml-auto">忘记密码？</a>
          </label>
        </div>

        <button 
          type="submit" 
          class="btn btn-primary w-full rounded-xl h-12 text-base font-bold bg-gradient-to-r from-purple-600 to-indigo-600 border-none shadow-lg shadow-purple-500/20 hover:shadow-purple-500/40 transition-all"
          :class="{ 'loading': loading }"
        >
          {{ loading ? '登录中...' : '立即登录' }}
        </button>
      </form>

      <!-- Register Form -->
      <form v-else @submit.prevent="handleRegister" class="space-y-4">
         <div class="form-control">
          <div class="relative">
            <input 
              v-model="registerForm.username" 
              type="text" 
              placeholder="用户名 (3-20字符)" 
              class="input input-bordered w-full pl-12 rounded-xl bg-base-200/50 focus:bg-base-100 transition-colors h-12"
              required 
              minlength="3"
              maxlength="20"
            />
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 absolute left-4 top-3.5 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
          </div>
        </div>
        
        <div class="form-control">
          <div class="relative">
             <input 
              v-model="registerForm.nickname" 
              type="text" 
              placeholder="昵称 (可选)" 
              class="input input-bordered w-full pl-12 rounded-xl bg-base-200/50 focus:bg-base-100 transition-colors h-12"
            />
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 absolute left-4 top-3.5 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.828 14.828a4 4 0 01-5.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
        </div>

        <div class="form-control">
          <div class="relative">
            <input 
              v-model="registerForm.password" 
              type="password" 
              placeholder="密码 (6-32字符)" 
              class="input input-bordered w-full pl-12 rounded-xl bg-base-200/50 focus:bg-base-100 transition-colors h-12"
              required 
              minlength="6"
              maxlength="32"
            />
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 absolute left-4 top-3.5 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
          </div>
        </div>

        <div class="form-control">
          <div class="relative">
            <input 
              v-model="registerForm.confirmPassword" 
              type="password" 
              placeholder="确认密码" 
              class="input input-bordered w-full pl-12 rounded-xl bg-base-200/50 focus:bg-base-100 transition-colors h-12"
              required 
            />
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 absolute left-4 top-3.5 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
           <label class="label">
            <span class="label-text-alt text-error" v-if="errorMsg">{{ errorMsg }}</span>
          </label>
        </div>

        <button 
          type="submit" 
          class="btn btn-primary w-full rounded-xl h-12 text-base font-bold bg-gradient-to-r from-purple-600 to-indigo-600 border-none shadow-lg shadow-purple-500/20 hover:shadow-purple-500/40 transition-all"
          :class="{ 'loading': loading }"
        >
          {{ loading ? '注册中...' : '立即注册' }}
        </button>
      </form>
      
      <!-- Footer -->
      <div class="text-center mt-6">
        <p class="text-sm text-slate-500">
          {{ isLogin ? '还没有账号？' : '已有账号？' }}
          <a @click="toggleMode" class="text-primary font-bold hover:underline cursor-pointer">
            {{ isLogin ? '立即注册' : '立即登录' }}
          </a>
        </p>
      </div>
    </div>
  </dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const isLogin = ref(true)
const loading = ref(false)
const errorMsg = ref('')

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

const toggleMode = () => {
  isLogin.value = !isLogin.value
  errorMsg.value = ''
  // Reset forms logic could be added here
}

const handleLogin = async () => {
  loading.value = true
  errorMsg.value = ''
  try {
    await userStore.login(loginForm)
    // Close modal
    const modal = document.getElementById('login_modal') as HTMLDialogElement
    modal?.close()
    // Reset form
    loginForm.username = ''
    loginForm.password = ''
    window.location.reload() // Or router refresh
  } catch (error: any) {
    errorMsg.value = error.message || '登录失败，请检查用户名和密码'
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  if (registerForm.password !== registerForm.confirmPassword) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }
  
  loading.value = true
  errorMsg.value = ''
  try {
    await userStore.register(registerForm)
    // Switch to login
    isLogin.value = true
    // Pre-fill login
    loginForm.username = registerForm.username
    loginForm.password = '' // Don't pre-fill password for security/UX preference
    errorMsg.value = '' // Clear error
    // Maybe show success message toast?
    alert('注册成功，请登录')
  } catch (error: any) {
    errorMsg.value = error.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>
