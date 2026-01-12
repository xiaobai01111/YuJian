<template>
  <div class="navbar sticky top-0 z-50 bg-base-100/80 backdrop-blur-md border-b border-base-200 transition-all duration-300 h-16">
    <div class="container mx-auto px-4">
      <!-- Navbar Start: Logo -->
      <div class="navbar-start w-auto mr-8">
        <div class="dropdown lg:hidden">
          <label tabindex="0" class="btn btn-ghost btn-circle">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h8m-8 6h16" />
            </svg>
          </label>
          <ul tabindex="0" class="menu menu-sm dropdown-content mt-3 z-[1] p-2 shadow bg-base-100 rounded-box w-52">
            <li><router-link to="/">首页</router-link></li>
            <li><router-link to="/confessions">表白墙</router-link></li>
            <li><router-link to="/treehole">树洞</router-link></li>
            <li><router-link to="/help">求助</router-link></li>
            <li><router-link to="/market">市集</router-link></li>
            <li><router-link to="/lost-found">失物</router-link></li>
          </ul>
        </div>
        <router-link to="/" class="flex items-center gap-2 group">
          <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-primary to-secondary flex items-center justify-center text-white font-bold text-lg shadow-lg group-hover:scale-110 transition-transform">
            {{ title.charAt(0) }}
          </div>
          <span class="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-base-content to-base-content/60">
            {{ title }}
          </span>
        </router-link>
      </div>

      <!-- Navbar Center: Navigation Links -->
      <div class="navbar-center hidden lg:flex flex-1 justify-center">
        <ul class="menu menu-horizontal px-1 gap-1">
          <li>
            <router-link to="/" class="group flex items-center gap-2 px-4 py-2 hover:bg-transparent hover:text-primary transition-colors" active-class="text-primary font-medium">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 group-hover:-translate-y-0.5 transition-transform" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
              </svg>
              首页
            </router-link>
          </li>
          <li>
            <router-link to="/confessions" class="group flex items-center gap-2 px-4 py-2 hover:bg-transparent hover:text-pink-500 transition-colors" active-class="text-pink-500 font-medium">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 group-hover:-translate-y-0.5 transition-transform" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
              </svg>
              表白墙
            </router-link>
          </li>
          <li>
            <router-link to="/treehole" class="group flex items-center gap-2 px-4 py-2 hover:bg-transparent hover:text-green-500 transition-colors" active-class="text-green-500 font-medium">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 group-hover:-translate-y-0.5 transition-transform" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
              树洞
            </router-link>
          </li>
          <li>
            <router-link to="/help" class="group flex items-center gap-2 px-4 py-2 hover:bg-transparent hover:text-blue-500 transition-colors" active-class="text-blue-500 font-medium">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 group-hover:-translate-y-0.5 transition-transform" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              求助
            </router-link>
          </li>
          <li>
            <router-link to="/market" class="group flex items-center gap-2 px-4 py-2 hover:bg-transparent hover:text-orange-500 transition-colors" active-class="text-orange-500 font-medium">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 group-hover:-translate-y-0.5 transition-transform" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
              </svg>
              市集
            </router-link>
          </li>
          <li>
            <router-link to="/lost-found" class="group flex items-center gap-2 px-4 py-2 hover:bg-transparent hover:text-purple-500 transition-colors" active-class="text-purple-500 font-medium">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 group-hover:-translate-y-0.5 transition-transform" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              失物
            </router-link>
          </li>
        </ul>
      </div>

      <!-- Navbar End: Actions -->
      <div class="navbar-end w-auto flex items-center gap-2">
        <!-- Search Button -->
        <button class="btn btn-ghost btn-circle btn-sm">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </button>

        <!-- Write/Edit Button -->
        <button class="btn btn-ghost btn-circle btn-sm">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
          </svg>
        </button>
        
        <div class="divider divider-horizontal mx-1 h-6 self-center"></div>

        <!-- Post Button -->
        <button class="btn btn-primary btn-sm rounded-full px-5 gap-2 bg-gradient-to-r from-purple-600 to-indigo-600 border-none hover:shadow-lg hover:shadow-indigo-500/30 transition-all">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          发布
        </button>

        <!-- Notification -->
         <button class="btn btn-circle btn-sm btn-ghost relative">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
          </svg>
          <span class="badge badge-xs badge-error absolute top-0 right-0"></span>
        </button>

        <!-- User Dropdown -->
        <div class="dropdown dropdown-end">
          <label tabindex="0" class="btn btn-ghost btn-circle avatar online placeholder ring-2 ring-primary ring-offset-2 ring-offset-base-100 transition-all hover:scale-110 ml-2">
            <div class="bg-neutral text-neutral-content rounded-full w-9">
              <span class="text-lg">{{ userStore.token ? getAvatarText(userStore.userInfo?.nickname || userStore.userInfo?.username) : '客' }}</span>
            </div>
          </label>
          <ul tabindex="0" class="menu menu-sm dropdown-content mt-3 z-[1] p-2 shadow-xl bg-base-100 rounded-2xl w-56 border border-base-200">
            <li class="menu-title px-4 py-2">
              <span class="text-xs font-normal text-slate-400">当前身份</span>
              <span class="font-bold text-slate-800 text-sm truncate block max-w-[180px]">
                {{ userStore.token ? (userStore.userInfo?.nickname || userStore.userInfo?.username) : '游客 (Guest)' }}
              </span>
            </li>
            <li class="my-1 border-b border-base-200"></li>
            
            <template v-if="userStore.token">
              <li><router-link to="/console/dashboard">进入后台</router-link></li>
              <li><a>个人设置</a></li>
              <li class="my-1 border-b border-base-200"></li>
              <li><a @click="handleLogout" class="text-error">退出登录</a></li>
            </template>
            
            <template v-else>
              <li><a @click="openLoginModal" class="text-primary font-bold">登录 / 注册</a></li>
              <li><a href="#" class="text-slate-500">使用帮助</a></li>
            </template>
          </ul>
        </div>
      </div>
    </div>
  </div>

  <LoginModal />
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import LoginModal from '@/components/auth/LoginModal.vue'

const userStore = useUserStore()
const router = useRouter()
const title = import.meta.env.VITE_APP_TITLE || 'CampusWall'

const handleLogout = () => {
  userStore.logout()
  router.push('/')
}

const openLoginModal = () => {
  const modal = document.getElementById('login_modal') as HTMLDialogElement
  modal?.showModal()
}

const getAvatarText = (name: string | undefined) => {
  if (!name) return '?'
  // Return first char, handle potential diverse characters
  return name.charAt(0).toUpperCase()
}
</script>
