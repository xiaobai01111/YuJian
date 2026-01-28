<template>
  <div class="min-h-screen flex items-center justify-center bg-base-200">
    <div class="card bg-base-100 shadow-xl max-w-md w-full mx-4">
      <div class="card-body text-center">
        <div class="text-6xl mb-4">🔒</div>
        <h2 class="card-title justify-center text-2xl mb-2">暂无后台权限</h2>
        <p class="text-slate-500 mb-6">
          您的账号没有后台管理权限，如需开通请联系管理员。
        </p>
        
        <div v-if="adminContact.email || adminContact.phone" class="bg-base-200 rounded-lg p-4 mb-6">
          <h3 class="font-semibold mb-3">管理员联系方式</h3>
          <div class="space-y-2 text-sm">
            <div v-if="adminContact.email" class="flex items-center justify-center gap-2">
              <span>📧</span>
              <a :href="'mailto:' + adminContact.email" class="link link-primary">{{ adminContact.email }}</a>
            </div>
            <div v-if="adminContact.phone" class="flex items-center justify-center gap-2">
              <span>📱</span>
              <a :href="'tel:' + adminContact.phone" class="link link-primary">{{ adminContact.phone }}</a>
            </div>
          </div>
        </div>

        <div v-else class="bg-base-200 rounded-lg p-4 mb-6">
          <p class="text-slate-400 text-sm">管理员联系方式未设置</p>
        </div>

        <div class="card-actions justify-center">
          <button class="btn btn-primary" @click="goHome">返回首页</button>
          <button class="btn btn-outline" @click="logout">退出登录</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getAdminContact, type AdminContactVO } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const adminContact = ref<AdminContactVO>({})

onMounted(async () => {
  try {
    const res: any = await getAdminContact()
    adminContact.value = res || {}
  } catch (e) {
    console.error('Failed to get admin contact', e)
  }
})

const goHome = () => {
  router.push('/')
}

const logout = () => {
  userStore.logout()
  router.push('/')
}
</script>
